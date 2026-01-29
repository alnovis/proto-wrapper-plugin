# Release Notes - Proto Wrapper Plugin v2.3.1

**Release Date:** January 29, 2026

## Overview

Version 2.3.1 introduces **Schema Metadata** — runtime access to enum values, message metadata, and version diffs without hardcoding.

Key highlights:
- **Runtime Schema Introspection** — access enum values and message info at runtime via `SchemaInfo`
- **Version Diff API** — programmatically query what changed between versions via `VersionSchemaDiff`
- **Dynamic Validation** — validate enum values against actual schema without magic numbers
- **Migration Tooling** — understand field changes, type changes, and get migration hints

> **Note:** This builds on v2.3.0 which added Validation Annotations support.

## Bug Fixes

- **Gradle plugin now generates SchemaInfo and SchemaDiff classes** — previously the Gradle plugin accepted `generateSchemaMetadata=true` configuration but did not actually generate the metadata files (`SchemaInfoVx.java`, `SchemaDiffVxToVy.java`). Now both Maven and Gradle plugins generate metadata classes identically.

## New Features

### Schema Metadata

Generate runtime metadata classes for schema introspection:

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

### Usage Examples

#### Access Enum Values at Runtime

```java
VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V2);
SchemaInfo schema = ctx.getSchemaInfo();

// Get enum values without hardcoding
schema.getEnum("TaxTypeEnum").ifPresent(enumInfo -> {
    for (SchemaInfo.EnumValue value : enumInfo.getValues()) {
        System.out.println(value.name() + " = " + value.number());
    }
});
// Output: VAT = 100, EXCISE = 200, NO_TAX = 0
```

#### Query Version Differences

```java
VersionContext v2Ctx = VersionContext.forVersionId(ProtocolVersions.V2);

v2Ctx.getDiffFrom(ProtocolVersions.V1).ifPresent(diff -> {
    // Find what changed for a specific field
    diff.findFieldChange("Tax", "type").ifPresent(fc -> {
        System.out.println("Change: " + fc.changeType());
        System.out.println("Old type: " + fc.oldType());
        System.out.println("New type: " + fc.newType());
        System.out.println("Hint: " + fc.migrationHint());
    });

    // Get all removed fields
    for (var fc : diff.getRemovedFields()) {
        System.out.println("Removed: " + fc.messageName() + "." + fc.fieldName());
    }
});
```

#### Dynamic Enum Validation

```java
public void validateEnumValue(int rawValue, String enumName, String versionId) {
    VersionContext ctx = VersionContext.forVersionId(versionId);
    SchemaInfo schema = ctx.getSchemaInfo();

    schema.getEnum(enumName).ifPresent(enumInfo -> {
        boolean isValid = enumInfo.getValues().stream()
                .anyMatch(v -> v.number() == rawValue);

        if (!isValid) {
            String validValues = enumInfo.getValues().stream()
                    .map(v -> v.name() + "(" + v.number() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                "Invalid " + enumName + " value " + rawValue +
                ". Valid values: " + validValues);
        }
    });
}
```

### Generated Code Structure

```
com.example.model/
├── api/
│   └── VersionContext.java     # + getSchemaInfo(), getDiffFrom()
├── metadata/                   # NEW package
│   ├── SchemaInfoV1.java       # Enum/message metadata for V1
│   ├── SchemaInfoV2.java       # Enum/message metadata for V2
│   └── SchemaDiffV1ToV2.java   # Changes from V1 to V2
├── v1/
│   └── VersionContextV1.java   # Implements metadata methods
└── v2/
    └── VersionContextV2.java   # Implements metadata methods
```

### API Summary

| Interface | Description |
|-----------|-------------|
| `SchemaInfo` | Access enum values and message metadata |
| `SchemaInfo.EnumInfo` | Enum name, full name, and values |
| `SchemaInfo.EnumValue` | Enum value name and number |
| `VersionSchemaDiff` | Schema changes between versions |
| `VersionSchemaDiff.FieldChange` | Field addition, removal, type change, etc. |
| `VersionSchemaDiff.EnumChange` | Enum value additions and removals |

---

## Previous Release: v2.3.0 - Validation Annotations

Version 2.3.0 introduced **Validation Annotations** — automatic generation of Bean Validation (JSR-380) annotations.

### Configuration

Generate Bean Validation annotations on wrapper interface getters based on proto field analysis:

**Maven:**
```xml
<configuration>
    <generateValidationAnnotations>true</generateValidationAnnotations>
    <validationAnnotationStyle>jakarta</validationAnnotationStyle>
</configuration>
```

**Gradle:**
```kotlin
protoWrapper {
    generateValidationAnnotations.set(true)
    validationAnnotationStyle.set("jakarta")
}
```

### Generated Code Example

