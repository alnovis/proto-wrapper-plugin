# Proto Wrapper Plugin Cookbook

A practical guide to using proto-wrapper-maven-plugin with detailed examples.

## Table of Contents

- [Quick Start](#quick-start)
- [Plugin Configuration](#plugin-configuration)
- [Type Conflict Handling](#type-conflict-handling)
- [Generation Modes](#generation-modes)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### Step 1: Add the plugin to pom.xml

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.1.0</version>
    <configuration>
        <basePackage>com.mycompany.model</basePackage>
        <protoPackagePattern>com.mycompany.proto.{version}</protoPackagePattern>
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

### Step 2: Organize proto files

```
src/main/proto/
├── v1/
│   ├── common.proto
│   └── order.proto
└── v2/
    ├── common.proto
    └── order.proto
```

### Step 3: Generate code

```bash
mvn generate-sources
```

### Step 4: Use the API

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

---

## Plugin Configuration

### Minimal Configuration

```xml
<configuration>
    <basePackage>com.example.model</basePackage>
    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>
    <protoRoot>${basedir}/proto</protoRoot>
    <versions>
        <version><protoDir>v1</protoDir></version>
        <version><protoDir>v2</protoDir></version>
    </versions>
</configuration>
```

### Full Configuration

```xml
<configuration>
    <!-- Base package (required) -->
    <basePackage>com.example.model</basePackage>

    <!-- Proto package pattern (required) -->
    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>

    <!-- Root directory with proto files (required) -->
    <protoRoot>${basedir}/proto</protoRoot>

    <!-- Output directory (default: target/generated-sources/proto-wrapper) -->
    <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>

    <!-- Generate Builder interfaces (default: false) -->
    <generateBuilders>true</generateBuilders>

    <!-- Version suffix in class names: MoneyV1 vs Money (default: true) -->
    <includeVersionSuffix>true</includeVersionSuffix>

    <!-- Protobuf version: 2 or 3 (default: 3) -->
    <protobufMajorVersion>3</protobufMajorVersion>

    <!-- Generation flags (all true by default) -->
    <generateInterfaces>true</generateInterfaces>
    <generateAbstractClasses>true</generateAbstractClasses>
    <generateImplClasses>true</generateImplClasses>
    <generateVersionContext>true</generateVersionContext>

    <!-- Message filtering -->
    <includeMessages>
        <message>Order</message>
        <message>Customer</message>
    </includeMessages>
    <excludeMessages>
        <message>InternalMessage</message>
    </excludeMessages>

    <!-- Version configurations -->
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
</configuration>
```

### Derived Packages

When `basePackage` is set, other packages are computed automatically:

| Parameter | Value |
|----------|-------|
| `apiPackage` | `{basePackage}.api` |
| `implPackagePattern` | `{basePackage}.{version}` |

Result for `basePackage=com.example.model`:
- Interfaces: `com.example.model.api`
- V1 implementations: `com.example.model.v1`
- V2 implementations: `com.example.model.v2`

---

## Type Conflict Handling

The plugin automatically detects and handles situations where field types differ between versions.

### Conflict Types Overview

| Conflict Type | Description | Convertible? |
|--------------|-------------|--------------|
| `NONE` | No conflict | - |
| `INT_ENUM` | `int` ↔ `enum` | Yes |
| `WIDENING` | `int` → `long`, `float` → `double` | Yes |
| `NARROWING` | `long` → `int`, `double` → `float` | No (lossy) |
| `STRING_BYTES` | `string` ↔ `bytes` | Yes (UTF-8) |
| `PRIMITIVE_MESSAGE` | `int` → `SomeMessage` | No |
| `INCOMPATIBLE` | Incompatible types | No |

### INT_ENUM Conflict

**Situation**: Field is `int32` in one version and `enum` in another.

```protobuf
// v1/sensor.proto
message SensorReading {
    int32 unit_type = 1;  // 0=Celsius, 1=Fahrenheit
}

// v2/sensor.proto
message SensorReading {
    UnitType unit_type = 1;
}
enum UnitType {
    UNIT_CELSIUS = 0;
    UNIT_FAHRENHEIT = 1;
    UNIT_KELVIN = 2;  // New value in v2
}
```

**Generated code**:

```java
// Unified enum
public enum UnitType {
    UNIT_CELSIUS(0),
    UNIT_FAHRENHEIT(1),
    UNIT_KELVIN(2);

    public static UnitType fromProtoValue(int value) { ... }
    public int getProtoValue() { return value; }
}

// Interface with dual getters
public interface SensorReading {
    int getUnitType();              // Returns int
    UnitType getUnitTypeEnum();     // Returns unified enum

    interface Builder {
        Builder setUnitType(int value);       // Set via int
        Builder setUnitType(UnitType value);  // Set via enum
    }
}
```

**Usage**:

```java
SensorReading reading = ctx.wrapSensorReading(proto);

// Reading
int rawValue = reading.getUnitType();           // 0, 1, 2...
UnitType type = reading.getUnitTypeEnum();      // UNIT_CELSIUS, etc.

// Writing (with Builder)
SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)  // Via enum
    // or
    .setUnitType(2)                      // Via int
    .build();
```

### WIDENING Conflict

**Situation**: Type widening between versions.

```protobuf
// v1/data.proto
message Data {
    int32 value = 1;
    float temperature = 2;
}

// v2/data.proto
message Data {
    int64 value = 1;      // Widened to 64-bit
    double temperature = 2; // Widened to double
}
```

**Generated code**:

```java
public interface Data {
    long getValue();        // Unified as long
    double getTemperature(); // Unified as double

    interface Builder {
        Builder setValue(long value);
        Builder setTemperature(double value);
    }
}

// V1 implementation auto-widens values:
class DataV1 {
    @Override
    protected long extractValue(DataProto.Data proto) {
        return (long) proto.getValue();  // int → long
    }
}

// V1 builder checks range:
class DataV1$Builder {
    @Override
    protected void doSetValue(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Value exceeds int range for V1");
        }
        protoBuilder.setValue((int) value);
    }
}
```

**Usage**:

```java
Data data = ctx.wrapData(proto);
long value = data.getValue();  // Always long

// Builder safely handles narrowing
Data modified = data.toBuilder()
    .setValue(42L)  // OK for both versions
    .build();

// Error when exceeding V1 range
try {
    Data v1Modified = v1Data.toBuilder()
        .setValue(Long.MAX_VALUE)  // Too large for int
        .build();
} catch (IllegalArgumentException e) {
    // "Value exceeds int range for V1"
}
```

### STRING_BYTES Conflict

**Situation**: Field is `string` in one version and `bytes` in another.

```protobuf
// v1/report.proto
message TelemetryReport {
    string checksum = 1;
}

// v2/report.proto
message TelemetryReport {
    bytes checksum = 1;
}
```

**Generated code**:

```java
public interface TelemetryReport {
    String getChecksum();       // String (UTF-8 for bytes)
    byte[] getChecksumBytes();  // byte[] (UTF-8 for string)

    interface Builder {
        Builder setChecksum(String value);
    }
}
```

**Usage**:

```java
TelemetryReport report = ctx.wrapTelemetryReport(proto);

// Reading
String checksumStr = report.getChecksum();        // "abc123"
byte[] checksumBytes = report.getChecksumBytes(); // [97, 98, 99, ...]

// Writing
TelemetryReport modified = report.toBuilder()
    .setChecksum("new_checksum")
    .build();
```

### PRIMITIVE_MESSAGE Conflict

**Situation**: Primitive in one version, message in another.

```protobuf
// v1/order.proto
message Order {
    int32 shipping_cost = 1;  // Simple value
}

// v2/order.proto
message Order {
    Money shipping_cost = 1;  // Complex object
}
```

**Generated code**:

```java
public interface Order {
    int getShippingCost();           // Primitive value
    Money getShippingCostMessage();  // Message (null for primitive versions)

    // Builder NOT generated for such fields
}
```

**Usage**:

```java
Order order = ctx.wrapOrder(proto);

// V1
int cost = order.getShippingCost();  // 100
Money costMsg = order.getShippingCostMessage();  // null

// V2
int cost = order.getShippingCost();  // 0 (default)
Money costMsg = order.getShippingCostMessage();  // Money object
```

### Repeated Fields with Conflicts

For repeated fields with type conflicts, only reading is supported:

```java
public interface RepeatedConflicts {
    List<Long> getNumbers();    // int32 → int64 (elements widened)
    List<Integer> getCodes();   // int32 → enum (enum.getNumber())
    List<String> getTexts();    // string → bytes (UTF-8 conversion)

    // Builder methods for such fields are NOT generated
}
```

**Workaround for writing repeated fields with conflicts**:

```java
// Direct access to typed proto builder
var v2 = (RepeatedConflictsV2) wrapper;
var modified = new RepeatedConflictsV2(
    v2.getTypedProto().toBuilder()
        .addNumbers(12345L)
        .build()
);
```

---

## Generation Modes

### Read-Only Mode (default)

```xml
<configuration>
    <generateBuilders>false</generateBuilders>
</configuration>
```

Only getters are generated. Suitable for:
- Reading and serializing proto messages
- Integration with legacy systems
- Minimal generated code size

```java
Order order = ctx.wrapOrder(proto);

// Reading
DateTime dateTime = order.getDateTime();
List<OrderItem> items = order.getItems();

// Serialization
byte[] bytes = order.toBytes();
```

### Builder Mode

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>
</configuration>
```

Builder interfaces are generated for modification. Suitable for:
- Creating new proto messages
- Modifying existing wrappers
- Full CRUD functionality

```java
// Modify existing
Order modified = order.toBuilder()
    .setCustomerId("CUST-123")
    .setTotalAmount(money)
    .build();

// Create new
Order newOrder = Order.newBuilder()
    .setOrderId("ORD-456")
    .setCustomerId("CUST-789")
    .build();
```

---

## Common Use Cases

### Use Case 1: API Version Migration

```java
public class OrderMigrationService {

    public Order migrateToV2(Order v1Order) {
        // Serialize V1
        byte[] bytes = v1Order.toBytes();

        // Parse as V2
        OrderV2Proto proto = OrderV2Proto.parseFrom(bytes);

        // Wrap as V2
        return new OrderV2(proto);
    }
}
```

### Use Case 2: Versioned API Endpoint

```java
@RestController
public class OrderController {

    @PostMapping("/api/orders")
    public ResponseEntity<byte[]> createOrder(
            @RequestBody byte[] protoBytes,
            @RequestHeader("X-API-Version") int version) {

        VersionContext ctx = VersionContext.forVersion(version);

        // Parse with correct version
        Order order = ctx.wrapOrder(parseProto(protoBytes, version));

        // Business logic
        Order processed = processOrder(order);

        // Response in same version
        return ResponseEntity.ok(processed.toBytes());
    }
}
```

### Use Case 3: Data Aggregation from Multiple Versions

```java
public class OrderAggregator {

    public OrderSummary aggregate(List<Order> orders) {
        long totalAmount = 0;
        int itemCount = 0;

        for (Order order : orders) {
            // Unified API works regardless of version
            totalAmount += order.getTotalAmount();
            itemCount += order.getItems().size();
        }

        return new OrderSummary(totalAmount, itemCount);
    }
}
```

### Use Case 4: Testing with Multiple Versions

```java
@ParameterizedTest
@ValueSource(ints = {1, 2})
void testOrderProcessing(int version) {
    // Create test data for each version
    VersionContext ctx = VersionContext.forVersion(version);

    Order order = createTestOrder(ctx);

    // Test business logic
    Order result = orderService.process(order);

    // Assertions work for any version
    assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
}
```

---

## Troubleshooting

### Error: "protoc not found"

```
protoc not found. Please install protobuf compiler or set protocPath parameter.
```

**Solution**: Install protobuf compiler or specify path:

```xml
<configuration>
    <protocPath>/usr/local/bin/protoc</protocPath>
</configuration>
```

### Error: "Cannot resolve symbol" in IDE

IDE may show errors for `${os.detected.classifier}` - this is normal.

**Solution**: Run `mvn compile` from command line.

### Warning: "Type conflict for field"

```
[WARN] Type conflict for field 'value' [WIDENING]: v1:int, v2:long
```

This is informational. The plugin automatically handles the conflict.

### Builder setter not generated for a field

Check the conflict type. Builder setters are not generated for:
- `PRIMITIVE_MESSAGE` conflicts
- `INCOMPATIBLE` conflicts
- Repeated fields with type conflicts

**Workaround**: Use typed proto builder directly.

### Enum not found in unified API

Verify the enum is defined identically in both versions:
- Same value names
- Same numeric codes

The plugin auto-detects equivalent enums.

---

## See Also

- [VERSION_AGNOSTIC_API.md](VERSION_AGNOSTIC_API.md) - Detailed Version-Agnostic API description
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) - Known limitations
