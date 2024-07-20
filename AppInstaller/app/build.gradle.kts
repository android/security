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

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.protobuf") version "0.8.12"
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.samples.appinstaller"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidxCore.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.toolingPreview)
    implementation(libs.compose.icons)
    implementation(libs.lifecycle.runtimeKtx)
    implementation(libs.lifecycle.livedataKtx)
    implementation(libs.lifecycle.process)
    implementation(libs.navigation.compose)

    implementation(libs.protobuf.javalite)
    implementation(libs.datastore)

    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.androidCompiler)

    implementation(libs.workmanager.runtimeKtx)
    implementation(libs.hilt.work)
    kapt(libs.hilt.workCompiler)

    implementation(libs.logcat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTest.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.junit)
    debugImplementation(libs.compose.tooling)
}

kapt {
    correctErrorTypes = true
}

protobuf {
    protoc {
        artifact =
            "${libs.protobuf.protoc.get().module}:${libs.protobuf.protoc.get().versionConstraint}"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}