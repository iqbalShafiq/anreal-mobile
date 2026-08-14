import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.anrealDomain)
}

kotlin {
    android {
        namespace = "co.ratmo.anreal.feature.auth.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
    }
}
