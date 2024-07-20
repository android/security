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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.samples.appinstaller.store.PackageName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface PackageInstallerDao {
    @Query("SELECT * FROM package_actions ORDER BY created_at DESC")
    fun getAllActions(): Flow<List<PackageAction>>

    /**
     * Get most recent action by [PackageName]
     */
    fun getActionsByPackage(): Flow<Map<PackageName, PackageAction>> {
        return getAllActions().map { actions -> actions.map { it.packageName to it }.toMap() }
    }

    /**
     * Get actions older created earlier than a defined time
     */
    @Query("SELECT * FROM package_actions WHERE status = 'INITIALIZED' AND created_at < :currentTime")
    suspend fun getObsoleteActions(currentTime: Long): List<PackageAction>

    /**
     * Get [PackageAction] only if they're set to PENDING_USER_ACTION
     */
    @Query("SELECT * FROM package_actions WHERE status = 'PENDING_USER_ACTION' ORDER BY created_at ASC")
    suspend fun getPendingUserActions(): List<PackageAction>

    /**
     * Get [PackageAction] only if it's set to PENDING_USER_ACTION
     */
    @Query("SELECT * FROM package_actions WHERE packageName = :packageName")
    suspend fun getPackageAction(packageName: PackageName): PackageAction?

    suspend fun getPackageAction(packageName: PackageName, status: ActionStatus): PackageAction? {
        val action = getPackageAction(packageName)

        return if (action?.status == status) action else null
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PackageAction)

    @Insert
    suspend fun insertLog(log: PackageActionLog)

    /**
     * Add [PackageAction] to the database to update the UI and track event changes in logs
     */
    @Transaction
    suspend fun addAction(
        packageName: PackageName,
        type: ActionType,
        sessionId: Int? = null,
        status: ActionStatus,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val packageAction = PackageAction(
            packageName = packageName,
            type = type,
            sessionId = sessionId,
            status = status,
            createdAt = createdAt
        )

        insertAction(packageAction)
        insertLog(packageAction.toLog())
    }

    @Query("SELECT * FROM action_logs ORDER BY created_at DESC")
    fun getAllLogs(): Flow<List<PackageActionLog>>
}
