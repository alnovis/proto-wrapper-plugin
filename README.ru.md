# Proto Wrapper Maven Plugin

[English version](README.md)

Maven-плагин для генерации версионно-независимых Java wrapper-классов из нескольких версий protobuf-схем.

## Возможности

- Автоматический парсинг `.proto` файлов через `protoc`
- Слияние нескольких версий схем в единый API
- Генерация:
  - Версионно-независимых интерфейсов
  - Абстрактных базовых классов (паттерн Template Method)
  - Версионно-специфичных реализаций
  - VersionContext для фабричных операций
  - Паттерн Builder для модификации (опционально)
- Автоматическая обработка конфликтов типов:
  - `INT_ENUM`: int ↔ enum конвертация
  - `WIDENING`: int → long, float → double
  - `STRING_BYTES`: string ↔ bytes (UTF-8)
  - `PRIMITIVE_MESSAGE`: primitive ↔ message
- Автоматическое обнаружение эквивалентных enum'ов (nested vs top-level)
- Информация о поддерживаемых версиях в Javadoc
- Потокобезопасные неизменяемые обёртки

## Документация

| Документ | Описание |
|----------|----------|
| [COOKBOOK.ru.md](docs/COOKBOOK.ru.md) | Практическое руководство с примерами |
| [VERSION_AGNOSTIC_API.ru.md](docs/VERSION_AGNOSTIC_API.ru.md) | Детальная документация API |
| [KNOWN_ISSUES.ru.md](docs/KNOWN_ISSUES.ru.md) | Известные ограничения и обходные пути |

## Установка

```bash
cd proto-wrapper-plugin
mvn install
```

## Быстрый старт

### 1. Добавьте плагин в pom.xml

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.4</version>
    <configuration>
        <basePackage>com.mycompany.myapp.model</basePackage>
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

### 2. Организуйте proto-файлы

```
src/main/proto/
├── v1/
│   ├── common.proto
│   └── order.proto
└── v2/
    ├── common.proto
    └── order.proto
```

### 3. Сгенерируйте код

```bash
mvn generate-sources
```

### 4. Используйте API

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

## Структура генерируемого кода

При `basePackage=com.mycompany.myapp.model`:

```
target/generated-sources/proto-wrapper/
├── com/mycompany/myapp/model/api/
│   ├── Money.java                    # Интерфейс
│   ├── Order.java                    # Интерфейс с nested интерфейсами
│   ├── PaymentTypeEnum.java          # Унифицированный enum
│   ├── VersionContext.java           # Фабричный интерфейс
│   └── impl/
│       ├── AbstractMoney.java        # Абстрактный базовый класс
│       └── AbstractOrder.java
├── com/mycompany/myapp/model/v1/
│   ├── MoneyV1.java                  # Реализация
│   ├── OrderV1.java
│   └── VersionContextV1.java
└── com/mycompany/myapp/model/v2/
    ├── MoneyV2.java
    ├── OrderV2.java
    └── VersionContextV2.java
```

## Конфигурация

### Основные параметры

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `basePackage` | (обязательный) | Базовый пакет для всех генерируемых классов |
| `protoRoot` | (обязательный) | Корневая директория с proto файлами |
| `versions` | (обязательный) | Список конфигураций версий |
| `outputDirectory` | `target/generated-sources/proto-wrapper` | Директория вывода |
| `protoPackagePattern` | `{basePackage}.proto.{version}` | Паттерн пакета для proto-классов |
| `generateBuilders` | `false` | Генерировать паттерн Builder |
| `protobufMajorVersion` | `3` | Версия Protobuf (2 или 3) |
| `includeVersionSuffix` | `true` | Включать суффикс версии (MoneyV1 vs Money) |
| `includeMessages` | (все) | Список имён сообщений для включения |
| `excludeMessages` | (нет) | Список имён сообщений для исключения |

### Параметры версии

| Параметр | Описание |
|----------|----------|
| `protoDir` | Директория с proto файлами относительно `protoRoot` |
| `name` | Имя версии (по умолчанию uppercase: `v1` → `V1`) |
| `excludeProtos` | Список proto файлов для исключения |

### Расширенная конфигурация

```xml
<configuration>
    <basePackage>com.mycompany.myapp.model</basePackage>
    <protoRoot>${basedir}/proto</protoRoot>
    <protoPackagePattern>com.mycompany.myapp.proto.{version}</protoPackagePattern>
    <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>

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

    <includeMessages>
        <message>Order</message>
        <message>Customer</message>
    </includeMessages>
</configuration>
```

## Обработка конфликтов типов

Плагин автоматически обрабатывает ситуации, когда тип поля различается между версиями:

