# Proto Wrapper Plugin

Build plugins for generating version-agnostic Java wrapper classes from multiple protobuf schema versions.

Supports both **Maven** and **Gradle** build systems.

## Features

- Automatic parsing of `.proto` files via `protoc`
- Merging multiple schema versions into a unified API
- Generation of:
  - Version-agnostic interfaces
  - Abstract base classes (Template Method pattern)
  - Version-specific implementations
  - VersionContext for factory operations
  - Builder pattern for modifications (optional)
- Automatic type conflict handling:
  - `INT_ENUM`: int ↔ enum conversion
  - `WIDENING`: int → long, float → double
  - `STRING_BYTES`: string ↔ bytes (UTF-8)
  - `PRIMITIVE_MESSAGE`: primitive ↔ message
- Automatic detection of equivalent enums (nested vs top-level)
- Supported versions info in Javadoc
- Thread-safe immutable wrappers

## Documentation

| Document | Description |
|----------|-------------|
| [COOKBOOK.md](docs/COOKBOOK.md) | Practical guide with examples |
| [API_REFERENCE.md](docs/API_REFERENCE.md) | Generated code API reference |
| [VERSION_AGNOSTIC_API.md](docs/VERSION_AGNOSTIC_API.md) | Detailed API documentation |
| [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) | Known limitations and workarounds |

## Installation

### Maven

```bash
cd proto-wrapper-plugin
mvn install
```

### Gradle

```bash
cd proto-wrapper-plugin
./gradlew publishToMavenLocal
```

## Quick Start

### Maven

#### 1. Add the plugin to pom.xml

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.1.0</version>
    <configuration>
        <basePackage>com.mycompany.myapp.model</basePackage>
        <protoRoot>${basedir}/src/main/proto</protoRoot>
        <versions>
            <version><protoDir>v1</protoDir></version>
            <version><protoDir>v2</protoDir></version>
        </versions>
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

#### 2. Organize proto files

```
src/main/proto/
├── v1/
│   ├── common.proto
│   └── order.proto
└── v2/
    ├── common.proto
    └── order.proto
```

#### 3. Generate code

```bash
mvn generate-sources
```

### Gradle

#### 1. Add the plugin to build.gradle.kts

```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.1.0"
}

protoWrapper {
    basePackage.set("com.mycompany.myapp.model")
    protoRoot.set(file("src/main/proto"))
    versions {
        version("v1")
        version("v2")
    }
}
```

#### 2. Organize proto files

```
src/main/proto/
├── v1/
│   ├── common.proto
│   └── order.proto
└── v2/
    ├── common.proto
    └── order.proto
```

#### 3. Generate code

```bash
./gradlew generateProtoWrapper
```

### 4. Use the API

```java
// Determine version and wrap proto
int version = determineVersion(protoBytes);
VersionContext ctx = VersionContext.forVersion(version);

Order order = ctx.wrapOrder(OrderProto.parseFrom(protoBytes));

// Use version-agnostic API
DateTime dateTime = order.getDateTime();
List<OrderItem> items = order.getItems();
PaymentType payment = order.getPaymentType();

// Serialize back
byte[] outputBytes = order.toBytes();
```

## Generated Code Structure

With `basePackage=com.mycompany.myapp.model`:

**Maven:** `target/generated-sources/proto-wrapper/`
**Gradle:** `build/generated/sources/proto-wrapper/main/java/`

```
{output-dir}/
├── com/mycompany/myapp/model/api/
│   ├── Money.java                    # Interface
│   ├── Order.java                    # Interface with nested interfaces
│   ├── PaymentTypeEnum.java          # Unified enum
│   ├── VersionContext.java           # Factory interface
│   └── impl/
│       ├── AbstractMoney.java        # Abstract base class
│       └── AbstractOrder.java
├── com/mycompany/myapp/model/v1/
│   ├── MoneyV1.java                  # Implementation
│   ├── OrderV1.java
│   └── VersionContextV1.java
└── com/mycompany/myapp/model/v2/
    ├── MoneyV2.java
    ├── OrderV2.java
    └── VersionContextV2.java
```

## Configuration

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `basePackage` | (required) | Base package for all generated classes |
| `protoRoot` | (required) | Root directory with proto files |
| `versions` | (required) | List of version configurations |
| `outputDirectory` | `target/generated-sources/proto-wrapper` (Maven) or `build/generated/sources/proto-wrapper/main/java` (Gradle) | Output directory |
| `protoPackagePattern` | `{basePackage}.proto.{version}` | Package pattern for proto classes |
| `generateBuilders` | `false` | Generate Builder pattern for modifications |
| `protobufMajorVersion` | `3` | Protobuf version (2 or 3) |
| `includeVersionSuffix` | `true` | Include version suffix (MoneyV1 vs Money) |
| `includeMessages` | (all) | List of message names to include |
| `excludeMessages` | (none) | List of message names to exclude |

### Version Parameters

| Parameter | Description |
|-----------|-------------|
| `protoDir` | Directory with proto files relative to `protoRoot` |
| `name` | Version name (defaults to uppercase: `v1` → `V1`) |
| `excludeProtos` | List of proto files to exclude |

### Extended Configuration (Maven)

```xml
<configuration>
    <basePackage>com.mycompany.myapp.model</basePackage>
    <protoRoot>${basedir}/proto</protoRoot>
    <protoPackagePattern>com.mycompany.myapp.proto.{version}</protoPackagePattern>
    <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>

    <versions>
        <version>
            <protoDir>v1</protoDir>
            <name>V1</name>
            <excludeProtos>
                <excludeProto>internal.proto</excludeProto>
            </excludeProtos>
        </version>
        <version>
            <protoDir>v2</protoDir>
        </version>
    </versions>

    <includeMessages>
        <message>Order</message>
        <message>Customer</message>
    </includeMessages>
</configuration>
```

