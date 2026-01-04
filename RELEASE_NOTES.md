# Release Notes - Proto Wrapper Plugin v1.5.1

**Release Date:** January 4, 2026

## Overview

Version 1.5.1 is a patch release that fixes the breaking change classification in the Schema Diff Tool. PRIMITIVE_MESSAGE type conflicts are now correctly classified as plugin-handled (INFO) instead of incompatible (ERROR).

## Bug Fix

### PRIMITIVE_MESSAGE Classification

The diff tool was incorrectly classifying primitive-to-message type conflicts (e.g., `uint32` → `ParentTicket`) as ERROR. The plugin actually handles these conflicts by generating dual accessors:

- `getXxx()` - Returns primitive value (works in primitive versions)
- `getXxxMessage()` - Returns message wrapper (works in message versions)
- `supportsXxx()` - Returns true for primitive versions
- `supportsXxxMessage()` - Returns true for message versions

**Before (v1.5.0):**
```
Changes: 1 error, 1 warning, 32 plugin-handled
[ERROR] FIELD_TYPE_INCOMPATIBLE: TicketRequest.shiftDocumentNumber (uint32 -> ParentTicket)
```

**After (v1.5.1):**
```
Changes: 0 errors, 1 warning, 33 plugin-handled
[INFO] FIELD_TYPE_CONVERTED: TicketRequest.shiftDocumentNumber (uint32 -> ParentTicket)
       PRIMITIVE_MESSAGE: Plugin generates getXxx() and getXxxMessage() accessors
```

---

# Schema Diff Tool (from v1.5.0)

This release introduces the **Schema Diff Tool** - a comprehensive solution for comparing protobuf schemas between versions and detecting breaking changes. Available as CLI, Maven goal, and Gradle task, it integrates seamlessly with CI/CD pipelines.

## What's New

### Schema Diff Tool

Compare proto schemas and detect changes including breaking changes that could affect wire compatibility.

#### Three Ways to Use

1. **CLI** - Standalone command-line tool
2. **Maven** - `proto-wrapper:diff` goal
3. **Gradle** - `SchemaDiffTask`

### CLI Tool

The CLI is packaged as an executable JAR with all dependencies included.

```bash
# Basic comparison
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2

# Output formats
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --format=text
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --format=json
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --format=markdown

# Show only breaking changes
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --breaking-only

# Write to file
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --output=diff-report.md

# CI/CD mode: exit code 1 if breaking changes detected
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --fail-on-breaking

# Custom version names
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --v1-name=production --v2-name=development

# Quiet mode
java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 -q
```

#### CLI Options

| Option | Description |
|--------|-------------|
| `--v1-name=<name>` | Name for source version (default: v1) |
| `--v2-name=<name>` | Name for target version (default: v2) |
| `-f, --format=<fmt>` | Output format: text, json, markdown (default: text) |
| `-o, --output=<file>` | Write output to file instead of console |
| `-b, --breaking-only` | Show only breaking changes |
| `--fail-on-breaking` | Exit with code 1 if breaking changes detected |
| `--fail-on-warning` | Treat warnings as errors |
| `--protoc=<path>` | Path to protoc executable |
| `-q, --quiet` | Suppress informational messages |
| `-h, --help` | Show help message |
| `-V, --version` | Print version information |

### Maven Goal

Use the `diff` goal to compare schemas in Maven builds:

```bash
# Command line usage
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2

# With output format
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Dformat=markdown

# Write to file
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Doutput=target/diff-report.md

# Fail on breaking changes (CI/CD)
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -DfailOnBreaking=true

# Custom version names
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Dv1Name=production -Dv2Name=development
```

