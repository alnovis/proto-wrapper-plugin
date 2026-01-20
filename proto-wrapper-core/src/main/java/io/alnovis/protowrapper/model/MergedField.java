package io.alnovis.protowrapper.model;

import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;

import java.util.*;

/**
 * Represents a merged field from multiple protocol versions.
 *
 * <p>Contains the unified field definition with type information
 * that works across all versions where this field is present.</p>
 *
 * <p>Can be created via constructor (mutable) or via Builder (immutable):</p>
 * <pre>
 * // Mutable approach (legacy)
 * MergedField field = new MergedField(fieldInfo, "v1");
 * field.addVersion("v2", fieldInfo2);
 *
 * // Immutable approach (preferred)
 * MergedField field = MergedField.builder()
 *     .addVersionField("v1", fieldInfo1)
 *     .addVersionField("v2", fieldInfo2)
 *     .build();
 * </pre>
 */
public class MergedField {

    /**
     * Type of conflict between field types across versions.
     *
     * <p>Each conflict type has an associated {@link Handling} that indicates
     * how the proto-wrapper plugin handles the conflict, and a {@link Severity}
     * for breaking change detection.</p>
     */
    public enum ConflictType {
        /** No type conflict - same type in all versions */
        NONE(Handling.NATIVE, "Types are identical"),

        /** int ↔ enum conflict (plugin uses int with enum helper methods) */
        INT_ENUM(Handling.CONVERTED, "Plugin uses int type with enum helper methods"),

        /** enum ↔ enum conflict (different enum types, plugin uses int) */
        ENUM_ENUM(Handling.CONVERTED, "Plugin uses int type for unified access"),

        /** Integer type widening: int32 → int64 (plugin uses long) */
        WIDENING(Handling.CONVERTED, "Plugin uses wider type (long)"),

        /** Float type widening: float → double (plugin uses double) */
        FLOAT_DOUBLE(Handling.CONVERTED, "Plugin uses double type"),

        /** Signed/unsigned conflict: int32 ↔ uint32 (plugin uses long for safety) */
        SIGNED_UNSIGNED(Handling.CONVERTED, "Plugin uses long type for unsigned safety"),

        /** Repeated ↔ singular conflict (plugin uses List for both) */
        REPEATED_SINGLE(Handling.CONVERTED, "Plugin uses List<T> for unified access"),

        /** Type narrowing: long → int (potential data loss) */
        NARROWING(Handling.WARNING, "Potential data loss on narrowing conversion"),

        /** string ↔ bytes conflict (requires manual conversion) */
        STRING_BYTES(Handling.MANUAL, "Requires getBytes()/new String() conversion"),

        /** Primitive to message conflict (plugin generates dual accessors) */
        PRIMITIVE_MESSAGE(Handling.CONVERTED, "Plugin generates getXxx() and getXxxMessage() accessors"),

        /** Optional ↔ required conflict (plugin handles via hasX() methods) */
        OPTIONAL_REQUIRED(Handling.NATIVE, "Plugin provides hasX() method for checking"),

        /** Other incompatible types (not convertible) */
        INCOMPATIBLE(Handling.INCOMPATIBLE, "Incompatible type change");

        /**
         * How the plugin handles this type of conflict.
         */
        public enum Handling {
            /** No special handling needed - types are compatible */
            NATIVE,
            /** Plugin automatically converts between types */
            CONVERTED,
            /** Conversion possible but requires manual code */
            MANUAL,
            /** Works but may have issues (data loss, etc.) */
            WARNING,
            /** Types are fundamentally incompatible */
            INCOMPATIBLE
        }

        /**
         * Severity level for breaking change detection.
         */
        public enum Severity {
            /** Informational - plugin handles automatically */
            INFO,
            /** Warning - may require attention */
            WARNING,
            /** Error - breaking change that plugin cannot handle */
            ERROR
        }

        private final Handling handling;
        private final String pluginNote;

        ConflictType(Handling handling, String pluginNote) {
            this.handling = handling;
            this.pluginNote = pluginNote;
        }

