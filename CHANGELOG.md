# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2025-12-22

### Added

#### Version Conversion (Phase 4)
- **`asVersion(Class<T>)` method** — Convert wrappers between protocol versions via serialization
  - `Money v2 = v1Money.asVersion(MoneyV2.class)` converts V1 to V2
  - Returns same instance if already target type (optimization)
  - Works with all message types
- **`parseXxxFromBytes(byte[])` methods** — New VersionContext methods for parsing bytes directly
  - `Money money = ctx.parseMoneyFromBytes(bytes)`
  - Used internally by `asVersion()` for cross-version conversion

#### Better INT_ENUM Error Messages (Phase 3)
- **`fromProtoValueOrThrow(int)` method** — Version-aware validation for INT_ENUM conflicts
  - Throws `IllegalArgumentException` with detailed message for invalid values
  - Message includes: invalid value, field name, and list of valid enum values
  - Only validates for versions that use enum type (int versions accept any value)
- Example: `"Invalid value 999 for TaxType. Valid values: [VAT(100), EXCISE(200), ..."`

#### Improved toString() (Phase 2)
- **Enhanced `toString()` output** — Better debugging with proto content
  - Format: `ClassName[version=N] field1: value1, field2: value2`
  - Shows wrapper class name, protocol version, and proto content
  - Uses proto's `TextFormat.shortDebugString()` for compact output

#### equals() and hashCode() (Phase 1)
- **Value-based equality** — Proper `equals()` and `hashCode()` implementations
  - Based on proto content and wrapper version
  - Works correctly in collections (List, Set, Map)
  - Consistent: equal objects have equal hash codes

### Changed
- **Major version bump** — Changed from 1.0.x to 1.1.0 to reflect completed improvement phases

---

## [1.0.4] - 2025-12-22

### Added

#### Builder Pattern Support
- **`generateBuilders` parameter** — Generate Builder pattern for modifying wrapper objects
  - `toBuilder()` method on all wrapper classes
  - `newBuilder()` static factory method
  - Fluent API: `setXxx()`, `clearXxx()`, `addXxx()`, `addAllXxx()`
  - Support for all field types: primitives, messages, enums, repeated fields

#### Protobuf Version Compatibility
- **`protobufMajorVersion` parameter** — Support for protobuf 2.x and 3.x APIs
  - `2`: Uses `EnumType.valueOf(int)` for enum conversion
  - `3` (default): Uses `EnumType.forNumber(int)` for enum conversion

#### Integration Tests
- **NestedPrimitiveMessageConflictTest** — Tests for nested message types in PRIMITIVE_MESSAGE conflicts
- Separate `proto-wrapper-integration-tests` module with comprehensive conflict type coverage

#### Documentation
- **KNOWN_ISSUES.md** — Detailed documentation of builder limitations
- **COOKBOOK.md** — Practical guide with conflict resolution examples
- **VERSION_AGNOSTIC_API.md** — Comprehensive API documentation
- Updated README with builder usage examples

### Fixed

#### Nested Types in PRIMITIVE_MESSAGE Conflicts
- **Fixed `getMessageTypeForField`** — Properly qualifies nested message types (e.g., `TicketRequest.ParentTicket` instead of `ParentTicket`)
- Uses `extractNestedTypePath()` and `ClassName.get()` with nested parts for correct type resolution

#### Java 11 Compatibility
- **Replaced `Stream.toList()`** — Changed to `collect(Collectors.toList())` for Java 11 support
- Affected files: `CodeGenerationHelper.java`, `RepeatedConflictHandler.java`

### Known Builder Limitations
- Type conflicts across versions (int→enum, primitive→message) not fully supported
- bytes fields require manual ByteString conversion
- See [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for details

---

## [1.0.3] - 2025-12-21

### Added

#### Multi-Module Architecture
- **Project split into separate modules** — Better separation of concerns and reusability:
  - `proto-wrapper-core` — Core library with generators, analyzers, and models
  - `proto-wrapper-maven-plugin` — Maven plugin depending on core
  - `examples/maven-example` — Example project with integration tests

#### Integration Tests (75 tests)
- **InterfaceGenerationTest** — Validates generated interfaces have correct methods
- **EnumGenerationTest** — Validates enum generation (top-level and nested)
- **FieldMappingTest** — Validates proto field mapping to wrapper classes
- **VersionContextTest** — Validates VersionContext factory methods
- **VersionEvolutionTest** — Validates V1/V2 field handling and version compatibility

#### Example Project
- **Comprehensive example** in `examples/maven-example/` demonstrating:
  - Multi-version proto support (v1, v2)
  - Top-level and nested enums
  - Deeply nested messages (`UserProfile.Preferences.DisplaySettings`)
  - Version-specific fields
  - VersionContext for runtime version selection
  - Working demo application

### Fixed

- **Enum generation bug** — Fixed Java 9+ `Stream.count()` optimization skipping intermediate `map()` operations in `GenerationOrchestrator.generateEnums()`. Changed to `forEach()` with manual counter.
- **Package name truncation** — Fixed `TypeResolver.extractProtoPackage()` incorrectly truncating package names (e.g., `com.example.proto.v1` → `example.proto.v1`), causing nested type resolution failures.

### Changed

- **MavenLogger extracted** — Logging adapter moved from inline to separate `MavenLogger` class in maven-plugin module
- **PluginLogger simplified** — Core library uses simple `PluginLogger` interface without Maven dependencies

---

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