#### pom.xml Configuration

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.5.1</version>
    <executions>
        <!-- Schema diff during verify phase -->
        <execution>
            <id>check-breaking-changes</id>
            <phase>verify</phase>
            <goals>
                <goal>diff</goal>
            </goals>
            <configuration>
                <v1>${basedir}/proto/production</v1>
                <v2>${basedir}/proto/development</v2>
                <v1Name>production</v1Name>
                <v2Name>development</v2Name>
                <outputFormat>markdown</outputFormat>
                <outputFile>${project.build.directory}/schema-diff.md</outputFile>
                <failOnBreaking>true</failOnBreaking>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Maven Goal Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `v1` | (required) | Directory with source version proto files |
| `v2` | (required) | Directory with target version proto files |
| `v1Name` | `v1` | Name for source version in reports |
| `v2Name` | `v2` | Name for target version in reports |
| `format` | `text` | Output format: text, json, markdown |
| `output` | (console) | Output file path |
| `breakingOnly` | `false` | Show only breaking changes |
| `failOnBreaking` | `false` | Fail build on breaking changes |
| `failOnWarning` | `false` | Treat warnings as errors |
| `protoc.path` | (from PATH) | Custom protoc executable path |

### Gradle Task

Register and configure a `SchemaDiffTask`:

```kotlin
// build.gradle.kts
plugins {
    id("space.alnovis.proto-wrapper") version "1.5.1"
}

// Register diff task
tasks.register<space.alnovis.protowrapper.gradle.SchemaDiffTask>("diffSchemas") {
    v1Directory.set(file("proto/v1"))
    v2Directory.set(file("proto/v2"))
    v1Name.set("v1")
    v2Name.set("v2")
    outputFormat.set("markdown")
    outputFile.set(file("build/reports/schema-diff.md"))
    failOnBreaking.set(false)
    breakingOnly.set(false)
}

// Run as part of check
tasks.named("check") {
    dependsOn("diffSchemas")
}
```

```bash
# Run diff task
./gradlew diffSchemas
```

#### Gradle Task Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `v1Directory` | `DirectoryProperty` | (required) | Source version directory |
| `v2Directory` | `DirectoryProperty` | (required) | Target version directory |
| `v1Name` | `Property<String>` | `v1` | Name for source version |
| `v2Name` | `Property<String>` | `v2` | Name for target version |
| `outputFormat` | `Property<String>` | `text` | Output format: text, json, markdown |
| `outputFile` | `RegularFileProperty` | (console) | Output file |
| `breakingOnly` | `Property<Boolean>` | `false` | Show only breaking changes |
| `failOnBreaking` | `Property<Boolean>` | `false` | Fail task on breaking changes |

### Output Formats

#### Text Format

```
Schema Comparison: v1 -> v2

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
  [ERROR] MESSAGE_REMOVED: DeprecatedMessage (was: DeprecatedMessage)

================================================================================
SUMMARY
================================================================================

Messages:  +1 added, ~1 modified, -1 removed
Enums:     +0 added, ~0 modified, -0 removed
Breaking:  2 errors, 0 warnings
```

#### JSON Format

```json
{
  "v1": "v1",
  "v2": "v2",
  "summary": {
    "addedMessages": 1,
    "removedMessages": 1,
    "modifiedMessages": 1,
    "errorCount": 2,
    "warningCount": 0
  },
  "messages": {
    "added": [{"name": "Profile", "sourceFile": "user.proto"}],
    "removed": [{"name": "DeprecatedMessage"}],
    "modified": [{"name": "User", "fieldChanges": [...]}]
  },
  "breakingChanges": [
    {"type": "FIELD_REMOVED", "severity": "ERROR", "entityPath": "User.email"},
    {"type": "MESSAGE_REMOVED", "severity": "ERROR", "entityPath": "DeprecatedMessage"}
  ]
}
```

#### Markdown Format

```markdown
# Schema Comparison: v1 -> v2

## Summary

| Category | Added | Modified | Removed |
|----------|-------|----------|---------|
| Messages | 1 | 1 | 1 |

**Breaking Changes:** 2 errors, 0 warnings

## Breaking Changes

| Severity | Type | Entity | Description |
|----------|------|--------|-------------|
| ERROR | FIELD_REMOVED | User.email | Field removed |
| ERROR | MESSAGE_REMOVED | DeprecatedMessage | Message removed |
```

