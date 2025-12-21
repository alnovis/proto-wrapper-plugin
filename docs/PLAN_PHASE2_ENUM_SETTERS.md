# Phase 2: Overloaded Setters for INT_ENUM Conflicts

## Status: ✅ COMPLETED (December 2025)

---

## Implementation Summary

Phase 2 was successfully implemented using **Variant B: Unified Enum**.

### What was delivered:

1. **ConflictEnumInfo model** (`proto-wrapper-core/.../model/ConflictEnumInfo.java`)
   - Stores merged enum values from all versions
   - Maps version IDs to proto enum types

2. **ConflictEnumGenerator** (`proto-wrapper-core/.../generator/ConflictEnumGenerator.java`)
   - Generates unified enums (e.g., `UnitType`, `SeverityCode`, `SyncStatus`)
   - Includes `getValue()`, `fromProtoValue()`, `fromProtoValueOrDefault()` methods

3. **VersionMerger changes** - Collects enum values when INT_ENUM conflict detected

4. **MergedSchema changes** - Stores and retrieves conflict enums

5. **InterfaceGenerator changes**:
   - Adds `getXxxEnum()` method returning unified enum
   - Adds overloaded `setXxx(int)` and `setXxx(UnifiedEnum)` builder setters

6. **AbstractClassGenerator changes** - Adds abstract `doSetXxx` methods for both types

7. **ImplClassGenerator changes** - Implements extract and set methods for each version

### Generated API example:

```java
// Unified enum (auto-generated)
public enum UnitType {
    UNIT_CELSIUS(0), UNIT_FAHRENHEIT(1), UNIT_KELVIN(2), ...
}

// Interface
public interface SensorReading {
    int getUnitType();              // Returns int value
    UnitType getUnitTypeEnum();     // Returns unified enum

    interface Builder {
        Builder setUnitType(int value);
        Builder setUnitType(UnitType value);
    }
}

// Usage
SensorReading reading = ...;
UnitType type = reading.getUnitTypeEnum();

SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)
    .build();
```

### Test coverage:
- 144 tests passing
- New Phase 2 tests for unified enum getters and overloaded setters
- Round-trip tests verifying data integrity

---

## Problem Statement

В Phase 1 мы пропускаем все конфликтующие поля в builders. Для INT_ENUM конфликтов (int ↔ enum) это означает, что пользователи не могут устанавливать значения через unified interface:

```java
// Текущее состояние (Phase 1):
// setUnitType() НЕ доступен в builder
SensorReading reading = builder
    .setSensorId("SENSOR-001")
    // .setUnitType(???)  — нельзя!
    .build();

// Приходится использовать getTypedProto():
var v2 = (SensorReadingV2) reading;
var proto = v2.getTypedProto().toBuilder()
    .setUnitType(UnitTypeEnum.UNIT_KELVIN)
    .build();
```

**Цель Phase 2:** Поддержать INT_ENUM конфликты через перегруженные setters.

---

## Анализ сложности

### Проблема 1: Типы enum из разных версий

```
V1: int unitType       → com.example.proto.v1 (нет enum)
V2: UnitTypeEnum       → com.example.proto.v2.Telemetry.UnitTypeEnum
```

Если добавить `setUnitType(UnitTypeEnum)` в unified interface, создаётся зависимость от V2-специфичного типа.

### Проблема 2: Несколько версий с разными enum

```
V1: int taxType
V2: TaxTypeEnumV2
V3: TaxTypeEnumV3 (другой enum с новыми значениями)
```

Какой тип использовать в unified interface?

### Проблема 3: Разные значения enum

```
V2: enum { CELSIUS=0, FAHRENHEIT=1 }
V3: enum { CELSIUS=0, FAHRENHEIT=1, KELVIN=2, RANKINE=3 }
```

Unified enum должен объединять все значения.

---

## Варианты решения

### Вариант A: Прямое использование proto enum (простой, но грязный)

```java
// В unified interface Builder:
Builder setUnitType(int unitType);
Builder setUnitType(com.example.proto.v2.Telemetry.UnitTypeEnum unitType);
```

| Плюсы | Минусы |
|-------|--------|
| Простая реализация | Нарушает абстракцию версий |
| Type-safe для V2 | API зависит от V2 proto |
| | Непонятно какую версию использовать при V3 |

**Оценка:** ❌ Не рекомендуется

---

### Вариант B: Unified Enum для конфликтных полей

Генерируем unified enum в API пакете для полей с INT_ENUM конфликтом:

