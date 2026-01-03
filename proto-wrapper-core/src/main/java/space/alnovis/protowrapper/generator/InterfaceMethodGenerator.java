package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MapInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static space.alnovis.protowrapper.generator.GeneratorUtils.*;
import static space.alnovis.protowrapper.generator.TypeUtils.*;

/**
 * Generates interface methods for wrapper interfaces.
 *
 * <p>This class is responsible for generating:</p>
 * <ul>
 *   <li>Getter methods ({@code getXxx()})</li>
 *   <li>Has-methods ({@code hasXxx()})</li>
 *   <li>Supports methods ({@code supportsXxx()})</li>
 *   <li>Conflict-specific getters (enum, bytes, message variants)</li>
 *   <li>Map field methods</li>
 * </ul>
 *
 * <p>Extracted from {@link InterfaceGenerator} to improve maintainability
 * and separation of concerns.</p>
 *
 * @since 1.2.0
 * @see InterfaceGenerator
 * @see InterfaceUtilityGenerator
 */
public final class InterfaceMethodGenerator {

    private final GeneratorConfig config;

    public InterfaceMethodGenerator(GeneratorConfig config) {
        this.config = config;
    }

    // ==================== Standard Getters ====================

    /**
     * Generate getter method for a field.
     *
     * @param field Field to generate getter for
     * @param message Parent message (for version info)
     * @param resolver Type resolver
     * @return Generated getter method
     */
    public MethodSpec generateGetter(MergedField field, MergedMessage message, TypeResolver resolver) {
        TypeName returnType = resolver.parseFieldType(field, message);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.getGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(returnType);

        MergedField.ConflictType conflictType = field.getConflictType();
        if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
            addConflictJavaDoc(builder, field, conflictType, resolver);
        } else if (!field.isUniversal(message.getPresentInVersions())) {
            builder.addJavadoc("@return $L value, or null if not present in this version\n",
                    field.getJavaName());
            builder.addJavadoc("@apiNote Present in versions: $L\n", field.getPresentInVersions());
        } else {
            builder.addJavadoc("@return $L value\n", field.getJavaName());
        }

