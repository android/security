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
package com.samples.appinstaller.store

import android.content.Context
import android.content.pm.PackageManager
import com.samples.appinstaller.R
import com.samples.appinstaller.database.ActionStatus
import com.samples.appinstaller.database.ActionType
import com.samples.appinstaller.database.PackageInstallerDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This list contains the apps available in our store. In a production app, this list would be
 * fetched from a remote server as it may be updated and not be static like here
 */
private val internalStoreApps = listOf(
    AppPackage(
        packageName = "com.acme.spaceshooter",
        label = "Space Shooter",
        company = "ACME Inc.",
        icon = R.drawable.ic_app_spaceshooter_foreground
    ),
    AppPackage(
        packageName = "com.champollion.pockettranslator",
        label = "Pocket Translator",
        company = "Champollion SA",
        icon = R.drawable.ic_app_pockettranslator_foreground,
    ),
    AppPackage(
        packageName = "com.echolabs.citymaker",
        label = "City Maker",
        company = "Echo Labs Ltd",
        icon = R.drawable.ic_app_citymaker_foreground,
    ),
    AppPackage(
        packageName = "com.paca.nicekart",
        label = "Nice Kart",
        company = "PACA SARL",
        icon = R.drawable.ic_app_nicekart_foreground,
    ),
).associateBy { it.packageName }.toSortedMap()

/**
 * This repository contains the list of the store apps and their status on the current device
 * (installed, uninstalled, etc.) and acts as the source of truth for the UI. In a production app,
 * this list would be saved in a database (using Jetpack Room) as it may be too big to be kept on
 * memory
 */
@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    database: PackageInstallerDao,
) {
    companion object {
        val storeApps = internalStoreApps
    }

    private val packageManager: PackageManager
        get() = context.packageManager

    private val _apps = MutableStateFlow(storeApps)

    /**
     * We combine our store apps list and the installed apps (filtered from apps not being
     * in our store) with the apps being installed or upgraded to get the most up-to-date
     * library state
     */
    val apps = _apps.combine(database.getActionsByPackage()) { currentApps, packageActions ->

        currentApps.mapValues {
            val packageName = it.key
            val app = it.value
            val action = packageActions[packageName]

            when (action?.status) {
                ActionStatus.COMMITTED,
                ActionStatus.INITIALIZED,
                ActionStatus.PENDING_USER_ACTION -> {
                    when (action.type) {
                        ActionType.INSTALL -> app.copy(status = AppStatus.INSTALLING)
                        ActionType.UNINSTALL -> app.copy(status = AppStatus.UNINSTALLING)
                    }
                }

                ActionStatus.SUCCESS,
                ActionStatus.FAILURE,
                ActionStatus.UNKNOWN,
                ActionStatus.CANCELLATION,
                null -> {
                    if (action?.status == ActionStatus.SUCCESS && action.type == ActionType.UNINSTALL) {
                        app.copy(status = AppStatus.UNINSTALLED, updatedAt = -1)
                    } else {

                        val installTime = getPackageInstallTime(app.packageName)

                        if (installTime > -1) {
                            app.copy(status = AppStatus.INSTALLED, updatedAt = installTime)
                        } else {
                            app
                        }
                    }
                }
            }
        }.toSortedMap()
    }

    init {
        runBlocking {
            _apps.emit(storeApps)
        }
    }

    /**
     * We load the library by listing our store apps list and updating their status if they're
     * installed or being installed (uninstalled by default if we can't find them on the device)
     */
    suspend fun loadLibrary() {
        withContext(Dispatchers.IO) {
            val installedPackages =
                packageManager.getInstalledPackages(0).associateBy { it.packageName }

            /**
             * We combine our store apps list and the installed apps (filtered from apps not being
             * in our store)
             */
            val updatedApps = storeApps.mapValues {
                val packageName = it.key
                val app = it.value

                if (installedPackages[packageName] != null) {
                    app.copy(
                        status = AppStatus.INSTALLED,
                        updatedAt = installedPackages[packageName]!!.lastUpdateTime
                    )
                } else {
                    app
                }
            }.toSortedMap()

            _apps.emit(updatedApps)
        }
    }

    private fun getPackageInstallTime(packageName: PackageName): Long {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
}