Given this proto:
```protobuf
message Order {
    string order_id = 1;
    repeated OrderItem items = 2;
    Customer customer = 3;
    int64 total_amount = 4;
}
```

Generated interface:
```java
public interface Order {
    String getOrderId();  // No @NotNull - optional scalar

    @NotNull  // Repeated field - always returns List, never null
    List<@Valid OrderItem> getItems();  // @Valid for nested message validation

    @Valid
    @NotNull  // Message field present in all versions
    Customer getCustomer();

    long getTotalAmount();  // No @NotNull - primitive type
}
```

### Auto-Detection Rules

#### @NotNull Applied To:
| Field Type | Condition |
|------------|-----------|
| Repeated fields | Always (List is never null) |
| Map fields | Always (Map is never null) |
| Message fields | When present in all versions and non-optional |

#### @NotNull Skipped For:
| Field Type | Reason |
|------------|--------|
| Primitive types | `int`, `long`, `boolean`, etc. cannot be null |
| Type conflicts | Field has different types across versions |
| Oneof fields | Only one field in oneof can be set |
| Version-specific | Field not present in all schema versions |

#### @Valid Applied To:
| Field Type | Condition |
|------------|-----------|
| Message fields | Always (enables nested validation) |
| Repeated message | On list element type: `List<@Valid Item>` |

### Namespace Configuration

| Style | Package | Use Case |
|-------|---------|----------|
| `jakarta` | `jakarta.validation.constraints` | Java 9+, Spring Boot 3.x |
| `javax` | `javax.validation.constraints` | Java 8, Spring Boot 2.x |

> **Auto-Switch:** When `targetJavaVersion` is set to `8`, the plugin automatically uses `javax` namespace regardless of the configured style.

## Configuration Reference

### New Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `generateValidationAnnotations` | `false` | Enable validation annotation generation |
| `validationAnnotationStyle` | `jakarta` | Namespace: `jakarta` or `javax` |

### Maven Configuration

```xml
<plugin>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>2.3.0</version>
    <configuration>
        <basePackage>com.example.model</basePackage>
        <protoRoot>${basedir}/proto</protoRoot>
        <generateValidationAnnotations>true</generateValidationAnnotations>
        <validationAnnotationStyle>jakarta</validationAnnotationStyle>
        <versions>
            <version><protoDir>v1</protoDir></version>
            <version><protoDir>v2</protoDir></version>
        </versions>
    </configuration>
</plugin>
```

### Gradle Configuration

```kotlin
protoWrapper {
    basePackage.set("com.example.model")
    protoRoot.set(file("proto"))
    generateValidationAnnotations.set(true)
    validationAnnotationStyle.set("jakarta")
    versions {
        version("v1")
        version("v2")
    }
}
```

## Dependencies

To use validation annotations, add the validation API to your project:

**Jakarta (Java 9+, Spring Boot 3.x):**
```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>
```

**Javax (Java 8, Spring Boot 2.x):**
```xml
<dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>2.0.1.Final</version>
</dependency>
```

For runtime validation, add a validator implementation (e.g., Hibernate Validator).

## Migration

### From 2.2.x

1. Update plugin version to `2.3.0`
2. (Optional) Enable validation annotations:
   ```xml
   <generateValidationAnnotations>true</generateValidationAnnotations>
   ```
3. Add validation API dependency if using annotations

### No Breaking Changes

This release is fully backward compatible. Validation annotations are opt-in and disabled by default.

## Implementation Details

New classes added to `proto-wrapper-core`:

| Class | Description |
|-------|-------------|
| `FieldConstraints` | Record model for validation constraint metadata |
| `ValidationConstraintResolver` | Resolves constraints from MergedField based on auto-detection rules |
| `ValidationAnnotationGenerator` | Converts FieldConstraints to JavaPoet AnnotationSpec |

## Roadmap: Phase 2

Phase 2 (planned for v2.7.0) will add support for custom constraints via buf.validate:

```protobuf
import "buf/validate/validate.proto";

message User {
    string email = 1 [(buf.validate.field).string.email = true];
    int32 age = 2 [(buf.validate.field).int32 = {gte: 0, lte: 150}];
}
```

Generated:
```java
public interface User {
    @Email
    String getEmail();

    @Min(0) @Max(150)
    int getAge();
}
```

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete list of changes.

---

## Previous Releases

- [v2.2.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.2.0) - Per-version proto syntax, field mappings
- [v2.1.1](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.1) - defaultVersion configuration
- [v2.1.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.1.0) - ProtocolVersions class generation, parallel generation
- [v2.0.0](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v2.0.0) - Namespace migration to io.alnovis, removed deprecated APIs
- [v1.6.9](https://github.com/alnovis/proto-wrapper-plugin/releases/tag/v1.6.9) - String-based version API
