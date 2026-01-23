package io.alnovis.protowrapper.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a field mapping override for version-agnostic field matching.
 *
 * <p>By default, the merger groups fields by field number across versions.
 * A FieldMapping allows overriding this behavior for specific fields that
 * have different numbers across versions but represent the same semantic field.</p>
 *
 * <h2>Simple form (match by name)</h2>
 * <p>When only {@code message} and {@code fieldName} are specified, fields with
 * the same proto name are matched regardless of their field numbers.</p>
 *
 * <h2>Explicit form (match by version-specific numbers)</h2>
 * <p>When {@code versionNumbers} is also specified, fields are matched by
 * their version-specific field numbers.</p>
 *
 * <h2>Maven Configuration Example</h2>
 * <pre>{@code
 * <fieldMappings>
 *     <!-- Simple form: match by name -->
 *     <fieldMapping>
 *         <message>Order</message>
 *         <fieldName>parent_order</fieldName>
 *     </fieldMapping>
 *     <!-- Explicit form: match by version-specific numbers -->
 *     <fieldMapping>
 *         <message>Payment</message>
 *         <fieldName>amount</fieldName>
 *         <versionNumbers>
 *             <v1>17</v1>
 *             <v2>15</v2>
 *         </versionNumbers>
 *     </fieldMapping>
 * </fieldMappings>
 * }</pre>
 *
 * @since 2.2.0
 */
public class FieldMapping {

    private String message;
    private String fieldName;
    private Map<String, Integer> versionNumbers;

    /**
     * Default constructor for Maven/framework instantiation.
     */
    public FieldMapping() {
    }

    /**
     * Create a simple name-based field mapping.
     *
     * @param message the message name
     * @param fieldName the proto field name
     */
    public FieldMapping(String message, String fieldName) {
        this.message = message;
        this.fieldName = fieldName;
    }

    /**
     * Create an explicit field mapping with version-specific numbers.
     *
     * @param message the message name
     * @param fieldName the proto field name
     * @param versionNumbers map of version to field number
     */
    public FieldMapping(String message, String fieldName, Map<String, Integer> versionNumbers) {
        this.message = message;
        this.fieldName = fieldName;
        this.versionNumbers = versionNumbers != null
                ? new LinkedHashMap<>(versionNumbers)
                : null;
    }

    /**
     * Get the message name this mapping applies to.
     *
     * @return the message name
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the message name.
     *
     * @param message the message name
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the proto field name to match.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Set the proto field name.
     *
     * @param fieldName the field name
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Get the version-specific field numbers.
     *
     * @return map of version to field number, or null if using name-based matching
     */
    public Map<String, Integer> getVersionNumbers() {
        return versionNumbers != null
                ? Collections.unmodifiableMap(versionNumbers)
                : null;
    }

    /**
     * Set the version-specific field numbers.
     *
     * @param versionNumbers map of version to field number
     */
    public void setVersionNumbers(Map<String, Integer> versionNumbers) {
        this.versionNumbers = versionNumbers != null
                ? new LinkedHashMap<>(versionNumbers)
                : null;
    }

    /**
     * Check if this mapping uses explicit version-specific numbers.
     *
     * @return true if versionNumbers is specified and non-empty
     */
    public boolean hasExplicitNumbers() {
        return versionNumbers != null && !versionNumbers.isEmpty();
    }

    /**
     * Validate this mapping configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("FieldMapping: 'message' is required");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("FieldMapping: 'fieldName' is required for message '" + message + "'");
        }
        if (versionNumbers != null) {
            for (Map.Entry<String, Integer> entry : versionNumbers.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    throw new IllegalArgumentException(
                            "FieldMapping: version key cannot be blank in " + message + "." + fieldName);
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException(
                            "FieldMapping: field number must be positive in " + message + "." + fieldName
                                    + " for version '" + entry.getKey() + "'");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMapping that = (FieldMapping) o;
        return Objects.equals(message, that.message)
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(versionNumbers, that.versionNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, fieldName, versionNumbers);
    }

    @Override
    public String toString() {
        if (hasExplicitNumbers()) {
            return "FieldMapping[" + message + "." + fieldName + ", numbers=" + versionNumbers + "]";
        }
        return "FieldMapping[" + message + "." + fieldName + " (by name)]";
    }
}
