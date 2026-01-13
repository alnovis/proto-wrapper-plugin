# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

_No changes yet._

---

## [1.6.4] - 2026-01-13

### Fixed

#### Proto3 Singular Scalar Fields
- **Fixed proto3 singular scalars returning `null` instead of value** - Proto3 singular scalar fields (int32, bool, float, etc.) without the `optional` keyword were incorrectly returning `null` even when values were set.
  - Root cause: `needsHasCheck()` returned `true` based only on `optional && primitive`, ignoring that proto3 singular scalars don't have `has*()` methods
  - Fix: Added `allVersionsSupportHas` field to `MergedField` that checks if ALL versions support `has*()` method
  - Now `needsHasCheck()` returns `false` for proto3 singular scalars, so getter returns value directly

#### Message Fields Returning Default Instance
- **Fixed message fields returning default instance instead of `null`** - Unset message fields returned a wrapped default instance, which was inconsistent with `has*()` returning `false`.
  - Root cause: `needsHasCheck()` required `primitive` to be `true`, but message fields have `primitive = false`
  - Fix: Extended `needsHasCheck()` to also return `true` for message fields when `allVersionsSupportHas` is `true`
  - Now unset message fields return `null`, consistent with `has*()` returning `false`

#### Proto2 `has*()` Method Support
- **Fixed `supportsHasMethod()` for proto2 `required` fields** - Previously, the plugin only recognized `optional` fields as having `has*()` methods in proto2 syntax. This caused proto2 `required` fields to be incorrectly handled, resulting in `null` returns from getter methods when the underlying proto field was properly set.
  - Root cause: `determineHasMethodSupport()` checked `proto.getLabel() == LABEL_OPTIONAL` instead of `proto.getLabel() != LABEL_REPEATED`
  - In proto2, both `optional` and `required` fields have `has*()` methods; only `repeated` fields do not

#### Repeated Field Handling
- **Fixed `supportsHasMethod()` for repeated message fields** - Repeated fields should never have `has*()` methods, regardless of field type (scalar, message, enum). The check for `LABEL_REPEATED` is now performed first, before any type-specific logic.

### Added

#### Golden Tests Module
- **New `proto-wrapper-golden-tests` module** - Comprehensive test suite with 110 tests covering:
  - Proto2: required fields, optional fields, repeated fields
  - Proto3: singular fields, optional fields, repeated fields, oneof fields
  - Cross-version consistency tests
  - All scalar types, message types, enum types

#### Comprehensive Test Coverage
- Added 27 new tests in `FieldInfoTest` covering all combinations of:
  - Proto syntax: proto2, proto3
  - Field labels: `LABEL_OPTIONAL`, `LABEL_REQUIRED`, `LABEL_REPEATED`
  - Field types: scalar (int32, int64, bool, double, etc.), string, bytes, message, enum
  - Special cases: oneof fields, proto3 optional scalar fields

### Changed

- **`FieldInfo.determineHasMethodSupport()`** - Refactored to correct order of checks:
  1. Repeated fields (`LABEL_REPEATED`) - always return `false`
  2. Message types - always return `true` (singular messages always have `has*()`)
  3. Oneof fields - always return `true`
  4. Proto2 non-repeated fields - return `true` (both optional and required)
  5. Proto3 scalar fields - return `false` (unless optional modifier or message type)

---

## [1.6.3] - 2026-01-06

### Changed

#### Documentation Refactoring
- **README.md refactored** - Reduced from 1118 to ~250 lines with improved navigation
  - Added Mermaid diagrams for Problem/Solution sections
  - Focused on Quick Start and feature overview
  - Better documentation hierarchy with links to detailed guides

#### New Documentation Files
- **[GETTING_STARTED.md](docs/GETTING_STARTED.md)** - Step-by-step tutorial for beginners (~15 min)
- **[CONFIGURATION.md](docs/CONFIGURATION.md)** - Complete Maven/Gradle configuration reference
- **[SCHEMA_DIFF.md](docs/SCHEMA_DIFF.md)** - Schema comparison tool documentation (extracted from README)
- **[INCREMENTAL_BUILD.md](docs/INCREMENTAL_BUILD.md)** - Build optimization guide (extracted from README)

#### Architecture Documentation
- **ARCHITECTURE.md** - Added high-level pipeline diagram, fixed Component Diagram

#### CI/CD Improvements
- Added Google Maven mirror to avoid rate limiting on GitHub Actions
- Increased retry attempts and wait times for Gradle builds
- Added dependency prefetch step

### Deprecated
- **VERSION_AGNOSTIC_API.md** - Moved to docs/archive/ (content merged into COOKBOOK.md)

---

## [1.6.2] - 2026-01-05

### Added

