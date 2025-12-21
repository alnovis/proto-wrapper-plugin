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
| Phase 6 | Optional Chain Optimization - ifPresent(), map(), orElse() | ✅ Complete | ~40 |
| Phase 7 | Switch Expression Conversion - Java 17+ switch with arrow syntax | ✅ Complete | ~25 |
| Phase 8 | Conditional Statement Helper - addVersionConditional() helpers | ✅ Complete | ~60 |
| Phase 9 | Field-Type Dispatch Extraction - FieldTypeDispatcher record | ✅ Complete | ~45 |
| Phase 10 | Sealed Interface - ConflictHandler, AbstractConflictHandler | ✅ Complete | Type safety |
| Phase 11 | Stream Parent Hierarchy - Stream.iterate() with Collectors | ✅ Complete | Functional style |

### Files Created During Refactoring

```
generator/
├── conflict/
│   ├── ConflictHandler.java          (sealed interface)
│   ├── AbstractConflictHandler.java  (sealed base class)
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

## Refactoring Complete

All planned refactoring phases have been successfully completed. The codebase now fully adopts Java 17+ features and modern functional programming patterns.

---

## Metrics

### Final State (After All Phases)

| File | Original LOC | Current LOC | Reduction |
|------|-------------|-------------|-----------|
| ImplClassGenerator.java | 1,803 | ~1,195 | -34% |
| AbstractClassGenerator.java | 1,131 | ~750 | -34% |
| InterfaceGenerator.java | 931 | ~850 | -9% |
| IntEnumHandler.java | N/A | ~280 | Optimized |
| DefaultHandler.java | N/A | ~250 | Optimized |
| CodeGenerationHelper.java | N/A | ~485 | Optimized |
| **Total proto-wrapper-core** | ~9,000 | ~7,115 | -21% |

### Code Quality Metrics

| Metric | Before Phase 9 | After Phase 9 |
|--------|----------------|---------------|
| Duplication | ~10% | ~8% |
| If-Else Chains (isMessage/isEnum) | 4+ | 0 (in DefaultHandler) |

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
| Records | ProcessingContext, BuilderImplContext, FieldTypeDispatcher | 6+ |
| Switch Expressions | Conflict type handling | Handlers |
| Pattern Matching instanceof | Type checks | AbstractConflictHandler |
| Stream.toList() | Throughout | All generators |
| Stream.iterate() | Parent hierarchy traversal | ImplClassGenerator |
| Collectors.collectingAndThen() | Stream post-processing | ImplClassGenerator |
| Text Blocks | Javadoc (partial) | Some files |
| Functional Interface | FieldTypeAction | CodeGenerationHelper |
| Sealed Classes | ConflictHandler, AbstractConflictHandler | conflict/ package |

---

## Appendix: Code Quality Checklist

Before marking any phase complete:

- [ ] All 106 tests pass
- [ ] No new warnings introduced
- [ ] Code follows existing style conventions
- [ ] Javadoc updated if public API changed
- [ ] Git commit with descriptive message
