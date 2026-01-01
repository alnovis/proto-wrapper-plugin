package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for INT_ENUM type conflict fields.
 *
 * <p>This handler processes fields where one proto version uses an integer type
 * (int32, int64) and another version uses an enum type for the same field number.
 * This is a common scenario when evolving proto schemas from raw integers to
 * type-safe enums.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Order { int32 status = 1; }
 * // v2: message Order { OrderStatus status = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The handler generates dual accessor methods:</p>
 * <ul>
 *   <li>{@code getStatus()} - returns int value (works with all versions)</li>
 *   <li>{@code getStatusEnum()} - returns unified ConflictEnum type</li>
 * </ul>
 *
 * <p>A unified {@code ConflictEnum} is generated containing all enum values
 * from all versions. The enum provides bidirectional conversion:</p>
 * <ul>
 *   <li>{@code getValue()} - returns the int value</li>
 *   <li>{@code fromProtoValue(int)} - converts int to enum constant</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code int getStatus()}, {@code StatusEnum getStatusEnum()},
 *       {@code Builder.setStatus(int)}, {@code Builder.setStatus(StatusEnum)}</li>
 *   <li><b>Abstract:</b> {@code extractStatus(proto)}, {@code extractStatusEnum(proto)}</li>
 *   <li><b>Impl:</b> Version-specific extraction with int-to-enum conversion</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder setter methods include validation:</p>
 * <ul>
 *   <li>{@code setStatus(int)} - validates the int value against enum range for enum versions</li>
 *   <li>{@code setStatus(StatusEnum)} - always valid, converts enum to int internally</li>
 * </ul>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see space.alnovis.protowrapper.model.ConflictEnumInfo
 */
