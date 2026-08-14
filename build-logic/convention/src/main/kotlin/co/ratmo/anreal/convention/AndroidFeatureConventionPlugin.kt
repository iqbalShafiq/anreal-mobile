package co.ratmo.anreal.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("anreal.android.library")
            pluginManager.apply("anreal.compose")
            pluginManager.apply("anreal.koin")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":core:domain"))
                    implementation(project(":core:presentation"))
                    implementation(project(":core:design-system"))
                }
            }
        }
    }
}
