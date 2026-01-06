package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mapping between Java type names and JavaPoet TypeName objects.
 *
 * <p>Replaces repetitive switch-case statements for type resolution
 * with a centralized, efficient lookup mechanism.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * TypeName type = JavaTypeMapping.resolve("int");  // Returns TypeName.INT
 * TypeName boxed = JavaTypeMapping.resolve("Integer");  // Returns ClassName.get(Integer.class)
 * String defaultVal = JavaTypeMapping.getDefaultValue("long");  // Returns "0L"
 * String boxedName = JavaTypeMapping.toBoxedName("int");  // Returns "Integer"
 * </pre>
 */
public enum JavaTypeMapping {

    /** Primitive int type. */
    INT("int", TypeName.INT, "Integer", "0"),
    /** Primitive long type. */
    LONG("long", TypeName.LONG, "Long", "0L"),
    /** Primitive double type. */
    DOUBLE("double", TypeName.DOUBLE, "Double", "0.0"),
    /** Primitive float type. */
    FLOAT("float", TypeName.FLOAT, "Float", "0.0f"),
    /** Primitive boolean type. */
    BOOLEAN("boolean", TypeName.BOOLEAN, "Boolean", "false"),

    /** Boxed Integer type. */
    INTEGER_BOXED("Integer", ClassName.get(Integer.class), "Integer", "null"),
    /** Boxed Long type. */
    LONG_BOXED("Long", ClassName.get(Long.class), "Long", "null"),
    /** Boxed Double type. */
    DOUBLE_BOXED("Double", ClassName.get(Double.class), "Double", "null"),
    /** Boxed Float type. */
    FLOAT_BOXED("Float", ClassName.get(Float.class), "Float", "null"),
    /** Boxed Boolean type. */
    BOOLEAN_BOXED("Boolean", ClassName.get(Boolean.class), "Boolean", "null"),

    /** String type. */
    STRING("String", ClassName.get(String.class), "String", "null"),
    /** Byte array type. */
    BYTE_ARRAY("byte[]", ArrayTypeName.of(TypeName.BYTE), "byte[]", "null");

    private final String name;
    private final TypeName typeName;
    private final String boxedName;
    private final String defaultValue;

    private static final Map<String, JavaTypeMapping> BY_NAME = new HashMap<>();

    static {
        for (JavaTypeMapping mapping : values()) {
            BY_NAME.put(mapping.name, mapping);
        }
    }

    JavaTypeMapping(String name, TypeName typeName, String boxedName, String defaultValue) {
        this.name = name;
        this.typeName = typeName;
        this.boxedName = boxedName;
        this.defaultValue = defaultValue;
    }

    /**
     * Get the Java type name string.
     * @return Type name (e.g., "int", "String")
     */
    public String getName() {
        return name;
    }

    /**
     * Get the JavaPoet TypeName.
     * @return TypeName for code generation
     */
    public TypeName getTypeName() {
        return typeName;
    }

    /**
     * Get the boxed type name.
     * @return Boxed type name (e.g., "Integer" for "int")
     */
    public String getBoxedName() {
        return boxedName;
    }

    /**
     * Get the default value for this type.
     * @return Default value string for code generation
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Resolve a type name to JavaPoet TypeName.
     *
     * @param typeName Type name string
     * @return TypeName or null if not a primitive/wrapper type
     */
    public static TypeName resolve(String typeName) {
        JavaTypeMapping mapping = BY_NAME.get(typeName);
        return mapping != null ? mapping.typeName : null;
    }

    /**
     * Check if a type name is a known primitive or wrapper type.
     *
     * @param typeName Type name string
     * @return true if this is a primitive or wrapper type
     */
    public static boolean isKnownType(String typeName) {
        return BY_NAME.containsKey(typeName);
    }

    /**
     * Get the default value for a type.
     *
     * @param typeName Type name string
     * @return Default value string, or "null" for unknown types
     */
    public static String getDefaultValue(String typeName) {
        JavaTypeMapping mapping = BY_NAME.get(typeName);
        return mapping != null ? mapping.defaultValue : "null";
    }

    /**
     * Convert a primitive type name to its boxed equivalent.
     *
     * @param typeName Primitive type name
     * @return Boxed type name, or the original if not a primitive
     */
    public static String toBoxedName(String typeName) {
        JavaTypeMapping mapping = BY_NAME.get(typeName);
        return mapping != null ? mapping.boxedName : typeName;
    }

    private static final Set<String> PRIMITIVES = Set.of("int", "long", "double", "float", "boolean");

    /**
     * Check if the given type is a primitive (not boxed).
     *
     * @param typeName Type name string
     * @return true if primitive type
     */
    public static boolean isPrimitive(String typeName) {
        return PRIMITIVES.contains(typeName);
    }
}
