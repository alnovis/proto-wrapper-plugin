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
    private static final String PROTOBUF_VERSION_KEY = "protobuf.version";
    private static final String UNKNOWN_VERSION = "unknown";

    private static volatile String cachedVersion;
    private static volatile String cachedProtobufVersion;

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
     * Get the protobuf version that the plugin was built with.
     *
     * <p>This version is used as the default for embedded protoc downloads.</p>
     *
     * @return protobuf version string (e.g., "4.28.2")
     */
    public static String getProtobufVersion() {
        String version = cachedProtobufVersion;
        if (version == null) {
            synchronized (PluginVersion.class) {
                version = cachedProtobufVersion;
                if (version == null) {
                    version = loadProperty(PROTOBUF_VERSION_KEY);
                    cachedProtobufVersion = version;
                }
            }
        }
        return version;
    }

    /**
     * Clear the cached versions.
     * Primarily for testing purposes.
     */
    static void clearCache() {
        cachedVersion = null;
        cachedProtobufVersion = null;
    }

    private static String loadVersion() {
        return loadProperty(VERSION_KEY);
    }

    private static String loadProperty(String key) {
        try (InputStream is = PluginVersion.class.getClassLoader()
                .getResourceAsStream(VERSION_PROPERTIES)) {
            if (is == null) {
                return UNKNOWN_VERSION;
            }
            Properties props = new Properties();
            props.load(is);
            String value = props.getProperty(key);
            if (value == null || value.isBlank() || value.startsWith("${")) {
                // Not filtered (running from IDE without Maven build)
                return UNKNOWN_VERSION;
            }
            return value.trim();
        } catch (IOException e) {
            return UNKNOWN_VERSION;
        }
    }
}
