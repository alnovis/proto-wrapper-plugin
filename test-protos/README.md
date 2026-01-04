# Test Proto Files

This directory contains proto files used for integration testing of proto-wrapper plugins.

## Directory Structure

```
test-protos/
└── scenarios/
    ├── diff/                    # Schema diff testing scenarios
    │   ├── 01-simple-add/       # Message addition
    │   ├── 02-simple-remove/    # Message removal (breaking change)
    │   ├── 03-field-changes/    # Field add/remove/type changes
    │   ├── 04-enum-changes/     # Enum value add/remove
    │   ├── 05-complex-changes/  # Multiple changes together
    │   └── 06-no-changes/       # Identical schemas
    │
    └── generation/              # Wrapper generation testing
        ├── v1/                  # Version 1 proto files
        └── v2/                  # Version 2 proto files
```

## Diff Scenarios

Each diff scenario contains `v1/` and `v2/` subdirectories with proto files representing
two schema versions. These are used to test the schema diff functionality.

| Scenario | Description | Breaking Changes |
|----------|-------------|------------------|
| 01-simple-add | New message added in v2 | No |
| 02-simple-remove | Message removed in v2 | Yes |
| 03-field-changes | Field additions, removals, type changes | Yes |
| 04-enum-changes | Enum value additions and removals | Yes (removal) |
| 05-complex-changes | Multiple types of changes | Yes |
| 06-no-changes | Identical v1 and v2 | No |

## Generation Scenarios

The `generation/` directory contains complex proto files with various features:
- Type conflicts between versions
- Nested messages and enums
- Well-known types (Timestamp, Duration, etc.)
- Map fields
- Oneof fields

These files are used to test the wrapper generation functionality.

## Usage

Both Maven and Gradle integration test modules reference these proto files
using relative paths from the project root.
