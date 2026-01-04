plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Protobuf for parsing descriptors
    api("com.google.protobuf:protobuf-java:3.24.0")

    // JavaPoet for code generation
    api("com.squareup:javapoet:1.13.0")

    // CLI framework
    implementation("info.picocli:picocli:4.7.5")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}

// CLI executable JAR
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("cli")
    manifest {
        attributes["Main-Class"] = "space.alnovis.protowrapper.cli.SchemaDiffCli"
    }
    mergeServiceFiles()
}

// Build CLI JAR as part of assemble
tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

// Publishing to Maven Local for local development
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
