package io.alnovis.protowrapper.generator.factory;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.generator.*;

import java.util.List;

/**
 * Factory interface for creating code generators.
 *
 * <p>This interface provides an abstraction layer for generator creation,
 * enabling support for different target languages (Java, Kotlin, etc.)
 * without changing the orchestration logic.</p>
 *
 * <h2>Extension Points</h2>
 * <p>To add support for a new language:</p>
 * <ol>
 *   <li>Implement this interface (e.g., {@code KotlinGeneratorFactory})</li>
 *   <li>Register via {@link GeneratorFactoryRegistry} or SPI</li>
 *   <li>Configure the plugin with {@code language = "kotlin"}</li>
 * </ol>
 *
 * <h2>SPI Registration</h2>
 * <p>Custom factories can be auto-discovered via ServiceLoader by adding
 * a file {@code META-INF/services/io.alnovis.protowrapper.generator.factory.GeneratorFactory}
 * containing the fully qualified class name of your factory implementation.</p>
 *
 * @since 2.4.0
 * @see JavaGeneratorFactory
 * @see GeneratorFactoryRegistry
 */
public interface GeneratorFactory {

    /**
     * Get the language identifier for this factory.
     *
     * <p>Used for factory lookup and configuration. Common values:</p>
     * <ul>
     *   <li>{@code "java"} - Java code generation (default)</li>
     *   <li>{@code "kotlin"} - Kotlin code generation</li>
     * </ul>
     *
     * @return language identifier, never null
     */
    String getLanguageId();

    /**
     * Create an enum generator.
     *
     * @param config generator configuration
     * @return enum generator instance
     */
    EnumGenerator createEnumGenerator(GeneratorConfig config);

    /**
     * Create a conflict enum generator for INT_ENUM type conflicts.
     *
     * @param config generator configuration
     * @return conflict enum generator instance
     */
    ConflictEnumGenerator createConflictEnumGenerator(GeneratorConfig config);

    /**
     * Create a ProtoWrapper interface generator.
     *
     * @param config generator configuration
     * @param versions list of version identifiers
     * @return ProtoWrapper generator instance
     */
    ProtoWrapperGenerator createProtoWrapperGenerator(GeneratorConfig config, List<String> versions);

    /**
     * Create an interface generator.
     *
     * @param config generator configuration
     * @return interface generator instance
     */
    InterfaceGenerator createInterfaceGenerator(GeneratorConfig config);

    /**
     * Create an abstract class generator.
     *
     * @param config generator configuration
     * @return abstract class generator instance
     */
    AbstractClassGenerator createAbstractClassGenerator(GeneratorConfig config);

    /**
     * Create an implementation class generator.
     *
     * @param config generator configuration
     * @return impl class generator instance
     */
    ImplClassGenerator createImplClassGenerator(GeneratorConfig config);

    /**
     * Create a VersionContext generator.
     *
     * @param config generator configuration
     * @return VersionContext generator instance
     */
    VersionContextGenerator createVersionContextGenerator(GeneratorConfig config);

    /**
     * Create a ProtocolVersions utility class generator.
     *
     * @param config generator configuration
     * @param versions list of version identifiers
     * @return ProtocolVersions generator instance
     */
    ProtocolVersionsGenerator createProtocolVersionsGenerator(GeneratorConfig config, List<String> versions);

    /**
     * Generate StructConverter utility class if needed.
     *
     * <p>This is a special case as StructConverter is language-independent
     * and the current implementation uses a static method. Factories can
     * override this to provide language-specific implementations.</p>
     *
     * @param apiPackage the API package for the generated class
     * @return generated JavaFile (or equivalent for other languages)
     */
    default JavaFile generateStructConverter(String apiPackage) {
        return io.alnovis.protowrapper.generator.wellknown.StructConverterGenerator.generate(apiPackage);
    }
}
