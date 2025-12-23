package space.alnovis.protowrapper.gradle

import org.gradle.api.logging.Logger
import space.alnovis.protowrapper.PluginLogger

/**
 * Gradle-specific logger adapter.
 *
 * <p>Wraps Gradle's {@link Logger} interface to implement {@link PluginLogger},
 * enabling the proto-wrapper-core library to log messages through Gradle's
 * logging infrastructure.</p>
 *
 * <p>Usage in Gradle tasks:</p>
 * <pre>
 * val pluginLogger = GradleLogger(logger)
 * val merger = VersionMerger(pluginLogger)
 * </pre>
 *
 * <p>Log level mapping:</p>
 * <ul>
 *   <li>info() -> Logger.lifecycle() (always visible)</li>
 *   <li>warn() -> Logger.warn()</li>
 *   <li>debug() -> Logger.debug() (visible with --debug flag)</li>
 *   <li>error() -> Logger.error()</li>
 * </ul>
 *
 * @property gradleLogger The underlying Gradle Logger instance
 * @see PluginLogger
 */
class GradleLogger(private val gradleLogger: Logger) : PluginLogger {

    /**
     * Logs an informational message.
     *
     * <p>Uses Gradle's lifecycle level to ensure visibility during normal builds.
     * These messages are displayed without requiring verbose flags.</p>
     *
     * @param message The message to log
     */
    override fun info(message: String) {
        gradleLogger.lifecycle(message)
    }

    /**
     * Logs a warning message.
     *
     * <p>Warning messages are displayed in builds and typically formatted
     * with a "WARNING:" prefix by Gradle.</p>
     *
     * @param message The warning message to log
     */
    override fun warn(message: String) {
        gradleLogger.warn(message)
    }

    /**
     * Logs a debug message.
     *
     * <p>Debug messages are only visible when running Gradle with the --debug
     * or --info flag. Use for detailed diagnostic information.</p>
     *
     * @param message The debug message to log
     */
    override fun debug(message: String) {
        gradleLogger.debug(message)
    }

    /**
     * Logs an error message.
     *
     * <p>Error messages are always visible and typically formatted with
     * an "ERROR:" prefix by Gradle.</p>
     *
     * @param message The error message to log
     */
    override fun error(message: String) {
        gradleLogger.error(message)
    }
}