#### PRIMITIVE_MESSAGE Builder Support
- **Dual setters for PRIMITIVE_MESSAGE conflicts** - Full builder support for fields that change type from primitive to message across protocol versions
  - `setXxx(primitive)` - for versions where field is primitive type (int, long, String, etc.)
  - `setXxxMessage(Message)` - for versions where field is message type
  - Runtime validation throws `UnsupportedOperationException` if wrong setter called for version
  - Mirrors existing dual getter pattern (`getXxx()` / `getXxxMessage()`)

#### Example Usage
```java
// V1 (primitive version) - use primitive setter
Order v1 = Order.newBuilder(ctxV1)
    .setTotal(1000L)           // Works - v1 uses int64
    .build();

// V2 (message version) - use message setter
Order v2 = Order.newBuilder(ctxV2)
    .setTotalMessage(Money.newBuilder(ctxV2)
        .setAmount(1000L)
        .setCurrency("USD")
        .build())
    .build();

// Wrong setter throws at runtime
Order.newBuilder(ctxV1)
    .setTotalMessage(money);   // throws UnsupportedOperationException
```

#### New Integration Tests
- `PrimitiveMessageBuilderTest` with 16 tests covering:
  - V1 (primitive) builder operations
  - V2 (message) builder operations
  - Runtime validation (wrong setter throws)
  - Round-trip serialization
  - toBuilder() modifications

### Changed

- `MergedField.ConflictType.shouldSkipBuilderSetter()` - Now returns false for PRIMITIVE_MESSAGE
- `BuilderInterfaceGenerator` - Added handler for PRIMITIVE_MESSAGE conflict type
- `PrimitiveMessageHandler` - Implemented builder methods (previously empty)
- Added helper methods to `MergedField`:
  - `getDoSetMessageMethodName()` - for message value setter
  - `getSupportsPrimitiveMethodName()` - for primitive type check
  - `getSupportsMessageMethodName()` - for message type check
  - `isPrimitiveInVersion(String)` - check if field is primitive in version
  - `isMessageInVersion(String)` - check if field is message in version

---

## [1.6.1] - 2026-01-05

### Fixed

#### CI/CD Stability
- Exclude benchmark tests from regular Gradle test task to prevent CI failures
  - Benchmark tests are sensitive to VM performance and fail on GitHub Actions runners
  - Added separate `benchmarkTest` task for manual execution
  - Mirrors Maven's surefire configuration that already excludes `@Tag("benchmark")`

### Added

#### Gradle Plugin Tests
- Added `ProtoWrapperPluginTest` with 23 unit tests
  - Plugin application, extension creation, task registration
  - Default values verification for incremental settings
  - Java plugin integration tests
- Added `GenerateWrappersTaskTest` with 25 functional tests
  - Basic generation, incremental caching, cache corruption recovery
  - forceRegenerate behavior, custom cache directory

#### Maven Integration Tests
- Added `IncrementalGenerationIntegrationTest` with 7 tests
  - Full generation on first run
  - Skip generation when no changes
  - forceRegenerate bypasses cache
  - Config change triggers regeneration
  - Corrupted cache recovery

### Changed

- Version bump to 1.6.1 across all modules

---

## [1.6.0] - 2026-01-05

### Added

#### Incremental Generation
- **Smart change detection** - Only regenerate wrapper classes when source proto files change, significantly reducing build times for large projects
  - >50% build time reduction when no changes detected
  - Correct regeneration on any proto file change
  - Support for dependencies between proto files (imports)

#### New Configuration Options
| Option | Default | Description |
|--------|---------|-------------|
| `incremental` | `true` | Enable incremental generation |
| `cacheDirectory` | `${build}/proto-wrapper-cache` | Directory for incremental state cache |
| `forceRegenerate` | `false` | Force full regeneration, ignoring cache |

#### Maven Plugin
```xml
<configuration>
    <basePackage>com.example.model</basePackage>
    <!-- Incremental generation (enabled by default) -->
    <incremental>true</incremental>
    <cacheDirectory>${project.build.directory}/proto-wrapper-cache</cacheDirectory>
    <!-- Force full regeneration if needed -->
    <!-- <forceRegenerate>true</forceRegenerate> -->
</configuration>
```

```bash
# Normal build (incremental)
mvn compile

# Force full regeneration
mvn compile -Dproto-wrapper.force=true

# Disable incremental
mvn compile -Dproto-wrapper.incremental=false
```

#### Gradle Plugin
```kotlin
protoWrapper {
    basePackage.set("com.example.model")
    // Incremental generation (enabled by default)
    incremental.set(true)
    cacheDirectory.set(layout.buildDirectory.dir("proto-wrapper-cache"))
    // Force full regeneration if needed
    // forceRegenerate.set(true)
}
```

```bash
# Normal build (incremental)
./gradlew generateProtoWrapper

# Force full regeneration
./gradlew generateProtoWrapper -Pproto-wrapper.force=true

# Clean and regenerate
./gradlew clean generateProtoWrapper
```