        return builder.build();
    }

    private void addConflictJavaDoc(MethodSpec.Builder builder, MergedField field,
                                     MergedField.ConflictType conflictType, TypeResolver resolver) {
        builder.addJavadoc("Get $L value.\n", field.getJavaName());
        builder.addJavadoc("<p><b>Type conflict [$L]:</b> ", conflictType.name());

        String typeInfo = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));
        builder.addJavadoc(typeInfo + "</p>\n");

        switch (conflictType) {
            case INT_ENUM -> builder.addJavadoc("<p>Use {@code get$LEnum()} for type-safe enum access.</p>\n",
                    resolver.capitalize(field.getJavaName()));
            case WIDENING -> builder.addJavadoc("<p>Value is automatically widened to the larger type.</p>\n");
            case NARROWING -> builder.addJavadoc("<p>Returns default value (0) for versions with wider type.</p>\n");
            case STRING_BYTES -> builder.addJavadoc("<p>For bytes versions, converts using UTF-8. Use {@code get$LBytes()} for raw bytes.</p>\n",
                    resolver.capitalize(field.getJavaName()));
            case PRIMITIVE_MESSAGE -> builder.addJavadoc("<p>Returns null/default for versions with message type.</p>\n");
            default -> {}
        }
        builder.addJavadoc("@return $L value\n", field.getJavaName());
    }

    // ==================== Has Methods ====================

    /**
     * Generate has-method for optional field.
     *
     * @param field Optional field
     * @param resolver Type resolver
     * @return Generated has-method
     */
    public MethodSpec generateHasMethod(MergedField field, TypeResolver resolver) {
        return MethodSpec.methodBuilder("has" + resolver.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if $L is present.\n", field.getJavaName())
                .addJavadoc("@return true if the field has a value\n")
                .build();
    }

    // ==================== Supports Methods ====================

    /**
     * Generate supportsXxx() default method for version-specific or conflicting fields.
     *
     * @param field Field to check support for
     * @param message Parent message
     * @param resolver Type resolver
     * @return Generated supports method
     */
    public MethodSpec generateSupportsMethod(MergedField field, MergedMessage message, TypeResolver resolver) {
        String methodName = "supports" + resolver.capitalize(field.getJavaName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN);

        MergedField.ConflictType conflictType = field.getConflictType();
        Set<String> presentVersions = field.getPresentInVersions();

        if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
            addConflictSupportsJavaDoc(builder, field, conflictType, presentVersions);
        } else {
            addVersionSpecificSupportsJavaDoc(builder, field, presentVersions);
        }

        return builder.build();
    }

    private void addConflictSupportsJavaDoc(MethodSpec.Builder builder, MergedField field,
                                             MergedField.ConflictType conflictType, Set<String> presentVersions) {
        builder.addJavadoc("Check if $L is fully supported in the current version.\n", field.getJavaName());
        builder.addJavadoc("<p>This field has a type conflict [$L] across versions.</p>\n", conflictType.name());

        switch (conflictType) {
            case INT_ENUM, ENUM_ENUM, WIDENING -> {
                builder.addJavadoc("<p>The conflict is automatically handled; this method returns true for versions: $L</p>\n",
                        presentVersions);
                builder.addJavadoc("@return true if this version has the field\n");
                builder.addStatement("return $L", buildVersionCheck(presentVersions));
            }
            case PRIMITIVE_MESSAGE -> {
                Set<String> primitiveVersions = field.getVersionFields().entrySet().stream()
                        .filter(entry -> isPrimitiveOrPrimitiveLike(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                if (primitiveVersions.isEmpty()) {
                    builder.addJavadoc("<p>This field is a message type in all versions.</p>\n");
                    builder.addJavadoc("@return false (primitive getter not available)\n");
                    builder.addStatement("return false");
                } else {
                    builder.addJavadoc("<p>Returns true only for versions with primitive type: $L</p>\n", primitiveVersions);
                    builder.addJavadoc("@return true if this version has primitive type for this field\n");
                    builder.addStatement("return $L", buildVersionCheck(primitiveVersions));
                }
            }
            default -> {
                builder.addJavadoc("<p>Returns false for versions where the field type is incompatible.</p>\n");
                builder.addJavadoc("@return true if this version has compatible type for this field\n");
                builder.addStatement("return $L", buildVersionCheck(presentVersions));
            }
        }
    }

    private void addVersionSpecificSupportsJavaDoc(MethodSpec.Builder builder, MergedField field,
                                                    Set<String> presentVersions) {
        builder.addJavadoc("Check if $L is available in the current version.\n", field.getJavaName());
        builder.addJavadoc("<p>This field is only present in versions: $L</p>\n", presentVersions);
        builder.addJavadoc("@return true if this version supports this field\n");
        builder.addStatement("return $L", buildVersionCheck(presentVersions));
    }

    // ==================== Conflict-Specific Getters ====================

    /**
     * Generate enum getter for INT_ENUM conflict field.
     *
     * @param field Conflict field
     * @param enumInfo Conflict enum info
     * @param resolver Type resolver
     * @return Generated enum getter method
     */
    public MethodSpec generateEnumGetter(MergedField field, ConflictEnumInfo enumInfo, TypeResolver resolver) {
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Enum";

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(enumType)
                .addJavadoc("Get $L as unified enum.\n", field.getJavaName())
                .addJavadoc("<p>This method provides type-safe enum access for a field that has\n")
                .addJavadoc("different types across protocol versions (int in some, enum in others).</p>\n")
                .addJavadoc("@return Unified enum value, or null if the value is not recognized\n")
                .build();
    }

    /**
     * Generate bytes getter for STRING_BYTES conflict field.
     *
     * @param field Conflict field
     * @param resolver Type resolver
     * @return Generated bytes getter method
     */
    public MethodSpec generateBytesGetter(MergedField field, TypeResolver resolver) {
        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Bytes";

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Get $L as raw bytes.\n", field.getJavaName())
                .addJavadoc("<p>This method provides byte array access for a field that has\n")
                .addJavadoc("different types across protocol versions (String in some, bytes in others).</p>\n")
                .addJavadoc("<p>For String versions, converts using UTF-8 encoding.</p>\n")
                .addJavadoc("@return Byte array value\n")
                .build();
    }

    /**
     * Generate message getter for PRIMITIVE_MESSAGE conflict field.
     *
     * @param field Conflict field
     * @param message Parent message
     * @param resolver Type resolver
     * @return Generated message getter method, or null if no message type found
     */
    public MethodSpec generateMessageGetter(MergedField field, MergedMessage message, TypeResolver resolver) {
        Map<String, FieldInfo> messageFields = field.getVersionFields().entrySet().stream()
                .filter(entry -> !entry.getValue().isPrimitive() && entry.getValue().getTypeName() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, java.util.LinkedHashMap::new));

        if (messageFields.isEmpty()) {
            return null;
        }

        String messageTypeName = messageFields.values().iterator().next().getJavaType();
        Set<String> messageVersions = messageFields.keySet();

        String simpleTypeName = messageTypeName.contains(".")
                ? messageTypeName.substring(messageTypeName.lastIndexOf('.') + 1)
                : messageTypeName;

        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Message";
        String versionsStr = String.join(", ", messageVersions);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.bestGuess(simpleTypeName))
                .addJavadoc("Get $L as message type.\n", field.getJavaName())
                .addJavadoc("<p>This method provides access to the message type for a field that has\n")
                .addJavadoc("different types across protocol versions (primitive in some, message in others).</p>\n")
                .addJavadoc("<p>Available in versions: [$L]</p>\n", versionsStr)
                .addJavadoc("<p>Returns null for primitive versions.</p>\n")
                .addJavadoc("@return Message wrapper, or null if this version has primitive type\n")
                .build();
    }

    /**
     * Generate supportsXxxMessage() method for PRIMITIVE_MESSAGE conflict field.
     *
     * @param field Conflict field
     * @param resolver Type resolver
     * @return Generated supports message method
     */
    public MethodSpec generateSupportsMessageMethod(MergedField field, TypeResolver resolver) {
        String methodName = "supports" + resolver.capitalize(field.getJavaName()) + "Message";

        Set<String> messageVersions = field.getVersionFields().entrySet().stream()
                .filter(entry -> !entry.getValue().isPrimitive() && entry.getValue().getTypeName() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        String versionsStr = String.join(", ", messageVersions);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if $L message type is available in the current version.\n", field.getJavaName())
                .addJavadoc("<p>This field has a type conflict [PRIMITIVE_MESSAGE] across versions.</p>\n")
                .addJavadoc("<p>Returns true only for versions with message type: [$L]</p>\n", versionsStr)
                .addJavadoc("@return true if this version has message type for this field\n")
                .addStatement("return $L", buildVersionCheck(messageVersions))
                .build();
    }

    // ==================== Map Field Methods ====================

    /**
     * Generate interface methods for a map field.
     *
     * @param interfaceBuilder Builder to add methods to
     * @param field Map field
     * @param resolver Type resolver
     */
    public void generateMapFieldMethods(TypeSpec.Builder interfaceBuilder, MergedField field, TypeResolver resolver) {
        MapInfo mapInfo = field.getMapInfo();

        TypeName keyType = parseMapKeyType(mapInfo);
        // Use resolved type if there's a map value conflict, otherwise check for WKT
        TypeName valueType;
        if (field.hasMapValueConflict() && field.getResolvedMapValueType() != null) {
            valueType = parseSimpleType(field.getResolvedMapValueType());
        } else {
            // Check for WKT map values and convert to Java types if enabled
            valueType = parseMapValueTypeWithWkt(mapInfo, config.isConvertWellKnownTypes());
        }
        TypeName boxedKeyType = keyType.isPrimitive() ? keyType.box() : keyType;
        TypeName boxedValueType = valueType.isPrimitive() ? valueType.box() : valueType;
        TypeName mapType = ParameterizedTypeName.get(
                ClassName.get(java.util.Map.class), boxedKeyType, boxedValueType);

        // 1. getXxxMap()
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(mapType)
                .addJavadoc("Get all entries in the $L map.\n", field.getJavaName())
                .addJavadoc("@return unmodifiable map of $L to $L\n",
                        mapInfo.getKeyJavaType(), mapInfo.getValueJavaType())
                .build());

        // 2. getXxxCount()
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapCountMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the number of entries in the $L map.\n", field.getJavaName())
                .addJavadoc("@return number of entries\n")
                .build());

        // 3. containsXxx(key)
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapContainsMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if the $L map contains the specified key.\n", field.getJavaName())
                .addJavadoc("@param key the key to check\n")
                .addJavadoc("@return true if the key exists\n")
                .build());

        // 4. getXxxOrDefault(key, defaultValue)
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapGetOrDefaultMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .addParameter(valueType, "defaultValue")
                .returns(valueType)
                .addJavadoc("Get the value for the specified key, or a default value if not present.\n")
                .addJavadoc("@param key the key to look up\n")
                .addJavadoc("@param defaultValue the value to return if key is not present\n")
                .addJavadoc("@return the value for the key, or defaultValue if not present\n")
                .build());

        // 5. getXxxOrThrow(key)
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapGetOrThrowMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .returns(valueType)
                .addJavadoc("Get the value for the specified key, throwing if not present.\n")
                .addJavadoc("@param key the key to look up\n")
                .addJavadoc("@return the value for the key\n")
                .addJavadoc("@throws IllegalArgumentException if key is not present\n")
                .build());
    }

    // ==================== Well-Known Type Raw Proto Accessors ====================

    /**
     * Check if raw proto accessor should be generated for a field.
     *
     * @param field Field to check
     * @return true if raw proto accessor should be generated
     * @since 1.3.0
     */
    public boolean shouldGenerateRawProtoAccessor(MergedField field) {
        return config.isGenerateRawProtoAccessors()
                && config.isConvertWellKnownTypes()
                && field.isWellKnownType()
                && !field.isMap();
    }

    /**
     * Generate raw proto accessor method for well-known type field.
     *
     * <p>When {@code generateRawProtoAccessors} is enabled, this generates
     * an additional accessor that returns the original proto type instead
     * of the converted Java type.</p>
     *
     * <p>Example:</p>
     * <ul>
     *   <li>{@code getCreatedAt()} returns {@code Instant}</li>
     *   <li>{@code getCreatedAtProto()} returns {@code Timestamp}</li>
     * </ul>
     *
     * @param field Well-known type field
     * @param resolver Type resolver
     * @return Generated raw proto accessor method
     * @since 1.3.0
     */
    public MethodSpec generateRawProtoAccessor(MergedField field, TypeResolver resolver) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Proto";

        // Determine proto type name from WKT info
        String protoTypeName = wkt.getProtoTypeShort(); // e.g., "google.protobuf.Timestamp"
        TypeName protoType = ClassName.bestGuess(protoTypeName);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(protoType)
                .addJavadoc("Get $L as raw protobuf type.\n", field.getJavaName())
                .addJavadoc("<p>Returns the original protobuf {@code $L} without conversion.</p>\n",
                        wkt.getProtoTypeShort())
                .addJavadoc("<p>Use {@code get$L()} for the converted {@code $L} type.</p>\n",
                        resolver.capitalize(field.getJavaName()), wkt.getJavaTypeSimpleName())
                .addJavadoc("@return Raw protobuf value, or null if not present\n");

        return builder.build();
    }

    /**
     * Generate raw proto accessor for repeated well-known type field.
     *
     * @param field Repeated well-known type field
     * @param resolver Type resolver
     * @return Generated raw proto accessor method for repeated field
     * @since 1.3.0
     */
    public MethodSpec generateRepeatedRawProtoAccessor(MergedField field, TypeResolver resolver) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "ProtoList";

        // Determine proto type name from WKT info
        String protoTypeName = wkt.getProtoTypeShort();
        TypeName protoType = ClassName.bestGuess(protoTypeName);
        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), protoType);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listType)
                .addJavadoc("Get $L list as raw protobuf types.\n", field.getJavaName())
                .addJavadoc("<p>Returns the original protobuf {@code $L} list without conversion.</p>\n",
                        wkt.getProtoTypeShort())
                .addJavadoc("<p>Use {@code get$L()} for the converted {@code List<$L>} type.</p>\n",
                        resolver.capitalize(field.getJavaName()), wkt.getJavaTypeSimpleName())
                .addJavadoc("@return List of raw protobuf values\n")
                .build();
    }
}
