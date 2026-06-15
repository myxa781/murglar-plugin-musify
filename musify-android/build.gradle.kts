plugins {
    alias(catalog.plugins.murglar.plugin.android)
}

murglarAndroidPlugin {
    id = "musify"
    name = "Musify"
    version = catalog.versions.murglar.musify.map(String::toInt)
    entryPointClass = "com.badmanners.murglar.lib.musify.MusifyMurglar"
}

dependencies {
    implementation(project(":musify-core"))
}

android {
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
