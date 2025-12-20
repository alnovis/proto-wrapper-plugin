# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2025-12-20

### Added

#### New Configuration Options
- **`includeVersionSuffix` parameter** — Controls version suffix in generated class names
  - `true` (default): `MoneyV1`, `DateTimeV2` — backward compatible
  - `false`: `Money`, `DateTime` — version determined by package only
- **`PluginLogger` interface** — Unified logging abstraction with multiple backends:
  - `PluginLogger.maven(getLog())` — Maven plugin logging
  - `PluginLogger.console()` — Console output for standalone usage
  - `PluginLogger.noop()` — Silent mode for testing
  - `PluginLogger.fromConsumer()` — Custom consumer adapter

### Changed

#### Code Modernization (Java 17+)
- **Minimum Java version raised to 17** — Required for records and modern language features
- **Records for helper classes** — Replaced inner helper classes with Java 17 records:
  - `FieldWithVersion` — Holds field info with its version
  - `EnumValueWithVersion` — Holds enum value with its version
- **Stream API throughout codebase** — Replaced ~45 imperative loops with functional streams (~70% Stream usage)
- **Modern collection APIs** — `Set.of()` for immutable sets, `List.of()` for immutable lists
- **Functional patterns** — `reduce()`, `flatMap()`, `groupingBy()`, `Collectors.toMap()`
- **String utilities** — `String.join()` and `Arrays.copyOfRange()` instead of manual loops

#### Refactored Components
- **VersionMerger** — Complete Stream API refactoring with `flatMap`, `groupingBy`, and `Optional.stream()`
- **GenerationOrchestrator** — Added `ThrowingSupplier` functional interface, stream-based file generation
- **MergedMessage** — Stream-based recursive search and filtering methods
- **TypeResolver** — `reduce()` for nested class name building, cleaner type resolution
- **JavaTypeMapping** — `Set.of()` for primitive type checking

### Fixed
- Improved code readability and maintainability through functional style
- Reduced code duplication in merge and generation logic

---

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

- Java 17+ (as of v1.0.2)
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
