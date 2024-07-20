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

import android.content.pm.PackageInstaller
import android.os.Handler
import com.samples.appinstaller.store.PackageName
import logcat.LogPriority
import logcat.logcat

/**
 * This observer is a workaround used to track dismissed (not cancelled) user actions from
 * [PackageInstaller] as they don't send a callback to [SessionStatusReceiver]
 */
class SessionActionObserver(
    private val packageInstaller: PackageInstaller,
    val packageName: PackageName,
    val trackedSessionId: Int
) : PackageInstaller.SessionCallback() {

    companion object {
        private const val CANCEL_TIMEOUT_MILLIS: Long = 1000
    }

    private var actionObserved = false
    private val handler: Handler = Handler()

    init {
        observe()
    }

    private fun observe() {
        packageInstaller.registerSessionCallback(this)
    }

    fun stop() {
        packageInstaller.unregisterSessionCallback(this)
    }

    fun startCancellationTimeout() {
        if (actionObserved) return
        handler.postDelayed(
            {
                if (!actionObserved) {
                    logcat { "Abandoning session: $trackedSessionId" }

                    try {
                        packageInstaller.abandonSession(trackedSessionId)
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR) { e.toString() }
                    }
                }
                packageInstaller.unregisterSessionCallback(this)
            },
            CANCEL_TIMEOUT_MILLIS
        )
    }

    private fun onActionObserved(sessionId: Int) {
        if (sessionId == trackedSessionId) {
            actionObserved = true
            packageInstaller.unregisterSessionCallback(this)
        }
    }

    override fun onCreated(sessionId: Int) {}
    override fun onBadgingChanged(sessionId: Int) {}
    override fun onActiveChanged(sessionId: Int, active: Boolean) {}
    override fun onProgressChanged(sessionId: Int, progress: Float) {
        onActionObserved(sessionId)
    }

    override fun onFinished(sessionId: Int, success: Boolean) {
        onActionObserved(sessionId)
    }
}
