# Release Notes - Proto Wrapper Plugin v1.6.7

**Release Date:** January 18, 2026

## Overview

Version 1.6.7 introduces the **String-based VersionContext API** and **Spring Boot Starter** for seamless integration with Spring Boot applications.

## What's New

### String-based VersionContext API

The new API uses string identifiers instead of integers, providing better flexibility for non-numeric version names:

```java
// NEW: String-based API (recommended)
VersionContext ctx = VersionContext.forVersionId("v1");
String versionId = ctx.getVersionId();  // "v1"

// OLD: Integer-based API (deprecated)
VersionContext ctx = VersionContext.forVersion(1);
int version = ctx.getVersion();  // 1
```

#### New Static Methods on VersionContext

| Method | Return Type | Description |
|--------|-------------|-------------|
| `forVersionId(String)` | `VersionContext` | Get context by string ID (throws if invalid) |
| `find(String)` | `Optional<VersionContext>` | Safe lookup, returns empty if not found |
| `getDefault()` | `VersionContext` | Get the latest/default version |
| `supportedVersions()` | `List<String>` | All supported version IDs |
| `defaultVersion()` | `String` | Default version ID |
| `isSupported(String)` | `boolean` | Check if version is supported |

#### Usage Examples

```java
// Get specific version
VersionContext v1 = VersionContext.forVersionId("v1");

// Safe lookup with Optional
Optional<VersionContext> maybeCtx = VersionContext.find("v1");
maybeCtx.ifPresent(ctx -> processWithVersion(ctx));

// Get default (latest) version
VersionContext latest = VersionContext.getDefault();

// Check supported versions
List<String> versions = VersionContext.supportedVersions();  // ["v1", "v2"]
boolean supported = VersionContext.isSupported("v3");        // false

// Get default version ID
String defaultVer = VersionContext.defaultVersion();         // "v2"
```

### Spring Boot Starter

New `proto-wrapper-spring-boot-starter` module provides auto-configuration for Spring Boot:

#### Installation

```xml
<dependency>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-spring-boot-starter</artifactId>
    <version>1.6.7</version>
</dependency>
```

#### Configuration

```yaml
proto-wrapper:
  base-package: com.example.model.api
  version-header: X-Protocol-Version  # optional, default
  default-version: v2                  # optional
```

#### Features

- **RequestScopedVersionContext** - Inject version context per HTTP request
- **VersionContextRequestFilter** - Extract version from HTTP headers
- **VersionContextProvider** - Programmatic access to all version contexts
- **ProtoWrapperExceptionHandler** - Unified error handling

#### Usage in Controllers

```java
@RestController
public class OrderController {
    private final RequestScopedVersionContext versionContext;
    private final VersionContextProvider provider;

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        String version = versionContext.getVersion();
        // Use version-specific logic...
    }

    @GetMapping("/versions")
    public List<String> getSupportedVersions() {
        return provider.getSupportedVersions();
    }
}
```

## Deprecated API

The following methods are deprecated and marked for removal:

| Deprecated | Replacement |
|------------|-------------|
| `VersionContext.forVersion(int)` | `VersionContext.forVersionId(String)` |
| `VersionContext.getVersion()` | `VersionContext.getVersionId()` |

## Upgrade Guide

### From 1.6.6

1. Update version number in your build file
2. Replace `forVersion(int)` with `forVersionId(String)`:
   ```java
   // Before
   VersionContext ctx = VersionContext.forVersion(1);

   // After
   VersionContext ctx = VersionContext.forVersionId("v1");
   ```
3. Replace `getVersion()` with `getVersionId()`:
   ```java
   // Before
   int version = ctx.getVersion();

   // After
   String versionId = ctx.getVersionId();
   ```

### Adding Spring Boot Starter

1. Add the starter dependency
2. Configure `proto-wrapper.base-package` in `application.yml`
3. Inject `RequestScopedVersionContext` or `VersionContextProvider`

## Breaking Changes

None. The deprecated methods continue to work.

## Documentation

- [Spring Boot Starter Guide](docs/SPRING_BOOT_STARTER.md) - Full integration documentation
- [API Reference](docs/API_REFERENCE.md) - Generated code reference

## Examples

- [Maven Example](examples/maven-example) - Complete Maven project with tests
- [Gradle Example](examples/gradle-example) - Complete Gradle project
- [Spring Boot Example](examples/spring-boot-example) - Spring Boot integration

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.
