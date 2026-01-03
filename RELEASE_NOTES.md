# Release Notes - Proto Wrapper Plugin v1.4.0

**Release Date:** January 3, 2026

## Overview

This release adds full builder support for repeated fields with type conflicts across versions. Previously, repeated fields with conflicts (like `repeated int32` in v1 vs `repeated int64` in v2) only had getter methods. Now they have complete builder support with runtime range validation.

## What's New

### Repeated Conflict Field Builders

Full builder support for repeated fields with type conflicts:

```java
// Proto definition:
// v1: repeated int32 numbers = 1;
// v2: repeated int64 numbers = 1;

// Generated interface:
interface RepeatedConflicts {
    List<Long> getNumbers();

    interface Builder {
        Builder addNumbers(long value);
        Builder addAllNumbers(List<Long> values);
        Builder setNumbers(List<Long> values);
        Builder clearNumbers();
    }
}
```

### Supported Conflict Types

| Conflict | v1 Type | v2 Type | Unified Type | Range Validation |
|----------|---------|---------|--------------|------------------|
| **WIDENING** | `repeated int32` | `repeated int64` | `List<Long>` | Yes |
| **FLOAT_DOUBLE** | `repeated float` | `repeated double` | `List<Double>` | Yes |
| **SIGNED_UNSIGNED** | `repeated int32` | `repeated uint32` | `List<Long>` | Yes |
| **INT_ENUM** | `repeated int32` | `repeated SomeEnum` | `List<Integer>` | No |
| **STRING_BYTES** | `repeated string` | `repeated bytes` | `List<String>` | No |

### Range Validation

When adding values to a version with a narrower type, runtime validation ensures values fit:

```java
// V1 builder (int32 range):
wrapper.toBuilder()
    .addNumbers(100L)              // OK - within int32 range
    .addNumbers(2_147_483_647L)    // OK - max int32
    .addNumbers(9_999_999_999L)    // throws IllegalArgumentException
    .build();

// V2 builder (int64 range):
wrapper.toBuilder()
    .addNumbers(9_999_999_999L)    // OK - within int64 range
    .build();
```

Error messages are clear and actionable:

```
IllegalArgumentException: Value 9999999999 exceeds int32 range for v1
IllegalArgumentException: Value 1.0E309 exceeds float range for v1
IllegalArgumentException: Value -1 exceeds uint32 range for v2
```

### Builder Methods

Four new builder methods for each repeated conflict field:

| Method | Description |
|--------|-------------|
| `addXxx(T value)` | Add single element with validation |
| `addAllXxx(List<T> values)` | Add multiple elements with validation |
| `setXxx(List<T> values)` | Replace all elements with validation |
| `clearXxx()` | Clear all elements |

### Usage Examples

#### Basic Usage

```java
// Create new message with repeated conflict field
RepeatedConflicts message = RepeatedConflicts.newBuilder(ctx)
    .addNumbers(100L)
    .addNumbers(200L)
    .addNumbers(300L)
    .build();

assertThat(message.getNumbers()).containsExactly(100L, 200L, 300L);
```

#### Modify Existing Message

```java
// Modify existing message
RepeatedConflicts modified = message.toBuilder()
    .addAllNumbers(List.of(400L, 500L))
    .build();

assertThat(modified.getNumbers()).containsExactly(100L, 200L, 300L, 400L, 500L);
```

#### Replace All Values

```java
// Replace all values
RepeatedConflicts replaced = message.toBuilder()
    .setNumbers(List.of(1L, 2L, 3L))
    .build();

assertThat(replaced.getNumbers()).containsExactly(1L, 2L, 3L);
```

#### Clear Values

```java
// Clear all values
RepeatedConflicts cleared = message.toBuilder()
    .clearNumbers()
    .build();

assertThat(cleared.getNumbers()).isEmpty();
```

### FLOAT_DOUBLE Conflicts

```java
// v1: repeated float values = 1;
// v2: repeated double values = 2;

// V1 builder validates float range:
wrapper.toBuilder()
    .addValues(3.14)           // OK
    .addValues(Float.MAX_VALUE) // OK
    .addValues(Double.MAX_VALUE) // throws - exceeds float range
    .build();
```

### STRING_BYTES Conflicts

```java
// v1: repeated string texts = 1;
// v2: repeated bytes texts = 2;

// Both versions accept String:
wrapper.toBuilder()
    .addTexts("Hello")
    .addTexts("World")
    .build();

// UTF-8 encoding is handled automatically
```

### INT_ENUM Conflicts

```java
// v1: repeated int32 codes = 1;
// v2: repeated Status codes = 2;

// Use integer values (enum ordinals):
wrapper.toBuilder()
    .addCodes(1)  // Status.ACTIVE
    .addCodes(2)  // Status.INACTIVE
    .build();
```

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.4.0</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.4.0"
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

### 3. Use New Builder Methods

Previously skipped repeated conflict fields now have builder methods:

```java
// Before (v1.3.0): No builder methods for repeated conflicts
// Compiler error: addNumbers() method not found

// After (v1.4.0): Full builder support
message.toBuilder()
    .addNumbers(100L)
    .addAllNumbers(List.of(200L, 300L))
    .build();
```

## Breaking Changes

None. This release is fully backward compatible.

## Implementation Details

### New Handler Methods

- `RepeatedConflictHandler.addAbstractBuilderMethods()` - Generates abstract doXxx methods
- `RepeatedConflictHandler.addBuilderImplMethods()` - Generates version-specific implementations
- `RepeatedConflictHandler.addConcreteBuilderMethods()` - Generates public wrapper methods

### Generated Code Structure

```java
// Interface (api package)
interface RepeatedConflicts {
    interface Builder {
        Builder addNumbers(long value);
        Builder addAllNumbers(List<Long> values);
        Builder setNumbers(List<Long> values);
        Builder clearNumbers();
    }
}

// Abstract class (api/impl package)
abstract class AbstractRepeatedConflicts {
    abstract class AbstractBuilder {
        protected abstract void doAddNumbers(long value);
        protected abstract void doAddAllNumbers(List<Long> values);
        protected abstract void doSetNumbers(List<Long> values);
        protected abstract void doClearNumbers();

        public final Builder addNumbers(long value) {
            doAddNumbers(value);
            return this;
        }
        // ... other methods
    }
}

// V1 Implementation (v1 package)
class RepeatedConflicts extends AbstractRepeatedConflicts {
    class BuilderImpl extends AbstractBuilder {
        @Override
        protected void doAddNumbers(long value) {
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Value " + value + " exceeds int32 range for v1");
            }
            protoBuilder.addNumbers((int) value);
        }
        // ... other methods
    }
}
```

## Test Coverage

35 comprehensive tests in `RepeatedConflictBuilderTest`:

| Category | Tests | Description |
|----------|-------|-------------|
| WIDENING | 10 | int32/int64 with range validation |
| FLOAT_DOUBLE | 7 | float/double with precision limits |
| INT_ENUM | 3 | int32/enum conversion |
| STRING_BYTES | 4 | string/bytes with UTF-8 |
| SIGNED_UNSIGNED | 8 | signed/unsigned bounds |
| Mixed Operations | 3 | Multiple field combinations |

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
