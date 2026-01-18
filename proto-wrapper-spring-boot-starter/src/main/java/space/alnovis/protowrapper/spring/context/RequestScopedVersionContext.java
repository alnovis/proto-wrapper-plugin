package space.alnovis.protowrapper.spring.context;

/**
 * Request-scoped holder for the current VersionContext.
 *
 * <p>The VersionContext is set by {@link space.alnovis.protowrapper.spring.web.VersionContextRequestFilter}
 * at the beginning of each HTTP request based on the X-Protocol-Version header.
 *
 * <p>Usage:
 * <pre>{@code
 * @RestController
 * public class OrderController {
 *     private final RequestScopedVersionContext versionContext;
 *
 *     public OrderController(RequestScopedVersionContext versionContext) {
 *         this.versionContext = versionContext;
 *     }
 *
 *     @PostMapping("/orders")
 *     public Order createOrder(@RequestBody byte[] protoBytes) {
 *         return versionContext.get().wrapOrder(protoBytes);
 *     }
 * }
 * }</pre>
 *
 * <p>Note: This class is annotated with @RequestScope in the auto-configuration.
 */
public class RequestScopedVersionContext {

    private Object context;
    private String version;

    /**
     * Get the current VersionContext.
     *
     * @param <T> expected VersionContext type
     * @return current VersionContext
     * @throws IllegalStateException if context has not been set
     */
    @SuppressWarnings("unchecked")
    public <T> T get() {
        if (context == null) {
            throw new IllegalStateException(
                "VersionContext not set. Ensure VersionContextRequestFilter is active " +
                "and request includes version header or default version is configured.");
        }
        return (T) context;
    }

    /**
     * Get the current version string.
     *
     * @return version string (e.g., "v2")
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the VersionContext for this request.
     * Called by {@link space.alnovis.protowrapper.spring.web.VersionContextRequestFilter}.
     *
     * @param context VersionContext instance
     * @param version version string
     */
    public void set(Object context, String version) {
        this.context = context;
        this.version = version;
    }

    /**
     * Check if context has been set.
     *
     * @return true if context is available
     */
    public boolean isPresent() {
        return context != null;
    }

    /**
     * Get the context if present, or null.
     *
     * @param <T> expected VersionContext type
     * @return context or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrNull() {
        return (T) context;
    }
}
