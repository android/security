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
package com.samples.appinstaller.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samples.appinstaller.store.PackageName

/**
 * Represents an action processed by the Package Installer, it helps to keep track of its progress
 */
@Entity(tableName = "package_actions")
data class PackageAction(
    @PrimaryKey val packageName: PackageName,
    @ColumnInfo(name = "type") val type: ActionType,
    @ColumnInfo(name = "session_id") val sessionId: Int?,
    @ColumnInfo(name = "status") val status: ActionStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toLog() = PackageActionLog(0, packageName, type, sessionId, status, createdAt)
}

/**
 * Immutable entry of a [PackageAction], used for monitoring
 */
@Entity(tableName = "action_logs")
data class PackageActionLog(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "package_name") val packageName: PackageName,
    @ColumnInfo(name = "type") val type: ActionType,
    @ColumnInfo(name = "session_id") val sessionId: Int?,
    @ColumnInfo(name = "status") val status: ActionStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

enum class ActionType {
    INSTALL, UNINSTALL
}

/**
 * Represents possible state of a [PackageAction]
 */
enum class ActionStatus {
    INITIALIZED, COMMITTED, PENDING_USER_ACTION, SUCCESS, FAILURE, UNKNOWN, CANCELLATION
}
