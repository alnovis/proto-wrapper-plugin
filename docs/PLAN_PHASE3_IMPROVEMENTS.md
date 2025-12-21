# Phase 3: Улучшения обработки конфликтов типов

## Status: Planning

---

## Анализ текущего состояния

### Результаты тестирования на domain проекте (proto-full-jabba)

**Статистика схемы:**
- v202: 33 messages, 9 enums, 188 fields, 27 nested types
- v203: 31 messages, 11 enums, 173 fields, 26 nested types
- Merged: 33 messages, 11 enums

**Сгенерировано файлов:** 148
- 11 enums (обычные)
- 4 conflict enums (unified для INT_ENUM)
- 33 interfaces
- 33 abstract classes
- 64 implementation classes
- 3 VersionContext files

---

## Обнаруженные конфликты

| # | Сообщение | Поле | Тип конфликта | v202 | v203 | Текущее решение |
|---|-----------|------|---------------|------|------|-----------------|
| 1 | `BindedTax` | `tax_type` | INT_ENUM | `int` | `TaxTypeEnum` | ✅ Unified enum |
| 2 | `BindedTaxation` | `taxation_type` | INT_ENUM | `int` | `TaxationTypeEnum` | ✅ Unified enum |
| 3 | `Tax` | `tax_type` | INT_ENUM | `int` | `TaxTypeEnum` | ✅ Unified enum |
| 4 | `Tax` | `taxation_type` | INT_ENUM | `int` | `TaxationTypeEnum` | ✅ Unified enum |
| 5 | `CardPaymentFields` | `pos_rrn` | WIDENING | `long` | `int` | ✅ Auto-widen read |
| 6 | `TicketRequest` | `shift_document_number` | PRIMITIVE_MESSAGE | `int` | `ParentTicket` | ⚠️ Returns null |

---

## Текущая обработка по типам конфликтов

| Тип конфликта | Чтение | Builder | Статус |
|---------------|--------|---------|--------|
| **INT_ENUM** | ✅ `getXxx()` + `getXxxEnum()` | ✅ `setXxx(int)` + `setXxx(Enum)` | **Phase 2 завершён** |
| **WIDENING** | ✅ Автоматическое расширение | ❌ Setter пропускается | Требует Phase 3 |
| **NARROWING** | ⚠️ Возвращает default | ❌ Setter пропускается | Требует Phase 3 |
| **STRING_BYTES** | ❌ Возвращает null | ❌ Setter пропускается | Требует Phase 3 |
| **PRIMITIVE_MESSAGE** | ❌ Возвращает null | ❌ Setter пропускается | Фундаментально несовместимо |

### Детали текущей реализации WIDENING

WIDENING для чтения уже работает корректно:

```java
// v203 implementation (int → long)
protected Long extractPosRrn(Ticket.TicketRequest.Payment.CardPaymentFields proto) {
    return (long) proto.getPosRrn();  // Safe widening cast
}
```

---

## Предлагаемые улучшения Phase 3

### Phase 3A: WIDENING в Builders

**Сложность:** Medium
**Ценность:** High
**Приоритет:** P1

**Проблема:**
```java
// Текущее состояние - setter пропускается для WIDENING полей
interface Builder {
    // Builder setPosRrn(long posRrn);  // НЕ ГЕНЕРИРУЕТСЯ!
}
```

**Решение:**
```java
interface Builder {
    /**
     * Set posRrn value.
     * @param posRrn The value to set
     * @return This builder
     * @throws IllegalArgumentException if value exceeds target type range
     */
    Builder setPosRrn(long posRrn);
}
```

**Реализация для разных версий:**

```java
// v202 impl (proto accepts long):
protected void doSetPosRrn(long value) {
    protoBuilder.setPosRrn(value);  // Direct assignment
}

// v203 impl (proto accepts int):
protected void doSetPosRrn(long value) {
    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        throw new IllegalArgumentException(
            "Value " + value + " exceeds int range for v203");
    }
    protoBuilder.setPosRrn((int) value);  // Safe narrowing after check
}
```

**Файлы для изменения:**
- `InterfaceGenerator.java` - генерировать setter для WIDENING
- `AbstractClassGenerator.java` - добавить abstract doSetXxx
- `ImplClassGenerator.java` - реализация с проверкой диапазона
- `MergedField.java` - изменить `shouldSkipBuilderSetter()` для WIDENING

