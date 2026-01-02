package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.List;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for REPEATED_SINGLE type conflict fields.
 *
 * <p>This handler processes fields where different proto versions have
 * different cardinality: some versions have repeated fields while others
 * have singular (optional/required) fields.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Order { repeated int32 items = 1; }
 * // v2: message Order { int32 item = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Uses {@code List<T>} as the unified type:</p>
 * <ul>
 *   <li>Reading from singular: wraps value in {@code Collections.singletonList()}</li>
 *   <li>Reading from repeated: returns list directly</li>
 *   <li>Writing to singular: validates list has exactly 1 element, extracts first</li>
 *   <li>Writing to repeated: adds all elements to list</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code List<Integer> getItems()}, {@code Builder.setItems(List<Integer>)}</li>
 *   <li><b>Getter:</b> Wraps singular values in singletonList</li>
 *   <li><b>Setter:</b> Validates single-element list for singular versions</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder setter methods include size validation for singular versions:</p>
 * <ul>
 *   <li>When setting to repeated version: adds all elements</li>
 *   <li>When setting to singular version: validates list.size() == 1</li>
 *   <li>Throws {@code IllegalArgumentException} if list is empty or has multiple elements</li>
 * </ul>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see MergedField.ConflictType#REPEATED_SINGLE
 */
