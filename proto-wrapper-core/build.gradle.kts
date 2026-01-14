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
    api("com.google.protobuf:protobuf-java:4.28.2")

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
    useJUnitPlatform {
        // Exclude benchmark tests from regular test runs (CI performance is unpredictable)
        excludeTags("benchmark")
    }
}

// Separate task to run benchmark tests manually
tasks.register<Test>("benchmarkTest") {
    useJUnitPlatform {
        includeTags("benchmark")
    }
    description = "Run performance benchmark tests"
    group = "verification"
}

// Resource filtering for version.properties (similar to Maven resource filtering)
tasks.processResources {
    filesMatching("version.properties") {
        expand(
            "projectVersion" to project.version,
            "projectGroupId" to project.group,
            "projectArtifactId" to project.name
        )
    }
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
