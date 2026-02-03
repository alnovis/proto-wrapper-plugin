plugins {
    base
    kotlin("jvm") version "2.1.20" apply false
}

// Read version from VERSION file (single source of truth)
val projectVersion = file("VERSION").readText().trim()

allprojects {
    group = "io.alnovis"
    version = projectVersion

    repositories {
        mavenCentral()
        mavenLocal() // For proto-wrapper-core from Maven during development
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

// Note: Maven modules are built separately via build.sh
// This Gradle build only handles:
//   - proto-wrapper-gradle-plugin
//   - (future) proto-wrapper-codegen-kotlin
