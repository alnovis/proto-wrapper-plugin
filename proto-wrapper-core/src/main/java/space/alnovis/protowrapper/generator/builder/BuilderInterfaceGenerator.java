package space.alnovis.protowrapper.generator.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.MapInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedOneof;
import space.alnovis.protowrapper.generator.GenerationContext;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
import space.alnovis.protowrapper.generator.TypeUtils;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;

import static space.alnovis.protowrapper.generator.TypeUtils.*;

/**
 * Generates Builder interface for message types.
 *
 * <p>This class is responsible for generating the nested Builder interface
 * that defines setter methods for all fields in a message.</p>
 */
public final class BuilderInterfaceGenerator {

    private final GeneratorConfig config;

    public BuilderInterfaceGenerator(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Generate the Builder interface for a top-level message.
     *
     * @param message The merged message
     * @param resolver Type resolver for field types
     * @param ctx Generation context
     * @return TypeSpec for the Builder interface
     */
    public TypeSpec generate(MergedMessage message, TypeResolver resolver, GenerationContext ctx) {
        return generate(message, resolver, ctx, false);
    }

    /**
     * Generate the Builder interface for a nested message.
     *
     * @param message The nested merged message
     * @param resolver Type resolver for field types
     * @param ctx Generation context
     * @return TypeSpec for the Builder interface
     */
    public TypeSpec generateForNested(MergedMessage message, TypeResolver resolver, GenerationContext ctx) {
        return generate(message, resolver, ctx, true);
    }

    private TypeSpec generate(MergedMessage message, TypeResolver resolver,
                               GenerationContext ctx, boolean isNested) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        if (!isNested) {
            builder.addJavadoc("Builder for creating and modifying $L instances.\n", message.getInterfaceName());
        }

        // Add setter methods for each field
        for (MergedField field : message.getFieldsSorted()) {
            addFieldMethods(builder, field, message, resolver, ctx, isNested);
        }

        // Add oneof clear methods (only for top-level messages)
        if (!isNested) {
            for (MergedOneof oneof : message.getOneofGroups()) {
                addOneofMethods(builder, oneof);
            }
        }

        // Add build method
        ClassName returnType = isNested
                ? ClassName.get("", message.getInterfaceName())
                : ClassName.get(config.getApiPackage(), message.getInterfaceName());

        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(returnType);

        if (!isNested) {
            buildMethod.addJavadoc("Build the $L instance.\n", message.getInterfaceName())
                    .addJavadoc("@return New immutable instance\n");
        }

        builder.addMethod(buildMethod.build());

        return builder.build();
    }

    private void addFieldMethods(TypeSpec.Builder builder, MergedField field,
                                  MergedMessage message, TypeResolver resolver,
                                  GenerationContext ctx, boolean isNested) {
        // Handle map fields specially (only for top-level messages)
        if (!isNested && field.isMap() && field.getMapInfo() != null) {
            addMapMethods(builder, field, resolver);
            return;
        }

        // Handle repeated fields with type conflicts (add, addAll, set, clear)
        // Exception: REPEATED_SINGLE conflicts are handled specially below
        if (field.isRepeated() && field.hasTypeConflict()
                && field.getConflictType() != MergedField.ConflictType.REPEATED_SINGLE) {
            addRepeatedConflictBuilderMethods(builder, field, resolver);
            return;
        }

        // Handle INT_ENUM conflicts with overloaded setters (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
            if (ctx.getSchema()
                    .getConflictEnum(message.getName(), field.getName())
                    .map(enumInfo -> { addIntEnumMethods(builder, field, enumInfo, resolver); return true; })
                    .orElse(false)) {
                return;
            }
        }