```java
// Генерируется в com.example.model.api
public enum UnitType {
    CELSIUS(0),
    FAHRENHEIT(1),
    KELVIN(2),
    PASCAL(3),
    BAR(4),
    PERCENT(5);

    private final int value;

    UnitType(int value) { this.value = value; }

    public int getValue() { return value; }

    public static UnitType fromProtoValue(int value) {
        for (UnitType e : values()) {
            if (e.value == value) return e;
        }
        return null;  // or throw
    }
}
```

```java
// В SensorReading interface:
int getUnitType();              // Возвращает int (совместимость)
UnitType getUnitTypeEnum();     // Возвращает unified enum (удобство)

interface Builder {
    Builder setUnitType(int unitType);
    Builder setUnitType(UnitType unitType);
}
```

| Плюсы | Минусы |
|-------|--------|
| Чистая абстракция | Сложная реализация |
| Type-safe | Два getter-а на поле |
| Независим от версий | Нужно сливать enum values |
| Объединяет значения всех версий | |

**Оценка:** ✅ Рекомендуется

---

### Вариант C: Int-константы (простой fallback)

```java
public interface SensorReading {
    // Константы для unitType (из V2 UnitTypeEnum)
    int UNIT_TYPE_CELSIUS = 0;
    int UNIT_TYPE_FAHRENHEIT = 1;
    int UNIT_TYPE_KELVIN = 2;
    int UNIT_TYPE_PASCAL = 3;

    interface Builder {
        Builder setUnitType(int unitType);
    }
}

// Использование:
builder.setUnitType(SensorReading.UNIT_TYPE_KELVIN);
```

| Плюсы | Минусы |
|-------|--------|
| Очень простая реализация | Нет type-safety |
| Нет новых типов | Менее элегантно |
| Обратная совместимость | |

**Оценка:** ⚠️ Fallback вариант

---

## Рекомендуемый подход: Вариант B

### Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    com.example.model.api                     │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────┐     │
│  │ UnitType (enum) │    │ SensorReading (interface)   │     │
│  │ - CELSIUS       │    │ + getUnitType(): int        │     │
│  │ - FAHRENHEIT    │    │ + getUnitTypeEnum(): UnitType│    │
│  │ - KELVIN        │    │                             │     │
│  │ - ...           │    │ interface Builder {         │     │
│  │                 │    │   + setUnitType(int)        │     │
│  │ + getValue()    │    │   + setUnitType(UnitType)   │     │
│  │ + fromProto()   │    │ }                           │     │
│  └─────────────────┘    └─────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┴──────────────────┐
           ▼                                      ▼
┌─────────────────────────┐          ┌─────────────────────────┐
│ com.example.model.v1    │          │ com.example.model.v2    │
├─────────────────────────┤          ├─────────────────────────┤
│ SensorReading           │          │ SensorReading           │
│ - proto: v1.Proto       │          │ - proto: v2.Proto       │
│                         │          │                         │
│ extractUnitType() {     │          │ extractUnitType() {     │
│   return proto.get()    │          │   return proto.get()    │
│ }                       │          │        .getNumber()     │
│                         │          │ }                       │
│ BuilderImpl:            │          │ BuilderImpl:            │
│ doSetUnitType(int) {    │          │ doSetUnitType(int) {    │
│   proto.set(int)        │          │   proto.set(forNumber)  │
│ }                       │          │ }                       │
│ doSetUnitType(Enum) {   │          │ doSetUnitType(Enum) {   │
│   proto.set(getValue()) │          │   proto.set(forNumber)  │
│ }                       │          │ }                       │
└─────────────────────────┘          └─────────────────────────┘
```

---

## План реализации

### Step 1: Модель данных для Conflict Enum

**Файл:** `proto-wrapper-core/.../model/ConflictEnumInfo.java`

```java
public class ConflictEnumInfo {
    private final String fieldName;           // "unitType"
    private final String enumName;            // "UnitType"
    private final Set<EnumValue> values;      // Merged values from all versions
    private final Map<String, String> versionEnumTypes;  // version -> proto enum FQN

