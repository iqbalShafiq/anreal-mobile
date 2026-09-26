import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidLibrary)
    alias(libs.plugins.anrealCompose)
}

compose.resources {
    packageOfResClass = "co.ratmo.anreal.core.designsystem.resources"
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
        implementation(libs.markdown.renderer)
        implementation(libs.markdown.renderer.m3)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.junit)
        implementation(libs.robolectric)
        implementation(libs.androidx.compose.ui.test.junit4)
        implementation(libs.androidx.compose.ui.test.manifest)
    }
}
