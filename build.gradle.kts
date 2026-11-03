// Top-level build file. Plugins are declared here and applied in :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// The project lives inside OneDrive. Build outputs are redirected outside the synced
// folder (%LOCALAPPDATA%\VrajKhetiBuild\<project>) so OneDrive never locks build files.
// Falls back to the default <project>/build when LOCALAPPDATA is not set (CI, macOS, Linux).
val externalBuildRoot: String? = System.getenv("LOCALAPPDATA")
allprojects {
    if (externalBuildRoot != null) {
        layout.buildDirectory.set(file("$externalBuildRoot/VrajKhetiBuild/${project.name}"))
    }
}
