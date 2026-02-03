plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    // proto-wrapper-core is built with Maven and installed to mavenLocal()
    implementation("io.alnovis:proto-wrapper-core:${project.version}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation(gradleTestKit())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

gradlePlugin {
    website.set("https://github.com/alnovis/proto-wrapper-plugin")
    vcsUrl.set("https://github.com/alnovis/proto-wrapper-plugin")

    plugins {
        create("protoWrapper") {
            id = "io.alnovis.proto-wrapper"
            displayName = "Proto Wrapper Plugin"
            description = """
                Generates version-agnostic Java wrapper classes from multiple protobuf schema versions.
                Features: automatic schema merging, type conflict resolution, Builder pattern support,
                VersionContext for runtime version selection.
            """.trimIndent()
            implementationClass = "io.alnovis.protowrapper.gradle.ProtoWrapperPlugin"
            tags.set(listOf(
                "protobuf",
                "proto",
                "code-generation",
                "wrapper",
                "versioning",
                "grpc",
                "schema-evolution"
            ))
        }
    }
}

// Publishing to Maven Central (in addition to Gradle Plugin Portal)
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Proto Wrapper Gradle Plugin")
                description.set("Gradle plugin for generating version-agnostic Java wrappers from multiple protobuf schema versions")
                url.set("https://github.com/alnovis/proto-wrapper-plugin")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("alnovis")
                        name.set("Alexander Novikov")
                        email.set("dev@alnovis.space")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/alnovis/proto-wrapper-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com/alnovis/proto-wrapper-plugin.git")
                    url.set("https://github.com/alnovis/proto-wrapper-plugin")
                }
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("slow")
    }
}

// Slow tests (TestKit-based) - run separately with parallel execution
tasks.register<Test>("slowTest") {
    description = "Run slow TestKit-based tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("slow")
    }

    // Parallel execution for TestKit tests
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // Each test gets its own forked JVM (required for TestKit isolation)
    forkEvery = 1

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}

// Run all tests including slow ones
tasks.register("allTests") {
    description = "Run all tests including slow TestKit tests"
    group = "verification"
    dependsOn("test", "slowTest")
}

