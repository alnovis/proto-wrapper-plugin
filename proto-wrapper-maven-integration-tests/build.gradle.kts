plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    id("io.alnovis.proto-wrapper")
}

group = "io.alnovis"
version = "1.6.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Protobuf runtime
    implementation("com.google.protobuf:protobuf-java:4.28.2")

    // Proto wrapper core (for exception classes in tests)
    testImplementation("io.alnovis:proto-wrapper-core:${version}")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

// Configure proto source sets
sourceSets {
    main {
        proto {
            srcDir("proto")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
}

// Configure proto-wrapper plugin
protoWrapper {
    basePackage.set("io.alnovis.protowrapper.it.model")
    protoPackagePattern.set("io.alnovis.protowrapper.it.proto.{version}")
    protoRoot.set(file("proto"))
    includeVersionSuffix.set(false)
    generateBuilders.set(true)
    convertWellKnownTypes.set(true)
    generateRawProtoAccessors.set(false)

    versions {
        create("v1") {
            protoDir.set("v1")
        }
        create("v2") {
            protoDir.set("v2")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
