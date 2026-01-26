# Release Notes - Proto Wrapper Plugin v2.3.0

**Release Date:** January 2026

## Overview

Version 2.3.0 introduces **Validation Annotations** support — automatic generation of Bean Validation (JSR-380) annotations on wrapper interfaces based on proto field metadata.

Key highlights:
- **@NotNull Auto-Detection** — automatically added to repeated/map fields and universal non-optional message fields
- **@Valid Auto-Detection** — automatically added to message-type fields for nested validation
- **Jakarta/Javax Support** — configurable validation namespace with auto-switch for Java 8
- **Smart Skip Logic** — primitives, type conflicts, oneof, and version-specific fields are correctly excluded

> **Note:** This release implements Phase 1 (auto-detection). Phase 2 (custom constraints via buf.validate) is planned for v2.7.0.

## New Features

### Validation Annotations

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
