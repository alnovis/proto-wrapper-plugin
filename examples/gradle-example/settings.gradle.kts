pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://repo1.maven.org/maven2/")
            name = "MavenCentralMirror"
        }
    }
    // Include the plugin from the parent project for development/testing
    // This uses Gradle's composite builds feature for plugin resolution
    includeBuild("../..") {
        name = "proto-wrapper-plugin"
    }
}

rootProject.name = "gradle-example"
