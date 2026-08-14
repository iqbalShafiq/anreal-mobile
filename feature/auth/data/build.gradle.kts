import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealAndroidLibrary)
    alias(libs.plugins.anrealKtor)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.feature.auth.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(project(":core:data"))
        implementation(project(":feature:auth:domain"))
        implementation(libs.koin.core)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.ktor.client.mock)
    }
}
