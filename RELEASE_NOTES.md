# Release Notes - Proto Wrapper Plugin v2.1.1

**Release Date:** January 2026

## Overview

Version 2.1.1 adds the `defaultVersion` configuration parameter, allowing explicit control over which protocol version is used as the default in generated `VersionContext.DEFAULT_VERSION` and `ProtocolVersions.DEFAULT` constants.

## New Features

### Default Version Configuration

Previously, the default version was always the **last version** in the configured versions list. This release adds explicit control via the `defaultVersion` parameter.

**Maven:**
```xml
<configuration>
    <versions>
        <version><protoDir>v202</protoDir></version>
        <version><protoDir>v203</protoDir></version>
    </versions>
    <defaultVersion>v202</defaultVersion>
</configuration>
```

**Gradle:**
```kotlin
protoWrapper {
    versions {
        version("v202")
        version("v203")
    }
    defaultVersion.set("v202")
}
```

### Generated Code Changes

With `defaultVersion=v202`, the generated code now contains:

```java
// ProtocolVersions.java
public final class ProtocolVersions {
    public static final String V202 = "v202";
    public static final String V203 = "v203";
    public static final String DEFAULT = V202;  // Explicitly configured
    // ...
}

// VersionContext.java
public interface VersionContext {
    String DEFAULT_VERSION = ProtocolVersions.V202;  // Explicitly configured
    // ...
}
```

### Validation

The plugin validates that `defaultVersion` is one of the configured version IDs:

```
[ERROR] defaultVersion 'v999' is not in the configured versions list. Valid versions: [v202, v203]
```

## Important Notes

### versionId vs name

The `defaultVersion` parameter expects a **versionId** (derived from `protoDir`), not the version `name`:

| Parameter | Source | Example | Used For |
|-----------|--------|---------|----------|
| `versionId` | `protoDir.toLowerCase()` | `v202` | `defaultVersion`, runtime identification |
| `name` | explicit or `protoDir.toUpperCase()` | `V202`, `Legacy` | Class name suffixes |

```xml
<!-- Correct -->
<version>
    <protoDir>v202</protoDir>
    <name>Legacy</name>
</version>
<defaultVersion>v202</defaultVersion>  <!-- Uses protoDir, not name -->

<!-- WRONG - will fail validation -->
<defaultVersion>Legacy</defaultVersion>
```

### Backward Compatibility

If `defaultVersion` is not specified, the plugin falls back to the previous behavior (last version in the list).

## Configuration Reference

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `defaultVersion` | String | (last version) | Version ID for `DEFAULT_VERSION` and `ProtocolVersions.DEFAULT` |

## Migration

No migration required. Existing configurations continue to work unchanged.

To adopt the new feature:
1. Update plugin version to `2.1.1`
2. Add `<defaultVersion>` parameter if you need explicit control

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.

---

## Previous Releases

- [v2.1.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.0) - ProtocolVersions class generation, parallel generation
- [v2.0.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.0.0) - Namespace migration to io.alnovis, removed deprecated APIs
- [v1.6.9](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v1.6.9) - String-based version API