        // Handle WIDENING conflicts with setter using wider type (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
            addWideningMethods(builder, field, resolver);
            return;
        }

        // Handle FLOAT_DOUBLE conflicts with setter using double type (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.FLOAT_DOUBLE) {
            addFloatDoubleMethods(builder, field, resolver);
            return;
        }

        // Handle SIGNED_UNSIGNED conflicts with setter using long type (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.SIGNED_UNSIGNED) {
            addSignedUnsignedMethods(builder, field, resolver);
            return;
        }

        // Handle STRING_BYTES conflicts with dual setters (String and byte[]) (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
            addStringBytesMethods(builder, field, resolver);
            return;
        }

        // Handle ENUM_ENUM conflicts with int setter (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.ENUM_ENUM) {
            addEnumEnumMethods(builder, field, resolver);
            return;
        }

        // Handle REPEATED_SINGLE conflicts with List<T> as unified type
        if (field.getConflictType() == MergedField.ConflictType.REPEATED_SINGLE) {
            addRepeatedSingleMethods(builder, field, resolver);
            return;
        }

        // Skip builder methods for fields with other non-convertible type conflicts
        if (field.shouldSkipBuilderSetter()) {
            builder.addJavadoc("\n<p><b>Note:</b> {@code $L} setter not available due to type conflict ($L).</p>\n",
                    field.getJavaName(), field.getConflictType());
            return;
        }

        TypeName fieldType = resolver.parseFieldType(field, message);

        if (field.isRepeated()) {
            addRepeatedFieldMethods(builder, field, fieldType, resolver);
        } else {
            addScalarFieldMethods(builder, field, fieldType, resolver);
        }
    }

    private void addRepeatedFieldMethods(TypeSpec.Builder builder, MergedField field,
                                          TypeName fieldType, TypeResolver resolver) {
        TypeName singleElementType = extractListElementType(fieldType);
        ClassName builderType = ClassName.get("", "Builder");
        String capName = resolver.capitalize(field.getJavaName());

        // add single element
        builder.addMethod(MethodSpec.methodBuilder("add" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(singleElementType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add a single $L element.\n", field.getJavaName())
                .build());

        // addAll
        builder.addMethod(MethodSpec.methodBuilder("addAll" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(fieldType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add all $L elements.\n", field.getJavaName())
                .build());

        // set (replace all)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(fieldType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Replace all $L elements.\n", field.getJavaName())
                .build());

        // clear
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear all $L elements.\n", field.getJavaName())
                .build());
    }

    private void addScalarFieldMethods(TypeSpec.Builder builder, MergedField field,
                                        TypeName fieldType, TypeResolver resolver) {
        ClassName builderType = ClassName.get("", "Builder");
        String capName = resolver.capitalize(field.getJavaName());

        // Regular setter
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(fieldType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value.\n", field.getJavaName())
                .build());

        // Clear method for optional fields
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(builderType)
                    .addJavadoc("Clear $L value.\n", field.getJavaName())
                    .build());
        }
    }

    /**
     * Add builder methods for map fields.
     * Generates: put, putAll, remove, clear, getXxxMap
     */
    private void addMapMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName keyType = parseMapKeyType(mapInfo);
        // Use resolved type if there's a map value conflict
        TypeName valueType;
        if (field.hasMapValueConflict() && field.getResolvedMapValueType() != null) {
            valueType = parseSimpleType(field.getResolvedMapValueType());
        } else {
            valueType = parseMapValueType(mapInfo);
        }
        TypeName boxedKeyType = keyType.isPrimitive() ? keyType.box() : keyType;
        TypeName boxedValueType = valueType.isPrimitive() ? valueType.box() : valueType;
        TypeName mapType = ParameterizedTypeName.get(
                ClassName.get(java.util.Map.class), boxedKeyType, boxedValueType);
        ClassName builderType = ClassName.get("", "Builder");

        // putXxx(key, value)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapPutMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .addParameter(valueType, "value")
                .returns(builderType)
                .addJavadoc("Put a single entry into the $L map.\n", field.getJavaName())
                .addJavadoc("@param key the key\n")
                .addJavadoc("@param value the value\n")
                .addJavadoc("@return this builder\n")
                .build());

        // putAllXxx(map)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapPutAllMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(mapType, "values")
                .returns(builderType)
                .addJavadoc("Put all entries from the map into $L.\n", field.getJavaName())
                .addJavadoc("@param values the entries to add\n")
                .addJavadoc("@return this builder\n")
                .build());

        // removeXxx(key)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapRemoveMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .returns(builderType)
                .addJavadoc("Remove an entry from the $L map.\n", field.getJavaName())
                .addJavadoc("@param key the key to remove\n")
                .addJavadoc("@return this builder\n")
                .build());

        // clearXxx()
        builder.addMethod(MethodSpec.methodBuilder(field.getMapClearMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear all entries from the $L map.\n", field.getJavaName())
                .addJavadoc("@return this builder\n")
                .build());

        // getXxxMap() - for inspection
        builder.addMethod(MethodSpec.methodBuilder(field.getMapGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(mapType)
                .addJavadoc("Get the current $L map (for inspection during building).\n", field.getJavaName())
                .addJavadoc("@return unmodifiable view of the current map\n")
                .build());
    }

    /**
     * Add overloaded builder methods for INT_ENUM conflict field.
     * Generates both int and enum setters.
     */
    private void addIntEnumMethods(TypeSpec.Builder builder, MergedField field,
                                    ConflictEnumInfo enumInfo, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
        ClassName builderType = ClassName.get("", "Builder");

        // int setter
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value using int.\n", field.getJavaName())
                .addJavadoc("@param $L The numeric value\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // enum setter
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(enumType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value using unified enum.\n", field.getJavaName())
                .addJavadoc("@param $L The enum value\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // clear method
        builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear $L value.\n", field.getJavaName())
                .build());
    }

    /**
     * Add builder methods for WIDENING conflict field.
     * Uses the wider type (e.g., long for int/long, double for int/double).
     */
    private void addWideningMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Get the wider type from the field (already resolved in VersionMerger)
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // setter with wider type
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(widerType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [WIDENING]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For versions with narrower type, value is validated at runtime.</p>\n")
                .addJavadoc("@param $L The value to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .addJavadoc("@throws IllegalArgumentException if value exceeds target type range\n")
                .build());

        // clear method
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(builderType)
                    .addJavadoc("Clear $L value.\n", field.getJavaName())
                    .build());
        }
    }

    /**
     * Add builder methods for FLOAT_DOUBLE conflict field.
     * Uses double as the unified type.
     */
    private void addFloatDoubleMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // setter with double type
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.DOUBLE, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [FLOAT_DOUBLE]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For float versions, value is validated to be within float range.</p>\n")
                .addJavadoc("@param $L The value to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .addJavadoc("@throws IllegalArgumentException if value exceeds float range\n")
                .build());

        // clear method
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(builderType)
                    .addJavadoc("Clear $L value.\n", field.getJavaName())
                    .build());
        }
    }

    /**
     * Add builder methods for SIGNED_UNSIGNED conflict field.
     * Uses long as the unified type to safely handle unsigned 32-bit values.
     */
    private void addSignedUnsignedMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // setter with long type
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.LONG, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [SIGNED_UNSIGNED]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For signed 32-bit versions, value is validated to be within int range.</p>\n")
                .addJavadoc("<p>For unsigned 32-bit versions, value is validated to be in range [0, 4294967295].</p>\n")
                .addJavadoc("@param $L The value to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .addJavadoc("@throws IllegalArgumentException if value is out of range for target version\n")
                .build());

        // clear method
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(builderType)
                    .addJavadoc("Clear $L value.\n", field.getJavaName())
                    .build());
        }
    }

    /**
     * Add builder methods for STRING_BYTES conflict field.
     * Generates both String and byte[] setters for unified access.
     */
    private void addStringBytesMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // String setter
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ClassName.get(String.class), field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value as String.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [STRING_BYTES]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For bytes versions, converts using UTF-8 encoding.</p>\n")
                .addJavadoc("@param $L The String value to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // byte[] setter
        builder.addMethod(MethodSpec.methodBuilder("set" + capName + "Bytes")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value as raw bytes.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [STRING_BYTES]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For String versions, converts using UTF-8 encoding.</p>\n")
                .addJavadoc("@param $L The byte array value to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // clear method
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(builderType)
                    .addJavadoc("Clear $L value.\n", field.getJavaName())
                    .build());
        }
    }

    /**
     * Add builder methods for ENUM_ENUM conflict field.
     * Uses int as the unified type since all enum types share the same wire format.
     */
    private void addEnumEnumMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // int setter
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value using int.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [ENUM_ENUM]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>All enum types use the same wire format (varint).</p>\n")
                .addJavadoc("@param $L The numeric value\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // clear method
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear $L value.\n", field.getJavaName())
                .build());
    }

    /**
     * Add builder methods for REPEATED_SINGLE conflict field.
     * Uses List as the unified type regardless of version cardinality.
     */
    private void addRepeatedSingleMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Determine element type from resolved type
        String resolvedType = field.getJavaType();
        TypeName elementType;
        TypeName listType;

        if (resolvedType != null && resolvedType.startsWith("java.util.List<")) {
            String elementTypeStr = resolvedType.substring(
                    "java.util.List<".length(),
                    resolvedType.length() - 1);
            elementType = resolveTypeName(elementTypeStr);
            listType = ParameterizedTypeName.get(ClassName.get(java.util.List.class), elementType);
        } else {
            // Fallback to Object
            elementType = ClassName.get(Object.class);
            listType = ParameterizedTypeName.get(ClassName.get(java.util.List.class), elementType);
        }

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue().isRepeated() ? "repeated" : "singular"))
                .collect(Collectors.joining(", "));

        // set (replace all)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L values (replaces all existing values).\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [REPEATED_SINGLE]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For singular versions, list must contain exactly one element.</p>\n")
                .addJavadoc("@param $L The list of values to set\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .addJavadoc("@throws IllegalArgumentException if list is empty or has multiple elements for singular version\n")
                .build());

        // add (single element)
        builder.addMethod(MethodSpec.methodBuilder("add" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(elementType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add a single $L element.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [REPEATED_SINGLE]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For singular versions, this replaces the existing value.</p>\n")
                .addJavadoc("@param $L The value to add\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // addAll (list of elements)
        builder.addMethod(MethodSpec.methodBuilder("addAll" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add all $L elements.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [REPEATED_SINGLE]:</b> $L</p>\n", versionsStr)
                .addJavadoc("<p>For singular versions, list must contain exactly one element.</p>\n")
                .addJavadoc("@param $L The list of values to add\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .addJavadoc("@throws IllegalArgumentException if list has multiple elements for singular version\n")
                .build());

        // clear
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear $L values.\n", field.getJavaName())
                .build());
    }

    /**
     * Add builder methods for repeated fields with type conflicts.
     * Generates add, addAll, set (replace all), and clear methods.
     *
     * <p>Supported conflict types: WIDENING, FLOAT_DOUBLE, SIGNED_UNSIGNED, INT_ENUM, STRING_BYTES</p>
     */
    private void addRepeatedConflictBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                    TypeResolver resolver) {
        MergedField.ConflictType conflictType = field.getConflictType();
        String capName = resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Determine the unified element type based on conflict type
        TypeName elementType = TypeUtils.getRepeatedConflictElementType(conflictType);
        TypeName listType = ParameterizedTypeName.get(ClassName.get(java.util.List.class), elementType);

        // Build version info for javadoc
        String versionsStr = field.getVersionFields().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                .collect(Collectors.joining(", "));

        // add single element
        builder.addMethod(MethodSpec.methodBuilder("add" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(elementType.isPrimitive() ? elementType : elementType.box(), field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add a single $L element.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [$L]:</b> $L</p>\n", conflictType, versionsStr)
                .addJavadoc(getConflictValidationNote(conflictType))
                .build());

        // addAll
        builder.addMethod(MethodSpec.methodBuilder("addAll" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Add all $L elements.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [$L]:</b> $L</p>\n", conflictType, versionsStr)
                .build());

        // set (replace all)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Replace all $L elements.\n", field.getJavaName())
                .addJavadoc("<p><b>Type conflict [$L]:</b> $L</p>\n", conflictType, versionsStr)
                .build());

        // clear
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear all $L elements.\n", field.getJavaName())
                .build());
    }

    /**
     * Get validation note for conflict type javadoc.
     */
    private String getConflictValidationNote(MergedField.ConflictType conflictType) {
        return switch (conflictType) {
            case WIDENING -> "<p>For versions with narrower type, value is validated at runtime.</p>\n" +
                    "@throws IllegalArgumentException if value exceeds target type range\n";
            case FLOAT_DOUBLE -> "<p>For float versions, value is validated to be within float range.</p>\n" +
                    "@throws IllegalArgumentException if value exceeds float range\n";
            case SIGNED_UNSIGNED -> "<p>Values are validated based on version signedness.</p>\n";
            case INT_ENUM -> "<p>For enum versions, value is converted using forNumber().</p>\n";
            case STRING_BYTES -> "<p>For bytes versions, value is converted using UTF-8 encoding.</p>\n";
            default -> "";
        };
    }

    /**
     * Resolve a type name string to a TypeName.
     */
    private TypeName resolveTypeName(String typeName) {
        return switch (typeName) {
            case "int", "Integer", "java.lang.Integer" -> TypeName.INT.box();
            case "long", "Long", "java.lang.Long" -> TypeName.LONG.box();
            case "float", "Float", "java.lang.Float" -> TypeName.FLOAT.box();
            case "double", "Double", "java.lang.Double" -> TypeName.DOUBLE.box();
            case "boolean", "Boolean", "java.lang.Boolean" -> TypeName.BOOLEAN.box();
            case "String", "java.lang.String" -> ClassName.get(String.class);
            default -> ClassName.bestGuess(typeName);
        };
    }

    /**
     * Add a javadoc note about a skipped field in the builder.
     */
    private void addSkippedFieldNote(TypeSpec.Builder builder, MergedField field, String reason) {
        builder.addJavadoc("\n<p><b>Note:</b> {@code $L} setter not available due to $L ($L).</p>\n",
                field.getJavaName(), reason, field.getConflictType());
    }

    /**
     * Add oneof-related methods to Builder interface.
     */
    private void addOneofMethods(TypeSpec.Builder builderBuilder, MergedOneof oneof) {
        ClassName builderType = ClassName.get("", "Builder");

        // Add clearXxx() method to clear entire oneof
        builderBuilder.addMethod(MethodSpec.methodBuilder(oneof.getClearMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear the '$L' oneof - unsets whichever field is currently set.\n", oneof.getProtoName())
                .addJavadoc("@return This builder\n")
                .build());
    }
}
