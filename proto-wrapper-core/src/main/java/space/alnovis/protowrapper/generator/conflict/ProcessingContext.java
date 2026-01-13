package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import space.alnovis.protowrapper.contract.ContractProvider;
import space.alnovis.protowrapper.contract.FieldMethodNames;
import space.alnovis.protowrapper.contract.MergedFieldContract;
import space.alnovis.protowrapper.generator.GenerationContext;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

/**
 * Context object containing all information needed for field processing.
 *
 * <p>This record encapsulates the common parameters passed to conflict handlers,
 * reducing parameter count and providing convenient accessor methods.</p>
 *
 * @param message The message containing the field being processed
 * @param protoType The proto type (either ClassName for impl or TypeVariableName for abstract)
 * @param genCtx The generation context with version and resolver info
 * @param config The generator configuration
 */
public record ProcessingContext(
        MergedMessage message,
        TypeName protoType,
        GenerationContext genCtx,
        GeneratorConfig config
) {
    /**
     * Create a context for implementation class generation.
     *
     * @param message the message being processed
     * @param protoType the proto class name
     * @param genCtx the generation context
     * @param config the generator configuration
     * @return the processing context
     */
    public static ProcessingContext forImpl(MergedMessage message, ClassName protoType,
                                             GenerationContext genCtx, GeneratorConfig config) {
        return new ProcessingContext(message, protoType, genCtx, config);
    }

    /**
     * Create a context for abstract class generation.
     *
     * @param message the message being processed
     * @param protoType the proto type variable name
     * @param genCtx the generation context
     * @param config the generator configuration
     * @return the processing context
     */
    public static ProcessingContext forAbstract(MergedMessage message, TypeVariableName protoType,
                                                 GenerationContext genCtx, GeneratorConfig config) {
        return new ProcessingContext(message, protoType, genCtx, config);
    }

    /**
     * Get the type resolver from the generation context.
     *
     * @return the type resolver
     */
    public TypeResolver resolver() {
        return genCtx.getTypeResolver();
    }

    /**
     * Get the merged schema from the generation context.
     *
     * @return the merged schema
     */
    public MergedSchema schema() {
        return genCtx.getSchema();
    }

    /**
     * Get the current version (if set).
     *
     * @return the current version, or null if not set
     */
    public String version() {
        return genCtx.getCurrentVersion();
    }

    /**
     * Get the current version, throwing if not set.
     *
     * @return the current version
     * @throws IllegalStateException if version is not set
     */
    public String requireVersion() {
        return genCtx.requireVersion();
    }

    /**
     * Get the API package name.
     *
     * @return the API package name
     */
    public String apiPackage() {
        return genCtx.getApiPackage();
    }

    /**
     * Get the implementation package name.
     *
     * @return the implementation package name
     */
    public String implPackage() {
        return genCtx.getImplPackage();
    }

    /**
     * Get the version number for the current version.
     *
     * @return the version number
     */
    public int versionNumber() {
        return genCtx.getVersionNumber();
    }

    /**
     * Check if the proto type is a concrete ClassName (for impl classes).
     *
     * @return true if proto type is a concrete ClassName
     */
    public boolean isConcreteType() {
        return protoType instanceof ClassName;
    }

    /**
     * Get the proto type as a ClassName.
     *
     * @return the proto type as a ClassName
     * @throws ClassCastException if protoType is not a ClassName
     */
    public ClassName protoClassName() {
        return (ClassName) protoType;
    }

    /**
     * Get the proto type as a TypeVariableName.
     *
     * @return the proto type as a TypeVariableName
     * @throws ClassCastException if protoType is not a TypeVariableName
     */
    public TypeVariableName protoTypeVariable() {
        return (TypeVariableName) protoType;
    }

    /**
     * Capitalize a string (first letter uppercase).
     *
     * @param s the string to capitalize
     * @return the capitalized string
     */
    public String capitalize(String s) {
        return resolver().capitalize(s);
    }

    /**
     * Parse the field type using the resolver.
     *
     * @param field the field to parse
     * @return the parsed TypeName
     */
    public TypeName parseFieldType(space.alnovis.protowrapper.model.MergedField field) {
        return resolver().parseFieldType(field, message);
    }

    /**
     * Get the implementation class name for a message.
     *
     * @param messageName the message name
     * @return the implementation class name
     */
    public String getImplClassName(String messageName) {
        return genCtx.getImplClassName(messageName);
    }

    // ==================== Contract Support ====================

    /**
     * Get the contract provider singleton.
     *
     * @return the contract provider
     */
    public ContractProvider contractProvider() {
        return ContractProvider.getInstance();
    }

    /**
     * Get the contract for a field.
     *
     * <p>This is a convenience method that delegates to {@link ContractProvider}
     * and caches contracts per message.</p>
     *
     * @param field the field to get contract for
     * @return the merged field contract
     */
    public MergedFieldContract getContractFor(MergedField field) {
        return contractProvider().getContract(message, field);
    }

    /**
     * Get method names for a field.
     *
     * @param field the field
     * @return the field method names
     */
    public FieldMethodNames getFieldNames(MergedField field) {
        return contractProvider().getMethodNames(field);
    }

    /**
     * Check if a field should have a has method according to its contract.
     *
     * @param field the field
     * @return true if has method should be generated
     */
    public boolean shouldGenerateHasMethod(MergedField field) {
        return getContractFor(field).unified().hasMethodExists();
    }

    /**
     * Check if a field's getter should use has-check pattern.
     *
     * @param field the field
     * @return true if getter should use has-check
     */
    public boolean shouldUseHasCheckInGetter(MergedField field) {
        return getContractFor(field).unified().getterUsesHasCheck();
    }
}
