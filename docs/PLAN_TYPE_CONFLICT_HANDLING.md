# Plan: Type Conflict Handling in Builders

## Status: Phase 1 Complete ✅

**Completed:** 2024-12-21
**Tests:** 140 total (34 new conflict tests)

---

## Problem Statement

When fields have different types across protocol versions, the generated builder setters fail to compile because they try to use the merged type (typically from the first version) for all versions.

### Original Errors (Now Fixed)

```
int cannot be converted to TaxTypeEnum
Integer cannot be converted to ParentReference
String cannot be converted to ByteString
long cannot be converted to Date
possible lossy conversion from long to int
```

## Conflict Types Classification

| Type | Example | Convertible | Status |
|------|---------|-------------|--------|
| `INT_ENUM` | int32 ↔ TaxTypeEnum | Yes (via getValue/forNumber) | ✅ Handled |
| `WIDENING` | int32 → int64, int32 → double | Yes (implicit widening) | ✅ Handled |
| `NARROWING` | int64 → int32 | Partial (lossy) | ✅ Handled |
| `STRING_BYTES` | string ↔ bytes | Yes (via getBytes/new String) | ✅ Handled |
| `PRIMITIVE_MESSAGE` | int32 → ParentReference | No | ✅ Handled |
| `INCOMPATIBLE` | Other incompatible types | No | ✅ Handled |

## Solution: Hybrid Approach

### Phase 1: Skip Conflicting Fields (MVP) ✅ COMPLETE

**Goal:** Builder compiles without errors by skipping problematic setters.

- [x] Detect type conflicts during merge
- [x] Mark fields with conflicts
- [x] Skip builder setter generation for conflicting fields
- [x] Add Javadoc warning to interface
- [x] Treat conflicting fields as "not present" in version implementations
- [x] Unit tests for all conflict types
- [x] Round-trip tests

### Phase 2: Support Convertible Conflicts (Optional Enhancement) 🔮

**Goal:** Support int↔enum conversion with overloaded setters.

- [ ] Generate overloaded setters for INT_ENUM conflicts
- [ ] Implement conversion logic in impl classes
- [ ] Generate type-safe enum setters in V2 implementations

---

## Implementation Plan

### Step 1: Enhance MergedField Model ✅

**File:** `proto-wrapper-core/src/main/java/space/alnovis/protowrapper/model/MergedField.java`

Added:
- `ConflictType` enum with all conflict types
- `typesPerVersion` map (version → javaType)
- `hasTypeConflict()` method
- `getConflictType()` method
- `shouldSkipBuilderSetter()` method
- `getAllTypes()` method

### Step 2: Update VersionMerger ✅

**File:** `proto-wrapper-core/src/main/java/space/alnovis/protowrapper/merger/VersionMerger.java`

Added:
- `detectConflictType()` method with full classification logic
- Helper methods: `isIntType()`, `isLongType()`, `isDoubleType()`
- Conflict logging via `PluginLogger`

### Step 3: Update InterfaceGenerator ✅

**File:** `proto-wrapper-core/src/main/java/space/alnovis/protowrapper/generator/InterfaceGenerator.java`

Added:
- Skip check for `field.shouldSkipBuilderSetter()` in `generateBuilderInterface()`
- Skip check for nested builders in `generateNestedBuilderInterface()`
- Javadoc notes documenting skipped fields with conflict type

### Step 4: Update AbstractClassGenerator ✅

**File:** `proto-wrapper-core/src/main/java/space/alnovis/protowrapper/generator/AbstractClassGenerator.java`

Added:
- Skip abstract `doSet*`/`doClear*` methods for conflicting fields
- Skip concrete implementations in abstract builder

### Step 5: Update ImplClassGenerator ✅

**File:** `proto-wrapper-core/src/main/java/space/alnovis/protowrapper/generator/ImplClassGenerator.java`

Added:
- Skip builder implementations for conflicting fields
- Enhanced `hasIncompatibleTypeConflict()` to detect all type mismatches
- Helper methods: `areEquivalentTypes()`, `normalizeType()`, `isWideningConversion()`
- Conflicting fields treated as "not present" (return default values)

### Step 6: Javadoc Warnings ✅

