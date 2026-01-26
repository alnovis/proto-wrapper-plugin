package io.alnovis.protowrapper.runtime;

import java.util.List;
import java.util.Optional;

/**
 * Runtime representation of schema differences between two protocol versions.
 *
 * <p>Provides access to field changes, enum changes, and migration hints
 * without requiring the full diff infrastructure at runtime.</p>
 *
 * <p>This interface is implemented by generated classes for each version pair,
 * allowing runtime introspection of schema evolution.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V203);
 * Optional<VersionSchemaDiff> diff = ctx.getDiffFrom(ProtocolVersions.V202);
 *
 * diff.ifPresent(d -> {
 *     // Find what happened to a field
 *     d.findFieldChange("Tax", "type").ifPresent(fc -> {
 *         System.out.println("Change: " + fc.changeType());
 *         System.out.println("Hint: " + fc.migrationHint());
 *     });
 *
 *     // Get all removed fields
 *     for (FieldChange fc : d.getRemovedFields()) {
 *         System.out.println("Removed: " + fc.messageName() + "." + fc.fieldName());
 *     }
 * });
 * }</pre>
 *
 * @since 2.3.0
 */
public interface VersionSchemaDiff {

    /**
     * Returns the source version identifier.
     *
     * @return source version ID (e.g., "v202")
     */
    String getFromVersion();

    /**
     * Returns the target version identifier.
     *
     * @return target version ID (e.g., "v203")
     */
    String getToVersion();

    /**
     * Returns all field changes between versions.
     *
     * @return unmodifiable list of field changes
     */
    List<FieldChange> getFieldChanges();

    /**
     * Returns all enum changes between versions.
     *
     * @return unmodifiable list of enum changes
     */
    List<EnumChange> getEnumChanges();

    /**
     * Finds a specific field change by message and field name.
     *
     * @param messageName simple name of the message
     * @param fieldName name of the field
     * @return optional containing field change if found
     */
    default Optional<FieldChange> findFieldChange(String messageName, String fieldName) {
        return getFieldChanges().stream()
                .filter(fc -> fc.messageName().equals(messageName) && fc.fieldName().equals(fieldName))
                .findFirst();
    }

    /**
     * Returns all fields that were added in the target version.
     *
     * @return list of added field changes
     */
    default List<FieldChange> getAddedFields() {
        return getFieldChanges().stream()
                .filter(fc -> fc.changeType() == FieldChangeType.ADDED)
                .toList();
    }

    /**
     * Returns all fields that were removed in the target version.
     *
     * @return list of removed field changes
     */
    default List<FieldChange> getRemovedFields() {
        return getFieldChanges().stream()
                .filter(fc -> fc.changeType() == FieldChangeType.REMOVED)
                .toList();
    }

    /**
     * Returns all fields that changed type.
     *
     * @return list of type-changed field changes
     */
    default List<FieldChange> getTypeChangedFields() {
        return getFieldChanges().stream()
                .filter(fc -> fc.changeType() == FieldChangeType.TYPE_CHANGED)
                .toList();
    }

    /**
     * Returns all fields that were renamed.
     *
     * @return list of renamed field changes
     */
    default List<FieldChange> getRenamedFields() {
        return getFieldChanges().stream()
                .filter(fc -> fc.changeType() == FieldChangeType.RENAMED)
                .toList();
    }

    /**
     * Checks if there are any changes between versions.
     *
     * @return true if there are any field or enum changes
     */
    default boolean hasChanges() {
        return !getFieldChanges().isEmpty() || !getEnumChanges().isEmpty();
    }

    // ==================== Change Types ====================

    /**
     * Type of field change.
     */
    enum FieldChangeType {
        /** Field was added in the target version */
        ADDED,
        /** Field was removed in the target version */
        REMOVED,
        /** Field was renamed (different name, same semantics) */
        RENAMED,
        /** Field type changed (e.g., int32 to enum) */
        TYPE_CHANGED,
        /** Field number changed */
        NUMBER_CHANGED,
        /** Field moved to different message */
        MOVED
    }

