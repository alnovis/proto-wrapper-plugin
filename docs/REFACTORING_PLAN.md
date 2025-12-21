# Refactoring Plan: Proto-Wrapper Plugin

## Overview

This document outlines the refactoring plan for the proto-wrapper-maven-plugin codebase, focusing on:
- Adopting functional programming patterns (Java 17+)
- Eliminating remaining code duplication
- Improving maintainability and extensibility
- Modernizing the codebase with current Java best practices

---

## Completed Phases

The following phases have been successfully completed:

| Phase | Description | Status | Lines Reduced |
|-------|-------------|--------|---------------|
| Phase 1 | Foundation - TypeNormalizer, method name generators, records | ✅ Complete | ~100 |
| Phase 2 | Stream Conversions - for-loops to streams, Collectors.joining() | ✅ Complete | ~80 |
| Phase 3 | Strategy Pattern - ConflictHandler hierarchy, FieldProcessingChain | ✅ Complete | ~400 |
| Phase 4 | Builder Unification - BuilderImplContext, AbstractBuilderContext | ✅ Complete | ~310 |
| Phase 5 | Visitor Pattern - MessageVisitor, MessageTraverser, utilities | ✅ Complete | New utilities |

### Files Created During Refactoring

```
generator/
├── conflict/
│   ├── ConflictHandler.java          (interface)
│   ├── AbstractConflictHandler.java  (base class)
│   ├── IntEnumHandler.java
│   ├── StringBytesHandler.java
│   ├── WideningHandler.java
│   ├── PrimitiveMessageHandler.java
│   ├── RepeatedConflictHandler.java
│   ├── DefaultHandler.java
│   ├── FieldProcessingChain.java
│   ├── CodeGenerationHelper.java
│   ├── ProcessingContext.java        (record)
│   ├── BuilderImplContext.java       (record)
│   └── AbstractBuilderContext.java   (record)
├── visitor/
│   ├── MessageVisitor.java           (interface)
│   ├── MessageTraverser.java
│   ├── SchemaStats.java
│   ├── FieldCollector.java
│   └── ConflictReporter.java
└── TypeNormalizer.java
```

---

## Remaining Refactoring Opportunities

### Phase 6: Optional Chain Optimization (LOW RISK)

Replace remaining null checks with idiomatic Optional patterns.

#### 6.1 ImplClassGenerator.java - getVersionSpecificJavaName()

**Location**: Lines 271-276

**Current**:
```java
Map<String, FieldInfo> versionFields = field.getVersionFields();
FieldInfo versionField = versionFields.get(version);
if (versionField != null) {
    return resolver.capitalize(versionField.getJavaName());
}
return resolver.capitalize(field.getJavaName());
```

**Improvement**:
```java
return Optional.ofNullable(field.getVersionFields().get(version))
    .map(vf -> resolver.capitalize(vf.getJavaName()))
    .orElseGet(() -> resolver.capitalize(field.getJavaName()));
```

#### 6.2 ImplClassGenerator.java - getProtoTypeForField()

**Location**: Lines 839-846

**Current**:
```java
FieldInfo versionField = field.getVersionFields().get(version);
if (versionField == null) {
    return "com.google.protobuf.Message";
}
String typeName = versionField.getTypeName();
```

**Improvement**:
```java
String typeName = Optional.ofNullable(field.getVersionFields().get(version))
    .map(FieldInfo::getTypeName)
    .orElse(null);
if (typeName == null) {
    return "com.google.protobuf.Message";
}
```

#### 6.3 TypeResolver.java - resolveNestedTypePath()

**Location**: Lines 147-154

**Current**:
```java
if (schema != null) {
    Optional<MergedMessage> crossMessage = schema.findMessageByPath(typePath);
    if (crossMessage.isPresent()) {
        return buildNestedClassName(typePath);
    }
    Optional<MergedEnum> crossEnum = schema.findEnumByPath(typePath);
    if (crossEnum.isPresent()) {
        return buildNestedClassName(typePath);
    }
}
```

**Improvement**:
```java
if (schema != null &&
    (schema.findMessageByPath(typePath).isPresent() ||
     schema.findEnumByPath(typePath).isPresent())) {
    return buildNestedClassName(typePath);
}
```

#### 6.4 TypeResolver.java - resolveSimpleTypePath()

**Location**: Lines 164-207

**Current**: Multiple sequential Optional checks with `if (opt.isPresent())` pattern

**Improvement**: Use `Optional.or()` chaining:
```java
return context.findNestedMessageRecursive(typePath)
    .map(m -> buildNestedClassName(m.getQualifiedInterfaceName()))
    .or(() -> context.findNestedEnumRecursive(typePath)
        .map(e -> buildNestedClassName(e.getQualifiedName())))
    .or(() -> schema != null
        ? schema.findMessageByPath(typePath)
            .map(m -> buildNestedClassName(m.getQualifiedInterfaceName()))
        : Optional.empty())
    .orElseGet(() -> ClassName.get(config.getApiPackage(), typePath));
```

**Files affected**: 2
**Estimated effort**: 1-2 hours

---

