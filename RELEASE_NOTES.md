# Release Notes - Proto Wrapper Plugin v1.6.0

**Release Date:** January 5, 2026

## Overview

Version 1.6.0 introduces **Incremental Generation** - a major feature that significantly reduces build times by only regenerating wrapper classes when proto files actually change. This release also adds thread-safe concurrent build support and comprehensive performance benchmarks.

## What's New

### Incremental Generation

The plugin now tracks proto file changes using SHA-256 content hashing and only regenerates affected wrapper classes. This provides:

- **>50% build time reduction** when no changes detected (benchmark shows 69.1%)
- **Dependency-aware regeneration** - tracks proto imports to regenerate dependents
- **Automatic cache invalidation** on plugin version or configuration changes

#### How It Works

```
First Build:
  Proto Files -> Fingerprint -> Generate All -> Save State

Subsequent Builds:
  Proto Files -> Compare Fingerprints -> Changed?
    No  -> Skip Generation (fast!)
    Yes -> Regenerate Affected Only -> Save State
```

#### Configuration

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.6.0</version>
    <configuration>
        <basePackage>com.example.model</basePackage>

        <!-- Incremental generation (enabled by default) -->
        <incremental>true</incremental>
        <cacheDirectory>${project.build.directory}/proto-wrapper-cache</cacheDirectory>

        <!-- Force full regeneration if needed -->
        <!-- <forceRegenerate>true</forceRegenerate> -->
    </configuration>
</plugin>
```

**Gradle:**
```kotlin
protoWrapper {
    basePackage.set("com.example.model")

    // Incremental generation (enabled by default)
    incremental.set(true)
    cacheDirectory.set(layout.buildDirectory.dir("proto-wrapper-cache"))

    // Force full regeneration if needed
    // forceRegenerate.set(true)
}
```

#### Command Line Options

```bash
# Maven - Force full regeneration
mvn compile -Dproto-wrapper.force=true

# Maven - Disable incremental
mvn compile -Dproto-wrapper.incremental=false

# Gradle - Force full regeneration (use --rerun-tasks or clean)
./gradlew clean generateProtoWrappers
```

#### New Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `incremental` | `true` | Enable incremental generation |
| `cacheDirectory` | `${build}/proto-wrapper-cache` | Directory for incremental state cache |
| `forceRegenerate` | `false` | Force full regeneration, ignoring cache |

### Thread-Safe Concurrent Builds

The plugin now supports concurrent builds using file locking:

- **CacheLock** mechanism prevents cache corruption during parallel builds
- Safe for CI/CD environments with multiple concurrent jobs
- Automatic lock acquisition with 30-second timeout
- Graceful recovery from stale locks

### Cache Invalidation Rules

The cache is automatically invalidated when:

| Condition | Action |
|-----------|--------|
| Plugin version changed | Full regeneration |
| Configuration changed | Full regeneration |
| Proto file modified | Regenerate affected + dependents |
| Proto file added | Regenerate new + dependents |
| Proto file deleted | Full regeneration |
| Cache corrupted | Graceful recovery, full regeneration |
| `forceRegenerate=true` | Full regeneration |

### Cache State File Format

The incremental state is stored in `state.json`:

```json
{
  "pluginVersion": "1.6.0",
  "configHash": "a1b2c3d4e5f67890",
  "lastGeneration": "2026-01-05T10:30:00.000Z",
  "protoFingerprints": {
    "v1/order.proto": {
      "relativePath": "v1/order.proto",
      "contentHash": "sha256:abc123...",
      "lastModified": 1707990000000,
      "fileSize": 2048
    }
  },
  "protoDependencies": {
    "v1/order.proto": ["v1/common.proto"]
  }
}
```

### Performance Benchmarks

Benchmark results with 22 proto files:

| Scenario | Time | Improvement |
|----------|------|-------------|
| Full generation | 13.73 ms | - |
| Incremental (no changes) | 4.25 ms | **69.1%** |
| Incremental (1 file changed) | 11.53 ms | 16.0% |

Run benchmarks:
```bash
mvn test -Dtest=IncrementalGenerationBenchmark -Dgroups=benchmark
```

## New Classes

### Incremental Generation Infrastructure

| Class | Description |
|-------|-------------|
| `CacheLock` | File locking for thread-safe cache access |
| `ChangeDetector` | Detects changes in proto files |
| `FileFingerprint` | SHA-256 content hash + timestamps |
| `GeneratedFileInfo` | Tracks generated file metadata |
| `IncrementalState` | Cache state model (JSON serializable) |
| `IncrementalStateManager` | Coordinates incremental generation |
| `ProtoDependencyGraph` | Tracks proto import dependencies |

### Test Classes

| Class | Description |
|-------|-------------|
| `IncrementalGenerationBenchmark` | Performance benchmarks |
| `CacheLockTest` | Thread-safety tests |
| `ChangeDetectorTest` | Change detection tests |
| `FileFingerprintTest` | Fingerprint computation tests |
| `IncrementalStateManagerTest` | State management tests |
| `IncrementalStateTest` | State serialization tests |
| `ProtoDependencyGraphTest` | Dependency tracking tests |
| `GenerationOrchestratorIncrementalTest` | Integration tests |
| `IncrementalGenerationIntegrationTest` | Maven IT tests |
| `GenerateWrappersTaskTest` | Gradle functional tests |
| `ProtoWrapperPluginTest` | Gradle plugin unit tests |

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.6.0</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.6.0"
}
```

