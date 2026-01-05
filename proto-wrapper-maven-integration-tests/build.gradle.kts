plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    id("space.alnovis.proto-wrapper")
}

group = "space.alnovis"
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
    implementation("com.google.protobuf:protobuf-java:3.24.0")

    // Proto wrapper core (for exception classes in tests)
    testImplementation("space.alnovis:proto-wrapper-core:${version}")

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
        artifact = "com.google.protobuf:protoc:3.24.0"
    }
}

// Configure proto-wrapper plugin
protoWrapper {
    basePackage.set("space.alnovis.protowrapper.it.model")
    protoPackagePattern.set("space.alnovis.protowrapper.it.proto.{version}")
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
