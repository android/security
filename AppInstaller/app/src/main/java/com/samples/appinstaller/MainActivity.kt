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
package com.samples.appinstaller

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.samples.appinstaller.installer.SessionActionObserver
import com.samples.appinstaller.ui.theme.AppInstallerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * We redeliver saved used actions from previous interactions and add them to the user
         * actions queue
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.cleanObsoleteSessions()
                viewModel.redeliverSavedUserActions()
            }
        }

        /**
         * We remove the notification if it exists as user actions will be shown inside the app
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.cancelInstallNotification()
            }
        }

        /**
         * When [onResume] is called, we track any active user action, update our library, check if
         * any pending user action is needed to be shown to the user and listen to future events
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.markUserActionComplete()
                requestUserActionIfNeeded()
                viewModel.pendingUserActionEvents.collect { requestUserActionIfNeeded() }
            }
        }

        /**
         * We collect launching app intents from [AppViewModel.appsToBeOpened] flow once the
         * activity is resumed and cancel the collect once it leaves this state
         */
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.appsToBeOpened.collect(::startActivity)
            }
        }

        // We display the app UI
        setContent {
            AppInstallerTheme {
                Router(viewModel)
            }
        }
    }

    /**
     * This method is called before [onResume] when a new intent is received.
     * When our app is re-launched from home screen or notification, because our launch mode is
     * singleTask, any other activity in our task will be finished.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.cancelActiveUserAction()
    }

    /**
     * We show a notification if there are still pending user actions when the app gets stopped
     */
    override fun onStop() {
        viewModel.notifyPendingUserActions()
        super.onStop()
    }

    /**
     * Before launching the intent containing the user confirmation dialog for install/uninstall
     * operation, we start monitoring with a [SessionActionObserver] any changes from the
     * [PackageInstaller]. As there's no way to listen to the dismiss of the dialog (which is
     * different from user cancelling it), we keep track of that specific session and start a timer
     * inside [onResume] with [AppViewModel.markUserActionComplete].
     * Within a second, the [PackageInstaller] should have sent a success/failure intent to
     * [com.samples.appinstaller.installer.SessionStatusReceiver] otherwise it means the user has
     * dismissed the dialog and we cancel the operation and update the UI
     */
    private fun requestUserActionIfNeeded() {
        viewModel.getPendingUserActionFromQueue()?.let { statusIntent ->
            val sessionId = statusIntent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            val packageName = statusIntent.data?.schemeSpecificPart ?: return

            viewModel.initSessionActionObserver(packageName, sessionId)
            startActivity(statusIntent.getParcelableExtra(Intent.EXTRA_INTENT))
        }
    }
}
