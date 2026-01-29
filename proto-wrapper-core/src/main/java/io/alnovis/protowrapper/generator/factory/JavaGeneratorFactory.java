package io.alnovis.protowrapper.generator.factory;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.generator.*;
import io.alnovis.protowrapper.generator.wellknown.StructConverterGenerator;

import java.util.List;

/**
 * Default factory implementation for Java code generation.
 *
 * <p>This factory creates all standard Java generators used by the
 * proto-wrapper plugin. It serves as the default implementation when
 * no specific language is configured.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * GeneratorFactory factory = JavaGeneratorFactory.INSTANCE;
 * InterfaceGenerator generator = factory.createInterfaceGenerator(config);
 * }</pre>
 *
 * @since 2.4.0
 * @see GeneratorFactory
 */
public final class JavaGeneratorFactory implements GeneratorFactory {

    /**
     * Language identifier for Java.
     */
    public static final String LANGUAGE_ID = "java";

    /**
     * Singleton instance.
     */
    public static final JavaGeneratorFactory INSTANCE = new JavaGeneratorFactory();

    private JavaGeneratorFactory() {
        // Singleton - use INSTANCE
    }

    @Override
    public String getLanguageId() {
        return LANGUAGE_ID;
    }

    @Override
    public EnumGenerator createEnumGenerator(GeneratorConfig config) {
        return new EnumGenerator(config);
    }

    @Override
    public ConflictEnumGenerator createConflictEnumGenerator(GeneratorConfig config) {
        return new ConflictEnumGenerator(config);
    }

    @Override
    public ProtoWrapperGenerator createProtoWrapperGenerator(GeneratorConfig config, List<String> versions) {
        return new ProtoWrapperGenerator(config, versions);
    }

    @Override
    public InterfaceGenerator createInterfaceGenerator(GeneratorConfig config) {
        return new InterfaceGenerator(config);
    }

    @Override
    public AbstractClassGenerator createAbstractClassGenerator(GeneratorConfig config) {
        return new AbstractClassGenerator(config);
    }

    @Override
    public ImplClassGenerator createImplClassGenerator(GeneratorConfig config) {
        return new ImplClassGenerator(config);
    }

    @Override
    public VersionContextGenerator createVersionContextGenerator(GeneratorConfig config) {
        return new VersionContextGenerator(config);
    }

    @Override
    public ProtocolVersionsGenerator createProtocolVersionsGenerator(GeneratorConfig config, List<String> versions) {
        return new ProtocolVersionsGenerator(config, versions);
    }

    @Override
    public JavaFile generateStructConverter(String apiPackage) {
        return StructConverterGenerator.generate(apiPackage);
    }
}
