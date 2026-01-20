# Release Notes - Proto Wrapper Plugin v2.0.0

**Release Date:** January 2026 (Upcoming)

## Overview

Version 2.0.0 is a **breaking change release** that removes all deprecated integer-based version APIs. The plugin now exclusively uses **string-based version identifiers** for all version-related operations.

## Breaking Changes

### Removed Methods

The following deprecated methods have been removed:

| Interface | Removed Method | Replacement |
|-----------|---------------|-------------|
| `ProtoWrapper` | `getWrapperVersion()` | `getWrapperVersionId()` |
| `ProtoWrapper` | `extractWrapperVersion()` | `extractWrapperVersionId()` |
| `VersionContext` | `getVersion()` | `getVersionId()` |
| `VersionContext` | `forVersion(int)` | `forVersionId(String)` |
| `Builder` | `getVersion()` | `getVersionId()` |

### Migration Guide

Update all usages of deprecated methods:

```java
// Before (v1.6.x)
int version = wrapper.getWrapperVersion();
VersionContext ctx = VersionContext.forVersion(1);
if (ctx.getVersion() == 2) { ... }

// After (v2.0.0)
String versionId = wrapper.getWrapperVersionId();  // "v1", "v2", etc.
VersionContext ctx = VersionContext.forVersionId("v1");
if ("v2".equals(ctx.getVersionId())) { ... }
```

### Benefits of String-Based API

- **Custom version names**: Supports non-numeric versions like `"legacy"`, `"v2beta"`, `"production"`
- **Consistency**: Single API style across all interfaces
- **Reliability**: No ambiguity with version number extraction from strings

### Generated Interface Changes

```java
public interface Order extends ProtoWrapper<Order> {
    // String-based version identifier (v2.0.0)
    String getWrapperVersionId();  // "v1", "v2", "legacy", etc.

    // Integer-based methods are REMOVED
    // int getWrapperVersion();  // REMOVED in v2.0.0
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
