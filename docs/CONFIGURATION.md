# Configuration Reference

Complete reference for all Proto Wrapper Plugin configuration options for both Maven and Gradle.

---

## Table of Contents

- [Maven Configuration](#maven-configuration)
  - [Plugin Parameters](#plugin-parameters)
  - [Version Configuration](#version-configuration)
  - [Message Filtering](#message-filtering)
  - [Incremental Build](#incremental-build)
- [Gradle Configuration](#gradle-configuration)
  - [Extension Properties](#extension-properties)
  - [Version Configuration](#version-configuration-1)
  - [Message Filtering](#message-filtering-1)
  - [Incremental Build](#incremental-build-1)
- [Common Patterns](#common-patterns)
  - [Multi-Module Projects](#multi-module-projects)
  - [Custom protoc Path](#custom-protoc-path)
  - [CI/CD Integration](#cicd-integration)

---

## Maven Configuration

### Plugin Declaration

```xml
<plugin>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>2.0.0</version>
    <configuration>
        <!-- Configuration options here -->
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Plugin Parameters

#### Required Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `basePackage` | String | Base package for all generated classes. Interfaces go to `{basePackage}.api`, implementations to `{basePackage}.{version}`. |
| `protoRoot` | File | Root directory containing version subdirectories with proto files. |
| `versions` | List | List of version configurations. At least one required. |

#### Optional Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `outputDirectory` | `target/generated-sources/proto-wrapper` | Output directory for generated Java files. |
| `protoPackagePattern` | `{basePackage}.proto.{version}` | Pattern for locating protobuf-generated Java classes. Use `{version}` placeholder. |
| `generateBuilders` | `false` | Generate Builder interfaces for creating/modifying messages. |
| `protobufMajorVersion` | `3` | Protobuf version (2 or 3). Affects enum conversion methods. |
| `includeVersionSuffix` | `true` | Include version suffix in class names (`MoneyV1` vs `Money`). |
| `convertWellKnownTypes` | `true` | Convert Google Well-Known Types to Java types (Timestamp to Instant, etc.). |
| `generateRawProtoAccessors` | `false` | Generate `getXxxProto()` methods for Well-Known Type fields. |
| `protocPath` | (auto) | Path to protoc executable. If not set, resolved automatically: system PATH, then embedded download. |
| `protocVersion` | (from plugin) | Version of protoc for embedded downloads. Only used if system protoc not found. *(since 2.0.0)* |
| `targetJavaVersion` | `9` | Target Java version for generated code. Use `8` for Java 8 compatibility (avoids private interface methods and `List.of()`). *(since 2.0.0)* |

#### Generation Flags

| Parameter | Default | Description |
|-----------|---------|-------------|
| `generateInterfaces` | `true` | Generate version-agnostic interfaces. |
| `generateAbstractClasses` | `true` | Generate abstract base classes with template methods. |
| `generateImplClasses` | `true` | Generate version-specific implementation classes. |
| `generateVersionContext` | `true` | Generate VersionContext factory interface. |

### Version Configuration

Each version entry supports:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `protoDir` | Yes | Directory name relative to `protoRoot` containing proto files. |
| `name` | No | Version name for generated classes. Defaults to uppercase of `protoDir` (e.g., `v1` becomes `V1`). |
| `excludeProtos` | No | List of proto files to exclude from this version. |

#### Example

```xml
<versions>
    <version>
        <protoDir>v1</protoDir>
        <name>V1</name>
        <excludeProtos>
            <excludeProto>internal.proto</excludeProto>
            <excludeProto>deprecated.proto</excludeProto>
        </excludeProtos>
    </version>
    <version>
        <protoDir>v2</protoDir>
        <!-- name defaults to "V2" -->
    </version>
    <version>
        <protoDir>v3-beta</protoDir>
        <name>V3Beta</name>
    </version>
</versions>
```

### Message Filtering

Filter which messages are processed:

```xml
<configuration>
    <!-- Only include these messages (whitelist) -->
    <includeMessages>
        <message>Order</message>
        <message>Customer</message>
        <message>Payment</message>
    </includeMessages>

    <!-- Exclude these messages (blacklist) -->
    <excludeMessages>
        <message>InternalConfig</message>
        <message>DebugInfo</message>
    </excludeMessages>
</configuration>
```

**Rules:**
- If `includeMessages` is specified, only listed messages are processed
- If `excludeMessages` is specified, listed messages are skipped
- Both can be used together: include takes precedence, then exclude filters

### Incremental Build

Configure incremental generation (enabled by default since v1.6.0):

```xml
<configuration>
    <!-- Enable/disable incremental generation -->
    <incremental>true</incremental>

    <!-- Cache directory for state files -->
    <cacheDirectory>${project.build.directory}/proto-wrapper-cache</cacheDirectory>

    <!-- Force full regeneration -->
    <forceRegenerate>false</forceRegenerate>
</configuration>
```

**Command-line overrides:**

```bash
# Force full regeneration
mvn compile -Dproto-wrapper.force=true

# Disable incremental mode
mvn compile -Dproto-wrapper.incremental=false
```

### Full Maven Example

```xml
<plugin>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>2.0.0</version>
    <configuration>
        <!-- Required -->
        <basePackage>com.example.model</basePackage>
        <protoRoot>${basedir}/proto</protoRoot>

        <!-- Proto class location -->
        <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>

        <!-- Output -->
        <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>

        <!-- Features -->
        <generateBuilders>true</generateBuilders>
        <convertWellKnownTypes>true</convertWellKnownTypes>
        <protobufMajorVersion>3</protobufMajorVersion>
        <targetJavaVersion>8</targetJavaVersion> <!-- Use 8 for Java 8 compatibility -->

        <!-- Class naming -->
        <includeVersionSuffix>true</includeVersionSuffix>

        <!-- Versions -->
        <versions>
            <version>
                <protoDir>v1</protoDir>
                <excludeProtos>
                    <excludeProto>internal.proto</excludeProto>
                </excludeProtos>
            </version>
            <version>
                <protoDir>v2</protoDir>
            </version>
        </versions>

        <!-- Filtering -->
        <excludeMessages>
            <message>DebugMessage</message>
        </excludeMessages>

        <!-- Incremental -->
        <incremental>true</incremental>
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## Gradle Configuration

### Plugin Declaration

```kotlin
// build.gradle.kts
plugins {
    id("io.alnovis.proto-wrapper") version "2.0.0"
}
```

Or using legacy plugin application:

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.alnovis:proto-wrapper-gradle-plugin:2.0.0")
    }
}

apply(plugin = "io.alnovis.proto-wrapper")
```

### Extension Properties

Configure via the `protoWrapper` extension:

```kotlin
protoWrapper {
    // Required
    basePackage.set("com.example.model")
    protoRoot.set(file("proto"))

    // Optional
    protoPackagePattern.set("com.example.proto.{version}")
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/proto-wrapper/main/java"))
    generateBuilders.set(true)
    protobufMajorVersion.set(3)
    includeVersionSuffix.set(true)
    convertWellKnownTypes.set(true)
    generateRawProtoAccessors.set(false)

    // Versions
    versions {
        version("v1")
        version("v2")
    }
}
```

#### Required Properties

| Property | Type | Description |
|----------|------|-------------|
| `basePackage` | `Property<String>` | Base package for generated classes. |
| `protoRoot` | `DirectoryProperty` | Root directory with version subdirectories. |

#### Optional Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `outputDirectory` | `DirectoryProperty` | `build/generated/sources/proto-wrapper/main/java` | Output directory. |
| `protoPackagePattern` | `Property<String>` | `{basePackage}.proto.{version}` | Proto class package pattern. |
| `generateBuilders` | `Property<Boolean>` | `false` | Enable Builder generation. |
| `protobufMajorVersion` | `Property<Int>` | `3` | Protobuf version. |
| `includeVersionSuffix` | `Property<Boolean>` | `true` | Version suffix in class names. |
| `convertWellKnownTypes` | `Property<Boolean>` | `true` | Convert WKT to Java types. |
| `generateRawProtoAccessors` | `Property<Boolean>` | `false` | Generate raw proto accessors for WKT. |
| `protocPath` | `Property<String>` | (auto) | Path to protoc. |
| `targetJavaVersion` | `Property<Int>` | `9` | Target Java version (use `8` for Java 8 compatibility). |

### Version Configuration

```kotlin
protoWrapper {
    versions {
        // Simple version
        version("v1")

        // Version with configuration
        version("v2") {
            name.set("V2")
            excludeProtos.set(listOf("internal.proto"))
        }

        // Custom name
        version("v3-beta") {
            name.set("V3Beta")
        }
    }
}
```

### Message Filtering

```kotlin
protoWrapper {
    // Whitelist
    includeMessages.set(listOf("Order", "Customer", "Payment"))

    // Blacklist
    excludeMessages.set(listOf("InternalConfig", "DebugInfo"))
}
```

### Incremental Build

```kotlin
protoWrapper {
    incremental.set(true)
    cacheDirectory.set(layout.buildDirectory.dir("proto-wrapper-cache"))
    forceRegenerate.set(false)
}
```

**Command-line overrides:**

```bash
# Force full regeneration
./gradlew generateProtoWrapper -Pproto-wrapper.force=true

# Clean and regenerate
./gradlew clean generateProtoWrapper
```

### Full Gradle Example

```kotlin
// build.gradle.kts
plugins {
    java
    id("io.alnovis.proto-wrapper") version "2.0.0"
}

protoWrapper {
    // Required
    basePackage.set("com.example.model")
    protoRoot.set(file("proto"))

    // Proto class location
    protoPackagePattern.set("com.example.proto.{version}")

    // Output
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/proto-wrapper/main/java"))

    // Features
    generateBuilders.set(true)
    convertWellKnownTypes.set(true)
    protobufMajorVersion.set(3)
    targetJavaVersion.set(8)  // Use 8 for Java 8 compatibility

    // Class naming
    includeVersionSuffix.set(true)

    // Versions
    versions {
        version("v1") {
            excludeProtos.set(listOf("internal.proto"))
        }
        version("v2")
    }

    // Filtering
    excludeMessages.set(listOf("DebugMessage"))

    // Incremental
    incremental.set(true)
}

// Ensure proto-wrapper runs after protobuf compilation
tasks.named("generateProtoWrapper") {
    dependsOn("generateProto")
}
```

---

## Common Patterns

### Multi-Module Projects

For multi-module projects where proto files are in a separate module:

**Maven:**

```xml
<!-- proto-definitions module -->
<plugin>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <configuration>
        <basePackage>com.example.proto.wrapper</basePackage>
        <protoRoot>${basedir}/src/main/proto</protoRoot>
        <!-- Generated classes go to this module's target -->
    </configuration>
</plugin>

<!-- consumer module -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>proto-definitions</artifactId>
    <version>${project.version}</version>
</dependency>
```

**Gradle:**

```kotlin
// settings.gradle.kts
include("proto-definitions", "app")

// proto-definitions/build.gradle.kts
plugins {
    id("io.alnovis.proto-wrapper")
}

protoWrapper {
    basePackage.set("com.example.proto.wrapper")
    protoRoot.set(file("src/main/proto"))
}

// app/build.gradle.kts
dependencies {
    implementation(project(":proto-definitions"))
}
```

### Custom protoc Path

When protoc is not in PATH:

**Maven:**

```xml
<configuration>
    <protocPath>/opt/protobuf/bin/protoc</protocPath>
</configuration>
```

**Gradle:**

```kotlin
protoWrapper {
    protocPath.set("/opt/protobuf/bin/protoc")
}
```

**Environment variable:**

```bash
export PROTOC=/opt/protobuf/bin/protoc
mvn compile
```

### CI/CD Integration

#### GitHub Actions

```yaml
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'

- name: Install protoc
  uses: arduino/setup-protoc@v3
  with:
    version: '25.x'

- name: Build with Maven
  run: mvn clean verify
```

#### GitLab CI

```yaml
build:
  image: maven:3.9-eclipse-temurin-17
  before_script:
    - apt-get update && apt-get install -y protobuf-compiler
  script:
    - mvn clean verify
```

#### Docker

```dockerfile
FROM maven:3.9-eclipse-temurin-17

RUN apt-get update && apt-get install -y protobuf-compiler

WORKDIR /app
COPY . .
RUN mvn clean package
```

### Disable Incremental for CI

For CI builds, you may want to ensure clean builds:

**Maven:**

```bash
mvn clean compile -Dproto-wrapper.force=true
```

**Gradle:**

```bash
./gradlew clean generateProtoWrapper
```

---

## Package Structure Reference

With `basePackage=com.example.model` and versions `v1`, `v2`:

```
com.example.model/
├── api/                          # Interfaces and enums
│   ├── Order.java                # Interface
│   ├── OrderStatusEnum.java      # Unified enum
│   ├── VersionContext.java       # Factory interface
│   └── impl/
│       └── AbstractOrder.java    # Abstract base class
├── v1/
│   ├── OrderV1.java              # V1 implementation
│   └── VersionContextV1.java     # V1 factory
└── v2/
    ├── OrderV2.java              # V2 implementation
    └── VersionContextV2.java     # V2 factory
```

With `includeVersionSuffix=false`:

```
com.example.model/
├── api/
│   ├── Order.java
│   └── VersionContext.java
├── v1/
│   ├── Order.java                # Same name, different package
│   └── VersionContext.java
└── v2/
    ├── Order.java
    └── VersionContext.java
```

---

## See Also

- [Getting Started](GETTING_STARTED.md) - Step-by-step tutorial
- [Cookbook](COOKBOOK.md) - Practical examples
- [Schema Diff](SCHEMA_DIFF.md) - Diff configuration
- [Incremental Build](INCREMENTAL_BUILD.md) - Build optimization details
