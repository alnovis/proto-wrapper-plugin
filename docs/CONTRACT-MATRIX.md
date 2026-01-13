# Contract Matrix: Field Behavior Specification

This document defines the behavior of protobuf fields in the wrapper API based on their characteristics. The contract matrix serves as the single source of truth for code generation decisions.

## Overview

The **Contract Matrix** systematizes field behavior across all combinations of:
- **Syntax**: Proto2 vs Proto3
- **Cardinality**: Singular, Repeated, Map
- **Type**: Scalar (numeric, string, bytes), Message, Enum
- **Presence**: Optional, Required, Implicit, Explicit
- **Oneof**: Whether the field is part of a oneof group

## Key Concepts

### Field Contract Properties

| Property | Description |
|----------|-------------|
| `hasMethodExists` | Whether `has*()` method is available in proto API |
| `getterUsesHasCheck` | Whether getter should use `has*() ? get*() : null` pattern |
| `nullable` | Whether the wrapper getter can return `null` |
| `defaultValue` | Value returned when field is unset |

### Decision Flow

```
                    ┌─────────────┐
                    │  Is field   │
                    │  repeated?  │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │ YES                     │ NO
              ▼                         ▼
    ┌─────────────────┐       ┌─────────────────┐
    │ hasMethod: NO   │       │  Is field in    │
    │ nullable: NO    │       │    oneof?       │
    │ default: []     │       └────────┬────────┘
    └─────────────────┘                │
                           ┌───────────┴───────────┐
                           │ YES                   │ NO
                           ▼                       ▼
                 ┌─────────────────┐     ┌─────────────────┐
                 │ hasMethod: YES  │     │  Check syntax   │
                 │ nullable: YES   │     │  and type...    │
                 └─────────────────┘     └─────────────────┘
```

---

## Field Behavior Matrix

### Proto2 Singular Fields

| Type | Label | In Oneof | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-------|----------|-----------------|-------------------|----------|--------------|
| int32/int64/etc | optional | No | **YES** | YES | YES | null (boxed) |
| int32/int64/etc | required | No | YES | NO | NO | 0 |
| float/double | optional | No | **YES** | YES | YES | null (boxed) |
| float/double | required | No | YES | NO | NO | 0.0 |
| bool | optional | No | **YES** | YES | YES | null (boxed) |
| bool | required | No | YES | NO | NO | false |
| string | optional | No | **YES** | YES | YES | null |
| string | required | No | YES | NO | NO | "" |
| bytes | optional | No | **YES** | YES | YES | null |
| bytes | required | No | YES | NO | NO | empty bytes |
| message | optional | No | **YES** | YES | **YES** | null |
| message | required | No | YES | NO | NO | default instance |
| enum | optional | No | **YES** | YES | YES | null |
| enum | required | No | YES | NO | NO | first value |
| any | any | **YES** | **YES** | YES | **YES** | null |

### Proto3 Singular Fields (Implicit Presence)

Fields without `optional` keyword in proto3 have **implicit presence** - they cannot distinguish "unset" from "default value".

| Type | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-----------------|-------------------|----------|--------------|
| int32/int64/etc | **NO** | NO | NO | 0 |
| float/double | **NO** | NO | NO | 0.0 |
| bool | **NO** | NO | NO | false |
| string | **NO** | NO | NO | "" |
| bytes | **NO** | NO | NO | empty bytes |
| message | **YES** | YES | **YES** | null |
| enum | **NO** | NO | NO | first value (0) |

### Proto3 Singular Fields (Explicit Presence)

Fields with `optional` keyword in proto3 have **explicit presence** via synthetic oneof.

| Type | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-----------------|-------------------|----------|--------------|
| int32/int64/etc | **YES** | YES | YES | null (boxed) |
| float/double | **YES** | YES | YES | null (boxed) |
| bool | **YES** | YES | YES | null (boxed) |
| string | **YES** | YES | YES | null |
| bytes | **YES** | YES | YES | null |
| message | **YES** | YES | YES | null |
| enum | **YES** | YES | YES | null |

