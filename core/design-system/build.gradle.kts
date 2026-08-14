import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidLibrary)
    alias(libs.plugins.anrealCompose)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.core.designsystem"
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
        implementation(libs.materialKolor)
        implementation(libs.haze)
        implementation(libs.haze.materials)
        implementation(libs.icons.material.symbols.rounded)
    }
}
