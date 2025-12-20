# Release Notes — Proto Wrapper Maven Plugin v1.0.2

**Release Date:** December 20, 2025

## Overview

This release brings significant code modernization with a focus on functional programming patterns and Java 17+ features. The codebase has been refactored to use Stream API, records, and modern collection utilities for improved readability and maintainability.

## Breaking Changes

### Java Version Requirement

**Minimum Java version is now 17** (previously 11).

This change was necessary to leverage:
- **Records** — Immutable data carriers for helper classes
- **Modern Stream API enhancements** — `Optional.stream()`, improved collectors
- **Text blocks** — Better string handling (future use)

## What's New

### New Configuration: `includeVersionSuffix`

Control whether generated implementation classes include version suffix:

```xml
<configuration>
    <!-- Default: true (backward compatible) -->
    <includeVersionSuffix>false</includeVersionSuffix>
</configuration>
```

| Setting | Generated Class | Package |
|---------|----------------|---------|
| `true` (default) | `MoneyV1`, `MoneyV2` | `com.example.model.v1` |
| `false` | `Money`, `Money` | `com.example.model.v1`, `com.example.model.v2` |

### Unified Logging: `PluginLogger`

New logging interface replaces direct `Consumer<String>` usage:

```java
// Maven plugin integration
PluginLogger logger = PluginLogger.maven(getLog());

// Standalone/testing
PluginLogger logger = PluginLogger.console();

// Silent mode
PluginLogger logger = PluginLogger.noop();

// Custom handler
PluginLogger logger = PluginLogger.fromConsumer(msg -> myLogger.log(msg));
```

Logging levels: `info()`, `warn()`, `debug()`, `error()`

### Functional Refactoring

The entire codebase has been modernized to follow functional programming paradigms:

| Metric | Before | After |
|--------|--------|-------|
| Imperative loops | ~45 | ~10 |
| Stream API usage | ~5% | ~70% |
| Records | 0 | 2 |
| Helper inner classes | 2 | 0 |

### Java 17 Records

New immutable record types replace verbose inner classes:

```java
// Before
private static class FieldWithVersion {
    private final FieldInfo field;
    private final String version;
    // constructor, getters...
}

// After
private record FieldWithVersion(FieldInfo field, String version) {}
```

### Stream API Patterns

Modern functional patterns throughout the codebase:

```java
// groupingBy for collecting
Map<Integer, List<FieldWithVersion>> fieldsByNumber = schemas.stream()
    .flatMap(schema -> schema.getMessage(messageName).stream()
        .flatMap(msg -> msg.getFields().stream()
            .map(field -> new FieldWithVersion(field, schema.getVersion()))))
    .collect(Collectors.groupingBy(
        fv -> fv.field().getNumber(),
        LinkedHashMap::new,
        Collectors.toList()
    ));

// reduce for nested class names
return Arrays.stream(parts)
    .skip(1)
    .reduce(
        ClassName.get(config.getApiPackage(), parts[0]),
        ClassName::nestedClass,
        (a, b) -> b
    );
```

### Refactored Components

| Component | Changes |
|-----------|---------|
| `VersionMerger` | `flatMap`, `groupingBy`, `Optional.stream()`, records |
| `GenerationOrchestrator` | `ThrowingSupplier` interface, stream-based generation |
| `MergedMessage` | Stream-based recursive search and filtering |
| `TypeResolver` | `reduce()` for class name building |
| `JavaTypeMapping` | `Set.of()` for immutable primitive set |

## Upgrade Guide

### 1. Update Java Version

Ensure your project uses Java 17 or higher:

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### 2. Update Plugin Version

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.2</version>
    <!-- ... -->
</plugin>
```

### 3. Verify Build

```bash
mvn clean compile
```

## API Compatibility

No changes to the generated code or public API. All existing configurations remain valid.

## Performance

The functional refactoring maintains the same performance characteristics while improving:
- Code readability
- Maintainability
- Reduced cognitive complexity

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation (EN)](README.md)
- [Documentation (RU)](README.ru.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
