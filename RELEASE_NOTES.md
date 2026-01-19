# Release Notes - Proto Wrapper Plugin v1.6.9

**Release Date:** January 19, 2026

## Overview

Version 1.6.9 introduces **string-based version identifiers** for wrapper instances and extends the **JavaVersionCodegen strategy** to additional generators, centralizing all Java version-specific code generation logic.

## What's New

### String-Based Version Identifiers for Wrappers

Wrapper instances now support `getWrapperVersionId()` method that returns the version identifier as a string:

```java
// New recommended API (v1.6.9+)
String versionId = wrapper.getWrapperVersionId();  // "v1", "v2", "legacy", etc.
if ("v2".equals(versionId)) {
    // V2-specific handling
}

// Deprecated API (still works)
int version = wrapper.getWrapperVersion();  // @Deprecated since 1.6.9
```

#### Why String Identifiers?

- **Custom version names**: Supports non-numeric versions like `"legacy"`, `"v2beta"`, `"production"`
- **Consistency**: Matches VersionContext API (`getVersionId()`) and plugin configuration
- **Reliability**: Integer extraction is unreliable for non-numeric versions

#### Generated Interface Changes

```java
public interface Order {
    // New primary method (v1.6.9+)
    String getWrapperVersionId();

    // Deprecated method (will be removed in v2.0)
    @Deprecated(since = "1.6.9", forRemoval = true)
    int getWrapperVersion();

    // ... other methods
}
```

### Extended JavaVersionCodegen Strategy

The `JavaVersionCodegen` interface has been extended with common code generation methods:

```java
public interface JavaVersionCodegen {
    // Existing methods
    FieldSpec createContextsField(...);
    FieldSpec createSupportedVersionsField(...);
    Optional<MethodSpec> createContextsMethod(...);
    boolean requiresHelperClass();

    // New methods (v1.6.9)
    AnnotationSpec deprecatedAnnotation(String since, boolean forRemoval);
    CodeBlock immutableListOf(String... elements);
    CodeBlock immutableSetOf(String... elements);
    boolean supportsPrivateInterfaceMethods();
}
```

#### Java 8 vs Java 9+ Differences

| Method | Java 8 | Java 9+ |
|--------|--------|---------|
| `deprecatedAnnotation()` | `@Deprecated` | `@Deprecated(since="...", forRemoval=...)` |
| `immutableListOf()` | `Collections.unmodifiableList(Arrays.asList(...))` | `List.of(...)` |
| `immutableSetOf()` | `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))` | `Set.of(...)` |
| `supportsPrivateInterfaceMethods()` | `false` | `true` |

#### Refactored Generators

The following generators now use `JavaVersionCodegen` strategy:

- `ProtoWrapperGenerator` - uses `deprecatedAnnotation()` and `immutableListOf()`
- `AbstractClassGenerator` - uses `deprecatedAnnotation()` and `immutableListOf()`
- `VersionContextGenerator` - already used strategy (v1.6.8)

This eliminates code duplication and ensures consistent Java version-specific code generation across all generators.

### Build Automation Script

New `build.sh` script automates version management and builds:

```bash
# Check version consistency across all files
./build.sh --check

# Update version in all files
./build.sh --bump 1.7.0

# Quick build (skip tests)
./build.sh --quick

# Run build with parallel execution
./build.sh --parallel

# CI-friendly output (no colors, exit on error)
./build.sh --ci

# Full build with tests (default)
./build.sh
```

The script:
- Checks version consistency across pom.xml, build.gradle.kts, examples, and integration tests
- Builds Maven modules (core, maven plugin, tests, examples)
- Builds Gradle plugin and runs tests
- Runs standalone Gradle integration tests
- Supports parallel execution and CI mode

## Migration Guide

### From 1.6.8

1. Update version number in your build file
2. Replace `getWrapperVersion()` with `getWrapperVersionId()`:

**Before:**
```java
if (wrapper.getWrapperVersion() == 2) {
    // V2 handling
}
```

**After:**
```java
if ("v2".equals(wrapper.getWrapperVersionId())) {
    // V2 handling
}
```

3. No other changes required - existing code using `getWrapperVersion()` will continue to work with deprecation warnings

## Deprecations

| Deprecated | Since | Replacement | Removal |
|------------|-------|-------------|---------|
| `wrapper.getWrapperVersion()` | 1.6.9 | `wrapper.getWrapperVersionId()` | v2.0 |

## Breaking Changes

None. All existing APIs remain compatible.

## New Tests

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `JavaVersionCodegenTest` | 18 | Extended strategy methods |
| `ProtoWrapperGeneratorTest` | Updated | Strategy usage verification |
| `AbstractClassGeneratorTest` | Updated | Strategy usage verification |

## Documentation

- [Configuration Guide](docs/CONFIGURATION.md) - All configuration parameters
- [API Reference](docs/API_REFERENCE.md) - Generated code reference with `getWrapperVersionId()`
- [Cookbook](docs/COOKBOOK.md) - Updated examples using new API

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.
