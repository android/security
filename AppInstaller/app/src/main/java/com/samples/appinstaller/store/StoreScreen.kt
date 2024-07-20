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
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ListItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.BuildConfig
import com.samples.appinstaller.R
import com.samples.appinstaller.Route

@Composable
fun StoreScreen(navController: NavController, viewModel: AppViewModel) {
    val appsMap by viewModel.apps.collectAsState(sortedMapOf())

    val apps by remember(appsMap) { derivedStateOf { appsMap.values.toList() } }
    var openPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun checkPermission(callback: () -> Unit) {
        if (!viewModel.canInstallPackages) {
            openPermissionDialog = true
        } else {
            callback()
        }
    }

    /**
     * Upgrading is following the same process as install. In this case, we re-install with the same
     * APK but in a production app, we would fetch a newer version of the app APK and do the same
     * steps as an install
     */
    fun upgrade(app: AppPackage) = checkPermission { viewModel.installApp(app.packageName) }
    fun install(app: AppPackage) = checkPermission { viewModel.installApp(app.packageName) }
    fun uninstall(app: AppPackage) = checkPermission { viewModel.uninstallApp(app.packageName) }
    fun open(app: AppPackage) = viewModel.openApp(app.packageName)
    fun cancel(app: AppPackage) = viewModel.cancelInstall(app.packageName)

    if (openPermissionDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.authorization_dialog_title)) },
            text = { Text(stringResource(R.string.authorization_dialog_description)) },
            confirmButton = {
                TextButton(onClick = { openPermissionDialog = false; requestPermission(context) }) {
                    Text(stringResource(R.string.authorization_dialog_confirm_label))
                }
            },
            onDismissRequest = { openPermissionDialog = false },
            dismissButton = {
                TextButton(onClick = { openLearnMoreLink(context) }) {
                    Text(stringResource(R.string.authorization_dialog_learn_more_label))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh_library))
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = { Icon(Route.Store.icon, contentDescription = null) },
                    label = { Text(Route.Store.title) },
                    selected = true,
                    onClick = {}
                )
                BottomNavigationItem(
                    icon = { Icon(Route.Logs.icon, contentDescription = null) },
                    label = { Text(Route.Logs.title) },
                    selected = false,
                    onClick = {
                        navController.navigate(Route.Logs.id)
                    }
                )
                BottomNavigationItem(
                    icon = { Icon(Route.Settings.icon, contentDescription = null) },
                    label = { Text(Route.Settings.title) },
                    selected = false,
                    onClick = {
                        navController.navigate(Route.Settings.id)
                    }
                )
            }
        },
        content = { innerPadding ->
            LazyColumn(Modifier.padding(innerPadding)) {
                items(apps) { app ->
                    AppItem(app, ::install, ::upgrade, ::uninstall, ::open, ::cancel)
                }
            }
        }
    )
}

@Composable
fun AppItem(
    app: AppPackage,
    onInstall: (app: AppPackage) -> Unit,
    onUpgrade: (app: AppPackage) -> Unit,
    onUninstall: (app: AppPackage) -> Unit,
    onOpen: (app: AppPackage) -> Unit,
    onCancel: (app: AppPackage) -> Unit
) {
    ListItem(
        text = { Text(app.label) },
        overlineText = { Text(app.company) },
        secondaryText = {
            Column {
                if (app.updatedAt > -1) {
                    val installationPeriod = DateUtils.getRelativeTimeSpanString(app.updatedAt)
                    Text(stringResource(R.string.install_time_label, installationPeriod))
                } else {
                    Text(stringResource(R.string.not_installed_label))
                }

                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth()) {
                    when (app.status) {
                        AppStatus.UNINSTALLING -> {
                            Spacer(Modifier.width(5.dp))
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = false,
                                onClick = { /*TODO*/ }
                            ) {
                                Text(stringResource(R.string.cancel_label))
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), enabled = false, onClick = { }) {
                                Text(stringResource(R.string.uninstalling_label))
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                        AppStatus.UNINSTALLED -> {
                            Button(modifier = Modifier.weight(1f), onClick = { onInstall(app) }) {
                                Text(stringResource(R.string.install_label))
                            }
                        }
                        AppStatus.INSTALLING -> {
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onCancel(app) }) {
                                Text(stringResource(R.string.cancel_label))
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), enabled = false, onClick = { }) {
                                Text(stringResource(R.string.installing_label))
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                        AppStatus.INSTALLED -> {
                            Column(Modifier.fillMaxWidth()) {
                                Row {
                                    Spacer(Modifier.width(5.dp))
                                    Button(modifier = Modifier.weight(1f), onClick = { onUninstall(app) }) {
                                        Text(stringResource(R.string.uninstall_label))
                                    }
                                    Spacer(Modifier.width(5.dp))
                                    Button(modifier = Modifier.weight(1f), onClick = { onOpen(app) }) {
                                        Text(stringResource(R.string.open_label))
                                    }
                                    Spacer(Modifier.width(5.dp))
                                }
                                Spacer(Modifier.height(5.dp))
                                Row {
                                    Spacer(Modifier.width(5.dp))
                                    OutlinedButton(modifier = Modifier.weight(1f), onClick = { onUpgrade(app) }) {
                                        Text(stringResource(R.string.upgrade_label))
                                    }
                                    Spacer(Modifier.width(5.dp))
                                }
                            }
                        }
                        AppStatus.UPGRADING -> {
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), onClick = { onCancel(app) }) {
                                Text(stringResource(R.string.cancel_label))
                            }
                            Spacer(Modifier.width(5.dp))
                            Button(modifier = Modifier.weight(1f), enabled = false, onClick = { }) {
                                Text(stringResource(R.string.upgrading_label))
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        },
        icon = {
            Image(
                painter = painterResource(app.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3DDC84))
            )
        }

    )
    if (app.status == AppStatus.INSTALLING || app.status == AppStatus.UPGRADING) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Divider()
}

private fun requestPermission(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
    }

    context.startActivity(intent)
}

private fun openLearnMoreLink(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data =
            Uri.parse("https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller")
    }

    context.startActivity(intent)
}
