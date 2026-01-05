package space.alnovis.protowrapper.incremental;

import space.alnovis.protowrapper.PluginLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages incremental generation state across builds.
 *
 * <p>This is the main coordinator for incremental generation. It:
 * <ul>
 *   <li>Loads and saves incremental state</li>
 *   <li>Detects changes in proto files</li>
 *   <li>Computes affected messages based on dependencies</li>
 *   <li>Handles cache invalidation</li>
 * </ul>
 *
 * <p>Thread safety: This class uses file locking via {@link CacheLock}
 * to ensure safe concurrent access from multiple build processes.
 *
 * <p>Typical usage:
 * <pre>
 * IncrementalStateManager manager = new IncrementalStateManager(...);
 * manager.loadPreviousState();
 *
 * if (manager.shouldInvalidateCache()) {
 *     // Full regeneration needed
 *     manager.invalidateCache();
 * } else {
 *     Set&lt;String&gt; affected = manager.analyzeAndGetAffectedFiles(protoFiles);
 *     if (affected.isEmpty()) {
 *         // No changes, skip generation
 *     } else {
 *         // Regenerate only affected files
 *     }
 * }
 *
 * manager.saveCurrentState();
 * </pre>
 *
 * @see CacheLock
 */
public class IncrementalStateManager {

    private static final String STATE_FILE = "state.json";

    private final Path cacheDirectory;
    private final Path protoRoot;
    private final String pluginVersion;
    private final String configHash;
    private final PluginLogger logger;

    private IncrementalState previousState;
    private ChangeDetector.ChangeResult changeResult;
    private ProtoDependencyGraph dependencyGraph;
    private boolean stateLoaded = false;

