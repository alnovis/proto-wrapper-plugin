package io.alnovis.protowrapper.generator.factory;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for GeneratorFactory implementations.
 *
 * <p>Provides factory lookup by language identifier with support for:</p>
 * <ul>
 *   <li>Built-in Java factory (always available)</li>
 *   <li>Programmatic registration via {@link #register(GeneratorFactory)}</li>
 *   <li>Automatic discovery via Java SPI (ServiceLoader)</li>
 * </ul>
 *
 * <h2>SPI Discovery</h2>
 * <p>Custom factories are automatically loaded from the classpath using
 * {@link ServiceLoader}. To register a custom factory:</p>
 * <ol>
 *   <li>Implement {@link GeneratorFactory}</li>
 *   <li>Create file: {@code META-INF/services/io.alnovis.protowrapper.generator.factory.GeneratorFactory}</li>
 *   <li>Add fully qualified class name to the file</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get default (Java) factory
 * GeneratorFactory factory = GeneratorFactoryRegistry.get("java");
 *
 * // Get Kotlin factory (if registered)
 * GeneratorFactory kotlinFactory = GeneratorFactoryRegistry.get("kotlin");
 *
 * // Get with fallback
 * GeneratorFactory factory = GeneratorFactoryRegistry.getOrDefault("kotlin");
 * }</pre>
 *
 * @since 2.4.0
 * @see GeneratorFactory
 * @see JavaGeneratorFactory
 */
public final class GeneratorFactoryRegistry {

    private static final Map<String, GeneratorFactory> FACTORIES = new ConcurrentHashMap<>();

    static {
        // Register default Java factory
        register(JavaGeneratorFactory.INSTANCE);

        // Load additional factories via SPI
        loadFromServiceLoader();
    }

    private GeneratorFactoryRegistry() {
        // Utility class - no instantiation
    }

    /**
     * Load factories from ServiceLoader.
     *
     * <p>Called automatically during class initialization.
     * Can be called again to reload factories if classpath changes.</p>
     *
     * <p>Errors during individual factory loading are caught and logged,
     * allowing other factories to be loaded successfully.</p>
     */
    public static void loadFromServiceLoader() {
        try {
            ServiceLoader<GeneratorFactory> loader = ServiceLoader.load(GeneratorFactory.class);
            for (GeneratorFactory factory : loader) {
                try {
                    register(factory);
                } catch (Exception e) {
                    // Log but don't fail - allow other factories to load
                    System.err.println("Failed to register factory: " + factory.getClass().getName() +
                            " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // ServiceLoader itself failed - this is non-fatal, we have the default factory
            System.err.println("ServiceLoader failed to load GeneratorFactory implementations: " + e.getMessage());
        }
    }

    /**
     * Register a factory implementation.
     *
     * <p>If a factory with the same language ID is already registered,
     * it will be replaced.</p>
     *
     * @param factory the factory to register
     * @throws NullPointerException if factory or its language ID is null
     */
    public static void register(GeneratorFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory cannot be null");
        }
        String languageId = factory.getLanguageId();
        if (languageId == null) {
            throw new NullPointerException("factory.getLanguageId() cannot return null");
        }
        FACTORIES.put(languageId.toLowerCase(), factory);
    }

    /**
     * Get a factory by language identifier.
     *
     * @param languageId the language identifier (case-insensitive)
     * @return Optional containing the factory, or empty if not found
     */
    public static Optional<GeneratorFactory> find(String languageId) {
        if (languageId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(FACTORIES.get(languageId.toLowerCase()));
    }

    /**
     * Get a factory by language identifier.
     *
     * @param languageId the language identifier (case-insensitive)
     * @return the factory
     * @throws IllegalArgumentException if no factory found for the language
     */
    public static GeneratorFactory get(String languageId) {
        return find(languageId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No GeneratorFactory registered for language: " + languageId +
                        ". Available: " + getRegisteredLanguages()));
    }

    /**
     * Get a factory by language identifier, with fallback to Java.
     *
     * @param languageId the language identifier (case-insensitive), may be null
     * @return the factory for the specified language, or Java factory as fallback
     */
    public static GeneratorFactory getOrDefault(String languageId) {
        if (languageId == null || languageId.isEmpty()) {
            return JavaGeneratorFactory.INSTANCE;
        }
        return find(languageId).orElse(JavaGeneratorFactory.INSTANCE);
    }

    /**
     * Check if a factory is registered for the given language.
     *
     * @param languageId the language identifier (case-insensitive)
     * @return true if a factory is registered
     */
    public static boolean isRegistered(String languageId) {
        return languageId != null && FACTORIES.containsKey(languageId.toLowerCase());
    }

    /**
     * Get all registered language identifiers.
     *
     * @return set of registered language IDs
     */
    public static Set<String> getRegisteredLanguages() {
        return Set.copyOf(FACTORIES.keySet());
    }

    /**
     * Get the default factory (Java).
     *
     * @return the Java generator factory
     */
    public static GeneratorFactory getDefault() {
        return JavaGeneratorFactory.INSTANCE;
    }
}