### Phase 7: Switch Expression Conversion (LOW RISK)

Convert remaining nested if-else chains to Java 17+ switch expressions.

#### 7.1 ImplClassGenerator.java - addWideningBuilderMethods()

**Location**: Lines 555-574

**Current**:
```java
if (needsNarrowing) {
    if ("long".equals(widerType) || "Long".equals(widerType)) {
        // long -> int narrowing with range check
        doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
        // ...
    } else if ("double".equals(widerType) || "Double".equals(widerType)) {
        // double -> float/int narrowing
        // ...
    } else {
        // Unknown narrowing - just cast
        doSet.addStatement("protoBuilder.set$L(($L) $L)",
                versionJavaName, versionType, field.getJavaName());
    }
}
```

**Improvement**:
```java
if (needsNarrowing) {
    switch (widerType) {
        case "long", "Long" -> {
            doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                    field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
            // ...
        }
        case "double", "Double" -> {
            // double -> float/int narrowing
            // ...
        }
        default -> doSet.addStatement("protoBuilder.set$L(($L) $L)",
                versionJavaName, versionType, field.getJavaName());
    }
}
```

#### 7.2 Similar Patterns in Other Files

**Locations**:
- `InterfaceGenerator.java`: Lines 709-745 (type-based dispatch)
- `WideningHandler.java`: Lines 127-150 (narrowing logic)

**Files affected**: 3
**Estimated effort**: 1-2 hours

---

### Phase 8: Conditional Statement Helper (MEDIUM PRIORITY)

Extract duplicated conditional version-check pattern into helper method.

#### 8.1 DefaultHandler.java - Repeated Pattern

**Pattern**: Appears 15+ times across handler files

**Current**:
```java
if (presentInVersion) {
    doAdd.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
} else {
    doAdd.addComment("Field not present in this version - ignored");
}
```

**Proposed Helper**:
```java
// In CodeGenerationHelper.java
public static void addConditionalStatement(MethodSpec.Builder method,
                                            boolean condition,
                                            Consumer<MethodSpec.Builder> ifPresent,
                                            String absentComment) {
    if (condition) {
        ifPresent.accept(method);
    } else {
        method.addComment(absentComment);
    }
}

// Alternative with varargs for statement
public static void addVersionConditionalStatement(MethodSpec.Builder method,
                                                   boolean presentInVersion,
                                                   String format,
                                                   Object... args) {
    if (presentInVersion) {
        method.addStatement(format, args);
    } else {
        method.addComment("Field not present in this version - ignored");
    }
}
```

**Usage**:
```java
addVersionConditionalStatement(doAdd, presentInVersion,
    "protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
```

**Files affected**: DefaultHandler.java, WideningHandler.java, IntEnumHandler.java, StringBytesHandler.java
**Estimated effort**: 2-3 hours
**Reduction**: ~50 lines of duplicated conditionals

---

### Phase 9: Field-Type Dispatch Extraction (MEDIUM PRIORITY)

Extract repeated field-type dispatch logic into reusable component.

#### 9.1 DefaultHandler.java - Repeated Type Dispatch

**Pattern**: The "if isMessage/if isEnum/else" pattern repeats 3-4 times

**Current** (Lines 221-232, 244-256, 268-281):
```java
if (field.isMessage()) {
    String protoTypeName = getProtoTypeForField(field, ctx, null);
    doAdd.addStatement("protoBuilder.add$L(($L) extractProto($L))",
            versionJavaName, protoTypeName, field.getJavaName());
} else if (field.isEnum()) {
    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
    String enumMethod = getEnumFromIntMethod(ctx.config());
    doAdd.addStatement("protoBuilder.add$L($L.$L($L.getProtoValue()))",
            versionJavaName, protoEnumType, enumMethod, field.getJavaName());
} else {
    doAdd.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
}
```

**Proposed Solution**:
```java
// In CodeGenerationHelper.java
@FunctionalInterface
public interface FieldTypeAction {
    void apply(MethodSpec.Builder method, MergedField field,
               String versionJavaName, ProcessingContext ctx);
}

public record FieldTypeDispatcher(
    FieldTypeAction messageAction,
    FieldTypeAction enumAction,
    FieldTypeAction primitiveAction
) {
    public void dispatch(MethodSpec.Builder method, MergedField field,
                         String versionJavaName, ProcessingContext ctx) {
        if (field.isMessage()) {
            messageAction.apply(method, field, versionJavaName, ctx);
        } else if (field.isEnum()) {
            enumAction.apply(method, field, versionJavaName, ctx);
        } else {
            primitiveAction.apply(method, field, versionJavaName, ctx);
        }
    }
}

// Pre-defined dispatchers
public static final FieldTypeDispatcher ADD_DISPATCHER = new FieldTypeDispatcher(
    (m, f, n, c) -> m.addStatement("protoBuilder.add$L(($L) extractProto($L))",
            n, getProtoTypeForField(f, c, null), f.getJavaName()),
    (m, f, n, c) -> m.addStatement("protoBuilder.add$L($L.$L($L.getProtoValue()))",
            n, getProtoEnumTypeForField(f, c, null),
            getEnumFromIntMethod(c.config()), f.getJavaName()),
    (m, f, n, c) -> m.addStatement("protoBuilder.add$L($L)", n, f.getJavaName())
);
```