    public record EnumValue(String name, int number) {}
}
```

### Step 2: Сбор enum values при слиянии

**Файл:** `VersionMerger.java`

```java
// При обнаружении INT_ENUM конфликта:
private ConflictEnumInfo collectEnumValuesForField(String fieldName,
                                                    List<FieldWithVersion> fields) {
    Set<EnumValue> allValues = new LinkedHashSet<>();
    Map<String, String> versionEnumTypes = new HashMap<>();

    for (FieldWithVersion fv : fields) {
        if (fv.field().isEnum()) {
            EnumInfo enumInfo = findEnumInfo(fv.field());
            enumInfo.getValues().forEach(v ->
                allValues.add(new EnumValue(v.getName(), v.getNumber())));
            versionEnumTypes.put(fv.version(), enumInfo.getFullyQualifiedName());
        }
    }

    return new ConflictEnumInfo(fieldName, deriveEnumName(fieldName),
                                 allValues, versionEnumTypes);
}
```

### Step 3: Хранение Conflict Enums в MergedSchema

**Файл:** `MergedSchema.java`

```java
public class MergedSchema {
    // Existing...

    // NEW: Enums generated for INT_ENUM conflict fields
    private final Map<String, ConflictEnumInfo> conflictEnums;  // fieldPath -> info

    public void addConflictEnum(String messageName, String fieldName,
                                 ConflictEnumInfo info) {
        conflictEnums.put(messageName + "." + fieldName, info);
    }

    public Optional<ConflictEnumInfo> getConflictEnum(String messageName,
                                                       String fieldName) {
        return Optional.ofNullable(conflictEnums.get(messageName + "." + fieldName));
    }
}
```

### Step 4: Генерация Conflict Enum

**Файл:** `ConflictEnumGenerator.java` (новый)

```java
public class ConflictEnumGenerator extends BaseGenerator<ConflictEnumInfo> {

    public JavaFile generate(ConflictEnumInfo info, String apiPackage) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(info.getEnumName())
            .addModifiers(Modifier.PUBLIC)
            .addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(generateConstructor())
            .addMethod(generateGetValue())
            .addMethod(generateFromProtoValue(info));

        // Add enum constants
        for (EnumValue value : info.getValues()) {
            enumBuilder.addEnumConstant(value.name(),
                TypeSpec.anonymousClassBuilder("$L", value.number()).build());
        }

        return JavaFile.builder(apiPackage, enumBuilder.build()).build();
    }
}
```

### Step 5: Добавить enum getter в Interface

**Файл:** `InterfaceGenerator.java`

```java
// Для полей с INT_ENUM конфликтом:
if (field.getConflictType() == ConflictType.INT_ENUM) {
    ConflictEnumInfo enumInfo = schema.getConflictEnum(message.getName(),
                                                        field.getName()).get();
    ClassName enumType = ClassName.get(apiPackage, enumInfo.getEnumName());

    // Добавляем enum getter
    interfaceBuilder.addMethod(MethodSpec.methodBuilder("get" + capitalize(field.getName()) + "Enum")
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .returns(enumType)
        .addJavadoc("Get $L as unified enum.\n@return Unified enum value", field.getName())
        .build());
}
```

### Step 6: Добавить перегруженные setters в Builder

**Файл:** `InterfaceGenerator.java`

```java
// Для INT_ENUM конфликтов - генерируем ОБА setter-а:
if (field.getConflictType() == ConflictType.INT_ENUM) {
    ConflictEnumInfo enumInfo = schema.getConflictEnum(...);
    ClassName enumType = ClassName.get(apiPackage, enumInfo.getEnumName());

    // int setter
    builderBuilder.addMethod(MethodSpec.methodBuilder("set" + capitalize(field.getName()))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addParameter(TypeName.INT, field.getJavaName())
        .returns(builderType)
        .build());

    // enum setter
    builderBuilder.addMethod(MethodSpec.methodBuilder("set" + capitalize(field.getName()))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addParameter(enumType, field.getJavaName())
        .returns(builderType)
        .build());
}
```

### Step 7: Abstract Builder методы

**Файл:** `AbstractClassGenerator.java`

```java
// Для INT_ENUM - два abstract метода:
if (field.getConflictType() == ConflictType.INT_ENUM) {
    // doSetXxx(int)
    abstractBuilderBuilder.addMethod(MethodSpec.methodBuilder("doSet" + capitalize(name))
        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
        .addParameter(TypeName.INT, name)
        .build());

    // doSetXxx(EnumType)
    abstractBuilderBuilder.addMethod(MethodSpec.methodBuilder("doSet" + capitalize(name))
        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
        .addParameter(enumType, name)
        .build());
}
```

### Step 8: Impl Builder реализации

**Файл:** `ImplClassGenerator.java`

```java
// V1 (proto accepts int):
void doSetUnitType(int unitType) {
    protoBuilder.setUnitType(unitType);
}
void doSetUnitType(UnitType unitType) {
    protoBuilder.setUnitType(unitType.getValue());
}

