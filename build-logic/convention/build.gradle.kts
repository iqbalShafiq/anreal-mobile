plugins {
    `kotlin-dsl`
}

group = "co.ratmo.anreal.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "anreal.android.application"
            implementationClass = "co.ratmo.anreal.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "anreal.android.library"
            implementationClass = "co.ratmo.anreal.convention.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "anreal.android.feature"
            implementationClass = "co.ratmo.anreal.convention.AndroidFeatureConventionPlugin"
        }
        register("domainModule") {
            id = "anreal.domain"
            implementationClass = "co.ratmo.anreal.convention.DomainModuleConventionPlugin"
        }
        register("compose") {
            id = "anreal.compose"
            implementationClass = "co.ratmo.anreal.convention.ComposeConventionPlugin"
        }
        register("koin") {
            id = "anreal.koin"
            implementationClass = "co.ratmo.anreal.convention.KoinConventionPlugin"
        }
        register("ktor") {
            id = "anreal.ktor"
            implementationClass = "co.ratmo.anreal.convention.KtorConventionPlugin"
        }
        register("kotlinxSerialization") {
            id = "anreal.serialization"
            implementationClass = "co.ratmo.anreal.convention.KotlinxSerializationConventionPlugin"
        }
    }
}
