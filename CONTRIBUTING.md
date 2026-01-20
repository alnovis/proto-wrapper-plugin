# Contributing to Proto Wrapper Plugin

Thank you for considering contributing to Proto Wrapper Plugin! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Project Structure](#project-structure)

---

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- Gradle 8.5+ (or use included wrapper)
- Protocol Buffers compiler (`protoc`) installed and in PATH

### Building the Project

The project uses a dual-build system: Maven for core and Maven plugin, Gradle for Gradle plugin.

#### Maven Build

```bash
# Build all Maven modules (core + Maven plugin)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl proto-wrapper-core
```

#### Gradle Build

```bash
# Build Gradle modules (core + Gradle plugin)
./gradlew build

# Build without tests
./gradlew build -x test

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

### Running Tests

#### Maven Tests

```bash
# Run all unit tests
mvn test

# Run Maven integration tests
mvn verify -pl proto-wrapper-maven-integration-tests

# Run specific test class
mvn test -Dtest=VersionMergerTest
```

#### Gradle Tests

```bash
# Run unit tests
./gradlew test

# Run Gradle integration tests (requires publishToMavenLocal first)
./gradlew publishToMavenLocal
cd proto-wrapper-gradle-integration-tests
gradle test --no-daemon
```

#### Full Test Suite

```bash
# Run all tests (Maven + Gradle)
mvn clean install
./gradlew build publishToMavenLocal
cd proto-wrapper-gradle-integration-tests && gradle test --no-daemon
```

---

## Code Style

### General Guidelines

1. **Language**: All code, comments, and documentation must be in **English only**
2. **Java Version**: Use Java 17+ features where appropriate (records, sealed classes, pattern matching)
3. **No Emojis**: Do not use emojis in code or documentation
4. **Indentation**: 4 spaces (no tabs)
5. **Line Length**: Maximum 120 characters

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `InterfaceGenerator`, `MergedField` |
| Methods | camelCase | `generateInterface()`, `resolveType()` |
| Constants | UPPER_SNAKE_CASE | `GENERATED_FILE_COMMENT` |
| Packages | lowercase | `io.alnovis.protowrapper.generator` |
| Type Parameters | Single uppercase | `<T>`, `<PROTO>` |

### JavaDoc Requirements

All public classes and methods must have JavaDoc:

```java
/**
 * Generates version-agnostic Java interfaces from merged schema.
 *
 * <p>Example usage:</p>
 * <pre>
 * InterfaceGenerator generator = new InterfaceGenerator(config);
 * JavaFile file = generator.generate(message, ctx);
 * </pre>
 *
 * @see AbstractClassGenerator
 */
public class InterfaceGenerator extends BaseGenerator<MergedMessage> {

    /**
     * Generate interface for a merged message.
     *
     * @param message Merged message info
     * @param ctx Generation context
     * @return Generated JavaFile
     * @throws IllegalArgumentException if message is null
     */
    public JavaFile generate(MergedMessage message, GenerationContext ctx) {
        // ...
    }
}
```

### Design Patterns

The project uses several design patterns. When contributing, follow existing patterns:

| Pattern | Location | When to Use |
|---------|----------|-------------|
| Template Method | `AbstractClassGenerator` | When creating base classes with extension points |
| Chain of Responsibility | `FieldProcessingChain` | For sequential processing with multiple handlers |
| Strategy | `ConflictHandler` | For interchangeable algorithms |
| Builder | `MergedField.Builder` | For complex object construction |
| Sealed Types | `ConflictHandler` | For exhaustive type hierarchies |

---

## Testing Requirements

### Test Coverage

1. **Unit Tests**: Required for all new utility classes and handlers
2. **Integration Tests**: Required for new conflict types or generation features
3. **All Tests Must Pass**: PRs will not be merged if tests fail

### Test Structure

```
proto-wrapper-core/src/test/java/                    # Unit tests
proto-wrapper-maven-integration-tests/src/test/      # Maven integration tests
proto-wrapper-gradle-integration-tests/src/test/     # Gradle integration tests (standalone project)

test-protos/scenarios/                               # Shared test proto files
├── generation/                                      # Proto files for code generation tests
│   ├── v1/
│   └── v2/
└── diff/                                            # Proto files for Schema Diff tests
    ├── 01-simple-add/
    ├── 02-simple-remove/
    ├── 03-field-changes/
    ├── 04-enum-changes/
    ├── 05-complex-changes/
    └── 06-no-changes/
```

Note: Gradle integration tests use a "Publish First, Test Second" pattern. The plugin
must be published to mavenLocal before running the tests.

### Writing Tests

```java
@Test
void shouldDetectIntEnumConflict() {
    // Given
    FieldInfo intField = FieldInfo.builder()
            .protoName("status")
            .javaType("int")
            .build();
    FieldInfo enumField = FieldInfo.builder()
            .protoName("status")
            .javaType("Status")
            .isEnum(true)
            .build();

    // When
    MergedField merged = MergedField.builder()
            .addVersionField("v1", intField)
            .addVersionField("v2", enumField)
            .build();

    // Then
    assertThat(merged.getConflictType()).isEqualTo(ConflictType.INT_ENUM);
}
```

---

## Pull Request Process

### Before Submitting

1. **Create an Issue**: Discuss significant changes before implementing
2. **Branch Naming**: Use descriptive names like `feature/add-map-support` or `fix/enum-conflict-detection`
3. **Commit Messages**: Use conventional commits format:
   - `feat: add OPTIONAL_REQUIRED conflict detection`
   - `fix: correct type resolution for nested messages`
   - `docs: update ARCHITECTURE.md with class diagrams`
   - `refactor: extract InterfaceMethodGenerator`
   - `test: add edge cases for StringBytesHandler`

### PR Checklist

- [ ] Code compiles without warnings
- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] JavaDoc added for public API
- [ ] No deprecated API used (use new `GenerationContext`-based API)
- [ ] Documentation updated if needed

### Review Process

1. Submit PR against `develop` branch
2. Wait for CI checks to pass
3. Address review comments
4. Squash commits if requested
5. Maintainer merges after approval

---

## Project Structure

```
proto-wrapper-plugin/
├── proto-wrapper-core/                    # Core library (Maven + Gradle)
│   └── src/main/java/io/alnovis/protowrapper/
│       ├── analyzer/                      # Proto file parsing
│       ├── diff/                          # Schema Diff functionality (v1.5.0+)
│       ├── exception/                     # Exception hierarchy
│       ├── generator/                     # Code generation
│       │   ├── builder/                   # Builder interface generation
│       │   ├── conflict/                  # Type conflict handling
│       │   ├── oneof/                     # Oneof field support
│       │   └── visitor/                   # Message traversal
│       ├── merger/                        # Version schema merging
│       └── model/                         # Domain model classes
├── proto-wrapper-maven-plugin/            # Maven plugin
├── proto-wrapper-gradle-plugin/           # Gradle plugin (Kotlin)
├── proto-wrapper-maven-integration-tests/ # Maven integration tests
├── proto-wrapper-gradle-integration-tests/# Gradle integration tests (standalone)
├── test-protos/                           # Shared test proto files
│   └── scenarios/
│       ├── generation/                    # Code generation test scenarios
│       └── diff/                          # Schema Diff test scenarios
├── examples/
│   ├── maven-example/                     # Example Maven project
│   └── gradle-example/                    # Example Gradle project
└── docs/                                  # Documentation
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `VersionMerger` | Merges multiple proto versions into unified schema |
| `MergedField` | Represents a field merged from multiple versions |
| `InterfaceGenerator` | Generates version-agnostic interfaces |
| `AbstractClassGenerator` | Generates abstract base classes |
| `ImplClassGenerator` | Generates version-specific implementations |
| `FieldProcessingChain` | Routes fields to appropriate conflict handlers |
| `GenerationContext` | Holds context for code generation |

---

## Questions?

If you have questions, please:
1. Check existing documentation in `docs/`
2. Search existing issues
3. Open a new issue with your question

Thank you for contributing!
