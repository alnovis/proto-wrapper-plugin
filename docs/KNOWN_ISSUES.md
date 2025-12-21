# Known Issues and Limitations

This document describes known issues and limitations of the proto-wrapper-maven-plugin, particularly related to Builder generation.

## Table of Contents

- [Builder Generation Issues](#builder-generation-issues)
  - [Type Conflicts Across Versions](#type-conflicts-across-versions)
  - [bytes Field Handling](#bytes-field-handling)
  - [Protobuf Version Compatibility](#protobuf-version-compatibility)
- [General Limitations](#general-limitations)
- [Workarounds](#workarounds)

---

## Builder Generation Issues

Builder generation (`generateBuilders=true`) has several known limitations that may cause compilation errors in certain scenarios.

### Type Conflicts Across Versions

**Status:** ✅ Handled (since v1.0.5)

**Description:**
When a field has different types in different schema versions, the plugin now automatically detects and handles these conflicts.

**Handled conflict types:**

| Conflict Type | Example | Handling |
|--------------|---------|----------|
| `INT_ENUM` | int ↔ TaxTypeEnum | **Full support** with unified enum (Phase 2) |
| `WIDENING` | int → long, int → double | Setter skipped, field read-only in builder |
| `NARROWING` | long → int | Setter skipped, field read-only in builder |
| `STRING_BYTES` | string ↔ bytes | Setter skipped, field read-only in builder |
| `PRIMITIVE_MESSAGE` | int → Message | Setter skipped, field read-only in builder |

**INT_ENUM Conflicts - Full Support (Phase 2):**

For fields that are `int` in one version and `enum` in another, the plugin generates:
1. **Unified Enum:** A merged enum with all values from all versions
2. **Dual Getters:** `getField()` returns int, `getFieldEnum()` returns unified enum
3. **Overloaded Setters:** Builder accepts both `int` and unified enum values

```java
// Unified enum generated automatically
public enum UnitType {
    UNIT_CELSIUS(0), UNIT_FAHRENHEIT(1), UNIT_KELVIN(2), ...
}

// Interface with dual getters
interface SensorReading {
    int getUnitType();              // Returns int value
    UnitType getUnitTypeEnum();     // Returns unified enum (or null)

    interface Builder {
        Builder setUnitType(int value);       // Set via int
        Builder setUnitType(UnitType value);  // Set via enum
    }
}

// Usage
SensorReading reading = ...;
UnitType type = reading.getUnitTypeEnum();  // Type-safe access

SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)  // Set via enum
    .build();
```

**How it works:**

1. **Conflict Detection:** During schema merge, fields with type mismatches are detected and classified
2. **Builder Setters:** Conflicting fields are excluded from the builder interface
3. **Javadoc:** Builder interface documents which fields are unavailable and why
4. **Getters:** Conflicting fields return default values (0, null, false) through the unified interface
5. **Access:** Full access to version-specific data via `getTypedProto()`

**Generated Javadoc example:**
```java
/**
 * Builder for creating and modifying SensorReading instances.
 *
 * <p><b>Note:</b> {@code unitType} setter not available due to type conflict (INT_ENUM).</p>
 * <p><b>Note:</b> {@code calibrationId} setter not available due to type conflict (PRIMITIVE_MESSAGE).</p>
 */
interface Builder {
    // Only non-conflicting fields have setters
}
```

**Accessing conflicting fields:**
```java
// INT_ENUM conflicts (Phase 2): Full support via unified interface
int unitType = reading.getUnitType();           // Returns actual int value
UnitType type = reading.getUnitTypeEnum();      // Returns unified enum

// Modify via builder with type-safe enum
SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)
    .build();

// Other conflict types: Access via typed proto
if (reading instanceof SensorReadingV2 v2) {
    double precision = v2.getTypedProto().getPrecisionLevel();  // WIDENING conflict
}
```

---

### bytes Field Handling

**Status:** Partial support

**Description:**
Fields of type `bytes` in protobuf require conversion between `byte[]` (Java) and `ByteString` (protobuf).

**Error example:**
```
incompatible types: byte[] cannot be converted to com.google.protobuf.ByteString
```

**Root cause:**
The generated builder setters pass `byte[]` directly to proto builder, but proto builders expect `ByteString`.

**Required fix:**
Builder setters for `bytes` fields should use:
```java
protoBuilder.setData(ByteString.copyFrom(bytes))
```

Instead of:
```java
protoBuilder.setData(bytes)  // Error!
```

**Workaround:**
- Use proto builder directly for messages with `bytes` fields
- Disable builders for affected messages

---

### Protobuf Version Compatibility

**Status:** Supported via configuration

**Description:**
Protobuf 2.x and 3.x have different APIs for converting integers to enum values.

| Protobuf Version | Method |
|-----------------|--------|
| 2.x | `EnumType.valueOf(int)` |
| 3.x | `EnumType.forNumber(int)` |

**Solution:**
Set `protobufMajorVersion` parameter in plugin configuration:

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>2</protobufMajorVersion> <!-- For protobuf 2.x -->
</configuration>
```

**Default value:** `3` (protobuf 3.x)

---

## General Limitations

These limitations apply to all generated code (not just builders):

### 1. oneof Fields
- **Status:** Not supported
- **Description:** Protobuf `oneof` fields are not handled specially

### 2. map Fields
- **Status:** Basic support
- **Description:** Map fields have limited support, may not work correctly in all cases

### 3. Complex Nested Hierarchies
- **Status:** Partial support
- **Description:** Deeply nested message hierarchies may require manual configuration

### 4. Extensions
- **Status:** Not supported
- **Description:** Protobuf extensions are not supported

---

## Workarounds

### For INT_ENUM Conflicts ✅ Fully Resolved

**No workaround needed!** INT_ENUM conflicts now have full support with unified enums (Phase 2):

```java
// Direct access via unified enum
UnitType type = reading.getUnitTypeEnum();

// Modify via builder - works with both int and enum
SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)  // or .setUnitType(2)
    .build();
```

### For Other Type Conflicts (WIDENING, PRIMITIVE_MESSAGE, etc.)

Use the typed proto for modifications:

```java
// Access typed proto for version-specific operations
var v2 = (SensorReadingV2) reading;
var protoBuilder = v2.getTypedProto().toBuilder();
protoBuilder.setPrecisionLevel(3.14);  // WIDENING: int → double
SensorReading modified = new SensorReadingV2(protoBuilder.build());
```

### For bytes Field Issues

Use native proto builder:

```java
// Get underlying proto and modify
var protoBuilder = wrapper.getTypedProto().toBuilder();
protoBuilder.setSignature(ByteString.copyFrom(signatureBytes));
TicketResponse modified = new TicketResponse(protoBuilder.build());
```

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
