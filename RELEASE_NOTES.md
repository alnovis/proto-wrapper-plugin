# Release Notes - Proto Wrapper Plugin v1.6.6

**Release Date:** January 16, 2026

## Overview

Version 1.6.6 introduces the **ProtoWrapper interface** - a common base interface for all generated wrapper classes that enables type-safe access to underlying protobuf messages without reflection.

## What's New

### ProtoWrapper Interface

All generated wrapper interfaces now extend a common `ProtoWrapper` interface:

```java
public interface ProtoWrapper {
    /**
     * Get the underlying protobuf message.
     * @return protobuf Message, never null
     */
    Message getTypedProto();

    /**
     * Get the protocol version this wrapper was created from.
     * @return version number (e.g., 1, 2)
     */
    int getWrapperVersion();

    /**
     * Serialize to protobuf bytes.
     * @return protobuf-encoded bytes
     */
    byte[] toBytes();
}
```

#### Benefits

1. **Zero reflection overhead** - Access wrapped proto directly via interface cast
2. **Polymorphic handling** - Process wrappers of different types uniformly
3. **Pattern matching** - Works with Java 16+ pattern matching
4. **Type safety** - Compile-time checked access to wrapper methods

#### Usage Examples

**Direct access (no reflection needed):**
```java
Request request = ctx.parseRequestFromBytes(bytes);
Message proto = request.getTypedProto();  // Direct method call
```

**Polymorphic processing:**
```java
void processWrapper(ProtoWrapper wrapper) {
    Message proto = wrapper.getTypedProto();
    int version = wrapper.getWrapperVersion();
    byte[] bytes = wrapper.toBytes();
}
```

**Pattern matching (Java 16+):**
```java
if (unknown instanceof ProtoWrapper pw) {
    Message proto = pw.getTypedProto();
    int version = pw.getWrapperVersion();
}
```

**Before (reflection required):**
```java
// Old approach with reflection
Method method = wrapper.getClass().getMethod("getTypedProto");
Message proto = (Message) method.invoke(wrapper);
```

**After (direct interface call):**
```java
// New approach - zero reflection
ProtoWrapper pw = (ProtoWrapper) wrapper;
Message proto = pw.getTypedProto();
```

### New Generator Class

- **`ProtoWrapperGenerator`** - Generates the `ProtoWrapper` interface in the API package alongside other generated code

### Architecture Changes

- `InterfaceGenerator` - Generated interfaces now extend `ProtoWrapper`
- `AbstractClassGenerator` - Added `getTypedProto()` implementation
- `ImplClassGenerator` - Added `@Override` to `getTypedProto()` (covariant return)

## Upgrade Guide

Simply update the version number. The change is fully backward compatible:

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.6.6</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.6.6"
}
```

## Breaking Changes

None. This release is fully backward compatible.

## Migration Notes

After regenerating your wrapper classes with v1.6.6:

1. All your wrapper interfaces will extend `ProtoWrapper`
2. You can now use `instanceof ProtoWrapper` checks
3. You can call `getTypedProto()`, `getWrapperVersion()`, and `toBytes()` via the `ProtoWrapper` interface
4. Existing code continues to work unchanged

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.