#### Cache File Format
State is persisted to JSON in cache directory (`state.json`):
```json
{
  "pluginVersion": "1.6.0",
  "configHash": "a1b2c3d4e5f67890",
  "lastGeneration": "2026-01-05T10:30:00.000Z",
  "protoFingerprints": {
    "v1/order.proto": {
      "relativePath": "v1/order.proto",
      "contentHash": "sha256:abc123...",
      "lastModified": 1707990000000,
      "fileSize": 2048
    }
  },
  "protoDependencies": {
    "v1/order.proto": ["v1/common.proto"]
  },
  "generatedFiles": {
    "com/example/model/api/Order.java": {
      "contentHash": "sha256:ghi789...",
      "lastModified": 1707990500000,
      "sourceProtos": ["v1/order.proto", "v2/order.proto"]
    }
  }
}
```

#### Cache Invalidation Rules
| Condition | Action | Reason |
|-----------|--------|--------|
| Plugin version changed | Full regeneration | Generated code format may differ |
| Config hash changed | Full regeneration | Output structure may differ |
| Proto file modified | Regenerate affected | Content changed |
| Proto file added | Regenerate + dependents | New messages |
| Proto file deleted | Full regeneration | May break dependencies |
| Imported file changed | Regenerate dependents | Transitive change |
| `clean` executed | Full regeneration | Cache deleted |
| `--force` flag | Full regeneration | User requested |
| Cache corrupted | Full regeneration | Recovery |

#### Thread-Safe Concurrent Builds
- **File locking** - Uses Java's `FileLock` API to prevent concurrent builds from corrupting cache
- **Automatic lock acquisition** - Lock acquired before reading/writing state file
- **Configurable timeout** - Default 30-second timeout for lock acquisition
- **Graceful handling** - If lock cannot be acquired, build fails with clear error message

#### New Core Classes
```
proto-wrapper-core/src/main/java/space/alnovis/protowrapper/incremental/
├── FileFingerprint.java          # File hash + timestamp for change detection
├── IncrementalState.java         # Persistent state model (record)
├── IncrementalStateManager.java  # Cache state management
├── ProtoDependencyGraph.java     # Proto import dependency graph
├── ChangeDetector.java           # Change detection logic
├── GeneratedFileInfo.java        # Generated file tracking
├── CacheLock.java                # File locking for thread safety
└── package-info.java
```

#### PluginVersion Utility
- **Runtime version access** - `PluginVersion.get()` returns current plugin version
- **Maven/Gradle compatible** - Uses resource filtering for version injection
- Used for cache invalidation on plugin updates

### Changed

#### Documentation Improvements
- **Mermaid diagrams** - Converted ASCII diagrams in README.md to Mermaid format for better rendering on GitHub:
  - Architecture diagram showing the complete code generation pipeline
  - Conflict Handling Architecture diagram showing handler hierarchy

#### Internal
- Version bump to 1.6.0 across all modules
- `GeneratorConfig` - Added incremental generation fields and `computeConfigHash()` method
- `GenerationOrchestrator` - Integrated incremental generation with automatic fallback to full generation

---

## [1.5.2] - 2026-01-04

### Changed

#### Unified Conflict Detection Architecture
- **Single source of truth for type conflicts** - DiffTool now uses the same `VersionMerger` infrastructure as the code generator, ensuring consistent conflict classification across the plugin.

### Added

#### MergedField.ConflictType Enhancements
- **Handling enum** - New nested enum indicating how the plugin handles each conflict type:
  - `NATIVE` - No special handling needed
  - `CONVERTED` - Plugin automatically converts between types
  - `MANUAL` - Conversion requires manual code
  - `WARNING` - Works but may have issues (data loss)
  - `INCOMPATIBLE` - Types are fundamentally incompatible

- **Severity enum** - New nested enum for breaking change detection:
  - `INFO` - Plugin handles automatically
  - `WARNING` - May require attention
  - `ERROR` - Breaking change that plugin cannot handle

- **New methods on ConflictType**:
  - `getHandling()` - Returns the handling strategy
  - `getPluginNote()` - Returns human-readable description
  - `isPluginHandled()` - Returns true if NATIVE or CONVERTED
  - `isBreaking()` - Returns true if INCOMPATIBLE
  - `isWarning()` - Returns true if WARNING or MANUAL
  - `getSeverity()` - Returns INFO/WARNING/ERROR based on handling

#### MergedSchemaDiffAdapter
- **New adapter class** - Converts `MergedSchema` to `SchemaDiff` using the unified conflict detection
- Enables consistent breaking change classification between code generation and diff reports

#### SchemaDiff API
- **`compareViaMerger()` methods** - Explicitly use VersionMerger infrastructure
- **`compare()` now uses unified infrastructure** - Default comparison uses the same logic as code generator

