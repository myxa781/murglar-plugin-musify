plugins {
    alias(catalog.plugins.murglar.plugin.core)
}

murglarPlugin {
    id = "musify"
    name = "Musify"
    version = catalog.versions.murglar.musify.map(String::toInt)
    entryPointClass = "com.badmanners.murglar.lib.musify.MusifyMurglar"
}
