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
    return resolveAnrealProperty("anreal.baseUrl") ?: "http://127.0.0.1:3001"
}

internal fun Project.resolveEnvironment(): String {
    val raw = resolveAnrealProperty("anreal.environment") ?: "development"
    return when (raw.lowercase()) {
        "development", "dev" -> "development"
        "staging", "stage" -> "staging"
        "production", "prod" -> "production"
        else -> error("Unknown anreal.environment=$raw. Use development, staging, or production.")
    }
}

private fun Project.resolveAnrealProperty(key: String): String? {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        val properties = Properties()
        localFile.inputStream().use(properties::load)
        properties.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return (findProperty(key) as String?)?.takeIf { it.isNotBlank() }
}