### Deprecated

- **`SchemaDiff.compareLegacy()`** - Use `compare()` instead (will be removed in v2.0)
- **`TypeConflictType`** - Use `MergedField.ConflictType` instead (will be removed in v2.0)
- **`SchemaDiffEngine`** - Replaced by `VersionMerger` + `MergedSchemaDiffAdapter` (will be removed in v2.0)
- **`BreakingChangeDetector`** - Logic moved to `MergedSchemaDiffAdapter` (will be removed in v2.0)

### Technical Details

The refactoring eliminates ~180 lines of duplicated code and ensures that:
1. Both DiffTool and code generator use the same conflict detection logic
2. Changes to conflict handling only need to be made in one place
3. Breaking change severity is consistent across all plugin features

---

## [1.5.1] - 2026-01-04

### Fixed

#### Schema Diff Tool - Breaking Change Classification
- **PRIMITIVE_MESSAGE conflicts now correctly classified as INFO** - The diff tool was incorrectly classifying primitive-to-message type conflicts (e.g., `uint32` → `ParentTicket`) as ERROR (incompatible). The plugin actually handles these conflicts by generating dual accessors:
  - `getXxx()` - Returns primitive value (works in primitive versions, returns null in message versions)
  - `getXxxMessage()` - Returns message wrapper (works in message versions, returns null in primitive versions)
  - `supportsXxx()` - Returns true for primitive versions
  - `supportsXxxMessage()` - Returns true for message versions

### Changed

- **TypeConflictType.PRIMITIVE_MESSAGE** - Changed from `Handling.INCOMPATIBLE` to `Handling.CONVERTED`
- **Breaking change summary** - Now correctly reports 0 errors for schemas that the plugin can fully handle

### Example

Before fix:
```
Changes: 1 error, 1 warning, 32 plugin-handled
[ERROR] FIELD_TYPE_INCOMPATIBLE: TicketRequest.shiftDocumentNumber (uint32 -> ParentTicket)
```

After fix:
```
Changes: 0 errors, 1 warning, 33 plugin-handled
[INFO] FIELD_TYPE_CONVERTED: TicketRequest.shiftDocumentNumber (uint32 -> ParentTicket)
       PRIMITIVE_MESSAGE: Plugin generates getXxx() and getXxxMessage() accessors
```

---

## [1.5.0] - 2026-01-04

### Added

#### Schema Diff Tool
- **Schema comparison tool** - Compare protobuf schemas between versions and detect changes
  - Detect added, modified, and removed messages/enums/fields
  - Identify breaking changes that could affect wire compatibility
  - Multiple output formats: text, JSON, Markdown
  - CI/CD integration with exit codes

#### CLI Tool
- **Standalone CLI** - Compare schemas from command line without Maven/Gradle
  - Packaged as executable JAR (`proto-wrapper-core-1.5.0-cli.jar`)
  - Uses picocli for argument parsing
  - Built with maven-shade-plugin (Maven) and shadow plugin (Gradle)

```bash
# Basic comparison
java -jar proto-wrapper-core-1.5.0-cli.jar diff proto/v1 proto/v2

# Output formats
java -jar proto-wrapper-core-1.5.0-cli.jar diff proto/v1 proto/v2 --format=json
java -jar proto-wrapper-core-1.5.0-cli.jar diff proto/v1 proto/v2 --format=markdown

# CI/CD mode
java -jar proto-wrapper-core-1.5.0-cli.jar diff proto/v1 proto/v2 --fail-on-breaking
```

#### CLI Options
| Option | Description |
|--------|-------------|
| `--v1-name=<name>` | Name for source version (default: v1) |
| `--v2-name=<name>` | Name for target version (default: v2) |
| `-f, --format=<fmt>` | Output format: text, json, markdown |
| `-o, --output=<file>` | Write output to file |
| `-b, --breaking-only` | Show only breaking changes |
| `--fail-on-breaking` | Exit code 1 on breaking changes |
| `--fail-on-warning` | Treat warnings as errors |
| `--protoc=<path>` | Path to protoc executable |
| `-q, --quiet` | Suppress informational messages |

#### Maven Goal
- **`proto-wrapper:diff` goal** - Compare schemas in Maven builds
  - Standalone goal (no project required): `mvn proto-wrapper:diff -Dv1=... -Dv2=...`
  - Can be bound to lifecycle phases for automated checks
  - Full parameter support via command line or pom.xml

```bash
# Command line usage
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -DfailOnBreaking=true

# pom.xml configuration
<execution>
    <id>check-breaking</id>
    <phase>verify</phase>
    <goals><goal>diff</goal></goals>
    <configuration>
        <v1>${basedir}/proto/production</v1>
        <v2>${basedir}/proto/development</v2>
        <failOnBreaking>true</failOnBreaking>
    </configuration>
</execution>
```