public final class IntEnumHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final IntEnumHandler INSTANCE = new IntEnumHandler();

    private IntEnumHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method (returns int)
        addAbstractExtractMethod(builder, field, returnType, ctx);

        // Add enum extract method
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    String enumExtractMethodName = field.getExtractEnumMethodName();

                    builder.addMethod(MethodSpec.methodBuilder(enumExtractMethodName)
                            .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                            .returns(enumType)
                            .addParameter(ctx.protoType(), "proto")
                            .build());
                });
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, ctx);
            return;
        }

        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName intReturnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Main extract - returns int
        MethodSpec.Builder extractInt = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(intReturnType)
                .addParameter(ctx.protoClassName(), "proto");

        if (versionIsEnum) {
            extractInt.addStatement("return proto.get$L().getNumber()", versionJavaName);
        } else {
            extractInt.addStatement("return proto.get$L()", versionJavaName);
        }
        builder.addMethod(extractInt.build());

        // Enum extract - returns unified enum
        final boolean isEnumVersion = versionIsEnum;
        final String javaName = versionJavaName;
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    String enumExtractMethodName = field.getExtractEnumMethodName();

                    MethodSpec.Builder extractEnum = MethodSpec.methodBuilder(enumExtractMethodName)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PROTECTED)
                            .returns(enumType)
                            .addParameter(ctx.protoClassName(), "proto");

                    if (isEnumVersion) {
                        extractEnum.addStatement("return $T.fromProtoValue(proto.get$L().getNumber())",
                                enumType, javaName);
                    } else {
                        extractEnum.addStatement("return $T.fromProtoValue(proto.get$L())",
                                enumType, javaName);
                    }
                    builder.addMethod(extractEnum.build());
                });
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add standard getter (returns int)
        addStandardGetterImpl(builder, field, returnType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);

        // Add enum getter
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    String enumGetterName = "get" + ctx.capitalize(field.getJavaName()) + "Enum";
                    String enumExtractMethodName = field.getExtractEnumMethodName();

                    MethodSpec.Builder enumGetter = MethodSpec.methodBuilder(enumGetterName)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(enumType);

                    if (field.needsHasCheck()) {
                        enumGetter.addStatement("return $L(proto) ? $L(proto) : null",
                                field.getExtractHasMethodName(), enumExtractMethodName);
                    } else {
                        enumGetter.addStatement("return $L(proto)", enumExtractMethodName);
                    }

                    builder.addMethod(enumGetter.build());
                });
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // doSet (int) - use primitive int to match ImplClassGenerator
        addAbstractDoSet(builder, field.getDoSetMethodName(), TypeName.INT, field.getJavaName());

        // Add enum setter
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    addAbstractDoSet(builder, field.getDoSetMethodName(), enumType, field.getJavaName());
                });

        // doClear - always generated for INT_ENUM (interface has clearXxx unconditionally)
        addAbstractDoClear(builder, field.getDoClearMethodName());
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // doSet (int) - use primitive int to match abstract method and ImplClassGenerator
        buildDoSetImpl(builder, field.getDoSetMethodName(), TypeName.INT, field.getJavaName(),
                presentInVersion, m -> {
                    if (versionIsEnum) {
                        String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                        String enumMethod = getEnumFromIntMethod(ctx.config());
                        m.addStatement("protoBuilder.set$L($L.$L($L))",
                                versionJavaName, protoEnumType, enumMethod, field.getJavaName());
                    } else {
                        m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                    }
                });

        // doSet (enum)
        final boolean isEnumVersion = versionIsEnum;
        final String javaName = versionJavaName;
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    buildDoSetImpl(builder, field.getDoSetMethodName(), enumType, field.getJavaName(),
                            presentInVersion, m -> {
                                if (isEnumVersion) {
                                    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                                    String enumMethod = getEnumFromIntMethod(ctx.config());
                                    m.addStatement("protoBuilder.set$L($L.$L($L.getValue()))",
                                            javaName, protoEnumType, enumMethod, field.getJavaName());
                                } else {
                                    m.addStatement("protoBuilder.set$L($L.getValue())",
                                            javaName, field.getJavaName());
                                }
                            });
                });

        // doClear - always generated for INT_ENUM
        buildDoClearImpl(builder, field.getDoClearMethodName(), presentInVersion, versionJavaName);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema().getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isEmpty()) {
            return;
        }
        ConflictEnumInfo enumInfo = enumInfoOpt.get();
        ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());

        // Build version check condition for enum versions only
        java.util.Set<String> enumVersions = enumInfo.getEnumVersions();
        java.util.List<Integer> enumVersionNumbers = enumVersions.stream()
                .map(ctx.resolver()::extractVersionNumber)
                .sorted()
                .toList();

        // setXxx(int) with version-aware validation
        MethodSpec.Builder setIntMethod = MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderReturnType);

        if (!enumVersionNumbers.isEmpty()) {
            String versionCondition = enumVersionNumbers.stream()
                    .map(v -> "getVersion() == " + v)
                    .reduce((a, b) -> a + " || " + b)
                    .orElse("false");

            setIntMethod.beginControlFlow("if ($L)", versionCondition)
                    .addStatement("$T.fromProtoValueOrThrow($L)", enumType, field.getJavaName())
                    .endControlFlow();
        }

        setIntMethod.addStatement("$L($L)", field.getDoSetMethodName(), field.getJavaName())
                .addStatement("return this");
        builder.addMethod(setIntMethod.build());

        // setXxx(EnumType)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(enumType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", field.getDoSetMethodName(), field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx() - always generated for INT_ENUM fields (interface has it unconditionally)
        addConcreteClearMethod(builder, field.getJavaName(), builderReturnType, ctx);
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing int extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);
        builder.addMethod(extract.build());

        // Add missing enum extract method
        ctx.schema().getConflictEnum(ctx.message().getName(), field.getName())
                .ifPresent(enumInfo -> {
                    ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
                    String enumExtractMethodName = field.getExtractEnumMethodName();

                    builder.addMethod(MethodSpec.methodBuilder(enumExtractMethodName)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PROTECTED)
                            .returns(enumType)
                            .addParameter(ctx.protoClassName(), "proto")
                            .addJavadoc("Field not present in this version.\n")
                            .addStatement("return null")
                            .build());
                });
    }
}
