package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

import java.util.function.Consumer;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Generates builder-related methods for conflict handlers.
 *
 * <p>This class extracts builder method generation logic from AbstractConflictHandler
 * to improve separation of concerns and reduce class size.</p>
 *
 * <h2>Generated Method Categories</h2>
 * <ul>
 *   <li><b>Abstract methods:</b> doSetXxx, doClearXxx, doAddXxx, doAddAllXxx</li>
 *   <li><b>Concrete methods:</b> setXxx, clearXxx, addXxx, addAllXxx (delegate to doXxx)</li>
 *   <li><b>Impl methods:</b> doXxx implementations with version-conditional logic</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In handler:
 * BuilderMethodGenerator.addAbstractScalarMethods(builder, field, fieldType, ctx);
 * BuilderMethodGenerator.addConcreteScalarMethods(builder, field, fieldType, returnType, ctx);
 * }</pre>
 *
 * @since 1.6.5
 * @see AbstractConflictHandler
 * @see ConflictHandler
 */
public final class BuilderMethodGenerator {

    private BuilderMethodGenerator() {
        // Utility class - no instantiation
    }

    // ========== Abstract Builder Methods (doXxx declarations) ==========

    /**
     * Add an abstract doSet method to the builder.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param paramType the type of the method parameter
     * @param paramName the name of the method parameter
     */
    public static void addAbstractDoSet(TypeSpec.Builder builder, String methodName,
                                         TypeName paramType, String paramName) {
        builder.addMethod(MethodSpecFactory.protectedAbstractDo(methodName, paramType, paramName).build());
    }

    /**
     * Add an abstract doClear method to the builder.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     */
    public static void addAbstractDoClear(TypeSpec.Builder builder, String methodName) {
        builder.addMethod(MethodSpecFactory.protectedAbstractDoNoParam(methodName).build());
    }

    /**
     * Add abstract doSet and optionally doClear for a scalar field.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param fieldType the type of the field
     * @param ctx the processing context
     */
    public static void addAbstractScalarMethods(TypeSpec.Builder builder, MergedField field,
                                                 TypeName fieldType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // doSet
        addAbstractDoSet(builder, "doSet" + capName, fieldType, field.getJavaName());

        // doClear for fields with has*()
        if (field.shouldGenerateHasMethod()) {
            addAbstractDoClear(builder, "doClear" + capName);
        }
    }

    /**
     * Add abstract doAdd, doAddAll, doSet, doClear for a repeated field.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param singleElementType the type of a single list element
     * @param listType the type of the list parameter
     * @param ctx the processing context
     */
    public static void addAbstractRepeatedMethods(TypeSpec.Builder builder, MergedField field,
                                                   TypeName singleElementType, TypeName listType,
                                                   ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // doAdd
        addAbstractDoSet(builder, "doAdd" + capName, singleElementType, field.getJavaName());

        // doAddAll
        addAbstractDoSet(builder, "doAddAll" + capName, listType, field.getJavaName());

        // doSet (replace all)
        addAbstractDoSet(builder, "doSet" + capName, listType, field.getJavaName());

        // doClear
        addAbstractDoClear(builder, "doClear" + capName);
    }

    // ========== Concrete Builder Methods (setXxx -> doSetXxx) ==========