Generated Javadoc example:
```java
/**
 * Builder for creating and modifying SensorReading instances.
 *
 * <p><b>Note:</b> {@code unitType} setter not available due to type conflict (INT_ENUM).</p>
 * <p><b>Note:</b> {@code precisionLevel} setter not available due to type conflict (WIDENING).</p>
 * <p><b>Note:</b> {@code calibrationId} setter not available due to type conflict (PRIMITIVE_MESSAGE).</p>
 */
interface Builder {
    // Only non-conflicting fields have setters
}
```

---

## Test Cases ✅

### Test Proto Files

- `proto/v1/telemetry.proto` - V1 with primitive types
- `proto/v2/telemetry.proto` - V2 with changed types

### Conflict Coverage

| Field | V1 Type | V2 Type | Conflict Type | Tested |
|-------|---------|---------|---------------|--------|
| `unit_type` | int32 | UnitTypeEnum | INT_ENUM | ✅ |
| `precision_level` | int32 | double | WIDENING | ✅ |
| `calibration_id` | int32 | CalibrationInfo | PRIMITIVE_MESSAGE | ✅ |
| `raw_value` | int32 | int64 | WIDENING | ✅ |
| `severity_code` | int32 | AlertSeverity | INT_ENUM | ✅ |
| `checksum` | string | bytes | STRING_BYTES | ✅ |
| `generated_at` | int64 | Date | PRIMITIVE_MESSAGE | ✅ |
| `sync_status` | int32 | SyncStatus | INT_ENUM | ✅ |

### Test Files

1. **TypeConflictTest.java** (22 tests)
   - INT_ENUM conflicts (unitType, severity, syncStatus)
   - WIDENING conflicts (precisionLevel, rawValue)
   - PRIMITIVE_MESSAGE conflicts (calibrationId, generatedAt)
   - STRING_BYTES conflicts (checksum)

2. **TypeConflictRoundTripTest.java** (12 tests)
   - V1 round-trip with all field types
   - V2 round-trip with enum/message types
   - Cross-version polymorphic usage
   - Builder modifications for both versions

### Test Results ✅

- [x] Build succeeds (no compilation errors)
- [x] 140 tests pass (106 original + 34 new)
- [x] Conflicting fields are read-only in builders
- [x] Javadoc documents which fields have conflicts

---

## Files Modified

| File | Status | Changes |
|------|--------|---------|
| `MergedField.java` | ✅ | ConflictType enum, typesPerVersion, helper methods |
| `VersionMerger.java` | ✅ | detectConflictType(), builder pattern for MergedField |
| `InterfaceGenerator.java` | ✅ | Skip setters, Javadoc warnings |
| `AbstractClassGenerator.java` | ✅ | Skip abstract methods |
| `ImplClassGenerator.java` | ✅ | Skip implementations, type conflict detection |

---

## Success Criteria ✅

1. **Compilation:** ✅ Example project builds without errors
2. **Tests:** ✅ All existing tests pass + 34 new conflict tests
3. **Documentation:** ✅ Clear Javadoc for affected fields
4. **Usability:** ✅ Non-conflicting fields work normally with builders

---

## Future Enhancements (Phase 2)

### Option 1: Overloaded Setters for INT_ENUM

```java
// In Builder interface:
Builder setUnitType(int unitType);           // Works for V1
Builder setUnitType(UnitTypeEnum unitType);  // Works for V2

// In V1 BuilderImpl:
void doSetUnitType(int unitType) {
    protoBuilder.setUnitType(unitType);
}
void doSetUnitType(UnitTypeEnum unitType) {
    protoBuilder.setUnitType(unitType.getNumber());
}

// In V2 BuilderImpl:
void doSetUnitType(int unitType) {
    protoBuilder.setUnitType(UnitTypeEnum.forNumber(unitType));
}
void doSetUnitType(UnitTypeEnum unitType) {
    protoBuilder.setUnitType(unitType);
}
```

### Option 2: Version-Specific Builder Access

```java
// Direct access to version-specific builder
if (wrapper instanceof SensorReadingV2 v2) {
    v2.getTypedProto().toBuilder()
        .setUnitType(UnitTypeEnum.UNIT_KELVIN)
        .build();
}
```

This is already supported - users can access `getTypedProto()` and use the proto builder directly.