// V2 (proto accepts enum):
void doSetUnitType(int unitType) {
    protoBuilder.setUnitType(V2Enum.forNumber(unitType));
}
void doSetUnitType(UnitType unitType) {
    protoBuilder.setUnitType(V2Enum.forNumber(unitType.getValue()));
}
```

### Step 9: Impl enum getter

**Файл:** `ImplClassGenerator.java`

```java
// V1:
protected UnitType extractUnitTypeEnum(Proto proto) {
    return UnitType.fromProtoValue(proto.getUnitType());
}

// V2:
protected UnitType extractUnitTypeEnum(Proto proto) {
    return UnitType.fromProtoValue(proto.getUnitType().getNumber());
}
```

### Step 10: Интеграция в Pipeline

**Файл:** `PluginMojo.java` или `GeneratorPipeline`

```java
// После генерации основных файлов:
ConflictEnumGenerator conflictEnumGen = new ConflictEnumGenerator(config);
for (ConflictEnumInfo info : schema.getConflictEnums()) {
    JavaFile enumFile = conflictEnumGen.generate(info, apiPackage);
    enumFile.writeTo(outputDir);
}
```

---

## Файлы для изменения/создания

| Файл | Действие | Изменения |
|------|----------|-----------|
| `ConflictEnumInfo.java` | CREATE | Модель для conflict enum |
| `ConflictEnumGenerator.java` | CREATE | Генерация unified enum |
| `MergedSchema.java` | MODIFY | Хранение conflict enums |
| `VersionMerger.java` | MODIFY | Сбор enum values |
| `MergedField.java` | MODIFY | Ссылка на ConflictEnumInfo |
| `InterfaceGenerator.java` | MODIFY | Enum getter + overloaded setters |
| `AbstractClassGenerator.java` | MODIFY | Overloaded abstract methods |
| `ImplClassGenerator.java` | MODIFY | Overloaded implementations |
| `PluginMojo.java` | MODIFY | Запуск ConflictEnumGenerator |

---

## Тестирование

### Unit Tests

1. `ConflictEnumInfoTest` - модель данных
2. `ConflictEnumGeneratorTest` - генерация enum
3. `VersionMergerConflictEnumTest` - сбор enum values

### Integration Tests

1. `IntEnumSetterTest` - использование int setter
2. `EnumSetterTest` - использование enum setter
3. `EnumGetterTest` - получение enum значения
4. `MixedVersionBuilderTest` - builder работает для V1 и V2

### Round-Trip Tests

1. V1: set via int → get via enum → verify
2. V2: set via enum → get via int → verify
3. Cross-version: V1 proto → wrap V2 → get enum → verify

---

## Риски и митигации

| Риск | Митигация |
|------|-----------|
| Конфликт имён enum | Использовать field name + "Type" суффикс |
| Разные значения в разных версиях | Объединять все значения, логировать warning |
| Сложность отладки | Подробные Javadoc, примеры использования |
| Производительность | Кэширование enum lookup |

---

## Критерии успеха

1. ✅ Генерируется unified enum для каждого INT_ENUM конфликта
2. ✅ Builder имеет перегруженные setters (int и enum)
3. ✅ Все версии корректно конвертируют значения
4. ✅ Существующие тесты проходят
5. ✅ Новые тесты для overloaded setters проходят
6. ✅ Документация обновлена

---

## Оценка трудозатрат

| Компонент | Сложность | Оценка |
|-----------|-----------|--------|
| ConflictEnumInfo model | Low | 1h |
| ConflictEnumGenerator | Medium | 3h |
| VersionMerger changes | Medium | 2h |
| InterfaceGenerator changes | Medium | 2h |
| AbstractClassGenerator changes | Low | 1h |
| ImplClassGenerator changes | High | 4h |
| Pipeline integration | Low | 1h |
| Unit tests | Medium | 2h |
| Integration tests | Medium | 3h |
| Documentation | Low | 1h |
| **Total** | | **~20h** |

---

## Альтернатива: Упрощённая Phase 2 (Вариант C)

Если полная реализация слишком сложна, можно сделать упрощённый вариант:

1. Генерировать только int-константы в interface
2. Не генерировать unified enum
3. Не генерировать overloaded setters

```java
public interface SensorReading {
    // Constants for unitType (from V2 enum)
    int UNIT_TYPE_CELSIUS = 0;
    int UNIT_TYPE_FAHRENHEIT = 1;

    interface Builder {
        Builder setUnitType(int unitType);  // Только int setter
    }
}
```

Это можно реализовать за ~4 часа.
