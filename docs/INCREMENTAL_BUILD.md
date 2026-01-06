# Incremental Build

Incremental generation reduces build times by regenerating only changed proto files and their dependents.

**Since:** v1.6.0

---

## Table of Contents

- [Overview](#overview)
- [Configuration](#configuration)
- [How It Works](#how-it-works)
- [Cache Invalidation](#cache-invalidation)
- [Performance](#performance)
- [Troubleshooting](#troubleshooting)

---

## Overview

Incremental generation is **enabled by default**. No configuration needed for typical use cases.

### Features

| Feature | Description |
|---------|-------------|
| Smart change detection | SHA-256 content hashing + timestamps |
| Dependency tracking | Tracks proto imports to regenerate dependents |
| Auto-invalidation | Full regeneration on plugin version or config changes |
| Graceful recovery | Handles corrupted cache automatically |
| Thread-safe | File locking for concurrent CI/CD builds |

---

## Configuration

### Maven

```xml
<configuration>
    <basePackage>com.example.model</basePackage>

    <!-- Incremental generation (default: true) -->
    <incremental>true</incremental>

    <!-- Cache directory (default: target/proto-wrapper-cache) -->
    <cacheDirectory>${project.build.directory}/proto-wrapper-cache</cacheDirectory>

    <!-- Force full regeneration (default: false) -->
    <forceRegenerate>false</forceRegenerate>
</configuration>
```

**Command-line overrides:**

```bash
# Force full regeneration
mvn compile -Dproto-wrapper.force=true

# Disable incremental mode
mvn compile -Dproto-wrapper.incremental=false

# Clean build (removes cache)
mvn clean compile
```

### Gradle

```kotlin
protoWrapper {
    basePackage.set("com.example.model")

    // Incremental generation (default: true)
    incremental.set(true)

    // Cache directory (default: build/proto-wrapper-cache)
    cacheDirectory.set(layout.buildDirectory.dir("proto-wrapper-cache"))

    // Force full regeneration (default: false)
    forceRegenerate.set(false)
}
```

**Command-line overrides:**

```bash
# Force full regeneration
./gradlew generateProtoWrapper -Pproto-wrapper.force=true

# Clean build (removes cache)
./gradlew clean generateProtoWrapper
```

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `incremental` | `true` | Enable incremental generation |
| `cacheDirectory` | `{build}/proto-wrapper-cache` | Directory for state cache |
| `forceRegenerate` | `false` | Force full regeneration, ignoring cache |

---

## How It Works

### Build Flow

1. **First build**: Full generation, cache created
2. **Subsequent builds**:
   - Load previous state from `state.json`
   - Compute fingerprints for all proto files
   - Compare with cached fingerprints
   - Regenerate only changed files and their dependents
3. **Cache structure**:
   ```
   proto-wrapper-cache/
   └── state.json    # Fingerprints, dependencies, timestamps
   ```

### State File Contents

The `state.json` file tracks:

```json
{
  "pluginVersion": "1.6.3",
  "configHash": "a1b2c3d4...",
  "protoFiles": {
    "proto/v1/order.proto": {
      "contentHash": "sha256:...",
      "lastModified": 1704067200000,
      "imports": ["proto/v1/common.proto"]
    }
  },
  "generatedFiles": {
    "Order.java": {
      "sourceProtos": ["proto/v1/order.proto", "proto/v2/order.proto"]
    }
  }
}
```

### Dependency Tracking

When a proto file changes, the plugin:

1. Identifies all files that import the changed file
2. Marks dependents for regeneration
3. Continues recursively until all affected files are found

Example:
```
common.proto (changed)
  └── order.proto (imports common.proto → regenerate)
        └── payment.proto (imports order.proto → regenerate)
```

---

## Cache Invalidation

### Full Regeneration Triggers

The plugin performs full regeneration when:

| Condition | Reason |
|-----------|--------|
| Plugin version changed | Generated code format may differ |
| Configuration changed | Output structure may differ |
| Proto file deleted | May break dependencies |
| Cache file corrupted | Recovery mode |
| `forceRegenerate=true` | User requested |
| `clean` task executed | Cache deleted |

### Partial Regeneration Triggers

Partial regeneration occurs when:

| Condition | Action |
|-----------|--------|
| Proto file content changed | Regenerate file + dependents |
| New proto file added | Generate new file + check for new imports |
| Import dependencies changed | Regenerate affected files |

### Manual Cache Clear

```bash
# Maven
rm -rf target/proto-wrapper-cache
mvn compile

# Gradle
rm -rf build/proto-wrapper-cache
./gradlew generateProtoWrapper

# Or simply
mvn clean compile
./gradlew clean generateProtoWrapper
```

---

## Performance

### Typical Improvements

| Scenario | Build Time Reduction |
|----------|---------------------|
| No changes | **80-90%** (fingerprint check only) |
| Single file change | **50-70%** (regenerate subset) |
| New file added | **40-60%** (generate new + dependents) |
| Config change | **0%** (full regeneration required) |
| Plugin version change | **0%** (full regeneration required) |

### Real-World Examples

| Project Size | Full Build | Incremental (no changes) |
|--------------|------------|--------------------------|
| 10 proto files | 2.5s | 0.3s |
| 50 proto files | 8s | 0.5s |
| 200 proto files | 25s | 1.2s |

### Best Practices

1. **Keep cache between builds** in CI:
   ```yaml
   # GitHub Actions
   - uses: actions/cache@v4
     with:
       path: target/proto-wrapper-cache
       key: proto-wrapper-${{ hashFiles('proto/**') }}
   ```

2. **Use clean builds for releases**:
   ```bash
   mvn clean install -Dproto-wrapper.force=true
   ```

3. **Don't disable incremental** unless debugging:
   ```bash
   # Only for debugging
   mvn compile -Dproto-wrapper.incremental=false
   ```

---

## Troubleshooting

### Generated code seems outdated

Force regeneration:

```bash
mvn compile -Dproto-wrapper.force=true
# or
./gradlew generateProtoWrapper -Pproto-wrapper.force=true
```

### Cache corruption errors

Delete cache and rebuild:

```bash
rm -rf target/proto-wrapper-cache
mvn compile
```

### Build always does full regeneration

Check for:
1. Configuration changes between builds
2. Plugin version mismatch
3. Filesystem timestamp issues (Docker, network drives)

Enable debug logging:

```bash
mvn compile -Dproto-wrapper.debug=true
```

### Concurrent build conflicts

The plugin uses file locking, but on some filesystems this may fail. If you see locking errors:

```bash
# Serialize builds
mvn compile -Dproto-wrapper.lockTimeout=60000
```

---

## See Also

- [Configuration](CONFIGURATION.md) - All configuration options
- [Getting Started](GETTING_STARTED.md) - Initial setup
- [Known Issues](KNOWN_ISSUES.md) - Limitations
