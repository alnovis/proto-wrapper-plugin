# Deprecation Policy

This document defines the deprecation and removal policy for proto-wrapper-plugin APIs.

---

## Policy Summary

**Hybrid SemVer + Minimum Releases Approach:**

> Deprecated APIs are removed only in **major versions**, but must be deprecated for at least **3 minor releases** before removal.

```
Removal version = max(next major, deprecation version + 3 minors)
```

---

## Rules

### 1. Deprecation Timing

| Rule | Description |
|------|-------------|
| Major-only removal | Breaking changes (including deprecated API removal) happen **only** in major version bumps (e.g., 2.x → 3.0) |
| Minimum grace period | API must be deprecated for **at least 3 minor releases** before removal |
| No surprise removals | If 3.0.0 is released before 3 minor releases pass, removal is postponed to 3.1.0+ or 4.0.0 |

### 2. Deprecation Requirements

Every deprecated API **must** include:

**Java:**
```java
/**
 * @deprecated Since 2.2.0. Use {@link #newMethod()} instead.
 *             Scheduled for removal in 3.0.0.
 */
@Deprecated(since = "2.2.0", forRemoval = true)
public void oldMethod() { ... }
```

**Kotlin:**
```kotlin
@Deprecated(
    message = "Use newMethod() instead",
    replaceWith = ReplaceWith("newMethod()"),
    level = DeprecationLevel.WARNING
)
fun oldMethod() { ... }
```

### 3. Documentation Requirements

| Requirement | Location |
|-------------|----------|
| Javadoc/KDoc | On the deprecated element with `@deprecated` tag |
| CHANGELOG.md | Entry in "Deprecated" section of the release |
| Migration guide | In this document or linked documentation |
| Removal version | Explicitly stated in deprecation message |

### 4. Removal Process

1. **Deprecation release (e.g., 2.2.0):**
   - Add `@Deprecated` annotation with `since` and `forRemoval`
   - Document in CHANGELOG.md under "Deprecated"
   - Add to "Currently Deprecated" table below
   - Provide migration guide

2. **Intermediate releases (2.3.0, 2.4.0, ...):**
   - Keep deprecated API functional
   - May add compiler warnings
   - Update documentation if alternatives change

3. **Removal release (e.g., 3.0.0):**
   - Remove deprecated code
   - Document in CHANGELOG.md under "Removed"
   - Remove from "Currently Deprecated" table
   - Move to "Removed APIs" table for historical reference

---

## Currently Deprecated APIs

### Plugin Configuration

| API | Module | Deprecated In | Min Removal | Alternative |
|-----|--------|---------------|-------------|-------------|
| `protobufMajorVersion` | maven-plugin | 2.2.0 | 3.0.0 | Per-version `protoSyntax` configuration |
| `protobufMajorVersion` | gradle-plugin | 2.2.0 | 3.0.0 | Per-version `protoSyntax` configuration |

### Core Library

| API | Class | Deprecated In | Min Removal | Alternative |
|-----|-------|---------------|-------------|-------------|
| `getEnumFromIntMethod(GeneratorConfig)` | `CodeGenerationHelper` | 2.2.0 | 3.0.0 | `getEnumFromIntMethod(ProcessingContext)` |
| `getProtobufMajorVersion()` | `GeneratorConfig` | 2.2.0 | 3.0.0 | `getDefaultSyntax()` |
| `isProtobuf2()` | `GeneratorConfig` | 2.2.0 | 3.0.0 | `getDefaultSyntax().isProto2()` |
| `isProtobuf3()` | `GeneratorConfig` | 2.2.0 | 3.0.0 | `getDefaultSyntax().isProto3()` |
| `Builder.protobufMajorVersion(int)` | `GeneratorConfig.Builder` | 2.2.0 | 3.0.0 | `Builder.defaultSyntax(ProtoSyntax)` |
| `fromMajorVersion(int)` | `ProtoSyntax` | 2.2.0 | 3.0.0 | Use `ProtoSyntax` enum directly |
| `getVersionField(MergedField, ProcessingContext)` | `ExtractMethodGenerator` | 2.1.0 | 3.0.0 | `ProcessingContext.versionSnapshot(MergedField)` |
| `createExtractMethodBuilder(...)` | `ExtractMethodGenerator` | 2.1.0 | 3.0.0 | `MethodSpecFactory.protectedExtract(...)` |
| `createExtractHasMethodBuilder(...)` | `ExtractMethodGenerator` | 2.1.0 | 3.0.0 | `MethodSpecFactory.protectedExtractHas(...)` |

