# Spring Boot Starter

The `proto-wrapper-spring-boot-starter` provides auto-configuration for integrating Proto Wrapper with Spring Boot applications.

**Requirements:**
- Spring Boot 3.0+ (uses Jakarta EE)
- Java 17+

## Installation

Add the starter dependency to your project:

**Maven:**
```xml
<dependency>
    <groupId>io.alnovis</groupId>
    <artifactId>proto-wrapper-spring-boot-starter</artifactId>
    <version>1.6.7</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("io.alnovis:proto-wrapper-spring-boot-starter:1.6.7")
```

## Configuration

Configure the starter in `application.yml`:

```yaml
proto-wrapper:
  # Required: package where generated VersionContext is located
  base-package: com.example.model.api

  # Optional: HTTP header for version detection (default: X-Protocol-Version)
  version-header: X-Protocol-Version

  # Optional: default version when header is missing
  default-version: v2

  # Optional: enable request-scoped version context (default: true)
  request-scoped: true
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `proto-wrapper.base-package` | String | (required) | Package containing generated `VersionContext` |
| `proto-wrapper.version-header` | String | `X-Protocol-Version` | HTTP header name for version |
| `proto-wrapper.default-version` | String | (from VersionContext) | Fallback version when header missing |
| `proto-wrapper.request-scoped` | boolean | `true` | Enable per-request version context |

## Components

### VersionContextProvider

Interface for accessing version contexts programmatically:

```java
public interface VersionContextProvider {
    Object getContext(String versionId);
    List<String> getSupportedVersions();
    String getDefaultVersion();
    boolean isSupported(String versionId);
}
```

**Usage:**
```java
@Service
public class OrderService {
    private final VersionContextProvider provider;

    public OrderService(VersionContextProvider provider) {
        this.provider = provider;
    }

    public void processOrder(String versionId, byte[] data) {
        if (!provider.isSupported(versionId)) {
            throw new IllegalArgumentException("Unsupported version: " + versionId);
        }

        // Get typed context (cast to your generated VersionContext)
        VersionContext ctx = (VersionContext) provider.getContext(versionId);
        Order order = ctx.parseOrderFromBytes(data);
        // ...
    }
}
```

### RequestScopedVersionContext

Request-scoped bean that holds the current version for the HTTP request:

```java
@RestController
public class OrderController {
    private final RequestScopedVersionContext versionContext;

    public OrderController(RequestScopedVersionContext versionContext) {
        this.versionContext = versionContext;
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Version is automatically extracted from X-Protocol-Version header
        String version = versionContext.getVersion();
        Object ctx = versionContext.getContext();

        // Use the context...
    }
}
```

### VersionContextRequestFilter

Servlet filter that extracts version from HTTP headers and populates `RequestScopedVersionContext`.

**Request flow:**
1. Client sends request with `X-Protocol-Version: v2` header
2. Filter extracts version from header
3. Filter sets version in `RequestScopedVersionContext`
4. Controller receives request with version already set
5. After request, context is cleared

**Header examples:**
```bash
# Explicit version
curl -H "X-Protocol-Version: v1" http://localhost:8080/api/orders

# No header - uses default version
curl http://localhost:8080/api/orders
```

### ProtoWrapperExceptionHandler

Global exception handler for Proto Wrapper errors:

```java
@ControllerAdvice
public class ProtoWrapperExceptionHandler {
    // Handles IllegalArgumentException for invalid versions
    // Returns 400 Bad Request with error details
}
```

**Response format:**
```json
{
  "error": "INVALID_VERSION",
  "message": "Unsupported version: v99",
  "supportedVersions": ["v1", "v2"]
}
```

## Usage Patterns

### Pattern 1: Request-Scoped Version (Recommended)

Best for REST APIs where version comes from HTTP header:

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    private final RequestScopedVersionContext versionContext;
    private final VersionContextProvider provider;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody byte[] data) {
        // Version automatically set from header
        VersionContext ctx = (VersionContext) versionContext.getContext();

        Order order = ctx.parseOrderFromBytes(data);
        // Process order...

        OrderResponse response = ctx.newOrderResponseBuilder()
            .setOrderId(order.getOrderId())
            .setStatus(OrderStatus.CREATED)
            .build();

        return ResponseEntity.ok(response);
    }
}
```