### Breaking Change Types

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
| `CARDINALITY_CHANGED` | WARNING | Field cardinality changed |
| `ONEOF_FIELD_MOVED` | WARNING | Field moved in/out of oneof |

### Programmatic API

```java
import space.alnovis.protowrapper.diff.SchemaDiff;
import space.alnovis.protowrapper.diff.model.*;

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
        System.err.println(bc.severity() + ": " + bc.entityPath() + " - " + bc.description());
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

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.5.1</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.5.1"
}
```

### 2. Download CLI JAR (Optional)

The CLI JAR is available as:
- `proto-wrapper-core-1.5.1-cli.jar` in Maven build (`target/`)
- `proto-wrapper-core-1.5.1-cli.jar` in Gradle build (`build/libs/`)

### 3. Add to CI/CD Pipeline

**GitHub Actions:**
```yaml
- name: Check Breaking Changes
  run: |
    java -jar proto-wrapper-core-1.5.1-cli.jar diff \
      proto/production proto/development \
      --fail-on-breaking \
      --output=schema-diff.md

- name: Upload Diff Report
  uses: actions/upload-artifact@v4
  with:
    name: schema-diff
    path: schema-diff.md
```

**GitLab CI:**
```yaml
check-schema:
  script:
    - java -jar proto-wrapper-core-1.5.1-cli.jar diff proto/v1 proto/v2 --fail-on-breaking
  artifacts:
    reports:
      junit: schema-diff.xml
```

## Breaking Changes

None. This release is fully backward compatible.

## Build Infrastructure Improvements

### Gradle 9.x Compatibility

- Upgraded Kotlin from 1.9.22 to 2.1.20
- Changed `kotlinOptions` to `compilerOptions` with `JvmTarget.JVM_17`
- Added JUnit Platform Launcher dependency

### Test Optimization

- Added `@Tag("slow")` for TestKit-based tests
- New `slowTest` task with parallel execution
- Regular `test` task excludes slow tests
- Build time reduced from ~3 min to ~45 sec

### CI Workflow Updates

- Added explicit Gradle version (8.5) via `gradle/actions/setup-gradle@v4`
- Added `mavenLocal()` to Gradle init.gradle
- Added `slowTest` step for comprehensive CI testing

## New Dependencies

- **picocli 4.7.5** - CLI argument parsing framework
- **maven-shade-plugin 3.5.1** - Executable JAR packaging (Maven)
- **shadow plugin 8.1.1** - Executable JAR packaging (Gradle)

## Implementation Details

### Core Classes

| Class | Package | Description |
|-------|---------|-------------|
| `SchemaDiff` | `diff` | Main facade for schema comparison |
| `SchemaDiffEngine` | `diff` | Core comparison logic |
| `BreakingChangeDetector` | `diff.breaking` | Breaking change identification |
| `TextDiffFormatter` | `diff.formatter` | Plain text output |
| `JsonDiffFormatter` | `diff.formatter` | JSON output |
| `MarkdownDiffFormatter` | `diff.formatter` | Markdown tables |
| `SchemaDiffCli` | `cli` | CLI entry point using picocli |
| `DiffMojo` | `mojo` | Maven goal |
| `SchemaDiffTask` | `gradle` | Gradle task |

### Data Models

| Class | Description |
|-------|-------------|
| `MessageDiff` | Message-level changes with field details |
| `FieldChange` | Individual field changes with type info |
| `EnumDiff` | Enum changes with value tracking |
| `EnumValueChange` | Enum value additions/removals |
| `BreakingChange` | Breaking change with severity and context |
| `ChangeType` | Enum: ADDED, REMOVED, MODIFIED, etc. |

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [API Reference](docs/API_REFERENCE.md)
- [Roadmap](docs/ROADMAP.md)
- [Cookbook](docs/COOKBOOK.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