**Files affected**: DefaultHandler.java, RepeatedConflictHandler.java
**Estimated effort**: 3-4 hours
**Reduction**: ~60 lines

---

### Phase 10: Sealed Interface for ConflictHandler (LOW PRIORITY)

Make ConflictHandler a sealed interface for better type safety.

#### 10.1 ConflictHandler.java

**Current**:
```java
public interface ConflictHandler {
    boolean handles(MergedField field, ProcessingContext ctx);
    // ...
}
```

**Improvement**:
```java
public sealed interface ConflictHandler permits
        IntEnumHandler, StringBytesHandler, WideningHandler,
        PrimitiveMessageHandler, RepeatedConflictHandler, DefaultHandler {

    boolean handles(MergedField field, ProcessingContext ctx);
    // ...
}

public final class IntEnumHandler extends AbstractConflictHandler
        implements ConflictHandler { ... }
// etc.
```

**Benefits**:
- Compiler guarantees exhaustive switch (when pattern matching in future)
- Documents the complete set of implementations
- Prevents accidental external extensions

**Files affected**: 7 handler files
**Estimated effort**: 30 minutes

---

### Phase 11: Stream Optimization - Parent Hierarchy (LOW PRIORITY)

Optimize parent hierarchy collection using functional approach.

#### 11.1 ImplClassGenerator.java - buildNestedAbstractClassName()

**Location**: Lines 256-261

**Current**:
```java
List<String> path = new java.util.ArrayList<>();
for (MergedMessage current = nested; current.getParent() != null; current = current.getParent()) {
    path.add(current.getName());
}
java.util.Collections.reverse(path);
```

**Improvement Option 1** - Stream.iterate():
```java
List<String> path = Stream.iterate(nested, m -> m.getParent() != null, MergedMessage::getParent)
    .map(MergedMessage::getName)
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        list -> { Collections.reverse(list); return list; }
    ));
```

**Improvement Option 2** - LinkedList for natural order:
```java
Deque<String> path = Stream.iterate(nested, m -> m.getParent() != null, MergedMessage::getParent)
    .map(MergedMessage::getName)
    .collect(Collectors.toCollection(ArrayDeque::new));
// Use path.descendingIterator() or convert as needed
```

**Files affected**: 1
**Estimated effort**: 30 minutes

---

## Implementation Priority

### High Priority (Do First)
| Phase | Description | Effort | Impact |
|-------|-------------|--------|--------|
| 8 | Conditional Statement Helper | 2-3 hours | Medium - reduces 50+ lines |
| 6 | Optional Chain Optimization | 1-2 hours | Medium - idiomatic Java |

### Medium Priority (Do When Time Permits)
| Phase | Description | Effort | Impact |
|-------|-------------|--------|--------|
| 9 | Field-Type Dispatch Extraction | 3-4 hours | Medium - reduces 60+ lines |
| 7 | Switch Expression Conversion | 1-2 hours | Low - readability |

### Low Priority (Optional)
| Phase | Description | Effort | Impact |
|-------|-------------|--------|--------|
| 10 | Sealed Interface | 30 minutes | Low - type safety |
| 11 | Stream Parent Hierarchy | 30 minutes | Low - functional style |

---

## Metrics

### Current State (After Phases 1-5)

| File | Original LOC | Current LOC | Reduction |
|------|-------------|-------------|-----------|
| ImplClassGenerator.java | 1,803 | ~1,200 | -33% |
| AbstractClassGenerator.java | 1,131 | ~750 | -34% |
| InterfaceGenerator.java | 931 | ~850 | -9% |
| **Total proto-wrapper-core** | ~9,000 | ~7,200 | -20% |

### Expected After Remaining Phases

| Metric | Current | After Phase 8-9 | Final |
|--------|---------|-----------------|-------|
| Duplication | ~15% | ~8% | <8% |
| Null Checks | ~30 | ~10 | <10 |
| If-Else Chains | ~15 | ~5 | <5 |

---

## Testing Strategy

All refactoring should:
1. Run existing 106 tests after each phase
2. Verify generated code is identical (diff test)
3. No new tests required for style changes

---

## Appendix: Java 17+ Features Used

| Feature | Usage | Files |
|---------|-------|-------|
| Records | ProcessingContext, BuilderImplContext, etc. | 5+ |
| Switch Expressions | Conflict type handling | Handlers |
| Pattern Matching instanceof | Type checks | AbstractConflictHandler |
| Stream.toList() | Throughout | All generators |
| Text Blocks | Javadoc (partial) | Some files |
| Sealed Classes | Proposed for Phase 10 | ConflictHandler |

---

## Appendix: Code Quality Checklist

Before marking any phase complete:

- [ ] All 106 tests pass
- [ ] No new warnings introduced
- [ ] Code follows existing style conventions
- [ ] Javadoc updated if public API changed
- [ ] Git commit with descriptive message