---

## Migration Guides

### `protobufMajorVersion` → Per-version `protoSyntax`

**Deprecated in:** 2.2.0
**Removal in:** 3.0.0
**Reason:** Global setting doesn't support mixed proto2/proto3 projects

**Before (deprecated):**
```xml
<!-- Maven -->
<configuration>
    <protobufMajorVersion>2</protobufMajorVersion>
    <versions>
        <version><protoDir>v1</protoDir></version>
        <version><protoDir>v2</protoDir></version>
    </versions>
</configuration>
```

```kotlin
// Gradle
protoWrapper {
    protobufMajorVersion.set(2)
    versions {
        version("v1")
        version("v2")
    }
}
```

**After (recommended):**
```xml
<!-- Maven -->
<configuration>
    <versions>
        <version>
            <protoDir>v1</protoDir>
            <protoSyntax>proto2</protoSyntax>
        </version>
        <version>
            <protoDir>v2</protoDir>
            <protoSyntax>proto3</protoSyntax>
        </version>
    </versions>
</configuration>
```

```kotlin
// Gradle
protoWrapper {
    versions {
        version("v1") {
            protoSyntax.set("proto2")
        }
        version("v2") {
            protoSyntax.set("proto3")
        }
    }
}
```

**Note:** If all versions use the same syntax, you can omit `protoSyntax` entirely — the plugin auto-detects syntax from `.proto` files.

---

## Removed APIs (Historical)

| API | Module | Deprecated In | Removed In | Alternative |
|-----|--------|---------------|------------|-------------|
| *(none yet)* | | | | |

---

## Version Timeline

```
2.2.0  ──────────────────────────────────────────────────────────►
       │ protobufMajorVersion deprecated
       │
2.3.0  ──────────────────────────────────────────────────────────►
       │ (grace period: 1 of 3)
       │
2.4.0  ──────────────────────────────────────────────────────────►
       │ (grace period: 2 of 3)
       │
2.5.0  ──────────────────────────────────────────────────────────►
       │ (grace period: 3 of 3) ✓ Minimum reached
       │
3.0.0  ──────────────────────────────────────────────────────────►
       │ protobufMajorVersion REMOVED
       │ (next major after minimum grace period)
```

---

## FAQ

### Q: What if I need to use deprecated API?

Deprecated APIs remain fully functional until removal. You'll see compiler warnings, but the code works. Plan migration before the removal version.

### Q: Can removal be postponed?

Yes. If significant user impact is discovered, removal may be postponed to a later major version. This will be communicated in release notes.

### Q: How do I know what's deprecated?

1. Compiler warnings when using deprecated APIs
2. This document's "Currently Deprecated" table
3. CHANGELOG.md "Deprecated" sections
4. IDE hints (strikethrough, warnings)

### Q: What about internal/private APIs?

This policy applies to **public APIs** only. Internal APIs (package-private, `@Internal` annotated) may change without deprecation notice.

---

## For Contributors

When deprecating an API:

1. Add proper annotations (`@Deprecated` with `since`, `forRemoval`)
2. Update this document's "Currently Deprecated" table
3. Add CHANGELOG.md entry under "### Deprecated"
4. Write migration guide in this document
5. Ensure alternative API exists and is documented
6. Add `// TODO: Remove in X.0.0` comment near deprecated code

When removing an API:

1. Verify minimum 3 minor releases have passed
2. Verify it's a major version release
3. Remove the code
4. Update this document (move to "Removed APIs")
5. Add CHANGELOG.md entry under "### Removed"
6. Update any documentation referencing the removed API
