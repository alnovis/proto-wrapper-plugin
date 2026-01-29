# Schema Metadata

Runtime access to schema information, enum values, and version diffs.

**Since:** v2.3.1

---

## Overview

Schema Metadata provides runtime introspection of your protobuf schema without hardcoding values. This is useful for:

- **Dynamic validation**: Check if an enum value is valid for a version at runtime
- **Migration tooling**: Understand what changed between versions programmatically
- **Documentation generation**: Extract field and enum information from generated code
- **Error messages**: Provide helpful context when parsing fails (e.g., "Unknown enum value 999. Valid values: VAT(100), EXCISE(200)")

---

## Quick Start

### Enable Schema Metadata

**Maven:**

```xml
<configuration>
    <generateSchemaMetadata>true</generateSchemaMetadata>
</configuration>
```

**Gradle:**

```kotlin
protoWrapper {
    generateSchemaMetadata.set(true)
}
```

### Generated Code

With `generateSchemaMetadata=true`, the plugin generates:

| Class | Description |
|-------|-------------|
| `SchemaInfoV1`, `SchemaInfoV2`, ... | Enum and message metadata for each version |
| `SchemaDiffV1ToV2`, ... | Schema changes between consecutive versions |

Plus additional methods on `VersionContext`:

| Method | Description |
|--------|-------------|
| `getSchemaInfo()` | Get metadata for this version |
| `getDiffFrom(String)` | Get diff from a previous version |

---

## Usage Examples

### Accessing Enum Values at Runtime

```java
VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V2);
SchemaInfo schema = ctx.getSchemaInfo();

// Get enum metadata
schema.getEnum("TaxTypeEnum").ifPresent(enumInfo -> {
    System.out.println("Enum: " + enumInfo.getName());
    System.out.println("Full name: " + enumInfo.getFullName());

    for (SchemaInfo.EnumValue value : enumInfo.getValues()) {
        System.out.println("  " + value.name() + " = " + value.number());
    }
});
```

Output:
```
Enum: TaxTypeEnum
Full name: com.example.proto.v2.TaxTypeEnum
  VAT = 100
  EXCISE = 200
  NO_TAX = 0
```

### Validating Enum Values

```java
public void validateTaxType(int rawValue, String versionId) {
    VersionContext ctx = VersionContext.forVersionId(versionId);
    SchemaInfo schema = ctx.getSchemaInfo();

    Optional<SchemaInfo.EnumInfo> taxEnum = schema.getEnum("TaxTypeEnum");
    if (taxEnum.isEmpty()) {
        throw new IllegalStateException("TaxTypeEnum not found in " + versionId);
    }

    boolean isValid = taxEnum.get().getValues().stream()
            .anyMatch(v -> v.number() == rawValue);

    if (!isValid) {
        String validValues = taxEnum.get().getValues().stream()
                .map(v -> v.name() + "(" + v.number() + ")")
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(
            "Invalid TaxType value " + rawValue + ". Valid values: " + validValues);
    }
}
```

### Understanding Version Differences

```java
VersionContext v2Ctx = VersionContext.forVersionId(ProtocolVersions.V2);

v2Ctx.getDiffFrom(ProtocolVersions.V1).ifPresent(diff -> {
    System.out.println("Changes from " + diff.getFromVersion() + " to " + diff.getToVersion());

    // List all field changes
    for (VersionSchemaDiff.FieldChange fc : diff.getFieldChanges()) {
        System.out.println("  " + fc.messageName() + "." + fc.fieldName() +
                           ": " + fc.changeType());
        if (fc.hasHint()) {
            System.out.println("    Hint: " + fc.migrationHint());
        }
    }

    // Get specific field change
    diff.findFieldChange("Tax", "type").ifPresent(fc -> {
        System.out.println("Tax.type changed: " + fc.oldType() + " -> " + fc.newType());
    });
});
```

### Migration Helper

```java
public class VersionMigrationHelper {

    public void logBreakingChanges(String fromVersion, String toVersion) {
        VersionContext ctx = VersionContext.forVersionId(toVersion);

        ctx.getDiffFrom(fromVersion).ifPresent(diff -> {
            // Removed fields - need migration
            List<VersionSchemaDiff.FieldChange> removed = diff.getRemovedFields();
            if (!removed.isEmpty()) {
                System.out.println("WARNING: Fields removed in " + toVersion + ":");
                for (var fc : removed) {
                    System.out.println("  - " + fc.messageName() + "." + fc.fieldName());
                }
            }

            // Type changes - may need conversion
            List<VersionSchemaDiff.FieldChange> typeChanged = diff.getTypeChangedFields();
            if (!typeChanged.isEmpty()) {
                System.out.println("INFO: Fields with type changes:");
                for (var fc : typeChanged) {
                    System.out.println("  - " + fc.messageName() + "." + fc.fieldName() +
                                       ": " + fc.oldType() + " -> " + fc.newType());
                }
            }

            // Enum changes
            for (var ec : diff.getEnumChanges()) {
                if (!ec.removedValues().isEmpty()) {
                    System.out.println("WARNING: Removed enum values from " + ec.enumName() + ":");
                    for (String v : ec.removedValues()) {
                        System.out.println("  - " + v);
                    }
                }
            }
        });
    }
}
```

