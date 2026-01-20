# Proto Wrapper Maven Example

This example demonstrates all features of the `proto-wrapper-maven-plugin`.

## Features Demonstrated

### 1. Multi-Version Support
- `proto/v1/` - First version of the API
- `proto/v2/` - Enhanced version with new fields and enums

### 2. Enum Types
- **Top-level enums**: `Status`, `OrderStatus`, `PaymentMethod`, `UserRole`
- **Nested enums**: `Address.AddressType`, `OrderItem.Discount.DiscountType`, `DisplaySettings.Theme`
- **Version-specific enums**: `Priority`, `DeliveryMethod`, `AuthMethod` (v2 only)

### 3. Message Types
- **Simple messages**: `Date`, `Money`
- **Messages with nested types**: `Address` (contains `GeoLocation`), `OrderItem` (contains `Discount`)
- **Deeply nested structures**: `UserProfile.Preferences.DisplaySettings`
- **Messages with repeated fields**: `OrderRequest.items`, `UserProfile.recent_activity`

### 4. Version Evolution
- New fields added in v2 (e.g., `Money.exchange_rate`, `Address.location`)
- New enum values in v2 (e.g., `OrderStatus.ORDER_RETURNED`)
- New nested types in v2 (e.g., `OrderResponse.DeliveryInfo`)

## Project Structure

```
maven-example/
├── pom.xml                          # Maven configuration
├── proto/
│   ├── v1/
│   │   ├── common.proto             # Common types (Date, Money, Address, Status)
│   │   ├── order.proto              # Order-related messages
│   │   └── user.proto               # User-related messages
│   └── v2/
│       ├── common.proto             # Enhanced common types
│       ├── order.proto              # Enhanced order messages
│       └── user.proto               # Enhanced user messages
└── src/main/java/
    └── com/example/demo/
        └── ProtoWrapperDemo.java    # Usage examples
```

## Generated Code Structure

After running `mvn generate-sources`:

```
target/generated-sources/
├── protobuf-v1/                     # Protobuf-generated Java classes for v1
├── protobuf-v2/                     # Protobuf-generated Java classes for v2
└── proto-wrapper/
    └── com/example/model/
        ├── api/                     # Version-agnostic interfaces
        │   ├── Money.java           # Interface
        │   ├── OrderStatus.java     # Enum
        │   ├── VersionContext.java  # Factory interface
        │   └── impl/
        │       └── AbstractMoney.java
        ├── v1/                      # V1 implementations
        │   ├── Money.java           # (same name, different package)
        │   └── VersionContext.java
        └── v2/                      # V2 implementations
            ├── Money.java
            └── VersionContext.java
```

## Usage

### Build the project

```bash
mvn clean compile
```

### Run the demo

```bash
mvn exec:java -Dexec.mainClass=com.example.demo.ProtoWrapperDemo
```

## Key Concepts

### Version-Agnostic Interfaces

All message types have a common interface in the `api` package:

```java
// Works with any version
public void processOrder(OrderResponse order) {
    System.out.println("Order: " + order.getOrderId());
    System.out.println("Status: " + order.getStatus());
}
```

### VersionContext

Use `VersionContext` to create wrappers at runtime:

```java
// Get context for a specific version (recommended)
VersionContext ctx = VersionContext.forVersionId("v1");

// Other useful static methods
Optional<VersionContext> maybeCtx = VersionContext.find("v1");
VersionContext defaultCtx = VersionContext.getDefault();
boolean supported = VersionContext.isSupported("v1");
List<String> versions = VersionContext.supportedVersions();

// Wrap a proto message
OrderResponse order = ctx.wrapOrderResponse(protoMessage);
```

### Using Version-Specific Implementations

With `includeVersionSuffix=false`, implementations are in version packages:

```java
// Use fully qualified names to distinguish versions
com.example.model.v1.Money v1Money = new com.example.model.v1.Money(protoV1);
com.example.model.v2.Money v2Money = new com.example.model.v2.Money(protoV2);

// Or use the common interface
Money money = new com.example.model.v1.Money(protoV1);
```

### Nested Types

Nested messages and enums are fully supported:

```java
OrderItem item = ...;
OrderItem.Discount discount = item.getDiscount();
OrderItem.Discount.DiscountType type = discount.getType();
```

## Plugin Configuration

See `pom.xml` for full configuration options:

```xml
<plugin>
    <groupId>io.github.alnovis</groupId>
    <artifactId>proto-wrapper-maven-plugin</artifactId>
    <configuration>
        <basePackage>com.example.model</basePackage>
        <protoPackagePattern>com.example.proto.{version}</protoPackagePattern>
        <protoRoot>${basedir}/proto</protoRoot>
        <includeVersionSuffix>false</includeVersionSuffix>
        <versions>
            <version><protoDir>v1</protoDir></version>
            <version><protoDir>v2</protoDir></version>
        </versions>
    </configuration>
</plugin>
```

### includeVersionSuffix Option

- `false` (default): Classes named `Money.java` in packages `v1`/`v2`
- `true`: Classes named `MoneyV1.java`/`MoneyV2.java` in a single package