---

### Phase 3B: STRING_BYTES unified access

**Сложность:** Medium
**Ценность:** Medium
**Приоритет:** P2

**Проблема:**
```java
// V1: string checksum
// V2: bytes checksum
// Unified interface возвращает null для несовпадающей версии
```

**Решение:**
```java
interface TelemetryReport {
    /**
     * Get checksum as String.
     * For versions with bytes type, converts using UTF-8.
     */
    String getChecksum();

    /**
     * Get checksum as bytes.
     * For versions with string type, converts using UTF-8.
     */
    byte[] getChecksumBytes();

    interface Builder {
        Builder setChecksum(String value);
        Builder setChecksum(byte[] value);
    }
}
```

**Реализация:**

```java
// V1 impl (proto has string):
protected String extractChecksum(Proto proto) {
    return proto.getChecksum();
}
protected byte[] extractChecksumBytes(Proto proto) {
    return proto.getChecksum().getBytes(StandardCharsets.UTF_8);
}
protected void doSetChecksum(String value) {
    protoBuilder.setChecksum(value);
}
protected void doSetChecksum(byte[] value) {
    protoBuilder.setChecksum(new String(value, StandardCharsets.UTF_8));
}

// V2 impl (proto has bytes):
protected String extractChecksum(Proto proto) {
    return proto.getChecksum().toStringUtf8();
}
protected byte[] extractChecksumBytes(Proto proto) {
    return proto.getChecksum().toByteArray();
}
protected void doSetChecksum(String value) {
    protoBuilder.setChecksum(ByteString.copyFromUtf8(value));
}
protected void doSetChecksum(byte[] value) {
    protoBuilder.setChecksum(ByteString.copyFrom(value));
}
```

**Файлы для изменения:**
- `InterfaceGenerator.java` - два getter + два setter
- `AbstractClassGenerator.java` - abstract методы
- `ImplClassGenerator.java` - конверсия string ↔ bytes

---

### Phase 3C: PRIMITIVE_MESSAGE improved API

**Сложность:** Low
**Ценность:** Medium
**Приоритет:** P2

**Проблема:**
```java
// shift_document_number: int в v202, ParentTicket в v203
// Фундаментально несовместимые типы
Integer getShiftDocumentNumber();  // null для v203
ParentTicket getParentTicket();    // null для v202
```

**Решение - добавить availability helpers:**

```java
interface TicketRequest {
    // Существующие методы...
    Integer getShiftDocumentNumber();
    ParentTicket getParentTicket();

    /**
     * Check if this version uses shiftDocumentNumber (int field).
     * @return true for v202, false for v203
     * @apiNote For v203, use {@link #getParentTicket()} instead
     */
    default boolean usesShiftDocumentNumber() {
        return getWrapperVersion() == 202;  // или проверка hasShiftDocumentNumber()
    }

    /**
     * Check if this version uses parentTicket (message field).
     * @return true for v203, false for v202
     */
    default boolean usesParentTicket() {
        return getWrapperVersion() == 203;
    }
}
```

**Альтернативный подход - Union type:**

```java
sealed interface ShiftDocumentOrParentTicket {
    record IntValue(int shiftDocumentNumber) implements ShiftDocumentOrParentTicket {}
    record MessageValue(ParentTicket parentTicket) implements ShiftDocumentOrParentTicket {}
}

interface TicketRequest {
    /**
     * Get the shift document identifier (int in v202, ParentTicket in v203).
     */
    ShiftDocumentOrParentTicket getShiftDocumentOrParentTicket();
}
```

**Файлы для изменения:**
- `InterfaceGenerator.java` - генерировать default методы или union types

---

### Phase 3D: Conflict Summary API

**Сложность:** Low
**Ценность:** Low
**Приоритет:** P3

**Решение:**

