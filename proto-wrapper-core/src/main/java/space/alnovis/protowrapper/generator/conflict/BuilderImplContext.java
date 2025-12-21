package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import space.alnovis.protowrapper.generator.GenerationContext;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

/**
 * Context for generating BuilderImpl classes.
 *
 * <p>This record captures all the information needed to generate a BuilderImpl class,
 * unifying the parameters between top-level and nested builder generation.</p>
 */
public record BuilderImplContext(
        MergedMessage message,
        ClassName protoType,
        ClassName protoBuilderType,
        ClassName abstractBuilderType,
        ClassName interfaceType,
        String implClassName,
        GenerationContext genCtx,
        GeneratorConfig config
) {
    /**
     * Create context for a top-level builder impl.
     */
    public static BuilderImplContext forTopLevel(
            MergedMessage message,
            ClassName protoType,
            String implClassName,
            GenerationContext genCtx,
            GeneratorConfig config) {

        ClassName interfaceType = ClassName.get(genCtx.getApiPackage(), message.getInterfaceName());
        ClassName abstractClass = ClassName.get(
                genCtx.getApiPackage() + ".impl",
                message.getAbstractClassName());
        ClassName abstractBuilderType = abstractClass.nestedClass("AbstractBuilder");
        ClassName protoBuilderType = protoType.nestedClass("Builder");

        return new BuilderImplContext(
                message, protoType, protoBuilderType, abstractBuilderType,
                interfaceType, implClassName, genCtx, config);
    }

    /**
     * Create context for a nested builder impl.
     */
    public static BuilderImplContext forNested(
            MergedMessage nested,
            ClassName protoType,
            ClassName abstractBuilderType,
            String implClassName,
            GenerationContext genCtx,
            GeneratorConfig config) {

        TypeResolver resolver = genCtx.getTypeResolver();
        ClassName interfaceType = resolver.buildNestedClassName(nested.getQualifiedInterfaceName());
        ClassName protoBuilderType = protoType.nestedClass("Builder");

        return new BuilderImplContext(
                nested, protoType, protoBuilderType, abstractBuilderType,
                interfaceType, implClassName, genCtx, config);
    }

    // Convenience methods
    public String version() {
        return genCtx.requireVersion();
    }

    public TypeResolver resolver() {
        return genCtx.getTypeResolver();
    }

    public MergedSchema schema() {
        return genCtx.getSchema();
    }

    public String capitalize(String name) {
        return resolver().capitalize(name);
    }

    public ClassName builderInterfaceType() {
        return interfaceType.nestedClass("Builder");
    }
}
