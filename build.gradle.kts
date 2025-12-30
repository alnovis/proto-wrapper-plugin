plugins {
    base
    kotlin("jvm") version "1.9.22" apply false
}

allprojects {
    group = "space.alnovis"
    version = "1.2.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal() // Для proto-wrapper-core из Maven при разработке
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

// Maven tasks for modules not included in Gradle build
tasks.register<Exec>("mavenInstall") {
    description = "Install Maven modules to local repository"
    group = "maven"
    workingDir = projectDir
    commandLine("mvn", "install", "-B", "-N")
    doLast {
        exec {
            workingDir = projectDir
            commandLine("mvn", "install", "-B", "-pl", "proto-wrapper-core,proto-wrapper-maven-plugin", "-DskipTests")
        }
    }
}

tasks.register<Exec>("integrationTest") {
    description = "Run Maven integration tests"
    group = "verification"
    workingDir = file("proto-wrapper-integration-tests")
    commandLine("mvn", "verify", "-B")
    dependsOn("mavenInstall")
}

tasks.register<Exec>("mavenExample") {
    description = "Build Maven example project"
    group = "verification"
    workingDir = file("examples/maven-example")
    commandLine("mvn", "verify", "-B")
    dependsOn("mavenInstall")
}

tasks.register("allTests") {
    description = "Run all tests (Gradle + Maven integration tests)"
    group = "verification"
    dependsOn("test", "integrationTest")
}

tasks.register("buildAll") {
    description = "Build everything (Gradle modules + Maven modules + examples)"
    group = "build"
    dependsOn("build", "integrationTest", "mavenExample")
}
