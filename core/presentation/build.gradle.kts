import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidLibrary)
    alias(libs.plugins.anrealCompose)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.core.presentation"
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
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(project(":core:design-system"))
    }
}
