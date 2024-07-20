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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samples.appinstaller.installer.LogsScreen
import com.samples.appinstaller.settings.SettingsScreen
import com.samples.appinstaller.store.StoreScreen

@Composable
fun Router(viewModel: AppViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Store.id
    ) {
        composable(Route.Store.id) { StoreScreen(navController, viewModel) }
        composable(Route.Settings.id) { SettingsScreen(navController, viewModel) }
        composable(Route.Logs.id) { LogsScreen(navController, viewModel) }
    }
}

sealed class Route(val id: String) {
    object Store : Route("store") {
        val icon = Icons.Filled.Home
        const val title = "Demo Store"
    }

    object Settings : Route("settings") {
        val icon = Icons.Filled.Settings
        const val title = "Settings"
    }

    object Logs : Route("logs") {
        val icon = Icons.Filled.Settings
        const val title = "Logs"
    }
}
