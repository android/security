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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "notification-channel"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    fun createInstallNotification() {
        createInstallChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.install_notification_title))
            .setContentText(context.getString(R.string.install_notification_description))
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_dialog_info)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createInstallChannel() {
        var notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)

        if (notificationChannel == null) {
            val channelName = context.getString(R.string.install_channel_name)
            notificationChannel = NotificationChannel(
                CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    /*
    Steps to reproduce:
    1. App running, move to background.
    2. Run background update job.
    3. Launch app from launcher.
    Result: See double prompt for one of the apps.
    To fix this: have tried clearing the internal list when posting the notification.
    Another problem now:
    1. With app visible, tap both buttons, and move app to background.
    2. Tap notification.
    3. Only see one prompt. The pending intent only contains one intent. Isn't being updated somehow? Does updatign work with FLAG_ONE_SHOT?
     */
    fun cancelInstallNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
