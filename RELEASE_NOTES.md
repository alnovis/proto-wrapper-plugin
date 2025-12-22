# Release Notes — Proto Wrapper Maven Plugin v1.0.4

**Release Date:** December 22, 2025

## Overview

This release introduces the Builder pattern for wrapper modification, protobuf 2.x/3.x compatibility, critical bug fixes for nested types in PRIMITIVE_MESSAGE conflicts, and Java 11 compatibility improvements.

## What's New

### Builder Pattern Support

Generate mutable builders for wrapper objects with the new `generateBuilders` parameter:

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
</configuration>
```

**Features:**
- `toBuilder()` method on all wrapper classes
- `newBuilder()` static factory method
- Fluent API: `setXxx()`, `clearXxx()`, `addXxx()`, `addAllXxx()`
- Support for all field types: primitives, messages, enums, repeated fields

**Example:**

```java
Order modified = order.toBuilder()
    .setStatus(OrderStatus.SHIPPED)
    .setShippingAddress(newAddress)
    .addItem(newItem)
    .build();
```

### Protobuf Version Compatibility

Support for both protobuf 2.x and 3.x APIs with the `protobufMajorVersion` parameter:

```xml
<configuration>
    <protobufMajorVersion>2</protobufMajorVersion>
</configuration>
```

- `2`: Uses `EnumType.valueOf(int)` for enum conversion
- `3` (default): Uses `EnumType.forNumber(int)` for enum conversion

### Integration Tests Module

New `proto-wrapper-integration-tests` module with comprehensive type conflict coverage:

| Test Class | Tests | Purpose |
|------------|-------|---------|
| `TypeConflictTest` | 46 | All conflict types (INT_ENUM, WIDENING, PRIMITIVE_MESSAGE, STRING_BYTES) |
| `TypeConflictRoundTripTest` | 12 | Serialization/deserialization tests |
| `NestedPrimitiveMessageConflictTest` | 6 | Nested message types in PRIMITIVE_MESSAGE conflicts |

### Documentation

- **COOKBOOK.md** — Practical guide with conflict resolution examples
- **VERSION_AGNOSTIC_API.md** — Comprehensive API documentation
- **KNOWN_ISSUES.md** — Detailed documentation of known limitations

## Bug Fixes

### Nested Types in PRIMITIVE_MESSAGE Conflicts

**Problem:** Generated code referenced nested message types incorrectly (e.g., `ParentTicket` instead of `TicketRequest.ParentTicket`), causing compilation errors.

**Cause:** `getMessageTypeForField()` in `CodeGenerationHelper.java` extracted only the simple type name without considering the nesting hierarchy.

**Fix:** Updated to use `extractNestedTypePath()` and properly construct `ClassName` with nested parts:

```java
// Before (broken)
ClassName.get(ctx.apiPackage(), simpleTypeName);
// Generated: org.example.api.ParentTicket

// After (fixed)
String[] nestedParts = Arrays.copyOfRange(parts, 1, parts.length);
ClassName.get(ctx.apiPackage(), parts[0], nestedParts);
// Generated: org.example.api.TicketRequest.ParentTicket
```

### Java 11 Compatibility

**Problem:** Generated code used `Stream.toList()` which is only available in Java 16+.

**Cause:** `toList()` was used in code generation for repeated enum fields.

**Fix:** Replaced with `collect(Collectors.toList())` for Java 11 compatibility:

```java
// Before (Java 16+)
.stream().map(...).toList()

// After (Java 11+)
.stream().map(...).collect(java.util.stream.Collectors.toList())
```

**Affected files:**
- `CodeGenerationHelper.java`
- `RepeatedConflictHandler.java`

## Upgrade Guide

### 1. Update Plugin Version

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.4</version>
</plugin>
```

### 2. Enable Builders (Optional)

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
</configuration>
```

### 3. Regenerate Code

```bash
mvn clean compile
```

## Known Builder Limitations

- Type conflicts across versions (int→enum, primitive→message) have limited builder support
- bytes fields require manual ByteString conversion
- See [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for details and workarounds

## API Compatibility

No breaking changes to the generated code or configuration. All existing setups remain valid.

## Dependencies

No new runtime dependencies.

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [Changelog](CHANGELOG.md)
- [Example Project](examples/maven-example/)

## License

Apache License 2.0
