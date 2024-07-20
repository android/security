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
package com.samples.appinstaller.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.R
import com.samples.appinstaller.Route

@Composable
fun SettingsScreen(navController: NavController, viewModel: AppViewModel) {
    val settings by viewModel.appSettings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = { Icon(Route.Store.icon, contentDescription = null) },
                    label = { Text(Route.Store.title) },
                    selected = false,
                    onClick = {
                        navController.navigate(Route.Store.id)
                    }
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
                    selected = true,
                    onClick = {}
                )
            }
        },
        content = { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_update_title),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.autoupdate_settings_description),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(24.dp))
                SettingItem(
                    stringResource(R.string.auto_update_schedule_label),
                    stringArrayResource(R.array.auto_update_schedule)[settings.autoUpdateScheduleValue],
                    stringArrayResource(R.array.auto_update_schedule)
                ) { value -> viewModel.setAutoUpdateSchedule(value) }

                Spacer(Modifier.height(16.dp))
                SettingItem(
                    stringResource(R.string.update_availability_period_label),
                    stringArrayResource(R.array.update_availability_period)[settings.updateAvailabilityPeriodValue],
                    stringArrayResource(R.array.update_availability_period)
                ) { value -> viewModel.setUpdateAvailabilityPeriod(value) }

                Spacer(Modifier.height(32.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = { /*TODO*/ }
                ) {
                    Text("Trigger auto-updating manually")
                }
            }
        }
    )
}

@Composable
fun SettingItem(
    settingLabel: String,
    valueLabel: String,
    items: Array<String>,
    onClick: (value: Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable(onClick = { isExpanded = !isExpanded }),
        text = {
            Column {
                Text(
                    text = settingLabel,
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.caption
                )
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    items.forEachIndexed { value, label ->
                        DropdownMenuItem(onClick = { onClick(value); isExpanded = false }) {
                            Text(label)
                        }
                    }
                }
            }
        }
    )
}