#### Gradle Task
- **`SchemaDiffTask`** - Compare schemas in Gradle builds
  - Register custom diff tasks with full configuration
  - Support for all output formats and CI/CD options

```kotlin
tasks.register<space.alnovis.protowrapper.gradle.SchemaDiffTask>("diffSchemas") {
    v1Directory.set(file("proto/v1"))
    v2Directory.set(file("proto/v2"))
    outputFormat.set("markdown")
    outputFile.set(file("build/reports/schema-diff.md"))
    failOnBreaking.set(true)
}
```

#### Core Diff Engine
- **`SchemaDiff`** - Main facade for schema comparison
  - `compare(Path v1Dir, Path v2Dir)` - Compare directories
  - `compare(VersionSchema v1, VersionSchema v2)` - Compare analyzed schemas
  - `toText()`, `toJson()`, `toMarkdown()` - Format output
  - `hasBreakingChanges()`, `getBreakingChanges()` - Query breaking changes
  - `getSummary()` - Get diff statistics

- **`SchemaDiffEngine`** - Core comparison logic
  - Compares messages, enums, fields, nested types
  - Tracks added, removed, and modified entities
  - Detects field type changes, number changes, label changes

- **`BreakingChangeDetector`** - Breaking change identification
  - Categorizes changes by severity (ERROR, WARNING, INFO)
  - Detects 11 types of breaking changes

#### Breaking Change Types
| Type | Severity | Description |
|------|----------|-------------|
| `MESSAGE_REMOVED` | ERROR | Message was removed |
| `FIELD_REMOVED` | ERROR | Field was removed |
| `FIELD_NUMBER_CHANGED` | ERROR | Field number changed |
| `FIELD_TYPE_INCOMPATIBLE` | ERROR | Incompatible type change |
| `ENUM_REMOVED` | ERROR | Enum was removed |
| `ENUM_VALUE_REMOVED` | ERROR | Enum value removed |
| `ENUM_VALUE_NUMBER_CHANGED` | ERROR | Enum value number changed |
| `REQUIRED_FIELD_ADDED` | WARNING | Required field added |
| `LABEL_CHANGED_TO_REQUIRED` | WARNING | Field changed to required |
| `CARDINALITY_CHANGED` | WARNING | Cardinality changed |
| `ONEOF_FIELD_MOVED` | WARNING | Field moved in/out of oneof |

#### Diff Formatters
- **`TextDiffFormatter`** - Plain text with ASCII formatting
- **`JsonDiffFormatter`** - Structured JSON output
- **`MarkdownDiffFormatter`** - Markdown with tables for reports

#### Data Models
- `MessageDiff` - Message-level changes with field details
- `FieldChange` - Individual field changes with type info
- `EnumDiff` - Enum changes with value tracking
- `EnumValueChange` - Enum value additions/removals
- `BreakingChange` - Breaking change with severity and context

#### New Dependencies
- **picocli 4.7.5** - CLI argument parsing framework
- **maven-shade-plugin 3.5.1** - Executable JAR packaging (Maven)
- **shadow plugin 8.1.1** - Executable JAR packaging (Gradle)

### Changed

- **proto-wrapper-core** - Added CLI entry point and executable JAR packaging
- **Documentation** - Added comprehensive Schema Diff Tool section to README.md

### Build Infrastructure

- **Gradle 9.x compatibility** - Updated for latest Gradle versions
  - Kotlin upgraded from 1.9.22 to 2.1.20
  - Changed `kotlinOptions` to `compilerOptions` with `JvmTarget.JVM_17`
  - Added JUnit Platform Launcher dependency

- **Test optimization** - Separated slow TestKit tests
  - Added `@Tag("slow")` for TestKit-based tests
  - New `slowTest` task with parallel execution
  - Regular `test` task excludes slow tests (~3 min → ~45 sec build time)

- **CI workflow updates**
  - Added explicit Gradle version (8.5) via `gradle/actions/setup-gradle@v4`
  - Added `mavenLocal()` to Gradle init.gradle
  - Added `slowTest` step for comprehensive CI testing

---

## [1.4.0] - 2026-01-03

### Added

#### Repeated Conflict Field Builders
- **Builder methods for repeated conflict fields** - Full builder support for repeated fields with type conflicts
  - `addXxx(element)` - Add single element
  - `addAllXxx(List)` - Add multiple elements
  - `setXxx(List)` - Replace all elements
  - `clearXxx()` - Clear all elements

