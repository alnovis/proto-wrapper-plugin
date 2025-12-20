package space.alnovis.protowrapper.model;

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
    private final Set<String> presentInVersions;
    private final Map<String, FieldInfo> versionFields; // Version -> original field

    /**
     * Create a new MergedField from a FieldInfo.
     * Use {@link #addVersion(String, FieldInfo)} to add more versions.
     *
     * @param field Field info from first version
     * @param version Version identifier
     * @deprecated Use {@link #builder()} for immutable construction
     */
    @Deprecated
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
        this.presentInVersions = new LinkedHashSet<>();
        this.presentInVersions.add(version);
        this.versionFields = new LinkedHashMap<>();
        this.versionFields.put(version, field);
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
        this.presentInVersions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.versionFields.keySet()));
        this.versionFields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.versionFields));
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
     * @deprecated Use {@link #builder()} for immutable construction
     */
    @Deprecated
    public void addVersion(String version, FieldInfo field) {
        presentInVersions.add(version);
        versionFields.put(version, field);
    }

    /**
     * Builder for creating immutable MergedField instances.
     */
    public static class Builder {
        private final Map<String, FieldInfo> versionFields = new LinkedHashMap<>();
        private String resolvedJavaType;
        private String resolvedGetterType;

        /**
         * Add a version field.
         *
         * @param version Version identifier
         * @param field Field info for this version
         * @return This builder
         */
        public Builder addVersionField(String version, FieldInfo field) {
            versionFields.put(version, field);
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

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    public Map<String, FieldInfo> getVersionFields() {
        return Collections.unmodifiableMap(versionFields);
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        return String.format("MergedField[%s:%s #%d, versions=%s]",
            name, javaType, number, presentInVersions);
    }
}
