# Proto Wrapper API Reference

This document describes the API of proto-wrapper plugin, including generated code and the Schema Diff Tool.

---

## Table of Contents

- [Schema Diff Tool API](#schema-diff-tool-api) **(New in v1.5.0)**
  - [SchemaDiff](#schemadiff)
  - [Model Classes](#model-classes)
  - [Formatter Classes](#formatter-classes)
  - [Breaking Change Types](#breaking-change-types)
- [Generated Code Overview](#overview)
- [Package Structure](#package-structure)
- [Generated Class Types](#generated-class-types)
  - [Interface](#interface)
  - [Abstract Class](#abstract-class)
  - [Implementation Class](#implementation-class)
  - [VersionContext](#versioncontext)
  - [ProtocolVersions](#protocolversions)
  - [Enum](#enum)
- [Generated Methods](#generated-methods)
  - [Field Accessors](#field-accessors)
  - [Version Information](#version-information)
  - [Serialization](#serialization)
  - [Version Conversion](#version-conversion)
  - [Builder Methods](#builder-methods)
- [Type Conflict Resolution](#type-conflict-resolution)
- [Repeated Fields with Type Conflicts](#repeated-fields-with-type-conflicts)
- [Well-Known Types](#well-known-types)
- [Map Fields](#map-fields)
- [Nested Types](#nested-types)
- [Oneof Fields](#oneof-fields)

---

## Schema Diff Tool API

*New in v1.5.0*

The Schema Diff Tool provides a programmatic API for comparing protobuf schemas between versions and detecting breaking changes.

### SchemaDiff

**Package:** `io.alnovis.protowrapper.diff`

**Purpose:** Main facade for schema comparison. Provides access to all differences between two schema versions.

#### Factory Methods

```java
// Compare two VersionSchema objects
static SchemaDiff compare(VersionSchema v1, VersionSchema v2);

// Compare with field mappings (since 2.2.0)
static SchemaDiff compare(VersionSchema v1, VersionSchema v2, List<FieldMapping> fieldMappings);

// Compare two directories containing proto files
static SchemaDiff compare(Path v1Dir, Path v2Dir) throws IOException;

// Compare with custom version names
static SchemaDiff compare(Path v1Dir, Path v2Dir, String v1Name, String v2Name) throws IOException;
```

#### Query Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getV1Name()` | `String` | Source version name |
| `getV2Name()` | `String` | Target version name |
| `getMessageDiffs()` | `List<MessageDiff>` | All message diffs |
| `getEnumDiffs()` | `List<EnumDiff>` | All enum diffs |
| `getAddedMessages()` | `List<MessageInfo>` | Messages added in v2 |
| `getRemovedMessages()` | `List<MessageInfo>` | Messages removed from v1 |
| `getModifiedMessages()` | `List<MessageDiff>` | Messages changed between versions |
| `getAddedEnums()` | `List<EnumInfo>` | Enums added in v2 |
| `getRemovedEnums()` | `List<EnumInfo>` | Enums removed from v1 |
| `getModifiedEnums()` | `List<EnumDiff>` | Enums changed between versions |

#### Renumber Methods *(since 2.2.0)*

| Method | Returns | Description |
|--------|---------|-------------|
| `hasSuspectedRenumbers()` | `boolean` | True if heuristic renumber detection found matches |
| `getSuspectedRenumbers()` | `List<SuspectedRenumber>` | All suspected renumbered fields |
| `getMappedRenumberCount()` | `int` | Count of fields mapped via fieldMappings |

#### Breaking Changes Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `hasBreakingChanges()` | `boolean` | True if any breaking changes exist |
| `getBreakingChanges()` | `List<BreakingChange>` | All breaking changes |
| `getErrors()` | `List<BreakingChange>` | ERROR severity only |
| `getWarnings()` | `List<BreakingChange>` | WARNING severity only |
| `getSummary()` | `DiffSummary` | Statistics summary |

#### Output Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `toText()` | `String` | Plain text format |
| `toJson()` | `String` | JSON format |
| `toMarkdown()` | `String` | Markdown format |
| `format(DiffFormatter)` | `String` | Custom formatter |

#### Usage Example

```java
import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.*;

// Compare schemas
SchemaDiff diff = SchemaDiff.compare(
    Path.of("proto/v1"),
    Path.of("proto/v2"),
    "production",
    "development"
);

// Query differences
List<MessageInfo> added = diff.getAddedMessages();
List<MessageInfo> removed = diff.getRemovedMessages();
List<MessageDiff> modified = diff.getModifiedMessages();

// Check breaking changes
if (diff.hasBreakingChanges()) {
    for (BreakingChange bc : diff.getBreakingChanges()) {
        System.err.println(bc.severity() + ": " + bc.entityPath());
    }
}

// Export to different formats
String textReport = diff.toText();
String jsonReport = diff.toJson();
String markdownReport = diff.toMarkdown();

// Get summary
SchemaDiff.DiffSummary summary = diff.getSummary();
System.out.println("Added: " + summary.addedMessages() + " messages");
System.out.println("Breaking: " + summary.errorCount() + " errors");
```

---

### DiffSummary

**Package:** `io.alnovis.protowrapper.diff.SchemaDiff.DiffSummary`

**Purpose:** Summary statistics for the schema diff.

```java
public record DiffSummary(
    int addedMessages,
    int removedMessages,
    int modifiedMessages,
    int addedEnums,
    int removedEnums,
    int modifiedEnums,
    int errorCount,
    int warningCount,
    int mappedRenumbers,      // since 2.2.0
    int suspectedRenumbers    // since 2.2.0
) {
    boolean hasDifferences();
    boolean hasBreakingChanges();
    boolean hasRenumbers();   // since 2.2.0
    int totalChanges();
}
```

---

### SuspectedRenumber *(since 2.2.0)*

**Package:** `io.alnovis.protowrapper.diff.model`

**Purpose:** Represents a heuristically detected renumbered field.

```java
public record SuspectedRenumber(
    String messageName,
    String fieldName,
    int v1Number,
    int v2Number,
    FieldInfo v1Field,
    FieldInfo v2Field,
    Confidence confidence
) {
    public enum Confidence { HIGH, MEDIUM }

    // Generate suggested FieldMapping configuration
    FieldMapping toSuggestedMapping(String v1Name, String v2Name);

    // Human-readable description
    String getDescription();
}
```

---

### Model Classes

#### ChangeType

**Package:** `io.alnovis.protowrapper.diff.model`

Enum representing the type of change detected.

| Value | Description | Category |
|-------|-------------|----------|
| `ADDED` | Entity added in v2 | Addition |
| `REMOVED` | Entity removed from v1 | Removal |
| `MODIFIED` | Entity modified | Modification |
| `UNCHANGED` | No change detected | - |
| `TYPE_CHANGED` | Field type changed | Modification |
| `LABEL_CHANGED` | Field label changed (optional/repeated) | Modification |
| `NUMBER_CHANGED` | Field number changed | Modification |
| `NAME_CHANGED` | Field name changed | Modification |
| `DEFAULT_CHANGED` | Default value changed | Modification |
| `VALUE_ADDED` | Enum value added | Addition |
| `VALUE_REMOVED` | Enum value removed | Removal |
| `VALUE_NUMBER_CHANGED` | Enum value number changed | Modification |

**Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `isAddition()` | `boolean` | True for ADDED, VALUE_ADDED |
| `isRemoval()` | `boolean` | True for REMOVED, VALUE_REMOVED |
| `isModification()` | `boolean` | True for MODIFIED, TYPE_CHANGED, etc. |
| `isPotentiallyBreaking()` | `boolean` | True for potentially breaking changes |

---

#### MessageDiff

**Package:** `io.alnovis.protowrapper.diff.model`

Represents changes to a message between two schema versions.

```java
public record MessageDiff(
    String messageName,
    ChangeType changeType,
    MessageInfo v1Message,
    MessageInfo v2Message,
    List<FieldChange> fieldChanges,
    List<MessageDiff> nestedMessageChanges,
    List<EnumDiff> nestedEnumChanges
) { }
```

**Factory Methods:**

| Method | Description |
|--------|-------------|
| `added(MessageInfo)` | Creates diff for added message |
| `removed(MessageInfo)` | Creates diff for removed message |
| `compared(v1, v2, fieldChanges, nestedMessages, nestedEnums)` | Creates diff for comparison |

**Query Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `getAddedFields()` | `List<FieldChange>` | Fields added in v2 |
| `getRemovedFields()` | `List<FieldChange>` | Fields removed from v1 |
| `getModifiedFields()` | `List<FieldChange>` | Fields with type/label/name changes |
| `hasBreakingChanges()` | `boolean` | True if any breaking changes |
| `countBreakingChanges()` | `int` | Total breaking changes count |
| `getSummary()` | `String` | Human-readable summary |
| `getSourceFileName()` | `String` | Proto source file name |

---

#### FieldChange

**Package:** `io.alnovis.protowrapper.diff.model`

Represents a change to a field between two schema versions.

```java
public record FieldChange(
    int fieldNumber,
    String fieldName,
    ChangeType changeType,
    FieldInfo v1Field,
    FieldInfo v2Field,
    List<String> changes
) { }
```

**Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `isBreaking()` | `boolean` | True if change is breaking (false for mapped renumbers) |
| `isCompatibleTypeChange()` | `boolean` | True if type change is compatible (widening) |
| `isRenumberedByMapping()` | `boolean` | True if NUMBER_CHANGED with same name in v1/v2 *(since 2.2.0)* |
| `getRenumberDescription()` | `String` | Formatted `"#N -> #M"` for renumbered fields *(since 2.2.0)* |
| `getCompatibilityNote()` | `String` | Compatibility description for type changes |
| `getSummary()` | `String` | Human-readable summary |
| `formatType(FieldInfo)` | `String` | Static method to format field type |

**Compatible Type Changes:**

The following type conversions are considered compatible (non-breaking):

| From | To |
|------|-----|
| `int32` | `int64`, `sint32`, `sint64` |
| `sint32` | `int64`, `sint64` |
| `uint32` | `uint64`, `int64` |
| `float` | `double` |
| `fixed32` | `fixed64` |
| `sfixed32` | `sfixed64` |
| `int32/int64` | `enum` (bidirectional) |

---

#### EnumDiff

**Package:** `io.alnovis.protowrapper.diff.model`

Represents changes to an enum between two schema versions.

```java
public record EnumDiff(
    String enumName,
    ChangeType changeType,
    EnumInfo v1Enum,
    EnumInfo v2Enum,
    List<EnumValueChange> valueChanges
) { }
```

**Factory Methods:**

| Method | Description |
|--------|-------------|
| `added(EnumInfo)` | Creates diff for added enum |
| `removed(EnumInfo)` | Creates diff for removed enum |
| `modified(v1, v2, valueChanges)` | Creates diff for modified enum |

**Query Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `getAddedValues()` | `List<EnumValueChange>` | Values added in v2 |
| `getRemovedValues()` | `List<EnumValueChange>` | Values removed from v1 |
| `getChangedValues()` | `List<EnumValueChange>` | Values with number changes |
| `hasBreakingChanges()` | `boolean` | True if any breaking changes |
| `getSummary()` | `String` | Human-readable summary |

---

#### EnumValueChange

**Package:** `io.alnovis.protowrapper.diff.model`

Represents a change to an enum value.

```java
public record EnumValueChange(
    String valueName,
    ChangeType changeType,
    Integer v1Number,
    Integer v2Number
) { }
```

**Factory Methods:**

| Method | Description |
|--------|-------------|
| `added(name, number)` | Creates change for added value |
| `removed(name, number)` | Creates change for removed value |
| `numberChanged(name, v1Number, v2Number)` | Creates change for renumbered value |

**Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `isBreaking()` | `boolean` | True for removed or number changed |
| `getSummary()` | `String` | Human-readable summary |

---

#### BreakingChange

**Package:** `io.alnovis.protowrapper.diff.model`

Represents a breaking change detected between schema versions.

```java
public record BreakingChange(
    Type type,
    Severity severity,
    String entityPath,
    String description,
    String v1Value,
    String v2Value
) { }
```

**Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `isError()` | `boolean` | True if severity is ERROR |
| `isWarning()` | `boolean` | True if severity is WARNING |
| `toDisplayString()` | `String` | Formatted display string |

---

### Breaking Change Types

#### Type Enum

| Type | Severity | Description |
|------|----------|-------------|
| `MESSAGE_REMOVED` | ERROR | Message was removed |
| `FIELD_REMOVED` | ERROR | Field was removed from message |
| `FIELD_NUMBER_CHANGED` | ERROR | Field number was changed |
| `FIELD_TYPE_INCOMPATIBLE` | ERROR | Field type changed incompatibly |
| `ENUM_REMOVED` | ERROR | Enum was removed |
| `ENUM_VALUE_REMOVED` | ERROR | Enum value was removed |
| `ENUM_VALUE_NUMBER_CHANGED` | ERROR | Enum value number changed |
| `REQUIRED_FIELD_ADDED` | WARNING | Required field added (proto2) |
| `LABEL_CHANGED_TO_REQUIRED` | WARNING | Field changed to required |
| `CARDINALITY_CHANGED` | WARNING | Field cardinality changed (repeated/singular) |
| `ONEOF_FIELD_MOVED` | WARNING | Field moved in/out of oneof |

#### Severity Enum

| Severity | Description |
|----------|-------------|
| `ERROR` | Breaking change that will cause wire incompatibility |
| `WARNING` | Potentially breaking change that may cause issues |

---

### Formatter Classes

All formatters implement the `DiffFormatter` interface:

```java
public interface DiffFormatter {
    String format(SchemaDiff diff);
    String formatBreakingOnly(SchemaDiff diff);
}
```

#### TextDiffFormatter

**Package:** `io.alnovis.protowrapper.diff.formatter`

Produces plain text output with ASCII separators.

```java
TextDiffFormatter formatter = new TextDiffFormatter();
String text = formatter.format(diff);
```

**Output Example:**

```
Schema Comparison: production -> development

================================================================================
MESSAGES
================================================================================

+ ADDED: Profile (user.proto)
    Fields:
      - user_id: int64 (#1)
      - bio: string (#2)

~ MODIFIED: User
    + Added field: phone (string, #7)
    - Removed field: email (#3) [BREAKING]

- REMOVED: DeprecatedMessage [BREAKING]

================================================================================
BREAKING CHANGES
================================================================================

ERRORS (2):
  [ERROR] FIELD_REMOVED: User.email (was: string email = 3)
  [ERROR] MESSAGE_REMOVED: DeprecatedMessage

================================================================================
SUMMARY
================================================================================

Messages:  +1 added, ~1 modified, -1 removed
Enums:     +0 added, ~0 modified, -0 removed
Breaking:  2 errors, 0 warnings
```

---

#### JsonDiffFormatter

**Package:** `io.alnovis.protowrapper.diff.formatter`

Produces JSON output for programmatic processing.

```java
JsonDiffFormatter formatter = new JsonDiffFormatter();
String json = formatter.format(diff);
```

**Output Structure:**

```json
{
  "v1": "production",
  "v2": "development",
  "summary": {
    "addedMessages": 1,
    "removedMessages": 1,
    "modifiedMessages": 1,
    "addedEnums": 0,
    "removedEnums": 0,
    "modifiedEnums": 0,
    "errorCount": 2,
    "warningCount": 0
  },
  "messages": {
    "added": [
      {
        "name": "Profile",
        "sourceFile": "user.proto",
        "fields": [
          {"name": "user_id", "type": "int64", "number": 1},
          {"name": "bio", "type": "string", "number": 2}
        ]
      }
    ],
    "removed": [
      {"name": "DeprecatedMessage"}
    ],
    "modified": [
      {
        "name": "User",
        "fieldChanges": [
          {
            "fieldNumber": 7,
            "fieldName": "phone",
            "changeType": "ADDED",
            "v2Type": "string",
            "breaking": false
          },
          {
            "fieldNumber": 3,
            "fieldName": "email",
            "changeType": "REMOVED",
            "v1Type": "string",
            "breaking": true
          }
        ]
      }
    ]
  },
  "enums": {
    "added": [],
    "removed": [],
    "modified": []
  },
  "breakingChanges": [
    {
      "type": "FIELD_REMOVED",
      "severity": "ERROR",
      "entityPath": "User.email",
      "description": "Field removed",
      "v1Value": "string email = 3"
    },
    {
      "type": "MESSAGE_REMOVED",
      "severity": "ERROR",
      "entityPath": "DeprecatedMessage",
      "description": "Message removed"
    }
  ]
}
```

---

#### MarkdownDiffFormatter

**Package:** `io.alnovis.protowrapper.diff.formatter`

Produces Markdown output with tables, suitable for documentation and PRs.

```java
MarkdownDiffFormatter formatter = new MarkdownDiffFormatter();
String markdown = formatter.format(diff);
```

**Output Example:**

```markdown
# Schema Comparison: production -> development

## Summary

| Category | Added | Modified | Removed |
|----------|-------|----------|---------|
| Messages | 1 | 1 | 1 |
| Enums | 0 | 0 | 0 |

**Breaking Changes:** 2 errors, 0 warnings

---

## Messages

### Added Messages

#### Profile
*Source: user.proto*

| Field | Type | Number |
|-------|------|--------|
| user_id | `int64` | 1 |
| bio | `string` | 2 |

### Modified Messages

#### User

| Change | Field | Details |
|--------|-------|---------|
| + Added | phone (#7) | `string` |
| - Removed | email (#3) | **BREAKING** |

### Removed Messages

- **DeprecatedMessage** - **BREAKING**

---

## Breaking Changes

| Severity | Type | Entity | Description |
|----------|------|--------|-------------|
| ERROR | Field Removed | User.email | Field removed (was: `string email = 3`) |
| ERROR | Message Removed | DeprecatedMessage | Message removed |
```

---

### Custom Formatter

You can create custom formatters by implementing `DiffFormatter`:

```java
public class XmlDiffFormatter implements DiffFormatter {
    @Override
    public String format(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<schemaDiff>\n");
        sb.append("  <v1>").append(diff.getV1Name()).append("</v1>\n");
        sb.append("  <v2>").append(diff.getV2Name()).append("</v2>\n");
        // ... format messages, enums, breaking changes
        sb.append("</schemaDiff>\n");
        return sb.toString();
    }

    @Override
    public String formatBreakingOnly(SchemaDiff diff) {
        // ... format only breaking changes
    }
}

// Usage
String xml = diff.format(new XmlDiffFormatter());
```

---

# Generated Code API

## Overview

Proto-wrapper generates a layer of version-agnostic wrapper classes on top of protobuf-generated code. This allows application code to work with multiple protocol versions through a unified API.

### Design Goals

1. **Version Agnostic API** — Single interface works with all protocol versions
2. **Type Safety** — Compile-time type checking for all operations
3. **Immutability** — All wrapper instances are immutable and thread-safe
4. **Lossless Conversion** — Data is preserved when converting between versions
5. **Transparent Conflicts** — Type differences between versions are handled automatically

### Architecture Pattern

The generated code follows a **Template Method** pattern:

```
Interface (api)
    ↓
AbstractClass (api.impl) — shared logic via template methods
    ↓
Implementation (v1, v2, ...) — version-specific extraction
```

---

## Package Structure

Generated code is organized into the following packages:

| Package | Contents | Purpose |
|---------|----------|---------|
| `{basePackage}.api` | Interfaces, Enums, VersionContext, ProtocolVersions | Version-agnostic public API |
| `{basePackage}.api.impl` | Abstract classes | Shared implementation logic |
| `{basePackage}.v1` | V1 implementations, VersionContextV1 | Version 1 specific code |
| `{basePackage}.v2` | V2 implementations, VersionContextV2 | Version 2 specific code |
| ... | Additional version packages | One package per protocol version |

---

## Generated Class Types

### Interface

**Location:** `{basePackage}.api`

**Purpose:** Defines the version-agnostic contract for a message type. Application code should depend only on these interfaces.

**Structure:**
```java
public interface {MessageName} {
    // Field accessors
    // Version information methods
    // Serialization methods
    // Version conversion methods
    // Builder access methods

    // Static factory methods
    static Builder newBuilder(VersionContext ctx);                           // v1.1.1+
    static {MessageName} parseFromBytes(VersionContext ctx, byte[] bytes);   // v1.6.9+

    interface Builder { ... }  // Optional, when generateBuilders=true
}
```

**Key Characteristics:**
- Contains only method signatures, no implementation
- Declares all fields from all versions (union of fields)
- Fields not present in some versions return `null` or default values
- Nested message types are declared as nested interfaces

---

### Abstract Class

**Location:** `{basePackage}.api.impl`

**Purpose:** Implements shared logic using Template Method pattern. Concrete implementations only need to provide version-specific field extraction.

**Structure:**
```java
public abstract class Abstract{MessageName}<PROTO extends Message>
    implements {MessageName} {

    protected final PROTO proto;

    // Template methods (abstract) — implemented by subclasses
    protected abstract {Type} extract{Field}(PROTO proto);
    protected abstract boolean extractHas{Field}(PROTO proto);

    // Final implementations — delegate to template methods
    public final {Type} get{Field}() { ... }
    public final boolean has{Field}() { ... }

    // Common implementations
    public final String toString() { ... }
    public final boolean equals(Object obj) { ... }
    public final int hashCode() { ... }
}
```

**Key Characteristics:**
- Generic type parameter `PROTO` represents the version-specific protobuf class
- Holds immutable reference to underlying proto message
- All public methods are `final` to ensure consistent behavior
- `extract*` methods are the only version-specific code

---

### Implementation Class

**Location:** `{basePackage}.v1`, `{basePackage}.v2`, etc.

**Purpose:** Provides version-specific implementation by extracting values from the concrete protobuf message type.

**Structure:**
```java
public class {MessageName} extends Abstract{MessageName}<{Proto.MessageName}> {

    // Constructor
    public {MessageName}({Proto.MessageName} proto);

    // Static factory
    public static {MessageName} from({Proto.MessageName} proto);

    // Static builder factory (when generateBuilders=true)
    public static Builder newBuilder();

    // Typed proto access
    public {Proto.MessageName} getTypedProto();

    // Template method implementations
    @Override
    protected {Type} extract{Field}({Proto.MessageName} proto) { ... }
}
```

**Key Characteristics:**
- Extends abstract class with concrete proto type
- Implements all `extract*` template methods
- Provides typed access to underlying proto via `getTypedProto()`
- Contains nested `BuilderImpl` class when builders are enabled

---

### VersionContext

**Location:**
- Interface: `{basePackage}.api.VersionContext`
- Implementations: `{basePackage}.v1.VersionContextV1`, `{basePackage}.v2.VersionContextV2`, etc.

**Purpose:** Factory for creating wrapper instances of a specific protocol version. Provides a single entry point for all wrapper creation operations.

**Structure (v1.7.0+):**
```java
public interface VersionContext {
    // Static fields (initialized at class load)
    Map<String, VersionContext> CONTEXTS;
    List<String> SUPPORTED_VERSIONS;
    String DEFAULT_VERSION;

    // Primary static factory methods (v1.7.0+)
    static VersionContext forVersionId(String versionId);      // throws if not found
    static Optional<VersionContext> find(String versionId);    // returns Optional
    static VersionContext getDefault();                        // latest version
    static List<String> supportedVersions();                   // ["v1", "v2"]
    static String defaultVersion();                            // "v2"
    static boolean isSupported(String versionId);              // true/false

    // Primary version info (v1.6.7+)
    String getVersionId();

    // Deprecated static factory - will be removed in v2.0
    @Deprecated(since = "1.6.7", forRemoval = true)
    static VersionContext forVersion(int version);

    // Deprecated version info - will be removed in v2.0
    @Deprecated(since = "1.6.7", forRemoval = true)
    int getVersion();

    // For each message type:
    {MessageName} wrap{MessageName}(Message proto);
    {MessageName} parse{MessageName}FromBytes(byte[] bytes) throws InvalidProtocolBufferException;
    {MessageName}.Builder new{MessageName}Builder();  // when generateBuilders=true

    // For nested types:
    {Parent}.{Nested} parse{Parent}{Nested}FromBytes(byte[] bytes) throws InvalidProtocolBufferException;
    {Parent}.{Nested}.Builder new{Parent}{Nested}Builder();
}
```

**Key Characteristics:**
- Singleton pattern — each version has one instance (`INSTANCE` field)
- Provides `wrap*` methods for wrapping existing proto messages
- Provides `parse*FromBytes` methods for deserializing from bytes
- Provides `new*Builder` methods for creating new instances
- Methods for types not present in a version throw `UnsupportedOperationException`

#### Version Identifier API (v1.6.7+)

Since v1.6.7, version identification uses String identifiers (`"v1"`, `"v2"`, `"legacy"`, etc.) instead of integers:

```java
// Recommended: use String-based API
VersionContext ctx = VersionContext.forVersionId("v2");
String versionId = ctx.getVersionId();  // "v2"

// Deprecated: integer API only works for numeric versions
VersionContext ctx = VersionContext.forVersion(2);  // @Deprecated
int version = ctx.getVersion();  // @Deprecated, returns 2
```

**Why String identifiers?**
- Supports custom version names (e.g., `"legacy"`, `"v2beta"`, `"production"`)
- Consistent with plugin configuration and Spring Boot starter
- Integer extraction is unreliable for non-numeric versions (e.g., `"legacy"` → 0)

**Migration:**
```java
// Before (deprecated)
VersionContext ctx = VersionContext.forVersion(1);
if (ctx.getVersion() == 2) { ... }

// After (recommended)
VersionContext ctx = VersionContext.forVersionId("v1");
if (ctx.getVersionId().equals("v2")) { ... }
```

### VersionContext Static Factory Methods (v1.7.0+)

Since v1.7.0, all factory methods are available directly on the `VersionContext` interface:

**Usage:**
```java
// Get specific version (throws if not found)
VersionContext v2 = VersionContext.forVersionId("v2");

// Safe lookup (returns Optional)
Optional<VersionContext> maybeCtx = VersionContext.find("v2");

// Get default (latest) version
VersionContext latest = VersionContext.getDefault();

// Check available versions
List<String> versions = VersionContext.supportedVersions();  // ["v1", "v2"]
String defaultVer = VersionContext.defaultVersion();         // "v2"
boolean supported = VersionContext.isSupported("v3");        // false
```

> **Note:** `VersionContextFactory` class was removed in v1.7.0. Use `VersionContext` static methods instead.

---

### ProtocolVersions

*New in v2.1.0*

**Location:** `{basePackage}.api.ProtocolVersions`

**Purpose:** Centralized class containing version identifier constants. Generated when `generateProtocolVersions=true`.

**Structure:**
```java
public final class ProtocolVersions {
    /** Version identifier: "v1" */
    public static final String V1 = "v1";

    /** Version identifier: "v2" */
    public static final String V2 = "v2";

    /** List of all supported versions */
    public static final List<String> ALL_VERSIONS = List.of(V1, V2);

    /** Default (latest) version */
    public static final String DEFAULT_VERSION = V2;

    private ProtocolVersions() {}
}
```

**Key Characteristics:**
- Contains `public static final String` constants for each version
- Constant names are uppercase version identifiers (e.g., `V1`, `V2`, `LEGACY`)
- Provides `ALL_VERSIONS` list and `DEFAULT_VERSION` constant
- Private constructor prevents instantiation
- When enabled, all generated code references these constants instead of string literals

**Benefits:**
- **Type Safety**: IDE autocomplete and compile-time checking for version strings
- **Refactoring**: Rename a version in one place
- **Discoverability**: See all versions in one class
- **Consistency**: Eliminates typos in version strings

**Usage:**
```java
// Without ProtocolVersions (string literals)
VersionContext ctx = VersionContext.forVersionId("v1");

// With ProtocolVersions (constants)
VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V1);

// Check supported versions
if (ProtocolVersions.ALL_VERSIONS.contains(requestedVersion)) {
    // ...
}

// Use default version
VersionContext defaultCtx = VersionContext.forVersionId(ProtocolVersions.DEFAULT_VERSION);
```

**Configuration:**

Maven:
```xml
<configuration>
    <generateProtocolVersions>true</generateProtocolVersions>
</configuration>
```

Gradle:
```kotlin
protoWrapper {
    generateProtocolVersions.set(true)
}
```

**Generated Code References:**

When `generateProtocolVersions=true`, the generated code uses constants:

```java
// VersionContext interface
public interface VersionContext {
    List<String> SUPPORTED_VERSIONS = List.of(
        ProtocolVersions.V1,
        ProtocolVersions.V2
    );
    String DEFAULT_VERSION = ProtocolVersions.V2;
}

// Implementation classes
public class OrderV1 {
    @Override
    public String getWrapperVersionId() {
        return ProtocolVersions.V1;
    }
}
```

When `generateProtocolVersions=false` (default), string literals are used directly.

---

### Enum

**Location:** `{basePackage}.api`

**Purpose:** Unified enum type combining values from all protocol versions.

**Structure:**
```java
public enum {EnumName} {
    VALUE_ONE(0),
    VALUE_TWO(1),
    /** Present only in versions: [v2] */
    VALUE_THREE(2);  // Version-specific values are documented

    private final int value;

    {EnumName}(int value);

    public int getValue();
    public static {EnumName} fromProtoValue(int value);
}
```

**Conflict Enum** (generated for INT_ENUM conflicts):
```java
public enum {FieldName}Enum {
    // Same structure, but also includes:
    public static {EnumName} fromProtoValueOrDefault(int value, {EnumName} defaultValue);
    public static {EnumName} fromProtoValueOrThrow(int value);
}
```

**Version-agnostic conversion methods** *(since 2.2.0)*:
```java
public enum {EnumName} {
    // Convert any proto enum to this wrapper enum (uses reflection on getNumber())
    public static {EnumName} fromProto(Object protoEnum);

    // Compare with any proto enum by numeric value
    public boolean matches(Object protoEnum);
}
```

`fromProto()` works with any version's proto enum by extracting the numeric value via reflection:
```java
// Works with any version's proto enum
CommandTypeEnum cmd = CommandTypeEnum.fromProto(v202Message.getCommand());
CommandTypeEnum cmd = CommandTypeEnum.fromProto(v203Message.getCommand());

// Compare directly
if (CommandTypeEnum.COMMAND_TICKET.matches(protoCommand)) { ... }
```

---

## Generated Methods

### Field Accessors

#### get{Field}()

**Signature:** `{Type} get{Field}()`

**Purpose:** Returns the value of a message field.

**Behavior:**
- For required/singular fields: returns the value
- For optional fields not present: returns `null` (objects) or default (primitives)
- For repeated fields: returns unmodifiable `List<T>`, never `null`

---

#### has{Field}()

**Signature:** `boolean has{Field}()`

**Purpose:** Checks if an optional field has a value set.

**Generated for:** Optional fields only.

**Returns:** `true` if field has a value, `false` otherwise.

---

#### supports{Field}()

**Signature:** `default boolean supports{Field}()`

**Purpose:** Checks if this field exists in the current protocol version.

**Generated for:** Fields that exist only in some versions.

**Returns:** `true` if the current wrapper version supports this field.

**Use case:** Allows conditional logic based on field availability:
```java
if (wrapper.supportsNewField()) {
    // Safe to call getNewField()
}
```

---

### Version Information

#### getWrapperVersionId() (v1.6.9+)

**Signature:** `String getWrapperVersionId()`

**Purpose:** Returns the version identifier of this wrapper instance.

**Returns:** Version identifier string (e.g., "v1", "v2", "legacy").

**Use case:** Runtime version detection for version-specific handling:

```java
String versionId = wrapper.getWrapperVersionId();  // "v1", "v2", etc.
if ("v2".equals(versionId)) {
    // V2-specific handling
}
```

---

#### getWrapperVersion() (deprecated)

**Signature:** `@Deprecated int getWrapperVersion()`

**Purpose:** Returns the protocol version number of this wrapper instance.

**Returns:** Version number (1, 2, 3, ...). Returns 0 for non-numeric version identifiers.

**Deprecated since:** 1.6.9. Use `getWrapperVersionId()` instead.

**Use case:** Legacy version detection (prefer `getWrapperVersionId()`).

---

#### getContext()

**Signature:** `VersionContext getContext()`

**Purpose:** Returns the VersionContext for this wrapper's version.

**Use case:** Creating related objects of the same version:
```java
VersionContext ctx = order.getContext();
Money payment = ctx.newMoneyBuilder().setAmount(100).build();
```

---

### Serialization

#### toBytes()

**Signature:** `byte[] toBytes()`

**Purpose:** Serializes the wrapper to protobuf binary format.

**Returns:** Protobuf-encoded byte array.

**Use case:** Network transmission, storage, version conversion.

---

#### parseFromBytes(VersionContext, byte[]) — Static Factory (v1.6.9+)

**Signature:** `static {Interface} parseFromBytes(VersionContext ctx, byte[] bytes)`

**Location:** On each interface (top-level and nested)

**Purpose:** Parses bytes into a wrapper instance using the specified version context.

**Parameters:**
- `ctx` — VersionContext determining the protocol version for parsing
- `bytes` — Serialized protobuf data

**Returns:** Parsed wrapper instance.

**Throws:** `InvalidProtocolBufferException` if the bytes are not valid protobuf data.

**Equivalence:** `Money.parseFromBytes(ctx, bytes)` is equivalent to `ctx.parseMoneyFromBytes(bytes)`.

**Use case:** More intuitive deserialization that mirrors the native protobuf pattern:
```java
// Native protobuf style
MyProto.MyMessage proto = MyProto.MyMessage.parseFrom(bytes);

// Proto wrapper style (matches the pattern)
MyMessage wrapper = MyMessage.parseFromBytes(ctx, bytes);
```

**Nested types:**
```java
// For nested interface Address.GeoLocation
Address.GeoLocation location = Address.GeoLocation.parseFromBytes(ctx, bytes);
```

---

#### getTypedProto()

**Signature:** `{Proto.Type} getTypedProto()` (on implementation class only)

**Purpose:** Returns the underlying typed protobuf message.

**Use case:** Interoperability with code expecting raw proto objects.

---

### Version Conversion

#### asVersion(Class\<T\>)

**Signature:** `<T extends {Interface}> T asVersion(Class<T> versionClass)`

**Purpose:** Converts wrapper to a different version implementation.

**Mechanism:** Serializes to bytes, then deserializes with target version's parser.

**Data preservation:** All data is preserved via protobuf's unknown field handling. Fields not in target version become inaccessible through API but remain in the serialized form.

---

#### asVersion(VersionContext)

**Signature:** `{Interface} asVersion(VersionContext targetContext)`

**Purpose:** Converts wrapper to a different version using VersionContext. More efficient than class-based conversion (no reflection).

**Mechanism:** Serializes to bytes via `toBytes()`, then parses with target context's `parseXxxFromBytes()` method.

**Example:**
```java
VersionContext v203 = VersionContext.forVersionId("v203");
TicketRequest v203Request = v202Request.asVersion(v203);
```

**Throws:** `RuntimeException` if conversion fails (e.g., `InvalidProtocolBufferException`).

*(since 2.2.0)*

---

#### asVersionStrict(Class\<T\>)

**Signature:** `<T extends {Interface}> T asVersionStrict(Class<T> versionClass)`

**Purpose:** Strict conversion that fails if any populated fields would become inaccessible.

**Throws:** `IllegalStateException` if conversion would make fields inaccessible.

**Use case:** When data accessibility is critical and silent field inaccessibility is unacceptable.

---

#### getFieldsInaccessibleInVersion(int)

**Signature:** `List<String> getFieldsInaccessibleInVersion(int targetVersion)`

**Purpose:** Lists fields that have values but would become inaccessible in target version.

**Returns:** List of field names, empty if all fields remain accessible.

**Use case:** Pre-conversion validation, logging, user warnings.

---

#### canConvertLosslesslyTo(int)

**Signature:** `default boolean canConvertLosslesslyTo(int targetVersion)`

**Purpose:** Checks if conversion preserves accessibility of all populated fields.

**Returns:** `true` if `getFieldsInaccessibleInVersion(targetVersion).isEmpty()`.

---

### Builder Methods

Generated when `generateBuilders=true`.

#### newBuilder(VersionContext) — Static Factory

**Signature:** `static Builder newBuilder(VersionContext ctx)`

**Location:** On each interface (top-level and nested)

**Purpose:** Creates an empty builder using the specified version context.

**Parameters:**
- `ctx` — VersionContext determining the protocol version

**Returns:** Empty builder for creating new instances.

**Equivalence:** `Money.newBuilder(ctx)` is equivalent to `ctx.newMoneyBuilder()`.

**Use case:** More intuitive builder creation that mirrors the native protobuf pattern:
```java
// Native protobuf style
MyProto.MyMessage proto = MyProto.MyMessage.newBuilder()
        .setField("value")
        .build();

// Proto wrapper style (matches the pattern)
MyMessage wrapper = MyMessage.newBuilder(ctx)
        .setField("value")
        .build();
```

**Nested types:**
```java
// For nested interface Address.GeoLocation
Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
        .setLatitude(40.7128)
        .setLongitude(-74.0060)
        .build();
```

---

#### toBuilder()

**Signature:** `Builder toBuilder()`

**Purpose:** Creates a builder initialized with current wrapper's values.

**Use case:** Creating modified copies of existing objects.

---

#### emptyBuilder()

**Signature:** `Builder emptyBuilder()`

**Purpose:** Creates an empty builder for the same version as this wrapper.

**Use case:** Creating new objects of the same version.

---

### Builder Interface Methods

#### set{Field}(value)

**Signature:** `Builder set{Field}({Type} value)`

**Purpose:** Sets the value of a field.

**Returns:** `this` for method chaining.

**Behavior for version-absent fields:** Throws `UnsupportedOperationException` with message indicating which versions support the field.

---

#### clear{Field}()

**Signature:** `Builder clear{Field}()`

**Purpose:** Clears an optional field to its default/unset state.

**Returns:** `this` for method chaining.

---

#### add{Field}(element) / addAll{Field}(list)

**Signature:** `Builder add{Field}({ElementType} element)`

**Purpose:** Adds element(s) to a repeated field.

**Generated for:** Repeated fields only.

---

#### build()

**Signature:** `{Interface} build()`

**Purpose:** Constructs the immutable wrapper instance.

**Returns:** New wrapper instance with builder's values.

---

## Type Conflict Resolution

When field types differ between protocol versions, the plugin generates unified accessors.

### INT_ENUM Conflict

**Condition:** Field is `int32` in some versions and `enum` in others.

**Generated methods:**

| Method | Returns | Purpose |
|--------|---------|---------|
| `get{Field}()` | `int` | Raw numeric value (works in all versions) |
| `get{Field}Enum()` | `{Field}Enum` | Type-safe enum value |

**Generated enum:** `{Field}Enum` with all enum values and `fromProtoValue()` converters.

---

### STRING_BYTES Conflict

**Condition:** Field is `string` in some versions and `bytes` in others.

**Generated methods:**

| Method | Returns | Purpose |
|--------|---------|---------|
| `get{Field}()` | `String` | String value (UTF-8 decoded for bytes) |
| `get{Field}Bytes()` | `byte[]` | Raw bytes (UTF-8 encoded for strings) |

---

### PRIMITIVE_MESSAGE Conflict

**Condition:** Field is primitive in some versions and message in others.

**Generated methods:**

| Method | Returns | Purpose |
|--------|---------|---------|
| `get{Field}()` | primitive type | Primitive value (0/default for message versions) |
| `get{Field}Message()` | message interface | Message value (null for primitive versions) |
| `supports{Field}()` | `boolean` | True if this version has primitive field |
| `supports{Field}Message()` | `boolean` | True if this version has message field |

---

### WIDENING Conflict

**Condition:** Field type widens between versions (e.g., `int32` → `int64`).

**Resolution:** Uses the wider type in the interface.

**Example:** `int32` in v1, `int64` in v2 → interface declares `long get{Field}()`.

---

## Repeated Fields with Type Conflicts

Since v1.4.0, proto-wrapper provides full builder support for repeated fields with type conflicts across versions.

### Supported Conflict Types

| Conflict | v1 Type | v2 Type | Unified Type | Range Validation |
|----------|---------|---------|--------------|------------------|
| **WIDENING** | `repeated int32` | `repeated int64` | `List<Long>` | Yes |
| **FLOAT_DOUBLE** | `repeated float` | `repeated double` | `List<Double>` | Yes |
| **SIGNED_UNSIGNED** | `repeated int32` | `repeated uint32` | `List<Long>` | Yes |
| **INT_ENUM** | `repeated int32` | `repeated SomeEnum` | `List<Integer>` | No |
| **STRING_BYTES** | `repeated string` | `repeated bytes` | `List<String>` | No |

### Generated Interface

```java
// Proto:
// v1: repeated int32 numbers = 1;
// v2: repeated int64 numbers = 1;

public interface RepeatedConflicts {
    // Getter - unified as wider type
    List<Long> getNumbers();

    interface Builder {
        // Add single element
        Builder addNumbers(long value);

        // Add multiple elements
        Builder addAllNumbers(List<Long> values);

        // Replace all elements
        Builder setNumbers(List<Long> values);

        // Clear all elements
        Builder clearNumbers();
    }
}
```

### Range Validation

When adding values to a version with a narrower type, runtime validation ensures values fit within the target type's range.

```java
// V1 builder (int32 range: -2,147,483,648 to 2,147,483,647):
wrapper.toBuilder()
    .addNumbers(100L)              // OK - within int32 range
    .addNumbers(2_147_483_647L)    // OK - max int32
    .addNumbers(9_999_999_999L)    // throws IllegalArgumentException
    .build();

// V2 builder (int64 range):
wrapper.toBuilder()
    .addNumbers(9_999_999_999L)    // OK - within int64 range
    .build();
```

**Error Messages:**

```
IllegalArgumentException: Value 9999999999 exceeds int32 range for v1
IllegalArgumentException: Value 1.0E309 exceeds float range for v1
IllegalArgumentException: Value -1 exceeds uint32 range for v2
```

### Usage Examples

#### Basic Usage

```java
VersionContext ctx = VersionContext.forVersionId("v2");

// Create new message
RepeatedConflicts message = RepeatedConflicts.newBuilder(ctx)
    .addNumbers(100L)
    .addNumbers(200L)
    .addNumbers(300L)
    .build();

assertThat(message.getNumbers()).containsExactly(100L, 200L, 300L);
```

#### Add Multiple Elements

```java
RepeatedConflicts modified = message.toBuilder()
    .addAllNumbers(List.of(400L, 500L))
    .build();

assertThat(modified.getNumbers()).containsExactly(100L, 200L, 300L, 400L, 500L);
```

#### Replace All Elements

```java
RepeatedConflicts replaced = message.toBuilder()
    .setNumbers(List.of(1L, 2L, 3L))
    .build();

assertThat(replaced.getNumbers()).containsExactly(1L, 2L, 3L);
```

#### Clear Elements

```java
RepeatedConflicts cleared = message.toBuilder()
    .clearNumbers()
    .build();

assertThat(cleared.getNumbers()).isEmpty();
```

### FLOAT_DOUBLE Conflict Example

```java
// v1: repeated float values = 1;
// v2: repeated double values = 2;

// Interface uses wider type (double)
List<Double> getValues();

// V1 builder validates float range
wrapper.toBuilder()
    .addValues(3.14)              // OK
    .addValues(Float.MAX_VALUE)   // OK
    .addValues(Double.MAX_VALUE)  // throws - exceeds float range
    .build();
```

### STRING_BYTES Conflict Example

```java
// v1: repeated string texts = 1;
// v2: repeated bytes texts = 2;

// Interface uses String with UTF-8 conversion
List<String> getTexts();

// Both versions accept String
wrapper.toBuilder()
    .addTexts("Hello")
    .addTexts("World")
    .addTexts("Unicode: \u4E2D\u6587")  // UTF-8 encoded for bytes version
    .build();
```

### INT_ENUM Conflict Example

```java
// v1: repeated int32 codes = 1;
// v2: repeated Status codes = 2;  // enum

// Interface uses Integer
List<Integer> getCodes();

// Use enum number values
wrapper.toBuilder()
    .addCodes(0)   // Status.UNKNOWN
    .addCodes(1)   // Status.ACTIVE
    .addCodes(2)   // Status.DELETED
    .build();
```

### Implementation Details

The generated code follows the Template Method pattern:

```java
// Abstract class
public abstract class AbstractRepeatedConflicts {
    public abstract static class AbstractBuilder {
        // Abstract methods for version-specific implementation
        protected abstract void doAddNumbers(long value);
        protected abstract void doAddAllNumbers(List<Long> values);
        protected abstract void doSetNumbers(List<Long> values);
        protected abstract void doClearNumbers();

        // Concrete public methods
        public final Builder addNumbers(long value) {
            doAddNumbers(value);
            return this;
        }
        // ... other methods
    }
}

// V1 Implementation with range validation
public class RepeatedConflicts extends AbstractRepeatedConflicts {
    private static class BuilderImpl extends AbstractBuilder {
        @Override
        protected void doAddNumbers(long value) {
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Value " + value + " exceeds int32 range for v1");
            }
            protoBuilder.addNumbers((int) value);
        }
    }
}

// V2 Implementation (no validation needed for wider type)
public class RepeatedConflicts extends AbstractRepeatedConflicts {
    private static class BuilderImpl extends AbstractBuilder {
        @Override
        protected void doAddNumbers(long value) {
            protoBuilder.addNumbers(value);  // Direct assignment
        }
    }
}
```

---

## Well-Known Types

Since v1.3.0, proto-wrapper automatically converts Google Well-Known Types to idiomatic Java types.

### Supported Types

| Proto Type | Java Type | Notes |
|------------|-----------|-------|
| `google.protobuf.Timestamp` | `java.time.Instant` | Epoch-based time |
| `google.protobuf.Duration` | `java.time.Duration` | Time interval |
| `google.protobuf.StringValue` | `String` | Nullable string |
| `google.protobuf.Int32Value` | `Integer` | Nullable int |
| `google.protobuf.Int64Value` | `Long` | Nullable long |
| `google.protobuf.UInt32Value` | `Long` | Unsigned, nullable |
| `google.protobuf.UInt64Value` | `Long` | Unsigned, nullable |
| `google.protobuf.BoolValue` | `Boolean` | Nullable boolean |
| `google.protobuf.FloatValue` | `Float` | Nullable float |
| `google.protobuf.DoubleValue` | `Double` | Nullable double |
| `google.protobuf.BytesValue` | `byte[]` | Nullable bytes |
| `google.protobuf.FieldMask` | `List<String>` | Field paths |
| `google.protobuf.Struct` | `Map<String, Object>` | JSON-like |
| `google.protobuf.Value` | `Object` | Dynamic type |
| `google.protobuf.ListValue` | `List<Object>` | Dynamic list |

### Generated Interface Example

```java
// Proto definition:
// message Event {
//     google.protobuf.Timestamp created_at = 1;
//     google.protobuf.Duration timeout = 2;
//     google.protobuf.StringValue optional_name = 3;
//     google.protobuf.Struct metadata = 4;
// }

// Generated interface:
public interface Event {
    Instant getCreatedAt();         // java.time.Instant
    Duration getTimeout();           // java.time.Duration
    String getOptionalName();        // nullable String
    Map<String, Object> getMetadata(); // JSON-like structure

    boolean hasCreatedAt();
    boolean hasTimeout();
    boolean hasOptionalName();
    boolean hasMetadata();
}
```

### Temporal Types (Timestamp, Duration)

```java
Event event = ctx.wrapEvent(protoEvent);

// Get as Java types
Instant createdAt = event.getCreatedAt();
Duration timeout = event.getTimeout();

// Use Java Time API
LocalDateTime local = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
long seconds = timeout.getSeconds();
int nanos = timeout.getNano();
```

### Wrapper Types (Nullable Primitives)

```java
// These can distinguish "not set" from default value
String name = event.getOptionalName();  // null if not set
Integer count = event.getOptionalCount(); // null if not set

// Check if set
if (event.hasOptionalName()) {
    String name = event.getOptionalName();
}
```

### Struct/Value/ListValue (JSON-like)

```java
// Get as Java Map
Map<String, Object> metadata = event.getMetadata();

// Values can be: null, Double, String, Boolean, Map<String,Object>, List<Object>
String name = (String) metadata.get("name");
Double count = (Double) metadata.get("count");
Map<?, ?> nested = (Map<?, ?>) metadata.get("nested");
List<?> items = (List<?>) metadata.get("items");
```

### StructConverter Utility Class

When Struct/Value/ListValue fields are used, a utility class is auto-generated:

```java
// Generated in your API package
public final class StructConverter {
    public static Map<String, Object> toMap(Struct struct);
    public static Struct toStruct(Map<String, ?> map);
    public static Object toObject(Value value);
    public static Value toValue(Object obj);
    public static List<Object> toList(ListValue listValue);
    public static ListValue toListValue(List<?> list);
}
```

### Repeated Well-Known Types

```java
// Proto: repeated google.protobuf.Timestamp events = 1;
List<Instant> getEvents();  // List<java.time.Instant>

// Proto: repeated google.protobuf.StringValue optional_names = 2;
List<String> getOptionalNames();  // List<String>
```

### Builder Support

```java
Event event = Event.newBuilder(ctx)
    .setCreatedAt(Instant.now())
    .setTimeout(Duration.ofMinutes(5))
    .setOptionalName("test-event")
    .setMetadata(Map.of(
        "key1", "value1",
        "key2", 123.45,
        "nested", Map.of("a", "b")
    ))
    .build();
```

### Configuration

| Option | Default | Description |
|--------|---------|-------------|
| `convertWellKnownTypes` | `true` | Enable/disable WKT conversion |
| `generateRawProtoAccessors` | `false` | Generate `getXxxProto()` methods |

```xml
<configuration>
    <convertWellKnownTypes>true</convertWellKnownTypes>
    <generateRawProtoAccessors>false</generateRawProtoAccessors>
</configuration>
```

### Raw Proto Accessors

When `generateRawProtoAccessors=true`:

```java
public interface Event {
    // Converted accessor
    Instant getCreatedAt();

    // Raw proto accessor (additional)
    com.google.protobuf.Timestamp getCreatedAtProto();
}
```

### Not Supported: google.protobuf.Any

`Any` type requires a runtime type registry to unpack, which conflicts with the plugin's design principle of inline code with no runtime dependencies.

Workaround using raw accessor:

```java
// With generateRawProtoAccessors=true
Any any = message.getPayloadProto();
if (any.is(User.class)) {
    User user = any.unpack(User.class);
}
```

---

## Map Fields

Proto-wrapper provides full support for protobuf `map<K, V>` fields, including type conflict handling across versions.

### Generated Methods

For each map field, the following methods are generated:

| Method | Returns | Purpose |
|--------|---------|---------|
| `get{Field}Map()` | `Map<K, V>` | Returns unmodifiable map of all entries |
| `get{Field}Count()` | `int` | Returns number of entries |
| `contains{Field}(K key)` | `boolean` | Checks if key exists |
| `get{Field}OrDefault(K key, V default)` | `V` | Returns value or default if not present |
| `get{Field}OrThrow(K key)` | `V` | Returns value or throws if not present |

### Interface Example

```java
public interface MapTestMessage {
    // Map<String, Integer> field
    Map<String, Integer> getCountsMap();
    int getCountsCount();
    boolean containsCounts(String key);
    int getCountsOrDefault(String key, int defaultValue);
    int getCountsOrThrow(String key);

    // For version-specific maps
    boolean supportsCounts();  // Generated for fields not in all versions
}
```

### Builder Methods

```java
interface Builder {
    // Put single entry
    Builder putCounts(String key, int value);

    // Put all entries from map
    Builder putAllCounts(Map<String, Integer> values);

    // Remove entry by key
    Builder removeCounts(String key);

    // Clear all entries
    Builder clearCounts();

    // Get current map state (for inspection during building)
    Map<String, Integer> getCountsMap();
}
```

### Usage Example

```java
VersionContext ctx = VersionContext.forVersionId("v1");

// Create with map entries
MapTestMessage message = MapTestMessage.newBuilder(ctx)
        .putCounts("apple", 5)
        .putCounts("banana", 3)
        .putAllCounts(Map.of("cherry", 10, "date", 7))
        .build();

// Read map data
Map<String, Integer> counts = message.getCountsMap();
int appleCount = message.getCountsOrDefault("apple", 0);  // 5
boolean hasGrape = message.containsCounts("grape");        // false

// Iterate
for (Map.Entry<String, Integer> entry : counts.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

### Map Type Conflicts

When map value types differ between versions, the plugin automatically handles conversion.

#### WIDENING Map Conflict

**Condition:** Map value type widens (e.g., `map<string, int32>` → `map<string, int64>`).

**Resolution:** Uses wider type in interface, automatic conversion in implementations.

```java
// v1: map<string, int32> counts = 1;
// v2: map<string, int64> counts = 1;

public interface Message {
    Map<String, Long> getCountsMap();  // Unified as Long
}
```

**Implementation behavior:**
- V1: Converts `int` values to `long` on read
- V2: Returns `long` values directly
- Builder validates range when setting values for V1

#### INT_ENUM Map Conflict

**Condition:** Map value is `int32` in some versions and `enum` in others.

**Resolution:** Uses `int` as unified type, with automatic enum conversion.

```java
// v1: map<string, int32> status_map = 1;
// v2: map<string, Status> status_map = 1;  // Status enum

public interface Message {
    Map<String, Integer> getStatusMapMap();  // Unified as Integer
}
```

**Implementation behavior:**
- V1: Returns `int` values directly
- V2: Converts enum to `int` using `getNumber()` (NOT `ordinal()`)
- Builder validates enum values when setting for V2

**Important:** For non-sequential enum values (e.g., `UNKNOWN=0, ACTIVE=10, DELETED=20`), the correct proto number is preserved. Using `ordinal()` would incorrectly return 0, 1, 2.

### Performance Optimization: Lazy Caching

For maps requiring type conversion (WIDENING, INT_ENUM), the generated code uses lazy caching with `volatile` fields for thread-safe performance:

```java
// Generated implementation (simplified)
private volatile Map<String, Long> cachedWideningCountsMap;

@Override
protected Map<String, Long> extractWideningCountsMap(Proto proto) {
    if (cachedWideningCountsMap != null) {
        return cachedWideningCountsMap;
    }
    // Convert and cache
    Map<String, Integer> source = proto.getWideningCountsMap();
    Map<String, Long> result = new LinkedHashMap<>(source.size());
    source.forEach((k, v) -> result.put(k, v.longValue()));
    cachedWideningCountsMap = result;
    return result;
}
```

**Benefits:**
- First access computes and caches the converted map
- Subsequent accesses return cached value immediately
- Thread-safe via `volatile` field
- Immutable wrapper ensures cache validity

### Builder Validation

The builder validates enum values when putting entries for versions that use enum types:

```java
MapConflictMessage.Builder builder = MapConflictMessage.newBuilder(ctx);

// Valid enum values
builder.putStatusMap("key1", 0);   // OK - UNKNOWN
builder.putStatusMap("key2", 1);   // OK - ACTIVE

// Invalid enum value
builder.putStatusMap("key3", 999); // Throws IllegalArgumentException
// Message: "Invalid enum value 999 for field 'status_map'"
```

### Version-Specific Maps

Maps that exist only in some versions:

```java
// scores map only in V2
if (message.supportsScores()) {
    Map<String, Double> scores = message.getScoresMap();
}

// In V1, getScoresMap() returns empty map
```

### Cross-Version Conversion

Map data is preserved during version conversion:

```java
// V1 wrapper with map data
MapTestMessage v1 = ctx1.newMapTestMessageBuilder()
        .putCounts("key", 100)
        .build();

// Convert to V2
MapTestMessage v2 = v1.asVersion(space.example.v2.MapTestMessage.class);

// Data preserved
assertEquals(100, v2.getCountsOrThrow("key"));
```

---

## Nested Types

Nested protobuf messages are generated as nested interfaces/classes.

**Structure:**
```java
public interface {Parent} {

    interface {Nested} {
        // Field accessors

        // Static factory method (v1.1.1+)
        static Builder newBuilder(VersionContext ctx);

        interface Builder { ... }
    }

    {Nested} get{Nested}();
    boolean has{Nested}();
}
```

**Creating nested type instances:**

```java
// Using static newBuilder (recommended, v1.1.1+)
Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
        .setLatitude(40.7128)
        .setLongitude(-74.0060)
        .build();

// Using VersionContext method (still works)
Address.GeoLocation location = ctx.newAddressGeoLocationBuilder()
        .setLatitude(40.7128)
        .setLongitude(-74.0060)
        .build();
```

**VersionContext methods for nested types:**
```java
{Parent}.{Nested}.Builder new{Parent}{Nested}Builder();
```

**Deep nesting:** Supported to arbitrary depth:
- Static method: `Parent.Child.GrandChild.newBuilder(ctx)`
- Context method: `ctx.newParentChildGrandChildBuilder()`

---

## Oneof Fields

Proto-wrapper provides full support for protobuf `oneof` fields, generating type-safe discriminator enums and appropriate accessor methods.

### Case Enum

For each oneof group, a nested Case enum is generated:

```java
public interface Payment {
    // Case enum for oneof 'method'
    enum MethodCase {
        CREDIT_CARD(10),
        BANK_TRANSFER(11),
        CRYPTO(12),           // V2 only
        METHOD_NOT_SET(0);

        private final int number;

        MethodCase(int number) { this.number = number; }
        public int getNumber() { return number; }

        public static MethodCase forNumber(int number) {
            for (MethodCase c : values()) {
                if (c.number == number) return c;
            }
            return METHOD_NOT_SET;
        }
    }

    // Discriminator method
    MethodCase getMethodCase();

    // Individual field checks
    boolean hasCreditCard();
    boolean hasBankTransfer();
    boolean hasCrypto();

    // Field accessors
    CreditCard getCreditCard();
    BankTransfer getBankTransfer();
    Crypto getCrypto();
}
```

### Usage Example

```java
VersionContext ctx = VersionContext.forVersionId("v1");

// Create with credit card
CreditCard card = ctx.newCreditCardBuilder()
        .setCardNumber("4111111111111111")
        .setExpiry("12/25")
        .build();

Payment payment = ctx.newPaymentBuilder()
        .setId("PAY-001")
        .setAmount(10000L)
        .setCreditCard(card)
        .build();

// Check which field is set
switch (payment.getMethodCase()) {
    case CREDIT_CARD -> handleCard(payment.getCreditCard());
    case BANK_TRANSFER -> handleTransfer(payment.getBankTransfer());
    case CRYPTO -> handleCrypto(payment.getCrypto());
    case METHOD_NOT_SET -> handleNoMethod();
}
```

### Builder Methods

```java
Payment.Builder builder = ctx.newPaymentBuilder()
        .setId("PAY-001")
        .setAmount(10000L);

// Set oneof field (replaces any previously set field)
builder.setCreditCard(card);
builder.setBankTransfer(transfer);  // Replaces credit card

// Clear oneof group
builder.clearMethod();  // All oneof fields become unset

Payment payment = builder.build();
```

### Version Differences

When oneof options differ between versions:

- **V1 has 2 options:** CREDIT_CARD, BANK_TRANSFER
- **V2 has 3 options:** CREDIT_CARD, BANK_TRANSFER, CRYPTO

The merged interface/enum includes all options from all versions. When converting:

- **V1 -> V2:** Works seamlessly, V1 fields are preserved
- **V2 -> V1 with CRYPTO:** Field is silently dropped (V1 doesn't support it)

---

### Oneof Conflict Detection

The plugin automatically detects and logs conflicts related to oneof fields across versions. Conflicts are logged at WARN level during code generation.

#### Conflict Types

| Conflict Type | Description | Example |
|--------------|-------------|---------|
| `PARTIAL_EXISTENCE` | Oneof exists only in some versions | V1: no oneof, V2: has oneof "method" |
| `FIELD_SET_DIFFERENCE` | Different fields in oneof across versions | V1: {credit_card, bank_transfer}, V2: adds {crypto} |
| `FIELD_TYPE_CONFLICT` | Type conflict within oneof field | V1: string text=1, V2: bytes text=1 |
| `RENAMED` | Oneof renamed between versions | V1: "payment_method", V2: "method" (same fields) |
| `FIELD_MEMBERSHIP_CHANGE` | Field moved in/out of oneof | V1: regular field, V2: inside oneof |
| `FIELD_NUMBER_CHANGE` | Field number changed | V1: credit_card=10, V2: credit_card=15 |
| `FIELD_REMOVED` | Field removed from oneof | V1: {credit_card, cash}, V2: {credit_card} only |
| `INCOMPATIBLE_TYPES` | Incompatible field types in oneof | Cannot be safely merged |

#### Renamed Oneof Detection

The plugin detects renamed oneofs by comparing field number sets:

```protobuf
// V1
message Payment {
    oneof payment_method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}

// V2 - same fields, different oneof name
message Payment {
    oneof method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}
```

When detected, the plugin:
1. Merges both oneofs into a single `MergedOneof`
2. Uses the most common name across versions
3. Logs a warning: `Oneof RENAMED: 'Payment.method' has different names: {v1=payment_method, v2=method}`

#### Field Membership Changes

Detects when a field moves in or out of a oneof:

```protobuf
// V1 - regular field
message Payment {
    CreditCard credit_card = 10;
}

// V2 - now inside oneof
message Payment {
    oneof method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}
```

Warning: `Oneof MEMBERSHIP: 'Payment' - Field 'credit_card' (#10) moved into oneof in versions: [v2]`

#### Example Log Output

```
[WARN] Oneof PARTIAL: 'Payment.method' not in all versions - Missing in: [v1]
[WARN] Oneof FIELD_DIFF: 'Payment.method' - Field #12 present only in: [v2]
[WARN] Oneof RENAMED: 'Payment.method' has different names: {v1=payment_method, v2=method}
[WARN] Oneof MEMBERSHIP: 'Payment' - Field 'credit_card' (#10) moved into oneof
```

#### Accessing Conflict Information Programmatically

The `MergedOneof` class provides methods to query detected conflicts:

```java
MergedOneof oneof = mergedMessage.findOneofByName("method").orElseThrow();

// Check for conflicts
if (oneof.hasConflicts()) {
    List<OneofConflictInfo> conflicts = oneof.getConflicts();

    for (OneofConflictInfo conflict : conflicts) {
        System.out.println("Type: " + conflict.getType());
        System.out.println("Description: " + conflict.getDescription());
        System.out.println("Affected versions: " + conflict.getAffectedVersions());
    }
}

// Check for specific conflict type
if (oneof.hasConflictOfType(OneofConflictType.RENAMED)) {
    Set<String> names = oneof.getMergedFromNames();
    System.out.println("Oneof was renamed, original names: " + names);
}

// Check if oneof exists in all versions
Set<String> allVersions = Set.of("v1", "v2", "v3");
if (!oneof.isUniversal(allVersions)) {
    Set<String> missing = oneof.getMissingVersions(allVersions);
    System.out.println("Oneof missing in: " + missing);
}
```

---

## Thread Safety

- **Wrapper instances:** Immutable, thread-safe
- **Builder instances:** NOT thread-safe, use separate builder per thread
- **VersionContext:** Thread-safe singleton

---

## See Also

- [COOKBOOK.md](COOKBOOK.md) — Practical usage examples
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) — Limitations and workarounds
- [VERSION_AGNOSTIC_API.md](VERSION_AGNOSTIC_API.md) — Design philosophy
- [RELEASE_NOTES.md](../RELEASE_NOTES.md) — Release notes for v1.5.0 with CLI, Maven, Gradle usage
