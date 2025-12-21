# Proto Wrapper Plugin Cookbook

[English version](COOKBOOK.md)

Практическое руководство по использованию proto-wrapper-maven-plugin с подробными примерами.

## Содержание

- [Быстрый старт](#быстрый-старт)
- [Конфигурация плагина](#конфигурация-плагина)
- [Обработка конфликтов типов](#обработка-конфликтов-типов)
- [Режимы генерации](#режимы-генерации)
- [Типичные сценарии использования](#типичные-сценарии-использования)
- [Решение проблем](#решение-проблем)

---

## Быстрый старт

### Шаг 1: Добавьте плагин в pom.xml

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.4</version>
    <configuration>
        <basePackage>com.mycompany.model</basePackage>
        <protoPackagePattern>com.mycompany.proto.{version}</protoPackagePattern>
        <protoRoot>${basedir}/src/main/proto</protoRoot>
        <versions>
            <version><protoDir>v1</protoDir></version>
            <version><protoDir>v2</protoDir></version>
        </versions>
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Шаг 2: Организуйте proto-файлы

```
src/main/proto/
├── v1/
│   ├── common.proto
│   └── order.proto
└── v2/
    ├── common.proto
    └── order.proto
```

### Шаг 3: Сгенерируйте код

```bash
mvn generate-sources
```

### Шаг 4: Используйте API

```java
// Определите версию и оберните proto
int version = determineVersion(protoBytes);
VersionContext ctx = VersionContext.forVersion(version);

Order order = ctx.wrapOrder(OrderProto.parseFrom(protoBytes));

// Используйте версионно-независимый API
DateTime dateTime = order.getDateTime();
List<OrderItem> items = order.getItems();
PaymentType payment = order.getPaymentType();

// Сериализуйте обратно
byte[] outputBytes = order.toBytes();
```

---

## Конфигурация плагина

### Минимальная конфигурация

```xml
<configuration>
    <basePackage>com.example.model</basePackage>
    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>
    <protoRoot>${basedir}/proto</protoRoot>
    <versions>
        <version><protoDir>v1</protoDir></version>
        <version><protoDir>v2</protoDir></version>
    </versions>
</configuration>
```

### Полная конфигурация

```xml
<configuration>
    <!-- Базовый пакет (обязательный) -->
    <basePackage>com.example.model</basePackage>

    <!-- Шаблон пакета proto (обязательный) -->
    <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>

    <!-- Корневая директория с proto-файлами (обязательный) -->
    <protoRoot>${basedir}/proto</protoRoot>

    <!-- Директория вывода (по умолчанию: target/generated-sources/proto-wrapper) -->
    <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>

    <!-- Генерировать интерфейсы Builder (по умолчанию: false) -->
    <generateBuilders>true</generateBuilders>

    <!-- Суффикс версии в именах классов: MoneyV1 vs Money (по умолчанию: true) -->
    <includeVersionSuffix>true</includeVersionSuffix>

    <!-- Версия Protobuf: 2 или 3 (по умолчанию: 3) -->
    <protobufMajorVersion>3</protobufMajorVersion>

    <!-- Флаги генерации (все true по умолчанию) -->
    <generateInterfaces>true</generateInterfaces>
    <generateAbstractClasses>true</generateAbstractClasses>
    <generateImplClasses>true</generateImplClasses>
    <generateVersionContext>true</generateVersionContext>

    <!-- Фильтрация сообщений -->
    <includeMessages>
        <message>Order</message>
        <message>Customer</message>
    </includeMessages>
    <excludeMessages>
        <message>InternalMessage</message>
    </excludeMessages>

    <!-- Конфигурации версий -->
    <versions>
        <version>
            <protoDir>v1</protoDir>
            <name>V1</name>
            <excludeProtos>
                <excludeProto>internal.proto</excludeProto>
            </excludeProtos>
        </version>
        <version>
            <protoDir>v2</protoDir>
        </version>
    </versions>
</configuration>
```

### Производные пакеты

Когда задан `basePackage`, другие пакеты вычисляются автоматически:

| Параметр | Значение |
|----------|----------|
| `apiPackage` | `{basePackage}.api` |
| `implPackagePattern` | `{basePackage}.{version}` |

Результат для `basePackage=com.example.model`:
- Интерфейсы: `com.example.model.api`
- V1 реализации: `com.example.model.v1`
- V2 реализации: `com.example.model.v2`

---

## Обработка конфликтов типов

Плагин автоматически обнаруживает и обрабатывает ситуации, когда типы полей различаются между версиями.

### Обзор типов конфликтов

| Тип конфликта | Описание | Конвертируемый? |
|---------------|----------|-----------------|
| `NONE` | Нет конфликта | - |
| `INT_ENUM` | `int` ↔ `enum` | Да |
| `WIDENING` | `int` → `long`, `float` → `double` | Да |
| `NARROWING` | `long` → `int`, `double` → `float` | Нет (потеря данных) |
| `STRING_BYTES` | `string` ↔ `bytes` | Да (UTF-8) |
| `PRIMITIVE_MESSAGE` | `int` → `SomeMessage` | Нет |
| `INCOMPATIBLE` | Несовместимые типы | Нет |

### Конфликт INT_ENUM

**Ситуация**: Поле `int32` в одной версии и `enum` в другой.

```protobuf
// v1/sensor.proto
message SensorReading {
    int32 unit_type = 1;  // 0=Celsius, 1=Fahrenheit
}

// v2/sensor.proto
message SensorReading {
    UnitType unit_type = 1;
}
enum UnitType {
    UNIT_CELSIUS = 0;
    UNIT_FAHRENHEIT = 1;
    UNIT_KELVIN = 2;  // Новое значение в v2
}
```

**Сгенерированный код**:

```java
// Унифицированный enum
public enum UnitType {
    UNIT_CELSIUS(0),
    UNIT_FAHRENHEIT(1),
    UNIT_KELVIN(2);

    public static UnitType fromProtoValue(int value) { ... }
    public int getProtoValue() { return value; }
}

// Интерфейс с двойными геттерами
public interface SensorReading {
    int getUnitType();              // Возвращает int
    UnitType getUnitTypeEnum();     // Возвращает унифицированный enum

    interface Builder {
        Builder setUnitType(int value);       // Установка через int
        Builder setUnitType(UnitType value);  // Установка через enum
    }
}
```

**Использование**:

```java
SensorReading reading = ctx.wrapSensorReading(proto);

// Чтение
int rawValue = reading.getUnitType();           // 0, 1, 2...
UnitType type = reading.getUnitTypeEnum();      // UNIT_CELSIUS и т.д.

// Запись (с Builder)
SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)  // Через enum
    // или
    .setUnitType(2)                      // Через int
    .build();
```

### Конфликт WIDENING

**Ситуация**: Расширение типа между версиями.

```protobuf
// v1/data.proto
message Data {
    int32 value = 1;
    float temperature = 2;
}

// v2/data.proto
message Data {
    int64 value = 1;      // Расширен до 64 бит
    double temperature = 2; // Расширен до double
}
```

**Сгенерированный код**:

```java
public interface Data {
    long getValue();        // Унифицирован как long
    double getTemperature(); // Унифицирован как double

    interface Builder {
        Builder setValue(long value);
        Builder setTemperature(double value);
    }
}

// V1 реализация автоматически расширяет значения:
class DataV1 {
    @Override
    protected long extractValue(DataProto.Data proto) {
        return (long) proto.getValue();  // int → long
    }
}

// V1 билдер проверяет диапазон:
class DataV1$Builder {
    @Override
    protected void doSetValue(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Value exceeds int range for V1");
        }
        protoBuilder.setValue((int) value);
    }
}
```

**Использование**:

```java
Data data = ctx.wrapData(proto);
long value = data.getValue();  // Всегда long

// Builder безопасно обрабатывает сужение
Data modified = data.toBuilder()
    .setValue(42L)  // OK для обеих версий
    .build();

// Ошибка при превышении диапазона V1
try {
    Data v1Modified = v1Data.toBuilder()
        .setValue(Long.MAX_VALUE)  // Слишком большое для int
        .build();
} catch (IllegalArgumentException e) {
    // "Value exceeds int range for V1"
}
```

### Конфликт STRING_BYTES

**Ситуация**: Поле `string` в одной версии и `bytes` в другой.

```protobuf
// v1/report.proto
message TelemetryReport {
    string checksum = 1;
}

// v2/report.proto
message TelemetryReport {
    bytes checksum = 1;
}
```

**Сгенерированный код**:

```java
public interface TelemetryReport {
    String getChecksum();       // String (UTF-8 для bytes)
    byte[] getChecksumBytes();  // byte[] (UTF-8 для string)

    interface Builder {
        Builder setChecksum(String value);
    }
}
```

**Использование**:

```java
TelemetryReport report = ctx.wrapTelemetryReport(proto);

// Чтение
String checksumStr = report.getChecksum();        // "abc123"
byte[] checksumBytes = report.getChecksumBytes(); // [97, 98, 99, ...]

// Запись
TelemetryReport modified = report.toBuilder()
    .setChecksum("new_checksum")
    .build();
```

### Конфликт PRIMITIVE_MESSAGE

**Ситуация**: Примитив в одной версии, сообщение в другой.

```protobuf
// v1/order.proto
message Order {
    int32 shipping_cost = 1;  // Простое значение
}

// v2/order.proto
message Order {
    Money shipping_cost = 1;  // Сложный объект
}
```

**Сгенерированный код**:

```java
public interface Order {
    int getShippingCost();           // Примитивное значение
    Money getShippingCostMessage();  // Сообщение (null для примитивных версий)

    // Builder НЕ генерируется для таких полей
}
```

**Использование**:

```java
Order order = ctx.wrapOrder(proto);

// V1
int cost = order.getShippingCost();  // 100
Money costMsg = order.getShippingCostMessage();  // null

// V2
int cost = order.getShippingCost();  // 0 (по умолчанию)
Money costMsg = order.getShippingCostMessage();  // Объект Money
```

### Repeated-поля с конфликтами

Для repeated-полей с конфликтами типов поддерживается только чтение:

```java
public interface RepeatedConflicts {
    List<Long> getNumbers();    // int32 → int64 (элементы расширены)
    List<Integer> getCodes();   // int32 → enum (enum.getNumber())
    List<String> getTexts();    // string → bytes (UTF-8 конвертация)

    // Методы Builder для таких полей НЕ генерируются
}
```

**Обходной путь для записи repeated-полей с конфликтами**:

```java
// Прямой доступ к типизированному proto builder
var v2 = (RepeatedConflictsV2) wrapper;
var modified = new RepeatedConflictsV2(
    v2.getTypedProto().toBuilder()
        .addNumbers(12345L)
        .build()
);
```

---

## Режимы генерации

### Режим только для чтения (по умолчанию)

```xml
<configuration>
    <generateBuilders>false</generateBuilders>
</configuration>
```

Генерируются только геттеры. Подходит для:
- Чтения и сериализации proto-сообщений
- Интеграции с устаревшими системами
- Минимального размера сгенерированного кода

```java
Order order = ctx.wrapOrder(proto);

// Чтение
DateTime dateTime = order.getDateTime();
List<OrderItem> items = order.getItems();

// Сериализация
byte[] bytes = order.toBytes();
```

### Режим Builder

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>
</configuration>
```

Генерируются интерфейсы Builder для модификации. Подходит для:
- Создания новых proto-сообщений
- Модификации существующих обёрток
- Полной CRUD-функциональности

```java
// Модификация существующего
Order modified = order.toBuilder()
    .setCustomerId("CUST-123")
    .setTotalAmount(money)
    .build();

// Создание нового
Order newOrder = Order.newBuilder()
    .setOrderId("ORD-456")
    .setCustomerId("CUST-789")
    .build();
```

---

## Типичные сценарии использования

### Сценарий 1: Миграция версий API

```java
public class OrderMigrationService {

    public Order migrateToV2(Order v1Order) {
        // Сериализация V1
        byte[] bytes = v1Order.toBytes();

        // Парсинг как V2
        OrderV2Proto proto = OrderV2Proto.parseFrom(bytes);

        // Обёртка как V2
        return new OrderV2(proto);
    }
}
```

### Сценарий 2: Версионированный API-эндпоинт

```java
@RestController
public class OrderController {

    @PostMapping("/api/orders")
    public ResponseEntity<byte[]> createOrder(
            @RequestBody byte[] protoBytes,
            @RequestHeader("X-API-Version") int version) {

        VersionContext ctx = VersionContext.forVersion(version);

        // Парсинг с правильной версией
        Order order = ctx.wrapOrder(parseProto(protoBytes, version));

        // Бизнес-логика
        Order processed = processOrder(order);

        // Ответ в той же версии
        return ResponseEntity.ok(processed.toBytes());
    }
}
```

### Сценарий 3: Агрегация данных из нескольких версий

```java
public class OrderAggregator {

    public OrderSummary aggregate(List<Order> orders) {
        long totalAmount = 0;
        int itemCount = 0;

        for (Order order : orders) {
            // Унифицированный API работает независимо от версии
            totalAmount += order.getTotalAmount();
            itemCount += order.getItems().size();
        }

        return new OrderSummary(totalAmount, itemCount);
    }
}
```

### Сценарий 4: Тестирование с несколькими версиями

```java
@ParameterizedTest
@ValueSource(ints = {1, 2})
void testOrderProcessing(int version) {
    // Создание тестовых данных для каждой версии
    VersionContext ctx = VersionContext.forVersion(version);

    Order order = createTestOrder(ctx);

    // Тестирование бизнес-логики
    Order result = orderService.process(order);

    // Проверки работают для любой версии
    assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
}
```

---

## Решение проблем

### Ошибка: "protoc not found"

```
protoc not found. Please install protobuf compiler or set protocPath parameter.
```

**Решение**: Установите компилятор protobuf или укажите путь:

```xml
<configuration>
    <protocPath>/usr/local/bin/protoc</protocPath>
</configuration>
```

### Ошибка: "Cannot resolve symbol" в IDE

IDE может показывать ошибки для `${os.detected.classifier}` - это нормально.

**Решение**: Запустите `mvn compile` из командной строки.

### Предупреждение: "Type conflict for field"

```
[WARN] Type conflict for field 'value' [WIDENING]: v1:int, v2:long
```

Это информационное сообщение. Плагин автоматически обрабатывает конфликт.

### Не генерируется setter Builder для поля

Проверьте тип конфликта. Сеттеры Builder не генерируются для:
- Конфликтов `PRIMITIVE_MESSAGE`
- Конфликтов `INCOMPATIBLE`
- Repeated-полей с конфликтами типов

**Обходной путь**: Используйте типизированный proto builder напрямую.

### Enum не найден в унифицированном API

Убедитесь, что enum определён идентично в обеих версиях:
- Одинаковые имена значений
- Одинаковые числовые коды

Плагин автоматически определяет эквивалентные enum'ы.

---

## См. также

- [VERSION_AGNOSTIC_API.ru.md](VERSION_AGNOSTIC_API.ru.md) - Детальное описание Version-Agnostic API
- [KNOWN_ISSUES.ru.md](KNOWN_ISSUES.ru.md) - Известные ограничения