    /**
     * Create a new incremental state manager.
     *
     * @param cacheDirectory directory for storing cache files
     * @param protoRoot root directory for proto files
     * @param pluginVersion current plugin version
     * @param configHash hash of current generation configuration
     * @param logger logger for messages
     */
    public IncrementalStateManager(
            Path cacheDirectory,
            Path protoRoot,
            String pluginVersion,
            String configHash,
            PluginLogger logger) {
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory must not be null");
        this.protoRoot = Objects.requireNonNull(protoRoot, "protoRoot must not be null");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion must not be null");
        this.configHash = Objects.requireNonNull(configHash, "configHash must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /**
     * Load previous state from cache.
     *
     * <p>This method acquires a file lock to ensure thread-safe access
     * when multiple builds run concurrently.
     *
     * @throws IOException if cache cannot be read or lock cannot be acquired
     */
    public void loadPreviousState() throws IOException {
        try (CacheLock lock = CacheLock.acquire(cacheDirectory)) {
            Path stateFile = cacheDirectory.resolve(STATE_FILE);
            previousState = IncrementalState.readFrom(stateFile);
            stateLoaded = true;

            if (!previousState.isEmpty()) {
                logger.info("Loaded incremental state from " + previousState.lastGeneration());
            } else {
                logger.info("No previous incremental state found");
            }
        }
    }

    /**
     * Check if cache should be fully invalidated.
     * This happens when plugin version or configuration changes.
     *
     * @return true if cache should be invalidated
     */
    public boolean shouldInvalidateCache() {
        ensureStateLoaded();
        return previousState.shouldInvalidate(pluginVersion, configHash);
    }

    /**
     * Get the reason for cache invalidation (for logging).
     *
     * @return reason string, or null if cache is valid
     */
    public String getInvalidationReason() {
        ensureStateLoaded();

        if (previousState.isEmpty()) {
            return "No previous state (first run or cache cleared)";
        }

        if (!Objects.equals(previousState.pluginVersion(), pluginVersion)) {
            return String.format("Plugin version changed: %s -> %s",
                previousState.pluginVersion(), pluginVersion);
        }

        if (!Objects.equals(previousState.configHash(), configHash)) {
            return "Configuration changed";
        }

        return null;
    }

    /**
     * Analyze proto files and detect changes.
     *
     * @param protoFiles current set of proto files (absolute paths)
     * @return change result
     * @throws IOException if files cannot be read
     */
    public ChangeDetector.ChangeResult analyzeChanges(Set<Path> protoFiles) throws IOException {
        ensureStateLoaded();
        Objects.requireNonNull(protoFiles, "protoFiles must not be null");

        // Build dependency graph
        dependencyGraph = ProtoDependencyGraph.build(protoFiles, protoRoot);
        logger.debug("Built dependency graph: " + dependencyGraph);

        // Detect changes
        ChangeDetector detector = new ChangeDetector(protoRoot, previousState);
        changeResult = detector.detectChanges(protoFiles);

        if (changeResult.hasChanges()) {
            logger.info(String.format("Detected changes: %d added, %d modified, %d deleted",
                changeResult.added().size(),
                changeResult.modified().size(),
                changeResult.deleted().size()));
        }

        return changeResult;
    }

    /**
     * Analyze and get all affected files considering dependencies.
     *
     * <p>This includes:
     * <ul>
     *   <li>Files that were added or modified</li>
     *   <li>Files that depend on changed files (transitively)</li>
     * </ul>
     *
     * @param protoFiles current set of proto files
     * @return set of affected file paths (relative)
     * @throws IOException if files cannot be read
     */
    public Set<String> analyzeAndGetAffectedFiles(Set<Path> protoFiles) throws IOException {
        ChangeDetector.ChangeResult changes = analyzeChanges(protoFiles);
        return getAffectedFiles(changes);
    }

    /**
     * Get all affected files based on change result and dependencies.
     *
     * @param changes change detection result
     * @return set of affected file paths (relative)
     */
    public Set<String> getAffectedFiles(ChangeDetector.ChangeResult changes) {
        if (!changes.hasChanges()) {
            return Set.of();
        }

        // If any files were deleted, we need full regeneration
        // because we can't know what messages they contained
        if (changes.hasDeleted()) {
            logger.info("Files deleted, full regeneration required");
            return changes.currentFingerprints().keySet();
        }

        Set<String> affected = new HashSet<>();

        // Add directly changed files
        affected.addAll(changes.getChangedFiles());

        // Add transitive dependents
        for (String changed : changes.getChangedFiles()) {
            Set<String> dependents = dependencyGraph.getTransitiveDependents(changed);
            affected.addAll(dependents);
        }

        if (affected.size() > changes.getChangedFiles().size()) {
            logger.info(String.format("Including %d dependent files",
                affected.size() - changes.getChangedFiles().size()));
        }

        return affected;
    }

    /**
     * Get affected message names based on affected files.
     *
     * @param affectedFiles set of affected file paths
     * @param fileToMessages mapping from proto file path to message names
     * @return set of message names that need regeneration
     */
    public Set<String> getAffectedMessages(
            Set<String> affectedFiles,
            Map<String, Set<String>> fileToMessages) {

        Set<String> affectedMessages = new HashSet<>();

        for (String file : affectedFiles) {
            Set<String> messages = fileToMessages.get(file);
            if (messages != null) {
                affectedMessages.addAll(messages);
            }
        }

        return affectedMessages;
    }

    /**
     * Save current state after successful generation.
     *
     * @throws IOException if state cannot be saved
     */
    public void saveCurrentState() throws IOException {
        saveCurrentState(null);
    }

    /**
     * Save current state after successful generation with generated files info.
     *
     * <p>This method acquires a file lock to ensure thread-safe access
     * when multiple builds run concurrently.
     *
     * @param generatedFiles map of generated file paths to their info, or null
     * @throws IOException if state cannot be saved or lock cannot be acquired
     */
    public void saveCurrentState(Map<String, GeneratedFileInfo> generatedFiles) throws IOException {
        ensureAnalysisDone();

        IncrementalState newState = previousState.withUpdates(
            pluginVersion,
            configHash,
            changeResult.currentFingerprints(),
            dependencyGraph.getImports(),
            generatedFiles
        );

        try (CacheLock lock = CacheLock.acquire(cacheDirectory)) {
            Path stateFile = cacheDirectory.resolve(STATE_FILE);
            newState.writeTo(stateFile);
        }

        logger.info("Saved incremental state to " + cacheDirectory.resolve(STATE_FILE));
    }

    /**
     * Invalidate cache completely.
     *
     * <p>This method acquires a file lock to ensure thread-safe access
     * when multiple builds run concurrently.
     *
     * @throws IOException if cache cannot be deleted or lock cannot be acquired
     */
    public void invalidateCache() throws IOException {
        try (CacheLock lock = CacheLock.acquire(cacheDirectory)) {
            Path stateFile = cacheDirectory.resolve(STATE_FILE);
            if (Files.exists(stateFile)) {
                Files.delete(stateFile);
            }
        }
        previousState = IncrementalState.empty();
        logger.info("Cache invalidated");
    }

    /**
     * Get the cache directory.
     *
     * @return cache directory path
     */
    public Path getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Get the proto root directory.
     *
     * @return proto root path
     */
    public Path getProtoRoot() {
        return protoRoot;
    }

    /**
     * Get the current plugin version.
     *
     * @return plugin version
     */
    public String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Get the current configuration hash.
     *
     * @return config hash
     */
    public String getConfigHash() {
        return configHash;
    }

    /**
     * Check if previous state was loaded.
     *
     * @return true if state was loaded
     */
    public boolean isStateLoaded() {
        return stateLoaded;
    }

    /**
     * Get the dependency graph (only available after analysis).
     *
     * @return dependency graph, or null if not yet analyzed
     */
    public ProtoDependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Get the change result (only available after analysis).
     *
     * @return change result, or null if not yet analyzed
     */
    public ChangeDetector.ChangeResult getChangeResult() {
        return changeResult;
    }

    /**
     * Get the previously generated files info.
     *
     * @return map of generated file paths to their info, or empty map
     */
    public Map<String, GeneratedFileInfo> getPreviousGeneratedFiles() {
        ensureStateLoaded();
        return previousState.generatedFiles();
    }

    private void ensureStateLoaded() {
        if (!stateLoaded) {
            throw new IllegalStateException("Previous state not loaded. Call loadPreviousState() first.");
        }
    }

    private void ensureAnalysisDone() {
        if (changeResult == null || dependencyGraph == null) {
            throw new IllegalStateException("Analysis not done. Call analyzeChanges() first.");
        }
    }
}
