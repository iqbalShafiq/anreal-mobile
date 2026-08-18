import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidLibrary)
    alias(libs.plugins.anrealCompose)
    alias(libs.plugins.anrealSerialization)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(project(":core:presentation"))
        implementation(project(":core:design-system"))
        implementation(project(":feature:auth:domain"))
        implementation(project(":feature:auth:presentation"))
        implementation(project(":feature:chat:presentation"))
        implementation(project(":feature:workspace:presentation"))
        implementation(libs.androidx.navigation.compose)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.androidx.compose.ui.test.junit4)
        implementation(libs.androidx.compose.ui.test.manifest)
        implementation(libs.junit)
        implementation(libs.robolectric)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
