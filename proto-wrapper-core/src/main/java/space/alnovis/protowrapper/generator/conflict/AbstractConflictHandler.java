package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import java.util.function.Consumer;

/**
 * Sealed base class providing common functionality for conflict handlers.
 *
 * <p>This class delegates to specialized generators for different aspects of code generation:</p>
 * <ul>
 *   <li>{@link ExtractMethodGenerator} - extract, has, and getter methods</li>
 *   <li>{@link BuilderMethodGenerator} - builder methods (doSet, doClear, set, clear, etc.)</li>
 * </ul>
 *
 * <p>Protected methods are provided for backward compatibility with existing handlers.
 * New code should prefer using the generator classes directly for better clarity.</p>
 *
 * <p>This is a sealed class that permits only the known handler implementations,
 * ensuring type safety and preventing accidental external extensions.</p>
 *
 * @see ExtractMethodGenerator
 * @see BuilderMethodGenerator
 * @see ConflictHandler
 */
public abstract sealed class AbstractConflictHandler permits
        IntEnumHandler, EnumEnumHandler, StringBytesHandler, WideningHandler, FloatDoubleHandler,
        SignedUnsignedHandler, RepeatedSingleHandler, PrimitiveMessageHandler,
        RepeatedConflictHandler, MapFieldHandler, WellKnownTypeHandler, RepeatedWellKnownTypeHandler,
        DefaultHandler {

    // ========== Extract/Has Method Generation (delegate to ExtractMethodGenerator) ==========

    /**
     * Add an abstract extractHas method for optional fields.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    protected void addAbstractHasMethod(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        ExtractMethodGenerator.addAbstractHasMethod(builder, field, ctx);
    }

    /**
     * Add an abstract extract method.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param ctx the processing context
     */
    protected void addAbstractExtractMethod(TypeSpec.Builder builder, MergedField field,
                                             TypeName returnType, ProcessingContext ctx) {
        ExtractMethodGenerator.addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    /**
     * Add a concrete has method implementation for present fields.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param versionJavaName the Java name for this version
     * @param ctx the processing context
     */
    protected void addHasMethodImpl(TypeSpec.Builder builder, MergedField field,
                                     String versionJavaName, ProcessingContext ctx) {
        ExtractMethodGenerator.addHasMethodImpl(builder, field, versionJavaName, ctx);
    }

    /**
     * Add a concrete has method implementation returning false (field not present).
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    protected void addMissingHasMethodImpl(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        ExtractMethodGenerator.addMissingHasMethodImpl(builder, field, ctx);
    }

    /**
     * Add a getter implementation that delegates to extract methods.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param returnType the return type for the getter
     * @param ctx the processing context
     */
    protected void addStandardGetterImpl(TypeSpec.Builder builder, MergedField field,
                                          TypeName returnType, ProcessingContext ctx) {
        ExtractMethodGenerator.addStandardGetterImpl(builder, field, returnType, ctx);
    }

    /**
     * Add has method implementation to abstract class.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    protected void addHasMethodToAbstract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        ExtractMethodGenerator.addHasMethodToAbstract(builder, field, ctx);
    }

    /**
     * Get the FieldInfo for the current version.
     *
     * @param field the merged field
     * @param ctx the processing context containing version info
     * @return the FieldInfo for this version, or null if not present
     */
    protected FieldInfo getVersionField(MergedField field, ProcessingContext ctx) {
        return ctx.versionSnapshot(field).fieldInfo();
    }

    /**
     * Add extract and optionally has method for a field not present in this version.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param defaultValue the default value literal (e.g., "0", "0L", "null")
     * @param ctx the processing context
     */
    protected void addMissingFieldExtract(TypeSpec.Builder builder, MergedField field,
                                           TypeName returnType, String defaultValue,
                                           ProcessingContext ctx) {
        ExtractMethodGenerator.addMissingFieldExtract(builder, field, returnType, defaultValue, ctx);
    }

    /**
     * Add extract and optionally has method for a field not present in this version.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param defaultValueFormat the format string for the default value
     * @param defaultValueArgs arguments for the format string
     * @param ctx the processing context
     */
    protected void addMissingFieldExtract(TypeSpec.Builder builder, MergedField field,
                                           TypeName returnType, String defaultValueFormat,
                                           Object[] defaultValueArgs, ProcessingContext ctx) {
        ExtractMethodGenerator.addMissingFieldExtract(builder, field, returnType, defaultValueFormat,
                defaultValueArgs, ctx);
    }

    /**
     * Add extract and optionally has method for a field not present in this version,
     * with auto-resolved default value from resolver.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    protected void addMissingFieldExtractWithResolvedDefault(TypeSpec.Builder builder,
                                                              MergedField field,
                                                              ProcessingContext ctx) {
        ExtractMethodGenerator.addMissingFieldExtractWithResolvedDefault(builder, field, ctx);
    }

    // ========== Abstract Builder Methods (delegate to BuilderMethodGenerator) ==========

    /**
     * Add an abstract doSet method to the builder.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param paramType the type of the method parameter
     * @param paramName the name of the method parameter
     */
    protected void addAbstractDoSet(TypeSpec.Builder builder, String methodName,
                                     TypeName paramType, String paramName) {
        BuilderMethodGenerator.addAbstractDoSet(builder, methodName, paramType, paramName);
    }

    /**
     * Add an abstract doClear method to the builder.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     */
    protected void addAbstractDoClear(TypeSpec.Builder builder, String methodName) {
        BuilderMethodGenerator.addAbstractDoClear(builder, methodName);
    }

    /**
     * Add abstract doSet and optionally doClear for a scalar field.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param fieldType the type of the field
     * @param ctx the processing context
     */
    protected void addAbstractScalarBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                    TypeName fieldType, ProcessingContext ctx) {
        BuilderMethodGenerator.addAbstractScalarMethods(builder, field, fieldType, ctx);
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
    protected void addAbstractRepeatedBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                      TypeName singleElementType, TypeName listType,
                                                      ProcessingContext ctx) {
        BuilderMethodGenerator.addAbstractRepeatedMethods(builder, field, singleElementType, listType, ctx);
    }

    // ========== Concrete Builder Methods (delegate to BuilderMethodGenerator) ==========

    /**
     * Build a concrete setXxx method that delegates to doSetXxx.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param fieldName the name of the field
     * @param paramType the type of the method parameter
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    protected void addConcreteSetMethod(TypeSpec.Builder builder, String fieldName,
                                         TypeName paramType, TypeName builderReturnType,
                                         ProcessingContext ctx) {
        BuilderMethodGenerator.addConcreteSet(builder, fieldName, paramType, builderReturnType, ctx);
    }

    /**
     * Build a concrete clearXxx method that delegates to doClearXxx.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param fieldName the name of the field
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    protected void addConcreteClearMethod(TypeSpec.Builder builder, String fieldName,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        BuilderMethodGenerator.addConcreteClear(builder, fieldName, builderReturnType, ctx);
    }

    /**
     * Add standard concrete builder methods for a scalar field.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param fieldType the type of the field
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    protected void addScalarConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                    TypeName fieldType, TypeName builderReturnType,
                                                    ProcessingContext ctx) {
        BuilderMethodGenerator.addConcreteScalarMethods(builder, field, fieldType, builderReturnType, ctx);
    }

    /**
     * Add standard concrete builder methods for a repeated field.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param singleElementType the type of a single list element
     * @param listType the type of the list parameter
     * @param builderReturnType the return type for builder methods
     * @param ctx the processing context
     */
    protected void addRepeatedConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                      TypeName singleElementType, TypeName listType,
                                                      TypeName builderReturnType, ProcessingContext ctx) {
        BuilderMethodGenerator.addConcreteRepeatedMethods(builder, field, singleElementType, listType,
                builderReturnType, ctx);
    }

    // ========== Builder Impl Methods (delegate to BuilderMethodGenerator) ==========

    /**
     * Build a doSet implementation method with version-conditional body.
     *
     * @param builder TypeSpec builder to add method to
     * @param methodName Full method name (e.g., "doSetName")
     * @param paramType Parameter type
     * @param paramName Parameter name
     * @param presentInVersion Whether field is present in current version
     * @param bodyBuilder Consumer that builds the method body when field is present
     */
    protected void buildDoSetImpl(TypeSpec.Builder builder, String methodName,
                                   TypeName paramType, String paramName,
                                   boolean presentInVersion,
                                   Consumer<MethodSpec.Builder> bodyBuilder) {
        BuilderMethodGenerator.buildDoSetImpl(builder, methodName, paramType, paramName,
                presentInVersion, bodyBuilder);
    }

    /**
     * Build a doSet implementation method that throws when field not present.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param paramType the type of the method parameter
     * @param paramName the name of the method parameter
     * @param presentInVersion whether the field is present in the current version
     * @param field the merged field definition
     * @param bodyBuilder consumer that builds the method body when field is present
     */
    protected void buildDoSetImplOrThrow(TypeSpec.Builder builder, String methodName,
                                          TypeName paramType, String paramName,
                                          boolean presentInVersion, MergedField field,
                                          Consumer<MethodSpec.Builder> bodyBuilder) {
        BuilderMethodGenerator.buildDoSetImplOrThrow(builder, methodName, paramType, paramName,
                presentInVersion, field, bodyBuilder);
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
    protected void buildSimpleDoSetImpl(TypeSpec.Builder builder, String methodName,
                                         TypeName paramType, String paramName,
                                         boolean presentInVersion, String versionJavaName) {
        BuilderMethodGenerator.buildSimpleDoSetImpl(builder, methodName, paramType, paramName,
                presentInVersion, versionJavaName);
    }

    /**
     * Build a doClear implementation method with version-conditional clear.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param methodName the name of the method to generate
     * @param presentInVersion whether the field is present in the current version
     * @param versionJavaName the Java name for this version
     */
    protected void buildDoClearImpl(TypeSpec.Builder builder, String methodName,
                                     boolean presentInVersion, String versionJavaName) {
        BuilderMethodGenerator.buildDoClearImpl(builder, methodName, presentInVersion, versionJavaName);
    }

    /**
     * Build a doClear implementation for a field using field's method name.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param presentInVersion whether the field is present in the current version
     * @param versionJavaName the Java name for this version
     */
    protected void buildDoClearImplForField(TypeSpec.Builder builder, MergedField field,
                                             boolean presentInVersion, String versionJavaName) {
        BuilderMethodGenerator.buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
    }
}
