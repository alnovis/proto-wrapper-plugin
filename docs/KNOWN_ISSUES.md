# Known Issues and Limitations

This document describes known issues and limitations of the proto-wrapper-maven-plugin.

## Table of Contents

- [Type Conflict Handling](#type-conflict-handling)
- [Repeated Fields with Type Conflicts](#repeated-fields-with-type-conflicts)
- [Version-Specific Messages](#version-specific-messages)
- [General Limitations](#general-limitations)
- [Runtime Behavior](#runtime-behavior)
- [Performance Considerations](#performance-considerations)
- [Workarounds](#workarounds)

---

## Type Conflict Handling

**Status:** Fully Handled (v1.0.5+)

When a field has different types in different schema versions, the plugin automatically detects and handles these conflicts.

### All Conflict Types

| Conflict Type | Example | Read | Builder | Notes |
|--------------|---------|------|---------|-------|
| `NONE` | Same type in all versions | Normal getter | Normal setter | No special handling |
| `INT_ENUM` | int ↔ TaxTypeEnum | `getXxx()` + `getXxxEnum()` | `setXxx(int)` + `setXxx(Enum)` | Convertible |
| `WIDENING` | int → long, float → double | Auto-widened to wider type | Setter with range check | Convertible |
| `NARROWING` | long → int, double → float | Uses wider type (long/double) | Setter with range check | Handled as WIDENING |
| `STRING_BYTES` | string ↔ bytes | `getXxx()` + `getXxxBytes()` | `setXxx(String)` | UTF-8 conversion |
| `PRIMITIVE_MESSAGE` | int → Message | `getXxx()` + `getXxxMessage()` | `setXxx()` + `setXxxMessage()` | Runtime validation (v1.6.2+) |
| `INCOMPATIBLE` | string ↔ int, bool ↔ message | Returns default value | **Not generated** | Field treated as absent |

### NARROWING Behavior

Although defined as "lossy", NARROWING conflicts are handled the same way as WIDENING:

```java
// v1: int64 value = 1;
// v2: int32 value = 1;

interface Data {
    long getValue();  // Unified as wider type (long)

    interface Builder {
        // Range check throws IllegalArgumentException if value exceeds int range for V2
        Builder setValue(long value);
    }
}
```

### INCOMPATIBLE Behavior

Fields with incompatible types are treated as "not present" in versions where the type doesn't match:

```java
// v1: string data = 1;
// v2: int32 data = 1;

interface Message {
    // Returns empty string for V2, actual value for V1
    String getData();

    // has-method returns false for V2
    boolean hasData();

    // Builder setter NOT generated (would be ambiguous)
}
```

### INT_ENUM Example

```java
// Unified enum generated automatically
public enum UnitType {
    UNIT_CELSIUS(0), UNIT_FAHRENHEIT(1), UNIT_KELVIN(2), ...
}

// Interface with dual getters and overloaded setters
interface SensorReading {
    int getUnitType();              // Returns int value
    UnitType getUnitTypeEnum();     // Returns unified enum

    interface Builder {
        Builder setUnitType(int value);       // Set via int
        Builder setUnitType(UnitType value);  // Set via enum
    }
}
```

### STRING_BYTES Example

```java
interface TelemetryReport {
    String getChecksum();       // String representation (UTF-8 for bytes versions)
    byte[] getChecksumBytes();  // Raw bytes (UTF-8 encoded for string versions)

    interface Builder {
        Builder setChecksum(String value);
        Builder setChecksumBytes(byte[] value);
    }
}
```

### WIDENING Example

```java
interface SensorReading {
    long getRawValue();    // int32 in v1, int64 in v2 → unified as long
    double getValues();    // float in v1, double in v2 → unified as double

    interface Builder {
        Builder setRawValue(long value);  // Range check for narrower versions
    }
}
```

---

## Repeated Fields with Type Conflicts

**Status:** Full builder support (v1.4.0+)

Repeated fields with type conflicts have complete support including builder methods with runtime range validation.

### Supported Operations

| Conflict Type | Example | Unified Type | Range Validation |
|--------------|---------|--------------|------------------|
| `WIDENING` | `repeated int32` → `repeated int64` | `List<Long>` | Yes |
| `FLOAT_DOUBLE` | `repeated float` → `repeated double` | `List<Double>` | Yes |
| `SIGNED_UNSIGNED` | `repeated int32` → `repeated uint32` | `List<Long>` | Yes |
| `INT_ENUM` | `repeated int32` → `repeated SomeEnum` | `List<Integer>` | No |
| `STRING_BYTES` | `repeated string` → `repeated bytes` | `List<String>` | No |

### Example

```java
interface RepeatedConflicts {
    List<Long> getNumbers();    // int32 in v1, int64 in v2 → widened elements
    List<Integer> getCodes();   // int32 in v1, enum in v2 → enum.getNumber()
    List<String> getTexts();    // string in v1, bytes in v2 → UTF-8 conversion
    List<Double> getValues();   // float in v1, double in v2 → widened elements

    // Builder - full support in v1.4.0+
    interface Builder {
        Builder addNumbers(long value);
        Builder addAllNumbers(List<Long> values);
        Builder setNumbers(List<Long> values);
        Builder clearNumbers();
        // Same pattern for codes, texts, values
    }
}
```

### Range Validation

When using builders with narrowing versions, values are validated at runtime:

```java
// V1 builder (int32 range):
wrapper.toBuilder()
    .addNumbers(100L)              // OK - within int32 range
    .addNumbers(9_999_999_999L)    // throws IllegalArgumentException
    .build();

// Error message:
// "Value 9999999999 exceeds int32 range for v1"
```

---

## Version-Specific Messages

**Status:** Runtime exception for missing versions

Messages that exist only in some versions throw `UnsupportedOperationException` when accessed via wrong VersionContext.

### Behavior

```java
// Message "NewFeature" exists only in V2

// Using V2 context - works
VersionContext v2 = VersionContext.V2;
NewFeature feature = v2.wrapNewFeature(proto);  // OK
NewFeature.Builder builder = v2.newNewFeatureBuilder();  // OK

// Using V1 context - throws
VersionContext v1 = VersionContext.V1;
NewFeature feature = v1.wrapNewFeature(proto);  // UnsupportedOperationException!
```

### Exception Message

```
NewFeature is not available in this version. Present in: [V2]
```

### Safe Access Pattern

```java
// Check version before accessing
public NewFeature getNewFeatureIfAvailable(VersionContext ctx, Message proto) {
    if (ctx instanceof VersionContextV2) {
        return ctx.wrapNewFeature(proto);
    }
    return null;  // Not available in this version
}
```

---

## General Limitations

### 1. oneof Fields
- **Status:** Supported (v1.2.0+)
- **Description:** Full support for protobuf `oneof` fields with conflict detection
- **Features:**
  - `XxxCase` enum for discriminator (e.g., `MethodCase.CREDIT_CARD`)
  - `getXxxCase()` method to check which field is set
  - `hasXxx()` methods for individual oneof fields
  - `clearXxx()` builder method to clear entire oneof group
- **Conflict Detection:**
  - Partial existence (oneof in some versions only)
  - Field set differences across versions
  - Renamed oneofs (detected by matching field numbers)
  - Field membership changes (field moved in/out of oneof)
- **Note:** Renamed oneofs use the most common name across versions

### 2. map Fields
- **Status:** Full support (v1.2.0+)
- **Description:** Map fields are fully supported including type conflicts
- **Features:**
  - All map accessor methods: `getXxxMap()`, `getXxxCount()`, `containsXxx()`, `getXxxOrDefault()`, `getXxxOrThrow()`
  - Builder methods: `putXxx()`, `putAllXxx()`, `removeXxx()`, `clearXxx()`
  - Type conflict handling: WIDENING (int32→int64), INT_ENUM (int32→enum)
  - Lazy caching with volatile fields for thread-safe performance
  - Non-sequential enum value support (uses `getNumber()` not `ordinal()`)
  - Validation for invalid enum values in builders

### 3. Extensions
- **Status:** Not supported
- **Description:** Protobuf extensions (proto2) are not supported

### 4. Version Conversion (`asVersion`)
- **Status:** Implemented
- **Description:** The `asVersion(Class<T> versionClass)` method converts between version-specific implementations via serialization
- **Note:** Version-specific fields may be lost during conversion (e.g., v2-only fields lost when converting to v1)
- **Example:**
  ```java
  Money v1 = VersionContext.forVersionId("v1").newMoneyBuilder()
          .setAmount(1000L)
          .setCurrency("USD")
          .build();

  // Convert to v2
  space.example.v2.Money v2 = v1.asVersion(space.example.v2.Money.class);
  ```

### 5. Complex Nested Hierarchies
- **Status:** Partial support
- **Description:** Deeply nested message hierarchies with conflicts may require manual configuration

### 6. Well-Known Types (google.protobuf.*)
- **Status:** Full support for 15 types (v1.3.0+)
- **Description:** Google Well-Known Types are automatically converted to idiomatic Java types
- **Supported Types:**
  - `Timestamp` → `java.time.Instant`
  - `Duration` → `java.time.Duration`
  - Wrapper types (`StringValue`, `Int32Value`, etc.) → nullable Java primitives
  - `Struct` → `Map<String, Object>`
  - `Value` → `Object`
  - `ListValue` → `List<Object>`
  - `FieldMask` → `List<String>`
- **Not Supported:** `google.protobuf.Any` (requires runtime type registry)
- **Configuration:** Set `convertWellKnownTypes=false` to disable conversion

```java
// Proto: google.protobuf.Timestamp created_at = 1;
// Generated:
Instant getCreatedAt();  // Automatic conversion to java.time.Instant
```

### 7. Field Number Conflicts
- **Status:** Undefined behavior
- **Description:** If the same field name has different field numbers across versions, behavior is unpredictable
- **Recommendation:** Ensure field numbers are consistent across versions

### 8. Services and RPCs
- **Status:** Not supported
- **Description:** Only message and enum types are processed; service definitions are ignored

---

## Runtime Behavior

### Thread Safety

| Component | Thread-Safe? | Notes |
|-----------|--------------|-------|
| Wrapper classes | Yes | Immutable, proto is immutable |
| Builder classes | **No** | Not designed for concurrent modification |
| VersionContext | Yes | Stateless singleton |

```java
// Safe: Share wrappers across threads
Order order = ctx.wrapOrder(proto);
executor.submit(() -> processOrder(order));  // OK

// Unsafe: Don't share builders
Order.Builder builder = ctx.newOrderBuilder();
executor.submit(() -> builder.setOrderId("1"));  // RACE CONDITION!
```

### Nullability

| Field Type | Getter Returns | Notes |
|------------|----------------|-------|
| Required (proto2) | Never null | Always has value |
| Optional scalar | May be null | Use `hasXxx()` to check |
| Optional message | May be null | Use `hasXxx()` to check |
| Repeated | Never null | Returns empty list if no elements |
| Map | Never null | Returns empty map if no entries |

```java
// Safe patterns
if (order.hasShippingAddress()) {
    Address addr = order.getShippingAddress();  // Not null here
}

List<OrderItem> items = order.getItems();  // Never null, may be empty
for (OrderItem item : items) {
    // Safe iteration
}
```

### ByteString Conversion

Protobuf uses `ByteString` internally; wrappers convert to `byte[]`:

```java
interface Report {
    byte[] getData();  // Converted from ByteString
}

// Implications:
// 1. New byte[] array created on each call
// 2. Modifications to returned array don't affect proto
// 3. For large binary data, consider using getTypedProto()
```

---

## Performance Considerations

### List Wrapper Creation

Repeated field getters create new wrapper lists on each call:

```java
// Each call creates new ArrayList with wrapped elements
List<OrderItem> items1 = order.getItems();
List<OrderItem> items2 = order.getItems();
assert items1 != items2;  // Different list instances

// For hot paths, cache the result
List<OrderItem> items = order.getItems();
for (int i = 0; i < 1000; i++) {
    process(items);  // Reuse cached list
}
```

### Wrapper Overhead

Each wrapper adds minimal overhead:
- One object allocation per wrapper
- One reference to underlying proto
- Method calls delegate to proto

For performance-critical code with millions of messages, consider:
1. Direct proto access via `getTypedProto()`
2. Batch processing with cached wrappers
3. Proto streaming instead of wrapper collections

---

## Workarounds

### For PRIMITIVE_MESSAGE Conflicts

Access via typed proto:

```java
if (wrapper instanceof MyMessageV2 v2) {
    NestedMessage nested = v2.getTypedProto().getNestedField();
}
```

### For Version Conversion

Serialize and parse:

```java
// Convert from V1 to V2
byte[] bytes = v1Wrapper.toBytes();
V2Proto proto = V2Proto.parseFrom(bytes);
MyMessageV2 v2Wrapper = new MyMessageV2(proto);
```

---

## Protobuf Version Compatibility

**Status:** Supported via configuration

| Protobuf Version | Method for enum conversion |
|-----------------|---------------------------|
| 2.x | `EnumType.valueOf(int)` |
| 3.x | `EnumType.forNumber(int)` |

Set `protobufMajorVersion` parameter in plugin configuration:

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>2</protobufMajorVersion> <!-- For protobuf 2.x -->
</configuration>
```

**Default value:** `3` (protobuf 3.x)

---

## Reporting Issues

If you encounter issues not documented here, please report them at:
https://github.com/alnovis/proto-wrapper-plugin/issues

Include:
- Plugin version
- Protobuf version
- Relevant proto file snippets
- Error messages
- Maven/Gradle configuration

---

## See Also

- [Getting Started](GETTING_STARTED.md) - Setup and basic usage
- [Configuration](CONFIGURATION.md) - All plugin options
- [Cookbook](COOKBOOK.md) - Practical examples
- [Schema Diff](SCHEMA_DIFF.md) - Compare schema versions

