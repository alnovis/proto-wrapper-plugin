# Proto Wrapper Plugin Cookbook

A practical guide with detailed examples for common Proto Wrapper use cases.

**Prerequisites:** Complete the [Getting Started](GETTING_STARTED.md) guide first.

---

## Table of Contents

- [Using ProtocolVersions Constants](#using-protocolversions-constants)
- [Parsing and Serialization](#parsing-and-serialization)
- [Type Conflict Handling](#type-conflict-handling)
- [Generation Modes](#generation-modes)
- [Oneof Field Handling](#oneof-field-handling)
- [Common Use Cases](#common-use-cases)
- [Well-Known Types](#well-known-types)
- [Troubleshooting](#troubleshooting)

---

## Related Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](GETTING_STARTED.md) | Step-by-step setup tutorial |
| [Configuration](CONFIGURATION.md) | All plugin options |
| [Schema Diff](SCHEMA_DIFF.md) | Compare schema versions |
| [Known Issues](KNOWN_ISSUES.md) | Limitations and workarounds |

---

## Using ProtocolVersions Constants

*New in v2.1.0*

When `generateProtocolVersions=true`, a `ProtocolVersions` class is generated with constants for all version identifiers. This improves type safety and makes code more maintainable.

### Enabling ProtocolVersions

**Maven:**
```xml
<configuration>
    <generateProtocolVersions>true</generateProtocolVersions>
</configuration>
```

**Gradle:**
```kotlin
protoWrapper {
    generateProtocolVersions.set(true)
}
```

### Generated Class

```java
public final class ProtocolVersions {
    public static final String V1 = "v1";
    public static final String V2 = "v2";
    public static final List<String> ALL_VERSIONS = List.of(V1, V2);
    public static final String DEFAULT_VERSION = V2;

    private ProtocolVersions() {}
}
```

### Basic Usage

```java
import com.example.model.api.ProtocolVersions;
import com.example.model.api.VersionContext;

// Use constants instead of string literals
VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V1);

// IDE autocomplete helps prevent typos
Order order = Order.newBuilder(ctx)
        .setOrderId("ORD-001")
        .build();

// Check version at runtime
if (ProtocolVersions.V2.equals(order.getWrapperVersionId())) {
    // V2-specific logic
}
```

### Version Validation

```java
public Order processOrder(byte[] bytes, String requestedVersion)
        throws InvalidProtocolBufferException {

    // Validate using the generated list
    if (!ProtocolVersions.ALL_VERSIONS.contains(requestedVersion)) {
        throw new IllegalArgumentException(
            "Unsupported version: " + requestedVersion +
            ". Supported: " + ProtocolVersions.ALL_VERSIONS);
    }

    VersionContext ctx = VersionContext.forVersionId(requestedVersion);
    return Order.parseFromBytes(ctx, bytes);
}
```

### Using Default Version

```java
// Get context for latest (default) version
VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.DEFAULT_VERSION);

// Or use VersionContext.getDefault() which is equivalent
VersionContext defaultCtx = VersionContext.getDefault();
```

### Switch on Version

```java
public void handleOrder(Order order) {
    String version = order.getWrapperVersionId();

    // Compare with constants
    if (ProtocolVersions.V1.equals(version)) {
        handleV1Order(order);
    } else if (ProtocolVersions.V2.equals(version)) {
        handleV2Order(order);
    }
}
```

### Benefits Over String Literals

| With Literals | With Constants |
|---------------|----------------|
| `"v1"` (typo-prone) | `ProtocolVersions.V1` (compile-time checked) |
| No autocomplete | IDE autocomplete |
| Scattered magic strings | Centralized constants |
| Hard to find all usages | Easy "Find Usages" refactoring |

### Integration with Generated Code

When `generateProtocolVersions=true`, all generated code uses these constants:

```java
// VersionContext uses constants
public interface VersionContext {
    List<String> SUPPORTED_VERSIONS = List.of(
        ProtocolVersions.V1,
        ProtocolVersions.V2
    );
    String DEFAULT_VERSION = ProtocolVersions.V2;
}

// Implementation classes use constants
public class OrderV1 {
    @Override
    public String getWrapperVersionId() {
        return ProtocolVersions.V1;  // Not "v1"
    }
}
```

---

## Parsing and Serialization

Proto-wrapper provides multiple ways to parse byte arrays into wrapper objects and serialize them back.

### Overview of Parsing Options

| Method | Location | Description | Since |
|--------|----------|-------------|-------|
| `Interface.parseFromBytes(ctx, bytes)` | Interface | Static method on each interface | v1.6.9 |
| `ctx.parse{Type}FromBytes(bytes)` | VersionContext | Instance method on context | v1.0.0 |
| `ctx.wrap{Type}(proto)` | VersionContext | Wrap existing proto object | v1.0.0 |

### Option 1: Static parseFromBytes (Recommended)

*Since v1.6.9*

The most intuitive approach — call `parseFromBytes` directly on the interface type:

```java
import com.example.model.api.Money;
import com.example.model.api.VersionContext;
import com.google.protobuf.InvalidProtocolBufferException;

// Get version context
VersionContext ctx = VersionContext.forVersionId("v1");

// Parse bytes using static method on interface
Money money = Money.parseFromBytes(ctx, bytes);

// Access fields
long amount = money.getAmount();
String currency = money.getCurrency();
```

**Advantages:**
- Mirrors native protobuf pattern (`MyProto.parseFrom(bytes)`)
- Type is explicit in the method call
- IDE autocomplete works naturally

**Nested types:**

```java
// For nested interface Address.GeoLocation
Address.GeoLocation location = Address.GeoLocation.parseFromBytes(ctx, bytes);

// For deeply nested types
Order.Item.Discount discount = Order.Item.Discount.parseFromBytes(ctx, bytes);
```

### Option 2: VersionContext Method

Call the parse method on the VersionContext instance:

```java
VersionContext ctx = VersionContext.forVersionId("v2");

// Parse using context method
Money money = ctx.parseMoneyFromBytes(bytes);
Order order = ctx.parseOrderFromBytes(bytes);
Customer customer = ctx.parseCustomerFromBytes(bytes);

// Nested types
Address.GeoLocation location = ctx.parseAddressGeoLocationFromBytes(bytes);
```

**Advantages:**
- Available since v1.0.0
- Good when you have the context and need to parse many different types

### Option 3: Wrap Existing Proto Object

When you already have a parsed protobuf object:

```java
import com.example.proto.v1.Common;

// Parse proto directly
Common.Money proto = Common.Money.parseFrom(bytes);

// Wrap it
VersionContext ctx = VersionContext.forVersionId("v1");
Money money = ctx.wrapMoney(proto);
```

**Use case:** Interoperability with code that works with raw protobuf objects.

### Serialization

All wrapper objects can be serialized to bytes:

```java
Money money = Money.newBuilder(ctx)
        .setAmount(1000)
        .setCurrency("USD")
        .build();

// Serialize to bytes
byte[] bytes = money.toBytes();

// Round-trip: parse back
Money restored = Money.parseFromBytes(ctx, bytes);
assert restored.getAmount() == 1000;
assert restored.getCurrency().equals("USD");
```

### Complete Example: Request/Response Handling

```java
public class PaymentService {

    private final VersionContext ctx;

    public PaymentService(String protocolVersion) {
        this.ctx = VersionContext.forVersionId(protocolVersion);
    }

    /**
     * Process payment request from bytes.
     */
    public byte[] processPayment(byte[] requestBytes)
            throws InvalidProtocolBufferException {

        // Parse request using static method
        Payment request = Payment.parseFromBytes(ctx, requestBytes);

        // Business logic
        PaymentResult result = executePayment(request);

        // Build response
        PaymentResponse response = PaymentResponse.newBuilder(ctx)
                .setPaymentId(request.getId())
                .setStatus(result.isSuccess() ? "SUCCESS" : "FAILED")
                .setTimestamp(Instant.now())
                .build();

        // Serialize response
        return response.toBytes();
    }
}
```

### Cross-Version Parsing

Parse data with one version, convert to another:

```java
// Receive V1 data
VersionContext v1Ctx = VersionContext.forVersionId("v1");
Money v1Money = Money.parseFromBytes(v1Ctx, bytesFromV1Client);

// Convert to V2 for processing
Money v2Money = v1Money.asVersion(
        com.example.model.v2.Money.class);

// V2-only fields become accessible if data exists
if (v2Money.supportsExchangeRate() && v2Money.hasExchangeRate()) {
    Double rate = v2Money.getExchangeRate();
}
```

### Error Handling

```java
VersionContext ctx = VersionContext.forVersionId("v1");

try {
    Money money = Money.parseFromBytes(ctx, bytes);
    // Process money...
} catch (InvalidProtocolBufferException e) {
    // Invalid protobuf data
    log.error("Failed to parse Money: {}", e.getMessage());
    throw new BadRequestException("Invalid payment data");
}
```

**Common errors:**
- `InvalidProtocolBufferException` — bytes are not valid protobuf
- `UninitializedMessageException` — required fields missing (proto2)

### Parsing with Unknown Version

When version comes from external source (e.g., header):

```java
public Money parseMoney(byte[] bytes, String versionHeader)
        throws InvalidProtocolBufferException {

    // Validate version
    if (!VersionContext.isSupported(versionHeader)) {
        throw new IllegalArgumentException(
            "Unsupported version: " + versionHeader +
            ". Supported: " + VersionContext.supportedVersions());
    }

    VersionContext ctx = VersionContext.forVersionId(versionHeader);
    return Money.parseFromBytes(ctx, bytes);
}
```

### Best Practices

1. **Prefer static parseFromBytes** for clarity:
   ```java
   // Good - type is explicit
   Money money = Money.parseFromBytes(ctx, bytes);

   // OK - but type only in variable declaration
   Money money = ctx.parseMoneyFromBytes(bytes);
   ```

2. **Reuse VersionContext** when processing multiple messages:
   ```java
   VersionContext ctx = VersionContext.forVersionId("v1");

   // Parse multiple objects with same context
   Order order = Order.parseFromBytes(ctx, orderBytes);
   Customer customer = Customer.parseFromBytes(ctx, customerBytes);
   Payment payment = Payment.parseFromBytes(ctx, paymentBytes);
   ```

3. **Use getContext() for related objects**:
   ```java
   // Parse main object
   Order order = Order.parseFromBytes(ctx, orderBytes);

   // Use its context for creating related objects
   VersionContext orderCtx = order.getContext();
   Payment payment = Payment.newBuilder(orderCtx)
           .setOrderId(order.getId())
           .setAmount(order.getTotal())
           .build();
   ```

4. **Handle version mismatches gracefully**:
   ```java
   // Check if field is accessible before using
   Money money = Money.parseFromBytes(ctx, bytes);

   if (money.supportsExchangeRate()) {
       Double rate = money.getExchangeRate();
       // Use rate...
   } else {
       // Use default or calculate...
   }
   ```

---

## Type Conflict Handling

The plugin automatically detects and handles situations where field types differ between versions.

### Conflict Types Overview

| Conflict Type | Description | Convertible? |
|--------------|-------------|--------------|
| `NONE` | No conflict | - |
| `INT_ENUM` | `int` ↔ `enum` | Yes |
| `WIDENING` | `int` → `long`, `float` → `double` | Yes |
| `NARROWING` | `long` → `int`, `double` → `float` | No (lossy) |
| `STRING_BYTES` | `string` ↔ `bytes` | Yes (UTF-8) |
| `PRIMITIVE_MESSAGE` | `int` → `SomeMessage` | No |
| `INCOMPATIBLE` | Incompatible types | No |

### INT_ENUM Conflict

**Situation**: Field is `int32` in one version and `enum` in another.

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
    UNIT_KELVIN = 2;  // New value in v2
}
```

**Generated code**:

```java
// Unified enum
public enum UnitType {
    UNIT_CELSIUS(0),
    UNIT_FAHRENHEIT(1),
    UNIT_KELVIN(2);

    public static UnitType fromProtoValue(int value) { ... }
    public int getProtoValue() { return value; }
}

// Interface with dual getters
public interface SensorReading {
    int getUnitType();              // Returns int
    UnitType getUnitTypeEnum();     // Returns unified enum

    interface Builder {
        Builder setUnitType(int value);       // Set via int
        Builder setUnitType(UnitType value);  // Set via enum
    }
}
```

**Usage**:

```java
SensorReading reading = ctx.wrapSensorReading(proto);

// Reading
int rawValue = reading.getUnitType();           // 0, 1, 2...
UnitType type = reading.getUnitTypeEnum();      // UNIT_CELSIUS, etc.

// Writing (with Builder)
SensorReading modified = reading.toBuilder()
    .setUnitType(UnitType.UNIT_KELVIN)  // Via enum
    // or
    .setUnitType(2)                      // Via int
    .build();
```

### WIDENING Conflict

**Situation**: Type widening between versions.

```protobuf
// v1/data.proto
message Data {
    int32 value = 1;
    float temperature = 2;
}

// v2/data.proto
message Data {
    int64 value = 1;      // Widened to 64-bit
    double temperature = 2; // Widened to double
}
```

**Generated code**:

```java
public interface Data {
    long getValue();        // Unified as long
    double getTemperature(); // Unified as double

    interface Builder {
        Builder setValue(long value);
        Builder setTemperature(double value);
    }
}

// V1 implementation auto-widens values:
class DataV1 {
    @Override
    protected long extractValue(DataProto.Data proto) {
        return (long) proto.getValue();  // int → long
    }
}

// V1 builder checks range:
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

**Usage**:

```java
Data data = ctx.wrapData(proto);
long value = data.getValue();  // Always long

// Builder safely handles narrowing
Data modified = data.toBuilder()
    .setValue(42L)  // OK for both versions
    .build();

// Error when exceeding V1 range
try {
    Data v1Modified = v1Data.toBuilder()
        .setValue(Long.MAX_VALUE)  // Too large for int
        .build();
} catch (IllegalArgumentException e) {
    // "Value exceeds int range for V1"
}
```

### STRING_BYTES Conflict

**Situation**: Field is `string` in one version and `bytes` in another.

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

**Generated code**:

```java
public interface TelemetryReport {
    String getChecksum();       // String (UTF-8 for bytes)
    byte[] getChecksumBytes();  // byte[] (UTF-8 for string)

    interface Builder {
        Builder setChecksum(String value);
    }
}
```

**Usage**:

```java
TelemetryReport report = ctx.wrapTelemetryReport(proto);

// Reading
String checksumStr = report.getChecksum();        // "abc123"
byte[] checksumBytes = report.getChecksumBytes(); // [97, 98, 99, ...]

// Writing
TelemetryReport modified = report.toBuilder()
    .setChecksum("new_checksum")
    .build();
```

### PRIMITIVE_MESSAGE Conflict

**Situation**: Primitive in one version, message in another.

```protobuf
// v1/order.proto
message Order {
    int32 shipping_cost = 1;  // Simple value
}

// v2/order.proto
message Order {
    Money shipping_cost = 1;  // Complex object
}
```

**Generated code**:

```java
public interface Order {
    // Dual getters
    int getShippingCost();           // Primitive value (0 for message versions)
    Money getShippingCostMessage();  // Message (null for primitive versions)

    interface Builder {
        // Dual setters (v1.6.2+)
        Builder setShippingCost(int value);        // For primitive versions
        Builder setShippingCostMessage(Money value); // For message versions

        // Runtime support checks
        boolean supportsPrimitiveShippingCost();   // true for V1
        boolean supportsMessageShippingCost();     // true for V2
    }
}
```

**Reading values**:

```java
Order order = ctx.wrapOrder(proto);

// V1 (primitive)
int cost = order.getShippingCost();              // 100
Money costMsg = order.getShippingCostMessage();  // null

// V2 (message)
int cost = order.getShippingCost();              // 0 (default)
Money costMsg = order.getShippingCostMessage();  // Money object
```

**Writing with Builder (v1.6.2+)**:

```java
// V1: use primitive setter
VersionContext v1Ctx = VersionContext.forVersionId("v1");
Order v1Order = Order.newBuilder(v1Ctx)
    .setShippingCost(100)  // OK - V1 supports primitive
    .build();

// V2: use message setter
VersionContext v2Ctx = VersionContext.forVersionId("v2");
Money money = Money.newBuilder(v2Ctx)
    .setAmount(100)
    .setCurrency("USD")
    .build();
Order v2Order = Order.newBuilder(v2Ctx)
    .setShippingCostMessage(money)  // OK - V2 supports message
    .build();
```

**Runtime validation**:

```java
// Calling wrong setter throws UnsupportedOperationException
Order.Builder v1Builder = Order.newBuilder(v1Ctx);
v1Builder.setShippingCostMessage(money);  // throws UnsupportedOperationException!
// "V1 does not support message type for field 'shippingCost'"

// Check support before calling
if (builder.supportsMessageShippingCost()) {
    builder.setShippingCostMessage(money);
} else {
    builder.setShippingCost(100);
}
```

### Repeated Fields with Conflicts (v1.4.0+)

Full builder support is available for repeated fields with type conflicts:

```java
public interface RepeatedConflicts {
    // Reading
    List<Long> getNumbers();    // int32 → int64 (elements widened)
    List<Integer> getCodes();   // int32 → enum (enum.getNumber())
    List<String> getTexts();    // string → bytes (UTF-8 conversion)

    interface Builder {
        // Adding elements
        Builder addNumbers(long value);      // Range-validated for V1
        Builder addAllNumbers(Iterable<Long> values);

        Builder addCodes(int value);         // Works for both int and enum
        Builder addCodes(CodeEnum value);    // Enum version

        Builder addTexts(String value);      // UTF-8 converted for bytes versions

        // Clearing
        Builder clearNumbers();
        Builder clearCodes();
        Builder clearTexts();
    }
}
```

**Usage**:

```java
RepeatedConflicts data = RepeatedConflicts.newBuilder(ctx)
    .addNumbers(100L)
    .addNumbers(200L)
    .addAllNumbers(List.of(300L, 400L))
    .addCodes(CodeEnum.VALUE_A)
    .addTexts("hello")
    .build();

// Range validation for narrowing (V1 int32 ← V2 int64)
RepeatedConflicts.Builder v1Builder = RepeatedConflicts.newBuilder(v1Ctx);
v1Builder.addNumbers(Long.MAX_VALUE);  // throws IllegalArgumentException!
// "Value 9223372036854775807 exceeds int32 range"
```

---

## Generation Modes

### Read-Only Mode (default)

```xml
<configuration>
    <generateBuilders>false</generateBuilders>
</configuration>
```

Only getters are generated. Suitable for:
- Reading and serializing proto messages
- Integration with legacy systems
- Minimal generated code size

```java
Order order = ctx.wrapOrder(proto);

// Reading
DateTime dateTime = order.getDateTime();
List<OrderItem> items = order.getItems();

// Serialization
byte[] bytes = order.toBytes();
```

### Builder Mode

```xml
<configuration>
    <generateBuilders>true</generateBuilders>
    <protobufMajorVersion>3</protobufMajorVersion>
</configuration>
```

Builder interfaces are generated for modification. Suitable for:
- Creating new proto messages
- Modifying existing wrappers
- Full CRUD functionality

```java
// Modify existing
Order modified = order.toBuilder()
    .setCustomerId("CUST-123")
    .setTotalAmount(money)
    .build();

// Create new
Order newOrder = Order.newBuilder()
    .setOrderId("ORD-456")
    .setCustomerId("CUST-789")
    .build();
```

---

## Oneof Field Handling

Proto-wrapper fully supports protobuf `oneof` fields with automatic conflict detection.

### Basic Oneof Usage

**Proto definition:**

```protobuf
message Payment {
    string id = 1;
    int64 amount = 2;

    oneof method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
        Crypto crypto = 12;  // V2 only
    }
}
```

**Creating a payment with oneof:**

```java
VersionContext ctx = VersionContext.forVersionId("v1");

// Create the oneof field value
CreditCard card = ctx.newCreditCardBuilder()
        .setCardNumber("4111111111111111")
        .setExpiry("12/25")
        .setHolderName("John Doe")
        .build();

// Create payment with credit card selected
Payment payment = ctx.newPaymentBuilder()
        .setId("PAY-001")
        .setAmount(10000L)
        .setCreditCard(card)
        .build();
```

**Checking which oneof field is set:**

```java
switch (payment.getMethodCase()) {
    case CREDIT_CARD -> {
        CreditCard card = payment.getCreditCard();
        processCard(card);
    }
    case BANK_TRANSFER -> {
        BankTransfer transfer = payment.getBankTransfer();
        processTransfer(transfer);
    }
    case CRYPTO -> {
        Crypto crypto = payment.getCrypto();
        processCrypto(crypto);
    }
    case METHOD_NOT_SET -> {
        throw new IllegalStateException("Payment method not specified");
    }
}
```

**Using individual has-methods:**

```java
if (payment.hasCreditCard()) {
    CreditCard card = payment.getCreditCard();
    // Process card...
} else if (payment.hasBankTransfer()) {
    BankTransfer transfer = payment.getBankTransfer();
    // Process transfer...
}
```

### Changing Oneof Selection

Setting a new oneof field automatically clears the previous one:

```java
Payment.Builder builder = payment.toBuilder();

// Currently has credit card selected
assert builder.build().getMethodCase() == MethodCase.CREDIT_CARD;

// Setting bank transfer clears credit card
BankTransfer transfer = ctx.newBankTransferBuilder()
        .setAccountNumber("123456789")
        .setBankCode("SWIFT123")
        .build();
builder.setBankTransfer(transfer);

Payment modified = builder.build();
assert modified.getMethodCase() == MethodCase.BANK_TRANSFER;
assert !modified.hasCreditCard();  // Credit card is now cleared
```

### Clearing Oneof

```java
Payment cleared = payment.toBuilder()
        .clearMethod()  // Clears the entire oneof group
        .build();

assert cleared.getMethodCase() == MethodCase.METHOD_NOT_SET;
assert !cleared.hasCreditCard();
assert !cleared.hasBankTransfer();
assert !cleared.hasCrypto();
```

### Version Conversion with Oneof

**V1 to V2 - field is preserved:**

```java
// V1 payment with credit card
VersionContext v1Ctx = VersionContext.forVersionId("v1");
Payment v1Payment = v1Ctx.newPaymentBuilder()
        .setId("PAY-001")
        .setCreditCard(card)
        .build();

// Convert to V2 - credit card is preserved
Payment v2Payment = v1Payment.asVersion(
        space.example.model.v2.Payment.class);

assert v2Payment.getMethodCase() == MethodCase.CREDIT_CARD;
assert v2Payment.getCreditCard() != null;
```

**V2 to V1 - V2-only field is dropped:**

```java
// V2 payment with crypto (V2-only option)
VersionContext v2Ctx = VersionContext.forVersionId("v2");
Payment v2Payment = v2Ctx.newPaymentBuilder()
        .setId("PAY-002")
        .setCrypto(crypto)
        .build();

assert v2Payment.getMethodCase() == MethodCase.CRYPTO;

// Convert to V1 - crypto doesn't exist in V1
Payment v1Payment = v2Payment.asVersion(
        space.example.model.v1.Payment.class);

// Oneof becomes unset because crypto is not supported
assert v1Payment.getMethodCase() == MethodCase.METHOD_NOT_SET;
```

### Oneof with Different Fields Across Versions

When versions have different oneof fields:

```protobuf
// V1: 2 payment methods
message Payment {
    oneof method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}

// V2: 3 payment methods (adds crypto)
message Payment {
    oneof method {
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
        Crypto crypto = 12;
    }
}
```

**Generated Case enum includes all options:**

```java
public enum MethodCase {
    CREDIT_CARD(10),
    BANK_TRANSFER(11),
    CRYPTO(12),        // V2 only
    METHOD_NOT_SET(0);
}
```

**Handling version-specific options safely:**

```java
public void processPayment(Payment payment) {
    switch (payment.getMethodCase()) {
        case CREDIT_CARD, BANK_TRANSFER -> {
            // Available in all versions
            processStandardPayment(payment);
        }
        case CRYPTO -> {
            // V2 only - check version first
            if (!"v2".equals(payment.getWrapperVersionId())) {
                throw new IllegalStateException(
                    "Crypto not supported in version " + payment.getWrapperVersionId());
            }
            processCryptoPayment(payment.getCrypto());
        }
        case METHOD_NOT_SET -> {
            throw new IllegalArgumentException("Payment method required");
        }
    }
}
```

### Renamed Oneof Handling

When oneof is renamed between versions but has the same fields:

```protobuf
// V1
message Payment {
    oneof payment_method {  // Original name
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}

// V2
message Payment {
    oneof method {  // Renamed
        CreditCard credit_card = 10;
        BankTransfer bank_transfer = 11;
    }
}
```

The plugin detects this and generates a unified API using the most common name. A warning is logged:

```
[WARN] Oneof RENAMED: 'Payment.method' has different names: {v1=payment_method, v2=method}
```

Usage remains the same regardless of version:

```java
// Works for both V1 and V2
Payment payment = ctx.parsePaymentFromBytes(bytes);
MethodCase methodCase = payment.getMethodCase();
```

### Testing Oneof Fields

```java
@ParameterizedTest
@ValueSource(strings = {"v1", "v2"})
void testPaymentWithCreditCard(String versionId) {
    VersionContext ctx = VersionContext.forVersionId(versionId);

    CreditCard card = ctx.newCreditCardBuilder()
            .setCardNumber("4111111111111111")
            .setExpiry("12/25")
            .build();

    Payment payment = ctx.newPaymentBuilder()
            .setId("TEST-001")
            .setAmount(1000L)
            .setCreditCard(card)
            .build();

    // Assertions work for any version
    assertThat(payment.getMethodCase()).isEqualTo(MethodCase.CREDIT_CARD);
    assertThat(payment.hasCreditCard()).isTrue();
    assertThat(payment.hasBankTransfer()).isFalse();
    assertThat(payment.getCreditCard().getCardNumber()).isEqualTo("4111111111111111");
}

@Test
void testCryptoOnlyInV2() {
    VersionContext v1Ctx = VersionContext.forVersionId("v1");
    VersionContext v2Ctx = VersionContext.forVersionId("v2");

    // Crypto is V2-only
    Payment v1Payment = v1Ctx.newPaymentBuilder()
            .setId("TEST-002")
            .build();

    assertThat(v1Payment.hasCrypto()).isFalse();
    assertThat(v1Payment.getCrypto()).isNull();

    // V2 can have crypto
    Crypto crypto = v2Ctx.newCryptoBuilder()
            .setWalletAddress("0x123")
            .setCurrency("ETH")
            .build();

    Payment v2Payment = v2Ctx.newPaymentBuilder()
            .setId("TEST-003")
            .setCrypto(crypto)
            .build();

    assertThat(v2Payment.hasCrypto()).isTrue();
    assertThat(v2Payment.getCrypto().getCurrency()).isEqualTo("ETH");
}
```

---

## Common Use Cases

### Use Case 1: API Version Migration

```java
public class OrderMigrationService {

    public Order migrateToV2(Order v1Order) {
        // Serialize V1
        byte[] bytes = v1Order.toBytes();

        // Parse as V2
        OrderV2Proto proto = OrderV2Proto.parseFrom(bytes);

        // Wrap as V2
        return new OrderV2(proto);
    }
}
```

### Use Case 2: Versioned API Endpoint

```java
@RestController
public class OrderController {

    @PostMapping("/api/orders")
    public ResponseEntity<byte[]> createOrder(
            @RequestBody byte[] protoBytes,
            @RequestHeader("X-API-Version") String versionId) {

        VersionContext ctx = VersionContext.forVersionId(versionId);

        // Parse with correct version
        Order order = ctx.wrapOrder(parseProto(protoBytes, versionId));

        // Business logic
        Order processed = processOrder(order);

        // Response in same version
        return ResponseEntity.ok(processed.toBytes());
    }
}
```

### Use Case 3: Data Aggregation from Multiple Versions

```java
public class OrderAggregator {

    public OrderSummary aggregate(List<Order> orders) {
        long totalAmount = 0;
        int itemCount = 0;

        for (Order order : orders) {
            // Unified API works regardless of version
            totalAmount += order.getTotalAmount();
            itemCount += order.getItems().size();
        }

        return new OrderSummary(totalAmount, itemCount);
    }
}
```

### Use Case 4: Testing with Multiple Versions

```java
@ParameterizedTest
@ValueSource(strings = {"v1", "v2"})
void testOrderProcessing(String versionId) {
    // Create test data for each version
    VersionContext ctx = VersionContext.forVersionId(versionId);

    Order order = createTestOrder(ctx);

    // Test business logic
    Order result = orderService.process(order);

    // Assertions work for any version
    assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
}
```

---

## Well-Known Types

Since v1.3.0, proto-wrapper automatically converts Google Well-Known Types to Java standard library types.

### Temporal Types

```protobuf
// Proto definition
message Event {
    google.protobuf.Timestamp created_at = 1;
    google.protobuf.Duration timeout = 2;
}
```

```java
// Generated interface
public interface Event {
    Instant getCreatedAt();    // java.time.Instant
    Duration getTimeout();      // java.time.Duration
}

// Usage
Event event = ctx.wrapEvent(protoEvent);
Instant createdAt = event.getCreatedAt();
Duration timeout = event.getTimeout();

// Java Time API operations
LocalDateTime local = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
long minutes = timeout.toMinutes();
```

### Wrapper Types (Nullable Primitives)

```protobuf
message UserProfile {
    google.protobuf.StringValue nickname = 1;
    google.protobuf.Int32Value age = 2;
    google.protobuf.BoolValue verified = 3;
}
```

```java
// Generated interface - nullable types!
public interface UserProfile {
    String getNickname();       // null if not set
    Integer getAge();           // null if not set
    Boolean getVerified();      // null if not set
}

// Usage - can distinguish "not set" from default value
UserProfile profile = ctx.wrapUserProfile(proto);

String nickname = profile.getNickname();
if (nickname == null) {
    System.out.println("User has no nickname");
}

Integer age = profile.getAge();
if (age != null && age >= 18) {
    System.out.println("User is an adult");
}
```

### FieldMask

```protobuf
message UpdateRequest {
    string id = 1;
    google.protobuf.FieldMask update_mask = 2;
}
```

```java
// Generated interface
public interface UpdateRequest {
    String getId();
    List<String> getUpdateMask();  // List of field paths
}

// Usage
UpdateRequest request = ctx.wrapUpdateRequest(proto);
List<String> fieldsToUpdate = request.getUpdateMask();
// fieldsToUpdate = ["name", "email", "address.city"]
```

### Struct/Value/ListValue (JSON-like)

```protobuf
message ApiResponse {
    google.protobuf.Struct metadata = 1;
    google.protobuf.Value dynamic_data = 2;
}
```

```java
// Generated interface
public interface ApiResponse {
    Map<String, Object> getMetadata();
    Object getDynamicData();
}

// Usage
ApiResponse response = ctx.wrapApiResponse(proto);
Map<String, Object> metadata = response.getMetadata();

// Values can be: null, Double, String, Boolean, Map, List
String type = (String) metadata.get("type");
Double count = (Double) metadata.get("count");
Map<?, ?> nested = (Map<?, ?>) metadata.get("nested_object");
List<?> items = (List<?>) metadata.get("items");

// Dynamic data
Object data = response.getDynamicData();
if (data instanceof Map<?, ?> map) {
    // Handle object
} else if (data instanceof List<?> list) {
    // Handle array
} else if (data instanceof String s) {
    // Handle string
}
```

### Building with Well-Known Types

```java
Event event = Event.newBuilder(ctx)
    .setCreatedAt(Instant.now())
    .setTimeout(Duration.ofMinutes(30))
    .build();

UserProfile profile = UserProfile.newBuilder(ctx)
    .setNickname("john_doe")
    .setAge(25)
    .setVerified(true)
    .build();

ApiResponse response = ApiResponse.newBuilder(ctx)
    .setMetadata(Map.of(
        "status", "success",
        "count", 42.0,
        "tags", List.of("a", "b", "c"),
        "nested", Map.of("key", "value")
    ))
    .build();
```

### Repeated Well-Known Types

```protobuf
message EventLog {
    repeated google.protobuf.Timestamp events = 1;
}
```

```java
// Generated interface
public interface EventLog {
    List<Instant> getEvents();  // List<java.time.Instant>
}

// Builder
EventLog log = EventLog.newBuilder(ctx)
    .addEvents(Instant.now())
    .addEvents(Instant.now().minusHours(1))
    .build();
```

### Disabling WKT Conversion

If you prefer the original protobuf types:

```xml
<configuration>
    <convertWellKnownTypes>false</convertWellKnownTypes>
</configuration>
```

---

## Troubleshooting

### Error: "protoc not found"

```
protoc not found. Please install protobuf compiler or set protocPath parameter.
```

**Solution**: Install protobuf compiler or specify path:

```xml
<configuration>
    <protocPath>/usr/local/bin/protoc</protocPath>
</configuration>
```

### Error: "Cannot resolve symbol" in IDE

IDE may show errors for `${os.detected.classifier}` - this is normal.

**Solution**: Run `mvn compile` from command line.

### Warning: "Type conflict for field"

```
[WARN] Type conflict for field 'value' [WIDENING]: v1:int, v2:long
```

This is informational. The plugin automatically handles the conflict.

### Builder setter not generated for a field

Check the conflict type. Builder setters are not generated for:
- `INCOMPATIBLE` conflicts (e.g., string ↔ int)

**Note**: Since v1.4.0, repeated fields with type conflicts have full builder support.
Since v1.6.2, `PRIMITIVE_MESSAGE` conflicts have dual setters with runtime validation.

**Workaround for INCOMPATIBLE**: Use typed proto builder directly.

### Enum not found in unified API

Verify the enum is defined identically in both versions:
- Same value names
- Same numeric codes

The plugin auto-detects equivalent enums.

---

## See Also

- [Getting Started](GETTING_STARTED.md) - Step-by-step setup tutorial
- [Configuration](CONFIGURATION.md) - All plugin options
- [Known Issues](KNOWN_ISSUES.md) - Known limitations
- [API Reference](API_REFERENCE.md) - Generated code API reference