#### Supported Conflict Types for Repeated Fields
| Conflict | Example | Unified Type | Range Validation |
|----------|---------|--------------|------------------|
| **WIDENING** | `repeated int32` vs `int64` | `List<Long>` | Yes (int range check) |
| **FLOAT_DOUBLE** | `repeated float` vs `double` | `List<Double>` | Yes (float range check) |
| **SIGNED_UNSIGNED** | `repeated int32` vs `uint32` | `List<Long>` | Yes (signed/unsigned bounds) |
| **INT_ENUM** | `repeated int32` vs `SomeEnum` | `List<Integer>` | No |
| **STRING_BYTES** | `repeated string` vs `bytes` | `List<String>` | No (UTF-8 conversion) |

#### Range Validation
- **Runtime validation for narrowing conversions** - Clear error messages when values exceed target type range
  - WIDENING: `"Value 9999999999 exceeds int32 range for v1"`
  - FLOAT_DOUBLE: `"Value 1.0E309 exceeds float range for v1"`
  - SIGNED_UNSIGNED: `"Value -1 exceeds uint32 range for v2"`

#### New Handler Methods
- `RepeatedConflictHandler.addAbstractBuilderMethods()` - Generate abstract doXxx methods
- `RepeatedConflictHandler.addBuilderImplMethods()` - Generate version-specific implementations
- `RepeatedConflictHandler.addConcreteBuilderMethods()` - Generate public wrapper methods

#### New Tests
- `RepeatedConflictBuilderTest` - 35 comprehensive tests covering all conflict types:
  - WIDENING: 10 tests with boundary values and range validation
  - FLOAT_DOUBLE: 7 tests including special values (NaN, Infinity)
  - INT_ENUM: 3 tests for int/enum conversion
  - STRING_BYTES: 4 tests including Unicode support
  - SIGNED_UNSIGNED: 8 tests for int32/uint32, int64/uint64, sint32/int32
  - Mixed operations: 3 tests for combining multiple fields

### Changed
- `BuilderInterfaceGenerator` - Added `addRepeatedConflictBuilderMethods()` for generating interface methods
- `FieldProcessingChain` - Integrated `RepeatedConflictHandler` for builder generation
- `AbstractConflictHandler` - Added template methods for repeated builder patterns

### Example Usage

```java
// Proto definition:
// v1: repeated int32 numbers = 1;
// v2: repeated int64 numbers = 1;

// Generated interface:
interface RepeatedConflicts {
    List<Long> getNumbers();

    interface Builder {
        Builder addNumbers(long value);
        Builder addAllNumbers(List<Long> values);
        Builder setNumbers(List<Long> values);
        Builder clearNumbers();
    }
}

// Usage with V1 (range validation):
RepeatedConflicts result = wrapper.toBuilder()
    .addNumbers(100L)        // OK
    .addNumbers(200L)        // OK
    .addNumbers(9_999_999_999L)  // throws IllegalArgumentException
    .build();

// Usage with V2 (accepts full long range):
RepeatedConflicts result = wrapper.toBuilder()
    .addNumbers(9_999_999_999L)  // OK - V2 supports int64
    .build();
```

### Migration Notes

- No breaking changes
- New feature is backward compatible
- Previously skipped repeated conflict fields now have full builder support

---

## [1.3.0] - 2026-01-02

### Added

#### Google Well-Known Types Support
- **Automatic type conversion** — Convert Google Well-Known Types to idiomatic Java types
  - `google.protobuf.Timestamp` → `java.time.Instant`
  - `google.protobuf.Duration` → `java.time.Duration`
  - `google.protobuf.StringValue` → `String` (nullable)
  - `google.protobuf.Int32Value` → `Integer` (nullable)
  - `google.protobuf.Int64Value` → `Long` (nullable)
  - `google.protobuf.UInt32Value` → `Long` (nullable, unsigned)
  - `google.protobuf.UInt64Value` → `Long` (nullable, unsigned)
  - `google.protobuf.BoolValue` → `Boolean` (nullable)
  - `google.protobuf.FloatValue` → `Float` (nullable)
  - `google.protobuf.DoubleValue` → `Double` (nullable)
  - `google.protobuf.BytesValue` → `byte[]` (nullable)
  - `google.protobuf.FieldMask` → `List<String>`
  - `google.protobuf.Struct` → `Map<String, Object>`
  - `google.protobuf.Value` → `Object`
  - `google.protobuf.ListValue` → `List<Object>`

#### New Configuration Options
- **`convertWellKnownTypes`** (default: `true`) — Enable/disable WKT conversion
- **`generateRawProtoAccessors`** (default: `false`) — Generate additional `getXxxProto()` methods

#### New Handlers
- `WellKnownTypeHandler` — Handler for scalar well-known type fields
- `RepeatedWellKnownTypeHandler` — Handler for repeated well-known type fields

#### New Generator Classes
- `WellKnownTypeInfo` — Enum registry with 15 supported types and inline conversion code
- `StructConverterGenerator` — Generates utility class for Struct/Value/ListValue conversion

