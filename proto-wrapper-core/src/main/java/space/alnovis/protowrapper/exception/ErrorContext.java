package space.alnovis.protowrapper.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contextual information for proto-wrapper exceptions.
 *
 * <p>Provides structured error context including field path, version information,
 * and additional details for debugging and error handling.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ErrorContext context = ErrorContext.builder()
 *     .messageType("Order")
 *     .fieldPath("items[].product.name")
 *     .version("v3")
 *     .addDetail("availableVersions", List.of("v1", "v2"))
 *     .build();
 * }</pre>
 */
public final class ErrorContext {

    private final String messageType;
    private final String fieldPath;
    private final String version;
    private final String fieldName;
    private final Integer fieldNumber;
    private final Set<String> availableVersions;
    private final Map<String, Object> additionalDetails;

    private ErrorContext(Builder builder) {
        this.messageType = builder.messageType;
        this.fieldPath = builder.fieldPath;
        this.version = builder.version;
        this.fieldName = builder.fieldName;
        this.fieldNumber = builder.fieldNumber;
        this.availableVersions = builder.availableVersions != null
                ? Set.copyOf(builder.availableVersions)
                : Set.of();
        this.additionalDetails = builder.additionalDetails != null
                ? Map.copyOf(builder.additionalDetails)
                : Map.of();
    }

    /**
     * Creates a new builder for ErrorContext.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple context with just message type.
     *
     * @param messageType the message type name
     * @return new ErrorContext instance
     */
    public static ErrorContext forMessage(String messageType) {
        return builder().messageType(messageType).build();
    }

    /**
     * Creates a context for a specific field.
     *
     * @param messageType the message type name
     * @param fieldName the field name
     * @return new ErrorContext instance
     */
    public static ErrorContext forField(String messageType, String fieldName) {
        return builder()
                .messageType(messageType)
                .fieldName(fieldName)
                .fieldPath(messageType + "." + fieldName)
                .build();
    }

    /**
     * Creates a context for a version-specific error.
     *
     * @param messageType the message type name
     * @param version the protocol version
     * @return new ErrorContext instance
     */
    public static ErrorContext forVersion(String messageType, String version) {
        return builder()
                .messageType(messageType)
                .version(version)
                .build();
    }

    // Getters

    /**
     * Returns the message type name (e.g., "Order", "Person").
     *
     * @return the message type name
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns the full field path (e.g., "Order.items[].product.name").
     *
     * @return the field path
     */
    public String getFieldPath() {
        return fieldPath;
    }

    /**
     * Returns the protocol version (e.g., "v3").
     *
     * @return the protocol version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the field name without path.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the protobuf field number.
     *
     * @return the field number
     */
    public Integer getFieldNumber() {
        return fieldNumber;
    }

    /**
     * Returns the set of available/supported versions.
     *
     * @return the available versions set
     */
    public Set<String> getAvailableVersions() {
        return availableVersions;
    }

    /**
     * Returns additional context details.
     *
     * @return the additional details map
     */
    public Map<String, Object> getAdditionalDetails() {
        return additionalDetails;
    }

    /**
     * Gets a specific detail value.
     *
     * @param key the detail key
     * @param <T> the expected type
     * @return the detail value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return (T) additionalDetails.get(key);
    }

    /**
     * Checks if this context has a specific detail.
     *
     * @param key the detail key
     * @return true if the detail exists
     */
    public boolean hasDetail(String key) {
        return additionalDetails.containsKey(key);
    }

    /**
     * Returns a human-readable location string for error messages.
     *
     * @return location string (e.g., "Order.items[]" or "Order (v3)")
     */
    public String getLocation() {
        StringBuilder sb = new StringBuilder();

        if (fieldPath != null) {
            sb.append(fieldPath);
        } else if (messageType != null) {
            sb.append(messageType);
            if (fieldName != null) {
                sb.append(".").append(fieldName);
            }
        }

        if (version != null && !sb.isEmpty()) {
            sb.append(" (").append(version).append(")");
        } else if (version != null) {
            sb.append(version);
        }

        return !sb.isEmpty() ? sb.toString() : "unknown location";
    }

    @Override
    public String toString() {
        return "ErrorContext{" +
                "location=" + getLocation() +
                (availableVersions.isEmpty() ? "" : ", availableVersions=" + availableVersions) +
                (additionalDetails.isEmpty() ? "" : ", details=" + additionalDetails) +
                '}';
    }

    /**
     * Builder for ErrorContext.
     */
    public static final class Builder {
        private String messageType;
        private String fieldPath;
        private String version;
        private String fieldName;
        private Integer fieldNumber;
        private Set<String> availableVersions;
        private Map<String, Object> additionalDetails;

        private Builder() {
        }

        /**
         * Set the message type.
         *
         * @param messageType the message type name
         * @return this builder
         */
        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * Set the field path.
         *
         * @param fieldPath the field path
         * @return this builder
         */
        public Builder fieldPath(String fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        /**
         * Set the version.
         *
         * @param version the protocol version
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Set the field name.
         *
         * @param fieldName the field name
         * @return this builder
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Set the field number.
         *
         * @param fieldNumber the protobuf field number
         * @return this builder
         */
        public Builder fieldNumber(Integer fieldNumber) {
            this.fieldNumber = fieldNumber;
            return this;
        }

        /**
         * Set the available versions.
         *
         * @param versions the set of available versions
         * @return this builder
         */
        public Builder availableVersions(Set<String> versions) {
            this.availableVersions = versions;
            return this;
        }

        /**
         * Set the available versions from a list.
         *
         * @param versions the list of available versions
         * @return this builder
         */
        public Builder availableVersions(List<String> versions) {
            this.availableVersions = versions != null ? Set.copyOf(versions) : null;
            return this;
        }

        /**
         * Add an additional detail.
         *
         * @param key the detail key
         * @param value the detail value
         * @return this builder
         */
        public Builder addDetail(String key, Object value) {
            if (this.additionalDetails == null) {
                this.additionalDetails = new LinkedHashMap<>();
            }
            this.additionalDetails.put(key, value);
            return this;
        }

        /**
         * Build the ErrorContext.
         *
         * @return the built ErrorContext
         */
        public ErrorContext build() {
            return new ErrorContext(this);
        }
    }
}
