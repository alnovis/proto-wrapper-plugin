# Proto Wrapper Maven Plugin

Maven-плагин для генерации версионно-независимых Java wrapper-классов из нескольких версий protobuf-схем.

## Возможности

- Автоматический парсинг `.proto` файлов через `protoc`
- Слияние нескольких версий схем в единый API
- Генерация:
  - Версионно-независимых интерфейсов
  - Абстрактных базовых классов (паттерн Template Method)
  - Версионно-специфичных реализаций
  - VersionContext для фабричных операций
- Автоматическое обнаружение эквивалентных enum'ов (nested vs top-level)
- Обработка конфликтов типов между версиями (int→Long, несовместимые типы)
- Информация о поддерживаемых версиях в Javadoc

## Установка

```bash
cd proto-wrapper-plugin
mvn install
```

## Использование

### Минимальная конфигурация (рекомендуется)

Укажите `basePackage`, `protoRoot` и директории с proto файлами — плагин сделает остальное:

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <basePackage>com.mycompany.myapp.model</basePackage>
        <protoRoot>${basedir}/src/main/proto</protoRoot>
        <versions>
            <version>
                <protoDir>v202</protoDir>
            </version>
            <version>
                <protoDir>v203</protoDir>
            </version>
        </versions>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Классы будут сгенерированы в:
- `com.mycompany.myapp.model.api` — интерфейсы, enum'ы и абстрактные классы
- `com.mycompany.myapp.model.v202` — реализации для v202
- `com.mycompany.myapp.model.v203` — реализации для v203

Плагин автоматически:
1. Найдёт все `.proto` файлы в каждой директории
2. Вызовет `protoc` для генерации дескрипторов
3. Проанализирует proto файлы и создаст маппинги классов
4. Сгенерирует все wrapper-классы

### Запуск генерации

```bash
mvn generate-sources
# или
mvn compile
```

### Расширенная конфигурация

```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Базовый пакет для генерируемых классов -->
        <basePackage>com.mycompany.myapp.model</basePackage>

        <!-- Корневая директория с proto файлами -->
        <protoRoot>${basedir}/proto</protoRoot>

        <!-- Паттерн пакета для proto-классов (сгенерированных protoc) -->
        <protoPackagePattern>com.mycompany.myapp.proto.{version}</protoPackagePattern>

        <!-- Директория для сгенерированных файлов -->
        <outputDirectory>${project.build.directory}/generated-sources/proto-wrapper</outputDirectory>

        <versions>
            <version>
                <!-- Директория относительно protoRoot -->
                <protoDir>v202</protoDir>
                <!-- Опционально: имя версии (по умолчанию — uppercase от protoDir) -->
                <name>V202</name>
                <!-- Опционально: исключить определённые proto файлы -->
                <excludeProtos>
                    <excludeProto>internal.proto</excludeProto>
                    <excludeProto>deprecated.proto</excludeProto>
                </excludeProtos>
            </version>
            <version>
                <protoDir>v203</protoDir>
            </version>
        </versions>
    </configuration>
</plugin>
```

## Структура генерируемого кода

При `basePackage=com.mycompany.myapp.model`:

```
target/generated-sources/proto-wrapper/
├── com/mycompany/myapp/model/api/
│   ├── Money.java                    # Интерфейс
│   ├── DateTime.java                 # Интерфейс
│   ├── TicketRequest.java            # Интерфейс с nested интерфейсами
│   ├── PaymentTypeEnum.java          # Enum
│   ├── VersionContext.java           # Фабричный интерфейс
│   └── impl/
│       ├── AbstractMoney.java        # Абстрактный базовый класс
│       ├── AbstractDateTime.java
│       └── AbstractTicketRequest.java
├── com/mycompany/myapp/model/v202/
│   ├── MoneyV202.java                # Реализация
│   ├── DateTimeV202.java
│   ├── TicketRequestV202.java
│   └── VersionContextV202.java
└── com/mycompany/myapp/model/v203/
    ├── MoneyV203.java
    ├── DateTimeV203.java
    ├── TicketRequestV203.java
    └── VersionContextV203.java
```

## Примеры сгенерированного кода

### Интерфейс

```java
/**
 * Version-agnostic interface for Money.
 *
 * <p>Supported in versions: [v202, v203]</p>
 */
public interface Money {
    long getBills();
    int getCoins();

    /** @return Protocol version (e.g., 202, 203) */
    int getWrapperVersion();

    /** Serialize to protobuf bytes. */
    byte[] toBytes();

    /** Convert to a specific version implementation. */
    <T extends Money> T asVersion(Class<T> versionClass);
}
```

### Абстрактный класс

```java
public abstract class AbstractMoney<PROTO extends Message> implements Money {
    protected final PROTO proto;

    protected AbstractMoney(PROTO proto) {
        this.proto = proto;
    }

    protected abstract long extractBills(PROTO proto);
    protected abstract int extractCoins(PROTO proto);

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

### Реализация

```java
public class MoneyV202 extends AbstractMoney<Common.Money> {

