# Release Notes - Proto Wrapper Plugin v1.6.8

**Release Date:** January 19, 2026

## Overview

Version 1.6.8 introduces **Java 8 compatibility** via the new `targetJavaVersion` parameter and includes a major **architectural refactoring** of the VersionContextGenerator using the Composer + Strategy pattern.

## What's New

### Java 8 Compatibility

The new `targetJavaVersion` parameter allows generating code compatible with Java 8 runtime:

```xml
<!-- Maven -->
<configuration>
    <targetJavaVersion>8</targetJavaVersion>
</configuration>
```

```kotlin
// Gradle
protoWrapper {
    targetJavaVersion.set(8)
}
```

#### Generated Code Differences

| Feature | Java 9+ (default) | Java 8 |
|---------|-------------------|--------|
| List creation | `List.of("v1", "v2")` | `Collections.unmodifiableList(Arrays.asList("v1", "v2"))` |
| CONTEXTS init | Private interface method | External `VersionContextHelper` class |

#### Java 9+ Generated Code (default)

```java
public interface VersionContext {
    Map<String, VersionContext> CONTEXTS = createContexts();
    List<String> SUPPORTED_VERSIONS = List.of("v1", "v2");

    private static Map<String, VersionContext> createContexts() {
        Map<String, VersionContext> map = new LinkedHashMap<>();
        map.put("v1", VersionContextV1.INSTANCE);
        map.put("v2", VersionContextV2.INSTANCE);
        return Collections.unmodifiableMap(map);
    }
}
```

#### Java 8 Generated Code

```java
public interface VersionContext {
    Map<String, VersionContext> CONTEXTS = VersionContextHelper.createContexts();
    List<String> SUPPORTED_VERSIONS =
        Collections.unmodifiableList(Arrays.asList("v1", "v2"));
}

// Separate helper class (private interface methods not available in Java 8)
final class VersionContextHelper {
    static Map<String, VersionContext> createContexts() {
        Map<String, VersionContext> map = new LinkedHashMap<>();
        map.put("v1", VersionContextV1.INSTANCE);
        map.put("v2", VersionContextV2.INSTANCE);
        return Collections.unmodifiableMap(map);
    }
}
```

### Architecture Refactoring

The `VersionContextGenerator` has been completely refactored using two design patterns:

#### Strategy Pattern: JavaVersionCodegen

```
JavaVersionCodegen (interface)
├── Java8Codegen      — Java 8 compatible code generation
└── Java9PlusCodegen  — Modern Java code generation (default)
```

#### Composer Pattern: VersionContextInterfaceComposer

Fluent API for building the VersionContext interface:

```java
JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
    .addStaticFields()      // CONTEXTS, SUPPORTED_VERSIONS, DEFAULT_VERSION
    .addStaticMethods()     // forVersionId, find, getDefault, etc.
    .addInstanceMethods()   // getVersionId, getVersion
    .addWrapMethods()       // wrapXxx, parseXxxFromBytes
    .addBuilderMethods()    // newXxxBuilder
    .addConvenienceMethods() // zeroMoney, createMoney
    .build();
```

#### Component Classes

| Component | Responsibility |
|-----------|---------------|
| `StaticFieldsComponent` | CONTEXTS, SUPPORTED_VERSIONS, DEFAULT_VERSION, createContexts() |
| `StaticMethodsComponent` | forVersionId, find, getDefault, supportedVersions, etc. |
| `InstanceMethodsComponent` | getVersionId, getVersion (abstract methods) |
| `WrapMethodsComponent` | wrapXxx, parseXxxFromBytes for each message |
| `BuilderMethodsComponent` | newXxxBuilder for each message |
| `ConvenienceMethodsComponent` | zeroMoney, createMoney (when applicable) |

#### Benefits

- **Reduced complexity**: `generateInterface()` method reduced from ~350 lines to ~10 lines
- **Single Responsibility**: Each component handles one aspect of code generation
- **Testability**: 62 new tests covering all components
- **Extensibility**: Easy to add new Java version strategies (Java 17+, Java 21+) in the future

## New Tests

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `JavaVersionCodegenTest` | 14 | Java8Codegen, Java9PlusCodegen, selection logic |
| `InterfaceComponentsTest` | 26 | All 6 component classes |
| `VersionContextInterfaceComposerTest` | 22 | Fluent API, selective composition, edge cases |
| **Total** | **62** | Full versioncontext package coverage |

## Upgrade Guide

### From 1.6.7

1. Update version number in your build file
2. No code changes required — default behavior unchanged
3. For Java 8 compatibility, add `targetJavaVersion`:

**Maven:**
```xml
<configuration>
    <targetJavaVersion>8</targetJavaVersion>
</configuration>
```

**Gradle:**
```kotlin
protoWrapper {
    targetJavaVersion.set(8)
}
```

## Breaking Changes

None. All existing APIs remain compatible.

## Configuration Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| `targetJavaVersion` | `9` | Target Java version (8 or 9+) |

## Documentation

- [Configuration Guide](docs/CONFIGURATION.md) - All configuration parameters
- [API Reference](docs/API_REFERENCE.md) - Generated code reference
- [ROADMAP](docs/ROADMAP.md) - Future Java version codegen strategies

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.
