# Known Issues and Limitations

This document describes known issues and limitations of the proto-wrapper-maven-plugin.

## Table of Contents

- [Type Conflict Handling](#type-conflict-handling)
- [Repeated Fields with Type Conflicts](#repeated-fields-with-type-conflicts)
- [General Limitations](#general-limitations)
- [Workarounds](#workarounds)

---

## Type Conflict Handling

**Status:** Fully Handled (v1.0.5+)

When a field has different types in different schema versions, the plugin automatically detects and handles these conflicts.

### Supported Conflict Types

| Conflict Type | Example | Read | Builder |
|--------------|---------|------|---------|
| `INT_ENUM` | int ↔ TaxTypeEnum | `getXxx()` + `getXxxEnum()` | `setXxx(int)` + `setXxx(Enum)` |
| `WIDENING` | int → long, float → double | Auto-widened to wider type | Setter available |
| `STRING_BYTES` | string ↔ bytes | `getXxx()` + `getXxxBytes()` | `setXxx(String)` + `setXxxBytes(byte[])` |
| `PRIMITIVE_MESSAGE` | int → Message | Returns null for message versions | Setter skipped |

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

**Status:** Read-only (v1.0.6+)

Repeated fields with type conflicts are fully readable but not settable via the unified builder.

### Supported Read Operations

| Conflict Type | Example | Unified Type |
|--------------|---------|--------------|
| `WIDENING` | `repeated int32` → `repeated int64` | `List<Long>` |
| `WIDENING` | `repeated float` → `repeated double` | `List<Double>` |
| `INT_ENUM` | `repeated int32` → `repeated SomeEnum` | `List<Integer>` |
| `STRING_BYTES` | `repeated string` → `repeated bytes` | `List<String>` |

### Example

```java
interface RepeatedConflicts {
    List<Long> getNumbers();    // int32 in v1, int64 in v2 → widened elements
    List<Integer> getCodes();   // int32 in v1, enum in v2 → enum.getNumber()
    List<String> getTexts();    // string in v1, bytes in v2 → UTF-8 conversion
    List<Double> getValues();   // float in v1, double in v2 → widened elements

    // Builder - repeated conflict fields NOT available
    interface Builder {
        // Note: setNumbers, setCodes, setTexts, setValues are NOT generated
        // Use typed proto builder for direct access
    }
}
```

### Accessing Repeated Conflict Fields for Modification

Use the typed proto builder directly:

```java
// For V2 version
var v2 = (RepeatedConflictsV2) wrapper;
var modified = new RepeatedConflictsV2(
    v2.getTypedProto().toBuilder()
        .addNumbers(12345L)
        .addCodes(CodeEnum.CODE_SUCCESS)
        .build()
);
```

---

## General Limitations

### 1. oneof Fields
- **Status:** Not supported
- **Description:** Protobuf `oneof` fields are not handled specially

### 2. map Fields
- **Status:** Basic support
- **Description:** Map fields have limited support, may not work correctly in all cases

### 3. Extensions
- **Status:** Not supported
- **Description:** Protobuf extensions are not supported

### 4. Version Conversion (`asVersion`)
- **Status:** Not implemented
- **Description:** The `asVersion(Class<T> versionClass)` method throws `UnsupportedOperationException`
- **Workaround:** Use serialization: `targetVersion.from(TargetProto.parseFrom(source.toBytes()))`

### 5. Complex Nested Hierarchies
- **Status:** Partial support
- **Description:** Deeply nested message hierarchies with conflicts may require manual configuration

---

## Workarounds

### For Repeated Conflict Fields in Builders

Use the typed proto builder:

```java
var v2 = (MyMessageV2) wrapper;
var protoBuilder = v2.getTypedProto().toBuilder();
protoBuilder.addRepeatedField(value);
MyMessage modified = new MyMessageV2(protoBuilder.build());
```

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
https://github.com/anthropics/proto-wrapper-plugin/issues

Include:
- Plugin version
- Protobuf version
- Relevant proto file snippets
- Error messages
- Maven configuration