public final class RepeatedSingleHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final RepeatedSingleHandler INSTANCE = new RepeatedSingleHandler();

    private RepeatedSingleHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.REPEATED_SINGLE;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return field.getConflictType() == MergedField.ConflictType.REPEATED_SINGLE;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName listType = getListType(field);

        // has method (for optional fields)
        addAbstractHasMethod(builder, field, ctx);

        // extract method returning List<T>
        addAbstractExtractMethod(builder, field, listType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldExtract(builder, field, ctx);
            return;
        }

        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        boolean isRepeatedInVersion = versionField != null && versionField.isRepeated();

        TypeName listType = getListType(field);

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(listType)
                .addParameter(ctx.protoClassName(), "proto");

        if (isRepeatedInVersion) {
            // Repeated in this version - return list directly
            extract.addStatement("return proto.get$LList()", versionJavaName);
        } else {
            // Singular in this version - wrap in singletonList
            extract.addStatement("return $T.singletonList(proto.get$L())",
                    Collections.class, versionJavaName);
        }

        builder.addMethod(extract.build());

        // has method implementation
        if (field.isOptional()) {
            addHasExtractImpl(builder, field, versionField, presentInVersion, versionJavaName, isRepeatedInVersion, ctx);
        }
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Getter delegates to extract method and returns the list
        DefaultHandler.INSTANCE.addGetterImplementation(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());
        TypeName listType = getListType(field);
        TypeName elementType = getElementType(field);

        // doSet (replace all with list)
        addAbstractDoSet(builder, "doSet" + capName, listType, field.getJavaName());

        // doAdd (add single element)
        addAbstractDoSet(builder, "doAdd" + capName, elementType, field.getJavaName());

        // doAddAll (add list of elements)
        addAbstractDoSet(builder, "doAddAll" + capName, listType, field.getJavaName());

        // doClear
        addAbstractDoClear(builder, "doClear" + capName);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());
        TypeName listType = getListType(field);
        TypeName elementType = getElementType(field);

        // set (replace all)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(listType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // add (single element)
        builder.addMethod(MethodSpec.methodBuilder("add" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(elementType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doAdd$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // addAll (list of elements)
        builder.addMethod(MethodSpec.methodBuilder("addAll" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(listType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doAddAll$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // clear
        addConcreteClearMethod(builder, field.getJavaName(), builderReturnType, ctx);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        FieldInfo versionField = field.getVersionFields().get(version);
        boolean isRepeatedInVersion = versionField != null && versionField.isRepeated();

        TypeName listType = getListType(field);
        TypeName elementType = getElementType(field);

        // doSet - replace all elements
        buildDoSetImpl(builder, "doSet" + capitalizedName, listType, field.getJavaName(),
                presentInVersion, m -> {
                    if (isRepeatedInVersion) {
                        // Repeated: clear and add all
                        m.addStatement("protoBuilder.clear$L()", versionJavaName);
                        m.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                    } else {
                        // Singular: validate single element
                        addSingularSetterValidation(m, field.getJavaName(), versionJavaName, version);
                    }
                });

        // doAdd - add single element
        buildDoSetImpl(builder, "doAdd" + capitalizedName, elementType, field.getJavaName(),
                presentInVersion, m -> {
                    if (isRepeatedInVersion) {
                        // Repeated: add to list
                        m.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                    } else {
                        // Singular: can only add if not set yet (would require tracking state)
                        // For simplicity, just set the value (replaces any existing)
                        m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                    }
                });

        // doAddAll - add list of elements
        buildDoSetImpl(builder, "doAddAll" + capitalizedName, listType, field.getJavaName(),
                presentInVersion, m -> {
                    if (isRepeatedInVersion) {
                        // Repeated: add all to list
                        m.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                    } else {
                        // Singular: validate single element and set
                        addSingularSetterValidation(m, field.getJavaName(), versionJavaName, version);
                    }
                });

        // doClear
        buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
    }

    /**
     * Gets the List type for the unified interface.
     */
    private TypeName getListType(MergedField field) {
        TypeName elementType = getElementType(field);
        return ParameterizedTypeName.get(ClassName.get(List.class), elementType);
    }

    /**
     * Gets the element type for the list.
     */
    private TypeName getElementType(MergedField field) {
        String resolvedType = field.getJavaType();
        if (resolvedType != null && resolvedType.startsWith("java.util.List<")) {
            // Extract element type from List<T>
            String elementTypeStr = resolvedType.substring(
                    "java.util.List<".length(),
                    resolvedType.length() - 1);
            return boxedTypeName(elementTypeStr);
        }
        // Fall back to field's Java type (should be boxed for List element)
        return boxedTypeName(field.getJavaType());
    }

    /**
     * Converts a type name string to a boxed TypeName.
     */
    private TypeName boxedTypeName(String typeName) {
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
     * Adds validation for setting a list to a singular field.
     * Validates list has exactly one element.
     */
    private void addSingularSetterValidation(MethodSpec.Builder doSet, String fieldName,
                                              String versionJavaName, String version) {
        doSet.beginControlFlow("if ($L == null || $L.isEmpty())", fieldName, fieldName);
        doSet.addStatement("throw new $T(\"List cannot be empty for singular field in $L\")",
                IllegalArgumentException.class, version);
        doSet.endControlFlow();

        doSet.beginControlFlow("if ($L.size() > 1)", fieldName);
        doSet.addStatement("throw new $T(\"List must have exactly one element for singular field in $L, got \" + $L.size())",
                IllegalArgumentException.class, version, fieldName);
        doSet.endControlFlow();

        doSet.addStatement("protoBuilder.set$L($L.get(0))", versionJavaName, fieldName);
    }

    /**
     * Adds extract method for missing field (returns empty list).
     */
    private void addMissingFieldExtract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName listType = getListType(field);

        MethodSpec extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(listType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return $T.emptyList()", Collections.class)
                .build();
        builder.addMethod(extract);

        // has method returns false
        if (field.isOptional()) {
            MethodSpec hasMethod = MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addJavadoc("Field not present in this version.\n")
                    .addStatement("return false")
                    .build();
            builder.addMethod(hasMethod);
        }
    }

    /**
     * Adds has extract implementation.
     */
    private void addHasExtractImpl(TypeSpec.Builder builder, MergedField field,
                                    FieldInfo versionField, boolean presentInVersion,
                                    String versionJavaName, boolean isRepeatedInVersion,
                                    ProcessingContext ctx) {
        MethodSpec.Builder hasMethod = MethodSpec.methodBuilder(field.getExtractHasMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addParameter(ctx.protoClassName(), "proto");

        if (presentInVersion) {
            if (isRepeatedInVersion) {
                // For repeated: check if count > 0
                hasMethod.addStatement("return proto.get$LCount() > 0", versionJavaName);
            } else {
                // For singular: use has method if available
                hasMethod.addStatement("return proto.has$L()", versionJavaName);
            }
        } else {
            hasMethod.addJavadoc("Field not present in this version.\n");
            hasMethod.addStatement("return false");
        }

        builder.addMethod(hasMethod.build());
    }
}