    public MoneyV202(Common.Money proto) {
        super(proto);
    }

    @Override
    protected long extractBills(Common.Money proto) {
        return proto.getBills();
    }

    @Override
    protected int extractCoins(Common.Money proto) {
        return proto.getCoins();
    }

    public static MoneyV202 from(Common.Money proto) {
        return new MoneyV202(proto);
    }
}
```

### Enum с информацией о версиях

```java
/**
 * Version-agnostic enum for PaymentTypeEnum.
 *
 * <p>Supported in versions: [v202, v203]</p>
 */
public enum PaymentTypeEnum {
    CASH(0),
    CARD(1),

    /**
     * Present only in versions: [v202]
     */
    CREDIT(2),

    /**
     * Present only in versions: [v202]
     */
    TARE(3),

    MOBILE(4);

    // ...
}
```

### VersionContext

```java
// Получение контекста для версии
VersionContext ctx = VersionContext.forVersion(203);

// Обёртывание proto-сообщения
Money money = ctx.wrapMoney(protoMessage);
TicketRequest ticket = ctx.wrapTicketRequest(ticketProto);
```

## Параметры конфигурации

### Основные параметры

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `basePackage` | — | Базовый пакет для всех генерируемых классов |
| `protoRoot` | — | Корневая директория с proto файлами |
| `versions` | (обязательный) | Список конфигураций версий |
| `outputDirectory` | `${project.build.directory}/generated-sources/proto-wrapper` | Директория для сгенерированных файлов |
| `protoPackagePattern` | `{basePackage}.proto.{version}` | Паттерн пакета для proto-классов |

### Параметры версии

| Параметр | Описание |
|----------|----------|
| `protoDir` | Директория с proto файлами относительно `protoRoot` |
| `name` | Имя версии (по умолчанию — uppercase от `protoDir`, например `v202` → `V202`) |
| `excludeProtos` | Список proto файлов для исключения |

### Вычисляемые пакеты

При установке `basePackage` остальные пакеты вычисляются автоматически:
- `apiPackage` = `basePackage` + `.api`
- `implPackagePattern` = `basePackage` + `.{version}`

## Обработка различий между версиями

### Конфликты типов

Плагин автоматически обрабатывает ситуации, когда тип поля различается между версиями:

| Ситуация | Решение |
|----------|---------|
| `int` в v202 → `long` в v203 | Используется `long`, в v202 применяется cast `(long)` |
| `int` в v202 → `enum` в v203 | Используется `int`, enum конвертируется через `getNumber()` |
| Несовместимые типы (message vs primitive) | Поле помечается как отсутствующее в конфликтующей версии |

### Эквивалентные enum'ы

Если enum определён как nested в одной версии и как top-level в другой:

```protobuf
// v202: nested enum
message NomenclatureResponse {
  enum TaxTypeEnum { VAT = 100; }
}

// v203: top-level enum (в отдельном файле)
enum TaxTypeEnum { VAT = 100; }
```

Плагин автоматически:
1. Обнаружит эквивалентность по имени и значениям
2. Использует единый top-level enum
3. Удалит дублирующийся nested enum

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    Proto Files                               │
│                  v202/*.proto, v203/*.proto                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    protoc                                    │
│            Генерирует FileDescriptorSet (.pb)               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ProtoAnalyzer                             │
│          Парсит дескрипторы в VersionSchema                 │
│          Фильтрует по sourcePrefix (директории)             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    VersionMerger                             │
│         Объединяет схемы в единую MergedSchema              │
│         Обнаруживает эквивалентные enum'ы                   │
│         Обрабатывает конфликты типов                        │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┬───────────────┐
              ▼               ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│    Interface    │ │  AbstractClass  │ │    ImplClass    │ │ VersionContext  │
│    Generator    │ │   Generator     │ │   Generator     │ │   Generator     │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
              │               │               │               │
              └───────────────┴───────────────┴───────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    JavaPoet                                  │
│              Генерирует .java исходные файлы                │
└─────────────────────────────────────────────────────────────┘
```

## Ограничения

- Поля `oneof` не поддерживаются
- Поля `map` имеют базовую поддержку
- Сложные иерархии вложенных сообщений могут потребовать ручной настройки

## Разработка

```bash
# Сборка
mvn clean install

# Запуск тестов
mvn test

# Сборка без тестов
mvn install -DskipTests
```

## Пример использования сгенерированных классов

```java
// Парсинг proto-сообщения
byte[] protoBytes = ...;
Ticket.TicketRequest protoRequest = Ticket.TicketRequest.parseFrom(protoBytes);

// Определение версии и обёртывание
int version = 203;
VersionContext ctx = VersionContext.forVersion(version);
TicketRequest request = ctx.wrapTicketRequest(protoRequest);

// Использование версионно-независимого API
DateTime dateTime = request.getDateTime();
List<TicketRequest.Item> items = request.getItems();
OperationTypeEnum operation = request.getOperation();

// Сериализация обратно в proto
byte[] outputBytes = request.toBytes();
```
