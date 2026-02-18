# Release Notes - Proto Wrapper Plugin v2.3.2

**Release Date:** February 18, 2026

## Overview

Version 2.3.2 changes **message field getter behavior** to match native protobuf semantics — unset message fields now return a **non-null default instance** instead of `null`.

This eliminates a common source of `NullPointerException` in code that chains message getters (e.g., `request.getTicket().hasParentTicket()`), and makes wrapper behavior consistent with protobuf's `getXxx()` contract.

> **Note:** This is a behavioral change. Code that checks `getXxx() == null` for message fields should migrate to `hasXxx()`. See [Migration](#migration) below.

## What Changed

### Message getters return default instance instead of null

**Before (v2.3.1):**
```java
AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();
msg.getSingularMessage()           // → null
msg.getSingularMessage().getId()   // → NPE!
msg.hasSingularMessage()           // → false
```

**After (v2.3.2):**
```java
AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();
msg.getSingularMessage()           // → non-null default instance
msg.getSingularMessage().getId()   // → 0 (default)
msg.getSingularMessage().getName() // → "" (default)
msg.hasSingularMessage()           // → false (unchanged)
```

### What is affected

| Field type | Old behavior | New behavior |
|------------|-------------|-------------|
| Singular message (proto3) | `null` when unset | Default instance |
| Optional message (proto3) | `null` when unset | Default instance |
| Optional message (proto2) | `null` when unset | Default instance |
| Required message (proto2) | Default instance | Default instance (unchanged) |

### What is NOT affected

| Field type | Behavior |
|------------|----------|
| Oneof message fields | Still `null` when oneof case is not active |
| Optional primitive fields | Still `null` when unset (`Integer`, `Boolean`, etc.) |
| `hasXxx()` methods | Still `false` for unset fields |
| Builder methods | No changes |
| Missing-in-version fields | Still `null` (field doesn't exist in that proto version) |

## Bug Fix

- **Eliminated NPE risk for unset message fields** — code like `request.getTicket().hasParentTicket()` no longer throws `NullPointerException` when `ticket` is not set. This was the original motivation for the change: the previous null-returning behavior broke the protobuf contract and caused runtime failures.

## Migration

### From v2.3.1

1. Update plugin version to `2.3.2`
2. Regenerate wrappers (`mvn clean generate-sources` or `gradle generateProtoWrappers`)
3. Review code that checks message getters for null:

**Replace null checks with `hasXxx()`:**
```java
// Before — worked in v2.3.1, still works in v2.3.2
if (request.getTicket() != null) {
    process(request.getTicket());
}

// After — idiomatic, recommended approach
if (request.hasTicket()) {
    process(request.getTicket());
}
```

**Chained getters are now safe:**
```java
// Before — NPE if ticket is not set
boolean hasParent = request.getTicket().hasParentTicket();

// After — safe, returns false (default instance has no parent ticket)
boolean hasParent = request.getTicket().hasParentTicket();
```

### No Breaking Changes for Common Patterns

- `hasXxx()` + `getXxx()` pattern: works identically
- `getXxx() != null` checks: still true (default instance is non-null), but `hasXxx()` is preferred
- Builder `.setXxx()` / `.clearXxx()`: unchanged

### Potentially Breaking Pattern

Code that relies on `getXxx() == null` to detect unset message fields will no longer work:

```java
// This pattern BREAKS — getXxx() is never null for message fields
if (request.getTicket() == null) {
    // This branch is now unreachable for message fields
}

// Fix: use hasXxx() instead
if (!request.hasTicket()) {
    // Correct way to check if field is unset
}
```

## Implementation Details

| Component | Change |
|-----------|--------|
| `MergedField.needsHasCheck()` | Returns `false` for non-oneof message fields (was `true` for optional messages) |
| `FieldContract.computeNullable()` | Returns `false` for non-oneof message fields |
| `FieldContract.computeDefaultValue()` | Returns `DEFAULT_INSTANCE` for message fields |
| `CONTRACT-MATRIX.md` | Updated to reflect new message field behavior |

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.

---

## Previous Releases

- [v2.3.1](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.3.1) - Schema Metadata, Gradle plugin fix
- [v2.3.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.3.0) - Validation Annotations
- [v2.2.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.2.0) - Per-version proto syntax, field mappings
- [v2.1.1](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.1) - defaultVersion configuration
- [v2.1.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.0) - ProtocolVersions class generation, parallel generation
- [v2.0.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.0.0) - Namespace migration to io.alnovis, removed deprecated APIs
- [v1.6.9](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v1.6.9) - String-based version API
