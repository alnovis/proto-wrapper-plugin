package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

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

    // Legacy field - kept for backward compatibility
    @Deprecated
    private TypeResolver typeResolver;

    public InterfaceGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @param schema The merged schema
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
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
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    ConflictEnumInfo enumInfo = enumInfoOpt.get();
                    MethodSpec enumGetter = generateEnumGetter(field, enumInfo, resolver);
                    interfaceBuilder.addMethod(enumGetter);
                }
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

        // Add nested enums first
        for (MergedEnum nestedEnum : message.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            interfaceBuilder.addType(enumSpec);
        }

        // Add nested interfaces for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            TypeSpec nestedInterface = generateNestedInterface(nested, ctx);
            interfaceBuilder.addType(nestedInterface);
        }

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

            TypeSpec builderInterface = generateBuilderInterface(message, resolver, ctx);
            interfaceBuilder.addType(builderInterface);
        }

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(ctx.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate interface for a merged message.
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
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
            Map<String, FieldInfo> versionFields = field.getVersionFields();
            List<String> typeInfo = new java.util.ArrayList<>();
            for (Map.Entry<String, FieldInfo> entry : versionFields.entrySet()) {
                typeInfo.add(entry.getKey() + "=" + entry.getValue().getJavaType());
            }
            builder.addJavadoc(String.join(", ", typeInfo) + "</p>\n");

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

            if (conflictType == MergedField.ConflictType.INT_ENUM || conflictType == MergedField.ConflictType.WIDENING) {
                // These conflicts are resolved - return true for all versions where field exists
                builder.addJavadoc("<p>The conflict is automatically handled; this method returns true for versions: $L</p>\n",
                        presentVersions);
                builder.addJavadoc("@return true if this version has the field\n");
                builder.addStatement("return $L", buildVersionCheck(presentVersions));
            } else if (conflictType == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                // For PRIMITIVE_MESSAGE, the getter returns the primitive type
                // Only return true for versions where the field IS primitive
                Set<String> primitiveVersions = new java.util.HashSet<>();
                for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
                    if (entry.getValue().isPrimitive()) {
                        primitiveVersions.add(entry.getKey());
                    }
                }
                if (primitiveVersions.isEmpty()) {
                    builder.addJavadoc("<p>This field is a message type in all versions.</p>\n");
                    builder.addJavadoc("@return false (primitive getter not available)\n");
                    builder.addStatement("return false");
                } else {
                    builder.addJavadoc("<p>Returns true only for versions with primitive type: $L</p>\n", primitiveVersions);
                    builder.addJavadoc("@return true if this version has primitive type for this field\n");
                    builder.addStatement("return $L", buildVersionCheck(primitiveVersions));
                }
            } else {
                // NARROWING, STRING_BYTES - only some versions work properly
                builder.addJavadoc("<p>Returns false for versions where the field type is incompatible.</p>\n");
                builder.addJavadoc("@return true if this version has compatible type for this field\n");
                builder.addStatement("return $L", buildVersionCheck(presentVersions));
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
            // Multiple versions - build OR condition
            List<String> checks = new java.util.ArrayList<>();
            for (String version : versions) {
                String numericPart = version.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    checks.add("getWrapperVersion() == " + numericPart);
                }
            }
            if (checks.isEmpty()) {
                return "true";
            }
            return String.join(" || ", checks);
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
        // Find the message type from the version fields
        String messageTypeName = null;
        Set<String> messageVersions = new java.util.LinkedHashSet<>();

        for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
            FieldInfo fieldInfo = entry.getValue();
            if (!fieldInfo.isPrimitive() && fieldInfo.getTypeName() != null) {
                messageTypeName = fieldInfo.getJavaType();
                messageVersions.add(entry.getKey());
            }
        }

        if (messageTypeName == null) {
            return null; // No message type found
        }

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

        // Find message versions
        Set<String> messageVersions = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
            FieldInfo fieldInfo = entry.getValue();
            if (!fieldInfo.isPrimitive() && fieldInfo.getTypeName() != null) {
                messageVersions.add(entry.getKey());
            }
        }

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

    private TypeSpec generateNestedInterface(MergedMessage nested, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(nested.getInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested interface for $L.\n", nested.getName());

        for (MergedField field : nested.getFieldsSorted()) {
            // Use shared generateGetter for consistent Javadoc including conflict info
            MethodSpec getter = generateGetter(field, nested, resolver);
            builder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                builder.addMethod(generateHasMethod(field, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    ConflictEnumInfo enumInfo = enumInfoOpt.get();
                    MethodSpec enumGetter = generateEnumGetter(field, enumInfo, resolver);
                    builder.addMethod(enumGetter);
                }
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

        for (MergedEnum nestedEnum : nested.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            builder.addType(enumSpec);
        }

        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            TypeSpec deeplyNestedInterface = generateNestedInterface(deeplyNested, ctx);
            builder.addType(deeplyNestedInterface);
        }

        // Add Builder interface for nested message if enabled
        if (config.isGenerateBuilders()) {
            builder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .build());

            TypeSpec nestedBuilder = generateNestedBuilderInterface(nested, resolver, ctx);
            builder.addType(nestedBuilder);
        }

        return builder.build();
    }

    private TypeSpec generateNestedBuilderInterface(MergedMessage nested, TypeResolver resolver, GenerationContext ctx) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for (MergedField field : nested.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                addSkippedFieldNote(builder, field, "type conflict");
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), resolver);
                    continue;
                }
            }

            // Handle WIDENING conflicts with setter using wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningBuilderMethods(builder, field, resolver);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (String and byte[]) (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesBuilderMethods(builder, field, resolver);
                continue;
            }

            // Skip builder methods for fields with non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, nested);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);

                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get("", "Builder"))
                        .build());
            } else {
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get("", "Builder"))
                            .build());
                }
            }
        }

        // Return type is the nested interface itself
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("", nested.getInterfaceName()))
                .build());

        return builder.build();
    }

    private TypeSpec generateBuilderInterface(MergedMessage message, TypeResolver resolver, GenerationContext ctx) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Builder for creating and modifying $L instances.\n", message.getInterfaceName());

        // Add setter methods for each field
        for (MergedField field : message.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                addSkippedFieldNote(builder, field, "type conflict");
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), resolver);
                    continue;
                }
            }

            // Handle WIDENING conflicts with setter using wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningBuilderMethods(builder, field, resolver);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (String and byte[]) (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesBuilderMethods(builder, field, resolver);
                continue;
            }

            // Skip builder methods for fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                // Add Javadoc note about skipped field
                builder.addJavadoc("\n<p><b>Note:</b> {@code $L} setter not available due to type conflict ($L).</p>\n",
                        field.getJavaName(), field.getConflictType());
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, message);

            if (field.isRepeated()) {
                // For repeated fields: add, addAll, set (replace all), clear
                TypeName singleElementType = extractListElementType(fieldType);

                // add single element
                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Add a single $L element.\n", field.getJavaName())
                        .build());

                // addAll
                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Add all $L elements.\n", field.getJavaName())
                        .build());

                // set (replace all)
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Replace all $L elements.\n", field.getJavaName())
                        .build());

                // clear
                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Clear all $L elements.\n", field.getJavaName())
                        .build());
            } else {
                // Regular setter
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Set $L value.\n", field.getJavaName())
                        .build());

                // Clear method for optional fields
                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get("", "Builder"))
                            .addJavadoc("Clear $L value.\n", field.getJavaName())
                            .build());
                }
            }
        }

        // Add build method
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(config.getApiPackage(), message.getInterfaceName()))
                .addJavadoc("Build the $L instance.\n", message.getInterfaceName())
                .addJavadoc("@return New immutable instance\n")
                .build());

        return builder.build();
    }

    /**
     * Add overloaded builder methods for INT_ENUM conflict field.
     * Generates both int and enum setters.
     */
    private void addIntEnumBuilderMethods(TypeSpec.Builder builder, MergedField field,
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
    private void addWideningBuilderMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Get the wider type from the field (already resolved in VersionMerger)
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());

        // Build version info for javadoc
        Map<String, FieldInfo> versionFields = field.getVersionFields();
        List<String> typeInfo = new java.util.ArrayList<>();
        for (Map.Entry<String, FieldInfo> entry : versionFields.entrySet()) {
            typeInfo.add(entry.getKey() + "=" + entry.getValue().getJavaType());
        }
        String versionsStr = String.join(", ", typeInfo);

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
     * Get the primitive TypeName for a wider type string.
     */
    private TypeName getWiderPrimitiveType(String javaType) {
        return switch (javaType) {
            case "long", "Long" -> TypeName.LONG;
            case "double", "Double" -> TypeName.DOUBLE;
            case "int", "Integer" -> TypeName.INT;
            default -> TypeName.LONG; // Default to long for numeric widening
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
     * Add builder methods for STRING_BYTES conflict field.
     * Generates both String and byte[] setters for unified access.
     */
    private void addStringBytesBuilderMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName builderType = ClassName.get("", "Builder");

        // Build version info for javadoc
        Map<String, FieldInfo> versionFields = field.getVersionFields();
        List<String> typeInfo = new java.util.ArrayList<>();
        for (Map.Entry<String, FieldInfo> entry : versionFields.entrySet()) {
            typeInfo.add(entry.getKey() + "=" + entry.getValue().getJavaType());
        }
        String versionsStr = String.join(", ", typeInfo);

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
     * Extract element type from List<T> type.
     */
    private TypeName extractListElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterized = (ParameterizedTypeName) listType;
            if (!parameterized.typeArguments.isEmpty()) {
                return parameterized.typeArguments.get(0);
            }
        }
        // Fallback to Object
        return ClassName.get(Object.class);
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
        builder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the wrapper protocol version this instance was created from.\n")
                .addJavadoc("@return Protocol version (e.g., 1, 2)\n")
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
     * @deprecated Use {@link #generateAndWrite(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
    public Path generateAndWrite(MergedMessage message) throws IOException {
        JavaFile javaFile = generate(message);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
