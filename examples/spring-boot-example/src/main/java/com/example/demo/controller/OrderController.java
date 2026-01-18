package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import space.alnovis.protowrapper.spring.context.RequestScopedVersionContext;
import space.alnovis.protowrapper.spring.context.VersionContextProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Example REST controller demonstrating proto-wrapper Spring Boot integration.
 *
 * <p>Shows two approaches to using version contexts:
 * <ul>
 *   <li>Request-scoped injection via {@link RequestScopedVersionContext}</li>
 *   <li>Direct provider injection via {@link VersionContextProvider}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RequestScopedVersionContext versionContext;
    private final VersionContextProvider versionContextProvider;

    public OrderController(
            RequestScopedVersionContext versionContext,
            VersionContextProvider versionContextProvider) {
        this.versionContext = versionContext;
        this.versionContextProvider = versionContextProvider;
    }

    /**
     * Get current protocol version info.
     *
     * <p>Example:
     * <pre>
     * curl http://localhost:8080/api/version
     * curl -H "X-Protocol-Version: v2" http://localhost:8080/api/version
     * </pre>
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersionInfo() {
        String currentVersion = versionContext.getVersion();
        List<String> supportedVersions = versionContextProvider.getSupportedVersions();
        String defaultVersion = versionContextProvider.getDefaultVersion();

        log.info("Version info requested. Current: {}", currentVersion);

        return ResponseEntity.ok(Map.of(
            "currentVersion", currentVersion,
            "supportedVersions", supportedVersions,
            "defaultVersion", defaultVersion
        ));
    }

    /**
     * Create a new order using version-agnostic wrapper.
     *
     * <p>The protocol version is determined by X-Protocol-Version header.
     * The wrapper handles serialization/deserialization automatically.
     *
     * <p>Example:
     * <pre>
     * # Using v1 protocol
     * curl -X POST http://localhost:8080/api/orders \
     *   -H "Content-Type: application/json" \
     *   -H "X-Protocol-Version: v1" \
     *   -d '{"customerName": "John Doe", "items": [{"productId": "P001", "name": "Widget", "quantity": 2, "price": 19.99}]}'
     *
     * # Using v2 protocol (with shipping address)
     * curl -X POST http://localhost:8080/api/orders \
     *   -H "Content-Type: application/json" \
     *   -H "X-Protocol-Version: v2" \
     *   -d '{"customerName": "John Doe", "items": [...], "shippingAddress": {"street": "123 Main St", "city": "NYC", "country": "USA", "postalCode": "10001"}}'
     * </pre>
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        String version = versionContext.getVersion();
        log.info("Creating order with protocol version: {}", version);

        // In real application, you would use the VersionContext to get
        // version-specific wrapper and build protobuf message:
        //
        // VersionContext ctx = versionContext.get();
        // Order.Builder builder = ctx.orderBuilder();
        // builder.setCustomerName(request.get("customerName"));
        // ...
        // byte[] protoBytes = builder.build().toByteArray();

        // For this demo, we just simulate order creation
        String orderId = UUID.randomUUID().toString();
        String customerName = (String) request.get("customerName");

        Map<String, Object> response = Map.of(
            "orderId", orderId,
            "customerName", customerName,
            "protocolVersion", version,
            "status", "CREATED",
            "message", "Order created using " + version + " protocol"
        );

        log.info("Order {} created successfully", orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get order by ID (demonstrates direct provider usage).
     *
     * <p>Example:
     * <pre>
     * curl http://localhost:8080/api/orders/123?version=v1
     * curl http://localhost:8080/api/orders/123?version=v2
     * </pre>
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String version) {

        // Use explicit version or fall back to request-scoped context
        String effectiveVersion = version != null ? version : versionContext.getVersion();

        // Validate version if explicitly provided
        if (version != null && !versionContextProvider.isSupported(version)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "VERSION_NOT_SUPPORTED",
                "message", "Version '" + version + "' is not supported",
                "supportedVersions", versionContextProvider.getSupportedVersions()
            ));
        }

        log.info("Getting order {} with version {}", orderId, effectiveVersion);

        // In real application:
        // Object ctx = versionContextProvider.getContext(effectiveVersion);
        // Order order = ... // fetch from database
        // return wrapper.wrapOrder(order);

        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "protocolVersion", effectiveVersion,
            "customerName", "Demo Customer",
            "total", 99.99,
            "status", "PENDING"
        ));
    }
}
