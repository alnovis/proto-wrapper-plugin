# Getting Started with Proto Wrapper

This guide walks you through setting up Proto Wrapper Plugin from scratch. By the end, you'll have a working project that generates version-agnostic Java wrappers from multiple protobuf schema versions.

**Time required:** 15-20 minutes

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Step 1: Create Project Structure](#step-1-create-project-structure)
- [Step 2: Define Proto Schemas](#step-2-define-proto-schemas)
- [Step 3: Configure the Plugin](#step-3-configure-the-plugin)
- [Step 4: Generate Wrapper Code](#step-4-generate-wrapper-code)
- [Step 5: Use the Generated API](#step-5-use-the-generated-api)
- [Next Steps](#next-steps)

---

## Prerequisites

Before you begin, ensure you have:

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |

**Note:** Gradle 8.5+ is also supported. This guide uses Maven; see [Gradle Configuration](CONFIGURATION.md#gradle-configuration) for Gradle setup.

**No manual protoc installation required!** The plugin automatically downloads the appropriate `protoc` binary from Maven Central if not found in PATH (since v1.6.5).

---

## Step 1: Create Project Structure

Create a new Maven project with the following structure:

```
my-proto-project/
├── pom.xml
├── proto/
│   ├── v1/
│   │   └── order.proto
│   └── v2/
│       └── order.proto
└── src/main/java/
    └── com/example/
        └── Main.java
```

Create the directories:

```bash
mkdir -p my-proto-project/proto/v1
mkdir -p my-proto-project/proto/v2
mkdir -p my-proto-project/src/main/java/com/example
cd my-proto-project
```

---

## Step 2: Define Proto Schemas

Create two versions of a simple proto schema to demonstrate version evolution.

### Version 1: proto/v1/order.proto

```protobuf
syntax = "proto3";

package com.example.proto.v1;

option java_package = "com.example.proto.v1";
option java_outer_classname = "OrderProtos";

message Order {
    string order_id = 1;
    string customer_id = 2;
    int32 total_cents = 3;        // Price in cents (integer)
    int32 status = 4;             // Status as integer: 0=PENDING, 1=COMPLETED
}
```

### Version 2: proto/v2/order.proto

```protobuf
syntax = "proto3";

package com.example.proto.v2;

option java_package = "com.example.proto.v2";
option java_outer_classname = "OrderProtos";

message Order {
    string order_id = 1;
    string customer_id = 2;
    int64 total_cents = 3;        // Changed to int64 for larger amounts
    OrderStatus status = 4;       // Changed to enum

    // New field in v2
    string notes = 5;
}

enum OrderStatus {
    ORDER_STATUS_UNSPECIFIED = 0;
    ORDER_STATUS_PENDING = 1;
    ORDER_STATUS_COMPLETED = 2;
    ORDER_STATUS_CANCELLED = 3;   // New status in v2
}
```

**Notice the differences:**
- `total_cents`: `int32` in v1 vs `int64` in v2 (WIDENING conflict)
- `status`: `int32` in v1 vs `enum` in v2 (INT_ENUM conflict)
- `notes`: New field in v2 only

Proto Wrapper will automatically handle these conflicts.

---

## Step 3: Configure the Plugin

Create `pom.xml` with the following content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-proto-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <protobuf.version>3.25.1</protobuf.version>
        <proto-wrapper.version>1.6.5</proto-wrapper.version>
    </properties>

    <dependencies>
        <!-- Protobuf runtime -->
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>${protobuf.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 1. Compile proto files to Java (v1) -->
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.1</version>
                <executions>
                    <execution>
                        <id>protoc-v1</id>
                        <goals><goal>compile</goal></goals>
                        <configuration>
                            <protoSourceRoot>${basedir}/proto/v1</protoSourceRoot>
                            <outputDirectory>${project.build.directory}/generated-sources/protobuf-v1</outputDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>protoc-v2</id>
                        <goals><goal>compile</goal></goals>
                        <configuration>
                            <protoSourceRoot>${basedir}/proto/v2</protoSourceRoot>
                            <outputDirectory>${project.build.directory}/generated-sources/protobuf-v2</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- 2. Generate Proto Wrappers -->
            <plugin>
                <groupId>io.alnovis</groupId>
                <artifactId>proto-wrapper-maven-plugin</artifactId>
                <version>${proto-wrapper.version}</version>
                <configuration>
                    <basePackage>com.example.model</basePackage>
                    <protoRoot>${basedir}/proto</protoRoot>
                    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>
                    <generateBuilders>true</generateBuilders>
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

            <!-- 3. Add generated sources to compilation -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>3.4.0</version>
                <executions>
                    <execution>
                        <id>add-source</id>
                        <phase>generate-sources</phase>
                        <goals><goal>add-source</goal></goals>
                        <configuration>
                            <sources>
                                <source>${project.build.directory}/generated-sources/protobuf-v1</source>
                                <source>${project.build.directory}/generated-sources/protobuf-v2</source>
                                <source>${project.build.directory}/generated-sources/proto-wrapper</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**Key configuration options:**

| Option | Value | Description |
|--------|-------|-------------|
| `basePackage` | `com.example.model` | Base package for generated wrappers |
| `protoRoot` | `${basedir}/proto` | Directory containing version subdirectories |
| `protoPackagePattern` | `com.example.proto.{version}` | Pattern for proto Java packages |
| `generateBuilders` | `true` | Enable Builder pattern for modifications |
| `generateProtocolVersions` | `false` | Generate `ProtocolVersions` class with version constants *(since 2.1.0)* |

See [Configuration Reference](CONFIGURATION.md) for all options.

---

## Step 4: Generate Wrapper Code

Run Maven to generate both protobuf classes and wrapper code:

```bash
mvn clean compile
```

You should see output indicating successful generation:

```
[INFO] --- proto-wrapper-maven-plugin:1.6.5:generate ---
[INFO] Proto Wrapper: Generating wrappers for 2 versions
[INFO] Proto Wrapper: Processing version v1 (1 messages)
[INFO] Proto Wrapper: Processing version v2 (1 messages)
[INFO] Proto Wrapper: Generated 6 Java files
```

### Generated Code Structure

After generation, check the output:

```
target/generated-sources/
├── protobuf-v1/
│   └── com/example/proto/v1/
│       └── OrderProtos.java          # Protobuf-generated v1
├── protobuf-v2/
│   └── com/example/proto/v2/
│       └── OrderProtos.java          # Protobuf-generated v2
└── proto-wrapper/
    └── com/example/model/
        ├── api/
        │   ├── Order.java            # Interface
        │   ├── OrderStatusEnum.java  # Unified enum
        │   ├── VersionContext.java   # Factory interface
        │   └── impl/
        │       └── AbstractOrder.java
        ├── v1/
        │   ├── OrderV1.java          # V1 implementation
        │   └── VersionContextV1.java
        └── v2/
            ├── OrderV2.java          # V2 implementation
            └── VersionContextV2.java
```

---

## Step 5: Use the Generated API

Create `src/main/java/com/example/Main.java`:

```java
package com.example;

import com.example.model.api.Order;
import com.example.model.api.OrderStatusEnum;
import com.example.model.api.VersionContext;
import com.example.proto.v1.OrderProtos;

public class Main {
    public static void main(String[] args) throws Exception {
        // Example 1: Wrap an existing v1 proto message
        OrderProtos.Order v1Proto = OrderProtos.Order.newBuilder()
                .setOrderId("ORD-001")
                .setCustomerId("CUST-123")
                .setTotalCents(9999)
                .setStatus(1)  // COMPLETED as int
                .build();

        // Get version context for v1 (recommended String-based API)
        VersionContext ctx = VersionContext.forVersionId("v1");

        // Wrap the proto - now you have a version-agnostic interface
        Order order = ctx.wrapOrder(v1Proto);

        // Use the unified API
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Customer: " + order.getCustomerId());
        System.out.println("Total (cents): " + order.getTotalCents());  // Returns long
        System.out.println("Status (int): " + order.getStatus());
        System.out.println("Status (enum): " + order.getStatusEnum()); // Auto-converted to enum!

        // Example 2: Create a new order using Builder
        Order newOrder = Order.newBuilder(ctx)
                .setOrderId("ORD-002")
                .setCustomerId("CUST-456")
                .setTotalCents(15000L)
                .setStatus(OrderStatusEnum.ORDER_STATUS_PENDING)
                .build();

        System.out.println("\nNew order: " + newOrder.getOrderId());

        // Example 3: Serialize back to bytes
        byte[] bytes = newOrder.toBytes();
        System.out.println("Serialized size: " + bytes.length + " bytes");

        // Example 4: Modify existing order
        Order modified = order.toBuilder()
                .setStatus(OrderStatusEnum.ORDER_STATUS_COMPLETED)
                .build();

        System.out.println("\nModified status: " + modified.getStatusEnum());
    }
}
```

Run the example:

```bash
mvn exec:java -Dexec.mainClass=com.example.Main
```

Expected output:

```
Order ID: ORD-001
Customer: CUST-123
Total (cents): 9999
Status (int): 1
Status (enum): ORDER_STATUS_COMPLETED

New order: ORD-002
Serialized size: 28 bytes

Modified status: ORDER_STATUS_COMPLETED
```

---

## Understanding the Generated API

### Version-Agnostic Interface

The `Order` interface works with any version:

```java
public interface Order {
    String getOrderId();
    String getCustomerId();
    long getTotalCents();           // Unified as long (wider type)
    int getStatus();                // Returns int value
    OrderStatusEnum getStatusEnum(); // Returns unified enum

    // Serialization
    byte[] toBytes();
    int getWrapperVersion();

    // Builder (when generateBuilders=true)
    Builder toBuilder();
    static Builder newBuilder(VersionContext ctx) { ... }
}
```

### Type Conflict Handling

Proto Wrapper automatically handles type differences:

| Field | V1 Type | V2 Type | Unified Type | How |
|-------|---------|---------|--------------|-----|
| `total_cents` | `int32` | `int64` | `long` | WIDENING: auto-widen v1 values |
| `status` | `int32` | `enum` | `int` + `Enum` | INT_ENUM: dual getters |
| `notes` | - | `string` | `String` | Returns `null` for v1 |

### VersionContext

Use `VersionContext` to:
- Wrap proto messages: `ctx.wrapOrder(proto)`
- Create new builders: `ctx.newOrderBuilder()`
- Get version info: `ctx.getVersionId()` (returns `"v1"`, `"v2"`, etc.)

```java
// Get context for specific version (recommended String-based API)
VersionContext v1Ctx = VersionContext.forVersionId("v1");
VersionContext v2Ctx = VersionContext.forVersionId("v2");

// Other useful static methods on VersionContext
Optional<VersionContext> maybeCtx = VersionContext.find("v1");
VersionContext defaultCtx = VersionContext.getDefault();
List<String> versions = VersionContext.supportedVersions();

// Wrap protos from different versions
Order orderFromV1 = v1Ctx.wrapOrder(v1Proto);
Order orderFromV2 = v2Ctx.wrapOrder(v2Proto);

// Both use the same interface!
processOrder(orderFromV1);
processOrder(orderFromV2);
```

### ProtocolVersions (Optional)

When `generateProtocolVersions=true`, a `ProtocolVersions` class is generated with constants:

```java
// With generateProtocolVersions=true, use constants instead of strings
import com.example.model.api.ProtocolVersions;

VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V1);

// Benefits: IDE autocomplete, compile-time checking, easy refactoring
if (ProtocolVersions.V2.equals(order.getWrapperVersionId())) {
    // V2-specific logic
}
```

See [Cookbook: Using ProtocolVersions Constants](COOKBOOK.md#using-protocolversions-constants) for more details.

---

## Next Steps

Now that you have a working setup:

1. **[Configuration Reference](CONFIGURATION.md)** - Learn all plugin options
2. **[Cookbook](COOKBOOK.md)** - See patterns for common scenarios:
   - Handling different conflict types
   - Working with nested messages
   - Oneof field handling
   - Well-known types (Timestamp, Duration)
3. **[Examples](../examples/)** - Browse complete example projects
4. **[Schema Diff](SCHEMA_DIFF.md)** - Compare schemas and detect breaking changes
5. **[Known Issues](KNOWN_ISSUES.md)** - Understand current limitations

---

## Troubleshooting

### "protoc not found"

Ensure `protoc` is in your PATH or specify the path:

```xml
<configuration>
    <protocPath>/usr/local/bin/protoc</protocPath>
</configuration>
```

### IDE shows errors for generated code

Run `mvn compile` from command line first, then refresh your IDE project.

### Version mismatch errors

Ensure `protoPackagePattern` matches your proto files' `java_package` option.

---

## Summary

You've learned how to:

1. Set up a project with multiple proto versions
2. Configure Proto Wrapper Plugin
3. Generate version-agnostic wrappers
4. Use the unified API with type conflict handling
5. Create and modify messages using Builders

For more advanced topics, continue to the [Cookbook](COOKBOOK.md).
