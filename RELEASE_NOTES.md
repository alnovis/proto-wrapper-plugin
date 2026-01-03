# Release Notes - Proto Wrapper Plugin v1.3.0

**Release Date:** January 2, 2026

## Overview

This release introduces support for Google Well-Known Types, automatically converting protobuf wrapper types, timestamps, durations, and JSON-like structures to idiomatic Java types.

## What's New

### Google Well-Known Types Support

15 Well-Known Types are now automatically converted to Java types:

#### Temporal Types

```java
// Proto definition:
// google.protobuf.Timestamp created_at = 1;
// google.protobuf.Duration timeout = 2;

// Generated interface:
Instant getCreatedAt();      // java.time.Instant
Duration getTimeout();        // java.time.Duration
```

#### Wrapper Types (Nullable Primitives)

```java
// Proto definition:
// google.protobuf.StringValue optional_name = 1;
// google.protobuf.Int32Value optional_count = 2;

// Generated interface:
String getOptionalName();     // null if not set
Integer getOptionalCount();   // null if not set
```

#### Complete Type Mapping

| Proto Type | Java Type | Notes |
|------------|-----------|-------|
| `google.protobuf.Timestamp` | `java.time.Instant` | |
| `google.protobuf.Duration` | `java.time.Duration` | |
| `google.protobuf.StringValue` | `String` | nullable |
| `google.protobuf.Int32Value` | `Integer` | nullable |
| `google.protobuf.Int64Value` | `Long` | nullable |
| `google.protobuf.UInt32Value` | `Long` | unsigned, nullable |
| `google.protobuf.UInt64Value` | `Long` | unsigned, nullable |
| `google.protobuf.BoolValue` | `Boolean` | nullable |
| `google.protobuf.FloatValue` | `Float` | nullable |
| `google.protobuf.DoubleValue` | `Double` | nullable |
| `google.protobuf.BytesValue` | `byte[]` | nullable |
| `google.protobuf.FieldMask` | `List<String>` | path list |
| `google.protobuf.Struct` | `Map<String, Object>` | JSON-like |
| `google.protobuf.Value` | `Object` | dynamic |
| `google.protobuf.ListValue` | `List<Object>` | dynamic |

### JSON-like Structures (Struct/Value/ListValue)

Full support for dynamic JSON-like structures:

```java
// Proto definition:
// google.protobuf.Struct metadata = 1;
// google.protobuf.Value dynamic_field = 2;

// Generated interface:
Map<String, Object> getMetadata();
Object getDynamicField();

// Usage:
Map<String, Object> metadata = message.getMetadata();
String name = (String) metadata.get("name");
Double count = (Double) metadata.get("count");
List<?> items = (List<?>) metadata.get("items");
```

### StructConverter Utility Class

When Struct/Value/ListValue fields are used, a utility class is auto-generated:

```java
// Generated in your API package
public final class StructConverter {
    // Struct <-> Map conversion
    public static Map<String, Object> toMap(Struct struct);
    public static Struct toStruct(Map<String, ?> map);

    // Value <-> Object conversion
    public static Object toObject(Value value);
    public static Value toValue(Object obj);

    // ListValue <-> List conversion
    public static List<Object> toList(ListValue listValue);
    public static ListValue toListValue(List<?> list);
}
```

### Configuration Options

Two new configuration options:

```xml
<!-- Maven -->
<configuration>
    <!-- Enable/disable WKT conversion (default: true) -->
    <convertWellKnownTypes>true</convertWellKnownTypes>

    <!-- Generate getXxxProto() methods (default: false) -->
    <generateRawProtoAccessors>false</generateRawProtoAccessors>
</configuration>
```

```kotlin
// Gradle
protoWrapper {
    convertWellKnownTypes = true
    generateRawProtoAccessors = false
}
```

### Repeated Well-Known Types

Lists of well-known types are also converted:

```java
// Proto definition:
// repeated google.protobuf.Timestamp events = 1;

// Generated interface:
List<Instant> getEvents();  // List<java.time.Instant>
```

### Builder Support

Full builder support for all well-known types:

```java
Event event = Event.newBuilder(ctx)
    .setCreatedAt(Instant.now())
    .setTimeout(Duration.ofMinutes(5))
    .setOptionalName("test-event")
    .setMetadata(Map.of(
        "key1", "value1",
        "key2", 123.45,
        "nested", Map.of("a", "b")
    ))
    .build();
```

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.3.0</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.3.0"
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

### 3. Update Code to Use Java Types

If you were using protobuf types directly, update to Java types:

```java
// Before (v1.2.0):
Timestamp createdAt = message.getCreatedAt();
long seconds = createdAt.getSeconds();

// After (v1.3.0):
Instant createdAt = message.getCreatedAt();
long seconds = createdAt.getEpochSecond();
```

### 4. Disable Conversion (If Needed)

If you prefer the old behavior:

```xml
<convertWellKnownTypes>false</convertWellKnownTypes>
```

## Breaking Changes

**Behavioral change:** Well-known type fields now return Java types instead of protobuf types by default.

To maintain old behavior, set `convertWellKnownTypes=false`.

## Not Supported

### google.protobuf.Any

`Any` type is not supported because it requires a runtime type registry to unpack. This conflicts with our design principle of inline code with no runtime dependencies.

Workaround: Use raw proto accessor with `generateRawProtoAccessors=true`:

```java
// In generated code:
com.google.protobuf.Any getPayloadProto();

// Usage:
Any any = message.getPayloadProto();
if (any.is(User.class)) {
    User user = any.unpack(User.class);
}
```

## Design Decisions

1. **Inline conversion code** - No runtime dependencies required
2. **StructConverter generated only when needed** - Reduces generated code size
3. **Default enabled** - Most users expect Java types
4. **Nullable wrappers** - Wrapper types can distinguish "not set" from default value

## Test Coverage

| Category | Tests | Description |
|----------|-------|-------------|
| WellKnownTypeInfo | 15 | All type mappings |
| WellKnownTypeHandler | 12 | Scalar field handling |
| RepeatedWellKnownTypeHandler | 8 | List field handling |
| HandlerType | 13 | All handler types |
| Integration Tests | 349+ | End-to-end scenarios |

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
