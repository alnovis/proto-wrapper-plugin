package space.alnovis.protowrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides access to plugin version information.
 *
 * <p>Version is read from {@code version.properties} resource file,
 * which is populated by Maven resource filtering during build.</p>
 *
 * <p>This class uses lazy initialization and caches the version
 * for subsequent calls.</p>
 */
public final class PluginVersion {

    private static final String VERSION_PROPERTIES = "version.properties";
    private static final String VERSION_KEY = "plugin.version";
    private static final String UNKNOWN_VERSION = "unknown";

    private static volatile String cachedVersion;

    private PluginVersion() {
        // Utility class
    }

    /**
     * Get the plugin version.
     *
     * <p>The version is read from the {@code version.properties} resource file.
     * If the file is not found or cannot be read, returns "unknown".</p>
     *
     * @return plugin version string (e.g., "1.6.0")
     */
    public static String get() {
        String version = cachedVersion;
        if (version == null) {
            synchronized (PluginVersion.class) {
                version = cachedVersion;
                if (version == null) {
                    version = loadVersion();
                    cachedVersion = version;
                }
            }
        }
        return version;
    }

    /**
     * Check if the version was successfully loaded.
     *
     * @return true if version is known, false if it's "unknown"
     */
    public static boolean isKnown() {
        return !UNKNOWN_VERSION.equals(get());
    }

    /**
     * Clear the cached version.
     * Primarily for testing purposes.
     */
    static void clearCache() {
        cachedVersion = null;
    }

    private static String loadVersion() {
        try (InputStream is = PluginVersion.class.getClassLoader()
                .getResourceAsStream(VERSION_PROPERTIES)) {
            if (is == null) {
                return UNKNOWN_VERSION;
            }
            Properties props = new Properties();
            props.load(is);
            String version = props.getProperty(VERSION_KEY);
            if (version == null || version.isBlank() || version.startsWith("${")) {
                // Not filtered (running from IDE without Maven build)
                return UNKNOWN_VERSION;
            }
            return version.trim();
        } catch (IOException e) {
            return UNKNOWN_VERSION;
        }
    }
}
