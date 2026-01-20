plugins {
    base
    kotlin("jvm") version "2.1.20" apply false
}

allprojects {
    group = "io.alnovis"
    version = "2.0.0"

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

// Maven tasks for modules not included in Gradle build
tasks.register<Exec>("mavenInstallParent") {
    description = "Install parent POM to local repository"
    group = "maven"
    workingDir = projectDir
    commandLine("mvn", "install", "-B", "-N")
}

tasks.register<Exec>("mavenInstall") {
    description = "Install Maven modules to local repository"
    group = "maven"
    workingDir = projectDir
    commandLine("mvn", "install", "-B", "-pl", "proto-wrapper-core,proto-wrapper-maven-plugin", "-DskipTests")
    dependsOn("mavenInstallParent")
}

tasks.register<Exec>("mavenIntegrationTest") {
    description = "Run Maven integration tests"
    group = "verification"
    workingDir = file("proto-wrapper-maven-integration-tests")
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
    dependsOn("test", "mavenIntegrationTest")
}

tasks.register("buildAll") {
    description = "Build everything (Gradle modules + Maven modules + examples)"
    group = "build"
    dependsOn("build", "mavenIntegrationTest", "mavenExample")
}
