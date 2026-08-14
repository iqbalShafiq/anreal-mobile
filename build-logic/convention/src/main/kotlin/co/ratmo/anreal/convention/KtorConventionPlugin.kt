package co.ratmo.anreal.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KtorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("ktor-client-core").get())
                    implementation(libs.findLibrary("ktor-client-content-negotiation").get())
                    implementation(libs.findLibrary("ktor-client-logging").get())
                    implementation(libs.findLibrary("ktor-client-auth").get())
                    implementation(libs.findLibrary("ktor-serialization-kotlinx-json").get())
                    implementation(libs.findLibrary("kotlinx-serialization-json").get())
                }
                sourceSets.androidMain.dependencies {
                    implementation(libs.findLibrary("ktor-client-okhttp").get())
                }
                sourceSets.iosMain.dependencies {
                    implementation(libs.findLibrary("ktor-client-darwin").get())
                }
            }
        }
    }
}
