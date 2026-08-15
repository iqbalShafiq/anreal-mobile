plugins {
    alias(libs.plugins.anrealAndroidApplication)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":feature:auth:data"))
    implementation(project(":feature:auth:presentation"))
    implementation(project(":feature:chat:data"))
    implementation(project(":feature:chat:presentation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