### Proto3 Oneof Fields

All fields in a oneof (non-synthetic) have explicit presence.

| Type | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-----------------|-------------------|----------|--------------|
| any scalar | **YES** | YES | YES | null |
| message | **YES** | YES | YES | null |
| enum | **YES** | YES | YES | null |

### Repeated Fields (Both Proto2 and Proto3)

Repeated fields never have `has*()` methods and are never null.

| Type | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-----------------|-------------------|----------|--------------|
| any | **NO** | NO | **NO** | empty list `[]` |

### Map Fields (Both Proto2 and Proto3)

Map fields are similar to repeated fields.

| Type | hasMethodExists | getterUsesHasCheck | nullable | defaultValue |
|------|-----------------|-------------------|----------|--------------|
| any | **NO** | NO | **NO** | empty map `{}` |

---

## Merge Rules for Multi-Version Fields

When a field exists in multiple proto versions with different characteristics, the contracts are merged according to these rules:

### hasMethodExists

```
Unified hasMethodExists = ALL versions have hasMethodExists
```

**Rationale**: If any version lacks `has*()`, we cannot reliably check presence across all versions.

| v1 | v2 | Unified |
|----|----| --------|
| YES | YES | YES |
| YES | NO | **NO** |
| NO | YES | **NO** |
| NO | NO | NO |

### nullable

```
Unified nullable = ANY version is nullable
```

**Rationale**: If the field can be null in any version, the unified API must handle null.

| v1 | v2 | Unified |
|----|----|---------|
| YES | YES | YES |
| YES | NO | **YES** |
| NO | YES | **YES** |
| NO | NO | NO |

### getterUsesHasCheck

```
Unified getterUsesHasCheck = hasMethodExists AND nullable
```

**Rationale**: We only use has-check pattern if we can check presence AND need to return null.

### cardinality

```
Priority: MAP > REPEATED > SINGULAR
```

**Rationale**: For REPEATED_SINGLE conflicts, we treat the field as repeated (can always convert single to list).

| v1 | v2 | Unified |
|----|----|---------|
| SINGULAR | SINGULAR | SINGULAR |
| SINGULAR | REPEATED | **REPEATED** |
| REPEATED | SINGULAR | **REPEATED** |
| REPEATED | REPEATED | REPEATED |
| any | MAP | **MAP** |

### defaultValue

| Cardinality | nullable | defaultValue |
|-------------|----------|--------------|
| REPEATED | - | EMPTY_LIST |
| MAP | - | EMPTY_MAP |
| SINGULAR | YES | NULL |
| SINGULAR | NO | Type-specific (0, false, "", etc.) |

---

## Conflict Types and Builder Behavior

Some field conflicts affect whether builder setters can be generated:

| Conflict Type | Builder Setters | Reason |
|---------------|-----------------|--------|
| NONE | Generated | No conflict |
| WIDENING (int→long) | **Skipped** | Cannot safely narrow value |
| FLOAT_DOUBLE | **Skipped** | Cannot safely narrow value |
| SIGNED_UNSIGNED | **Skipped** | Range validation complex |
| STRING_BYTES | **Skipped** | Incompatible types |
| INT_ENUM | Generated* | Uses conflict enum |
| ENUM_ENUM | Generated* | Uses unified enum |
| REPEATED_SINGLE | Generated | Uses list type |
| PRIMITIVE_MESSAGE | **Skipped** | Incompatible types |
| INCOMPATIBLE | **Skipped** | Cannot convert |

*With special handling

---

## Quick Reference Card

### When does getter return null?

```
nullable = true when:
  - Proto2 optional field (any type)
  - Proto3 message field
  - Proto3 optional scalar field
  - Any field in oneof
  - Multi-version: ANY version is nullable
```

### When is has*() method generated?

```
hasMethodExists = true when:
  - Proto2: all singular fields
  - Proto3: message fields, optional scalars, oneof fields
  - Multi-version: ALL versions support has*()
  - NEVER for repeated/map fields
```

### Getter Pattern

