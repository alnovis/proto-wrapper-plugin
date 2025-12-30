# Release Notes - Proto Wrapper Plugin v1.1.1

**Release Date:** December 30, 2025

## Overview

This release introduces the static `newBuilder(VersionContext)` method on all generated interfaces, providing a more intuitive and consistent API for creating builder instances that mirrors the native protobuf pattern.

## What's New

### Static newBuilder(VersionContext) Method

All generated interfaces now include a static `newBuilder(ctx)` method for creating builders:

```java
// Before (still works)
Money money = ctx.newMoneyBuilder()
        .setAmount(1000)
        .setCurrency("USD")
        .build();

// After (new intuitive approach)
Money money = Money.newBuilder(ctx)
        .setAmount(1000)
        .setCurrency("USD")
        .build();
```

**Why this matters:**
- More intuitive - matches the native protobuf pattern `Type.newBuilder()`
- Better IDE discoverability - type the class name, then `.newBuilder`
- Cleaner code when building complex object graphs
- Consistent API across all generated interfaces

### Nested Interface Support

The static `newBuilder(ctx)` method is available on nested interfaces too:

```java
// Nested GeoLocation inside Address
Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
        .setLatitude(40.7128)
        .setLongitude(-74.0060)
        .setAccuracy(10.0)
        .build();

// Nested SecurityChallenge inside AuthResponse
AuthResponse.SecurityChallenge challenge = AuthResponse.SecurityChallenge.newBuilder(ctx)
        .setType(ChallengeType.SMS_VERIFICATION)
        .setChallengeId("CHAL-123")
        .setHint("Enter 6-digit code")
        .build();
```

### Complex Object Graphs

Building complex nested structures is now cleaner:

```java
VersionContext ctx = VersionContext.forVersion(2);

OrderRequest order = OrderRequest.newBuilder(ctx)
        .setOrderId("ORD-001")
        .setCustomer(Customer.newBuilder(ctx)
                .setId("CUST-001")
                .setName("John Doe")
                .setEmail("john@example.com")
                .setShippingAddress(Address.newBuilder(ctx)
                        .setStreet("123 Main St")
                        .setCity("Boston")
                        .setCountry("USA")
                        .setLocation(Address.GeoLocation.newBuilder(ctx)
                                .setLatitude(42.3601)
                                .setLongitude(-71.0589)
                                .build())
                        .build())
                .build())
        .addItems(OrderItem.newBuilder(ctx)
                .setProductId("PROD-001")
                .setProductName("Widget")
                .setQuantity(2)
                .setUnitPrice(Money.newBuilder(ctx)
                        .setAmount(2500)
                        .setCurrency("USD")
                        .build())
                .build())
        .setTotalAmount(Money.newBuilder(ctx)
                .setAmount(5000)
                .setCurrency("USD")
                .build())
        .setPaymentMethod(PaymentMethod.CARD)
        .setOrderDate(Date.newBuilder(ctx)
                .setYear(2024)
                .setMonth(12)
                .setDay(30)
                .build())
        .build();
```

### Context Propagation

The built objects retain their context, allowing chained creation:

```java
Money money = Money.newBuilder(ctx)
        .setAmount(100)
        .setCurrency("USD")
        .build();

// Use the context from the built object
Date date = Date.newBuilder(money.getContext())
        .setYear(2024)
        .setMonth(6)
        .setDay(15)
        .build();
```

## Upgrade Guide

### 1. Update Plugin Version

**Maven:**
```xml
<plugin>
    <groupId>space.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <version>1.1.1</version>
</plugin>
```

**Gradle:**
```kotlin
plugins {
    id("space.alnovis.proto-wrapper") version "1.1.1"
}
```

### 2. Regenerate Code

**Maven:**
```bash
mvn clean compile
```

**Gradle:**
```bash
./gradlew clean generateProtoWrapper
```

### 3. Use New Feature (Optional)

Replace `ctx.newTypeBuilder()` calls with `Type.newBuilder(ctx)` for improved readability:

```java
// Old style (still works)
Money money = ctx.newMoneyBuilder().setAmount(100).build();

// New style (recommended)
Money money = Money.newBuilder(ctx).setAmount(100).build();
```

## Breaking Changes

None. All changes are backward compatible.

## API Compatibility

- Generated code is fully compatible with previous versions
- The new `newBuilder(ctx)` method is additive
- Existing `ctx.newTypeBuilder()` calls continue to work

## Test Coverage

| Test Class | Tests | Purpose |
|------------|-------|---------|
| `StaticNewBuilderTest` | 24 | Static newBuilder method |
| - TopLevelInterfaceTests | 8 | Money, Date, Address, UserProfile |
| - NestedInterfaceTests | 4 | GeoLocation, SecurityChallenge |
| - BuilderChainingTests | 2 | Fluent API, clear methods |
| - RoundTripTests | 2 | Serialization round-trip |
| - CrossVersionTests | 2 | V1/V2 conversion |
| - GetContextTests | 2 | Context propagation |
| - ComplexGraphTests | 2 | Nested object graphs |
| - ToBuilderTests | 2 | toBuilder/emptyBuilder |

**Total integration tests:** 106

## Generated Code Example

```java
public interface Money {
    // ... existing methods ...

    /**
     * Create a new builder for Money using the specified version context.
     * <p>This is a convenience method equivalent to {@code ctx.newMoneyBuilder()}.</p>
     * @param ctx Version context to use for builder creation
     * @return Empty builder for Money
     */
    static Builder newBuilder(VersionContext ctx) {
        return ctx.newMoneyBuilder();
    }

    interface Builder {
        Builder setAmount(long amount);
        Builder setCurrency(String currency);
        Money build();
    }
}
```

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## Links

- [GitHub Repository](https://github.com/alnovis/proto-wrapper-plugin)
- [Documentation](README.md)
- [API Reference](docs/API_REFERENCE.md)
- [Cookbook](docs/COOKBOOK.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0
