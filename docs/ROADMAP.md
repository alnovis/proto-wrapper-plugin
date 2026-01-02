# Proto Wrapper Plugin Roadmap

Version: 1.1
Last Updated: 2026-01-02

This document outlines the development roadmap for upcoming releases.

---

## Table of Contents

- [Version 1.3.0](#version-130) - Well-Known Types Support
- [Version 1.4.0](#version-140) - Repeated Conflict Field Builders
- [Version 1.5.0](#version-150) - Schema Diff Tool
- [Version 1.6.0](#version-160) - Incremental Generation
- [Version 1.7.0](#version-170) - Parallel Generation
- [Version 1.8.0](#version-180) - Per-version Proto Syntax
- [Version 1.9.0](#version-190) - Validation Annotations
- [Version 2.0.0](#version-200) - Kotlin Extensions
- [Version 2.1.0](#version-210) - Service/RPC Wrappers
- [Version 2.2.0](#version-220) - API Cleanup
- [Future Considerations](#future-considerations)
- [Contributing](#contributing)

---

## Version 1.3.0

**Target:** Jan 2026
**Theme:** Well-Known Types Support

### Feature: Well-Known Types Support

**Priority:** High
**Complexity:** Medium

#### Description

Automatically convert Google Well-Known Types to Java standard library equivalents.

#### Supported Types

| Proto Type | Java Type | Notes |
|------------|-----------|-------|
| `google.protobuf.Timestamp` | `java.time.Instant` | Epoch-based |
| `google.protobuf.Duration` | `java.time.Duration` | Nanosecond precision |
| `google.protobuf.StringValue` | `String` | Nullable wrapper |
| `google.protobuf.Int32Value` | `Integer` | Nullable wrapper |
| `google.protobuf.Int64Value` | `Long` | Nullable wrapper |
| `google.protobuf.BoolValue` | `Boolean` | Nullable wrapper |
| `google.protobuf.FloatValue` | `Float` | Nullable wrapper |
| `google.protobuf.DoubleValue` | `Double` | Nullable wrapper |
| `google.protobuf.BytesValue` | `byte[]` | Nullable wrapper |

#### Generated Code Example

```java
// Proto definition
message Event {
    google.protobuf.Timestamp created_at = 1;
    google.protobuf.Duration duration = 2;
    google.protobuf.StringValue optional_name = 3;
}

// Generated interface
public interface Event {
    // Direct Java types
    Instant getCreatedAt();
    Duration getDuration();
    String getOptionalName();  // null if not set

    // Optional: Raw proto access
    Timestamp getCreatedAtProto();

    interface Builder {
        Builder setCreatedAt(Instant value);
        Builder setDuration(Duration value);
        Builder setOptionalName(String value);
    }
}
```

#### Implementation Plan

1. Create `WellKnownTypeResolver` class
   - Map proto types to Java types
   - Generate conversion code
2. Update `TypeResolver` to detect well-known types
3. Create handlers for each type:
   - `TimestampHandler`
   - `DurationHandler`
   - `WrapperTypeHandler` (for StringValue, Int32Value, etc.)
4. Add configuration option to enable/disable:
   ```xml
   <configuration>
       <convertWellKnownTypes>true</convertWellKnownTypes>
   </configuration>
   ```
5. Update documentation

#### Acceptance Criteria

- [ ] All listed types converted automatically
- [ ] Null handling for wrapper types
- [ ] Builder setters accept Java types
- [ ] Configuration option to disable
- [ ] Integration tests for all types
- [ ] Documentation updated

### Migration Notes

- No breaking changes
- New feature is opt-in (enabled by default)

---

## Version 1.4.0

**Target:** Jan 2026
**Theme:** Repeated Conflict Field Builders

### Feature: Repeated Conflict Field Builders

**Priority:** Medium
**Complexity:** Medium

#### Description

Add builder methods for repeated fields that have type conflicts across versions.

#### Current Limitation

```java
// v1: repeated int32 numbers = 1;
// v2: repeated int64 numbers = 1;

interface Message {
    List<Long> getNumbers();  // Works (read)

    interface Builder {
        // NOT generated currently
    }
}
```

#### Target Implementation

```java
interface Message {
    List<Long> getNumbers();

    interface Builder {
        Builder addNumbers(long value);
        Builder addAllNumbers(Iterable<Long> values);
        Builder setNumbers(int index, long value);
        Builder clearNumbers();
    }
}
```

#### Implementation Plan

1. Update `RepeatedConflictHandler`:
   - Add builder method generation
   - Handle type conversion in setters
   - Add range validation for narrowing conversions

2. Generate conversion logic:
   ```java
   // In V1 impl builder
   @Override
   public Builder addNumbers(long value) {
       if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
           throw new IllegalArgumentException(
               "Value " + value + " exceeds int32 range for V1");
       }
       protoBuilder.addNumbers((int) value);
       return this;
   }
   ```

3. Handle all conflict types:
   - WIDENING: Range check for narrower version
   - INT_ENUM: Convert to enum for enum version
   - STRING_BYTES: UTF-8 conversion

#### Acceptance Criteria

- [ ] All repeated conflict types have builder methods
- [ ] Range validation with clear error messages
- [ ] Type conversion handled correctly
- [ ] Integration tests for all cases
- [ ] Documentation updated

### Migration Notes

- No breaking changes
- New feature is backward compatible

---

## Version 1.5.0

**Target:** Feb 2026
**Theme:** Schema Diff Tool

### Feature: Schema Diff Tool

**Priority:** Medium
**Complexity:** Medium

#### Description

CLI and programmatic tool to compare proto schemas across versions and generate reports.

#### CLI Usage

```bash
# Compare two version directories
proto-wrapper diff proto/v1 proto/v2

# Output formats
proto-wrapper diff proto/v1 proto/v2 --format=text
proto-wrapper diff proto/v1 proto/v2 --format=json
proto-wrapper diff proto/v1 proto/v2 --format=markdown

# Check for breaking changes
proto-wrapper diff proto/v1 proto/v2 --breaking-only
```

#### Output Example (Text)

```
Schema Comparison: v1 -> v2

MESSAGES:
  + Added: PaymentMethod (payment.proto:15)
  ~ Modified: Order
      + Added field: tracking_number (string, #10)
      ~ Changed field: status
          Type: int32 -> OrderStatus (enum)
          Breaking: No (compatible via INT_ENUM)
      - Removed field: legacy_id (#99)
          Breaking: Yes (field removal)
  - Removed: DeprecatedMessage
      Breaking: Yes (message removal)

ENUMS:
  + Added: OrderStatus
      Values: PENDING(0), CONFIRMED(1), SHIPPED(2), DELIVERED(3)
  ~ Modified: PaymentType
      + Added value: CRYPTO(4)

SUMMARY:
  Added: 2 messages, 1 enum
  Modified: 2 messages, 1 enum
  Removed: 1 message
  Breaking changes: 2
```

#### Programmatic API

```java
SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

// Query differences
diff.getAddedMessages();      // List<MessageInfo>
diff.getRemovedMessages();    // List<MessageInfo>
diff.getModifiedMessages();   // List<MessageDiff>

// Check breaking changes
diff.hasBreakingChanges();    // boolean
diff.getBreakingChanges();    // List<BreakingChange>

// Export
diff.toMarkdown();            // String
diff.toJson();                // String
```

#### Maven Goal

```bash
mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2
```

#### Gradle Task

```bash
./gradlew protoWrapperDiff --v1=proto/v1 --v2=proto/v2
```

#### Implementation Plan

1. Create `schema-diff` submodule
2. Implement comparison logic:
   - Message comparison
   - Field comparison (type, number, name)
   - Enum comparison
   - Breaking change detection
3. Create output formatters:
   - TextFormatter
   - JsonFormatter
   - MarkdownFormatter
4. Add CLI entry point
5. Add Maven/Gradle integration

#### Acceptance Criteria

- [ ] Detect all types of changes
- [ ] Identify breaking vs non-breaking
- [ ] Multiple output formats
- [ ] CLI tool
- [ ] Maven goal
- [ ] Gradle task
- [ ] Programmatic API

### Migration Notes

- No breaking changes
- Schema diff tool is a new optional feature

---

## Version 1.6.0

**Target:** Feb 2026
**Theme:** Incremental Generation

### Feature: Incremental Generation

**Priority:** High
**Complexity:** High

#### Description

Only regenerate wrappers when source proto files change, significantly reducing build times for large projects.

#### Implementation Plan

1. **Input Tracking**
   - Create `.proto-wrapper-cache` directory in build output
   - Store hash of each .proto file
   - Store generation timestamp

2. **Change Detection**
   ```java
   public class IncrementalGenerator {
       private final Path cacheDir;

       public Set<Path> getChangedProtos(Set<Path> allProtos) {
           return allProtos.stream()
               .filter(this::hasChanged)
               .collect(toSet());
       }

       private boolean hasChanged(Path proto) {
           String currentHash = computeHash(proto);
           String cachedHash = readCachedHash(proto);
           return !currentHash.equals(cachedHash);
       }
   }
   ```

3. **Dependency Tracking**
   - Track proto imports
   - Regenerate dependents when dependency changes
   - Build dependency graph

4. **Selective Generation**
   - Only regenerate affected messages
   - Preserve unchanged generated files
   - Update VersionContext if any message changes

5. **Cache Invalidation**
   - Plugin version change
   - Configuration change
   - Manual clean

#### Configuration

```xml
<configuration>
    <incremental>true</incremental>
    <cacheDirectory>${project.build.directory}/proto-wrapper-cache</cacheDirectory>
</configuration>
```

```kotlin
protoWrapper {
    incremental = true
    cacheDirectory = layout.buildDirectory.dir("proto-wrapper-cache")
}
```

#### Acceptance Criteria

- [ ] No regeneration when protos unchanged
- [ ] Correct regeneration on proto change
- [ ] Dependency-aware regeneration
- [ ] Cache invalidation on config change
- [ ] Measurable build time improvement (>50% for unchanged)
- [ ] Works with both Maven and Gradle
- [ ] `--force` flag to bypass cache

### Migration Notes

- No breaking changes
- Recommended: Enable incremental generation for faster builds

---

## Version 1.7.0

**Target:** Mar 2026
**Theme:** Parallel Generation

### Feature: Parallel Generation

**Priority:** Medium
**Complexity:** Low

#### Description

Generate wrapper classes in parallel to improve build performance for large schemas.

#### Implementation

```java
public class ParallelGenerationOrchestrator {
    private final ExecutorService executor;

    public void generateAll(MergedSchema schema, GeneratorConfig config) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Generate messages in parallel
        for (MergedMessage message : schema.getMessages()) {
            futures.add(CompletableFuture.runAsync(() ->
                generateMessage(message, config), executor));
        }

        // Wait for all
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Generate VersionContext (depends on all messages)
        generateVersionContext(schema, config);
    }
}
```

#### Configuration

```xml
<configuration>
    <parallelGeneration>true</parallelGeneration>
    <generationThreads>4</generationThreads> <!-- 0 = auto -->
</configuration>
```

#### Acceptance Criteria

- [ ] Thread-safe generation
- [ ] Configurable thread count
- [ ] No race conditions in file writing
- [ ] Measurable performance improvement
- [ ] Proper error handling and reporting

### Migration Notes

- No breaking changes
- New feature is opt-in

---

## Version 1.8.0

**Target:** Mar 2026
**Theme:** Per-version Proto Syntax

### Feature: Per-version Proto Syntax

**Priority:** High
**Complexity:** Medium

#### Description

Support mixed proto2/proto3 syntax across different protocol versions. Each version can specify its own protobuf syntax, enabling migration scenarios.

#### Problem

Current limitation: global `protobufMajorVersion` setting doesn't work for projects migrating from proto2 to proto3.

```xml
<!-- Current: global setting -->
<configuration>
    <protobufMajorVersion>3</protobufMajorVersion>  <!-- Applies to all versions -->
</configuration>
```

#### Solution

Per-version syntax configuration:

```xml
<versions>
    <version>
        <protoDir>v1</protoDir>
        <protoSyntax>proto2</protoSyntax>
    </version>
    <version>
        <protoDir>v2</protoDir>
        <protoSyntax>proto3</protoSyntax>
    </version>
</versions>
```

```kotlin
protoWrapper {
    versions {
        version("v1") {
            protoDir = "v1"
            protoSyntax = ProtoSyntax.PROTO2
        }
        version("v2") {
            protoDir = "v2"
            protoSyntax = ProtoSyntax.PROTO3
        }
    }
}
```

#### Syntax Differences Handled

| Aspect | proto2 | proto3 | Generated Code |
|--------|--------|--------|----------------|
| Enum conversion | `valueOf(int)` | `forNumber(int)` | Per-version method |
| Required fields | Supported | Not supported | `@NotNull` for proto2 |
| hasXxx() | All fields | Message/optional only | Conditional generation |
| Default values | Custom | Zero values | Documented in JavaDoc |
| Optional keyword | Explicit | Implicit (+ explicit) | Unified handling |

#### Generated Code Example

```java
// Proto2 version (v1)
public class MoneyV1 extends AbstractMoney<V1Money> {
    // Uses valueOf() for enum conversion
    @Override
    protected Currency extractCurrency(V1Money proto) {
        return Currency.valueOf(proto.getCurrency().getNumber());
    }

    // hasXxx for all fields (proto2 behavior)
    @Override
    protected boolean extractHasAmount(V1Money proto) {
        return proto.hasAmount();
    }
}

// Proto3 version (v2)
public class MoneyV2 extends AbstractMoney<V2Money> {
    // Uses forNumber() for enum conversion
    @Override
    protected Currency extractCurrency(V2Money proto) {
        return Currency.forNumber(proto.getCurrency().getNumber());
    }

    // hasXxx only for message fields (proto3 behavior)
    @Override
    protected boolean extractHasAmount(V2Money proto) {
        return true;  // Scalar always "present" in proto3
    }
}
```

#### Implementation Plan

1. Add `protoSyntax` field to `VersionConfig`:
   ```java
   public enum ProtoSyntax { PROTO2, PROTO3, AUTO }

   public class VersionConfig {
       private String protoDir;
       private ProtoSyntax protoSyntax = ProtoSyntax.AUTO;
   }
   ```

2. Auto-detection from .proto files:
   ```java
   // Parse first line: syntax = "proto3";
   public ProtoSyntax detectSyntax(Path protoFile) {
       String firstLine = Files.readFirstLine(protoFile);
       if (firstLine.contains("proto3")) return PROTO3;
       return PROTO2;  // Default for proto2
   }
   ```

3. Update `ImplClassGenerator`:
   - Pass syntax to handler context
   - Generate version-appropriate enum conversion
   - Generate version-appropriate hasXxx methods

4. Update `VersionMerger`:
   - Track syntax per version
   - Consider syntax in conflict detection

5. Deprecate global `protobufMajorVersion`:
   ```java
   @Deprecated(since = "1.8.0", forRemoval = true)
   private int protobufMajorVersion;
   ```

#### Acceptance Criteria

- [ ] Per-version protoSyntax configuration
- [ ] Auto-detection from .proto files
- [ ] Correct enum conversion per syntax
- [ ] Correct hasXxx generation per syntax
- [ ] Mixed proto2/proto3 integration tests
- [ ] Deprecation of global protobufMajorVersion
- [ ] Migration documentation

### Migration Notes

- No breaking changes
- New feature is opt-in
- Global `protobufMajorVersion` deprecated

---

## Version 1.9.0

**Target:** Apr 2026
**Theme:** Validation Annotations

### Feature: Validation Annotations

**Priority:** High
**Complexity:** Medium

#### Description

Generate Bean Validation (JSR-380) annotations on wrapper interfaces based on proto constraints and conventions.

#### Supported Annotations

| Annotation | Proto Source | Example |
|------------|--------------|---------|
| `@NotNull` | Required fields, non-optional | `required string id = 1;` |
| `@Size` | Repeated fields | `repeated Item items = 2; // @Size(min=1)` |
| `@Min/@Max` | Field options (custom) | `int32 age = 3 [(validate.min) = 0];` |
| `@Pattern` | Field options (custom) | `string email = 4 [(validate.pattern) = "..."];` |
| `@Valid` | Message fields | Nested message validation |

#### Generated Code Example

```java
public interface Order {
    @NotNull
    @Size(min = 1, max = 50)
    String getOrderId();

    @NotNull
    @Size(min = 1, message = "Order must have at least one item")
    List<@Valid OrderItem> getItems();

    @Min(0)
    long getTotalAmount();

    @Valid
    @NotNull
    Customer getCustomer();
}
```

#### Configuration

```xml
<configuration>
    <generateValidationAnnotations>true</generateValidationAnnotations>
    <validationAnnotationStyle>javax</validationAnnotationStyle> <!-- or jakarta -->
</configuration>
```

#### Implementation Plan

1. Add dependency on validation API (provided scope)
2. Create `ValidationAnnotationGenerator`:
   - Analyze field characteristics
   - Generate appropriate annotations
3. Support both `javax.validation` and `jakarta.validation`
4. Custom proto options for explicit constraints:
   ```protobuf
   import "validate/validate.proto";

   message User {
       string email = 1 [(validate.pattern) = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$"];
       int32 age = 2 [(validate.min) = 0, (validate.max) = 150];
   }
   ```

#### Acceptance Criteria

- [ ] @NotNull on required/non-optional fields
- [ ] @Size on repeated fields
- [ ] @Valid on message fields
- [ ] Support for javax and jakarta namespaces
- [ ] Optional custom proto options
- [ ] Configuration to enable/disable
- [ ] Integration with Spring Validation

### Migration Notes

- No breaking changes
- New feature is opt-in
- Validation annotations require adding validation API dependency

---

## Version 2.0.0

**Target:** May 2026
**Theme:** Kotlin Extensions

### Feature: Kotlin Extensions

**Priority:** High
**Complexity:** High

#### Description

Generate Kotlin-idiomatic wrappers with DSL builders, extension functions, and null-safe accessors.

#### Generated Code Example

```kotlin
// Generated Kotlin interface
interface Order {
    val orderId: String
    val items: List<OrderItem>
    val customer: Customer
    val totalAmount: Long
    val shippingAddress: Address?  // Nullable for optional

    fun toBuilder(): Builder

    interface Builder {
        var orderId: String
        var customer: Customer
        var totalAmount: Long
        var shippingAddress: Address?

        fun addItem(item: OrderItem): Builder
        fun items(block: MutableList<OrderItem>.() -> Unit): Builder
        fun customer(block: Customer.Builder.() -> Unit): Builder

        fun build(): Order
    }

    companion object {
        fun build(ctx: VersionContext, block: Builder.() -> Unit): Order
    }
}

// DSL Usage
val order = Order.build(ctx) {
    orderId = "ORD-001"
    totalAmount = 5000

    customer {
        name = "John Doe"
        email = "john@example.com"
    }

    items {
        add(OrderItem.build(ctx) {
            productId = "PROD-001"
            quantity = 2
        })
    }

    shippingAddress = Address.build(ctx) {
        street = "123 Main St"
        city = "Boston"
    }
}
```

#### Extension Functions

```kotlin
// Generated extensions
fun Order.formatted(): String =
    "Order ${orderId}: ${items.size} items, total: $${totalAmount / 100.0}"

fun Money.formatted(locale: Locale = Locale.getDefault()): String =
    NumberFormat.getCurrencyInstance(locale).format(amount / 100.0)

// Null-safe conversions
fun Order.asV2OrNull(): OrderV2? =
    if (this is OrderV2) this else runCatching { asVersion(OrderV2::class.java) }.getOrNull()
```

#### Configuration

```kotlin
protoWrapper {
    kotlinExtensions {
        enabled = true
        generateDsl = true
        generateExtensions = true
        nullableOptionalFields = true
    }
}
```

#### Implementation Plan

1. Create `kotlin-extensions` submodule
2. Implement Kotlin code generators:
   - `KotlinInterfaceGenerator`
   - `KotlinBuilderGenerator`
   - `KotlinDslGenerator`
   - `KotlinExtensionGenerator`
3. Use KotlinPoet for code generation
4. Generate alongside Java (not replacing)
5. Add Gradle plugin configuration

#### Acceptance Criteria

- [ ] Kotlin interfaces with properties
- [ ] DSL builders
- [ ] Extension functions
- [ ] Null-safe optional fields
- [ ] Works alongside Java wrappers
- [ ] Gradle plugin support
- [ ] Documentation

### Migration Notes

- No breaking changes to Java API
- Kotlin extensions are additive

---

## Version 2.1.0

**Target:** Jun 2026
**Theme:** Service/RPC Wrappers

### Feature: Service/RPC Wrappers

**Priority:** Medium
**Complexity:** High

#### Description

Generate version-agnostic service interfaces from proto service definitions.

#### Proto Definition

```protobuf
service OrderService {
    rpc CreateOrder(CreateOrderRequest) returns (Order);
    rpc GetOrder(GetOrderRequest) returns (Order);
    rpc ListOrders(ListOrdersRequest) returns (stream Order);
    rpc UpdateOrder(stream UpdateOrderRequest) returns (Order);
}
```

#### Generated Code

```java
// Version-agnostic service interface
public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order getOrder(GetOrderRequest request);
    Iterator<Order> listOrders(ListOrdersRequest request);  // Server streaming
    Order updateOrder(Iterator<UpdateOrderRequest> requests);  // Client streaming
}

// Async variant
public interface OrderServiceAsync {
    CompletableFuture<Order> createOrder(CreateOrderRequest request);
    CompletableFuture<Order> getOrder(GetOrderRequest request);
    Publisher<Order> listOrders(ListOrdersRequest request);  // Reactive
}

// gRPC adapter
public class OrderServiceGrpcAdapter implements OrderService {
    private final OrderServiceGrpc.OrderServiceBlockingStub stub;
    private final VersionContext ctx;

    @Override
    public Order createOrder(CreateOrderRequest request) {
        var protoRequest = toProto(request);
        var protoResponse = stub.createOrder(protoRequest);
        return ctx.wrapOrder(protoResponse);
    }
}
```

#### Implementation Plan

1. Parse service definitions from proto
2. Generate service interfaces
3. Generate gRPC adapters (optional)
4. Support streaming methods
5. Async/reactive variants

#### Acceptance Criteria

- [ ] Service interface generation
- [ ] All RPC types (unary, streaming)
- [ ] gRPC adapter generation
- [ ] Async variants
- [ ] Version-agnostic wrappers for request/response

### Migration Notes

- No breaking changes
- Service wrappers are a new optional feature

---

## Version 2.2.0

**Target:** Jul 2026
**Theme:** API Cleanup

### Feature: Remove Deprecated API

**Priority:** High
**Complexity:** Low

#### Deprecated API to Remove

| Class/Method | Deprecated Since | Replacement |
|--------------|------------------|-------------|
| `InterfaceGenerator.setSchema()` | 1.2.0 | Use `GenerationContext` |
| `InterfaceGenerator.generate(MergedMessage)` | 1.2.0 | Use `generate(GenerationContext)` |
| `InterfaceGenerator.generateAndWrite(MergedMessage)` | 1.2.0 | Use `generateAndWrite(GenerationContext)` |
| `AbstractClassGenerator.setSchema()` | 1.2.0 | Use `GenerationContext` |
| `AbstractClassGenerator.generate(MergedMessage)` | 1.2.0 | Use `generate(GenerationContext)` |
| `ImplClassGenerator.setSchema()` | 1.2.0 | Use `GenerationContext` |
| `ImplClassGenerator.generate(MergedMessage, String, String)` | 1.2.0 | Use `generate(GenerationContext)` |
| `MergedField(FieldInfo, String)` constructor | 1.2.0 | Use `MergedField.create()` |
| `MergedField.addVersion(String, FieldInfo)` | 1.2.0 | Use builder pattern |
| `ProtocExecutor(Consumer<String>)` | 1.2.0 | Use `ProtocExecutor(PluginLogger)` |

#### Migration Guide

```java
// Before (deprecated)
InterfaceGenerator generator = new InterfaceGenerator(config);
generator.setSchema(schema);
generator.generate(message);

// After
GenerationContext ctx = new GenerationContext(schema, config);
InterfaceGenerator generator = new InterfaceGenerator(config);
generator.generate(ctx.forMessage(message));
```

---

### Feature: API Refinements

#### Sealed Interfaces

```java
// Make all handler interfaces sealed
public sealed interface ConflictHandler permits
    IntEnumHandler, EnumEnumHandler, StringBytesHandler,
    WideningHandler, FloatDoubleHandler, SignedUnsignedHandler,
    RepeatedSingleHandler, PrimitiveMessageHandler,
    MapFieldHandler, DefaultHandler {}
```

#### Record-based Models

```java
// Convert remaining model classes to records
public record GeneratorConfig(
    String apiPackage,
    String implPackagePattern,
    boolean generateBuilders,
    boolean generateInterfaces,
    // ...
) {
    public static Builder builder() { ... }
}
```

#### Builder Improvements

```java
// Fluent configuration
GeneratorConfig config = GeneratorConfig.builder()
    .apiPackage("com.example.api")
    .implPackagePattern("{api}.{version}")
    .generateBuilders(true)
    .wellKnownTypes(WellKnownTypeConfig.builder()
        .convertTimestamp(true)
        .convertDuration(true)
        .build())
    .validation(ValidationConfig.builder()
        .enabled(true)
        .style(ValidationStyle.JAKARTA)
        .build())
    .build();
```

### Migration Notes

#### Breaking Changes

1. **Removed deprecated API** - See migration guide above
2. **Minimum Java version: 17** (unchanged)
3. **GenerationContext required** for all generators

#### Migration Steps

1. Replace deprecated method calls
2. Use `GenerationContext` for generation
3. Update custom handlers if any

---

## Future Considerations

Features considered for v2.3.0+:

### Protocol Buffers Edition Support
- Support for proto editions (2023+)
- Feature detection and adaptation

### Custom Type Mappings
```xml
<typeMappings>
    <mapping proto="google.type.Money" java="org.javamoney.moneta.Money"/>
    <mapping proto="google.type.Date" java="java.time.LocalDate"/>
</typeMappings>
```

### IDE Plugin
- IntelliJ IDEA plugin
- Syntax highlighting for generated code
- Navigation between proto and wrapper
- Quick fixes for conflicts

### Proto Linting Integration
- Integration with buf
- Detect potential conflict patterns
- Suggest proto improvements

### Performance Benchmarks
- Automated performance testing
- Comparison with raw proto access
- Memory usage analysis

---

## Contributing

We welcome contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

### Priority Areas

1. Well-Known Types support
2. Kotlin extensions
3. Documentation improvements
4. Integration tests

### How to Propose Features

1. Open GitHub issue with `[Feature]` prefix
2. Describe use case and expected behavior
3. Discuss implementation approach
4. Submit PR when approved

---

## Version History

| Version | Feature | Target |
|---------|---------|--------|
| 1.2.0 | Map Support, Lazy Caching, Oneof | Released (2026-01-02) |
| 1.3.0 | Well-Known Types Support | Jan 2026 |
| 1.4.0 | Repeated Conflict Field Builders | Jan 2026 |
| 1.5.0 | Schema Diff Tool | Feb 2026 |
| 1.6.0 | Incremental Generation | Feb 2026 |
| 1.7.0 | Parallel Generation | Mar 2026 |
| 1.8.0 | Per-version Proto Syntax | Mar 2026 |
| 1.9.0 | Validation Annotations | Apr 2026 |
| 2.0.0 | Kotlin Extensions | May 2026 |
| 2.1.0 | Service/RPC Wrappers | Jun 2026 |
| 2.2.0 | API Cleanup | Jul 2026 |
