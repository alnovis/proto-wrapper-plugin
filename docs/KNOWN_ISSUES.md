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

**Status:** Not supported in builders

**Description:**
When a field has different types in different schema versions, the builder setters may not compile correctly.

**Examples of problematic scenarios:**

| Field | Version 1 | Version 2 | Issue |
|-------|-----------|-----------|-------|
| `tax_type` | `int` | `TaxTypeEnum` | Merged type is `int`, but v2 setter expects enum |
| `parent_ticket` | `int` | `ParentTicket` (message) | Merged type is `Integer`, but v2 setter expects message |
| `pos_rrn` | `long` | `int` | Type widening/narrowing mismatch |

**Error examples:**
```
incompatible types: int cannot be converted to TaxTypeEnum
incompatible types: java.lang.Integer cannot be converted to ParentTicket
```

**Root cause:**
The builder generates setters based on the merged field type (e.g., `int`), but the underlying proto builder in a specific version may expect a different type (e.g., enum or message).

**Workaround:**
- Avoid enabling builders for schemas with significant type changes between versions
- Use read-only wrappers (`generateBuilders=false`) for such projects
- Manually create/modify protos using the native proto builder API

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

### For Type Conflict Issues

If your project has type conflicts between versions, you have several options:

1. **Disable builders entirely:**
   ```xml
   <generateBuilders>false</generateBuilders>
   ```

2. **Use native proto API for modifications:**
   ```java
   // Instead of using wrapper builder
   Order.OrderRequest.Builder protoBuilder =
       ((com.example.v2.OrderRequest) wrapper).getTypedProto().toBuilder();
   protoBuilder.setTaxType(TaxTypeEnum.VAT);
   OrderRequest modified = new com.example.v2.OrderRequest(protoBuilder.build());
   ```

3. **Exclude problematic messages:**
   ```xml
   <excludeMessages>
       <message>TicketRequest</message>
       <message>BindedTaxation</message>
   </excludeMessages>
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