    /**
     * Build a concrete setXxx method that delegates to doSetXxx.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param fieldName the name of the field
     * @param paramType the type of the method parameter
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    public static void addConcreteSet(TypeSpec.Builder builder, String fieldName,
                                       TypeName paramType, TypeName builderReturnType,
                                       ProcessingContext ctx) {
        String capName = ctx.capitalize(fieldName);
        String methodName = MethodSpecFactory.methodName("set", fieldName, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderSetter(methodName, paramType, fieldName, builderReturnType)
                .addStatement("doSet$L($L)", capName, fieldName)
                .addStatement("return this")
                .build());
    }

    /**
     * Build a concrete clearXxx method that delegates to doClearXxx.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param fieldName the name of the field
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    public static void addConcreteClear(TypeSpec.Builder builder, String fieldName,
                                         TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(fieldName);
        String methodName = MethodSpecFactory.methodName("clear", fieldName, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderNoParam(methodName, builderReturnType)
                .addStatement("doClear$L()", capName)
                .addStatement("return this")
                .build());
    }

    /**
     * Add standard concrete builder methods for a scalar field.
     * Generates: setXxx, clearXxx (if optional)
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param fieldType the type of the field
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    public static void addConcreteScalarMethods(TypeSpec.Builder builder, MergedField field,
                                                 TypeName fieldType, TypeName builderReturnType,
                                                 ProcessingContext ctx) {
        // set
        addConcreteSet(builder, field.getJavaName(), fieldType, builderReturnType, ctx);

        // clear for fields with has*()
        if (field.shouldGenerateHasMethod()) {
            addConcreteClear(builder, field.getJavaName(), builderReturnType, ctx);
        }
    }

    /**
     * Add standard concrete builder methods for a repeated field.
     * Generates: addXxx, addAllXxx, setXxx (replace all), clearXxx
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param singleElementType the type of a single list element
     * @param listType the type of the list parameter
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    public static void addConcreteRepeatedMethods(TypeSpec.Builder builder, MergedField field,
                                                   TypeName singleElementType, TypeName listType,
                                                   TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());
        String paramName = field.getJavaName();

        // add single
        String addMethodName = MethodSpecFactory.methodName("add", field, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderSetter(addMethodName, singleElementType, paramName, builderReturnType)
                .addStatement("doAdd$L($L)", capName, paramName)
                .addStatement("return this")
                .build());

        // addAll
        String addAllMethodName = MethodSpecFactory.methodName("addAll", field, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderSetter(addAllMethodName, listType, paramName, builderReturnType)
                .addStatement("doAddAll$L($L)", capName, paramName)
                .addStatement("return this")
                .build());

        // set (replace all)
        String setMethodName = MethodSpecFactory.methodName("set", field, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderSetter(setMethodName, listType, paramName, builderReturnType)
                .addStatement("doSet$L($L)", capName, paramName)
                .addStatement("return this")
                .build());

        // clear
        String clearMethodName = MethodSpecFactory.methodName("clear", field, ctx);
        builder.addMethod(MethodSpecFactory.publicFinalBuilderNoParam(clearMethodName, builderReturnType)
                .addStatement("doClear$L()", capName)
                .addStatement("return this")
                .build());
    }

    // ========== Builder Impl Methods (doXxx implementations) ==========

    /**
     * Build a doSet implementation method with version-conditional body.
     * Uses silent ignore when field not present (adds comment).
     *
     * @param builder TypeSpec builder to add method to
     * @param methodName Full method name (e.g., "doSetName")
     * @param paramType Parameter type
     * @param paramName Parameter name
     * @param presentInVersion Whether field is present in current version
     * @param bodyBuilder Consumer that builds the method body when field is present
     */
    public static void buildDoSetImpl(TypeSpec.Builder builder, String methodName,
                                       TypeName paramType, String paramName,
                                       boolean presentInVersion,
                                       Consumer<MethodSpec.Builder> bodyBuilder) {
        MethodSpec.Builder method = MethodSpecFactory.protectedDoImpl(methodName, paramType, paramName);

        addVersionConditional(method, presentInVersion, bodyBuilder);
        builder.addMethod(method.build());
    }

    /**
     * Build a doSet implementation method that throws when field not present.
     * Use this for setter operations where silently ignoring would be confusing.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param paramType the type of the method parameter
     * @param paramName the name of the method parameter
     * @param presentInVersion whether the field is present in the current version
     * @param field the merged field definition
     * @param bodyBuilder consumer that builds the method body when field is present
     */
    public static void buildDoSetImplOrThrow(TypeSpec.Builder builder, String methodName,
                                              TypeName paramType, String paramName,
                                              boolean presentInVersion, MergedField field,
                                              Consumer<MethodSpec.Builder> bodyBuilder) {
        MethodSpec.Builder method = MethodSpecFactory.protectedDoImpl(methodName, paramType, paramName);

        addVersionConditionalOrThrow(method, presentInVersion, field, bodyBuilder);
        builder.addMethod(method.build());
    }

    /**
     * Build a doSet implementation with simple protoBuilder.setXxx call.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param paramType the type of the method parameter
     * @param paramName the name of the method parameter
     * @param presentInVersion whether the field is present in the current version
     * @param versionJavaName the Java name for this version
     */
    public static void buildSimpleDoSetImpl(TypeSpec.Builder builder, String methodName,
                                             TypeName paramType, String paramName,
                                             boolean presentInVersion, String versionJavaName) {
        buildDoSetImpl(builder, methodName, paramType, paramName, presentInVersion,
                m -> m.addStatement("protoBuilder.set$L($L)", versionJavaName, paramName));
    }

    /**
     * Build a doClear implementation method with version-conditional clear.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param presentInVersion whether the field is present in the current version
     * @param versionJavaName the Java name for this version
     */
    public static void buildDoClearImpl(TypeSpec.Builder builder, String methodName,
                                         boolean presentInVersion, String versionJavaName) {
        MethodSpec.Builder method = MethodSpecFactory.protectedDoImplNoParam(methodName);

        addVersionConditionalClear(method, presentInVersion, versionJavaName);
        builder.addMethod(method.build());
    }

    /**
     * Build a doClear implementation for a field using field's method name.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param presentInVersion whether the field is present in the current version
     * @param versionJavaName the Java name for this version
     */
    public static void buildDoClearImplForField(TypeSpec.Builder builder, MergedField field,
                                                 boolean presentInVersion, String versionJavaName) {
        buildDoClearImpl(builder, field.getDoClearMethodName(), presentInVersion, versionJavaName);
    }
}