### Pattern 2: Explicit Version Parameter

When version comes from query parameter or path:

```java
@GetMapping("/orders/{id}")
public ResponseEntity<?> getOrder(
        @PathVariable String id,
        @RequestParam(required = false) String version) {

    // Use explicit version or fall back to request-scoped
    String effectiveVersion = version != null
        ? version
        : versionContext.getVersion();

    if (!provider.isSupported(effectiveVersion)) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Unsupported version: " + effectiveVersion));
    }

    VersionContext ctx = (VersionContext) provider.getContext(effectiveVersion);
    // ...
}
```

### Pattern 3: Service Layer with Multiple Versions

Processing messages from different versions:

```java
@Service
public class MessageProcessor {
    private final VersionContextProvider provider;

    public void processMessage(String versionId, byte[] payload) {
        VersionContext ctx = (VersionContext) provider.getContext(versionId);

        Message message = ctx.parseMessageFromBytes(payload);

        // Process using version-agnostic interface
        handleMessage(message);
    }

    private void handleMessage(Message message) {
        // Works with any version
        System.out.println("Type: " + message.getType());
        System.out.println("Content: " + message.getContent());
    }
}
```

### Pattern 4: Kafka Consumer with Version Header

```java
@KafkaListener(topics = "orders")
public void handleOrder(
        @Payload byte[] payload,
        @Header("proto-version") String version) {

    VersionContext ctx = (VersionContext) provider.getContext(version);
    Order order = ctx.parseOrderFromBytes(payload);

    orderService.process(order);
}
```

## Version Discovery

Get information about available versions:

```java
@GetMapping("/api/versions")
public Map<String, Object> getVersionInfo() {
    return Map.of(
        "supported", provider.getSupportedVersions(),
        "default", provider.getDefaultVersion(),
        "current", versionContext.getVersion()
    );
}
```

**Response:**
```json
{
  "supported": ["v1", "v2"],
  "default": "v2",
  "current": "v1"
}
```

## Disabling Auto-Configuration

To disable specific components:

```yaml
proto-wrapper:
  request-scoped: false  # Disable request-scoped context
```

Or exclude entirely:

```java
@SpringBootApplication(exclude = ProtoWrapperAutoConfiguration.class)
public class MyApplication { }
```

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    VersionContextProvider provider;

    @Test
    void shouldProcessOrder() {
        // Given
        VersionContext mockCtx = mock(VersionContext.class);
        when(provider.getContext("v1")).thenReturn(mockCtx);
        when(provider.isSupported("v1")).thenReturn(true);

        // When
        OrderService service = new OrderService(provider);
        service.processOrder("v1", testData);

        // Then
        verify(mockCtx).parseOrderFromBytes(testData);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldCreateOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("X-Protocol-Version", "v1")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(orderBytes))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void shouldRejectInvalidVersion() throws Exception {
        mockMvc.perform(get("/api/orders/123")
                .header("X-Protocol-Version", "v99"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_VERSION"));
    }
}
```

## Troubleshooting

### "Could not find VersionContext class"

**Cause:** `base-package` is incorrect or generated code not on classpath.

**Fix:** Verify package matches your `basePackage` configuration in the plugin:
```yaml
proto-wrapper:
  base-package: com.example.model.api  # Must match plugin config
```

### "No bean of type VersionContextProvider"

**Cause:** Auto-configuration not triggered.

**Fix:**
1. Ensure `proto-wrapper-spring-boot-starter` is in dependencies
2. Check that `base-package` is configured
3. Verify generated code is compiled

### Request-scoped context returns null

**Cause:** Accessing context outside of HTTP request scope.

**Fix:** Use `VersionContextProvider` instead for non-request contexts:
```java
// In @Async, @Scheduled, or non-web contexts
Object ctx = provider.getContext("v1");  // Works anywhere
```

## See Also

- [Getting Started](GETTING_STARTED.md) - Basic setup tutorial
- [Configuration](CONFIGURATION.md) - Plugin configuration options
- [API Reference](API_REFERENCE.md) - Generated code reference
- [Spring Boot Example](../examples/spring-boot-example) - Complete example project