### 2. (Optional) Configure Cache Directory

By default, the cache is stored in `${build.directory}/proto-wrapper-cache`. You can customize this:

```xml
<configuration>
    <cacheDirectory>${project.build.directory}/my-cache</cacheDirectory>
</configuration>
```

### 3. (Optional) Disable Incremental Generation

If you encounter issues, you can disable incremental generation:

```xml
<configuration>
    <incremental>false</incremental>
</configuration>
```

### 4. Clean Build for First Run

For the first build after upgrading, consider a clean build:

```bash
mvn clean compile
# or
./gradlew clean generateProtoWrappers
```

## Breaking Changes

None. This release is fully backward compatible. Incremental generation is enabled by default but can be disabled if needed.

## Test Coverage

| Module | Tests |
|--------|-------|
| proto-wrapper-core | 699 |
| proto-wrapper-gradle-plugin (unit) | 23 |
| proto-wrapper-gradle-plugin (functional) | 25 |
| proto-wrapper-maven-integration-tests | 7+ |

## Documentation Updates

- README.md - Added Incremental Generation section
- CHANGELOG.md - Added v1.6.0 section with full details
- Archived implementation plan to `docs/archive/`

---

# Previous Releases

---

# Release Notes - Proto Wrapper Plugin v1.5.2

**Release Date:** January 4, 2026

## Overview

Version 1.5.2 is a refactoring release that unifies the conflict detection architecture. The DiffTool now uses the same `VersionMerger` infrastructure as the code generator, ensuring consistent conflict classification across the entire plugin.

## Architecture Improvements

### Unified Conflict Detection

Previously, the plugin had two parallel systems for analyzing type conflicts:
1. **Generator** - Used `MergedField.ConflictType` for code generation
2. **DiffTool** - Used `TypeConflictType` with duplicated logic

Now both systems use a single source of truth: `MergedField.ConflictType` with enhanced capabilities.

### MergedField.ConflictType Enhancements

The `ConflictType` enum now includes:

```java
public enum ConflictType {
    NONE(Handling.NATIVE, "Types are identical"),
    INT_ENUM(Handling.CONVERTED, "Plugin uses int type with enum helper methods"),
    PRIMITIVE_MESSAGE(Handling.CONVERTED, "Plugin generates getXxx() and getXxxMessage() accessors"),
    INCOMPATIBLE(Handling.INCOMPATIBLE, "Incompatible type change"),
    // ... other types

    public enum Handling { NATIVE, CONVERTED, MANUAL, WARNING, INCOMPATIBLE }
    public enum Severity { INFO, WARNING, ERROR }

    public Handling getHandling() { ... }
    public Severity getSeverity() { ... }
    public boolean isPluginHandled() { ... }
    public String getPluginNote() { ... }
}
```

### New Adapter Class

`MergedSchemaDiffAdapter` converts `MergedSchema` to `SchemaDiff`, bridging the gap between the generator infrastructure and the diff reporting system.

## API Changes

### New Methods

```java
// Explicit use of VersionMerger infrastructure
SchemaDiff.compareViaMerger(v1, v2);
SchemaDiff.compareViaMerger(v1Dir, v2Dir, "v1", "v2");

// ConflictType methods
conflictType.getHandling();     // NATIVE, CONVERTED, MANUAL, WARNING, INCOMPATIBLE
conflictType.getSeverity();     // INFO, WARNING, ERROR
conflictType.isPluginHandled(); // true for NATIVE or CONVERTED
conflictType.getPluginNote();   // Human-readable description
```

### Deprecated (will be removed in v2.0)

- `SchemaDiff.compareLegacy()` - Use `compare()` instead
- `TypeConflictType` - Use `MergedField.ConflictType` instead
- `SchemaDiffEngine` - Replaced by `VersionMerger` + `MergedSchemaDiffAdapter`
- `BreakingChangeDetector` - Logic moved to `MergedSchemaDiffAdapter`

## Benefits

1. **Consistency** - Same conflict classification in both code generation and diff reports
2. **Maintainability** - Changes to conflict handling only need to be made in one place
3. **Reduced code** - Eliminated ~180 lines of duplicated logic
4. **Accuracy** - Breaking change severity is always consistent

---

## v1.5.1 - PRIMITIVE_MESSAGE Fix

Fixed the breaking change classification for PRIMITIVE_MESSAGE conflicts. Now correctly classified as INFO (plugin-handled) instead of ERROR.

---

## v1.5.0 - Schema Diff Tool

Introduced the **Schema Diff Tool** - a comprehensive solution for comparing protobuf schemas between versions and detecting breaking changes. Available as CLI, Maven goal, and Gradle task.

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

---

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [API Reference](docs/API_REFERENCE.md)
- [Roadmap](docs/ROADMAP.md)
- [Cookbook](docs/COOKBOOK.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
