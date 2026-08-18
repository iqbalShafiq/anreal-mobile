import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidFeature)
    alias(libs.plugins.anrealSerialization)
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.feature.auth.presentation"
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
        implementation(project(":feature:auth:domain"))
        implementation(libs.androidx.navigation.compose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.icons.material.symbols.rounded)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine)
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.androidx.compose.ui.test.junit4)
        implementation(libs.androidx.compose.ui.test.manifest)
        implementation(libs.junit)
        implementation(libs.robolectric)
        implementation(libs.roborazzi)
        implementation(libs.roborazzi.compose)
    }
}
