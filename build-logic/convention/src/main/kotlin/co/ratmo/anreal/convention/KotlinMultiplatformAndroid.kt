package co.ratmo.anreal.convention

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKmpTargets(kmp: KotlinMultiplatformExtension) {
    kmp.apply {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
        iosArm64()
        iosSimulatorArm64()
        sourceSets.commonTest.dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
            implementation(libs.findLibrary("assertk").get())
        }
    }
}