---

## API Reference

### SchemaInfo Interface

```java
public interface SchemaInfo {
    /** Version identifier (e.g., "v1", "v2") */
    String getVersionId();

    /** All enums in this version, keyed by simple name */
    Map<String, EnumInfo> getEnums();

    /** All messages in this version, keyed by simple name */
    Map<String, MessageInfo> getMessages();

    /** Find enum by name */
    default Optional<EnumInfo> getEnum(String name);

    /** Find message by name */
    default Optional<MessageInfo> getMessage(String name);

    // Nested types
    interface EnumInfo {
        String getName();
        String getFullName();
        List<EnumValue> getValues();
    }

    record EnumValue(String name, int number) {}

    interface MessageInfo {
        String getName();
        String getFullName();
        Map<String, FieldInfo> getFields();
    }

    interface FieldInfo {
        String getName();
        int getNumber();
        String getType();
    }
}
```

### VersionSchemaDiff Interface

```java
public interface VersionSchemaDiff {
    /** Source version (e.g., "v1") */
    String getFromVersion();

    /** Target version (e.g., "v2") */
    String getToVersion();

    /** All field changes */
    List<FieldChange> getFieldChanges();

    /** All enum changes */
    List<EnumChange> getEnumChanges();

    /** Find specific field change */
    default Optional<FieldChange> findFieldChange(String messageName, String fieldName);

    /** Get added fields */
    default List<FieldChange> getAddedFields();

    /** Get removed fields */
    default List<FieldChange> getRemovedFields();

    /** Get fields with type changes */
    default List<FieldChange> getTypeChangedFields();

    /** Get renamed fields */
    default List<FieldChange> getRenamedFields();

    /** Check if any changes exist */
    default boolean hasChanges();

    // Change type enums
    enum FieldChangeType {
        ADDED, REMOVED, RENAMED, TYPE_CHANGED, NUMBER_CHANGED, MOVED
    }

    enum EnumChangeType {
        ADDED, REMOVED, VALUES_CHANGED
    }

    // Change records with factory methods
    record FieldChange(
        String messageName,
        String fieldName,
        FieldChangeType changeType,
        String oldType,
        String newType,
        String oldFieldName,
        String newFieldName,
        String newMessageName,
        String migrationHint
    ) {
        // Factory methods: added(), removed(), typeChanged(), renamed(), moved()
    }

    record EnumChange(
        String enumName,
        EnumChangeType changeType,
        List<String> addedValues,
        List<String> removedValues,
        String migrationHint
    ) {
        // Factory methods: added(), removed(), valuesChanged()
    }
}
```

### VersionContext Methods

When `generateSchemaMetadata=true`, `VersionContext` gains two new methods:

```java
public interface VersionContext {
    // ... existing methods ...

    /**
     * Get schema metadata for this version.
     * @return schema info with enum/message metadata
     */
    SchemaInfo getSchemaInfo();

    /**
     * Get schema diff from a previous version to this version.
     * @param previousVersion version ID to diff from
     * @return optional diff if available, empty if no diff exists
     */
    Optional<VersionSchemaDiff> getDiffFrom(String previousVersion);
}
```

---

## Generated Package Structure

```
com.example.model/
├── api/
│   ├── VersionContext.java     # + getSchemaInfo(), getDiffFrom()
│   └── ...
├── metadata/                   # NEW: Schema metadata package
│   ├── SchemaInfoV1.java       # implements SchemaInfo
│   ├── SchemaInfoV2.java       # implements SchemaInfo
│   └── SchemaDiffV1ToV2.java   # implements VersionSchemaDiff
├── v1/
│   └── VersionContextV1.java   # implements getSchemaInfo(), getDiffFrom()
└── v2/
    └── VersionContextV2.java   # implements getSchemaInfo(), getDiffFrom()
```

---

## Runtime Dependencies

Schema Metadata uses only standard Java types and records. No additional runtime dependencies are required.

The generated code uses:
- `java.util.Map`, `java.util.List`, `java.util.Optional`
- `java.util.Collections` for immutable collections
- Java records for `EnumValue`, `FieldChange`, `EnumChange`

---

## Limitations

- **Diff availability**: Diffs are only generated for consecutive version pairs. `ctx.getDiffFrom()` returns `Optional.empty()` for non-adjacent versions.
- **Message fields**: Currently, `MessageInfo.getFields()` returns an empty map. Full field metadata is planned for a future release.
- **Nested types**: Nested enum and message metadata is not yet included in `SchemaInfo`.

---

## See Also

- [Configuration](CONFIGURATION.md) - All plugin options
- [Schema Diff Tool](SCHEMA_DIFF.md) - CLI tool for schema comparison
- [API Reference](API_REFERENCE.md) - Generated code reference
