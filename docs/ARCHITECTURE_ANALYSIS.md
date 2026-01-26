# Архитектурный анализ

Комплексный архитектурный анализ кодовой базы proto-wrapper-plugin.

**Дата анализа:** 2025-01-26
**Версия:** 2.3.1
**Общая оценка:** 7.5/10

---

## Содержание

1. [Структура модулей](#1-структура-модулей)
2. [Ключевые классы и ответственности](#2-ключевые-классы-и-ответственности)
3. [Паттерны проектирования](#3-паттерны-проектирования)
4. [Выявленные проблемы](#4-выявленные-проблемы)
5. [Точки расширения](#5-точки-расширения)
6. [Рекомендации](#6-рекомендации)
7. [Метрики качества кода](#7-метрики-качества-кода)

---

## 1. Структура модулей

### Архитектура проекта

```
proto-wrapper-plugin (root, v2.3.1)
├── proto-wrapper-core (основная библиотека)
│   ├── analyzer/        - Парсинг и анализ proto
│   ├── generator/       - Генерация кода (~7913 строк)
│   │   ├── conflict/    - Обработчики конфликтов типов (~7617 строк)
│   │   ├── versioncontext/
│   │   ├── metadata/
│   │   ├── visitor/
│   │   ├── wellknown/
│   │   └── builder/
│   ├── merger/          - Объединение версий
│   ├── model/           - Доменные модели (14 классов)
│   ├── contract/        - Контракты полей
│   ├── diff/            - Движок сравнения схем
│   ├── incremental/     - Инкрементальная сборка
│   ├── exception/       - Иерархия исключений (9 типов)
│   ├── cli/             - CLI инструменты
│   └── runtime/         - Runtime интерфейсы
│
├── proto-wrapper-maven-plugin
│   └── mojo/            - Maven goals
│
├── proto-wrapper-gradle-plugin
│   └── kotlin/          - Gradle tasks
│
├── proto-wrapper-spring-boot-starter
│
├── examples/
│   ├── maven-example/
│   └── gradle-example/
│
└── tests/
    ├── proto-wrapper-golden-tests/
    ├── proto-wrapper-maven-integration-tests/
    └── proto-wrapper-gradle-integration-tests/
```

### Зависимости

| Зависимость | Версия | Назначение |
|-------------|--------|------------|
| protobuf-java | 4.28.2 | Парсинг proto дескрипторов |
| javapoet | 1.13.0 | Генерация Java кода |
| picocli | 4.7.5 | CLI для diff инструмента |
| commons-lang3 | 3.17.0 | Утилиты |
| JUnit 5 | 5.10.1 | Тестирование |
| AssertJ | 3.24.2 | Утверждения в тестах |

---

## 2. Ключевые классы и ответственности

### 2.1 Анализаторы (`analyzer/`)

| Класс | Ответственность |
|-------|-----------------|
| `ProtoAnalyzer` | Парсит `.pb` дескрипторы → `VersionSchema` |
| `ProtocResolver` | Определяет путь к protoc бинарнику |
| `ProtocExecutor` | Запускает protoc с `--descriptor_set_out` |
| `SyntaxDetector` | Определяет proto2 vs proto3 синтаксис |

### 2.2 Генераторы (`generator/`)

| Класс | Строк | Ответственность |
|-------|-------|-----------------|
| `BaseGenerator<T>` | 68 | Абстрактный базовый - конфиг, запись файлов |
| `GenerationOrchestrator` | **725** | Оркестрирует все генераторы |
| `InterfaceGenerator` | 263 | Версионно-агностичные интерфейсы |
| `AbstractClassGenerator` | **702** | Абстрактные базовые классы |
| `ImplClassGenerator` | 454 | Версионно-специфичные реализации |
| `VersionContextGenerator` | 437 | Фабрика со статическими методами |
| `EnumGenerator` | 173 | Генерация enum'ов |
| `InterfaceMethodGenerator` | 482 | Методы getters/has/supports |

### 2.3 Разрешение конфликтов (`generator/conflict/`)

**Паттерн Chain of Responsibility** - `FieldProcessingChain.java`:

```java
private static final List<ConflictHandler> HANDLERS = List.of(
    IntEnumHandler.INSTANCE,           // int ↔ enum
    EnumEnumHandler.INSTANCE,          // enum ↔ enum
    StringBytesHandler.INSTANCE,       // string ↔ bytes
    WideningHandler.INSTANCE,          // int → long
    FloatDoubleHandler.INSTANCE,       // float ↔ double
    SignedUnsignedHandler.INSTANCE,    // int32 vs uint32
    RepeatedSingleHandler.INSTANCE,    // repeated ↔ singular
    PrimitiveMessageHandler.INSTANCE,
    RepeatedConflictHandler.INSTANCE,
    WellKnownTypeHandler.INSTANCE,
    RepeatedWellKnownTypeHandler.INSTANCE,
    MapFieldHandler.INSTANCE,
    DefaultHandler.INSTANCE
);
```

| Класс | Строк | Описание |
|-------|-------|----------|
| `AbstractConflictHandler` | 363 | Sealed базовый класс (Java 17) |
| `MapFieldHandler` | **910** | Обработка map полей - **GOD CLASS** |
| `CodeGenerationHelper` | 778 | Вспомогательный код |
| `RepeatedConflictHandler` | 524 | Repeated поля с конфликтами |
| `MethodSpecFactory` | 394 | Фабрика для MethodSpec |

### 2.4 Version Context (`generator/versioncontext/`)

**Компонентная архитектура** - хорошо спроектирована:

| Класс | Ответственность |
|-------|-----------------|
| `VersionContextGenerator` | Главный генератор |
| `VersionContextInterfaceComposer` | Компонует интерфейс |
| `Java8Codegen` / `Java9PlusCodegen` | Стратегии для версий Java |
| `StaticFieldsComponent` | Статические поля |
| `StaticMethodsComponent` | Статические методы |
| `InstanceMethodsComponent` | Методы экземпляра |
| `WrapMethodsComponent` | Wrap методы |
| `BuilderMethodsComponent` | Builder методы |
| `MetadataMethodsComponent` | Metadata методы |

### 2.5 Модели (`model/`)

| Класс | Тип | Описание |
|-------|-----|----------|
| `MergedSchema` | class | Объединённая схема из всех версий |
| `MergedMessage` | class | Объединённое сообщение |
| `MergedField` | class | Объединённое поле |
| `MergedOneof` | class | Oneof группа |
| `MergedEnum` | class | Объединённый enum |
| `MessageInfo` | class | Информация о сообщении одной версии |
| `FieldInfo` | class | Информация о поле |
| `EnumInfo` | class | Информация о enum |
| `FieldContract` | **record** | Контракт поведения поля |

### 2.6 Движок сравнения (`diff/`)

| Класс | Описание |
|-------|----------|
| `SchemaDiffEngine` | Сравнивает `VersionSchema` → `SchemaDiff` |
| `BreakingChangeDetector` | Обнаруживает ломающие изменения |
| `MergedSchemaDiffAdapter` | Адаптер для `MergedSchema` |
| `DiffFormatter` | Интерфейс |
| `TextDiffFormatter` | Текстовый вывод |
| `JsonDiffFormatter` | JSON вывод |
| `MarkdownDiffFormatter` | Markdown вывод |

### 2.7 Инкрементальная сборка (`incremental/`)

| Класс | Описание |
|-------|----------|
| `IncrementalStateManager` | Координирует инкрементальное состояние |
| `IncrementalState` | Сохраняет состояние в JSON |
| `ChangeDetector` | Обнаруживает изменения в proto файлах |
| `ProtoDependencyGraph` | Граф зависимостей |
| `CacheLock` | File-based блокировка |

---

## 3. Паттерны проектирования

### 3.1 Используемые паттерны

| Паттерн | Местоположение | Оценка |
|---------|----------------|--------|
| **Chain of Responsibility** | `FieldProcessingChain` + 13 обработчиков | Отлично |
| **Builder** | `GeneratorConfig.builder()` | Хорошо |
| **Strategy** | `Java8Codegen` vs `Java9PlusCodegen` | Хорошо |
| **Composite** | Компоненты `VersionContextGenerator` | Отлично |
| **Visitor** | `MessageVisitor` + `MessageTraverser` | Хорошо |
| **Factory** | `VersionReferenceFactory` | Адекватно |
| **Singleton** | `FieldProcessingChain.getInstance()` | Адекватно |
| **Adapter** | `MergedSchemaDiffAdapter` | Адекватно |
| **Template Method** | `BaseGenerator` | Адекватно |
| **Sealed Classes** | `AbstractConflictHandler` (Java 17) | Отлично |

### 3.2 Оценка качества паттернов

**Хорошо реализованы:**
- Chain of Responsibility для разрешения конфликтов
- Компонентный `VersionContextGenerator`
- Sealed class иерархия для обработчиков

**Можно улучшить:**
- Registry паттерн вместо хардкод списков
- Dependency injection вместо прямого создания
- Strategy паттерн для выбора генератора

---

## 4. Выявленные проблемы

### 4.1 God Classes (Критические)

| Класс | Строк | Проблемы | Приоритет |
|-------|-------|----------|-----------|
| `MapFieldHandler` | **910** | Генерирует ВСЕ map методы (interface + builder + impl) | P0 |
| `GeneratorConfig` | **866** | Конфиг + валидация + разрешение + производные значения | P0 |
| `GenerationOrchestrator` | **725** | Оркестрирует 7+ генераторов + параллелизм + incremental | P1 |
| `AbstractClassGenerator` | **702** | Abstract классы + extract методы + builder логика | P1 |

### 4.2 Нарушения SRP

| Класс | Ответственности | Решение |
|-------|-----------------|---------|
| `MapFieldHandler` | 1. Interface методы<br>2. Builder методы<br>3. Impl методы<br>4. Разные типы map | Разбить на 3-4 класса обработчиков |
| `GeneratorConfig` | 1. Хранить конфиг<br>2. Валидировать<br>3. Разрешать конфликты<br>4. Вычислять производные | Выделить `ConfigValidator`, `ConfigResolver` |
| `GenerationOrchestrator` | 1. Оркестрировать 7+ генераторов<br>2. Управлять параллелизмом<br>3. Incremental логика<br>4. Управление состоянием | Выделить `GenerationPipeline`, `IncrementalCoordinator` |

### 4.3 Нарушения OCP

| Проблема | Местоположение | Описание |
|----------|----------------|----------|
| Хардкод типов конфликтов | `FieldProcessingChain` | Новый тип требует изменения списка обработчиков |
| Хардкод списка WKT | `WellKnownTypeInfo` | Список WKT захардкожен, нет расширяемости |
| Фиксированный набор компонентов | `VersionContextGenerator` | Новый компонент требует изменения главного генератора |

### 4.4 Дублирование кода

| Дублированный код | Статус | Решение |
|-------------------|--------|---------|
| Разрешение типов | РЕШЕНО | `TypeResolver` |
| Утилиты генератора | РЕШЕНО | `GeneratorUtils` |
| Утилиты типов | РЕШЕНО | `TypeUtils` |
| Парсинг map key/value | Частично | Консолидировать в `TypeUtils` |

### 4.5 Жёсткие зависимости

| Проблема | Файл | Решение |
|----------|------|---------|
| Прямое создание обработчиков | `FieldProcessingChain` | Статический список - хорошо, но не динамично |
| Прямое создание генераторов | `GenerationOrchestrator` | Хардкод `new GeneratorX()` - нет DI |
| Зависимость от JavaPoet | Все генераторы | Хорошо, но сложно менять реализацию |

### 4.6 Проблемы тестируемости

| Проблема | Описание |
|----------|----------|
| Сложное конструирование | `GeneratorConfig` - 866 строк, много полей |
| Много зависимостей | `GenerationOrchestrator` - нужно 7+ моков |
| Глобальные синглтоны | `FieldProcessingChain.getInstance()` |
| Сложно расширять | Компоненты `VersionContextGenerator` - нет DI |

---

## 5. Точки расширения

### 5.1 Легко расширяемые

| Функция | Сложность | Как |
|---------|-----------|-----|
| Новый обработчик конфликтов | Легко | Добавить класс в список `HANDLERS` |
| Новый форматтер diff | Легко | Реализовать интерфейс `DiffFormatter` |
| Новый маппинг типов | Легко | `customTypeMappings` в конфиге |
| Новый WKT | Средне | Изменить `WellKnownTypeInfo` |

### 5.2 Сложно расширяемые

| Функция | Сложность | Причина |
|---------|-----------|---------|
| Поддержка новой версии Java | Очень сложно | Требует изменения 50+ классов |
| Другой выходной язык | Экстремально | Нужно переписать весь пакет generator |
| Новый тип конфликта | Сложно | Требует изменений `FieldProcessingChain` + обработчик |
| Новая фаза генерации | Сложно | Требует изменений `GenerationOrchestrator` |

### 5.3 Архитектура плагинов

**Maven Plugin:**
- `GenerateMojo` (extends `AbstractMojo`)
- `DiffMojo`
- `ProtoWrapperConfig`
- `MavenLogger`

**Gradle Plugin:**
- `ProtoWrapperPlugin` (extends `Plugin<Project>`)
- `GenerateWrappersTask`
- `SchemaDiffTask`
- `ProtoWrapperExtension`
- `GradleLogger`

**Оценка:** Хорошо разделены, обе системы сборки используют core независимо.

---

## 6. Рекомендации

### 6.1 Критические (P0) - Немедленно

#### 1. Разбить `MapFieldHandler` (910 строк)

**Текущее состояние:** Один класс обрабатывает всю генерацию кода для map.

**Предложение:**
```
MapFieldHandler
├── MapFieldInterfaceHandler  (~300 строк)
├── MapFieldImplHandler       (~300 строк)
└── MapFieldBuilderHandler    (~300 строк)
```

**Выгода:** -300 строк в среднем на класс, +SRP, лучшая тестируемость

#### 2. Рефакторить `GeneratorConfig` (866 строк)

**Текущее состояние:** Монолитный класс конфигурации с валидацией, разрешением и вычислением производных значений.

**Предложение:**
```
GeneratorConfig
├── GeneratorConfigParams     (чистые данные, ~200 строк)
├── ConfigValidator           (логика валидации, ~150 строк)
├── ConfigResolver            (производные значения, ~200 строк)
└── GeneratorConfig.Builder   (паттерн builder, ~100 строк)
```

**Выгода:** -400 строк в главном классе, чёткие ответственности, проще тестировать

### 6.2 Высокий приоритет (P1) - Следующий квартал

#### 3. Оптимизировать `GenerationOrchestrator` (725 строк)

**Предложение:**
- Выделить `GenerationPipeline` для последовательности фаз
- Выделить `IncrementalCoordinator` для incremental логики
- Использовать dependency injection вместо прямого создания генераторов

#### 4. Упростить `AbstractClassGenerator` (702 строки)

**Предложение:**
- Выделить `ExtractMethodLogicGenerator`
- Использовать паттерн CompositeGenerator (как в `VersionContextGenerator`)

#### 5. Внедрить Registry паттерн

```java
public interface ConflictHandlerRegistry {
    void register(ConflictHandler handler);
    void register(ConflictHandler handler, int priority);
    List<ConflictHandler> getHandlers();
}

public interface WellKnownTypeRegistry {
    void register(WellKnownTypeInfo type);
    Optional<WellKnownTypeInfo> find(String protoType);
}
```

### 6.3 Средний приоритет (P2) - В течение года

#### 6. Внедрить Dependency Injection

**Текущее состояние:**
```java
// GenerationOrchestrator.java:80-113
new InterfaceGenerator(config, mergedSchema)
new AbstractClassGenerator(config, mergedSchema)
// ... больше прямых созданий
```

**Предложение:**
```java
@Inject InterfaceGenerator interfaceGenerator;
@Inject AbstractClassGenerator abstractClassGenerator;
// ... инжектируемые зависимости
```

#### 7. Добавить метрики и профилирование

- Инструментирование `GenerationOrchestrator`
- Профилирование узких мест в разрешении конфликтов
- Метрики времени сборки

### 6.4 Низкий приоритет (P3) - Желательно

#### 8. Мигрировать модели на Records

**Уже сделано:** `FieldContract`

**К миграции:**
- `MessageInfo` → `record MessageInfo(...)`
- `FieldInfo` → `record FieldInfo(...)`
- `EnumInfo` → `record EnumInfo(...)`

#### 9. Асинхронная генерация с Virtual Threads

**Текущее состояние:** `parallelStream` в `GenerationOrchestrator`

**Предложение:** Virtual threads (Java 21+) для лучшей конкурентности

---

## 7. Метрики качества кода

| Метрика | Значение | Оценка |
|---------|----------|--------|
| Средний размер класса | ~300 строк | Нормально (есть выбросы) |
| Максимальный размер класса | 910 строк (`MapFieldHandler`) | Плохо |
| Количество классов | 143 | Хорошо (хорошо разделены) |
| Количество интерфейсов | 8 | Адекватно |
| Sealed классы | 1 (`AbstractConflictHandler`) | Хорошая практика Java 17 |
| Покрытие тестами | ~80% (примерно) | Хорошо |
| Документация | Хороший JavaDoc | Отлично |

---

## 8. Заключение

### Сильные стороны

- Chain of Responsibility для обработчиков конфликтов (13 специализированных обработчиков)
- Компонентная архитектура `VersionContextGenerator`
- Хорошее разделение: analyzer → merger → generator → diff
- Инкрементальная сборка с file-based блокировкой
- Поддержка двух систем сборки (Maven + Gradle) с общим core
- Хороший паттерн контрактов для полей (`FieldContract` record)
- Использование фич Java 17 (sealed classes, records)
- Комплексная документация и JavaDoc

### Слабые стороны

- Три God Class: `MapFieldHandler` (910), `GeneratorConfig` (866), `GenerationOrchestrator` (725)
- Нарушения SRP в вышеупомянутых классах
- Хардкод списки вместо Registry паттерна
- Отсутствие Dependency Injection
- Ограниченная расширяемость для новых типов конфликтов
- Сложно поддерживать другие выходные языки

### Итоговый вердикт

**7.5/10** - Хорошо спроектированная система с хорошим использованием паттернов, но есть стратегические проблемы с размерами классов и архитектурой оркестратора. После исправления P0 → **8.5/10**.

---

## Приложение: Ссылки на файлы

| Проблема | Путь к файлу |
|----------|--------------|
| God Class | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/conflict/MapFieldHandler.java` |
| God Class | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/GeneratorConfig.java` |
| God Class | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/GenerationOrchestrator.java` |
| Chain паттерн | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/conflict/FieldProcessingChain.java` |
| Composite паттерн | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/versioncontext/VersionContextGenerator.java` |
| Хорошее выделение | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/TypeResolver.java` |
| Хорошее выделение | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/GeneratorUtils.java` |
| Хорошее выделение | `proto-wrapper-core/src/main/java/io/alnovis/protowrapper/generator/TypeUtils.java` |
