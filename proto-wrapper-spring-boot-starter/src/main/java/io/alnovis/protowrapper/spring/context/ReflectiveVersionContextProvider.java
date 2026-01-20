package io.alnovis.protowrapper.spring.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.alnovis.protowrapper.spring.web.VersionNotSupportedException;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VersionContextProvider implementation that discovers and instantiates
 * VersionContext classes via reflection.
 *
 * <p>Expects generated VersionContext classes to follow naming convention:
 * {@code {basePackage}.{version}.VersionContext{VersionSuffix}}
 *
 * <p>Example: com.example.model.api.v2.VersionContextV2
 */
public class ReflectiveVersionContextProvider implements VersionContextProvider {

    private static final Logger log = LoggerFactory.getLogger(ReflectiveVersionContextProvider.class);

    private final String basePackage;
    private final List<String> versions;
    private final String defaultVersion;
    private final Map<String, Object> contextCache = new ConcurrentHashMap<>();

    /**
     * Creates a new ReflectiveVersionContextProvider.
     *
     * @param basePackage base package for generated classes
     * @param versions list of supported versions
     * @param defaultVersion default version (or null to use first)
     */
    public ReflectiveVersionContextProvider(
            String basePackage,
            List<String> versions,
            String defaultVersion) {
        this.basePackage = basePackage;
        this.versions = List.copyOf(versions);
        this.defaultVersion = defaultVersion != null ? defaultVersion : versions.get(0);

        // Pre-load all contexts to fail fast on startup
        for (String version : versions) {
            loadContext(version);
        }

        log.info("Initialized VersionContextProvider with versions: {}, default: {}",
            versions, this.defaultVersion);
    }

    @Override
    public Object getContext(String version) {
        return findContext(version).orElseThrow(() ->
            new VersionNotSupportedException(version, versions));
    }

    @Override
    public Optional<Object> findContext(String version) {
        if (version == null || !versions.contains(version)) {
            return Optional.empty();
        }
        return Optional.of(contextCache.computeIfAbsent(version, this::loadContext));
    }

    @Override
    public Object getDefaultContext() {
        return getContext(defaultVersion);
    }

    @Override
    public List<String> getSupportedVersions() {
        return versions;
    }

    @Override
    public String getDefaultVersion() {
        return defaultVersion;
    }

    private Object loadContext(String version) {
        String className = resolveClassName(version);

        try {
            Class<?> clazz = Class.forName(className);

            // Try to get INSTANCE field first (singleton pattern)
            try {
                java.lang.reflect.Field instanceField = clazz.getField("INSTANCE");
                Object instance = instanceField.get(null);
                log.debug("Loaded VersionContext singleton for {}: {}", version, className);
                return instance;
            } catch (NoSuchFieldException e) {
                // Fall back to constructor if no INSTANCE field
                log.debug("No INSTANCE field found, using constructor for {}", className);
            }

            // Fallback: instantiate via constructor
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();

            log.debug("Loaded VersionContext for {}: {}", version, className);
            return instance;

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "VersionContext class not found: " + className +
                ". Ensure proto-wrapper plugin has generated wrapper classes.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Failed to instantiate VersionContext: " + className, e);
        }
    }

    /**
     * Resolves the fully qualified class name for a version.
     *
     * <p>Convention: {basePackage}.{version}.VersionContext{VersionSuffix}
     * <p>Example: v2 -> com.example.api.v2.VersionContextV2
     *
     * @param version version string (e.g., "v2")
     * @return fully qualified class name
     */
    private String resolveClassName(String version) {
        // Capitalize first letter for class name suffix
        String suffix = version.substring(0, 1).toUpperCase() + version.substring(1);

        return String.format("%s.%s.VersionContext%s", basePackage, version, suffix);
    }
}