#### StructConverter Utility Class
- **Auto-generated when needed** — Only generated if Struct/Value/ListValue fields are used
- **Bidirectional conversion:**
  - `toMap(Struct)` → `Map<String, Object>`
  - `toStruct(Map)` → `Struct`
  - `toObject(Value)` → `Object`
  - `toValue(Object)` → `Value`
  - `toList(ListValue)` → `List<Object>`
  - `toListValue(List)` → `ListValue`

### Changed
- `FieldInfo` — Added `wellKnownType` field and detection logic
- `MergedField` — Propagates well-known type info from FieldInfo
- `TypeResolver` — Added WKT type resolution in `parseFieldType()`
- `FieldProcessingChain` — Added WellKnownTypeHandler and RepeatedWellKnownTypeHandler
- `HandlerType` — Added `WELL_KNOWN_TYPE` and `REPEATED_WELL_KNOWN_TYPE` entries
- `ConflictHandler` — Updated sealed interface permits clause
- `AbstractConflictHandler` — Updated sealed class permits clause
- `GenerationOrchestrator` — Added utility class generation

### Design Decisions
- **Inline conversion code** — No runtime dependencies required
- **Conditional generation** — StructConverter only generated when Struct/Value/ListValue used
- **google.protobuf.Any not supported** — Requires runtime type registry, incompatible with inline approach

### Example Usage

```java
// Proto definition:
// google.protobuf.Timestamp created_at = 1;
// google.protobuf.Duration timeout = 2;
// google.protobuf.StringValue optional_name = 3;

// Generated interface:
public interface Event {
    Instant getCreatedAt();        // java.time.Instant
    Duration getTimeout();          // java.time.Duration
    String getOptionalName();       // nullable String
}

// Builder support:
Event event = Event.newBuilder(ctx)
    .setCreatedAt(Instant.now())
    .setTimeout(Duration.ofMinutes(5))
    .setOptionalName("test")
    .build();
```

---

## [1.2.0] - 2026-01-02

### Added

#### Comprehensive Conflict Type System
- **ENUM_ENUM conflict type** - Handle enum-to-enum conflicts with different values across versions
- **FLOAT_DOUBLE conflict type** - Handle float/double precision differences
- **SIGNED_UNSIGNED conflict type** - Handle signed/unsigned integer conflicts (int32/uint32, sint32, etc.)
- **REPEATED_SINGLE conflict type** - Handle repeated vs singular field conflicts
- **PRIMITIVE_MESSAGE conflict type** - Detect primitive to message type changes
- **OPTIONAL_REQUIRED conflict type** - Handle optional/required field modifier differences

#### New Conflict Handlers
- `EnumEnumHandler` - Generates unified int accessor for enum-enum conflicts
- `FloatDoubleHandler` - Generates double accessor for float/double conflicts
- `SignedUnsignedHandler` - Generates long accessor for signed/unsigned conflicts
- `RepeatedSingleHandler` - Generates List accessor for repeated/single conflicts
- `MapFieldHandler` - Full map field support with type conflicts:
  - WIDENING conflicts (int32 in v1, int64 in v2 → unified as long)
  - INT_ENUM conflicts (int32 in v1, enum in v2 → unified as int)
  - All accessor methods: `getXxxMap()`, `getXxxCount()`, `containsXxx()`, `getXxxOrDefault()`, `getXxxOrThrow()`
  - All builder methods: `putXxx()`, `putAllXxx()`, `removeXxx()`, `clearXxx()`
  - Lazy caching with `volatile` fields for thread-safe performance
  - Non-sequential enum value support (uses `getNumber()` instead of `ordinal()`)
  - Validation for invalid enum values in builders with clear error messages

#### Exception Hierarchy
- `ProtoWrapperException` - Base exception class
- `AnalysisException` - Proto file analysis errors
- `GenerationException` - Code generation errors
- `ConfigurationException` - Configuration validation errors
- `MergeException` - Schema merging errors

#### Oneof Field Support
- **Full oneof support** - Generate unified API for protobuf oneof fields across versions
  - `XxxCase` enum for discriminator (e.g., `MethodCase.CREDIT_CARD`)
  - `getXxxCase()` method to check which field is set
  - `hasXxx()` methods for individual oneof fields
  - `clearXxx()` builder method to clear entire oneof group
  - Works across versions with different oneof structures

#### Oneof Conflict Detection
- **Comprehensive conflict detection** - Automatically detects and logs oneof-related conflicts:
  - `PARTIAL_EXISTENCE` - Oneof exists only in some versions
  - `FIELD_SET_DIFFERENCE` - Different fields in oneof across versions
  - `FIELD_TYPE_CONFLICT` - Type conflict within oneof field
  - `RENAMED` - Oneof renamed between versions (detected by matching field numbers)
  - `FIELD_MEMBERSHIP_CHANGE` - Field moved in/out of oneof
  - `FIELD_NUMBER_CHANGE` - Field number changed within oneof
  - `FIELD_REMOVED` - Field removed from oneof in some version
  - `INCOMPATIBLE_TYPES` - Incompatible field types in oneof

