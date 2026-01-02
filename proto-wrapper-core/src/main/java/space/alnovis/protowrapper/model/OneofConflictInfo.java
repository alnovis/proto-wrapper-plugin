package space.alnovis.protowrapper.model;

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

    public OneofConflictType getType() {
        return type;
    }

    public String getOneofName() {
        return oneofName;
    }

    public String getMessageName() {
        return messageName;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getAffectedVersions() {
        return affectedVersions;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return (T) details.get(key);
    }

    /**
     * Get a formatted message for logging.
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

    public static class Builder {
        private final OneofConflictType type;
        private String oneofName = "";
        private String messageName = "";
        private String description = "";
        private final Set<String> affectedVersions = new LinkedHashSet<>();
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(OneofConflictType type) {
            this.type = Objects.requireNonNull(type);
            this.description = type.getDescription();
        }

        public Builder oneofName(String oneofName) {
            this.oneofName = oneofName;
            return this;
        }

        public Builder messageName(String messageName) {
            this.messageName = messageName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder affectedVersion(String version) {
            this.affectedVersions.add(version);
            return this;
        }

        public Builder affectedVersions(Collection<String> versions) {
            this.affectedVersions.addAll(versions);
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public OneofConflictInfo build() {
            return new OneofConflictInfo(this);
        }
    }
}
