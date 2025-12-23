pluginManagement {
    // Include the plugin from the parent project for development/testing
    // This uses Gradle's composite builds feature for plugin resolution
    includeBuild("../..") {
        name = "proto-wrapper-plugin"
    }
}

rootProject.name = "gradle-example"
