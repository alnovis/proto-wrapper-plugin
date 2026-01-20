package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;

import static io.alnovis.protowrapper.generator.ProtobufConstants.GENERATED_FILE_COMMENT;

/**
 * Composer that builds VersionContext interface using component pattern.
 *
 * <p>This class coordinates multiple {@link InterfaceComponent} instances to generate
 * the complete VersionContext interface. Each component is responsible for a specific
 * aspect of the interface, following the Single Responsibility Principle.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
 *     .addStaticFields()
 *     .addStaticMethods()
 *     .addInstanceMethods()
 *     .addWrapMethods()
 *     .addBuilderMethods()
 *     .addConvenienceMethods()
 *     .build();
 * }</pre>
 */
public class VersionContextInterfaceComposer {

    private final GeneratorConfig config;
    private final MergedSchema schema;
    private final JavaVersionCodegen codegen;
    private final TypeSpec.Builder builder;

    /**
     * Create a new composer.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public VersionContextInterfaceComposer(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
        this.codegen = config.isJava8Compatible() ? Java8Codegen.INSTANCE : Java9PlusCodegen.INSTANCE;
        this.builder = createInterfaceBuilder();
    }

    private TypeSpec.Builder createInterfaceBuilder() {
        return TypeSpec.interfaceBuilder("VersionContext")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Version context for creating version-specific wrapper instances.\n\n")
                .addJavadoc("<p>Provides factory methods for obtaining version contexts and creating wrapper types.</p>\n\n")
                .addJavadoc("<p>Usage:</p>\n")
                .addJavadoc("<pre>{@code\n")
                .addJavadoc("VersionContext ctx = VersionContext.forVersionId(\"v2\");\n")
                .addJavadoc("Order order = ctx.wrapOrder(protoMessage);\n")
                .addJavadoc("}</pre>\n");
    }

    /**
     * Add static fields (CONTEXTS, SUPPORTED_VERSIONS, DEFAULT_VERSION).
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addStaticFields() {
        new StaticFieldsComponent(codegen, config, schema).addTo(builder);
        return this;
    }

    /**
     * Add static factory methods (forVersionId, find, getDefault, etc.).
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addStaticMethods() {
        new StaticMethodsComponent(config, schema).addTo(builder);
        return this;
    }

    /**
     * Add abstract instance methods (getVersionId, getVersion).
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addInstanceMethods() {
        new InstanceMethodsComponent(config, schema).addTo(builder);
        return this;
    }

    /**
     * Add wrap and parse methods for each message type.
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addWrapMethods() {
        new WrapMethodsComponent(config, schema).addTo(builder);
        return this;
    }

    /**
     * Add builder factory methods (newXxxBuilder, etc.).
     *
     * <p>Only adds methods if builders are enabled in configuration.</p>
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addBuilderMethods() {
        new BuilderMethodsComponent(config, schema).addTo(builder);
        return this;
    }

    /**
     * Add convenience methods (zeroMoney, createMoney, etc.).
     *
     * <p>Only adds methods if builders are enabled and applicable
     * message types exist.</p>
     *
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addConvenienceMethods() {
        new ConvenienceMethodsComponent(config, schema).addTo(builder);
        return this;
    }

    /**
     * Add a custom component.
     *
     * @param component the component to add
     * @return this composer for chaining
     */
    public VersionContextInterfaceComposer addComponent(InterfaceComponent component) {
        component.addTo(builder);
        return this;
    }

    /**
     * Check if this composition requires a helper class (Java 8).
     *
     * @return true if helper class is needed
     */
    public boolean requiresHelperClass() {
        return codegen.requiresHelperClass();
    }

    /**
     * Build the complete JavaFile.
     *
     * @return the generated JavaFile
     */
    public JavaFile build() {
        return JavaFile.builder(config.getApiPackage(), builder.build())
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }
}