```java
// When getterUsesHasCheck = true:
public T getField() {
    return extractHasField(proto) ? extractField(proto) : null;
}

// When getterUsesHasCheck = false:
public T getField() {
    return extractField(proto);
}
```

---

## API Reference

### FieldContract

```java
public record FieldContract(
    FieldCardinality cardinality,    // SINGULAR, REPEATED, MAP
    FieldTypeCategory typeCategory,  // SCALAR_NUMERIC, SCALAR_STRING, SCALAR_BYTES, MESSAGE, ENUM
    FieldPresence presence,          // PROTO2_OPTIONAL, PROTO2_REQUIRED, PROTO3_IMPLICIT, PROTO3_EXPLICIT_OPTIONAL
    boolean inOneof,
    boolean hasMethodExists,
    boolean getterUsesHasCheck,
    boolean nullable,
    DefaultValue defaultValueWhenUnset
) {
    // Factory method
    static FieldContract from(FieldInfo field, ProtoSyntax syntax);
}
```

### MergedFieldContract

```java
public record MergedFieldContract(
    Map<String, FieldContract> versionContracts,  // Per-version contracts
    FieldContract unified,                         // Merged contract for API
    Set<String> presentInVersions,
    MergedField.ConflictType conflictType
) {
    // Factory method
    static MergedFieldContract from(MergedField field, Map<String, ProtoSyntax> syntaxPerVersion);

    // Convenience methods
    boolean isPresentIn(String version);
    boolean hasMethodAvailableIn(String version);
    boolean shouldSkipBuilderSetter();
}
```

### ContractProvider

```java
public final class ContractProvider {
    static ContractProvider getInstance();

    MergedFieldContract getContract(MergedField field);
    MergedFieldContract getContract(MergedMessage message, MergedField field);
    Map<MergedField, MergedFieldContract> getContracts(MergedMessage message);

    // Convenience methods
    boolean shouldGenerateHasMethod(MergedField field);
    boolean shouldUseHasCheckInGetter(MergedField field);
    boolean isNullable(MergedField field);
    boolean shouldSkipBuilderSetter(MergedField field);
}
```

### ProcessingContext Integration

```java
public record ProcessingContext(...) {
    // Contract support
    ContractProvider contractProvider();
    MergedFieldContract getContractFor(MergedField field);
    FieldMethodNames getFieldNames(MergedField field);
    boolean shouldGenerateHasMethod(MergedField field);
    boolean shouldUseHasCheckInGetter(MergedField field);
}
```

---

## Examples

### Example 1: Proto3 Implicit Scalar

```protobuf
// Proto3 without optional keyword
message User {
    string name = 1;  // implicit presence
}
```

Contract:
- `hasMethodExists`: false
- `getterUsesHasCheck`: false
- `nullable`: false
- `defaultValue`: ""

Generated:
```java
public String getName() {
    return extractName(proto);  // Never null, returns "" if unset
}
// No hasName() method generated
```

### Example 2: Proto3 Explicit Optional

```protobuf
message User {
    optional string nickname = 2;  // explicit presence
}
```

Contract:
- `hasMethodExists`: true
- `getterUsesHasCheck`: true
- `nullable`: true
- `defaultValue`: null

Generated:
```java
public String getNickname() {
    return extractHasNickname(proto) ? extractNickname(proto) : null;
}

public boolean hasNickname() {
    return extractHasNickname(proto);
}
```

### Example 3: Multi-Version with Different Presence

```protobuf
// v1: Proto2
message Order {
    optional int32 priority = 5;  // has*() available
}

// v2: Proto3 (implicit)
message Order {
    int32 priority = 5;  // no has*()
}
```

Merged Contract:
- `hasMethodExists`: false (v2 lacks it)
- `nullable`: true (v1 is nullable)
- `getterUsesHasCheck`: false (no has method)

Generated:
```java
public Integer getPriority() {
    return extractPriority(proto);  // May return 0 even if "unset" in v2
}
// No hasPriority() - cannot reliably check across versions
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01 | Initial contract matrix specification |
