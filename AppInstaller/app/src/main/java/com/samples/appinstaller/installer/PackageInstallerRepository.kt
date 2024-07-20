/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.appinstaller.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.os.BuildCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samples.appinstaller.NotificationRepository
import com.samples.appinstaller.database.ActionStatus
import com.samples.appinstaller.database.ActionType
import com.samples.appinstaller.database.PackageInstallerDao
import com.samples.appinstaller.store.PackageName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.io.IOException
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageInstallerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val database: PackageInstallerDao,
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    /**
     * Keep track of the pending user action per package name
     */
    private val pendingUserActionQueue: Queue<Intent> = LinkedList()
    private val _pendingUserActionEvents = MutableSharedFlow<Unit>()
    val pendingUserActionEvents: SharedFlow<Unit> = _pendingUserActionEvents

    /**
     * Add an item to the pending user actions queue
     */
    private fun enqueuePendingUserAction(packageName: PackageName, statusIntent: Intent) {
        logcat { "enqueue pending user action for $packageName" }
        runBlocking {
            pendingUserActionQueue.add(statusIntent)
            _pendingUserActionEvents.emit(Unit)
        }
    }

    /**
     * Get the first pending user action from the queue
     */
    fun getPendingUserActionFromQueue(): Intent? {
        return pendingUserActionQueue.peek()
    }

    /**
     * Get the first pending user action from the queue
     */
    fun removePendingUserActionFromQueue() {
        pendingUserActionQueue.poll()
    }

    /**
     * Clean obsolete sessions (initialized worker tasks terminated by the system without clearing
     * their install state)
     */
    fun cleanObsoleteSessions() {
        val obsoleteDelay = System.currentTimeMillis() - 60_000L
        runBlocking {
            database.getObsoleteActions(obsoleteDelay).forEach { packageAction ->
                WorkManager.getInstance(context).cancelAllWorkByTag(packageAction.packageName)

                database.addAction(
                    packageName = packageAction.packageName,
                    type = ActionType.INSTALL,
                    sessionId = packageAction.sessionId,
                    status = ActionStatus.FAILURE,
                )
            }
        }
    }

    /**
     * Add saved pending intents to the [pendingUserActionQueue]
     */
    fun redeliverSavedUserActions() {
        runBlocking {
            val pendingUserActions = database.getPendingUserActions()

            pendingUserActions.forEach { (packageName, _) ->
                val pendingIntent = getCachedStatusPendingIntent(packageName)

                if (pendingIntent == null) {
                    database.addAction(
                        packageName = packageName,
                        type = ActionType.INSTALL,
                        status = ActionStatus.FAILURE,
                    )
                } else {
                    try {
                        pendingIntent.send(
                            context,
                            0,
                            Intent(context, SessionStatusReceiver::class.java)
                        )

                        logcat { "Redelivered status intent for $packageName" }
                    } catch (ignored: PendingIntent.CanceledException) {
                        logcat(LogPriority.ERROR) { ignored.asLog() }
                        database.addAction(
                            packageName = packageName,
                            type = ActionType.INSTALL,
                            status = ActionStatus.FAILURE,
                        )
                    }
                }
            }
        }
    }

    /**
     * Check if the app is allowed to install apps
     */
    fun canInstallPackages(): Boolean {
        return packageManager.canRequestPackageInstalls()
    }

    /**
     * Get app launching intent (main intent with intent filter: android.intent.action.MAIN)
     */
    fun getAppLaunchingIntent(packageName: PackageName) =
        packageManager.getLaunchIntentForPackage(packageName)

    /**
     * Uninstall this package name
     */
    fun uninstallApp(packageName: PackageName) {
        runBlocking {
            database.addAction(
                packageName = packageName,
                type = ActionType.UNINSTALL,
                status = ActionStatus.INITIALIZED
            )
        }

        /**
         * We request an uninstallation from [PackageInstaller] and provides a pending intent that
         * will be send back to our app via [com.samples.appinstaller.installer.SessionStatusReceiver]
         * after the uninstallation has started
         */
        val statusIntent = Intent(context, SessionStatusReceiver::class.java).apply {
            data = Uri.fromParts("package", packageName, null)
            putExtra("action", SessionStatusReceiver.UNINSTALL_ACTION)
        }

        runBlocking {
            val statusPendingIntent =
                PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_MUTABLE)

            packageInstaller.uninstall(packageName, statusPendingIntent.intentSender)

            runBlocking {
                database.addAction(
                    packageName = packageName,
                    type = ActionType.UNINSTALL,
                    status = ActionStatus.COMMITTED
                )
            }
        }
    }

    /**
     * Cancel all pending & active installs or upgrades related to this package name. We cancel all
     * pending and active [InstallWorker] jobs related to [packageName] and abandon all
     * [PackageInstaller.Session] related to [packageName]
     */
    fun cancelInstall(packageName: PackageName) {
        WorkManager.getInstance(context).cancelAllWorkByTag(packageName)
        onInstallComplete(packageName, ActionStatus.CANCELLATION, null)
    }

    /**
     * Initialize an install session for this package name
     */
    fun installApp(packageName: PackageName) {
        // We add this install job to our WorkManager queue
        val installWorkRequest = OneTimeWorkRequestBuilder<InstallWorker>()
            .addTag(packageName)
            .setInputData(workDataOf(InstallWorker.PACKAGE_NAME_KEY to packageName))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(packageName, ExistingWorkPolicy.KEEP, installWorkRequest)
    }

    /**
     * Get [PackageInstaller.Session] details
     */
    fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
        return packageInstaller.getSessionInfo(sessionId)
    }

    /**
     * Generate an install session to be created and opened by the [PackageInstaller]
     */
    fun createInstallSession(packageName: PackageName): Int? {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
            .apply {
                setAppPackageName(packageName)
                setInstallReason(PackageManager.INSTALL_REASON_USER)

                if (BuildCompat.isAtLeastS()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

        return try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            logcat(LogPriority.ERROR) { "Exception when creating session ${e.asLog()}" }
            null
        }
    }

    /**
     * Activate a [PackageInstaller.Session], required step before committing it
     */
    fun openInstallSession(sessionId: Int): PackageInstaller.Session {
        return packageInstaller.openSession(sessionId)
    }

    /**
     * Create a status [Intent] that will be used to call the [SessionStatusReceiver] once an
     * install session has been processed
     */
    private fun createStatusIntent(packageName: PackageName): Intent {
        return Intent(context, SessionStatusReceiver::class.java).apply {
            // For convenience & to ensure a unique intent per-package:
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_RECEIVER_FOREGROUND

            putExtra("action", SessionStatusReceiver.INSTALL_ACTION)
        }
    }

    /**
     * Create a [PendingIntent] that will hold a status [Intent] created by [createStatusIntent] to
     * callback the [SessionStatusReceiver] once an install session has been processed
     */
    fun createStatusPendingIntent(packageName: PackageName): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * We update the saved [PendingIntent] (only if there's one already) to make sure the system
     * caches only the most recent install operation that requires user action
     */
    private fun updateStatusPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        )
    }

    /**
     * We request a [PendingIntent] related to [packageName] from the system only if there's one
     * already. In this case, it means our pending user action intent hasn't been completed by the
     * user yet, so we bring it back to the user to complete or cancel it
     */
    private fun getCachedStatusPendingIntent(packageName: PackageName): PendingIntent? {
        return PendingIntent.getBroadcast(
            context,
            0,
            createStatusIntent(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
        )
    }

    /**
     * We save this status intent inside a pending one to be used for later when the user
     */
    fun saveStatusPendingIntentForLater(statusIntent: Intent) {
        val packageName = statusIntent.data!!.schemeSpecificPart
        logcat { "saveStatusPendingIntentForLater $packageName" }

        // Don't save these extras which we want to receive future changes to.
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS)
        statusIntent.removeExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        statusIntent.putExtra(SessionStatusReceiver.EXTRA_REDELIVER, true)
        updateStatusPendingIntent(statusIntent)
    }

    /**
     * Once an install/uninstall operation is complete, we cancel any pending status intent related
     * to the given package name (to not trigger user action once the app is resumed
     */
    private fun cancelStatusPendingIntent(packageName: PackageName) {
        logcat { "cancelStatusPendingIntent $packageName" }
        getCachedStatusPendingIntent(packageName)?.cancel()
    }

    fun isSessionValid(packageName: PackageName, status: ActionStatus, sessionId: Int): Boolean {
        return runBlocking {
            val packageAction = database.getPackageAction(packageName, status)

            if (packageAction != null) {
                if (packageAction.sessionId == sessionId) {
                    return@runBlocking true
                } else {
                    logcat(LogPriority.ERROR) {
                        "isSessionValid: $packageName ($sessionId) isn't the same as the one saved ${packageAction.sessionId}"
                    }
                    return@runBlocking false
                }
            } else {
                logcat(LogPriority.ERROR) {
                    "isSessionValid: $packageName ($sessionId) doesn't have a saved package action"
                }
                return@runBlocking false
            }
        }
    }

    fun onInstallPendingUserAction(packageName: PackageName, sessionId: Int, statusIntent: Intent) {
        logcat { "onInstallPendingUserAction $packageName" }

        enqueuePendingUserAction(packageName, statusIntent)

        runBlocking {
            database.addAction(
                packageName = packageName,
                type = ActionType.INSTALL,
                sessionId = sessionId,
                status = ActionStatus.PENDING_USER_ACTION
            )
        }

        // We display a notification if the app isn't resumed
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notifyPendingUserActions()
        }
    }

    fun onUninstallPendingUserAction(packageName: PackageName, statusIntent: Intent) {
        logcat { "onUninstallPendingUserAction $packageName" }

        enqueuePendingUserAction(packageName, statusIntent)
    }

    fun notifyPendingUserActions() {
        runBlocking {
            val pendingUserActions = database.getPendingUserActions()
            if (pendingUserActions.isNotEmpty()) {
                logcat { "${pendingUserActions.size} pendingUserActions left" }
                notificationRepository.createInstallNotification()
            }
        }
    }

    private fun clearInstallNotificationOnComplete() {
        runBlocking {
            val pendingUserActions = database.getPendingUserActions()
            if (pendingUserActions.isEmpty()) {
                cancelInstallNotification()
            }
        }
    }

    fun cancelInstallNotification() {
        notificationRepository.cancelInstallNotification()
    }

    fun createSessionActionObserver(
        packageName: PackageName,
        sessionId: Int
    ): SessionActionObserver {
        return SessionActionObserver(packageInstaller, packageName, sessionId)
    }

    fun onInstallComplete(packageName: PackageName, status: ActionStatus, sessionId: Int?) {
        runBlocking {
            database.addAction(
                packageName = packageName,
                type = ActionType.INSTALL,
                sessionId = sessionId,
                status = status
            )
        }

        cancelStatusPendingIntent(packageName)
        clearInstallNotificationOnComplete()
    }

    fun onUninstallComplete(packageName: PackageName, status: ActionStatus, sessionId: Int) {
        runBlocking {
            database.addAction(
                packageName = packageName,
                type = ActionType.UNINSTALL,
                sessionId = sessionId,
                status = status
            )
        }

        cancelStatusPendingIntent(packageName)
        clearInstallNotificationOnComplete()
    }
}
