package io.alnovis.protowrapper.mojo;

import org.apache.maven.plugin.logging.Log;
import io.alnovis.protowrapper.PluginLogger;

/**
 * Maven-specific logger adapter.
 * Wraps Maven's {@link Log} interface to implement {@link PluginLogger}.
 *
 * <p>Usage in Maven plugins:</p>
 * <pre>
 * PluginLogger logger = MavenLogger.from(getLog());
 * </pre>
 */
public final class MavenLogger {

    private MavenLogger() {
        // Utility class
    }

    /**
     * Creates a PluginLogger that delegates to Maven's Log interface.
     *
     * @param mavenLog Maven's Log instance
     * @return PluginLogger that delegates to Maven Log
     */
    public static PluginLogger from(Log mavenLog) {
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
