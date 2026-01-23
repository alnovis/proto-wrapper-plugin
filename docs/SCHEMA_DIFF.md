# Schema Diff Tool

Compare protobuf schema versions and detect breaking changes. Available as CLI, Maven goal, and Gradle task.

**Since:** v1.5.0

---

## Table of Contents

- [Overview](#overview)
- [CLI Usage](#cli-usage)
- [Maven Usage](#maven-usage)
- [Gradle Usage](#gradle-usage)
- [Field Mappings & Renumber Detection](#field-mappings--renumber-detection)
- [Output Formats](#output-formats)
- [Breaking Change Types](#breaking-change-types)
- [Programmatic API](#programmatic-api)

---

## Overview

The Schema Diff tool analyzes differences between two protobuf schema directories and identifies:

- Added, modified, and removed messages and enums
- Breaking changes that affect wire compatibility
- Non-breaking changes (warnings)
- Suspected field renumbering (with suggested mappings)

### Features

- **Multiple output formats**: text, JSON, Markdown
- **CI/CD integration**: Exit codes for breaking changes
- **Custom version names**: Label versions in reports
- **Breaking-only mode**: Filter to show only critical issues
- **Field mappings**: Explicit mapping for renumbered fields
- **Renumber detection**: Heuristic detection of field renumbering
- **Auto-detect include path**: Resolves versioned proto imports automatically

---

## CLI Usage

The CLI is packaged as an executable JAR (`proto-wrapper-core-1.6.5-cli.jar`).

### Basic Commands

```bash
# Basic comparison
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2

# With output format
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 --format=markdown

# Show only breaking changes
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 --breaking-only

# Write to file
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 --output=report.md

# CI/CD mode: exit code 1 if breaking changes detected
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 --fail-on-breaking

# Custom version names
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 \
    --v1-name=production --v2-name=development

# Quiet mode
java -jar proto-wrapper-core-1.6.5-cli.jar diff proto/v1 proto/v2 -q
```

### CLI Options

| Option | Short | Description |
|--------|-------|-------------|
| `--v1-name=<name>` | | Name for source version (default: v1) |
| `--v2-name=<name>` | | Name for target version (default: v2) |
| `--format=<fmt>` | `-f` | Output format: text, json, markdown (default: text) |
| `--output=<file>` | `-o` | Write output to file instead of console |
| `--breaking-only` | `-b` | Show only breaking changes |
| `--fail-on-breaking` | | Exit with code 1 if breaking changes detected |
| `--fail-on-warning` | | Treat warnings as errors |
| `--protoc=<path>` | | Path to protoc executable |
| `--quiet` | `-q` | Suppress informational messages |
| `--help` | `-h` | Show help message |
| `--version` | `-V` | Print version information |

---

## Maven Usage

Use the `diff` goal to compare schemas.

### Command Line

```bash
# Basic usage
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2

# With output format
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Dformat=markdown

# Write to file
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Doutput=target/diff.md

# Fail on breaking changes
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -DfailOnBreaking=true

# Custom version names
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 \
    -Dv1Name=production -Dv2Name=development
```

### POM Configuration

Configure as part of your build:

```xml
<plugin>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.6.5</version>
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

### Maven Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `v1` | (required) | Source (older) version directory |
| `v2` | (required) | Target (newer) version directory |
| `v1Name` | `v1` | Name for source version in reports |
| `v2Name` | `v2` | Name for target version in reports |
| `format` | `text` | Output format: text, json, markdown |
| `output` | (console) | Output file path |
| `breakingOnly` | `false` | Show only breaking changes |
| `failOnBreaking` | `false` | Fail build on breaking changes |
| `failOnWarning` | `false` | Treat warnings as errors |
| `protoc.path` | (from PATH) | Custom protoc path |
| `includePath` | (auto-detected) | Include path for proto imports |
| `fieldMappings` | (none) | Field mapping configuration for renumbered fields |

### Field Mappings

Configure field mappings to handle renumbered fields:

```xml
<configuration>
    <fieldMappings>
        <fieldMapping>
            <message>TicketRequest</message>
            <fieldName>parent_ticket</fieldName>
            <versionNumbers>
                <v202>17</v202>
                <v203>15</v203>
            </versionNumbers>
        </fieldMapping>
    </fieldMappings>
</configuration>
```

When configured:
- Mapped fields are shown as `~ Renumbered field: fieldName #N -> #M [MAPPED]`
- Renumbered fields are treated as INFO-level (not breaking)
- The summary shows `Renumbers: N mapped, M suspected`

### Renumber Detection

Without explicit field mappings, the diff tool heuristically detects suspected renumbered fields:

```
SUSPECTED RENUMBERED FIELDS
----------------------------
  TicketRequest.parent_ticket: #17 -> #15 (HIGH confidence)
    Suggested mapping:
      <fieldMapping>
          <message>TicketRequest</message>
          <fieldName>parent_ticket</fieldName>
          <versionNumbers><v202>17</v202><v203>15</v203></versionNumbers>
      </fieldMapping>
```

Detection strategies:
1. **REMOVED+ADDED pairs** — same proto name in both removed and added fields
2. **Displaced fields** — a removed field's name matches the v2 side of a renamed field

Confidence levels:
- **HIGH** — same name and same type
- **MEDIUM** — same name with compatible type conversion (integer widening, int-enum, float-double, string-bytes)

---

## Gradle Usage

Register and configure a `SchemaDiffTask`.

### Build Configuration

```kotlin
// build.gradle.kts
plugins {
    id("io.alnovis.proto-wrapper") version "1.6.5"
}

// Register diff task
tasks.register<io.alnovis.protowrapper.gradle.SchemaDiffTask>("diffSchemas") {
    v1Directory.set(file("proto/v1"))
    v2Directory.set(file("proto/v2"))
    v1Name.set("production")
    v2Name.set("development")
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

### Running

```bash
# Run diff task
./gradlew diffSchemas

# With CI mode
./gradlew diffSchemas -PfailOnBreaking=true
```

### Gradle Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `v1Directory` | `DirectoryProperty` | (required) | Source version directory |
| `v2Directory` | `DirectoryProperty` | (required) | Target version directory |
| `v1Name` | `Property<String>` | `v1` | Name for source version |
| `v2Name` | `Property<String>` | `v2` | Name for target version |
| `outputFormat` | `Property<String>` | `text` | Format: text, json, markdown |
| `outputFile` | `RegularFileProperty` | (console) | Output file |
| `breakingOnly` | `Property<Boolean>` | `false` | Show only breaking changes |
| `failOnBreaking` | `Property<Boolean>` | `false` | Fail task on breaking changes |
| `fieldMappings` | `ListProperty<FieldMappingData>` | (none) | Field mapping configuration |

---

## Output Formats

### Text Format

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
Changes:   2 errors, 0 warnings, 0 plugin-handled
Renumbers: 0 mapped, 0 suspected
```

### JSON Format

```json
{
  "v1": "production",
  "v2": "development",
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

### Markdown Format

```markdown
# Schema Comparison: production -> development

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

---

## Breaking Change Types

### Errors (Breaking)

| Type | Description |
|------|-------------|
| `MESSAGE_REMOVED` | Message was removed entirely |
| `FIELD_REMOVED` | Field was removed from message |
| `FIELD_NUMBER_CHANGED` | Field number was changed |
| `FIELD_TYPE_INCOMPATIBLE` | Field type changed incompatibly |
| `ENUM_REMOVED` | Enum was removed |
| `ENUM_VALUE_REMOVED` | Enum value was removed |
| `ENUM_VALUE_NUMBER_CHANGED` | Enum value number changed |

### Warnings (Potentially Breaking)

| Type | Description |
|------|-------------|
| `REQUIRED_FIELD_ADDED` | Required field added (proto2) |
| `LABEL_CHANGED_TO_REQUIRED` | Field changed to required |
| `CARDINALITY_CHANGED` | Field cardinality changed (singular ↔ repeated) |
| `ONEOF_FIELD_MOVED` | Field moved in/out of oneof |

---

## Programmatic API

Use the Schema Diff API directly in your code:

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
        System.err.println(bc.severity() + ": " +
            bc.entityPath() + " - " + bc.description());
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

// Compare with field mappings
List<FieldMapping> mappings = List.of(
    new FieldMapping("Order", "parent_ref",
        Map.of("v1", 3, "v2", 5))
);
SchemaDiff diffWithMappings = SchemaDiff.compare(v1Schema, v2Schema, mappings);

// Check suspected renumbers
if (diff.hasSuspectedRenumbers()) {
    for (SuspectedRenumber sr : diff.getSuspectedRenumbers()) {
        System.out.println(sr.messageName() + "." + sr.fieldName() +
            ": #" + sr.v1Number() + " -> #" + sr.v2Number() +
            " (" + sr.confidence() + ")");
        // Generate suggested mapping
        FieldMapping suggested = sr.toSuggestedMapping("v1", "v2");
    }
}
```

---

## CI/CD Integration

### GitHub Actions

```yaml
- name: Check for Breaking Changes
  run: |
    mvn proto-wrapper:diff \
      -Dv1=proto/main \
      -Dv2=proto/feature \
      -DfailOnBreaking=true \
      -Dformat=markdown \
      -Doutput=diff-report.md

- name: Upload Report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: schema-diff-report
    path: diff-report.md
```

### GitLab CI

```yaml
schema-check:
  stage: test
  script:
    - mvn proto-wrapper:diff -Dv1=proto/main -Dv2=proto/$CI_COMMIT_REF_NAME -DfailOnBreaking=true
  allow_failure: false
```

---

## See Also

- [Getting Started](GETTING_STARTED.md) - Initial setup
- [Configuration](CONFIGURATION.md) - Plugin options
- [Known Issues](KNOWN_ISSUES.md) - Limitations
