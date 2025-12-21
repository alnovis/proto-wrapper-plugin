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
| `INT_ENUM` | int ↔ TaxTypeEnum | Setter skipped, field read-only in builder |
| `WIDENING` | int → long, int → double | Setter skipped, field read-only in builder |
| `NARROWING` | long → int | Setter skipped, field read-only in builder |
| `STRING_BYTES` | string ↔ bytes | Setter skipped, field read-only in builder |
| `PRIMITIVE_MESSAGE` | int → Message | Setter skipped, field read-only in builder |

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
// For read access via unified interface:
int unitType = reading.getUnitType();  // Returns 0 for V2 (conflict)

// For full access via typed proto:
if (reading instanceof SensorReadingV2 v2) {
    UnitTypeEnum unitType = v2.getTypedProto().getUnitType();  // Actual enum value
}

// For modifications:
var protoBuilder = v2.getTypedProto().toBuilder();
protoBuilder.setUnitType(UnitTypeEnum.UNIT_KELVIN);
SensorReading modified = new SensorReadingV2(protoBuilder.build());
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

### For Type Conflict Issues ✅

**Note:** Type conflicts are now automatically handled (see above). Conflicting fields are read-only in builders, and you can access them via `getTypedProto()`.

For fields with type conflicts, use the typed proto for modifications:

```java
// Access typed proto for version-specific operations
var v2 = (SensorReadingV2) reading;
var protoBuilder = v2.getTypedProto().toBuilder();
protoBuilder.setUnitType(UnitTypeEnum.UNIT_KELVIN);
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
