package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.Arrays;
import java.util.Optional;

/**
 * Resolves Java types from proto field information.
 *
 * <p>This class consolidates type resolution logic that was previously
 * duplicated across InterfaceGenerator, AbstractClassGenerator, and ImplClassGenerator.</p>
 *
 * <p>Main responsibilities:</p>
 * <ul>
 *   <li>Parse field types to JavaPoet TypeName</li>
 *   <li>Resolve nested type paths</li>
 *   <li>Handle cross-message type references</li>
 *   <li>Build nested class names</li>
 * </ul>
 */
public class TypeResolver {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new TypeResolver with the specified configuration and schema.
     *
     * @param config the generator configuration
     * @param schema the merged schema
     */
    public TypeResolver(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    /**
     * Parse field type to JavaPoet TypeName.
     *
     * @param field   The merged field
     * @param context The message context for resolving nested types
     * @return TypeName for the field
     */
    public TypeName parseFieldType(MergedField field, MergedMessage context) {
        // Check for well-known type conversion first
        if (config.isConvertWellKnownTypes() && field.isWellKnownType()) {
            WellKnownTypeInfo wkt = field.getWellKnownType();
            TypeName javaType = wkt.getJavaTypeName();
            if (field.isRepeated()) {
                return ParameterizedTypeName.get(ClassName.get(java.util.List.class), javaType);
            }
            return javaType;
        }

        String getterType = field.getGetterType();

        // Handle primitives and common types
        TypeName primitiveType = parsePrimitiveOrWrapperType(getterType);
        if (primitiveType != null) {
            return primitiveType;
        }

        // Handle List<T>
        if (getterType.startsWith("java.util.List<")) {
            String innerTypeName = getterType.substring("java.util.List<".length(), getterType.length() - 1);

            // Check if inner type is primitive/wrapper
            TypeName innerType = parsePrimitiveOrWrapperType(innerTypeName);
            if (innerType != null) {
                return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
            }

            // For message/enum types, use the full nested type path
            String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
            String fullTypePath = field.getNestedTypePath(protoPackage);
            innerType = resolveTypePath(fullTypePath, context);
            return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
        }

        // For message and enum types, use full nested type path
        String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);
        return resolveTypePath(fullTypePath, context);
    }

    /**
     * Parse primitive and wrapper types.
     *
     * @param typeName Type name string
     * @return TypeName or null if not a primitive/wrapper type
     */
    public TypeName parsePrimitiveOrWrapperType(String typeName) {
        return JavaTypeMapping.resolve(typeName);
    }

    /**
     * Parse a type name string to TypeName.
     *
     * @param typeName Type name string
     * @param context  Message context for resolving nested types
     * @return TypeName for the type
     */
    public TypeName parseTypeName(String typeName, MergedMessage context) {
        // Handle primitives and common types
        TypeName primitiveType = parsePrimitiveOrWrapperType(typeName);
        if (primitiveType != null) {
            return primitiveType;
        }

        // Handle List<T>
        if (typeName.startsWith("java.util.List<")) {
            String inner = typeName.substring("java.util.List<".length(), typeName.length() - 1);
            TypeName innerType = parseTypeName(inner, context);
            return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
        }

        // Use resolveTypePath for everything else
        return resolveTypePath(typeName, context);
    }

    /**
     * Resolve a type path that might be a cross-message nested type.
     *
     * @param typePath Type path (e.g., "Order.ShippingInfo" or simple "Money")
     * @param context  Message context for resolving relative types
     * @return ClassName for the resolved type
     */
    public TypeName resolveTypePath(String typePath, MergedMessage context) {
        // If typePath contains dots, it might be a cross-message path
        if (typePath.contains(".")) {
            return resolveNestedTypePath(typePath, context);
        }

        return resolveSimpleTypePath(typePath, context);
    }

    /**
     * Resolve a nested type path like "Order.ShippingInfo".
     */
    private TypeName resolveNestedTypePath(String typePath, MergedMessage context) {
        // Check if this nested path has an equivalent top-level enum
        if (schema != null && schema.hasEquivalentTopLevelEnum(typePath)) {
            String topLevelName = schema.getEquivalentTopLevelEnum(typePath);
            return ClassName.get(config.getApiPackage(), topLevelName);
        }

        String[] parts = typePath.split("\\.");
        String parentMessageName = parts[0];
        MergedMessage topLevel = context.getTopLevelParent();

        if (topLevel.getName().equals(parentMessageName)) {
            // It's within our own message hierarchy
            return buildNestedClassName(typePath);
        }

        // Cross-message reference - validate exists or use fallback
        // Note: All paths return buildNestedClassName, but schema check validates the type exists
        if (schema != null &&
                (schema.findMessageByPath(typePath).isPresent() ||
                 schema.findEnumByPath(typePath).isPresent())) {
            return buildNestedClassName(typePath);
        }

        // Fallback - assume it's valid (for types not in schema or when schema is null)
        return buildNestedClassName(typePath);
    }

