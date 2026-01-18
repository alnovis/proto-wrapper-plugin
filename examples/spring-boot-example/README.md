# Proto Wrapper Spring Boot Example

This example demonstrates how to integrate `proto-wrapper-spring-boot-starter` with a Spring Boot application to handle multi-version protobuf protocols.

## Features

- Request-scoped version context injection
- Version detection via HTTP headers
- Factory-based version context provider
- REST API with version-aware endpoints

## Project Structure

```
spring-boot-example/
├── src/main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java        # Spring Boot entry point
│   │   └── controller/
│   │       └── OrderController.java    # REST controller with version handling
│   ├── proto/
│   │   ├── v1/order.proto              # Protocol version 1
│   │   └── v2/order.proto              # Protocol version 2 (with new fields)
│   └── resources/
│       └── application.yml             # Configuration
└── pom.xml
```

## Prerequisites

- Java 17+
- Maven 3.6+
- proto-wrapper-plugin installed locally

## Building

```bash
# From the spring-boot-example directory
mvn clean compile

# Or from the examples parent directory
mvn clean compile -pl spring-boot-example
```

## Running

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

## Configuration

The starter is configured in `application.yml`:

```yaml
proto-wrapper:
  # Package where generated wrappers are located
  base-package: com.example.demo.api

  # Optional: HTTP header for version selection (default: X-Protocol-Version)
  # version-header: X-Protocol-Version

  # Optional: Provider type - FACTORY (default) or REFLECTIVE
  # provider-type: factory

  # Optional: Enable request-scoped version context (default: true)
  # request-scoped: true
```

## API Endpoints

### Get Version Info

```bash
# Get current version info (uses default version)
curl http://localhost:8080/api/version

# Specify protocol version via header
curl -H "X-Protocol-Version: v1" http://localhost:8080/api/version
curl -H "X-Protocol-Version: v2" http://localhost:8080/api/version
```

Response:
```json
{
  "currentVersion": "v2",
  "supportedVersions": ["v1", "v2"],
  "defaultVersion": "v2"
}
```

### Create Order

```bash
# Create order using v1 protocol
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Protocol-Version: v1" \
  -d '{
    "customerName": "John Doe",
    "items": [
      {"productId": "P001", "name": "Widget", "quantity": 2, "price": 19.99}
    ]
  }'

# Create order using v2 protocol (with shipping address)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Protocol-Version: v2" \
  -d '{
    "customerName": "John Doe",
    "items": [
      {"productId": "P001", "name": "Widget", "quantity": 2, "price": 19.99, "discount": 5.0}
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "country": "USA",
      "postalCode": "10001"
    }
  }'
```

### Get Order

```bash
# Get order using request-scoped version
curl http://localhost:8080/api/orders/123

# Get order with explicit version parameter
curl "http://localhost:8080/api/orders/123?version=v1"
curl "http://localhost:8080/api/orders/123?version=v2"
```

## Version Context Injection

The starter provides two ways to access version context:

### 1. Request-Scoped Injection

```java
@RestController
public class OrderController {
    private final RequestScopedVersionContext versionContext;

    public OrderController(RequestScopedVersionContext versionContext) {
        this.versionContext = versionContext;
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        String version = versionContext.getVersion();
        // Use version-specific logic
    }
}
```

### 2. Provider Injection

```java
@RestController
public class OrderController {
    private final VersionContextProvider provider;

    public OrderController(VersionContextProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam String version) {
        if (!provider.isSupported(version)) {
            return ResponseEntity.badRequest().body("Unsupported version");
        }
        Object ctx = provider.getContext(version);
        // Use specific version context
    }
}
```

## Protocol Schema Differences

### v1 Schema
- `OrderData`: id, customer_name, items, total
- `OrderItem`: product_id, name, quantity, price

### v2 Schema (additions)
- `OrderData`: + status, created_at, shipping_address
- `OrderItem`: + discount
- `ShippingAddress`: new message type

## Dependencies

| Dependency | Version |
|------------|---------|
| Spring Boot | 3.4.1 |
| proto-wrapper-spring-boot-starter | 1.6.7 |
| protobuf-java | 4.28.2 |

## See Also

- [Spring Boot Starter Guide](../../docs/SPRING_BOOT_STARTER.md) - Full documentation
- [Getting Started](../../docs/GETTING_STARTED.md) - Basic setup tutorial
- [Maven Example](../maven-example/README.md)
- [Gradle Example](../gradle-example/README.md)
