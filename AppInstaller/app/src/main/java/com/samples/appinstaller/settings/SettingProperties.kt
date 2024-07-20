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

import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import java.time.Duration

fun AutoUpdateSchedule.toDuration(): Duration {
    return when (this) {
        AutoUpdateSchedule.UNRECOGNIZED,
        AutoUpdateSchedule.MANUAL -> Duration.ZERO
        AutoUpdateSchedule.EVERY_15_MINUTES -> Duration.ofMinutes(15)
        AutoUpdateSchedule.EVERY_30_MINUTES -> Duration.ofMinutes(30)
        AutoUpdateSchedule.EVERY_60_MINUTES -> Duration.ofMinutes(60)
    }
}

fun UpdateAvailabilityPeriod.toDuration(): Duration {
    return when (this) {
        UpdateAvailabilityPeriod.UNRECOGNIZED,
        UpdateAvailabilityPeriod.NONE -> Duration.ZERO
        UpdateAvailabilityPeriod.AFTER_30_SECONDS -> Duration.ofSeconds(30)
        UpdateAvailabilityPeriod.AFTER_5_MINUTES -> Duration.ofMinutes(5)
        UpdateAvailabilityPeriod.AFTER_15_MINUTES -> Duration.ofMinutes(15)
        UpdateAvailabilityPeriod.AFTER_30_MINUTES -> Duration.ofMinutes(30)
        UpdateAvailabilityPeriod.AFTER_60_MINUTES -> Duration.ofMinutes(60)
    }
}
