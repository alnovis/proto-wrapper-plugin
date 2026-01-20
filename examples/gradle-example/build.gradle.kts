plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    id("io.alnovis.proto-wrapper")
}

group = "com.example"
version = "1.0.0"

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

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

// Configure proto source set
// All proto files (v1 and v2) are compiled together
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
    // Base package for all generated code
    basePackage.set("com.example.model")

    // Pattern for proto package (used to resolve proto class names)
    protoPackagePattern.set("com.example.proto.{version}")

    // Root directory containing version subdirectories
    protoRoot.set(file("proto"))

    // Whether to add version suffix to implementation class names
    // false: Money, Money (differentiated by package)
    // true: MoneyV1, MoneyV2
    includeVersionSuffix.set(false)

    // Generate Builder interfaces for modifying wrapper objects
    generateBuilders.set(true)

    // Version configurations
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