#### New Model Classes
- `OneofInfo` - Oneof group information (name, index, field numbers)
- `MergedOneof` - Merged oneof across versions with conflict tracking
- `OneofConflictType` - Enum of all oneof conflict types
- `OneofConflictInfo` - Detailed conflict information with affected versions
- `OneofConflictDetector` - Conflict detection logic

#### New Tests
- `OneofConflictDetectorTest` - Unit tests for all conflict detection types
- Integration tests for oneof field generation
- `NonSequentialEnumTests` - Tests for map fields with non-sequential enum values

#### Documentation Improvements
- **CONTRIBUTING.md** - Contribution guidelines with code style, testing, and PR process
- **Mermaid diagrams** in ARCHITECTURE.md - Class diagrams, sequence diagrams, component diagrams
- **docs/archive/** - Organized completed plans and drafts

#### Package Documentation
- Added `package-info.java` for all packages with comprehensive JavaDoc

### Changed
- `FieldInfo` - Added `oneofIndex` and `oneofName` fields
- `MergedMessage` - Added oneof groups tracking
- `VersionMerger` - Integrated oneof merging with conflict detection and logging
- Conflict warnings logged at WARN level for visibility
- Refactored generators to use `GenerationContext` consistently
- Improved type resolution for nested messages
- Better conflict detection logging

#### Deprecated API Improvements
- Updated `@Deprecated` annotations with `forRemoval = true, since = "1.2.0"`
- Added migration instructions in JavaDoc
- Deprecated API will be removed in version 2.0.0:
  - `InterfaceGenerator.setSchema()`, `generate(MergedMessage)`, `generateAndWrite(MergedMessage)`
  - `AbstractClassGenerator.setSchema()`, `generate(MergedMessage)`, `generateAndWrite(MergedMessage)`
  - `ImplClassGenerator.setSchema()`, `generate(MergedMessage, String, String)`, `generateAndWrite(...)`
  - `MergedField(FieldInfo, String)` constructor, `addVersion(String, FieldInfo)` method
  - `ProtocExecutor(Consumer<String>)` constructor

### Fixed
- Correct handling of optional fields in REPEATED_SINGLE conflicts
- Proper type resolution for PRIMITIVE_MESSAGE conflicts with nested types
- **CRITICAL:** Fixed `ordinal()` vs `getNumber()` bug in map enum conversion - was causing data corruption for non-sequential enum values
- Fixed NPE when putting invalid enum values in map builders - now throws `IllegalArgumentException` with clear message

### Known Limitations
- Renamed oneofs use the most common name across versions
- Fields in oneofs that exist only in some versions return null/default in other versions

---

## [1.1.1] - 2025-12-30

### Added

#### Static newBuilder(VersionContext) Method
- **`Type.newBuilder(ctx)` static method** — Create builders directly from interface types
  - Available on all generated interfaces (top-level and nested)
  - Equivalent to `ctx.newTypeBuilder()` but more intuitive
  - Supports full builder chaining

```java
// New intuitive approach
Money money = Money.newBuilder(ctx)
        .setAmount(1000)
        .setCurrency("USD")
        .build();

// For nested types
Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
        .setLatitude(40.7128)
        .setLongitude(-74.0060)
        .build();

// Complex object graphs
OrderRequest order = OrderRequest.newBuilder(ctx)
        .setOrderId("ORD-001")
        .setCustomer(Customer.newBuilder(ctx)
                .setId("CUST-001")
                .setName("John Doe")
                .build())
        .addItems(OrderItem.newBuilder(ctx)
                .setProductId("PROD-001")
                .setQuantity(2)
                .build())
        .build();
```

**Features:**
- Works with all top-level interfaces
- Works with all nested interfaces (e.g., `AuthResponse.SecurityChallenge.newBuilder(ctx)`)
- Generated method delegates to `ctx.newTypeBuilder()` internally
- Full JavaDoc with `@param` and `@return` documentation
- Consistent with protobuf's native `Type.newBuilder()` pattern

#### Integration Tests
- **StaticNewBuilderTest** — 24 comprehensive tests for the new feature:
  - Top-level interface tests (Money, Date, Address, UserProfile)
  - Nested interface tests (GeoLocation, SecurityChallenge)
  - Builder chaining tests
  - Round-trip serialization tests
  - Cross-version compatibility tests
  - getContext() integration tests
  - Complex object graph tests
  - toBuilder() integration tests

### Changed
- **Total integration tests increased** — From 82 to 106 tests

---

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

- `map` fields have basic support only (full support added in v1.2.0)
- Complex nested message hierarchies may require manual configuration
