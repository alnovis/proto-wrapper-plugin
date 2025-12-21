# Version-Agnostic API

[English version](VERSION_AGNOSTIC_API.md)

Исчерпывающее руководство по использованию версионно-независимого API, генерируемого proto-wrapper-maven-plugin.

## Содержание

- [Обзор](#обзор)
- [Архитектура](#архитектура)
- [Режим только для чтения](#режим-только-для-чтения)
- [Режим Builder](#режим-builder)
- [VersionContext](#versioncontext)
- [Работа с вложенными сообщениями](#работа-с-вложенными-сообщениями)
- [Обработка Enum](#обработка-enum)
- [Сериализация](#сериализация)
- [Лучшие практики](#лучшие-практики)

---

## Обзор

Версионно-независимый API предоставляет унифицированный интерфейс для работы с несколькими версиями protobuf-сообщений. Вместо написания версионно-специфичного кода, вы работаете с общими интерфейсами, которые абстрагируют различия.

### Ключевые преимущества

- **Единая кодовая база**: Пишете бизнес-логику один раз, работает со всеми версиями
- **Типобезопасность**: Проверки на этапе компиляции предотвращают несоответствия версий
- **Автоматическая конвертация**: Конфликты типов обрабатываются прозрачно
- **Расширяемость**: Легко добавлять новые версии без изменения существующего кода

### Генерируемые компоненты

| Компонент | Описание |
|-----------|----------|
| **Интерфейсы** | Контракты версионно-независимого API (пакет `api`) |
| **Абстрактные классы** | Общая логика реализации (пакет `api`) |
| **Классы реализации** | Версионно-специфичные реализации (пакеты `v1`, `v2`) |
| **VersionContext** | Фабрика для создания обёрток (пакет `api`) |
| **Enum'ы** | Унифицированные перечисления из всех версий |

---

## Архитектура

### Структура пакетов

```
com.example.model/
├── api/
│   ├── Order.java              (интерфейс)
│   ├── AbstractOrder.java      (абстрактный класс)
│   ├── OrderItem.java          (интерфейс)
│   ├── AbstractOrderItem.java  (абстрактный класс)
│   ├── PaymentType.java        (унифицированный enum)
│   └── VersionContext.java     (фабрика)
├── v1/
│   ├── OrderV1.java            (реализация)
│   └── OrderItemV1.java        (реализация)
└── v2/
    ├── OrderV2.java            (реализация)
    └── OrderItemV2.java        (реализация)
```

### Иерархия классов

```
                    <<interface>>
                       Order
                         │
                         ▼
                  AbstractOrder<P>
                    /         \
                   /           \
              OrderV1         OrderV2
         (proto: v1.Order)  (proto: v2.Order)
```

### Паттерн Template Method

Абстрактный класс использует паттерн Template Method, определяя скелет алгоритма, делегируя версионно-специфичное извлечение подклассам:

```java
// AbstractOrder.java
public abstract class AbstractOrder<P extends Message> implements Order {
    protected final P proto;

    // Шаблонный метод - одинаков для всех версий
    @Override
    public final String getCustomerId() {
        return hasCustomerId(proto) ? extractCustomerId(proto) : null;
    }

    // Абстрактные методы - реализуются версионно-специфичными классами
    protected abstract boolean hasCustomerId(P proto);
    protected abstract String extractCustomerId(P proto);
}
```

---

## Режим только для чтения

Режим только для чтения (по умолчанию) генерирует обёртки без возможности модификации.

### Конфигурация

```xml
<configuration>
    <generateBuilders>false</generateBuilders>
</configuration>
```

### Генерируемый интерфейс

```java
public interface Order {
    // Скалярные поля
    String getOrderId();
    String getCustomerId();
    long getTotalAmount();

    // Опциональные поля с has-методами
    boolean hasShippingAddress();
    Address getShippingAddress();

    // Repeated-поля
    List<OrderItem> getItems();

    // Enum-поля
    PaymentType getPaymentType();

    // Сериализация
    byte[] toBytes();
    Message getProto();
}
```

### Пример использования

```java
// Парсинг protobuf bytes
OrderProto proto = OrderProto.parseFrom(protoBytes);

// Оборачивание с version context
VersionContext ctx = VersionContext.forVersion(1);
Order order = ctx.wrapOrder(proto);

// Использование унифицированного API
String customerId = order.getCustomerId();
List<OrderItem> items = order.getItems();
PaymentType payment = order.getPaymentType();

// Вложенные сообщения тоже обёрнуты
for (OrderItem item : items) {
    String productId = item.getProductId();
    int quantity = item.getQuantity();
    Money price = item.getPrice();
}

// Сериализация обратно в bytes
byte[] outputBytes = order.toBytes();
```

### Прямое создание

Вы также можете создавать обёртки напрямую без VersionContext:

```java
// V1 обёртка
Order orderV1 = new OrderV1(v1Proto);

// V2 обёртка
Order orderV2 = new OrderV2(v2Proto);
```

---

## Режим Builder

Режим Builder генерирует интерфейсы и реализации для создания и модификации protobuf-сообщений.

### Конфигурация

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>
</configuration>
```

### Генерируемый интерфейс Builder

```java
public interface Order {
    // ... getter-методы ...

    // Конвертация в builder
    Builder toBuilder();

    // Вложенный интерфейс builder
    interface Builder {
        // Скалярные сеттеры
        Builder setOrderId(String value);
        Builder setCustomerId(String value);
        Builder setTotalAmount(long value);

        // Сеттеры опциональных полей
        Builder setShippingAddress(Address value);
        Builder clearShippingAddress();

        // Манипуляция repeated-полями
        Builder addItem(OrderItem item);
        Builder addAllItems(List<OrderItem> items);
        Builder clearItems();

        // Enum-сеттеры
        Builder setPaymentType(PaymentType value);

        // Построение обёртки
        Order build();
    }
}
```

### Создание новых сообщений

```java
// Получение builder из VersionContext
Order.Builder builder = VersionContext.forVersion(1).newOrderBuilder();

// Построение заказа
Order order = builder
    .setOrderId("ORD-001")
    .setCustomerId("CUST-123")
    .setTotalAmount(10000L)
    .setPaymentType(PaymentType.CREDIT_CARD)
    .build();

// Сериализация
byte[] bytes = order.toBytes();
```

### Модификация существующих сообщений

```java
// Оборачивание существующего proto
Order order = ctx.wrapOrder(proto);

// Создание модифицированной копии
Order modified = order.toBuilder()
    .setTotalAmount(order.getTotalAmount() + 500L)
    .setPaymentType(PaymentType.BANK_TRANSFER)
    .build();
```

### Добавление вложенных сообщений

```java
// Сначала создаём вложенное сообщение
OrderItem.Builder itemBuilder = ctx.newOrderItemBuilder();
OrderItem item = itemBuilder
    .setProductId("PROD-001")
    .setQuantity(2)
    .setPrice(money)
    .build();

// Добавляем в родительское
Order order = ctx.newOrderBuilder()
    .setOrderId("ORD-001")
    .addItem(item)
    .build();
```

### Детали реализации Builder

Builder использует похожий паттерн Template Method:

```java
// AbstractOrder$Builder.java
public abstract class AbstractOrder$Builder<P extends Message.Builder>
        implements Order.Builder {

    protected final P protoBuilder;

    @Override
    public final Order.Builder setCustomerId(String value) {
        doSetCustomerId(value);
        return this;
    }

    protected abstract void doSetCustomerId(String value);

    @Override
    public abstract Order build();
}
```

---

## VersionContext

VersionContext — это фабрика для создания версионно-специфичных обёрток и билдеров.

### Базовое использование

```java
// Получение контекста для конкретной версии
VersionContext ctx = VersionContext.forVersion(1);

// Или использование констант
VersionContext v1 = VersionContext.V1;
VersionContext v2 = VersionContext.V2;
```

### Методы оборачивания

```java
// Оборачивание protobuf-сообщений
Order order = ctx.wrapOrder(orderProto);
Customer customer = ctx.wrapCustomer(customerProto);
Product product = ctx.wrapProduct(productProto);
```

### Методы Builder (только режим Builder)

```java
// Создание новых билдеров
Order.Builder orderBuilder = ctx.newOrderBuilder();
Customer.Builder customerBuilder = ctx.newCustomerBuilder();
```

### Определение версии

```java
// Проверка версии обёртки
if (order instanceof OrderV1) {
    // Обработка V1-специфичной логики
}

// Или использование getVersion() если доступен
int version = order.getVersion();
```

### Динамический выбор версии

```java
public class OrderService {

    public Order processOrder(byte[] protoBytes, int apiVersion) {
        VersionContext ctx = VersionContext.forVersion(apiVersion);

        // Парсинг с правильным proto-классом
        Message proto = parseProto(protoBytes, apiVersion);

        // Оборачивание с унифицированным интерфейсом
        return ctx.wrapOrder(proto);
    }

    private Message parseProto(byte[] bytes, int version) {
        return switch (version) {
            case 1 -> com.example.proto.v1.OrderProto.parseFrom(bytes);
            case 2 -> com.example.proto.v2.OrderProto.parseFrom(bytes);
            default -> throw new IllegalArgumentException("Unknown version: " + version);
        };
    }
}
```

---

## Работа с вложенными сообщениями

Вложенные protobuf-сообщения автоматически оборачиваются в соответствующие wrapper-классы.

### Чтение вложенных сообщений

```java
Order order = ctx.wrapOrder(proto);

// Одиночное вложенное сообщение
Address address = order.getShippingAddress();
if (address != null) {
    String street = address.getStreet();
    String city = address.getCity();
}

// Repeated вложенные сообщения
List<OrderItem> items = order.getItems();
for (OrderItem item : items) {
    Money price = item.getPrice();
    long amount = price.getAmount();
    String currency = price.getCurrency();
}
```

### Запись вложенных сообщений (режим Builder)

```java
// Создание вложенного сообщения
Address.Builder addressBuilder = ctx.newAddressBuilder();
Address address = addressBuilder
    .setStreet("123 Main St")
    .setCity("New York")
    .setZipCode("10001")
    .build();

// Установка в родительское
Order order = ctx.newOrderBuilder()
    .setOrderId("ORD-001")
    .setShippingAddress(address)
    .build();
```

### Глубоко вложенные структуры

```java
// Доступ к глубоко вложенным полям
Order order = ctx.wrapOrder(proto);
Address shipping = order.getShippingAddress();
GeoLocation location = shipping.getLocation();
double latitude = location.getLatitude();
double longitude = location.getLongitude();
```

---

## Обработка Enum

Enum'ы объединяются из всех версий в единый Java enum.

### Генерация унифицированного Enum

Когда один и тот же enum присутствует в нескольких версиях, плагин их объединяет:

```protobuf
// v1/order.proto
enum PaymentType {
    UNKNOWN = 0;
    CASH = 1;
    CREDIT_CARD = 2;
}

// v2/order.proto
enum PaymentType {
    UNKNOWN = 0;
    CASH = 1;
    CREDIT_CARD = 2;
    BANK_TRANSFER = 3;  // Новое в v2
}
```

Сгенерированный унифицированный enum:

```java
public enum PaymentType {
    UNKNOWN(0),
    CASH(1),
    CREDIT_CARD(2),
    BANK_TRANSFER(3);  // Включён из v2

    private final int protoValue;

    public int getProtoValue() {
        return protoValue;
    }

    public static PaymentType fromProtoValue(int value) {
        // Возвращает соответствующий enum или UNKNOWN
    }
}
```

### Использование Enum

```java
// Чтение
Order order = ctx.wrapOrder(proto);
PaymentType type = order.getPaymentType();

switch (type) {
    case CASH -> handleCashPayment(order);
    case CREDIT_CARD -> handleCardPayment(order);
    case BANK_TRANSFER -> handleBankTransfer(order);
    default -> handleUnknown(order);
}

// Запись (режим Builder)
Order modified = order.toBuilder()
    .setPaymentType(PaymentType.BANK_TRANSFER)
    .build();
```

### Конфликт INT_ENUM

Когда поле `int32` в одной версии и `enum` в другой:

```java
public interface SensorReading {
    // Оба аксессора доступны
    int getUnitType();          // Сырое int-значение
    UnitType getUnitTypeEnum(); // Унифицированный enum

    interface Builder {
        Builder setUnitType(int value);
        Builder setUnitType(UnitType value);
    }
}
```

---

## Сериализация

Все обёртки поддерживают сериализацию обратно в protobuf bytes.

### Базовая сериализация

```java
Order order = ctx.wrapOrder(proto);

// В bytes
byte[] bytes = order.toBytes();

// Доступ к базовому proto
Message proto = order.getProto();
```

### Типизированный доступ к Proto

Для версионно-специфичных операций, получите типизированный proto:

```java
OrderV1 orderV1 = (OrderV1) order;
com.example.proto.v1.OrderProto typedProto = orderV1.getTypedProto();

// Использование proto-специфичных методов
typedProto.toBuilder()
    .setLegacyField("value")  // Поле только V1
    .build();
```

### Кросс-версионная сериализация

```java
// V1 заказ
Order v1Order = VersionContext.V1.wrapOrder(v1Proto);
byte[] v1Bytes = v1Order.toBytes();

// Парсинг V1 bytes как V2 (если совместимы)
com.example.proto.v2.OrderProto v2Proto =
    com.example.proto.v2.OrderProto.parseFrom(v1Bytes);
Order v2Order = VersionContext.V2.wrapOrder(v2Proto);
```

---

## Лучшие практики

### 1. Используйте VersionContext для динамического версионирования

```java
// Хорошо: Динамический выбор версии
public Order processOrder(byte[] bytes, int version) {
    VersionContext ctx = VersionContext.forVersion(version);
    return ctx.wrapOrder(parseProto(bytes, version));
}

// Избегайте: Хардкод проверок версий
public Order processOrder(byte[] bytes, int version) {
    if (version == 1) {
        return new OrderV1(V1OrderProto.parseFrom(bytes));
    } else {
        return new OrderV2(V2OrderProto.parseFrom(bytes));
    }
}
```

### 2. Программируйте на интерфейсы

```java
// Хорошо: Используйте типы интерфейсов
public void processOrder(Order order) {
    String customerId = order.getCustomerId();
    // Работает с любой версией
}

// Избегайте: Версионно-специфичные типы
public void processOrder(OrderV1 order) {
    // Работает только с V1
}
```

### 3. Безопасно обрабатывайте опциональные поля

```java
// Хорошо: Проверка наличия
if (order.hasShippingAddress()) {
    Address address = order.getShippingAddress();
    processAddress(address);
}

// Тоже хорошо: Null-проверка
Address address = order.getShippingAddress();
if (address != null) {
    processAddress(address);
}
```

### 4. Используйте паттерн Builder для модификаций

```java
// Хорошо: Иммутабельная модификация
Order modified = order.toBuilder()
    .setStatus(OrderStatus.SHIPPED)
    .build();

// Оригинальный order не изменён
assert order.getStatus() != OrderStatus.SHIPPED;
```

### 5. Обрабатывайте неизвестные значения Enum

```java
PaymentType type = order.getPaymentType();
if (type == null || type == PaymentType.UNKNOWN) {
    // Обработка с грацией
    type = PaymentType.CASH;  // По умолчанию
}
```

### 6. Версионно-специфичная логика при необходимости

```java
Order order = ctx.wrapOrder(proto);

// Когда нужно версионно-специфичное поведение
if (order instanceof OrderV2 v2Order) {
    // Доступ к V2-специфичным функциям
    String v2OnlyField = v2Order.getTypedProto().getV2OnlyField();
}
```

---

## См. также

- [COOKBOOK.ru.md](COOKBOOK.ru.md) - Практическое руководство с примерами
- [KNOWN_ISSUES.ru.md](KNOWN_ISSUES.ru.md) - Известные ограничения и обходные пути
- [README.ru.md](../README.ru.md) - Обзор проекта и быстрый старт

