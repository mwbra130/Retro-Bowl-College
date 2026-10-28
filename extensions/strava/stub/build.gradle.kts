plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.template.extension.strava.stub"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}
