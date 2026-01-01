package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MapInfo;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedOneof;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import space.alnovis.protowrapper.generator.builder.BuilderInterfaceGenerator;
import space.alnovis.protowrapper.generator.oneof.OneofGenerator;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;
import static space.alnovis.protowrapper.generator.TypeUtils.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates version-agnostic Java interfaces from merged schema.
 *
 * <p>Example output:</p>
 * <pre>
 * public interface Money {
 *     long getBills();
 *     int getCoins();
 *     long toKopecks();
 * }
 * </pre>
 */
public class InterfaceGenerator extends BaseGenerator<MergedMessage> {

    /**
     * Legacy field - kept for backward compatibility.
     * @deprecated Use {@link GenerationContext} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    private TypeResolver typeResolver;

    public InterfaceGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @param schema The merged schema
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public void setSchema(MergedSchema schema) {
        this.typeResolver = new TypeResolver(config, schema);
    }

    /**
     * Generate interface for a merged message using context.
     *
     * @param message Merged message info
     * @param ctx Generation context
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(message.getInterfaceName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Version-agnostic interface for $L.\n\n", message.getName())
                .addJavadoc("<p>Supported in versions: $L</p>\n", message.getPresentInVersions());

        // Add getter methods for all fields
        for (MergedField field : message.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                generateMapFieldMethods(interfaceBuilder, field, resolver);

                // Add supportsXxx() for version-specific fields
                if (!field.isUniversal(message.getPresentInVersions())) {
                    interfaceBuilder.addMethod(generateSupportsMethod(field, message, resolver));
                }
                continue;
            }

            MethodSpec getter = generateGetter(field, message, resolver);
            interfaceBuilder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                interfaceBuilder.addMethod(generateHasMethod(field, resolver));
            }

            // Add supportsXxx() for version-specific fields or fields with conflicts
            if (!field.isUniversal(message.getPresentInVersions()) ||
                (field.getConflictType() != null && field.getConflictType() != MergedField.ConflictType.NONE)) {
                interfaceBuilder.addMethod(generateSupportsMethod(field, message, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields (scalar only - repeated handled differently)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName())
                        .map(enumInfo -> generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(interfaceBuilder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = generateBytesGetter(field, resolver);
                interfaceBuilder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = generateMessageGetter(field, message, resolver);
                if (messageGetter != null) {
                    interfaceBuilder.addMethod(messageGetter);
                    // Also add supportsXxxMessage() method
                    interfaceBuilder.addMethod(generateSupportsMessageMethod(field, resolver));
                }
            }
        }

        // Add oneof Case enums and discriminator methods
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            // Generate Case enum as nested type
            interfaceBuilder.addType(oneofGenerator.generateCaseEnum(oneof));

            // Add discriminator method getXxxCase()
            interfaceBuilder.addMethod(oneofGenerator.generateInterfaceCaseGetter(oneof, message));

            // Note: hasXxx() methods for oneof fields are generated by normal field processing
            // since oneof fields are regular fields with isInOneof() flag
        }

        // Add nested enums first
        message.getNestedEnums().stream()
                .map(this::generateNestedEnum)
                .forEach(interfaceBuilder::addType);

        // Add nested interfaces for nested messages
        message.getNestedMessages().stream()
                .map(nested -> generateNestedInterface(nested, ctx))
                .forEach(interfaceBuilder::addType);

        // Add common utility methods
        addCommonMethods(interfaceBuilder, message);

        // Add Builder interface if enabled
        if (config.isGenerateBuilders()) {
            interfaceBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a builder initialized with this instance's values.\n")
                    .addJavadoc("@return Builder for creating modified copies\n")
                    .build());

            BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
            TypeSpec builderInterface = builderGen.generate(message, resolver, ctx);
            interfaceBuilder.addType(builderInterface);

            // Add static newBuilder(VersionContext ctx) method
            ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
            ClassName builderType = ClassName.get(config.getApiPackage(), message.getInterfaceName())
                    .nestedClass("Builder");
            String builderMethodName = "new" + message.getName() + "Builder";

            interfaceBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(versionContextType, "ctx")
                    .returns(builderType)
                    .addJavadoc("Create a new builder for $L using the specified version context.\n", message.getName())
                    .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L()}.</p>\n", builderMethodName)
                    .addJavadoc("@param ctx Version context to use for builder creation\n")
                    .addJavadoc("@return Empty builder for $L\n", message.getName())
                    .addStatement("return ctx.$L()", builderMethodName)
                    .build());
        }

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(ctx.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate interface for a merged message.
     * @param message Merged message info
     * @return Generated JavaFile
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public JavaFile generate(MergedMessage message) {
        if (typeResolver == null) {
            throw new IllegalStateException("Schema not set. Call setSchema() first or use generate(message, ctx)");
        }
        GenerationContext ctx = GenerationContext.create(typeResolver.getSchema(), config);
        return generate(message, ctx);
    }

    private MethodSpec generateGetter(MergedField field, MergedMessage message, TypeResolver resolver) {
        TypeName returnType = resolver.parseFieldType(field, message);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.getGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(returnType);

        // Add conflict information to Javadoc if applicable
        MergedField.ConflictType conflictType = field.getConflictType();
        if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
            builder.addJavadoc("Get $L value.\n", field.getJavaName());
            builder.addJavadoc("<p><b>Type conflict [$L]:</b> ", conflictType.name());

            // Add version-specific type info
            String typeInfo = field.getVersionFields().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().getJavaType())
                    .collect(Collectors.joining(", "));
            builder.addJavadoc(typeInfo + "</p>\n");

            // Add behavior note based on conflict type
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
        } else if (!field.isUniversal(message.getPresentInVersions())) {
            builder.addJavadoc("@return $L value, or null if not present in this version\n",
                    field.getJavaName());
            builder.addJavadoc("@apiNote Present in versions: $L\n", field.getPresentInVersions());
        } else {
            builder.addJavadoc("@return $L value\n", field.getJavaName());
        }

        return builder.build();
    }

    private MethodSpec generateHasMethod(MergedField field, TypeResolver resolver) {
        return MethodSpec.methodBuilder("has" + resolver.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if $L is present.\n", field.getJavaName())
                .addJavadoc("@return true if the field has a value\n")
                .build();
    }

    /**
     * Generate supportsXxx() default method for version-specific or conflicting fields.
     */
    private MethodSpec generateSupportsMethod(MergedField field, MergedMessage message, TypeResolver resolver) {
        String methodName = "supports" + resolver.capitalize(field.getJavaName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN);

        MergedField.ConflictType conflictType = field.getConflictType();
        Set<String> presentVersions = field.getPresentInVersions();
        Set<String> allVersions = message.getPresentInVersions();

        // Build version check condition
        if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
            // For conflict fields, explain the conflict and check version
            builder.addJavadoc("Check if $L is fully supported in the current version.\n", field.getJavaName());
            builder.addJavadoc("<p>This field has a type conflict [$L] across versions.</p>\n", conflictType.name());

            switch (conflictType) {
                case INT_ENUM, ENUM_ENUM, WIDENING -> {
                    // These conflicts are resolved - return true for all versions where field exists
                    builder.addJavadoc("<p>The conflict is automatically handled; this method returns true for versions: $L</p>\n",
                            presentVersions);
                    builder.addJavadoc("@return true if this version has the field\n");
                    builder.addStatement("return $L", buildVersionCheck(presentVersions));
                }
                case PRIMITIVE_MESSAGE -> {
                    // For PRIMITIVE_MESSAGE, the getter returns the primitive type
                    // Only return true for versions where the field IS primitive or primitive-like (String, bytes)
                    Set<String> primitiveVersions = field.getVersionFields().entrySet().stream()
                            .filter(entry -> isPrimitiveOrPrimitiveLike(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .collect(java.util.stream.Collectors.toSet());
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
                    // NARROWING, STRING_BYTES, etc. - only some versions work properly
                    builder.addJavadoc("<p>Returns false for versions where the field type is incompatible.</p>\n");
                    builder.addJavadoc("@return true if this version has compatible type for this field\n");
                    builder.addStatement("return $L", buildVersionCheck(presentVersions));
                }
            }
        } else {
            // Version-specific field (not present in all versions)
            builder.addJavadoc("Check if $L is available in the current version.\n", field.getJavaName());
            builder.addJavadoc("<p>This field is only present in versions: $L</p>\n", presentVersions);
            builder.addJavadoc("@return true if this version supports this field\n");
            builder.addStatement("return $L", buildVersionCheck(presentVersions));
        }

        return builder.build();
    }

    /**
     * Build a version check expression for the given set of versions.
     */
    private String buildVersionCheck(Set<String> versions) {
        if (versions.size() == 1) {
            String version = versions.iterator().next();
            // Extract numeric part if version is like "v1", "v202", etc.
            String numericPart = version.replaceAll("[^0-9]", "");
            if (!numericPart.isEmpty()) {
                return "getWrapperVersion() == " + numericPart;
            }
            return "true"; // Fallback
        } else {
            // Multiple versions - build OR condition using stream
            String result = versions.stream()
                    .map(version -> version.replaceAll("[^0-9]", ""))
                    .filter(numericPart -> !numericPart.isEmpty())
                    .map(numericPart -> "getWrapperVersion() == " + numericPart)
                    .collect(Collectors.joining(" || "));
            return result.isEmpty() ? "true" : result;
        }
    }

    /**
     * Generate enum getter for INT_ENUM conflict field.
     */
    private MethodSpec generateEnumGetter(MergedField field, ConflictEnumInfo enumInfo, TypeResolver resolver) {
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
     */
    private MethodSpec generateBytesGetter(MergedField field, TypeResolver resolver) {
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
     * Returns the message type for versions where the field is a message, null for primitive versions.
     */
    private MethodSpec generateMessageGetter(MergedField field, MergedMessage message, TypeResolver resolver) {
        // Find the message type from the version fields using stream
        Map<String, FieldInfo> messageFields = field.getVersionFields().entrySet().stream()
                .filter(entry -> !entry.getValue().isPrimitive() && entry.getValue().getTypeName() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, java.util.LinkedHashMap::new));

        if (messageFields.isEmpty()) {
            return null; // No message type found
        }

        String messageTypeName = messageFields.values().iterator().next().getJavaType();
        Set<String> messageVersions = messageFields.keySet();

        // Determine the return type - it should be the wrapper interface type in the api package
        // Extract simple name from the full type (e.g., "CalibrationInfo" from "space.alnovis...CalibrationInfo")
        String simpleTypeName = messageTypeName.contains(".")
                ? messageTypeName.substring(messageTypeName.lastIndexOf('.') + 1)
                : messageTypeName;

        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Message";
        String versionsStr = String.join(", ", messageVersions);

        // Use simple name for interface (same package)
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
     */
    private MethodSpec generateSupportsMessageMethod(MergedField field, TypeResolver resolver) {
        String methodName = "supports" + resolver.capitalize(field.getJavaName()) + "Message";

        // Find message versions using stream
        Set<String> messageVersions = field.getVersionFields().entrySet().stream()
                .filter(entry -> !entry.getValue().isPrimitive() && entry.getValue().getTypeName() != null)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        String versionsStr = String.join(", ", messageVersions);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if $L message type is available in the current version.\n", field.getJavaName())
                .addJavadoc("<p>This field has a type conflict [PRIMITIVE_MESSAGE] across versions.</p>\n")
                .addJavadoc("<p>Returns true only for versions with message type: [$L]</p>\n", versionsStr)
                .addJavadoc("@return true if this version has message type for this field\n");

        builder.addStatement("return $L", buildVersionCheck(messageVersions));

        return builder.build();
    }

    // ==================== Map Field Methods ====================

    /**
     * Generate interface methods for a map field.
     *
     * @param interfaceBuilder builder to add methods to
     * @param field the map field
     * @param resolver type resolver
     */
    private void generateMapFieldMethods(TypeSpec.Builder interfaceBuilder, MergedField field, TypeResolver resolver) {
        MapInfo mapInfo = field.getMapInfo();
        String capitalizedName = resolver.capitalize(field.getJavaName());

        // Determine types
        TypeName keyType = parseMapKeyType(mapInfo);
        TypeName valueType = parseMapValueType(mapInfo);
        TypeName boxedKeyType = keyType.isPrimitive() ? keyType.box() : keyType;
        TypeName boxedValueType = valueType.isPrimitive() ? valueType.box() : valueType;
        TypeName mapType = ParameterizedTypeName.get(
                ClassName.get(java.util.Map.class), boxedKeyType, boxedValueType);

        // 1. getXxxMap() - returns unmodifiable map
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(mapType)
                .addJavadoc("Get all entries in the $L map.\n", field.getJavaName())
                .addJavadoc("@return unmodifiable map of $L to $L\n",
                        mapInfo.getKeyJavaType(), mapInfo.getValueJavaType())
                .build());

        // 2. getXxxCount() - returns entry count
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapCountMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the number of entries in the $L map.\n", field.getJavaName())
                .addJavadoc("@return number of entries\n")
                .build());

        // 3. containsXxx(key) - check if key exists
        interfaceBuilder.addMethod(MethodSpec.methodBuilder(field.getMapContainsMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if the $L map contains the specified key.\n", field.getJavaName())
                .addJavadoc("@param key the key to check\n")
                .addJavadoc("@return true if the key exists\n")
                .build());

        // 4. getXxxOrDefault(key, defaultValue) - get value or default
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

        // 5. getXxxOrThrow(key) - get value or throw
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

    private TypeSpec generateNestedInterface(MergedMessage nested, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(nested.getInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested interface for $L.\n", nested.getName());

        for (MergedField field : nested.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                generateMapFieldMethods(builder, field, resolver);
                continue;
            }

            // Use shared generateGetter for consistent Javadoc including conflict info
            MethodSpec getter = generateGetter(field, nested, resolver);
            builder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                builder.addMethod(generateHasMethod(field, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName())
                        .map(enumInfo -> generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(builder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = generateBytesGetter(field, resolver);
                builder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = generateMessageGetter(field, nested, resolver);
                if (messageGetter != null) {
                    builder.addMethod(messageGetter);
                    builder.addMethod(generateSupportsMessageMethod(field, resolver));
                }
            }
        }

        nested.getNestedEnums().stream()
                .map(this::generateNestedEnum)
                .forEach(builder::addType);

        nested.getNestedMessages().stream()
                .map(deeplyNested -> generateNestedInterface(deeplyNested, ctx))
                .forEach(builder::addType);

        // Add Builder interface for nested message if enabled
        if (config.isGenerateBuilders()) {
            builder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .build());

            builder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a new empty builder of the same version as this instance.\n")
                    .build());

            BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
            TypeSpec nestedBuilder = builderGen.generateForNested(nested, resolver, ctx);
            builder.addType(nestedBuilder);

            // Add static newBuilder(VersionContext ctx) method for nested interface
            ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
            String builderMethodName = buildNestedBuilderMethodName(nested);
            String qualifiedName = buildNestedQualifiedName(nested);

            builder.addMethod(MethodSpec.methodBuilder("newBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(versionContextType, "ctx")
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a new builder for $L using the specified version context.\n", qualifiedName)
                    .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L()}.</p>\n", builderMethodName)
                    .addJavadoc("@param ctx Version context to use for builder creation\n")
                    .addJavadoc("@return Empty builder for $L\n", qualifiedName)
                    .addStatement("return ctx.$L()", builderMethodName)
                    .build());
        }

        return builder.build();
    }

    /**
     * Build the method name for creating a nested builder via VersionContext.
     * For example, for nested message Order.Item, returns "newOrderItemBuilder".
     */
    private String buildNestedBuilderMethodName(MergedMessage nested) {
        return "new" + String.join("", collectMessageHierarchyNames(nested)) + "Builder";
    }

    /**
     * Build the qualified name for a nested message.
     * For example, for nested message Order.Item, returns "Order.Item".
     */
    private String buildNestedQualifiedName(MergedMessage nested) {
        return String.join(".", collectMessageHierarchyNames(nested));
    }

    /**
     * Collect message names from root to current (e.g., ["Order", "Item"] for Order.Item).
     */
    private List<String> collectMessageHierarchyNames(MergedMessage message) {
        java.util.LinkedList<String> names = new java.util.LinkedList<>();
        for (MergedMessage current = message; current != null; current = current.getParent()) {
            names.addFirst(current.getName());
        }
        return names;
    }

    private TypeSpec generateNestedEnum(MergedEnum enumInfo) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested enum for $L.\n", enumInfo.getName());

        enumBuilder.addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL);

        enumBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value")
                .build());

        enumBuilder.addMethod(MethodSpec.methodBuilder("getValue")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return value")
                .build());

        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProtoValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", enumInfo.getName()))
                .addParameter(TypeName.INT, "value")
                .beginControlFlow("for ($L e : values())", enumInfo.getName())
                .beginControlFlow("if (e.value == value)")
                .addStatement("return e")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return null")
                .build());

