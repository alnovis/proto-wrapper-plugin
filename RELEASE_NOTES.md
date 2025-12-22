# Release Notes — Proto Wrapper Maven Plugin v1.1.0

**Release Date:** December 22, 2025

## Overview

This release completes the major improvements plan with version conversion support, better error messages for INT_ENUM conflicts, improved `toString()` output, and proper `equals()`/`hashCode()` implementations.

## What's New

### Version Conversion (asVersion)

The `asVersion()` method now works for cross-version conversion:

```java
// Create Money in v1
Money v1 = VersionContext.forVersion(1).newMoneyBuilder()
        .setAmount(1000L)
        .setCurrency("USD")
        .build();

// Convert to v2
space.example.v2.Money v2 = v1.asVersion(space.example.v2.Money.class);

// v2 has same data
assertEquals(1000L, v2.getAmount());
assertEquals("USD", v2.getCurrency());
```

**Features:**
- Serialization-based conversion between versions
- Returns same instance if already target type (optimization)
- Works with all message types
- `parseXxxFromBytes()` methods in VersionContext

### Better INT_ENUM Error Messages (Phase 3)

Version-aware validation for INT_ENUM conflict fields:

```java
// v202 (int version) - any int value allowed
builder.setTaxType(200);  // OK

// v203 (enum version) - only valid enum values allowed
builder.setTaxType(999);  // throws IllegalArgumentException:
// "Invalid value 999 for TaxType. Valid values: [VAT(100), ...]"
```

**Features:**
- `fromProtoValueOrThrow(int)` method on unified enums
- Validation only for versions that use enum type
- Informative error messages with invalid value and valid options

### Improved toString() (Phase 2)

Better debugging output with proto content:

```java
Money money = ctx.newMoneyBuilder()
        .setAmount(1000)
        .setCurrency("USD")
        .build();

System.out.println(money);
// Money[version=1] amount: 1000, currency: "USD"
```

**Features:**
- Shows wrapper class name
- Shows protocol version
- Shows proto content (compact format)

### equals() and hashCode() (Phase 1)

Proper value-based equality:

```java
Money m1 = ctx.newMoneyBuilder().setAmount(100).setCurrency("USD").build();
Money m2 = ctx.newMoneyBuilder().setAmount(100).setCurrency("USD").build();

assertEquals(m1, m2);  // true - same content
assertEquals(m1.hashCode(), m2.hashCode());  // consistent
```

**Features:**
- Based on proto content and version
- Works in collections (List, Set, Map)
- Consistent hashCode for equals objects

## Upgrade Guide

### 1. Update Plugin Version

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.1.0</version>
</plugin>
```

### 2. Regenerate Code

```bash
mvn clean compile
```

### 3. Use New Features

```java
// Version conversion
MyMessageV2 v2 = v1.asVersion(MyMessageV2.class);

// Better debugging
System.out.println(wrapper);  // MyMessage[version=1] field1: "value", ...

// Value-based equality
if (message1.equals(message2)) { ... }

// Enum validation (for enum versions only)
try {
    builder.setStatus(invalidInt);
} catch (IllegalArgumentException e) {
    // Informative error message
}
```

## Breaking Changes

None. All changes are backward compatible.

## API Compatibility

- Generated code is fully compatible with previous versions
- New methods are additive only
- Existing code continues to work without changes

## Test Coverage

| Test Class | Tests | Purpose |
|------------|-------|---------|
| `VersionConversionTest` | 9 | Cross-version conversion |
| `IntEnumErrorMessageTest` | 8 | INT_ENUM validation |
| `ToStringTest` | 8 | toString() output |
| `EqualsHashCodeTest` | 14 | equals/hashCode |

**Total integration tests:** 135

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [Improvement Plan](docs/PLAN_IMPROVEMENTS.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
