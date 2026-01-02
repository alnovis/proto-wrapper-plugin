package space.alnovis.protowrapper.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 *     .version("v203")
 *     .addDetail("availableVersions", List.of("v201", "v202"))
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
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns the full field path (e.g., "Order.items[].product.name").
     */
    public String getFieldPath() {
        return fieldPath;
    }

    /**
     * Returns the protocol version (e.g., "v203").
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the field name without path.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the protobuf field number.
     */
    public Integer getFieldNumber() {
        return fieldNumber;
    }

    /**
     * Returns the set of available/supported versions.
     */
    public Set<String> getAvailableVersions() {
        return availableVersions;
    }

    /**
     * Returns additional context details.
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
     * @return location string (e.g., "Order.items[]" or "Order (v203)")
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

        if (version != null && sb.length() > 0) {
            sb.append(" (").append(version).append(")");
        } else if (version != null) {
            sb.append(version);
        }

        return sb.length() > 0 ? sb.toString() : "unknown location";
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

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder fieldPath(String fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder fieldNumber(Integer fieldNumber) {
            this.fieldNumber = fieldNumber;
            return this;
        }

        public Builder availableVersions(Set<String> versions) {
            this.availableVersions = versions;
            return this;
        }

        public Builder availableVersions(List<String> versions) {
            this.availableVersions = versions != null ? Set.copyOf(versions) : null;
            return this;
        }

        public Builder addDetail(String key, Object value) {
            if (this.additionalDetails == null) {
                this.additionalDetails = new LinkedHashMap<>();
            }
            this.additionalDetails.put(key, value);
            return this;
        }

        public ErrorContext build() {
            return new ErrorContext(this);
        }
    }
}
