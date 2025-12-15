# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2025-12-15

### Added

#### CI/CD
- **GitHub Actions workflow** — Automated build and test pipeline
  - Multi-version Java testing (11, 17, 21)
  - Automatic protoc installation
  - Maven dependency caching
  - Build artifact upload (JAR files)
  - Triggered on push/PR to `master` and `develop` branches

### Changed
- Minor documentation improvements

---

## [1.0.0] - 2025-12-15

### Added

#### Core Features
- **Version-agnostic wrapper generation** — Generate Java wrapper classes that work across multiple protobuf schema versions
- **Automatic schema merging** — Merge multiple `.proto` file versions into a unified API
- **Template Method pattern** — Abstract base classes with `extract*` methods for clean separation of concerns

#### Code Generation
- **Interface generation** — Version-independent interfaces for all protobuf messages
- **Abstract class generation** — Base classes implementing common logic
- **Implementation class generation** — Version-specific implementations (e.g., `MoneyV1`, `MoneyV2`)
- **Enum generation** — Unified enums with version availability annotations in Javadoc
- **VersionContext generation** — Factory interface for creating version-specific wrappers

#### Smart Detection
- **Equivalent enum detection** — Automatically detects when a nested enum in one version equals a top-level enum in another
- **Type conflict resolution** — Handles `int` → `long` widening, `enum` → `int` conversion, and incompatible types
- **Nested message support** — Full support for deeply nested messages and enums

#### Maven Integration
- **Automatic protoc invocation** — No need to manually run protoc
- **Source filtering** — Process only proto files from specific directories
- **Configurable packages** — Flexible package naming with `{version}` placeholder
- **Generated sources integration** — Automatically adds output to compile source roots

#### Developer Experience
- **Javadoc annotations** — Generated code includes version availability info
- **`hasXxx()` methods** — Optional field presence checking
- **`toBytes()` method** — Serialize back to protobuf format
- **`getWrapperVersion()` method** — Get the protocol version number
- **Static `from()` factory methods** — Convenient wrapper creation

### Configuration Options

```xml
<configuration>
    <basePackage>com.example.model</basePackage>
    <protoRoot>${basedir}/proto</protoRoot>
    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>
    <versions>
        <version>
            <protoDir>v1</protoDir>
        </version>
        <version>
            <protoDir>v2</protoDir>
        </version>
    </versions>
</configuration>
```

### Requirements

- Java 11+
- Maven 3.8+
- protoc (Protocol Buffers compiler) in PATH

### Known Limitations

- `oneof` fields are not supported
- `map` fields have basic support only
- Complex nested message hierarchies may require manual configuration

---

## [Unreleased]

### Planned
- Support for `oneof` fields
- Improved `map` field handling
- Gradle plugin
- Code generation customization hooks
