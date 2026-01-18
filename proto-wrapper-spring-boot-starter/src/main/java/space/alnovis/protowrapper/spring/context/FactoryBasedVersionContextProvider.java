package space.alnovis.protowrapper.spring.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.alnovis.protowrapper.spring.web.VersionNotSupportedException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * VersionContextProvider implementation that uses generated VersionContext interface.
 *
 * <p>This provider delegates to the generated VersionContext interface,
 * which provides compile-time type safety for VersionContext access.
 *
 * <p>While this implementation uses reflection to invoke static methods
 * (since the interface is generated and unknown at compile time), the interface
 * itself is type-safe and statically initialized at class load time.
 *
 * <p>Expects generated VersionContext interface at:
 * {@code {basePackage}.api.VersionContext}
 */
public class FactoryBasedVersionContextProvider implements VersionContextProvider {

    private static final Logger log = LoggerFactory.getLogger(FactoryBasedVersionContextProvider.class);

    private final Class<?> versionContextClass;
    private final Method forVersionIdMethod;
    private final Method findMethod;
    private final Method getDefaultMethod;
    private final Method supportedVersionsMethod;
    private final Method defaultVersionMethod;
    private final Method isSupportedMethod;

    // Cached values from VersionContext
    private final List<String> supportedVersions;
    private final String defaultVersion;

    /**
     * Creates a new FactoryBasedVersionContextProvider.
     *
     * @param basePackage base package for generated classes (without .api suffix)
     */
    public FactoryBasedVersionContextProvider(String basePackage) {
        String versionContextClassName = basePackage + ".api.VersionContext";

        try {
            this.versionContextClass = Class.forName(versionContextClassName);

            // Cache method references
            this.forVersionIdMethod = versionContextClass.getMethod("forVersionId", String.class);
            this.findMethod = versionContextClass.getMethod("find", String.class);
            this.getDefaultMethod = versionContextClass.getMethod("getDefault");
            this.supportedVersionsMethod = versionContextClass.getMethod("supportedVersions");
            this.defaultVersionMethod = versionContextClass.getMethod("defaultVersion");
            this.isSupportedMethod = versionContextClass.getMethod("isSupported", String.class);

            // Cache static values
            @SuppressWarnings("unchecked")
            List<String> versions = (List<String>) supportedVersionsMethod.invoke(null);
            this.supportedVersions = versions;
            this.defaultVersion = (String) defaultVersionMethod.invoke(null);

            log.info("Initialized FactoryBasedVersionContextProvider using {} with versions: {}, default: {}",
                    versionContextClassName, supportedVersions, defaultVersion);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "VersionContext not found: " + versionContextClassName +
                            ". Ensure proto-wrapper plugin has generated wrapper classes.", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "VersionContext missing expected method: " + versionContextClassName, e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to initialize VersionContext: " + versionContextClassName, e);
        }
    }

    @Override
    public Object getContext(String version) {
        if (version == null || !isSupported(version)) {
            throw new VersionNotSupportedException(version, supportedVersions);
        }
        try {
            return forVersionIdMethod.invoke(null, version);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get VersionContext for: " + version, e);
        }
    }

    @Override
    public Optional<Object> findContext(String version) {
        if (version == null || !isSupported(version)) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Optional<Object> result = (Optional<Object>) findMethod.invoke(null, version);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to find VersionContext for: " + version, e);
        }
    }

    @Override
    public Object getDefaultContext() {
        try {
            return getDefaultMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get default VersionContext", e);
        }
    }

    @Override
    public List<String> getSupportedVersions() {
        return supportedVersions;
    }

    @Override
    public String getDefaultVersion() {
        return defaultVersion;
    }

    @Override
    public boolean isSupported(String version) {
        return version != null && supportedVersions.contains(version);
    }
}