        for (MergedEnumValue value : enumInfo.getValues()) {
            enumBuilder.addEnumConstant(value.getName(),
                    TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
        }

        return enumBuilder.build();
    }

    private void addCommonMethods(TypeSpec.Builder builder, MergedMessage message) {
        // Build version examples from actual versions
        String versionExamples = message.getPresentInVersions().stream()
                .map(v -> v.replaceAll("[^0-9]", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));

        builder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the wrapper protocol version this instance was created from.\n")
                .addJavadoc("@return Protocol version (e.g., $L)\n", versionExamples)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Serialize to protobuf bytes.\n")
                .addJavadoc("@return Protobuf-encoded bytes\n")
                .build());

        TypeVariableName typeVar = TypeVariableName.get("T", ClassName.get(config.getApiPackage(), message.getInterfaceName()));
        builder.addMethod(MethodSpec.methodBuilder("asVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Convert to a specific version implementation.\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                .build());

        // Add emptyBuilder() method - creates empty builder of same version
        if (config.isGenerateBuilders()) {
            builder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a new empty builder of the same version as this instance.\n")
                    .addJavadoc("<p>Unlike {@link #toBuilder()}, this creates an empty builder without\n")
                    .addJavadoc("copying any values from this instance.</p>\n")
                    .addJavadoc("@return Empty builder for creating new instances\n")
                    .build());
        }

        // Add getContext() method - returns VersionContext for this version
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        builder.addMethod(MethodSpec.methodBuilder("getContext")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(versionContextType)
                .addJavadoc("Get the VersionContext for this wrapper's version.\n")
                .addJavadoc("<p>The context provides factory methods for creating other wrapper types\n")
                .addJavadoc("of the same protocol version.</p>\n")
                .addJavadoc("@return VersionContext for this version\n")
                .build());

        // Add getFieldsInaccessibleInVersion() method for version compatibility checking
        TypeName listOfString = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class),
                ClassName.get(String.class)
        );
        builder.addMethod(MethodSpec.methodBuilder("getFieldsInaccessibleInVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listOfString)
                .addParameter(TypeName.INT, "targetVersion")
                .addJavadoc("Get fields that have values but will be inaccessible in the target version.\n")
                .addJavadoc("<p>These fields exist in the current version but not in the target version.\n")
                .addJavadoc("The data is NOT lost (protobuf preserves unknown fields), but it cannot be\n")
                .addJavadoc("accessed through the target version's API.</p>\n")
                .addJavadoc("@param targetVersion Target protocol version to check\n")
                .addJavadoc("@return List of field names that will become inaccessible\n")
                .build());

        // Add canConvertLosslesslyTo() convenience method
        builder.addMethod(MethodSpec.methodBuilder("canConvertLosslesslyTo")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.INT, "targetVersion")
                .addJavadoc("Check if conversion to target version will keep all data accessible.\n")
                .addJavadoc("<p>Returns true if no populated fields will become inaccessible.</p>\n")
                .addJavadoc("@param targetVersion Target protocol version to check\n")
                .addJavadoc("@return true if all populated fields will remain accessible\n")
                .addStatement("return getFieldsInaccessibleInVersion(targetVersion).isEmpty()")
                .build());

        // Add asVersionStrict() method - throws if data would become inaccessible
        TypeVariableName typeVarStrict = TypeVariableName.get("T", ClassName.get(config.getApiPackage(), message.getInterfaceName()));
        builder.addMethod(MethodSpec.methodBuilder("asVersionStrict")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(typeVarStrict)
                .returns(typeVarStrict)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVarStrict), "versionClass")
                .addJavadoc("Convert to a specific version with strict data accessibility check.\n")
                .addJavadoc("<p>Unlike {@link #asVersion(Class)}, this method throws an exception if any\n")
                .addJavadoc("populated fields would become inaccessible in the target version.</p>\n")
                .addJavadoc("<p><b>Note:</b> Data is NOT physically lost (protobuf preserves unknown fields),\n")
                .addJavadoc("but it cannot be accessed through the target version's API.</p>\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                .addJavadoc("@throws IllegalStateException if any populated fields would become inaccessible\n")
                .build());
    }

    /**
     * Generate and write interface using context.
     */
    public Path generateAndWrite(MergedMessage message, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, ctx);
        writeToFile(javaFile);

        String relativePath = ctx.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write interface.
     * @param message Merged message info
     * @return Path to the generated file
     * @throws IOException if writing fails
     * @deprecated Use {@link #generateAndWrite(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public Path generateAndWrite(MergedMessage message) throws IOException {
        JavaFile javaFile = generate(message);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Check if field is primitive or "primitive-like" (String, bytes).
     * String and bytes are treated as primitive-like because they can be accessed
     * directly without needing message wrappers, unlike nested message types.
     */
    private boolean isPrimitiveOrPrimitiveLike(FieldInfo field) {
        if (field.isPrimitive()) {
            return true;
        }
        // String and bytes are "primitive-like" - they're not nested messages
        String javaType = field.getJavaType();
        return "String".equals(javaType) || "byte[]".equals(javaType) || "ByteString".equals(javaType);
    }

}