### Extended Configuration (Gradle)

```kotlin
protoWrapper {
    basePackage.set("com.mycompany.myapp.model")
    protoRoot.set(file("proto"))
    protoPackagePattern.set("com.mycompany.myapp.proto.{version}")
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/proto-wrapper/main/java"))
    generateBuilders.set(true)
    protobufMajorVersion.set(3)

    versions {
        version("v1") {
            name.set("V1")
            excludeProtos.set(listOf("internal.proto"))
        }
        version("v2")
    }

    includeMessages.set(listOf("Order", "Customer"))
}
```

## Type Conflict Handling

The plugin automatically handles situations when field types differ between versions:

| Conflict Type | Example | Read API | Builder API |
|---------------|---------|----------|-------------|
| `NONE` | Same type | Normal getter | Normal setter |
| `INT_ENUM` | int ↔ enum | `getXxx()` + `getXxxEnum()` | `setXxx(int)` + `setXxx(Enum)` |
| `WIDENING` | int → long | Auto-widened | Setter with range check |
| `NARROWING` | long → int | Uses wider type | Setter with range check |
| `STRING_BYTES` | string ↔ bytes | `getXxx()` + `getXxxBytes()` | `setXxx(String)` |
| `PRIMITIVE_MESSAGE` | int → Message | `getXxx()` + `getXxxMessage()` | Not generated |
| `INCOMPATIBLE` | string ↔ int | Returns default | Not generated |

See [COOKBOOK.md](docs/COOKBOOK.md) for detailed examples of each conflict type.

## Builder Support

Enable builder generation for creating and modifying messages:

**Maven:**
```xml
<configuration>
    <generateBuilders>true</generateBuilders>
</configuration>
```

**Gradle:**
```kotlin
protoWrapper {
    generateBuilders.set(true)
}
```

### Usage

```java
// Modify existing wrapper
Order modified = order.toBuilder()
    .setCustomerId("CUST-123")
    .setTotalAmount(10000L)
    .build();

// Create new wrapper
Order newOrder = ctx.newOrderBuilder()
    .setOrderId("ORD-456")
    .setCustomerId("CUST-789")
    .build();
```

### Protobuf Version Compatibility

| protobufMajorVersion | Enum Conversion Method |
|---------------------|----------------------|
| `2` | `EnumType.valueOf(int)` |
| `3` (default) | `EnumType.forNumber(int)` |

## Generated Code Examples

### Interface

```java
public interface Money {
    long getBills();
    int getCoins();

    int getWrapperVersion();
    byte[] toBytes();
    Message getProto();

    // With generateBuilders=true
    Builder toBuilder();

    interface Builder {
        Builder setBills(long value);
        Builder setCoins(int value);
        Money build();
    }
}
```

### Abstract Class (Template Method Pattern)

```java
public abstract class AbstractMoney<P extends Message> implements Money {
    protected final P proto;

    protected AbstractMoney(P proto) {
        this.proto = proto;
    }

    protected abstract long extractBills(P proto);
    protected abstract int extractCoins(P proto);

    @Override
    public final long getBills() {
        return extractBills(proto);
    }

    @Override
    public final int getCoins() {
        return extractCoins(proto);
    }
}
```

### VersionContext

```java
// Get context for version
VersionContext ctx = VersionContext.forVersion(2);

// Wrap proto message
Money money = ctx.wrapMoney(protoMessage);
Order order = ctx.wrapOrder(orderProto);

// Create new builder (with generateBuilders=true)
Order.Builder builder = ctx.newOrderBuilder();
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Proto Files                              │
│                    v1/*.proto, v2/*.proto                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  ProtocExecutor → ProtoAnalyzer → VersionMerger             │
│         Parses proto files, merges into unified schema      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                GenerationOrchestrator                       │
│         Coordinates all code generators                     │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┬───────────────┐
              ▼               ▼               ▼               ▼
       InterfaceGen    AbstractClassGen   ImplClassGen   VersionContextGen
              │               │               │               │
              └───────────────┴───────────────┴───────────────┘
                              │
                              ▼
                         JavaPoet → .java files
```

### Conflict Handling Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  FieldProcessingChain                       │
│         Dispatches fields to appropriate handlers           │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
   IntEnumHandler      WideningHandler      StringBytesHandler
         │                    │                    │
         ▼                    ▼                    ▼
   PrimitiveMessageHandler   DefaultHandler   RepeatedConflictHandler
```

## Limitations

See [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for complete documentation.

**Summary:**
- `oneof` fields: not specially handled
- `map` fields: basic support
- Extensions (proto2): not supported
- Well-known types (google.protobuf.*): treated as regular messages
- Version conversion (`asVersion`): implemented via serialization
- Repeated fields with conflicts: read-only (no builder setters)

## Development

### Maven

```bash
# Build
mvn clean install

# Run tests
mvn test

# Build without tests
mvn install -DskipTests
```

### Gradle

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Build without tests
./gradlew build -x test

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## See Also

- [COOKBOOK.md](docs/COOKBOOK.md) - Practical usage guide
- [API_REFERENCE.md](docs/API_REFERENCE.md) - Generated code API reference
- [VERSION_AGNOSTIC_API.md](docs/VERSION_AGNOSTIC_API.md) - Detailed API documentation
- [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) - Known limitations and workarounds
- [examples/maven-example](examples/maven-example) - Maven example project
- [examples/gradle-example](examples/gradle-example) - Gradle example project

