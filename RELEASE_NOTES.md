# Release Notes - Proto Wrapper Plugin v1.2.0

**Release Date:** January 2, 2026

## Overview

This major release introduces comprehensive conflict type handling, full map field support with type conflicts, oneof field support, and a structured exception hierarchy. It also includes critical bug fixes for map fields with non-sequential enum values.

## What's New

### Full Map Field Support

Map fields are now fully supported with all accessor and builder methods:

```java
// Accessor methods
Map<String, Status> statuses = message.getStatusesMap();
int count = message.getStatusesCount();
boolean hasKey = message.containsStatuses("key");
Status status = message.getStatusesOrDefault("key", Status.UNKNOWN);
Status status = message.getStatusesOrThrow("key");

// Builder methods
builder.putStatuses("key", Status.ACTIVE);
builder.putAllStatuses(statusMap);
builder.removeStatuses("key");
builder.clearStatuses();
```

#### Map Type Conflict Handling

Maps with type conflicts across versions are handled automatically:

```java
// WIDENING conflict: int32 in v1, int64 in v2 -> unified as long
Map<String, Long> values = message.getValuesMap();

// INT_ENUM conflict: int32 in v1, enum in v2 -> unified as int
Map<String, Integer> types = message.getTypesMap();
```

#### Lazy Caching for Performance

Maps with type conversion use lazy caching with volatile fields for thread-safe performance:

```java
private volatile Map<String, Integer> cachedStatusMap;

@Override
protected Map<String, Integer> extractStatusMap() {
    if (cachedStatusMap != null) {
        return cachedStatusMap;
    }
    // Perform conversion...
    cachedStatusMap = result;
    return result;
}
```

### Oneof Field Support

Full support for protobuf oneof fields with unified API across versions:

```java
// Check which field is set
PaymentRequest.MethodCase methodCase = request.getMethodCase();

switch (methodCase) {
    case CREDIT_CARD -> processCreditCard(request.getCreditCard());
    case BANK_TRANSFER -> processBankTransfer(request.getBankTransfer());
    case CRYPTO -> processCrypto(request.getCrypto());
    case METHOD_NOT_SET -> handleNoMethod();
}

// Check individual fields
if (request.hasCreditCard()) {
    CreditCard card = request.getCreditCard();
}

// Clear oneof group in builder
builder.clearMethod();
```

### Comprehensive Conflict Type System

New conflict types for complete schema evolution handling:

| Conflict Type | Description | Unified Type |
|--------------|-------------|--------------|
| ENUM_ENUM | Different enum values across versions | int |
| FLOAT_DOUBLE | float/double precision differences | double |
| SIGNED_UNSIGNED | int32/uint32, sint32, etc. | long |
| REPEATED_SINGLE | repeated vs singular fields | List |
| PRIMITIVE_MESSAGE | Primitive to message type changes | Detection only |
| OPTIONAL_REQUIRED | Optional/required modifier differences | Optional |

### Exception Hierarchy

Structured exception classes for better error handling:

```java
try {
    schema = analyzer.analyze(protoFiles);
} catch (AnalysisException e) {
    // Proto file analysis errors
} catch (ConfigurationException e) {
    // Configuration validation errors
} catch (GenerationException e) {
    // Code generation errors
} catch (MergeException e) {
    // Schema merging errors
} catch (ProtoWrapperException e) {
    // Base exception for all plugin errors
}
```

### Builder Validation for Enum Maps

Invalid enum values in map builders throw clear error messages:

```java
// Throws IllegalArgumentException with message:
// "Invalid value 999 for StatusType in field 'statusMap'. Valid values: [ACTIVE(1), INACTIVE(2), PENDING(3)]"
builder.putStatusMap("key", 999);
```

## Critical Bug Fixes

### Fixed: ordinal() vs getNumber() in Map Enum Conversion

**Issue:** Map fields with enum values used `ordinal()` for conversion, causing data corruption when enums had non-sequential values (e.g., `ACTIVE=1, INACTIVE=5, PENDING=10`).

**Fix:** Changed to use `getNumber()` which returns the actual proto field number.

```java
// Before (WRONG - caused data corruption)
result.put(k, ((ProtocolMessageEnum) v).ordinal());

// After (CORRECT)
result.put(k, ((ProtocolMessageEnum) v).getNumber());
```

### Fixed: NPE for Invalid Enum Values

**Issue:** Putting an invalid enum value in map builders caused NullPointerException.

**Fix:** Added validation with clear error messages:

```java
StatusType enumValue = StatusType.forNumber(value);
if (enumValue == null) {
    throw new IllegalArgumentException(
        "Invalid value " + value + " for StatusType...");
}
```

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.2.0</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.2.0"
}
```

### 2. Regenerate Code

**Maven:**
```bash
mvn clean compile
```

**Gradle:**
```bash
./gradlew clean generateProtoWrapper
```

### 3. Update Exception Handling (Optional)

If you catch plugin exceptions, update to use new exception hierarchy:

```java
// Old (still works)
catch (RuntimeException e) { ... }

// New (recommended)
catch (ProtoWrapperException e) { ... }
```

## Breaking Changes

None. All changes are backward compatible.

## Deprecated API

The following APIs are deprecated and will be removed in version 2.0.0:

| Deprecated | Replacement |
|------------|-------------|
| `InterfaceGenerator.setSchema()` | Use `GenerationContext` |
| `InterfaceGenerator.generate(MergedMessage)` | Use `generate(GenerationContext)` |
| `MergedField(FieldInfo, String)` constructor | Use `MergedField.create()` factory |
| `ProtocExecutor(Consumer<String>)` | Use `ProtocExecutor(PluginLogger)` |

## Test Coverage

| Category | Tests | Description |
|----------|-------|-------------|
| Map Fields | 18 | Type conflicts, caching, builders |
| Oneof Fields | 24 | Case detection, clearing, conflicts |
| Conflict Handlers | 32 | All conflict types |
| Exception Hierarchy | 12 | Exception classes and messages |
| Integration Tests | 120+ | End-to-end scenarios |

## Known Limitations

### Oneof Fields
- Renamed oneofs use the most common name across versions
- Fields in oneofs that exist only in some versions return null/default in other versions

### Detected Conflicts (Not Auto-Resolved)
- PRIMITIVE_MESSAGE conflicts are detected but require manual handling
- Renaming oneofs between versions is supported but may need configuration

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [API Reference](docs/API_REFERENCE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Cookbook](docs/COOKBOOK.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
