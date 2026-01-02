pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "proto-wrapper"

// Модули для Gradle build
include("proto-wrapper-core")
include("proto-wrapper-gradle-plugin")

// Maven-only модули НЕ включаем в Gradle build:
// - proto-wrapper-maven-plugin (зависит от Maven Plugin API)
// - proto-wrapper-integration-tests (Maven-специфичные тесты)
// - examples/maven-example

// Gradle example как composite build (для сборки из корня)
includeBuild("examples/gradle-example")