    /**
     * Type of enum change.
     */
    enum EnumChangeType {
        /** Enum was added in the target version */
        ADDED,
        /** Enum was removed in the target version */
        REMOVED,
        /** Enum values changed */
        VALUES_CHANGED
    }

    // ==================== Change Records ====================

    /**
     * Represents a field change between versions.
     *
     * @param messageName simple name of the containing message
     * @param fieldName field name (in source version for REMOVED/RENAMED, target version otherwise)
     * @param changeType type of change
     * @param oldType previous type (for TYPE_CHANGED)
     * @param newType new type (for TYPE_CHANGED, ADDED)
     * @param oldFieldName previous field name (for RENAMED)
     * @param newFieldName new field name (for RENAMED, MOVED)
     * @param newMessageName new message name (for MOVED)
     * @param migrationHint human-readable hint for migration
     */
    record FieldChange(
            String messageName,
            String fieldName,
            FieldChangeType changeType,
            String oldType,
            String newType,
            String oldFieldName,
            String newFieldName,
            String newMessageName,
            String migrationHint
    ) {
        /**
         * Creates a simple ADDED field change.
         */
        public static FieldChange added(String messageName, String fieldName, String type, String hint) {
            return new FieldChange(messageName, fieldName, FieldChangeType.ADDED,
                    null, type, null, null, null, hint);
        }

        /**
         * Creates a simple REMOVED field change.
         */
        public static FieldChange removed(String messageName, String fieldName, String type, String hint) {
            return new FieldChange(messageName, fieldName, FieldChangeType.REMOVED,
                    type, null, null, null, null, hint);
        }

        /**
         * Creates a TYPE_CHANGED field change.
         */
        public static FieldChange typeChanged(String messageName, String fieldName,
                                               String oldType, String newType, String hint) {
            return new FieldChange(messageName, fieldName, FieldChangeType.TYPE_CHANGED,
                    oldType, newType, null, null, null, hint);
        }

        /**
         * Creates a RENAMED field change.
         */
        public static FieldChange renamed(String messageName, String oldFieldName,
                                          String newFieldName, String hint) {
            return new FieldChange(messageName, newFieldName, FieldChangeType.RENAMED,
                    null, null, oldFieldName, newFieldName, null, hint);
        }

        /**
         * Creates a MOVED field change.
         */
        public static FieldChange moved(String oldMessageName, String fieldName,
                                        String newMessageName, String newFieldName, String hint) {
            return new FieldChange(oldMessageName, fieldName, FieldChangeType.MOVED,
                    null, null, null, newFieldName, newMessageName, hint);
        }

        /**
         * Checks if this change has a migration hint.
         *
         * @return true if migration hint is present
         */
        public boolean hasHint() {
            return migrationHint != null && !migrationHint.isEmpty();
        }
    }

    /**
     * Represents an enum change between versions.
     *
     * @param enumName simple name of the enum
     * @param changeType type of change
     * @param addedValues values added in target version
     * @param removedValues values removed in target version
     * @param migrationHint human-readable hint for migration
     */
    record EnumChange(
            String enumName,
            EnumChangeType changeType,
            List<String> addedValues,
            List<String> removedValues,
            String migrationHint
    ) {
        /**
         * Creates an ADDED enum change.
         */
        public static EnumChange added(String enumName, List<String> values, String hint) {
            return new EnumChange(enumName, EnumChangeType.ADDED, values, List.of(), hint);
        }

        /**
         * Creates a REMOVED enum change.
         */
        public static EnumChange removed(String enumName, List<String> values, String hint) {
            return new EnumChange(enumName, EnumChangeType.REMOVED, List.of(), values, hint);
        }

        /**
         * Creates a VALUES_CHANGED enum change.
         */
        public static EnumChange valuesChanged(String enumName, List<String> added,
                                                List<String> removed, String hint) {
            return new EnumChange(enumName, EnumChangeType.VALUES_CHANGED, added, removed, hint);
        }

        /**
         * Checks if this change has a migration hint.
         *
         * @return true if migration hint is present
         */
        public boolean hasHint() {
            return migrationHint != null && !migrationHint.isEmpty();
        }
    }
}
