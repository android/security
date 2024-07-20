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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.R
import com.samples.appinstaller.Route
import com.samples.appinstaller.database.ActionStatus

@Composable
fun LogsScreen(navController: NavController, viewModel: AppViewModel) {
    val logs by viewModel.logs.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
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
                    selected = true,
                    onClick = {}
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
                items(logs) { log ->
                    val backgroundColor = when (log.status) {
                        ActionStatus.INITIALIZED -> Color.White
                        ActionStatus.COMMITTED -> Color.LightGray
                        ActionStatus.PENDING_USER_ACTION -> Color.Cyan
                        ActionStatus.SUCCESS -> Color.Green
                        ActionStatus.FAILURE -> Color.Yellow
                        ActionStatus.UNKNOWN -> Color.Magenta
                        ActionStatus.CANCELLATION -> Color.DarkGray
                    }

                    ListItem(
                        modifier = Modifier.background(backgroundColor.copy(alpha = 0.5f)),
                        text = { Text(log.packageName) },
                        secondaryText = { Text(log.toString()) },
                    )
                    Divider()
                }
            }
        }
    )
}
