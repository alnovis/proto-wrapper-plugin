package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import space.alnovis.protowrapper.generator.GenerationContext;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
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
     */
    public static ProcessingContext forImpl(MergedMessage message, ClassName protoType,
                                             GenerationContext genCtx, GeneratorConfig config) {
        return new ProcessingContext(message, protoType, genCtx, config);
    }

    /**
     * Create a context for abstract class generation.
     */
    public static ProcessingContext forAbstract(MergedMessage message, TypeVariableName protoType,
                                                 GenerationContext genCtx, GeneratorConfig config) {
        return new ProcessingContext(message, protoType, genCtx, config);
    }

    /**
     * Get the type resolver from the generation context.
     */
    public TypeResolver resolver() {
        return genCtx.getTypeResolver();
    }

    /**
     * Get the merged schema from the generation context.
     */
    public MergedSchema schema() {
        return genCtx.getSchema();
    }

    /**
     * Get the current version (if set).
     */
    public String version() {
        return genCtx.getCurrentVersion();
    }

    /**
     * Get the current version, throwing if not set.
     */
    public String requireVersion() {
        return genCtx.requireVersion();
    }

    /**
     * Get the API package name.
     */
    public String apiPackage() {
        return genCtx.getApiPackage();
    }

    /**
     * Get the implementation package name.
     */
    public String implPackage() {
        return genCtx.getImplPackage();
    }

    /**
     * Get the version number for the current version.
     */
    public int versionNumber() {
        return genCtx.getVersionNumber();
    }

    /**
     * Check if the proto type is a concrete ClassName (for impl classes).
     */
    public boolean isConcreteType() {
        return protoType instanceof ClassName;
    }

    /**
     * Get the proto type as a ClassName.
     * @throws ClassCastException if protoType is not a ClassName
     */
    public ClassName protoClassName() {
        return (ClassName) protoType;
    }

    /**
     * Get the proto type as a TypeVariableName.
     * @throws ClassCastException if protoType is not a TypeVariableName
     */
    public TypeVariableName protoTypeVariable() {
        return (TypeVariableName) protoType;
    }

    /**
     * Capitalize a string (first letter uppercase).
     */
    public String capitalize(String s) {
        return resolver().capitalize(s);
    }

    /**
     * Parse the field type using the resolver.
     */
    public TypeName parseFieldType(space.alnovis.protowrapper.model.MergedField field) {
        return resolver().parseFieldType(field, message);
    }

    /**
     * Get the implementation class name for a message.
     */
    public String getImplClassName(String messageName) {
        return genCtx.getImplClassName(messageName);
    }
}
