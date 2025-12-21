# Известные проблемы и ограничения

[English version](KNOWN_ISSUES.md)

Этот документ описывает известные проблемы и ограничения proto-wrapper-maven-plugin.

## Содержание

- [Обработка конфликтов типов](#обработка-конфликтов-типов)
- [Repeated-поля с конфликтами типов](#repeated-поля-с-конфликтами-типов)
- [Версионно-специфичные сообщения](#версионно-специфичные-сообщения)
- [Общие ограничения](#общие-ограничения)
- [Поведение во время выполнения](#поведение-во-время-выполнения)
- [Вопросы производительности](#вопросы-производительности)
- [Обходные пути](#обходные-пути)

---

## Обработка конфликтов типов

**Статус:** Полностью обрабатывается (v1.0.5+)

Когда поле имеет разные типы в разных версиях схемы, плагин автоматически обнаруживает и обрабатывает эти конфликты.

### Все типы конфликтов

| Тип конфликта | Пример | Чтение | Builder | Примечания |
|---------------|--------|--------|---------|------------|
| `NONE` | Одинаковый тип во всех версиях | Обычный getter | Обычный setter | Без специальной обработки |
| `INT_ENUM` | int ↔ TaxTypeEnum | `getXxx()` + `getXxxEnum()` | `setXxx(int)` + `setXxx(Enum)` | Конвертируемый |
| `WIDENING` | int → long, float → double | Автоматическое расширение | Setter с проверкой диапазона | Конвертируемый |
| `NARROWING` | long → int, double → float | Использует широкий тип (long/double) | Setter с проверкой диапазона | Обрабатывается как WIDENING |
| `STRING_BYTES` | string ↔ bytes | `getXxx()` + `getXxxBytes()` | `setXxx(String)` | UTF-8 конвертация |
| `PRIMITIVE_MESSAGE` | int → Message | `getXxx()` + `getXxxMessage()` | **Не генерируется** | Возвращает null/default |
| `INCOMPATIBLE` | string ↔ int, bool ↔ message | Возвращает значение по умолчанию | **Не генерируется** | Поле считается отсутствующим |

### Поведение NARROWING

Хотя определён как "с потерями", конфликты NARROWING обрабатываются так же, как WIDENING:

```java
// v1: int64 value = 1;
// v2: int32 value = 1;

interface Data {
    long getValue();  // Унифицирован как широкий тип (long)

    interface Builder {
        // Проверка диапазона выбрасывает IllegalArgumentException если значение превышает диапазон int для V2
        Builder setValue(long value);
    }
}
```

### Поведение INCOMPATIBLE

Поля с несовместимыми типами считаются "отсутствующими" в версиях, где тип не совпадает:

```java
// v1: string data = 1;
// v2: int32 data = 1;

interface Message {
    // Возвращает пустую строку для V2, реальное значение для V1
    String getData();

    // has-метод возвращает false для V2
    boolean hasData();

    // Setter Builder НЕ генерируется (был бы неоднозначным)
}
```

### Пример INT_ENUM

```java
// Унифицированный enum генерируется автоматически
public enum UnitType {
    UNIT_CELSIUS(0), UNIT_FAHRENHEIT(1), UNIT_KELVIN(2), ...
}

// Интерфейс с двойными геттерами и перегруженными сеттерами
interface SensorReading {
    int getUnitType();              // Возвращает int значение
    UnitType getUnitTypeEnum();     // Возвращает унифицированный enum

    interface Builder {
        Builder setUnitType(int value);       // Установка через int
        Builder setUnitType(UnitType value);  // Установка через enum
    }
}
```

### Пример STRING_BYTES

```java
interface TelemetryReport {
    String getChecksum();       // Строковое представление (UTF-8 для bytes версий)
    byte[] getChecksumBytes();  // Сырые байты (UTF-8 кодировка для string версий)

    interface Builder {
        Builder setChecksum(String value);
        Builder setChecksumBytes(byte[] value);
    }
}
```

### Пример WIDENING

```java
interface SensorReading {
    long getRawValue();    // int32 в v1, int64 в v2 → унифицирован как long
    double getValues();    // float в v1, double в v2 → унифицирован как double

    interface Builder {
        Builder setRawValue(long value);  // Проверка диапазона для узких версий
    }
}
```

---

## Repeated-поля с конфликтами типов

**Статус:** Только для чтения (v1.0.6+)

Repeated-поля с конфликтами типов полностью читаемы, но не устанавливаемы через унифицированный builder.

### Поддерживаемые операции чтения

| Тип конфликта | Пример | Унифицированный тип |
|---------------|--------|---------------------|
| `WIDENING` | `repeated int32` → `repeated int64` | `List<Long>` |
| `WIDENING` | `repeated float` → `repeated double` | `List<Double>` |
| `INT_ENUM` | `repeated int32` → `repeated SomeEnum` | `List<Integer>` |
| `STRING_BYTES` | `repeated string` → `repeated bytes` | `List<String>` |

### Пример

```java
interface RepeatedConflicts {
    List<Long> getNumbers();    // int32 в v1, int64 в v2 → расширенные элементы
    List<Integer> getCodes();   // int32 в v1, enum в v2 → enum.getNumber()
    List<String> getTexts();    // string в v1, bytes в v2 → UTF-8 конвертация
    List<Double> getValues();   // float в v1, double в v2 → расширенные элементы

    // Builder - repeated-поля с конфликтами НЕ доступны
    interface Builder {
        // Примечание: setNumbers, setCodes, setTexts, setValues НЕ генерируются
        // Используйте типизированный proto builder для прямого доступа
    }
}
```

### Доступ к repeated-полям с конфликтами для модификации

Используйте типизированный proto builder напрямую:

```java
// Для V2 версии
var v2 = (RepeatedConflictsV2) wrapper;
var modified = new RepeatedConflictsV2(
    v2.getTypedProto().toBuilder()
        .addNumbers(12345L)
        .addCodes(CodeEnum.CODE_SUCCESS)
        .build()
);
```

---

## Версионно-специфичные сообщения

**Статус:** Runtime-исключение для отсутствующих версий

Сообщения, которые существуют только в некоторых версиях, выбрасывают `UnsupportedOperationException` при доступе через неправильный VersionContext.

### Поведение

```java
// Сообщение "NewFeature" существует только в V2

// Использование V2 контекста - работает
VersionContext v2 = VersionContext.V2;
NewFeature feature = v2.wrapNewFeature(proto);  // OK
NewFeature.Builder builder = v2.newNewFeatureBuilder();  // OK

// Использование V1 контекста - выбрасывает исключение
VersionContext v1 = VersionContext.V1;
NewFeature feature = v1.wrapNewFeature(proto);  // UnsupportedOperationException!
```

### Сообщение исключения

```
NewFeature is not available in this version. Present in: [V2]
```

### Безопасный паттерн доступа

```java
// Проверьте версию перед доступом
public NewFeature getNewFeatureIfAvailable(VersionContext ctx, Message proto) {
    if (ctx instanceof VersionContextV2) {
        return ctx.wrapNewFeature(proto);
    }
    return null;  // Недоступно в этой версии
}
```

---

## Общие ограничения

### 1. oneof-поля
- **Статус:** Не поддерживается
- **Описание:** Protobuf `oneof` поля не обрабатываются специально; каждое поле в oneof обрабатывается независимо

### 2. map-поля
- **Статус:** Базовая поддержка
- **Описание:** Map-поля имеют ограниченную поддержку, могут работать некорректно с конфликтами типов

### 3. Extensions
- **Статус:** Не поддерживается
- **Описание:** Protobuf extensions (proto2) не поддерживаются

### 4. Конвертация версий (`asVersion`)
- **Статус:** Не реализовано
- **Описание:** Метод `asVersion(Class<T> versionClass)` выбрасывает `UnsupportedOperationException`
- **Обходной путь:** Используйте сериализацию: `targetVersion.from(TargetProto.parseFrom(source.toBytes()))`

### 5. Сложные вложенные иерархии
- **Статус:** Частичная поддержка
- **Описание:** Глубоко вложенные иерархии сообщений с конфликтами могут требовать ручной конфигурации

### 6. Well-Known Types (google.protobuf.*)
- **Статус:** Обрабатываются как обычные сообщения
- **Описание:** Типы вроде `Timestamp`, `Duration`, `Any`, `Struct` не обрабатываются специально
- **Обходной путь:** Доступ через `getTypedProto()` и использование protobuf-утилит:

```java
// Для Timestamp
var v1 = (MyMessageV1) wrapper;
Timestamp ts = v1.getTypedProto().getCreatedAt();
Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
```

### 7. Конфликты номеров полей
- **Статус:** Неопределённое поведение
- **Описание:** Если одно и то же имя поля имеет разные номера полей в разных версиях, поведение непредсказуемо
- **Рекомендация:** Убедитесь, что номера полей согласованы между версиями

### 8. Services и RPCs
- **Статус:** Не поддерживается
- **Описание:** Обрабатываются только типы message и enum; определения сервисов игнорируются

---

## Поведение во время выполнения

### Потокобезопасность

| Компонент | Потокобезопасен? | Примечания |
|-----------|------------------|------------|
| Wrapper-классы | Да | Неизменяемы, proto неизменяем |
| Builder-классы | **Нет** | Не предназначены для параллельной модификации |
| VersionContext | Да | Stateless singleton |

```java
// Безопасно: Разделяйте wrappers между потоками
Order order = ctx.wrapOrder(proto);
executor.submit(() -> processOrder(order));  // OK

// Небезопасно: Не разделяйте builders
Order.Builder builder = ctx.newOrderBuilder();
executor.submit(() -> builder.setOrderId("1"));  // RACE CONDITION!
```

### Nullability

| Тип поля | Getter возвращает | Примечания |
|----------|-------------------|------------|
| Required (proto2) | Никогда null | Всегда имеет значение |
| Optional scalar | Может быть null | Используйте `hasXxx()` для проверки |
| Optional message | Может быть null | Используйте `hasXxx()` для проверки |
| Repeated | Никогда null | Возвращает пустой список если нет элементов |
| Map | Никогда null | Возвращает пустую map если нет записей |

```java
// Безопасные паттерны
if (order.hasShippingAddress()) {
    Address addr = order.getShippingAddress();  // Не null здесь
}

List<OrderItem> items = order.getItems();  // Никогда null, может быть пустым
for (OrderItem item : items) {
    // Безопасная итерация
}
```

### Конвертация ByteString

Protobuf использует `ByteString` внутри; wrappers конвертируют в `byte[]`:

```java
interface Report {
    byte[] getData();  // Конвертировано из ByteString
}

// Последствия:
// 1. Новый byte[] массив создаётся при каждом вызове
// 2. Модификации возвращённого массива не влияют на proto
// 3. Для больших бинарных данных рассмотрите использование getTypedProto()
```

---

## Вопросы производительности

### Создание List-обёрток

Геттеры repeated-полей создают новые списки обёрток при каждом вызове:

```java
// Каждый вызов создаёт новый ArrayList с обёрнутыми элементами
List<OrderItem> items1 = order.getItems();
List<OrderItem> items2 = order.getItems();
assert items1 != items2;  // Разные экземпляры списков

// Для горячих путей кэшируйте результат
List<OrderItem> items = order.getItems();
for (int i = 0; i < 1000; i++) {
    process(items);  // Переиспользуйте кэшированный список
}
```

### Накладные расходы обёрток

Каждая обёртка добавляет минимальные накладные расходы:
- Одна аллокация объекта на обёртку
- Одна ссылка на базовый proto
- Вызовы методов делегируют к proto

Для критичного к производительности кода с миллионами сообщений рассмотрите:
1. Прямой доступ к proto через `getTypedProto()`
2. Пакетная обработка с кэшированными обёртками
3. Proto streaming вместо коллекций обёрток

---

## Обходные пути

### Для repeated-полей с конфликтами в Builders

Используйте типизированный proto builder:

```java
var v2 = (MyMessageV2) wrapper;
var protoBuilder = v2.getTypedProto().toBuilder();
protoBuilder.addRepeatedField(value);
MyMessage modified = new MyMessageV2(protoBuilder.build());
```

### Для конфликтов PRIMITIVE_MESSAGE

Доступ через типизированный proto:

```java
if (wrapper instanceof MyMessageV2 v2) {
    NestedMessage nested = v2.getTypedProto().getNestedField();
}
```

### Для конвертации версий

Сериализуйте и парсите:

```java
// Конвертация из V1 в V2
byte[] bytes = v1Wrapper.toBytes();
V2Proto proto = V2Proto.parseFrom(bytes);
MyMessageV2 v2Wrapper = new MyMessageV2(proto);
```

### Для Well-Known Types

Используйте protobuf-утилиты:

```java
// Timestamp → Instant
Timestamp ts = ((MyMessageV1) wrapper).getTypedProto().getTimestamp();
Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());

// Duration → java.time.Duration
Duration d = ((MyMessageV1) wrapper).getTypedProto().getDuration();
java.time.Duration duration = java.time.Duration.ofSeconds(d.getSeconds(), d.getNanos());
```

---

## Совместимость версий Protobuf

**Статус:** Поддерживается через конфигурацию

| Версия Protobuf | Метод для конвертации enum |
|-----------------|----------------------------|
| 2.x | `EnumType.valueOf(int)` |
| 3.x | `EnumType.forNumber(int)` |

Установите параметр `protobufMajorVersion` в конфигурации плагина:

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>2</protobufMajorVersion> <!-- Для protobuf 2.x -->
</configuration>
```

**Значение по умолчанию:** `3` (protobuf 3.x)

---

## Сообщение о проблемах

Если вы обнаружите проблемы, не документированные здесь, пожалуйста, сообщите о них:
https://github.com/anthropics/proto-wrapper-plugin/issues

Включите:
- Версию плагина
- Версию Protobuf
- Релевантные фрагменты proto-файлов
- Сообщения об ошибках
- Конфигурацию Maven

