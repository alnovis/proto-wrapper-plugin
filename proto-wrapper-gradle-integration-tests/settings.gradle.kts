pluginManagement {
    repositories {
        mavenLocal()  // Plugin published here before tests
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "proto-wrapper-gradle-integration-tests"
