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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appSettings = context.appSettings

    suspend fun setAutoUpdateSchedule(value: Int) {
        context.appSettings.updateData { currentSettings ->
            currentSettings.toBuilder().setAutoUpdateScheduleValue(value).build()
        }
    }

    suspend fun setUpdateAvailabilityPeriod(value: Int) {
        context.appSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setUpdateAvailabilityPeriodValue(value)
                .build()
        }
    }
}
