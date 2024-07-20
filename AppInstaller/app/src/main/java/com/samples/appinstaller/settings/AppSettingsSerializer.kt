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
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.samples.appinstaller.AppSettings
import java.io.InputStream
import java.io.OutputStream

const val PROTO_STORE_FILE_NAME = "app_settings.pb"

object AppSettingsSerializer : Serializer<AppSettings> {

    override val defaultValue: AppSettings = AppSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppSettings {
        try {
            return AppSettings.parseFrom(input)
        } catch (ipbe: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", ipbe)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) = t.writeTo(output)
}

val Context.appSettings: DataStore<AppSettings> by dataStore(
    fileName = PROTO_STORE_FILE_NAME,
    serializer = AppSettingsSerializer
)
