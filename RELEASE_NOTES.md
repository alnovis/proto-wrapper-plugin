# Release Notes - Proto Wrapper Plugin v2.2.0

**Release Date:** January 2026

## Overview

Version 2.2.0 introduces **per-version proto syntax configuration**, enabling projects with mixed proto2/proto3 schemas to generate correct code for each version independently. The plugin now auto-detects syntax from `.proto` files and uses the appropriate enum conversion methods and `has*()` method generation per version.

This release also establishes a formal **Deprecation Policy** for the project.

## New Features

### Per-Version Proto Syntax

Previously, `protobufMajorVersion` was a global setting affecting all versions. Now each version can specify its own proto syntax:

**Maven:**
```xml
<versions>
    <version>
        <protoDir>v1</protoDir>
        <protoSyntax>proto2</protoSyntax>
    </version>
    <version>
        <protoDir>v2</protoDir>
        <protoSyntax>proto3</protoSyntax>
    </version>
    <version>
        <protoDir>v3</protoDir>
        <!-- protoSyntax defaults to "auto" (detected from .proto files) -->
    </version>
</versions>
```

**Gradle:**
```kotlin
protoWrapper {
    versions {
        version("v1") {
            protoSyntax.set("proto2")
        }
        version("v2") {
            protoSyntax.set("proto3")
        }
        version("v3")  // Auto-detect from .proto files
    }
}
```

### Syntax Auto-Detection

When `protoSyntax` is not specified or set to `"auto"`, the plugin automatically detects syntax by parsing the `syntax = "proto2|proto3";` declaration in `.proto` files. Files without explicit syntax declaration default to proto2 per the Protocol Buffers specification.

### What This Affects

| Aspect | proto2 | proto3 |
|--------|--------|--------|
| Enum conversion | `EnumType.valueOf(int)` | `EnumType.forNumber(int)` |
| `has*()` methods | All optional fields | Only message types and `optional` keyword fields |

### Deprecation of `protobufMajorVersion`

The global `protobufMajorVersion` parameter is now **deprecated**. Use per-version `protoSyntax` configuration instead.

```xml
<!-- DEPRECATED -->
<protobufMajorVersion>2</protobufMajorVersion>

<!-- RECOMMENDED -->
<versions>
    <version>
        <protoDir>v1</protoDir>
        <protoSyntax>proto2</protoSyntax>
    </version>
</versions>
```

The deprecated parameter will be removed in version **3.0.0**.

## Deprecation Policy

This release establishes a formal deprecation policy documented in [docs/DEPRECATION_POLICY.md](docs/DEPRECATION_POLICY.md):

- **Major-only removal**: Deprecated APIs are removed only in major versions (e.g., 2.x → 3.0)
- **Minimum grace period**: APIs must be deprecated for at least 3 minor releases before removal
- **Clear documentation**: All deprecations include `@Deprecated(since, forRemoval)`, TODO comments, and migration guides

### APIs Deprecated in 2.2.0

| API | Alternative |
|-----|-------------|
| `protobufMajorVersion` (plugins) | Per-version `protoSyntax` |
| `GeneratorConfig.getProtobufMajorVersion()` | `getDefaultSyntax()` |
| `GeneratorConfig.isProtobuf2()` | `getDefaultSyntax().isProto2()` |
| `GeneratorConfig.isProtobuf3()` | `getDefaultSyntax().isProto3()` |
| `GeneratorConfig.Builder.protobufMajorVersion(int)` | `defaultSyntax(ProtoSyntax)` |
| `ProtoSyntax.fromMajorVersion(int)` | Use `ProtoSyntax` enum directly |
| `CodeGenerationHelper.getEnumFromIntMethod(GeneratorConfig)` | `getEnumFromIntMethod(ProcessingContext)` |

All deprecated APIs are scheduled for removal in **3.0.0**.

## Configuration Reference

### Version Configuration

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `protoDir` | Yes | - | Directory name relative to `protoRoot` |
| `name` | No | (from protoDir) | Version name for generated classes |
| `excludeProtos` | No | - | List of proto files to exclude |
| `protoSyntax` | No | `auto` | Proto syntax: `proto2`, `proto3`, or `auto` |

### protoSyntax Values

| Value | Description |
|-------|-------------|
| `proto2` | Force proto2 semantics (valueOf, has* for all fields) |
| `proto3` | Force proto3 semantics (forNumber, has* only for messages/optional) |
| `auto` | Detect from `.proto` file syntax declaration (default) |

## Migration

### From 2.1.x

1. Update plugin version to `2.2.0`
2. (Optional) Remove global `protobufMajorVersion` and add per-version `protoSyntax` if needed
3. If all versions use the same syntax, you can omit `protoSyntax` entirely (auto-detection)

### Mixed Proto2/Proto3 Projects

If your project has versions with different proto syntaxes:

```xml
<versions>
    <version>
        <protoDir>legacy</protoDir>
        <protoSyntax>proto2</protoSyntax>
    </version>
    <version>
        <protoDir>current</protoDir>
        <protoSyntax>proto3</protoSyntax>
    </version>
</versions>
```

## Internal Changes

- New `SyntaxDetector` class for parsing syntax from `.proto` files
- `FieldInfo` now tracks `detectedSyntax` per field
- `MergedSchema` tracks syntax per version via `versionSyntax` map
- `ContractProvider` uses detected syntax for correct `has*()` method generation
- All enum conversion code uses `ProcessingContext` for per-version syntax lookup

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.

---

## Previous Releases

- [v2.1.1](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.1) - defaultVersion configuration
- [v2.1.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.0) - ProtocolVersions class generation, parallel generation
- [v2.0.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.0.0) - Namespace migration to io.alnovis, removed deprecated APIs
- [v1.6.9](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v1.6.9) - String-based version API
