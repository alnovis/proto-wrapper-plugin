package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import io.alnovis.protowrapper.generator.GenerationContext;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.generator.TypeResolver;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

/**
 * Context for generating AbstractBuilder classes.
 *
 * <p>This record captures all the information needed to generate an AbstractBuilder class,
 * unifying the parameters between top-level and nested abstract builder generation.</p>
 */
public record AbstractBuilderContext(
        MergedMessage message,
        ClassName interfaceType,
        TypeResolver resolver,
        GenerationContext genCtx,
        GeneratorConfig config,
        boolean isTopLevel
) {
    /**
     * Create context for a top-level abstract builder.
     *
     * @param message the merged message to generate builder for
     * @param interfaceType the interface type name
     * @param resolver the type resolver
     * @param genCtx the generation context
     * @param config the generator configuration
     * @return a new context for top-level builder generation
     */
    public static AbstractBuilderContext forTopLevel(
            MergedMessage message,
            ClassName interfaceType,
            TypeResolver resolver,
            GenerationContext genCtx,
            GeneratorConfig config) {

        return new AbstractBuilderContext(message, interfaceType, resolver, genCtx, config, true);
    }

    /**
     * Create context for a nested abstract builder.
     *
     * @param nested the nested merged message to generate builder for
     * @param interfaceType the interface type name
     * @param resolver the type resolver
     * @param genCtx the generation context
     * @param config the generator configuration
     * @return a new context for nested builder generation
     */
    public static AbstractBuilderContext forNested(
            MergedMessage nested,
            ClassName interfaceType,
            TypeResolver resolver,
            GenerationContext genCtx,
            GeneratorConfig config) {

        return new AbstractBuilderContext(nested, interfaceType, resolver, genCtx, config, false);
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
     * Capitalize a field name.
     *
     * @param name the name to capitalize
     * @return the capitalized name
     */
    public String capitalize(String name) {
        return resolver.capitalize(name);
    }

    /**
     * Get the Builder nested interface type.
     *
     * @return the builder interface class name
     */
    public ClassName builderInterfaceType() {
        return interfaceType.nestedClass("Builder");
    }

    /**
     * Get the API package from configuration.
     *
     * @return the API package name
     */
    public String apiPackage() {
        return config.getApiPackage();
    }
}
