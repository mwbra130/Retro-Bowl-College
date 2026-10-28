dependencies {
    compileOnly(project(":extensions:strava:stub"))
}

extension {
    name = "extensions/strava.mpe"
}

android {
    // Unique per extension to avoid install-time package collisions.
    namespace = "app.template.extension.strava"
}
