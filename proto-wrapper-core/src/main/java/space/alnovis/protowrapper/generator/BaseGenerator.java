package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;

/**
 * Abstract base class for code generators.
 *
 * <p>Provides common functionality shared across all generators:</p>
 * <ul>
 *   <li>Configuration management</li>
 *   <li>File writing</li>
 * </ul>
 *
 * <p>Subclasses implement the specific generation logic for their target
 * (interfaces, abstract classes, implementations, enums, etc.)</p>
 *
 * @param <T> The input type for generation
 */
public abstract class BaseGenerator<T> implements CodeGenerator<T> {

    /** The generator configuration. */
    protected final GeneratorConfig config;

    /**
     * Create a new generator with the specified configuration.
     *
     * @param config Generator configuration
     */
    protected BaseGenerator(GeneratorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        this.config = config;
    }

    @Override
    public GeneratorConfig getConfig() {
        return config;
    }

    @Override
    public void writeToFile(JavaFile javaFile) throws IOException {
        javaFile.writeTo(config.getOutputDirectory());
    }

    /**
     * Get the API package from configuration.
     * Convenience method for subclasses.
     *
     * @return API package name
     */
    protected String getApiPackage() {
        return config.getApiPackage();
    }

    /**
     * Get the implementation package for a specific version.
     * Convenience method for subclasses.
     *
     * @param version Version string
     * @return Implementation package name
     */
    protected String getImplPackage(String version) {
        return config.getImplPackage(version);
    }
}
