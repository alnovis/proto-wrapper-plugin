# Release Notes — Proto Wrapper Maven Plugin v1.0.0

**Release Date:** December 15, 2025

## Overview

Proto Wrapper Maven Plugin is a code generation tool that creates version-agnostic Java wrapper classes from multiple protobuf schema versions. It enables you to write business logic that works across different protocol versions without version-specific code.

## Highlights

### Unified API Across Versions

Write code once, work with any version:

```java
// Works with v1, v2, or any future version
VersionContext ctx = VersionContext.forVersion(protocolVersion);
Order order = ctx.wrapOrder(protoMessage);

// Use the same API regardless of version
DateTime dateTime = order.getDateTime();
List<Order.Item> items = order.getItems();
```

### Smart Schema Merging

The plugin automatically:
- Merges fields from all versions into unified interfaces
- Detects equivalent enums (nested vs top-level)
- Resolves type conflicts between versions
- Tracks which fields exist in which versions

### Clean Generated Code

Generated code follows best practices:
- **Interfaces** — Define the version-agnostic contract
- **Abstract classes** — Template Method pattern for shared logic
- **Implementations** — Version-specific extraction logic
- **Enums** — Unified with Javadoc showing version availability

## Quick Start

### 1. Add plugin to pom.xml

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <basePackage>com.mycompany.model</basePackage>
        <protoRoot>${basedir}/proto</protoRoot>
        <protoPackagePattern>com.mycompany.proto.{version}</protoPackagePattern>
        <versions>
            <version><protoDir>v1</protoDir></version>
            <version><protoDir>v2</protoDir></version>
        </versions>
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

### 2. Run generation

```bash
mvn generate-sources
```

### 3. Use generated classes

```java
// Parse proto message
OrderProto.Order proto = OrderProto.Order.parseFrom(bytes);

// Wrap with version context
VersionContext ctx = VersionContext.forVersion(2);
Order order = ctx.wrapOrder(proto);

// Use version-agnostic API
System.out.println(order.getTotal());
System.out.println(order.getItems().size());
```

## Generated Code Structure

```
target/generated-sources/proto-wrapper/
├── com/mycompany/model/api/
│   ├── Order.java                 # Interface
│   ├── PaymentTypeEnum.java       # Enum
│   ├── VersionContext.java        # Factory
│   └── impl/
│       └── AbstractOrder.java     # Abstract class
├── com/mycompany/model/v1/
│   ├── OrderV1.java               # V1 implementation
│   └── VersionContextV1.java
└── com/mycompany/model/v2/
    ├── OrderV2.java               # V2 implementation
    └── VersionContextV2.java
```

## Key Features

| Feature | Description |
|---------|-------------|
| Multi-version support | Generate code for 2+ proto versions |
| Automatic merging | Unified API from different schemas |
| Type conflict handling | `int`→`long`, `enum`→`int` conversion |
| Equivalent enum detection | Nested ↔ top-level enum mapping |
| Nested message support | Full hierarchy support |
| Optional field handling | `hasXxx()` methods for presence check |
| Serialization | `toBytes()` for proto serialization |

## Requirements

- **Java:** 11 or higher
- **Maven:** 3.8 or higher
- **protoc:** Protocol Buffers compiler (must be in PATH)

## Known Limitations

- `oneof` fields are not yet supported
- `map` fields have basic support
- Very deep nesting (4+ levels) may need manual configuration

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation (EN)](README.md)
- [Documentation (RU)](README.ru.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
