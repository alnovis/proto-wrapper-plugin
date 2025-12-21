# Refactoring Plan: Proto-Wrapper Plugin

## Overview

This document outlines a comprehensive refactoring plan for the proto-wrapper-maven-plugin codebase, focusing on:
- Eliminating code duplication
- Adopting functional programming patterns (Java 17+)
- Improving maintainability and extensibility
- Modernizing the codebase with current Java best practices

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Critical Duplications](#2-critical-duplications)
3. [Functional Programming Opportunities](#3-functional-programming-opportunities)
4. [Java 17+ Modernization](#4-java-17-modernization)
5. [Architectural Improvements](#5-architectural-improvements)
6. [Implementation Phases](#6-implementation-phases)
7. [Risk Assessment](#7-risk-assessment)

---

## 1. Executive Summary

### Current State
- **Total LOC**: ~9,000 lines in proto-wrapper-core
- **Largest files**: ImplClassGenerator (1803), AbstractClassGenerator (1131), InterfaceGenerator (931)
- **Duplication ratio**: Estimated 25-30% of code is duplicated or near-duplicated

### Target State
- Reduce duplication by 40-50%
- Improve code maintainability score
- Full Java 17+ compatibility with modern idioms
- Better separation of concerns

---

## 2. Critical Duplications

### 2.1 Field Processing Loop (HIGH PRIORITY)

**Location**: Multiple generators

The same field iteration pattern with conflict type checking appears 6+ times:

```java
// Current pattern (repeated in 6 places)
for (MergedField field : message.getFieldsSorted()) {
    if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
        // handle INT_ENUM
    } else if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
        // handle STRING_BYTES
    } else if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
        // handle PRIMITIVE_MESSAGE
    } else if (field.isRepeated() && field.hasTypeConflict()) {
        // handle repeated conflicts
    } else if (hasIncompatibleTypeConflict(field, version)) {
        // handle incompatible
    } else {
        // normal handling
    }
}
```

**Files affected**:
- `ImplClassGenerator.java`: lines 102-121, 238-257, 1012-1057, 1148-1193
- `AbstractClassGenerator.java`: lines 128-135, 196-203, 254-321, 331-422
- `InterfaceGenerator.java`: lines 71-111

**Proposed Solution**: Strategy Pattern with Field Processors

```java
// New FieldProcessor interface
@FunctionalInterface
public interface FieldProcessor {
    void process(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);
}

// Conflict-specific processors
public sealed interface ConflictHandler permits
    IntEnumHandler, StringBytesHandler, WideningHandler,
    PrimitiveMessageHandler, RepeatedConflictHandler, DefaultHandler {

    boolean handles(MergedField field);
    void processExtract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);
    void processBuilder(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);
}

// Unified processing
public class FieldProcessingChain {
    private final List<ConflictHandler> handlers;

    public void processFields(TypeSpec.Builder builder,
                              List<MergedField> fields,
                              ProcessingContext ctx) {
        fields.forEach(field ->
            handlers.stream()
                .filter(h -> h.handles(field))
                .findFirst()
                .ifPresent(h -> h.processExtract(builder, field, ctx)));
    }
}
```

**Estimated reduction**: ~400 lines

---

### 2.2 Builder Generation Duplication (HIGH PRIORITY)

**Location**: `ImplClassGenerator.java`

Two nearly identical methods for top-level and nested builders:

| Method | Lines | Purpose |
|--------|-------|---------|
| `generateBuilderImplClass()` | 1124-1223 | Top-level builder |
| `generateNestedBuilderImplClass()` | 992-1085 | Nested builder |

**Similarity**: 90%+ identical code

**Proposed Solution**: Unified Builder Generator

```java
// Extract common builder generation
private TypeSpec generateBuilderImpl(BuilderContext ctx) {
    return TypeSpec.classBuilder("BuilderImpl")
        .addModifiers(ctx.getModifiers())
        .superclass(ctx.getSuperType())
        .addField(ctx.getProtoBuilderType(), "protoBuilder", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(generateConstructor(ctx))
        .addMethods(generateFieldMethods(ctx))
        .addMethod(generateDoBuild(ctx))
        .addMethod(generateExtractProto())
        .build();
}

// Context carries the differences
record BuilderContext(
    MergedMessage message,
    ClassName protoType,
    ClassName interfaceType,
    String implClassName,
    GenerationContext genCtx,
    boolean isNested
) {
    Modifier[] getModifiers() {
        return isNested
            ? new Modifier[]{Modifier.PRIVATE, Modifier.STATIC}
            : new Modifier[]{Modifier.PRIVATE, Modifier.STATIC};
    }
}
```

**Estimated reduction**: ~100 lines

---

### 2.3 AbstractBuilder Generation (MEDIUM PRIORITY)

**Location**: `AbstractClassGenerator.java`

Similar duplication between:
- `generateAbstractBuilder()` (lines 500-600)
- `generateNestedAbstractBuilder()` (lines 243-434)

**Proposed Solution**: Same as 2.2 - unified with context object

---

### 2.4 Type Normalization (MEDIUM PRIORITY)

**Location**: Multiple files

Type normalization logic duplicated:

```java
// ImplClassGenerator.java
private String normalizeType(String type) {
    return switch (type) {
        case "Integer" -> "int";
        case "Long" -> "long";
        case "Double" -> "double";
        case "Float" -> "float";
        // ...
    };
}

// Similar in VersionMerger, TypeResolver
```

**Proposed Solution**: Centralize in `JavaTypeMapping` or new `TypeNormalizer`

```java
public final class TypeNormalizer {
    private static final Map<String, String> WRAPPER_TO_PRIMITIVE = Map.of(
        "Integer", "int",
        "Long", "long",
        "Double", "double",
        "Float", "float",
        "Boolean", "boolean"
    );

    public static String toPrimitive(String type) {
        return WRAPPER_TO_PRIMITIVE.getOrDefault(type, type);
    }

    public static String toWrapper(String type) {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(type, type);
    }

    public static boolean isWidening(String from, String to) {
        return WIDENING_CONVERSIONS.contains(from + "->" + to);
    }
}
```

**Estimated reduction**: ~60 lines

---

### 2.5 Method Name Generation (LOW PRIORITY)

**Location**: Throughout generators

Repeated patterns:

```java
"extract" + resolver.capitalize(field.getJavaName())
"extract" + resolver.capitalize(field.getJavaName()) + "Enum"
"extract" + resolver.capitalize(field.getJavaName()) + "Bytes"
"extract" + resolver.capitalize(field.getJavaName()) + "Message"
"doSet" + resolver.capitalize(field.getJavaName())
"doClear" + resolver.capitalize(field.getJavaName())
```

**Proposed Solution**: Centralize in `MergedField`

```java
public class MergedField {
    // Add method name generators
    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }
    public String getExtractEnumMethodName() {
        return getExtractMethodName() + "Enum";
    }
    public String getExtractBytesMethodName() {
        return getExtractMethodName() + "Bytes";
    }
    public String getDoSetMethodName() {
        return "doSet" + capitalize(javaName);
    }
    public String getDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }
}
```

---

## 3. Functional Programming Opportunities

### 3.1 Replace For-Loops with Streams

**Current**:
```java
for (MergedField field : message.getFieldsSorted()) {
    MethodSpec getter = generateGetter(field, message, resolver);
    interfaceBuilder.addMethod(getter);

    if (field.isOptional() && !field.isRepeated()) {
        interfaceBuilder.addMethod(generateHasMethod(field, resolver));
    }
}
```

**Refactored**:
```java
message.getFieldsSorted().stream()
    .flatMap(field -> Stream.concat(
        Stream.of(generateGetter(field, message, resolver)),
        field.isOptional() && !field.isRepeated()
            ? Stream.of(generateHasMethod(field, resolver))
            : Stream.empty()
    ))
    .forEach(interfaceBuilder::addMethod);
```

**Or with helper method**:
```java
message.getFieldsSorted().stream()
    .flatMap(field -> generateFieldMethods(field, message, resolver))
    .forEach(interfaceBuilder::addMethod);

private Stream<MethodSpec> generateFieldMethods(MergedField field,
                                                 MergedMessage message,
                                                 TypeResolver resolver) {
    var methods = new ArrayList<MethodSpec>();
    methods.add(generateGetter(field, message, resolver));
    if (field.isOptional() && !field.isRepeated()) {
        methods.add(generateHasMethod(field, resolver));
    }
    // ... more methods based on conflict type
    return methods.stream();
}
```

### 3.2 Optional Chains

**Current**:
```java
FieldInfo versionField = field.getVersionFields().get(version);
if (versionField != null && versionField.isEnum()) {
    // handle enum case
}
```

**Refactored**:
```java
field.getVersionField(version)
    .filter(FieldInfo::isEnum)
    .ifPresent(vf -> {
        // handle enum case
    });
```

### 3.3 Pattern Matching for Conflict Types

**Current**:
```java
if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
    addIntEnumExtract(...);
} else if (field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
    addStringBytesExtract(...);
} else if (field.getConflictType() == MergedField.ConflictType.WIDENING) {
    addWideningExtract(...);
} // ... more cases
```

**Refactored (Java 21+ preview, or Java 17 switch)**:
```java
switch (field.getConflictType()) {
    case INT_ENUM -> addIntEnumExtract(builder, field, ctx);
    case STRING_BYTES -> addStringBytesExtract(builder, field, ctx);
    case WIDENING -> addWideningExtract(builder, field, ctx);
    case PRIMITIVE_MESSAGE -> addPrimitiveMessageExtract(builder, field, ctx);
    case NONE, NARROWING, INCOMPATIBLE -> addDefaultExtract(builder, field, ctx);
}
```

### 3.4 Collector Patterns

**Current**:
```java
List<String> typeInfo = new java.util.ArrayList<>();
for (Map.Entry<String, FieldInfo> entry : versionFields.entrySet()) {
    typeInfo.add(entry.getKey() + "=" + entry.getValue().getJavaType());
}
String result = String.join(", ", typeInfo);
```

**Refactored**:
```java
String result = versionFields.entrySet().stream()
    .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
    .collect(Collectors.joining(", "));
```

---

## 4. Java 17+ Modernization

### 4.1 Records

**Candidates for conversion to records**:

```java
// Current (in VersionMerger)
private static class FieldWithVersion {
    private final FieldInfo field;
    private final String version;
    // constructor, getters...
}

// As record
private record FieldWithVersion(FieldInfo field, String version) {}
```

**Additional record candidates**:
- `GenerationContext` (partially - it has mutable state)
- `BuilderContext` (new)
- `ProcessingContext` (new)
- `ConflictInfo` (new utility class)

### 4.2 Sealed Classes

**For type hierarchies**:

```java
// Conflict handlers as sealed hierarchy
public sealed interface ConflictHandler
    permits IntEnumHandler, StringBytesHandler, WideningHandler,
            PrimitiveMessageHandler, RepeatedConflictHandler, DefaultHandler {
    boolean handles(MergedField field);
    void process(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);
}

public final class IntEnumHandler implements ConflictHandler { ... }
public final class StringBytesHandler implements ConflictHandler { ... }
// etc.
```

### 4.3 Text Blocks

**For Javadoc templates**:

```java
// Current
builder.addJavadoc("Abstract base class for $L implementations.\n\n", interfaceName)
       .addJavadoc("<p>Uses template method pattern - subclasses implement extract* methods.</p>\n")
       .addJavadoc("@param <PROTO> Protocol-specific message type\n");

// With text block
builder.addJavadoc("""
    Abstract base class for $L implementations.

    <p>Uses template method pattern - subclasses implement extract* methods.</p>
    @param <PROTO> Protocol-specific message type
    """, interfaceName);
```

### 4.4 Stream.toList()

**Replace everywhere**:

```java
// Current
.collect(Collectors.toList())

// Java 16+
.toList()
```

**Note**: `toList()` returns unmodifiable list. Use `collect(Collectors.toCollection(ArrayList::new))` if mutability needed.

### 4.5 Pattern Matching for instanceof

**Current**:
```java
if (returnType instanceof ParameterizedTypeName) {
    ParameterizedTypeName paramType = (ParameterizedTypeName) returnType;
    return paramType.typeArguments.get(0);
}
```

**Refactored**:
```java
if (returnType instanceof ParameterizedTypeName paramType) {
    return paramType.typeArguments.get(0);
}
```

---

## 5. Architectural Improvements

### 5.1 Strategy Pattern for Field Processing

Extract conflict handling into pluggable strategies:

```
┌─────────────────┐
│ FieldProcessor  │<──────────────────────────┐
│   (interface)   │                           │
└────────┬────────┘                           │
         │                                    │
         ▼                                    │
┌─────────────────┐  ┌─────────────────┐      │
│ IntEnumProcessor│  │ WideningProcessor│      │
└─────────────────┘  └─────────────────┘      │
                                              │
┌─────────────────┐  ┌─────────────────┐      │
│StringBytesProc. │  │RepeatedConflict │──────┘
└─────────────────┘  └─────────────────┘
```

### 5.2 Visitor Pattern for Message Traversal

```java
public interface MessageVisitor {
    void visitMessage(MergedMessage message);
    void visitNestedMessage(MergedMessage parent, MergedMessage nested);
    void visitField(MergedMessage message, MergedField field);
    void visitNestedEnum(MergedMessage message, MergedEnum nestedEnum);
}

public class MessageTraverser {
    public void traverse(MergedMessage message, MessageVisitor visitor) {
        visitor.visitMessage(message);
        message.getFieldsSorted().forEach(f -> visitor.visitField(message, f));
        message.getNestedEnums().forEach(e -> visitor.visitNestedEnum(message, e));
        message.getNestedMessages().forEach(nested -> {
            visitor.visitNestedMessage(message, nested);
            traverse(nested, visitor);
        });
    }
}
```

### 5.3 Builder Fluent API Enhancement

```java
// Enhanced method generation
MethodBuilder.create("extractXxx")
    .withAnnotation(Override.class)
    .withModifiers(PROTECTED)
    .returning(returnType)
    .withParameter(protoType, "proto")
    .withStatement("return proto.get$L()", fieldName)
    .addTo(classBuilder);
```

### 5.4 Configuration as Immutable

Convert `GeneratorConfig` to immutable with builder:

```java
public record GeneratorConfig(
    Path outputDirectory,
    String apiPackage,
    String protoPackagePattern,
    boolean generateBuilders,
    int protobufMajorVersion
) {
    public static Builder builder() { return new Builder(); }

    public String getImplPackage(String version) {
        return apiPackage + "." + version.toLowerCase();
    }
}
```

---

## 6. Implementation Phases

### Phase 1: Foundation (LOW RISK)
**Duration**: 1-2 days

1. Extract `TypeNormalizer` utility class
2. Add method name generators to `MergedField`
3. Replace `Collectors.toList()` with `toList()` where applicable
4. Convert `FieldWithVersion` to record
5. Apply pattern matching for instanceof

**Files changed**: 5-7
**Tests**: Run existing, no new needed

### Phase 2: Stream Conversion (LOW RISK)
**Duration**: 1-2 days

1. Convert simple for-loops to streams in all generators
2. Replace null checks with Optional chains
3. Use Collectors.joining() for string building
4. Apply text blocks for Javadoc

**Files changed**: 4-5
**Tests**: Run existing

### Phase 3: Strategy Pattern (MEDIUM RISK)
**Duration**: 2-3 days

1. Create `ConflictHandler` sealed interface
2. Implement handlers: IntEnumHandler, StringBytesHandler, WideningHandler, etc.
3. Create `FieldProcessingChain`
4. Refactor generators to use chain
5. Update tests

**Files changed**: 8-10
**New files**: 6-8 (handlers + chain)
**Tests**: New unit tests for handlers

### Phase 4: Builder Unification (MEDIUM RISK)
**Duration**: 2 days

1. Create `BuilderContext` record
2. Unify `generateBuilderImplClass` and `generateNestedBuilderImplClass`
3. Unify abstract builder generation
4. Apply same pattern to AbstractClassGenerator

**Files changed**: 2
**Tests**: Run existing integration tests

### Phase 5: Visitor Pattern (LOW PRIORITY)
**Duration**: 1-2 days

1. Create `MessageVisitor` interface
2. Create `MessageTraverser`
3. Optionally convert generators to use visitor

**Files changed**: 3-4
**New files**: 2

---

## 7. Risk Assessment

### Low Risk
- Type normalizer extraction
- Stream conversions
- Record conversions
- Pattern matching updates

### Medium Risk
- Strategy pattern introduction
- Builder unification

### High Risk
- None identified (all changes preserve behavior)

### Mitigation Strategies

1. **Comprehensive test coverage**: Run all 70+ existing tests after each phase
2. **Incremental changes**: Small commits, easy rollback
3. **Generate & compare**: Generate code before/after, diff to verify identical output
4. **Feature flags**: Optional - could add config to use old vs new code path

---

## Appendix A: File Impact Summary

| File | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| ImplClassGenerator.java | ✓ | ✓ | ✓ | ✓ |
| AbstractClassGenerator.java | ✓ | ✓ | ✓ | ✓ |
| InterfaceGenerator.java | ✓ | ✓ | ✓ | |
| VersionMerger.java | ✓ | ✓ | | |
| TypeResolver.java | ✓ | | | |
| MergedField.java | ✓ | | | |
| JavaTypeMapping.java | ✓ | | | |

## Appendix B: New Files (Proposed)

```
generator/
├── conflict/
│   ├── ConflictHandler.java          (sealed interface)
│   ├── IntEnumHandler.java
│   ├── StringBytesHandler.java
│   ├── WideningHandler.java
│   ├── PrimitiveMessageHandler.java
│   ├── RepeatedConflictHandler.java
│   ├── DefaultHandler.java
│   └── FieldProcessingChain.java
├── context/
│   ├── BuilderContext.java           (record)
│   └── ProcessingContext.java        (record)
└── util/
    └── TypeNormalizer.java
```

## Appendix C: Metrics (Expected)

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Total LOC | ~9,000 | ~6,500 | -28% |
| ImplClassGenerator | 1,803 | ~1,200 | -33% |
| AbstractClassGenerator | 1,131 | ~800 | -29% |
| InterfaceGenerator | 931 | ~700 | -25% |
| Cyclomatic Complexity | High | Medium | Improved |
| Duplication | 25-30% | <10% | Significant |