        /**
         * Returns how the plugin handles this conflict type.
         * @return the handling strategy
         */
        public Handling getHandling() {
            return handling;
        }

        /**
         * Returns a note about how the plugin handles this conflict.
         * @return human-readable description
         */
        public String getPluginNote() {
            return pluginNote;
        }

        /**
         * Returns true if the plugin can automatically handle this conflict.
         * @return true if NATIVE or CONVERTED handling
         */
        public boolean isPluginHandled() {
            return handling == Handling.NATIVE || handling == Handling.CONVERTED;
        }

        /**
         * Returns true if this conflict is a breaking change.
         * Plugin-handled conflicts are not considered breaking.
         * @return true if INCOMPATIBLE handling
         */
        public boolean isBreaking() {
            return handling == Handling.INCOMPATIBLE;
        }

        /**
         * Returns true if this conflict should be shown as a warning.
         * @return true if WARNING or MANUAL handling
         */
        public boolean isWarning() {
            return handling == Handling.WARNING || handling == Handling.MANUAL;
        }

        /**
         * Returns the severity for breaking change detection.
         * @return INFO for plugin-handled, WARNING for manual/lossy, ERROR for incompatible
         */
        public Severity getSeverity() {
            return switch (handling) {
                case NATIVE, CONVERTED -> Severity.INFO;
                case MANUAL, WARNING -> Severity.WARNING;
                case INCOMPATIBLE -> Severity.ERROR;
            };
        }

        /**
         * Check if this conflict type can be safely converted.
         * @return true if conversion is possible without data loss
         */
        public boolean isConvertible() {
            return this == NONE || this == INT_ENUM || this == ENUM_ENUM || this == WIDENING
                    || this == FLOAT_DOUBLE || this == SIGNED_UNSIGNED
                    || this == REPEATED_SINGLE || this == STRING_BYTES
                    || this == OPTIONAL_REQUIRED || this == PRIMITIVE_MESSAGE;
        }

        /**
         * Check if builder setters should be skipped for this conflict type.
         * For the hybrid approach, type conflicts result in skipped builder setters
         * because the unified type cannot be directly passed to version-specific proto builders.
         *
         * <p>Exceptions (builder setters ARE generated):</p>
         * <ul>
         *   <li>{@link #NONE} - no conflict, normal setters</li>
         *   <li>{@link #OPTIONAL_REQUIRED} - same type, only optionality differs</li>
         *   <li>{@link #PRIMITIVE_MESSAGE} - dual setters with runtime validation</li>
         * </ul>
         *
         * @return true if setters should not be generated
         */
        public boolean shouldSkipBuilderSetter() {
            return this != NONE && this != OPTIONAL_REQUIRED && this != PRIMITIVE_MESSAGE;
        }
    }

    private final String name;
    private final String javaName;
    private final int number;
    private final String javaType;
    private final String getterType;
    private final boolean optional;
    private final boolean repeated;
    private final boolean primitive;
    private final boolean message;
    private final boolean isEnum;
    private final boolean isMap;
    private final MapInfo mapInfo; // null if not a map or if map info unavailable
    private final ConflictType mapValueConflictType; // Conflict type for map value types (null if not a map or no conflict)
    private final String resolvedMapValueType; // Resolved unified type for map values with conflicts
    private final Set<String> presentInVersions;
    private final Map<String, FieldInfo> versionFields; // Version -> original field
    private final ConflictType conflictType;
    private final Map<String, String> typesPerVersion; // Version -> javaType
    private final Map<String, String> oneofNamePerVersion; // Version -> oneof name (null if not in oneof)
    private final Map<String, Boolean> optionalityPerVersion; // Version -> isOptional
    private final boolean isInOneof; // true if in oneof in ANY version
    private final WellKnownTypeInfo wellKnownType; // null if not a well-known type
    private final boolean allVersionsSupportHas; // true if ALL versions have has*() method available

