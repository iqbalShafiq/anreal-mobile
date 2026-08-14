package co.ratmo.anreal.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val jvmTarget11: JvmTarget = JvmTarget.JVM_11

internal fun Project.intVersion(alias: String): Int =
    libs.findVersion(alias).get().requiredVersion.toInt()

internal fun Project.resolveBaseUrl(): String {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        val properties = Properties()
        localFile.inputStream().use(properties::load)
        properties.getProperty("anreal.baseUrl")?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return findProperty("anreal.baseUrl") as String? ?: "http://127.0.0.1:3001"
}
