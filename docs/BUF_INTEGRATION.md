# buf.build Integration Guide

Version: 1.0
Last Updated: 2026-01-18

This document describes integration between proto-wrapper and [buf.build](https://buf.build) ecosystem.

---

## Table of Contents

- [What is buf?](#what-is-buf)
- [Why buf?](#why-buf)
- [Installation](#installation)
- [Configuration](#configuration)
- [Linting](#linting)
- [Breaking Change Detection](#breaking-change-detection)
- [Code Generation](#code-generation)
- [Buf Schema Registry (BSR)](#buf-schema-registry-bsr)
- [CI/CD Integration](#cicd-integration)
- [Migration from protoc](#migration-from-protoc)
- [proto-wrapper Integration](#proto-wrapper-integration)

---

## What is buf?

**buf** is a modern toolchain for Protocol Buffers, created by Buf Technologies. It has become the de-facto standard for protobuf management, replacing fragmented protoc-based workflows.

### Core Components

| Component | Description |
|-----------|-------------|
| `buf lint` | Linting proto files against best practices |
| `buf breaking` | Detecting breaking changes between versions |
| `buf generate` | Code generation with declarative configuration |
| `buf build` | Compiling protos to binary format |
| `buf push/export` | Publishing/fetching schemas from BSR |

---

## Why buf?

### Comparison with Traditional Approach

| Aspect | protoc | buf |
|--------|--------|-----|
| Linting | None (requires external tools) | Built-in, configurable |
| Breaking detection | None (manual review) | Built-in, CI-ready |
| Dependencies | Copy files manually | `buf.yaml` with versioned deps |
| Generation | Complex CLI flags | Declarative `buf.gen.yaml` |
| Registry | None | BSR (Buf Schema Registry) |
| Performance | Slow (C++) | Fast (Rust/Go) |
| Error messages | Cryptic | Clear, actionable |

### Key Benefits

1. **Consistent Style** - Enforce naming conventions, documentation requirements
2. **Breaking Change Prevention** - Catch API breaks before merge
3. **Dependency Management** - Version proto dependencies like npm packages
4. **Fast Builds** - Parallel processing, caching
5. **CI/CD Ready** - Exit codes, JSON output for automation

---

## Installation

### macOS

```bash
brew install bufbuild/buf/buf
```

### Linux

```bash
# Binary
curl -sSL https://github.com/bufbuild/buf/releases/latest/download/buf-Linux-x86_64 \
  -o /usr/local/bin/buf && chmod +x /usr/local/bin/buf

# Or via npm
npm install -g @bufbuild/buf
```

### Verify

```bash
buf --version
# 1.28.0
```

---

## Configuration

### Project Structure

```
project/
├── buf.yaml           # Main configuration
├── buf.gen.yaml       # Code generation config
├── buf.lock           # Dependency lock file
└── proto/
    ├── v1/
    │   └── order.proto
    ├── v2/
    │   └── order.proto
    └── v3/
        └── order.proto
```

### buf.yaml

```yaml
version: v1

# Module name (for BSR publishing)
name: buf.build/yourorg/yourproject

# Directories containing proto files
dirs:
  - proto

# Linting configuration
lint:
  use:
    - DEFAULT
  except:
    - ENUM_VALUE_PREFIX
    - ENUM_ZERO_VALUE_SUFFIX
  ignore:
    - proto/vendor  # Ignore vendored protos

# Breaking change detection
breaking:
  use:
    - FILE
  ignore:
    - proto/internal  # Internal protos can break

# Dependencies
deps:
  - buf.build/googleapis/googleapis
  - buf.build/grpc-ecosystem/grpc-gateway
```

### buf.gen.yaml

```yaml
version: v1

managed:
  enabled: true
  java_package_prefix: com.example
  go_package_prefix:
    default: github.com/example/gen/go

plugins:
  # Java
  - plugin: java
    out: gen/java

  # Go
  - plugin: go
    out: gen/go
    opt: paths=source_relative

  # gRPC
  - plugin: grpc-java
    out: gen/java

  # proto-wrapper (planned for v2.2.0)
  - plugin: buf.build/alnovis/proto-wrapper
    out: gen/java
    opt:
      - basePackage=com.example.model
      - generateBuilders=true
```

---

## Linting

### Running Lint

```bash
# Lint all protos
buf lint

# Lint specific directory
buf lint proto/v3

# Output as JSON (for CI)
buf lint --error-format=json
```

### Lint Rules

#### Naming Rules

| Rule | Description | Example |
|------|-------------|---------|
| `PACKAGE_LOWER_SNAKE_CASE` | Package names lowercase | `package order.v1;` |
| `MESSAGE_PASCAL_CASE` | Messages in PascalCase | `message OrderItem` |
| `FIELD_LOWER_SNAKE_CASE` | Fields in snake_case | `string order_id = 1;` |
| `ENUM_PASCAL_CASE` | Enums in PascalCase | `enum PaymentType` |
| `ENUM_VALUE_UPPER_SNAKE_CASE` | Enum values UPPER_SNAKE | `PAYMENT_TYPE_CASH = 0;` |
| `RPC_PASCAL_CASE` | RPC methods in PascalCase | `rpc CreateOrder(...)` |
| `SERVICE_PASCAL_CASE` | Services in PascalCase | `service OrderService` |

#### Documentation Rules

| Rule | Description |
|------|-------------|
| `COMMENT_MESSAGE` | Messages must have comments |
| `COMMENT_FIELD` | Fields must have comments |
| `COMMENT_ENUM` | Enums must have comments |
| `COMMENT_ENUM_VALUE` | Enum values must have comments |
| `COMMENT_RPC` | RPC methods must have comments |
| `COMMENT_SERVICE` | Services must have comments |

#### Package Rules

| Rule | Description |
|------|-------------|
| `PACKAGE_DEFINED` | Package must be defined |
| `PACKAGE_DIRECTORY_MATCH` | Package matches directory structure |
| `PACKAGE_VERSION_SUFFIX` | Package should have version suffix |

#### Other Rules

| Rule | Description |
|------|-------------|
| `IMPORT_NO_WEAK` | No weak imports |
| `IMPORT_NO_PUBLIC` | No public imports |
| `IMPORT_USED` | All imports must be used |
| `ENUM_ZERO_VALUE_SUFFIX` | Zero value should be `_UNSPECIFIED` |
| `ENUM_VALUE_PREFIX` | Enum values prefixed with enum name |
| `FIELD_NO_DESCRIPTOR` | Fields not named `descriptor` |
| `ONEOF_LOWER_SNAKE_CASE` | Oneof names in snake_case |

### Custom Configuration

```yaml
# buf.yaml
lint:
  use:
    - DEFAULT
  except:
    # Disable rules that conflict with legacy protos
    - ENUM_VALUE_PREFIX
    - ENUM_ZERO_VALUE_SUFFIX
    - PACKAGE_VERSION_SUFFIX

  # Customize specific rules
  enum_zero_value_suffix:
    suffix: _UNKNOWN

  # Ignore specific files
  ignore:
    - proto/legacy
    - proto/third_party

  # Ignore specific rules in specific files
  ignore_only:
    FIELD_LOWER_SNAKE_CASE:
      - proto/legacy/old_api.proto
```

---

## Breaking Change Detection

### Running Breaking Check

```bash
# Against git branch
buf breaking --against '.git#branch=main'

# Against git tag
buf breaking --against '.git#tag=v1.0.0'

# Against BSR
buf breaking --against 'buf.build/org/repo'

# Against local directory
buf breaking --against '../old-version/proto'
```

### Breaking Change Categories

#### FILE Level (Default)

Checks breaking changes at file level:

| Rule | Description | Severity |
|------|-------------|----------|
| `ENUM_NO_DELETE` | Enum must not be deleted | ERROR |
| `ENUM_VALUE_NO_DELETE` | Enum value must not be deleted | ERROR |
| `ENUM_VALUE_NO_DELETE_UNLESS_NAME_RESERVED` | Enum value deletion requires reservation | ERROR |
| `ENUM_VALUE_NO_DELETE_UNLESS_NUMBER_RESERVED` | Same for number reservation | ERROR |
| `ENUM_VALUE_SAME_NAME` | Enum value name must not change | ERROR |
| `FIELD_NO_DELETE` | Field must not be deleted | ERROR |
| `FIELD_NO_DELETE_UNLESS_NAME_RESERVED` | Field deletion requires name reservation | ERROR |
| `FIELD_NO_DELETE_UNLESS_NUMBER_RESERVED` | Field deletion requires number reservation | ERROR |
| `FIELD_SAME_CARDINALITY` | Cardinality must not change | ERROR |
| `FIELD_SAME_CPP_STRING_TYPE` | C++ string type must not change | ERROR |
| `FIELD_SAME_JAVA_UTF8_VALIDATION` | Java UTF8 validation must not change | ERROR |
| `FIELD_SAME_JSON_NAME` | JSON name must not change | ERROR |
| `FIELD_SAME_JSTYPE` | JS type must not change | ERROR |
| `FIELD_SAME_NAME` | Field name must not change | ERROR |
| `FIELD_SAME_ONEOF` | Field oneof must not change | ERROR |
| `FIELD_SAME_TYPE` | Field type must not change | ERROR |
| `FILE_NO_DELETE` | File must not be deleted | ERROR |
| `FILE_SAME_PACKAGE` | Package must not change | ERROR |
| `FILE_SAME_SYNTAX` | Syntax must not change | ERROR |
| `MESSAGE_NO_DELETE` | Message must not be deleted | ERROR |
| `MESSAGE_NO_REMOVE_STANDARD_DESCRIPTOR_ACCESSOR` | Standard accessors must remain | ERROR |
| `MESSAGE_SAME_JSON_FORMAT` | JSON format must not change | ERROR |
| `MESSAGE_SAME_MESSAGE_SET_WIRE_FORMAT` | Wire format must not change | ERROR |
| `MESSAGE_SAME_REQUIRED_FIELDS` | Required fields must not change | ERROR |
| `ONEOF_NO_DELETE` | Oneof must not be deleted | ERROR |
| `PACKAGE_ENUM_NO_DELETE` | Package enum must not be deleted | ERROR |
| `PACKAGE_MESSAGE_NO_DELETE` | Package message must not be deleted | ERROR |
| `PACKAGE_NO_DELETE` | Package must not be deleted | ERROR |
| `PACKAGE_SERVICE_NO_DELETE` | Package service must not be deleted | ERROR |
| `RESERVED_ENUM_NO_DELETE` | Reserved enum must not be deleted | ERROR |
| `RESERVED_MESSAGE_NO_DELETE` | Reserved message must not be deleted | ERROR |
| `RPC_NO_DELETE` | RPC must not be deleted | ERROR |
| `RPC_SAME_CLIENT_STREAMING` | Client streaming must not change | ERROR |
| `RPC_SAME_IDEMPOTENCY_LEVEL` | Idempotency level must not change | ERROR |
| `RPC_SAME_REQUEST_TYPE` | Request type must not change | ERROR |
| `RPC_SAME_RESPONSE_TYPE` | Response type must not change | ERROR |
| `RPC_SAME_SERVER_STREAMING` | Server streaming must not change | ERROR |
| `SERVICE_NO_DELETE` | Service must not be deleted | ERROR |

#### PACKAGE Level

More relaxed, allows moving types between files within same package.

#### WIRE Level

Only checks wire compatibility (allows renaming).

#### WIRE_JSON Level

Checks both wire and JSON compatibility.

### Configuration

```yaml
# buf.yaml
breaking:
  use:
    - FILE
  except:
    # Allow changing JSON names (e.g., migrating from camelCase)
    - FIELD_SAME_JSON_NAME
  ignore:
    - proto/internal
    - proto/experimental
```

### Output Formats

```bash
# Human-readable (default)
buf breaking --against '.git#branch=main'
# proto/order.proto:15:3: Field "2" on message "Order" changed type from "int32" to "int64".

# JSON (for CI parsing)
buf breaking --against '.git#branch=main' --error-format=json

# Exit codes
# 0 = no breaking changes
# 1 = breaking changes found
# other = error
```

---

## Code Generation

### Basic Generation

```bash
# Generate using buf.gen.yaml
buf generate

# Generate specific protos
buf generate proto/v3

# Generate to specific output
buf generate --output gen/
```

### Plugin Configuration

```yaml
# buf.gen.yaml
version: v1

managed:
  enabled: true
  # Automatically set java_package based on proto package
  java_package_prefix: com.example
  # Set go_package
  go_package_prefix:
    default: github.com/example/gen/go
    except:
      - buf.build/googleapis/googleapis

plugins:
  # Official protoc plugins
  - plugin: java
    out: gen/java
    opt:
      - lite  # Use lite runtime

  - plugin: go
    out: gen/go
    opt: paths=source_relative

  # gRPC plugins
  - plugin: grpc-java
    out: gen/java

  - plugin: go-grpc
    out: gen/go
    opt: paths=source_relative

  # Remote plugins from BSR
  - plugin: buf.build/grpc/java
    out: gen/java

  # Local plugin binary
  - plugin: protoc-gen-custom
    out: gen/custom
    path: ./bin/protoc-gen-custom
```

### Remote Plugins

buf supports remote plugin execution from BSR:

```yaml
plugins:
  - plugin: buf.build/protocolbuffers/java
    out: gen/java

  - plugin: buf.build/grpc/java
    out: gen/java

  - plugin: buf.build/connectrpc/go
    out: gen/go
```

---

## Buf Schema Registry (BSR)

BSR is a centralized registry for protobuf schemas (like npm for proto).

### Publishing

```bash
# Login
buf registry login

# Push to BSR
buf push
# buf.build/yourorg/yourproject:abc123def456

# Push with tag
buf push --tag v1.0.0
```

### Using Dependencies

```yaml
# buf.yaml
deps:
  - buf.build/googleapis/googleapis
  - buf.build/grpc-ecosystem/grpc-gateway
  - buf.build/envoyproxy/protoc-gen-validate
```

```bash
# Update dependencies
buf mod update

# Export dependencies to local directory
buf export buf.build/googleapis/googleapis -o ./vendor/googleapis
```

### Importing from BSR

```protobuf
// In your proto file
import "google/api/annotations.proto";
import "google/protobuf/timestamp.proto";
import "validate/validate.proto";
```

### Private Registries

```bash
# Configure private registry
buf registry login private.buf.build

# Use in buf.yaml
deps:
  - private.buf.build/company/internal-protos
```

---

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/proto.yml
name: Proto CI

on:
  push:
    branches: [main]
    paths: ['proto/**', 'buf.yaml', 'buf.gen.yaml']
  pull_request:
    branches: [main]
    paths: ['proto/**', 'buf.yaml', 'buf.gen.yaml']

jobs:
  buf:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: bufbuild/buf-setup-action@v1
        with:
          version: latest

      # Lint
      - name: Lint protos
        uses: bufbuild/buf-lint-action@v1

      # Breaking change detection
      - name: Check breaking changes
        uses: bufbuild/buf-breaking-action@v1
        with:
          against: 'https://github.com/${{ github.repository }}.git#branch=main'

      # Generate code
      - name: Generate code
        run: buf generate

      # Verify generated code compiles
      - name: Build generated Java
        run: mvn compile -f gen/java/pom.xml
```

### GitLab CI

```yaml
# .gitlab-ci.yml
stages:
  - lint
  - breaking
  - generate

buf-lint:
  stage: lint
  image: bufbuild/buf:latest
  script:
    - buf lint

buf-breaking:
  stage: breaking
  image: bufbuild/buf:latest
  script:
    - buf breaking --against "${CI_REPOSITORY_URL}#branch=main"
  rules:
    - if: $CI_MERGE_REQUEST_ID

buf-generate:
  stage: generate
  image: bufbuild/buf:latest
  script:
    - buf generate
  artifacts:
    paths:
      - gen/
```

### Pre-commit Hook

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/bufbuild/buf
    rev: v1.28.0
    hooks:
      - id: buf-lint
      - id: buf-breaking
        args: ['--against', '.git#branch=main']
```

```bash
# Install pre-commit
pip install pre-commit
pre-commit install
```

---

## Migration from protoc

### Step 1: Initialize buf

```bash
cd your-project
buf mod init
```

### Step 2: Configure buf.yaml

```yaml
version: v1
dirs:
  - proto
lint:
  use:
    - DEFAULT
  except:
    # Start permissive, tighten later
    - ENUM_VALUE_PREFIX
    - ENUM_ZERO_VALUE_SUFFIX
    - COMMENT_FIELD
    - COMMENT_MESSAGE
breaking:
  use:
    - FILE
```

### Step 3: Create buf.gen.yaml

Convert your protoc commands:

```bash
# Before (protoc)
protoc \
  --java_out=gen/java \
  --grpc-java_out=gen/java \
  -I proto \
  proto/**/*.proto

# After (buf.gen.yaml)
```

```yaml
version: v1
plugins:
  - plugin: java
    out: gen/java
  - plugin: grpc-java
    out: gen/java
```

### Step 4: Fix Lint Issues

```bash
# See all issues
buf lint

# Fix iteratively
# 1. Rename files to match package
# 2. Fix naming conventions
# 3. Add missing documentation
```

### Step 5: Integrate in CI

Replace protoc calls with buf commands in CI pipeline.

---

## proto-wrapper Integration

### Current Status (v1.x)

proto-wrapper uses its own protoc-based compilation. buf can be used alongside for linting and breaking detection.

### Planned Integration (v2.2.0)

#### buf Plugin

```yaml
# buf.gen.yaml
plugins:
  - plugin: buf.build/alnovis/proto-wrapper
    out: gen/java
    opt:
      - basePackage=com.example.model
      - versions=v1,v2,v3
      - generateBuilders=true
```

#### BSR as Schema Source

```kotlin
// build.gradle.kts
protoWrapper {
    bsr {
        modules = listOf(
            "buf.build/yourorg/api:v1",
            "buf.build/yourorg/api:v2",
            "buf.build/yourorg/api:v3"
        )
    }
}
```

#### Compatible Breaking Detection

proto-wrapper's `diff` command will align with buf's output format:

```bash
# buf-compatible output
proto-wrapper diff --buf-compatible proto/v1 proto/v2

# Output matches buf breaking format
proto/v2/order.proto:15:3: Field "2" on message "Order" changed type from "int32" to "int64".
```

### Using buf with proto-wrapper Today

```bash
# 1. Lint your protos with buf
buf lint proto/v1
buf lint proto/v2
buf lint proto/v3

# 2. Check breaking changes between versions
buf breaking proto/v2 --against proto/v1
buf breaking proto/v3 --against proto/v2

# 3. Generate wrappers with proto-wrapper
mvn proto-wrapper:generate

# 4. Use proto-wrapper diff for wrapper-aware analysis
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v3
```

---

## Best Practices

### 1. Version Your Protos

```
proto/
├── v1/           # Stable, production
│   └── api.proto
├── v2/           # Current development
│   └── api.proto
└── v3alpha/      # Experimental
    └── api.proto
```

### 2. Use Package Versioning

```protobuf
// proto/v2/order.proto
package example.order.v2;
```

### 3. Reserve Deleted Fields

```protobuf
message Order {
  reserved 2, 15, 9 to 11;
  reserved "old_field", "deprecated_field";

  string id = 1;
  // field 2 was old_field (removed in v2)
  string name = 3;
}
```

### 4. Start Permissive, Tighten Later

```yaml
# Initial buf.yaml
lint:
  use:
    - MINIMAL

# Later, enable more rules
lint:
  use:
    - DEFAULT
```

### 5. Document Breaking Changes

```protobuf
// Order represents a customer order.
//
// Breaking changes:
// - v2: Removed `legacy_id` field (use `id` instead)
// - v2: Changed `total` from int32 to int64
message Order {
  // ...
}
```

---

## Resources

- [buf.build Documentation](https://buf.build/docs/)
- [buf CLI Reference](https://buf.build/docs/reference/cli/buf)
- [BSR (Buf Schema Registry)](https://buf.build/docs/bsr/introduction)
- [buf GitHub](https://github.com/bufbuild/buf)
- [Protobuf Style Guide](https://buf.build/docs/best-practices/style-guide)
- [Breaking Change Rules](https://buf.build/docs/breaking/rules)
- [Lint Rules](https://buf.build/docs/lint/rules)

---

## See Also

- [ROADMAP.md](ROADMAP.md) - Version 2.2.0 buf.build integration plans
- [SCHEMA_DIFF.md](SCHEMA_DIFF.md) - proto-wrapper's schema diff tool
- [CONFIGURATION.md](CONFIGURATION.md) - Plugin configuration options
