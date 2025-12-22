# Release Notes — Proto Wrapper Maven Plugin v1.0.3

**Release Date:** December 21, 2025

## Overview

This release introduces a multi-module architecture, comprehensive integration tests, and critical bug fixes. The project is now split into `proto-wrapper-core` and `proto-wrapper-maven-plugin` modules for better separation of concerns and potential reuse of the core library.

## What's New

### Multi-Module Architecture

The project has been restructured into separate Maven modules:

```
proto-wrapper-plugin/
├── proto-wrapper-core/           # Core library (no Maven dependencies)
│   └── src/main/java/
│       └── space/alnovis/protowrapper/
│           ├── analyzer/         # Proto file analysis
│           ├── generator/        # Code generation
│           ├── merger/           # Schema merging
│           └── model/            # Data models
├── proto-wrapper-maven-plugin/   # Maven plugin
│   └── src/main/java/
│       └── space/alnovis/protowrapper/mojo/
└── examples/maven-example/       # Example with tests
```

**Benefits:**
- Core library can be used independently of Maven
- Cleaner dependency management
- Easier testing and maintenance
- Foundation for future Gradle plugin

### Integration Tests

Added 75 comprehensive tests in `examples/maven-example/` that validate generated code against proto files:

| Test Class | Tests | Purpose |
|------------|-------|---------|
| `InterfaceGenerationTest` | 16 | Validates interfaces have correct methods |
| `EnumGenerationTest` | 15 | Validates enum values (top-level and nested) |
| `FieldMappingTest` | 13 | Validates proto→wrapper field mapping |
| `VersionContextTest` | 16 | Validates VersionContext factory |
| `VersionEvolutionTest` | 13 | Validates V1/V2 compatibility |

Tests run automatically as part of the main build:

```bash
mvn clean test
# Runs: core tests (70) + example integration tests (75) = 145 tests
```

### Example Project

A fully working example in `examples/maven-example/` demonstrates all plugin features:

```
examples/maven-example/
├── proto/
│   ├── v1/                    # Version 1 proto files
│   │   ├── common.proto       # Date, Money, Address, Status
│   │   ├── order.proto        # OrderItem, OrderRequest, OrderResponse
│   │   └── user.proto         # UserProfile, AuthRequest, AuthResponse
│   └── v2/                    # Version 2 with additional fields/enums
├── src/main/java/
│   └── com/example/demo/
│       └── ProtoWrapperDemo.java   # Usage examples
└── src/test/java/
    └── com/example/model/     # Integration tests
```

Run the demo:

```bash
cd examples/maven-example
mvn exec:java -Dexec.mainClass=com.example.demo.ProtoWrapperDemo
```

## Bug Fixes

### Enum Generation Bug

**Problem:** Enum files were not being generated despite logs showing "Generated N enums".

**Cause:** Java 9+ optimizes `stream().map().count()` to skip intermediate operations when the stream size is known. The `map()` operation (which performed file generation) was being skipped.

**Fix:** Changed `GenerationOrchestrator.generateEnums()` from:

```java
// Before (broken in Java 9+)
return schema.getEnums().stream()
    .map(enumInfo -> {
        generator.generateAndWrite(enumInfo);  // SKIPPED!
        return 1;
    })
    .count();
```

To:

```java
// After (works correctly)
int[] count = {0};
schema.getEnums().forEach(enumInfo -> {
    generator.generateAndWrite(enumInfo);
    count[0]++;
});
return count[0];
```

### Package Name Truncation Bug

**Problem:** Nested types were not resolving correctly (e.g., `SessionInfo` instead of `AuthResponse.SessionInfo`).

**Cause:** `TypeResolver.extractProtoPackage()` was incorrectly truncating package names using `Arrays.copyOfRange()`, resulting in:
- Input: `com.example.proto.v1`
- Output: `example.proto.v1` (wrong!)

**Fix:** Simplified method to just replace the `{version}` placeholder:

```java
public String extractProtoPackage(String pattern) {
    if (pattern == null || pattern.isEmpty()) {
        return "";
    }
    return pattern.replace("{version}", "v1");
}
```

## Upgrade Guide

### 1. Update Plugin Version

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.3</version>
</plugin>
```

### 2. Verify Build

```bash
mvn clean compile
```

### 3. (Optional) Run Integration Tests

Clone the repository and run:

```bash
mvn clean test
```

## API Compatibility

No breaking changes to the generated code or configuration. All existing setups remain valid.

## Dependencies

No new runtime dependencies. The core module has minimal dependencies:
- `protobuf-java` — For proto descriptor parsing
- `javapoet` — For Java code generation

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [Changelog](CHANGELOG.md)
- [Example Project](examples/maven-example/)

## License

Apache License 2.0