    /**
     * Resolve a simple type path (no dots).
     */
    private TypeName resolveSimpleTypePath(String typePath, MergedMessage context) {
        // Check if this enum has an equivalent top-level enum
        String nestedPath = context.getQualifiedInterfaceName() + "." + typePath;
        if (schema != null && schema.hasEquivalentTopLevelEnum(nestedPath)) {
            String topLevelName = schema.getEquivalentTopLevelEnum(nestedPath);
            return ClassName.get(config.getApiPackage(), topLevelName);
        }

        // Check in context hierarchy - chain Optional lookups with or()
        return context.findNestedMessageRecursive(typePath)
                .map(nested -> (TypeName) buildNestedClassName(nested.getQualifiedInterfaceName()))
                // Check nested enums
                .or(() -> context.findNestedEnumRecursive(typePath)
                        .map(ignored -> {
                            String enumPath = context.getTopLevelParent().getName() + "." + typePath;
                            if (schema != null && schema.hasEquivalentTopLevelEnum(enumPath)) {
                                String topLevelName = schema.getEquivalentTopLevelEnum(enumPath);
                                return ClassName.get(config.getApiPackage(), topLevelName);
                            }
                            return buildNestedClassName(enumPath);
                        }))
                // Check sibling types if context is nested
                .or(() -> {
                    if (!context.isNested()) return Optional.empty();
                    MergedMessage topLevel = context.getTopLevelParent();
                    return topLevel.findNestedMessageRecursive(typePath)
                            .map(sibling -> (TypeName) buildNestedClassName(sibling.getQualifiedInterfaceName()));
                })
                .or(() -> {
                    if (!context.isNested()) return Optional.empty();
                    MergedMessage topLevel = context.getTopLevelParent();
                    return topLevel.findNestedEnumRecursive(typePath)
                            .map(ignored -> {
                                String siblingEnumPath = topLevel.getName() + "." + typePath;
                                if (schema != null && schema.hasEquivalentTopLevelEnum(siblingEnumPath)) {
                                    String topLevelName = schema.getEquivalentTopLevelEnum(siblingEnumPath);
                                    return ClassName.get(config.getApiPackage(), topLevelName);
                                }
                                return buildNestedClassName(siblingEnumPath);
                            });
                })
                // Default: top-level type in API package
                .orElse(ClassName.get(config.getApiPackage(), typePath));
    }

    /**
     * Build a ClassName for a nested type path.
     * E.g., "Order.Tax" -> ClassName(apiPackage, "Order").nestedClass("Tax")
     *
     * @param qualifiedPath Path like "Order.Tax" or "Order.Item.Detail"
     * @return ClassName with proper nesting
     */
    public ClassName buildNestedClassName(String qualifiedPath) {
        String[] parts = qualifiedPath.split("\\.");
        return Arrays.stream(parts)
                .skip(1)
                .reduce(
                        ClassName.get(config.getApiPackage(), parts[0]),
                        ClassName::nestedClass,
                        (a, b) -> b  // combiner not used in sequential stream
                );
    }

    /**
     * Build a relative nested class name (without package).
     * Used for references within the same top-level interface.
     *
     * @param qualifiedPath Path like "Order.Tax"
     * @return ClassName with no package
     */
    public ClassName buildRelativeNestedClassName(String qualifiedPath) {
        String[] parts = qualifiedPath.split("\\.");
        return Arrays.stream(parts)
                .skip(1)
                .reduce(
                        ClassName.get("", parts[0]),
                        ClassName::nestedClass,
                        (a, b) -> b  // combiner not used in sequential stream
                );
    }

    /**
     * Extract the proto package from the pattern.
     * The pattern is like "com.example.proto.{version}" - replace {version} with
     * a placeholder version to get the concrete package.
     *
     * @param pattern Proto package pattern
     * @return Extracted proto package with placeholder version
     */
    public String extractProtoPackage(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return "";
        }
        // Replace {version} placeholder with a concrete version for matching
        // The actual version doesn't matter as FieldInfo.extractNestedTypePath
        // handles version-independent matching
        return pattern.replace("{version}", "v1");
    }

    /**
     * Capitalize the first character of a string.
     *
     * @param s String to capitalize
     * @return Capitalized string
     */
    public String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Get the default value for a type.
     *
     * @param typeName Type name
     * @return Default value string for code generation
     */
    public String getDefaultValue(String typeName) {
        return JavaTypeMapping.getDefaultValue(typeName);
    }

    /**
     * Extract simple type name from a potentially qualified name.
     * E.g., {@code "java.util.List<EnumType>" -> "EnumType"}
     *
     * @param typeName Full type name
     * @return Simple type name
     */
    public String extractSimpleTypeName(String typeName) {
        if (typeName == null) {
            return "Object";
        }
        // Handle List<EnumType> -> EnumType
        if (typeName.startsWith("java.util.List<")) {
            typeName = typeName.substring("java.util.List<".length(), typeName.length() - 1);
        }
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    /**
     * Extract version number from version string.
     * E.g., "v1" -> 1
     *
     * @param version Version string
     * @return Version number
     */
    public int extractVersionNumber(String version) {
        String numStr = version.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the generator configuration.
     *
     * @return the generator configuration
     */
    public GeneratorConfig getConfig() {
        return config;
    }

    /**
     * Returns the merged schema.
     *
     * @return the merged schema
     */
    public MergedSchema getSchema() {
        return schema;
    }
}
