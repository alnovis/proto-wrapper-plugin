plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    // For local development, use project dependency:
    implementation(project(":proto-wrapper-core"))

    // For publishing, use Maven Central dependency (uncomment when publishing):
    // implementation("space.alnovis:proto-wrapper-core:${project.version}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
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
            id = "space.alnovis.proto-wrapper"
            displayName = "Proto Wrapper Plugin"
            description = """
                Generates version-agnostic Java wrapper classes from multiple protobuf schema versions.
                Features: automatic schema merging, type conflict resolution, Builder pattern support,
                VersionContext for runtime version selection.
            """.trimIndent()
            implementationClass = "space.alnovis.protowrapper.gradle.ProtoWrapperPlugin"
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
                        name.set("Alnovis")
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
        // Uncomment for Maven Central publishing:
        // maven {
        //     name = "OSSRH"
        //     url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        //     credentials {
        //         username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
        //         password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
        //     }
        // }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Task to switch between local and published dependency
tasks.register("useLocalCore") {
    doLast {
        println("""
            To use local proto-wrapper-core during development:
            1. Comment out: implementation("space.alnovis:proto-wrapper-core:...")
            2. Uncomment: implementation(project(":proto-wrapper-core"))
            3. Run: ./gradlew build
        """.trimIndent())
    }
}
