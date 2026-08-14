plugins {
    alias(libs.plugins.anrealAndroidApplication)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