    /**
     * Private constructor for Builder.
     */
    private MergedField(Builder builder) {
        FieldInfo firstField = builder.versionFields.values().iterator().next();
        this.name = firstField.getProtoName();
        this.javaName = firstField.getJavaName();
        this.number = firstField.getNumber();
        this.javaType = builder.resolvedJavaType != null ? builder.resolvedJavaType : firstField.getJavaType();
        this.getterType = builder.resolvedGetterType != null ? builder.resolvedGetterType : firstField.getGetterType();
        this.optional = firstField.isOptional();
        this.repeated = firstField.isRepeated();
        this.primitive = firstField.isPrimitive();
        this.message = firstField.isMessage();
        this.isEnum = firstField.isEnum();
        this.isMap = firstField.isMap();
        // Find mapInfo from any version field (first non-null wins)
        this.mapInfo = findMapInfo(builder.versionFields.values());
        this.mapValueConflictType = builder.mapValueConflictType;
        this.resolvedMapValueType = builder.resolvedMapValueType;
        this.presentInVersions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.versionFields.keySet()));
        this.versionFields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.versionFields));
        this.conflictType = builder.conflictType != null ? builder.conflictType : ConflictType.NONE;
        this.typesPerVersion = Collections.unmodifiableMap(new LinkedHashMap<>(builder.typesPerVersion));
        this.oneofNamePerVersion = Collections.unmodifiableMap(new LinkedHashMap<>(builder.oneofNamePerVersion));
        this.optionalityPerVersion = Collections.unmodifiableMap(new LinkedHashMap<>(builder.optionalityPerVersion));
        this.isInOneof = !builder.oneofNamePerVersion.isEmpty();
        this.wellKnownType = firstField.getWellKnownType();
        // Check if ALL versions support has*() method
        this.allVersionsSupportHas = builder.versionFields.values().stream()
                .allMatch(FieldInfo::supportsHasMethod);
    }

    /**
     * Create a new builder for MergedField.
     *
     * @return New Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating immutable MergedField instances.
     */
    public static class Builder {
        private final Map<String, FieldInfo> versionFields = new LinkedHashMap<>();
        private final Map<String, String> typesPerVersion = new LinkedHashMap<>();
        private final Map<String, String> oneofNamePerVersion = new LinkedHashMap<>();
        private final Map<String, Boolean> optionalityPerVersion = new LinkedHashMap<>();
        private String resolvedJavaType;
        private String resolvedGetterType;
        private ConflictType conflictType;
        private ConflictType mapValueConflictType;
        private String resolvedMapValueType;

        /**
         * Add a version field.
         *
         * @param version Version identifier
         * @param field Field info for this version
         * @return This builder
         */
        public Builder addVersionField(String version, FieldInfo field) {
            versionFields.put(version, field);
            typesPerVersion.put(version, field.getJavaType());
            optionalityPerVersion.put(version, field.isOptional());
            if (field.isInOneof()) {
                oneofNamePerVersion.put(version, field.getOneofName());
            }
            return this;
        }

        /**
         * Set the resolved Java type (for type conflict resolution).
         *
         * @param javaType Resolved Java type
         * @return This builder
         */
        public Builder resolvedJavaType(String javaType) {
            this.resolvedJavaType = javaType;
            return this;
        }

        /**
         * Set the resolved getter type (for type conflict resolution).
         *
         * @param getterType Resolved getter type
         * @return This builder
         */
        public Builder resolvedGetterType(String getterType) {
            this.resolvedGetterType = getterType;
            return this;
        }

        /**
         * Set the conflict type for this field.
         *
         * @param conflictType Type of conflict between versions
         * @return This builder
         */
        public Builder conflictType(ConflictType conflictType) {
            this.conflictType = conflictType;
            return this;
        }

        /**
         * Get the current conflict type set in this builder.
         *
         * @return Current conflict type, or NONE if not set
         */
        public ConflictType getConflictType() {
            return conflictType != null ? conflictType : ConflictType.NONE;
        }

        /**
         * Set the map value conflict type (for map fields with type conflicts in values).
         *
         * @param mapValueConflictType Type of conflict in map values between versions
         * @return This builder
         */
        public Builder mapValueConflictType(ConflictType mapValueConflictType) {
            this.mapValueConflictType = mapValueConflictType;
            return this;
        }

        /**
         * Set the resolved map value type (for map value type conflict resolution).
         *
         * @param resolvedMapValueType Resolved unified type for map values
         * @return This builder
         */
        public Builder resolvedMapValueType(String resolvedMapValueType) {
            this.resolvedMapValueType = resolvedMapValueType;
            return this;
        }

        /**
         * Build the immutable MergedField.
         *
         * @return New MergedField instance
         * @throws IllegalStateException if no version fields were added
         */
        public MergedField build() {
            if (versionFields.isEmpty()) {
                throw new IllegalStateException("At least one version field must be added");
            }
            return new MergedField(this);
        }
    }

    /** @return the proto field name */
    public String getName() {
        return name;
    }

    /** @return the Java field name */
    public String getJavaName() {
        return javaName;
    }

    /** @return the proto field number */
    public int getNumber() {
        return number;
    }

    /** @return the Java type string */
    public String getJavaType() {
        return javaType;
    }

    /** @return the getter return type */
    public String getGetterType() {
        return getterType;
    }

    /** @return true if the field is optional */
    public boolean isOptional() {
        return optional;
    }

    /** @return true if the field is repeated */
    public boolean isRepeated() {
        return repeated;
    }

    /** @return true if the field is a primitive type */
    public boolean isPrimitive() {
        return primitive;
    }

    /**
     * Returns true if ALL versions of this field support has*() method.
     *
     * <p>In proto3, singular scalar fields without 'optional' keyword do not have
     * has*() methods. For mixed schemas or pure proto3 singular scalars, this returns false.</p>
     *
     * @return true if has*() is available in all versions
     */
    public boolean allVersionsSupportHas() {
        return allVersionsSupportHas;
    }

    /**
     * Returns true if has*() method should be generated in the unified API.
     *
     * <p>This is the canonical method to determine has*() method generation. It replaces
     * the incorrect pattern {@code field.isOptional() && !field.isRepeated()} which fails for:</p>
     * <ul>
     *   <li>Proto2 required fields - isOptional() returns false, but has*() IS needed</li>
     *   <li>Proto3 implicit scalars - isOptional() returns true, but has*() is NOT needed</li>
     * </ul>
     *
     * <p>The correct logic is based on {@link FieldInfo#supportsHasMethod()} aggregated
     * across all versions via {@link #allVersionsSupportHas()}.</p>
     *
     * <h3>Contract Matrix Rules:</h3>
     * <ul>
     *   <li>Repeated/Map fields: NEVER have has*()</li>
     *   <li>Message fields: ALWAYS have has*()</li>
     *   <li>Oneof fields: ALWAYS have has*()</li>
     *   <li>Proto2 all singular: ALWAYS have has*()</li>
     *   <li>Proto3 explicit optional: HAVE has*()</li>
     *   <li>Proto3 implicit scalar: NO has*()</li>
     * </ul>
     *
     * @return true if has*() method should be generated
     * @see FieldInfo#supportsHasMethod()
     * @see #allVersionsSupportHas()
     */
    public boolean shouldGenerateHasMethod() {
        // Repeated and map fields never have has*() method
        if (repeated || isMap) {
            return false;
        }
        // Delegate to the aggregated version support check
        return allVersionsSupportHas;
    }

    /** @return true if the field is a message type */
    public boolean isMessage() {
        return message;
    }

    /** @return true if the field is an enum type */
    public boolean isEnum() {
        return isEnum;
    }

    /** @return true if the field is a map type */
    public boolean isMap() {
        return isMap;
    }

    /** @return the map info, or null if not a map */
    public MapInfo getMapInfo() {
        return mapInfo;
    }

    /**
     * Get the well-known type info for this field.
     *
     * @return WellKnownTypeInfo if field is a well-known type, null otherwise
     * @since 1.3.0
     */
    public WellKnownTypeInfo getWellKnownType() {
        return wellKnownType;
    }

    /**
     * Check if this field is a Google Well-Known Type.
     *
     * @return true if field type is Timestamp, Duration, StringValue, etc.
     * @since 1.3.0
     */
    public boolean isWellKnownType() {
        return wellKnownType != null;
    }

    /**
     * Get the conflict type for map value types.
     * @return The type of conflict in map values, or null if not a map or no conflict
     */
    public ConflictType getMapValueConflictType() {
        return mapValueConflictType;
    }

    /**
     * Check if this map field has a type conflict in its values.
     * @return true if map value types differ across versions
     */
    public boolean hasMapValueConflict() {
        return mapValueConflictType != null && mapValueConflictType != ConflictType.NONE;
    }

    /**
     * Get the resolved unified type for map values (when there's a conflict).
     * @return Resolved map value type, or null if no conflict
     */
    public String getResolvedMapValueType() {
        return resolvedMapValueType;
    }

    /** @return the set of versions where this field is present */
    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    /** @return the map of version to FieldInfo */
    public Map<String, FieldInfo> getVersionFields() {
        return Collections.unmodifiableMap(versionFields);
    }

    /**
     * Get the FieldInfo for a specific version.
     * @param version Version identifier
     * @return Optional containing the FieldInfo for that version, or empty if not present
     */
    public Optional<FieldInfo> getFieldForVersion(String version) {
        return Optional.ofNullable(versionFields.get(version));
    }

    /**
     * Get the conflict type for this field.
     * @return The type of conflict, or NONE if no conflict
     */
    public ConflictType getConflictType() {
        return conflictType;
    }

    /**
     * Check if this field has a type conflict between versions.
     * @return true if field types differ across versions
     */
    public boolean hasTypeConflict() {
        return conflictType != ConflictType.NONE;
    }

    /**
     * Check if builder setters should be skipped for this field.
     * @return true if the conflict type requires skipping builder setters
     */
    public boolean shouldSkipBuilderSetter() {
        return conflictType.shouldSkipBuilderSetter();
    }

    /**
     * Get the Java type for a specific version.
     * @param version Version identifier
     * @return Java type for that version, or null if not present
     */
    public String getTypeForVersion(String version) {
        return typesPerVersion.get(version);
    }

    /**
     * Get all unique types across all versions.
     * @return Set of unique Java type names
     */
    public Set<String> getAllTypes() {
        return new LinkedHashSet<>(typesPerVersion.values());
    }

    /**
     * Get types per version map.
     * @return Unmodifiable map of version to Java type
     */
    public Map<String, String> getTypesPerVersion() {
        return Collections.unmodifiableMap(typesPerVersion);
    }

    /**
     * Check if this field is part of a oneof in any version.
     * @return true if field is in oneof in at least one version
     */
    public boolean isInOneof() {
        return isInOneof;
    }

    /**
     * Get the oneof name for a specific version.
     * @param version Version identifier
     * @return Oneof name for that version, or null if not in oneof
     */
    public String getOneofNameForVersion(String version) {
        return oneofNamePerVersion.get(version);
    }

    /**
     * Get oneof names per version map.
     * @return Unmodifiable map of version to oneof name (only versions where field is in oneof)
     */
    public Map<String, String> getOneofNamePerVersion() {
        return Collections.unmodifiableMap(oneofNamePerVersion);
    }

    /**
     * Check if this field has inconsistent oneof membership across versions.
     * (e.g., in oneof in v1 but not in v2, or in different oneofs)
     * @return true if oneof membership differs across versions
     */
    public boolean hasOneofMismatch() {
        if (oneofNamePerVersion.isEmpty()) {
            return false; // Not in oneof in any version
        }
        // Check if all versions have the same oneof name, or some are missing
        Set<String> oneofNames = new HashSet<>(oneofNamePerVersion.values());
        return oneofNames.size() > 1 || oneofNamePerVersion.size() != presentInVersions.size();
    }

    /**
     * Get optionality status for a specific version.
     * @param version Version identifier
     * @return true if the field is optional in that version, false if required
     */
    public boolean isOptionalInVersion(String version) {
        Boolean value = optionalityPerVersion.get(version);
        return value != null && value;
    }

    /**
     * Get optionality per version map.
     * @return Unmodifiable map of version to optionality status
     */
    public Map<String, Boolean> getOptionalityPerVersion() {
        return Collections.unmodifiableMap(optionalityPerVersion);
    }

    /**
     * Check if this field has an optional/required conflict across versions.
     * @return true if field is optional in some versions and required in others
     */
    public boolean hasOptionalRequiredMismatch() {
        if (optionalityPerVersion.isEmpty() || optionalityPerVersion.size() <= 1) {
            return false;
        }
        boolean hasOptional = optionalityPerVersion.values().stream().anyMatch(Boolean::booleanValue);
        boolean hasRequired = optionalityPerVersion.values().stream().anyMatch(v -> !v);
        return hasOptional && hasRequired;
    }

    /**
     * Check if this field should be treated as optional in the unified API.
     * Returns true if the field is optional in any version OR has an optional/required mismatch.
     * This is used for determining whether to generate has-methods and nullable return types.
     * @return true if field should be treated as optional in unified API
     */
    public boolean isEffectivelyOptional() {
        return optionalityPerVersion.values().stream().anyMatch(Boolean::booleanValue);
    }

    /**
     * Get the full nested type path from the proto type name.
     * E.g., for type ".example.proto.v1.Order.ShippingInfo" with package "example.proto.v1"
     * returns "Order.ShippingInfo".
     *
     * @param protoPackage The proto package to strip (e.g., "example.proto.v1")
     * @return Full nested type path or simple name if not nested
     */
    public String getNestedTypePath(String protoPackage) {
        // Get the original field info to access the full type name
        if (versionFields.isEmpty()) {
            return javaType;
        }
        FieldInfo firstField = versionFields.values().iterator().next();
        return firstField.extractNestedTypePath(protoPackage);
    }

    /**
     * Get the raw proto type name from the first version's field info.
     * @return Raw proto type name (e.g., ".example.proto.v1.Order.ShippingInfo")
     */
    public String getRawTypeName() {
        if (versionFields.isEmpty()) {
            return null;
        }
        FieldInfo firstField = versionFields.values().iterator().next();
        return firstField.getTypeName();
    }

    /** @return the getter method name */
    public String getGetterName() {
        String prefix = javaType.equals("boolean") || javaType.equals("Boolean") ? "is" : "get";
        return prefix + capitalize(javaName);
    }

    /** @return the extract method name */
    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }

    /** @return the extract-has method name */
    public String getExtractHasMethodName() {
        return "extractHas" + capitalize(javaName);
    }

    /**
     * Get extract method name for enum value (INT_ENUM conflicts).
     *
     * @return the extract enum method name
     */
    public String getExtractEnumMethodName() {
        return "extract" + capitalize(javaName) + "Enum";
    }

    /**
     * Get extract method name for bytes value (STRING_BYTES conflicts).
     *
     * @return the extract bytes method name
     */
    public String getExtractBytesMethodName() {
        return "extract" + capitalize(javaName) + "Bytes";
    }

    /**
     * Get extract method name for message value (PRIMITIVE_MESSAGE conflicts).
     *
     * @return the extract message method name
     */
    public String getExtractMessageMethodName() {
        return "extract" + capitalize(javaName) + "Message";
    }

    // ==================== Map Field Method Names ====================

    /**
     * Getter name for map field (e.g., "getItemCountsMap").
     * @return the map getter method name
     */
    public String getMapGetterName() {
        return "get" + capitalize(javaName) + "Map";
    }

    /**
     * Count getter name for map field (e.g., "getItemCountsCount").
     * @return the map count method name
     */
    public String getMapCountMethodName() {
        return "get" + capitalize(javaName) + "Count";
    }

    /**
     * Contains method name for map field (e.g., "containsItemCounts").
     * @return the map contains method name
     */
    public String getMapContainsMethodName() {
        return "contains" + capitalize(javaName);
    }

    /**
     * GetOrDefault method name for map field (e.g., "getItemCountsOrDefault").
     * @return the map getOrDefault method name
     */
    public String getMapGetOrDefaultMethodName() {
        return "get" + capitalize(javaName) + "OrDefault";
    }

    /**
     * GetOrThrow method name for map field (e.g., "getItemCountsOrThrow").
     * @return the map getOrThrow method name
     */
    public String getMapGetOrThrowMethodName() {
        return "get" + capitalize(javaName) + "OrThrow";
    }

    /**
     * Extract method name for map field (e.g., "extractItemCountsMap").
     * @return the map extract method name
     */
    public String getMapExtractMethodName() {
        return "extract" + capitalize(javaName) + "Map";
    }

    /**
     * Put method name for map builder (e.g., "putItemCounts").
     * @return the map put method name
     */
    public String getMapPutMethodName() {
        return "put" + capitalize(javaName);
    }

    /**
     * PutAll method name for map builder (e.g., "putAllItemCounts").
     * @return the map putAll method name
     */
    public String getMapPutAllMethodName() {
        return "putAll" + capitalize(javaName);
    }

    /**
     * Remove method name for map builder (e.g., "removeItemCounts").
     * @return the map remove method name
     */
    public String getMapRemoveMethodName() {
        return "remove" + capitalize(javaName);
    }

    /**
     * Clear method name for map builder (e.g., "clearItemCounts").
     * @return the map clear method name
     */
    public String getMapClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * DoSet method name for map builder (e.g., "doPutItemCounts").
     * @return the map doPut method name
     */
    public String getMapDoPutMethodName() {
        return "doPut" + capitalize(javaName);
    }

    /**
     * DoPutAll method name for map builder (e.g., "doPutAllItemCounts").
     * @return the map doPutAll method name
     */
    public String getMapDoPutAllMethodName() {
        return "doPutAll" + capitalize(javaName);
    }

    /**
     * DoRemove method name for map builder (e.g., "doRemoveItemCounts").
     * @return the map doRemove method name
     */
    public String getMapDoRemoveMethodName() {
        return "doRemove" + capitalize(javaName);
    }

    /**
     * DoClear method name for map builder (e.g., "doClearItemCounts").
     * @return the map doClear method name
     */
    public String getMapDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }

    /**
     * Do-get method name for map builder (e.g., "doGetItemCountsMap").
     * @return the map doGet method name
     */
    public String getMapDoGetMethodName() {
        return "doGet" + capitalize(javaName) + "Map";
    }

    // ==================== Builder Method Names ====================

    /**
     * Get doSet method name for builder.
     * @return the doSet method name
     */
    public String getDoSetMethodName() {
        return "doSet" + capitalize(javaName);
    }

    /**
     * Get doClear method name for builder.
     * @return the doClear method name
     */
    public String getDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }

    /**
     * Get doSet method name for message value (PRIMITIVE_MESSAGE conflicts).
     * @return the doSetMessage method name
     */
    public String getDoSetMessageMethodName() {
        return "doSet" + capitalize(javaName) + "Message";
    }

    /**
     * Get supportsPrimitive method name for builder (PRIMITIVE_MESSAGE conflicts).
     * @return the supportsPrimitive method name
     */
    public String getSupportsPrimitiveMethodName() {
        return "supportsPrimitive" + capitalize(javaName);
    }

    /**
     * Get supportsMessage method name for builder (PRIMITIVE_MESSAGE conflicts).
     * @return the supportsMessage method name
     */
    public String getSupportsMessageMethodName() {
        return "supportsMessage" + capitalize(javaName);
    }

    /**
     * Check if this version of the field uses primitive type (including String/bytes).
     * Used for PRIMITIVE_MESSAGE conflicts.
     *
     * @param version Version identifier
     * @return true if the field is primitive-like in this version
     */
    public boolean isPrimitiveInVersion(String version) {
        FieldInfo field = versionFields.get(version);
        if (field == null) return false;
        return field.isPrimitive() || isStringOrBytesType(field);
    }

    /**
     * Check if this version of the field uses message type.
     * Used for PRIMITIVE_MESSAGE conflicts.
     *
     * @param version Version identifier
     * @return true if the field is a message type in this version
     */
    public boolean isMessageInVersion(String version) {
        FieldInfo field = versionFields.get(version);
        if (field == null) return false;
        return field.isMessage() && !field.isPrimitive() && !isStringOrBytesType(field);
    }

    /**
     * Check if the field is String or bytes type.
     */
    private boolean isStringOrBytesType(FieldInfo field) {
        String type = field.getJavaType();
        return "String".equals(type) || "byte[]".equals(type) || "ByteString".equals(type);
    }

    /**
     * Get doAdd method name for repeated field builder.
     * @return the doAdd method name
     */
    public String getDoAddMethodName() {
        return "doAdd" + capitalize(javaName);
    }

    /**
     * Get doAddAll method name for repeated field builder.
     * @return the doAddAll method name
     */
    public String getDoAddAllMethodName() {
        return "doAddAll" + capitalize(javaName);
    }

    /**
     * Get setter method name for builder interface.
     * @return the setter method name
     */
    public String getSetterMethodName() {
        return "set" + capitalize(javaName);
    }

    /**
     * Get clear method name for builder interface.
     * @return the clear method name
     */
    public String getClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * Get add method name for repeated field builder interface.
     * @return the add method name
     */
    public String getAddMethodName() {
        return "add" + capitalize(javaName);
    }

    /**
     * Get addAll method name for repeated field builder interface.
     * @return the addAll method name
     */
    public String getAddAllMethodName() {
        return "addAll" + capitalize(javaName);
    }

    /**
     * Get has method name for optional fields.
     * @return the has method name
     */
    public String getHasMethodName() {
        return "has" + capitalize(javaName);
    }

    /**
     * Get supports method name for version-specific fields.
     * @return the supports method name
     */
    public String getSupportsMethodName() {
        return "supports" + capitalize(javaName);
    }

    /**
     * Get the capitalized java name (useful for building method names).
     * @return the capitalized name
     */
    public String getCapitalizedName() {
        return capitalize(javaName);
    }

    /**
     * Whether this field needs has-check pattern in getter.
     *
     * <p>Returns true if the getter should use the pattern
     * {@code extractHas*(proto) ? extract*(proto) : null} to return null when unset.</p>
     *
     * <h3>Oneof fields</h3>
     * <p>Oneof fields ALWAYS use has-check pattern. When a oneof field is not active
     * (another field in the oneof is set or none is set), the getter must return null.
     * This is consistent with proto semantics where only one field can be set at a time.</p>
     *
     * <h3>Message fields</h3>
     * <p>Message fields return null when unset (if has*() is available).
     * Without this check, proto returns a default instance instead of null,
     * which is inconsistent with {@code has*()} returning false.</p>
     *
     * <h3>Primitive fields</h3>
     * <p>Primitive optional fields (Integer, Boolean, etc.) return null when unset,
     * but only if has*() is available. For proto3 singular scalars without 'optional'
     * keyword, has*() is not available, so getter returns value directly.</p>
     *
     * <h3>Enum fields</h3>
     * <p>Enum fields do not use has-check pattern. They return the enum value directly,
     * with unset fields returning the first enum value (proto3 semantics).</p>
     *
     * @return true if getter should use has-check pattern
     */
    public boolean needsHasCheck() {
        // Oneof fields should ALWAYS return null when not active
        if (isInOneof) {
            return true;
        }
        // Message fields should return null when unset (consistent with has*() returning false)
        if (message) {
            return optional && allVersionsSupportHas;
        }
        // Primitive optional fields need has-check only when has*() is available
        return optional && primitive && allVersionsSupportHas;
    }

    /**
     * Whether this field exists in all versions.
     * @param allVersions all available versions
     * @return true if field exists in all versions
     */
    public boolean isUniversal(Set<String> allVersions) {
        return presentInVersions.containsAll(allVersions);
    }

    /**
     * Find MapInfo from any version field (first non-null wins).
     * This handles cases where map detection worked for some versions but not others.
     */
    private static MapInfo findMapInfo(java.util.Collection<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            MapInfo info = field.getMapInfo();
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        String conflictInfo = conflictType != ConflictType.NONE
                ? ", conflict=" + conflictType
                : "";
        return String.format("MergedField[%s:%s #%d, versions=%s%s]",
            name, javaType, number, presentInVersions, conflictInfo);
    }
}
