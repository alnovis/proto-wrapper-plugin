package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import space.alnovis.protowrapper.generator.GenerationContext;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

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
     */
    public static AbstractBuilderContext forNested(
            MergedMessage nested,
            ClassName interfaceType,
            TypeResolver resolver,
            GenerationContext genCtx,
            GeneratorConfig config) {

        return new AbstractBuilderContext(nested, interfaceType, resolver, genCtx, config, false);
    }

    // Convenience methods
    public MergedSchema schema() {
        return genCtx.getSchema();
    }

    public String capitalize(String name) {
        return resolver.capitalize(name);
    }

    public ClassName builderInterfaceType() {
        return interfaceType.nestedClass("Builder");
    }

    public String apiPackage() {
        return config.getApiPackage();
    }
}