```java
interface VersionContext {
    // Существующие методы...

    /**
     * Get all fields that have type conflicts across versions.
     * @return Set of "MessageName.fieldName" strings
     */
    Set<String> getConflictingFields();

    /**
     * Get detailed conflict information for a field.
     * @param messageName Message containing the field
     * @param fieldName Field name
     * @return Conflict info or empty if no conflict
     */
    Optional<ConflictInfo> getConflictInfo(String messageName, String fieldName);

    /**
     * Information about a type conflict.
     */
    record ConflictInfo(
        String fieldName,
        ConflictType type,
        Map<String, String> versionTypes  // e.g., {"v202": "int", "v203": "TaxTypeEnum"}
    ) {}

    enum ConflictType {
        INT_ENUM,
        WIDENING,
        NARROWING,
        STRING_BYTES,
        PRIMITIVE_MESSAGE
    }
}
```

**Файлы для изменения:**
- `VersionContextGenerator.java` - генерировать conflict summary
- `MergedSchema.java` - хранить информацию о конфликтах

---

## Quick Wins (можно сделать немедленно)

### 1. Улучшенные Javadoc для конфликтных полей

```java
/**
 * @return posRrn value
 * @apiNote Type differs across versions: v202 uses long, v203 uses int.
 *          Value is automatically widened to long for unified access.
 */
long getPosRrn();
```

### 2. Логирование статистики конфликтов

```
[INFO] Conflict summary:
[INFO]   INT_ENUM: 4 fields (tax_type, taxation_type x2)
[INFO]   WIDENING: 1 field (pos_rrn: long←int)
[INFO]   PRIMITIVE_MESSAGE: 1 field (shift_document_number: int→ParentTicket)
```

### 3. Генерация `supportsXxx()` методов

```java
interface TicketRequest {
    /**
     * Check if shiftDocumentNumber field is available in this version.
     * @return true for v202
     */
    default boolean supportsShiftDocumentNumber() {
        return hasShiftDocumentNumber();
    }
}
```

---

## Приоритеты и план реализации

| Фаза | Улучшение | Сложность | Ценность | Статус |
|------|-----------|-----------|----------|--------|
| 3A | WIDENING builders | Medium | High | ✅ DONE |
| 3B | STRING_BYTES unified | Medium | Medium | 🟡 TODO |
| 3C | PRIMITIVE_MESSAGE API | Low | Medium | 🟡 TODO |
| 3D | Conflict Summary | Low | Low | 🟢 TODO |
| QW | Javadoc improvements | Low | Medium | ✅ DONE |
| QW | Conflict logging | Low | Low | ✅ DONE |
| QW | supportsXxx() methods | Low | Medium | ✅ DONE |

---

## Файлы для изменения (сводка)

| Файл | 3A | 3B | 3C | 3D |
|------|----|----|----|----|
| `InterfaceGenerator.java` | ✓ | ✓ | ✓ | |
| `AbstractClassGenerator.java` | ✓ | ✓ | | |
| `ImplClassGenerator.java` | ✓ | ✓ | | |
| `MergedField.java` | ✓ | | | |
| `VersionContextGenerator.java` | | | | ✓ |
| `MergedSchema.java` | | | | ✓ |

---

## Критерии успеха Phase 3

### Phase 3A (WIDENING builders) ✅
- [x] Builder setter генерируется для WIDENING полей
- [x] Проверка диапазона при narrowing
- [x] IllegalArgumentException при выходе за диапазон
- [x] Unified getter использует широкий тип (long/double)

### Phase 3B (STRING_BYTES)
- [ ] Dual getters: `getXxx()` и `getXxxBytes()`
- [ ] Автоконверсия UTF-8
- [ ] Dual setters в Builder
- [ ] Тесты с различными кодировками

### Phase 3C (PRIMITIVE_MESSAGE)
- [ ] Helper методы `usesXxx()` или `supportsXxx()`
- [ ] Опционально: Union types
- [ ] Документация в Javadoc

### Phase 3D (Conflict Summary)
- [ ] `getConflictingFields()` в VersionContext
- [ ] `getConflictInfo()` с деталями
- [ ] ConflictInfo record

---

## Связанные документы

- [Phase 2: INT_ENUM Setters](PLAN_PHASE2_ENUM_SETTERS.md) - ✅ Завершено
- [Type Conflict Handling](PLAN_TYPE_CONFLICT_HANDLING.md) - Общая архитектура
- [Known Issues](KNOWN_ISSUES.md) - Текущие ограничения
