package space.alnovis.protowrapper;

import java.util.function.Consumer;

/**
 * Logger interface for proto-wrapper-plugin.
 * Provides different log levels and can be adapted to various logging backends.
 */
public interface PluginLogger {

    /**
     * Log informational message.
     */
    void info(String message);

    /**
     * Log warning message.
     */
    void warn(String message);

    /**
     * Log debug message.
     */
    void debug(String message);

    /**
     * Log error message.
     */
    void error(String message);

    /**
     * Creates a logger that outputs to System.out/System.err.
     * Useful for standalone usage and testing.
     */
    static PluginLogger console() {
        return new PluginLogger() {
            @Override
            public void info(String message) {
                System.out.println("[INFO] " + message);
            }

            @Override
            public void warn(String message) {
                System.err.println("[WARN] " + message);
            }

            @Override
            public void debug(String message) {
                System.out.println("[DEBUG] " + message);
            }

            @Override
            public void error(String message) {
                System.err.println("[ERROR] " + message);
            }
        };
    }

    /**
     * Creates a logger from a simple Consumer (for backward compatibility).
     * All messages go to the same consumer.
     */
    static PluginLogger fromConsumer(Consumer<String> consumer) {
        return new PluginLogger() {
            @Override
            public void info(String message) {
                consumer.accept(message);
            }

            @Override
            public void warn(String message) {
                consumer.accept("Warning: " + message);
            }

            @Override
            public void debug(String message) {
                consumer.accept(message);
            }

            @Override
            public void error(String message) {
                consumer.accept("ERROR: " + message);
            }
        };
    }

    /**
     * Creates a no-op logger that discards all messages.
     * Useful for testing or when logging is not needed.
     */
    static PluginLogger noop() {
        return new PluginLogger() {
            @Override
            public void info(String message) {}

            @Override
            public void warn(String message) {}

            @Override
            public void debug(String message) {}

            @Override
            public void error(String message) {}
        };
    }

    /**
     * Creates a logger that delegates to Maven's Log interface.
     * Usage in Maven plugins:
     * <pre>
     * PluginLogger logger = PluginLogger.maven(getLog());
     * </pre>
     *
     * @param mavenLog Maven's Log instance (org.apache.maven.plugin.logging.Log)
     * @return PluginLogger that delegates to Maven Log
     */
    static PluginLogger maven(org.apache.maven.plugin.logging.Log mavenLog) {
        return new PluginLogger() {
            @Override
            public void info(String message) {
                mavenLog.info(message);
            }

            @Override
            public void warn(String message) {
                mavenLog.warn(message);
            }

            @Override
            public void debug(String message) {
                mavenLog.debug(message);
            }

            @Override
            public void error(String message) {
                mavenLog.error(message);
            }
        };
    }
}
