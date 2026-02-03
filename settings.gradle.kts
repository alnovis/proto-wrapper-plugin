pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://repo1.maven.org/maven2/")
            name = "MavenCentralMirror"
        }
    }
}

rootProject.name = "proto-wrapper"

// Gradle modules (proto-wrapper-core is Maven-only, built separately)
include("proto-wrapper-gradle-plugin")

// Maven-only modules NOT included in Gradle build:
// - proto-wrapper-maven-plugin (depends on Maven Plugin API)
// - proto-wrapper-maven-integration-tests
// - examples/maven-example

// examples/gradle-example - built separately:
//   cd examples/gradle-example && ./gradlew build
// Its settings.gradle.kts uses includeBuild("../..") for plugin resolution
