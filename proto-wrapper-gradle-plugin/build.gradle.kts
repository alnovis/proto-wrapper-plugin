plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":proto-wrapper-core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation(gradleTestKit())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    plugins {
        create("protoWrapper") {
            id = "space.alnovis.proto-wrapper"
            displayName = "Proto Wrapper Plugin"
            description = "Generates version-agnostic Java wrappers from multiple protobuf schema versions"
            implementationClass = "space.alnovis.protowrapper.gradle.ProtoWrapperPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
