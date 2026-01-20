package io.alnovis.protowrapper.model;

import java.util.*;

/**
 * Detailed information about a oneof conflict detected during version merging.
 */
public class OneofConflictInfo {

    private final OneofConflictType type;
    private final String oneofName;
    private final String messageName;
    private final String description;
    private final Set<String> affectedVersions;
    private final Map<String, Object> details;

    private OneofConflictInfo(Builder builder) {
        this.type = builder.type;
        this.oneofName = builder.oneofName;
        this.messageName = builder.messageName;
        this.description = builder.description;
        this.affectedVersions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.affectedVersions));
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    }

    /** @return the conflict type */
    public OneofConflictType getType() {
        return type;
    }

    /** @return the oneof name */
    public String getOneofName() {
        return oneofName;
    }

    /** @return the message name containing this oneof */
    public String getMessageName() {
        return messageName;
    }

    /** @return the conflict description */
    public String getDescription() {
        return description;
    }

    /** @return unmodifiable set of affected version identifiers */
    public Set<String> getAffectedVersions() {
        return affectedVersions;
    }

    /** @return unmodifiable map of detail key-value pairs */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Get a detail value by key.
     *
     * @param key the detail key
     * @param <T> the expected value type
     * @return the detail value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return (T) details.get(key);
    }

    /**
     * Get a formatted message for logging.
     *
     * @return formatted log message
     */
    public String toLogMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type.name()).append("] ");
        sb.append(messageName).append(".").append(oneofName);
        sb.append(": ").append(description);
        if (!affectedVersions.isEmpty()) {
            sb.append(" (versions: ").append(String.join(", ", affectedVersions)).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toLogMessage();
    }

    /**
     * Create a new builder for OneofConflictInfo.
     *
     * @param type the conflict type
     * @return new Builder instance
     */
    public static Builder builder(OneofConflictType type) {
        return new Builder(type);
    }

    // ========== Detail keys for specific conflict types ==========

    /** For RENAMED: original oneof name */
    public static final String DETAIL_ORIGINAL_NAME = "originalName";
    /** For RENAMED: new oneof name */
    public static final String DETAIL_NEW_NAME = "newName";
    /** For FIELD_SET_DIFFERENCE: fields only in some versions */
    public static final String DETAIL_VERSION_SPECIFIC_FIELDS = "versionSpecificFields";
    /** For FIELD_MEMBERSHIP_CHANGE: field name that moved */
    public static final String DETAIL_FIELD_NAME = "fieldName";
    /** For FIELD_MEMBERSHIP_CHANGE: versions where field is in oneof */
    public static final String DETAIL_IN_ONEOF_VERSIONS = "inOneofVersions";
    /** For FIELD_MEMBERSHIP_CHANGE: versions where field is regular */
    public static final String DETAIL_REGULAR_VERSIONS = "regularVersions";
    /** For FIELD_NUMBER_CHANGE: old field number */
    public static final String DETAIL_OLD_NUMBER = "oldNumber";
    /** For FIELD_NUMBER_CHANGE: new field number */
    public static final String DETAIL_NEW_NUMBER = "newNumber";
    /** For FIELD_REMOVED: removed field names */
    public static final String DETAIL_REMOVED_FIELDS = "removedFields";
    /** For FIELD_TYPE_CONFLICT: the MergedField.ConflictType */
    public static final String DETAIL_FIELD_CONFLICT_TYPE = "fieldConflictType";

    /**
     * Builder for constructing OneofConflictInfo instances.
     */
    public static class Builder {
        private final OneofConflictType type;
        private String oneofName = "";
        private String messageName = "";
        private String description = "";
        private final Set<String> affectedVersions = new LinkedHashSet<>();
        private final Map<String, Object> details = new LinkedHashMap<>();

        /**
         * Create a new Builder.
         *
         * @param type the conflict type
         */
        private Builder(OneofConflictType type) {
            this.type = Objects.requireNonNull(type);
            this.description = type.getDescription();
        }

        /**
         * Set the oneof name.
         *
         * @param oneofName the oneof name
         * @return this builder for chaining
         */
        public Builder oneofName(String oneofName) {
            this.oneofName = oneofName;
            return this;
        }

        /**
         * Set the message name.
         *
         * @param messageName the message name
         * @return this builder for chaining
         */
        public Builder messageName(String messageName) {
            this.messageName = messageName;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description the conflict description
         * @return this builder for chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Add an affected version.
         *
         * @param version the version identifier
         * @return this builder for chaining
         */
        public Builder affectedVersion(String version) {
            this.affectedVersions.add(version);
            return this;
        }

        /**
         * Add multiple affected versions.
         *
         * @param versions the version identifiers to add
         * @return this builder for chaining
         */
        public Builder affectedVersions(Collection<String> versions) {
            this.affectedVersions.addAll(versions);
            return this;
        }

        /**
         * Add a detail key-value pair.
         *
         * @param key the detail key
         * @param value the detail value
         * @return this builder for chaining
         */
        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        /**
         * Build the OneofConflictInfo.
         *
         * @return the built instance
         */
        public OneofConflictInfo build() {
            return new OneofConflictInfo(this);
        }
    }
}
