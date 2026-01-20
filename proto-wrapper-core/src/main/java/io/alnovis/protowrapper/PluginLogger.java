package io.alnovis.protowrapper;

import java.util.function.Consumer;

/**
 * Logger interface for proto-wrapper.
 * Provides different log levels and can be adapted to various logging backends.
 *
 * <p>This interface is build-tool agnostic. For Maven-specific logging,
 * see {@code MavenLogger} in the proto-wrapper-maven-plugin module.</p>
 */
public interface PluginLogger {

    /**
     * Log informational message.
     *
     * @param message the message to log
     */
    void info(String message);

    /**
     * Log warning message.
     *
     * @param message the message to log
     */
    void warn(String message);

    /**
     * Log debug message.
     *
     * @param message the message to log
     */
    void debug(String message);

    /**
     * Log error message.
     *
     * @param message the message to log
     */
    void error(String message);

    /**
     * Creates a logger that outputs to System.out/System.err.
     * Useful for standalone usage and testing.
     *
     * @return a console-based logger
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
     *
     * @param consumer the consumer to receive log messages
     * @return a consumer-based logger
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
     *
     * @return a no-op logger
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

}