| Тип конфликта | Пример | Read API | Builder API |
|---------------|--------|----------|-------------|
| `NONE` | Одинаковый тип | Обычный getter | Обычный setter |
| `INT_ENUM` | int ↔ enum | `getXxx()` + `getXxxEnum()` | `setXxx(int)` + `setXxx(Enum)` |
| `WIDENING` | int → long | Авто-расширение | Setter с проверкой диапазона |
| `NARROWING` | long → int | Использует широкий тип | Setter с проверкой диапазона |
| `STRING_BYTES` | string ↔ bytes | `getXxx()` + `getXxxBytes()` | `setXxx(String)` |
| `PRIMITIVE_MESSAGE` | int → Message | `getXxx()` + `getXxxMessage()` | Не генерируется |
| `INCOMPATIBLE` | string ↔ int | Возвращает default | Не генерируется |

Подробные примеры см. в [COOKBOOK.ru.md](docs/COOKBOOK.ru.md).

## Поддержка Builder

Включите генерацию Builder для создания и модификации сообщений:

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
</configuration>
```

### Использование

```java
// Модификация существующей обёртки
Order modified = order.toBuilder()
    .setCustomerId("CUST-123")
    .setTotalAmount(10000L)
    .build();

// Создание новой обёртки
Order newOrder = ctx.newOrderBuilder()
    .setOrderId("ORD-456")
    .setCustomerId("CUST-789")
    .build();
```

### Совместимость версий Protobuf

| protobufMajorVersion | Метод конвертации Enum |
|---------------------|----------------------|
| `2` | `EnumType.valueOf(int)` |
| `3` (по умолчанию) | `EnumType.forNumber(int)` |

## Примеры сгенерированного кода

### Интерфейс

```java
public interface Money {
    long getBills();
    int getCoins();

    int getWrapperVersion();
    byte[] toBytes();
    Message getProto();

    // При generateBuilders=true
    Builder toBuilder();

    interface Builder {
        Builder setBills(long value);
        Builder setCoins(int value);
        Money build();
    }
}
```

### Абстрактный класс (паттерн Template Method)

```java
public abstract class AbstractMoney<P extends Message> implements Money {
    protected final P proto;

    protected AbstractMoney(P proto) {
        this.proto = proto;
    }

    protected abstract long extractBills(P proto);
    protected abstract int extractCoins(P proto);

    @Override
    public final long getBills() {
        return extractBills(proto);
    }

    @Override
    public final int getCoins() {
        return extractCoins(proto);
    }
}
```

### VersionContext

```java
// Получение контекста для версии
VersionContext ctx = VersionContext.forVersion(2);

// Обёртывание proto-сообщения
Money money = ctx.wrapMoney(protoMessage);
Order order = ctx.wrapOrder(orderProto);

// Создание нового builder (при generateBuilders=true)
Order.Builder builder = ctx.newOrderBuilder();
```

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    Proto Files                              │
│                    v1/*.proto, v2/*.proto                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  ProtocExecutor → ProtoAnalyzer → VersionMerger             │
│         Парсит proto файлы, объединяет в единую схему       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                GenerationOrchestrator                       │
│         Координирует все генераторы кода                    │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┬───────────────┐
              ▼               ▼               ▼               ▼
       InterfaceGen    AbstractClassGen   ImplClassGen   VersionContextGen
              │               │               │               │
              └───────────────┴───────────────┴───────────────┘
                              │
                              ▼
                         JavaPoet → .java файлы
```

### Архитектура обработки конфликтов

```
┌─────────────────────────────────────────────────────────────┐
│                  FieldProcessingChain                       │
│         Распределяет поля по соответствующим обработчикам   │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
   IntEnumHandler      WideningHandler      StringBytesHandler
         │                    │                    │
         ▼                    ▼                    ▼
   PrimitiveMessageHandler   DefaultHandler   RepeatedConflictHandler
```

## Ограничения

Полную документацию см. в [KNOWN_ISSUES.ru.md](docs/KNOWN_ISSUES.ru.md).

**Краткое резюме:**
- Поля `oneof`: не обрабатываются специально
- Поля `map`: базовая поддержка
- Extensions (proto2): не поддерживаются
- Well-known types (google.protobuf.*): обрабатываются как обычные сообщения
- Конвертация версий (`asVersion`): не реализована
- Repeated-поля с конфликтами: только чтение (нет builder setters)

## Разработка

```bash
# Сборка
mvn clean install

# Запуск тестов (106 тестов)
mvn test

# Сборка без тестов
mvn install -DskipTests
```

## См. также

- [COOKBOOK.ru.md](docs/COOKBOOK.ru.md) - Практическое руководство
- [VERSION_AGNOSTIC_API.ru.md](docs/VERSION_AGNOSTIC_API.ru.md) - Детальная документация API
- [KNOWN_ISSUES.ru.md](docs/KNOWN_ISSUES.ru.md) - Известные ограничения и обходные пути
- [examples/maven-example](examples/maven-example) - Рабочий пример проекта

