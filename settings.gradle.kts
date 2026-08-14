rootProject.name = "anreal-mobile"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":app")
include(":shared")
include(":core:domain")
include(":core:data")
include(":core:presentation")
include(":core:design-system")
include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")
include(":core:database")
include(":feature:chat:domain")
include(":feature:chat:data")
include(":feature:chat:presentation")