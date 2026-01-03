package space.alnovis.protowrapper.model;

import space.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;

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
     */
    public enum ConflictType {
        /** No type conflict - same type in all versions */
        NONE,
        /** int ↔ enum conflict (convertible via getValue/forNumber) */
        INT_ENUM,
        /** enum ↔ enum conflict (different enum types, use int accessor) */
        ENUM_ENUM,
        /** Integer type widening: int32 → int64 (safe conversion to long) */
        WIDENING,
        /** Float type widening: float → double (safe conversion to double) */
        FLOAT_DOUBLE,
        /** Signed/unsigned conflict: int32 ↔ uint32, sint32, etc. (use long for safety) */
        SIGNED_UNSIGNED,
        /** Repeated ↔ singular conflict: repeated T ↔ T (unified as List) */
        REPEATED_SINGLE,
        /** Type narrowing: long → int, double → int (lossy conversion) */
        NARROWING,
        /** string ↔ bytes conflict (convertible via getBytes/new String) */
        STRING_BYTES,
        /** Primitive to message: int → SomeMessage (not convertible) */
        PRIMITIVE_MESSAGE,
        /** Optional ↔ required conflict: field is optional in some versions, required in others */
        OPTIONAL_REQUIRED,
        /** Other incompatible types: string ↔ int, etc. (not convertible) */
        INCOMPATIBLE;

        /**
         * Check if this conflict type can be safely converted.
         * @return true if conversion is possible without data loss
         */
        public boolean isConvertible() {
            return this == NONE || this == INT_ENUM || this == ENUM_ENUM || this == WIDENING
                    || this == FLOAT_DOUBLE || this == SIGNED_UNSIGNED
                    || this == REPEATED_SINGLE || this == STRING_BYTES
                    || this == OPTIONAL_REQUIRED;
        }

        /**
         * Check if builder setters should be skipped for this conflict type.
         * For the hybrid approach, type conflicts result in skipped builder setters
         * because the unified type cannot be directly passed to version-specific proto builders.
         * OPTIONAL_REQUIRED is an exception - the type is the same, only optionality differs.
         * @return true if setters should not be generated
         */
        public boolean shouldSkipBuilderSetter() {
            return this != NONE && this != OPTIONAL_REQUIRED;
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

    /**
     * Create a new MergedField from a FieldInfo.
     * Use {@link #addVersion(String, FieldInfo)} to add more versions.
     *
     * @param field Field info from first version
     * @param version Version identifier
     * @deprecated Use {@link #builder()} for immutable construction. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public MergedField(FieldInfo field, String version) {
        this.name = field.getProtoName();
        this.javaName = field.getJavaName();
        this.number = field.getNumber();
        this.javaType = field.getJavaType();
        this.getterType = field.getGetterType();
        this.optional = field.isOptional();
        this.repeated = field.isRepeated();
        this.primitive = field.isPrimitive();
        this.message = field.isMessage();
        this.isEnum = field.isEnum();
        this.isMap = field.isMap();
        this.mapInfo = field.getMapInfo();
        this.mapValueConflictType = null;
        this.resolvedMapValueType = null;
        this.presentInVersions = new LinkedHashSet<>();
        this.presentInVersions.add(version);
        this.versionFields = new LinkedHashMap<>();
        this.versionFields.put(version, field);
        this.conflictType = ConflictType.NONE;
        this.typesPerVersion = new LinkedHashMap<>();
        this.typesPerVersion.put(version, field.getJavaType());
        this.oneofNamePerVersion = new LinkedHashMap<>();
        if (field.isInOneof()) {
            this.oneofNamePerVersion.put(version, field.getOneofName());
        }
        this.optionalityPerVersion = new LinkedHashMap<>();
        this.optionalityPerVersion.put(version, field.isOptional());
        this.isInOneof = field.isInOneof();
        this.wellKnownType = field.getWellKnownType();
    }

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
     * Add a version to this field.
     *
     * @param version Version identifier
     * @param field Field info for this version
     * @deprecated Use {@link #builder()} for immutable construction. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public void addVersion(String version, FieldInfo field) {
        presentInVersions.add(version);
        versionFields.put(version, field);
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

    public String getName() {
        return name;
    }

    public String getJavaName() {
        return javaName;
    }

    public int getNumber() {
        return number;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getGetterType() {
        return getterType;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isMessage() {
        return message;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isMap() {
        return isMap;
    }

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

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

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

    public String getGetterName() {
        String prefix = javaType.equals("boolean") || javaType.equals("Boolean") ? "is" : "get";
        return prefix + capitalize(javaName);
    }

    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }

    public String getExtractHasMethodName() {
        return "extractHas" + capitalize(javaName);
    }

    /**
     * Get extract method name for enum value (INT_ENUM conflicts).
     */
    public String getExtractEnumMethodName() {
        return "extract" + capitalize(javaName) + "Enum";
    }

    /**
     * Get extract method name for bytes value (STRING_BYTES conflicts).
     */
    public String getExtractBytesMethodName() {
        return "extract" + capitalize(javaName) + "Bytes";
    }

    /**
     * Get extract method name for message value (PRIMITIVE_MESSAGE conflicts).
     */
    public String getExtractMessageMethodName() {
        return "extract" + capitalize(javaName) + "Message";
    }

    // ==================== Map Field Method Names ====================

    /**
     * Getter name for map field (e.g., "getItemCountsMap").
     */
    public String getMapGetterName() {
        return "get" + capitalize(javaName) + "Map";
    }

    /**
     * Count getter name for map field (e.g., "getItemCountsCount").
     */
    public String getMapCountMethodName() {
        return "get" + capitalize(javaName) + "Count";
    }

    /**
     * Contains method name for map field (e.g., "containsItemCounts").
     */
    public String getMapContainsMethodName() {
        return "contains" + capitalize(javaName);
    }

    /**
     * GetOrDefault method name for map field (e.g., "getItemCountsOrDefault").
     */
    public String getMapGetOrDefaultMethodName() {
        return "get" + capitalize(javaName) + "OrDefault";
    }

    /**
     * GetOrThrow method name for map field (e.g., "getItemCountsOrThrow").
     */
    public String getMapGetOrThrowMethodName() {
        return "get" + capitalize(javaName) + "OrThrow";
    }

    /**
     * Extract method name for map field (e.g., "extractItemCountsMap").
     */
    public String getMapExtractMethodName() {
        return "extract" + capitalize(javaName) + "Map";
    }

    /**
     * Put method name for map builder (e.g., "putItemCounts").
     */
    public String getMapPutMethodName() {
        return "put" + capitalize(javaName);
    }

    /**
     * PutAll method name for map builder (e.g., "putAllItemCounts").
     */
    public String getMapPutAllMethodName() {
        return "putAll" + capitalize(javaName);
    }

    /**
     * Remove method name for map builder (e.g., "removeItemCounts").
     */
    public String getMapRemoveMethodName() {
        return "remove" + capitalize(javaName);
    }

    /**
     * Clear method name for map builder (e.g., "clearItemCounts").
     */
    public String getMapClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * DoSet method name for map builder (e.g., "doPutItemCounts").
     */
    public String getMapDoPutMethodName() {
        return "doPut" + capitalize(javaName);
    }

    /**
     * DoPutAll method name for map builder (e.g., "doPutAllItemCounts").
     */
    public String getMapDoPutAllMethodName() {
        return "doPutAll" + capitalize(javaName);
    }

    /**
     * DoRemove method name for map builder (e.g., "doRemoveItemCounts").
     */
    public String getMapDoRemoveMethodName() {
        return "doRemove" + capitalize(javaName);
    }

    /**
     * DoClear method name for map builder (e.g., "doClearItemCounts").
     */
    public String getMapDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }

    /**
     * Do-get method name for map builder (e.g., "doGetItemCountsMap").
     */
    public String getMapDoGetMethodName() {
        return "doGet" + capitalize(javaName) + "Map";
    }

    // ==================== Builder Method Names ====================

    /**
     * Get doSet method name for builder.
     */
    public String getDoSetMethodName() {
        return "doSet" + capitalize(javaName);
    }

    /**
     * Get doClear method name for builder.
     */
    public String getDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }

    /**
     * Get doAdd method name for repeated field builder.
     */
    public String getDoAddMethodName() {
        return "doAdd" + capitalize(javaName);
    }

    /**
     * Get doAddAll method name for repeated field builder.
     */
    public String getDoAddAllMethodName() {
        return "doAddAll" + capitalize(javaName);
    }

    /**
     * Get setter method name for builder interface.
     */
    public String getSetterMethodName() {
        return "set" + capitalize(javaName);
    }

    /**
     * Get clear method name for builder interface.
     */
    public String getClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * Get add method name for repeated field builder interface.
     */
    public String getAddMethodName() {
        return "add" + capitalize(javaName);
    }

    /**
     * Get addAll method name for repeated field builder interface.
     */
    public String getAddAllMethodName() {
        return "addAll" + capitalize(javaName);
    }

    /**
     * Get has method name for optional fields.
     */
    public String getHasMethodName() {
        return "has" + capitalize(javaName);
    }

    /**
     * Get supports method name for version-specific fields.
     */
    public String getSupportsMethodName() {
        return "supports" + capitalize(javaName);
    }

    /**
     * Get the capitalized java name (useful for building method names).
     */
    public String getCapitalizedName() {
        return capitalize(javaName);
    }

    /**
     * Whether this field needs has-check pattern (optional primitive).
     */
    public boolean needsHasCheck() {
        return optional && primitive;
    }

    /**
     * Whether this field exists in all versions.
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
