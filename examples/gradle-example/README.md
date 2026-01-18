# Proto Wrapper Gradle Example

This example demonstrates how to use the `proto-wrapper-gradle-plugin` with a Gradle project.

## Features Demonstrated

- Gradle Kotlin DSL configuration
- Proto Wrapper plugin DSL (`protoWrapper { ... }`)
- Multi-version protobuf schema support (v1, v2)
- Integration with `com.google.protobuf` Gradle plugin

## Project Structure

```
gradle-example/
├── build.gradle.kts           # Gradle build with plugin configuration
├── settings.gradle.kts        # Project settings
├── gradlew / gradlew.bat      # Gradle wrapper scripts
├── gradle/wrapper/            # Gradle wrapper files
└── proto/
    ├── v1/
    │   ├── common.proto       # Common types (Date, Money, Address, Status)
    │   ├── order.proto        # Order-related messages
    │   ├── user.proto         # User-related messages
    │   ├── invoice.proto      # Invoice messages
    │   └── telemetry.proto    # Telemetry messages
    └── v2/
        ├── common.proto       # Enhanced common types
        ├── order.proto        # Enhanced order messages
        ├── user.proto         # Enhanced user messages
        ├── invoice.proto      # Enhanced invoice messages
        └── telemetry.proto    # Enhanced telemetry messages
```

## Prerequisites

- Java 17+
- Gradle 8.5+ (or use included wrapper)
- proto-wrapper-gradle-plugin published to local Maven repository

## Building

```bash
# From the gradle-example directory
./gradlew clean build

# Generate wrapper code only
./gradlew generateProtoWrapper

# View generated sources
ls -la build/generated/sources/proto-wrapper/main/java/
```

## Generated Code Structure

After running `./gradlew build`:

```
build/generated/
├── source/proto/main/java/    # Protobuf-generated Java classes
│   └── com/example/proto/
│       ├── v1/                # V1 proto classes
│       └── v2/                # V2 proto classes
└── sources/proto-wrapper/main/java/
    └── com/example/model/
        ├── api/               # Version-agnostic interfaces
        │   ├── Money.java
        │   ├── OrderStatus.java
        │   ├── VersionContext.java
        │   └── impl/
        │       └── AbstractMoney.java
        ├── v1/                # V1 implementations
        │   ├── Money.java
        │   └── VersionContextV1.java
        └── v2/                # V2 implementations
            ├── Money.java
            └── VersionContextV2.java
```

## Plugin Configuration

The plugin is configured in `build.gradle.kts`:

```kotlin
plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    id("space.alnovis.proto-wrapper")
}

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
```

## Key Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `basePackage` | String | Base package for generated wrappers |
| `protoPackagePattern` | String | Pattern for proto Java packages (`{version}` is replaced) |
| `protoRoot` | File | Directory containing version subdirectories |
| `includeVersionSuffix` | Boolean | Add version suffix to class names |
| `generateBuilders` | Boolean | Generate Builder interfaces |
| `versions` | Container | Version configurations with `protoDir` |

## Usage Example

```java
import com.example.model.api.*;

public class Example {
    public static void main(String[] args) {
        // Get version context (recommended String-based API)
        VersionContext ctx = VersionContext.forVersionId("v1");

        // Other useful static methods
        VersionContext.find("v1");           // Optional<VersionContext>
        VersionContext.getDefault();         // VersionContext (latest)
        VersionContext.supportedVersions();  // List<String>
        VersionContext.isSupported("v1");    // boolean

        // Wrap a proto message
        com.example.proto.v1.Common.Money proto =
            com.example.proto.v1.Common.Money.newBuilder()
                .setAmount(1000)
                .setCurrency("USD")
                .build();

        Money money = ctx.wrapMoney(proto);
        System.out.println("Amount: " + money.getAmount());
        System.out.println("Currency: " + money.getCurrency());
    }
}
```

## Differences from Maven Plugin

| Aspect | Maven | Gradle |
|--------|-------|--------|
| Configuration | XML in `pom.xml` | Kotlin DSL in `build.gradle.kts` |
| Task name | `generate` goal | `generateProtoWrapper` task |
| Property syntax | `<basePackage>` | `basePackage.set(...)` |
| Version syntax | `<version><protoDir>v1</protoDir></version>` | `create("v1") { protoDir.set("v1") }` |

## Dependencies

| Dependency | Version |
|------------|---------|
| Gradle | 8.5+ |
| protobuf-gradle-plugin | 0.9.4 |
| protobuf-java | 4.28.2 |
| proto-wrapper-gradle-plugin | 1.6.7 |

## See Also

- [Proto Wrapper Documentation](../../docs/README.md)
- [Maven Example](../maven-example/README.md)
- [Spring Boot Example](../spring-boot-example/README.md)
- [Gradle Plugin Configuration](../../docs/CONFIGURATION.md#gradle-configuration)
