package space.alnovis.protowrapper.incremental;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Detects changes in proto files compared to previous state.
 *
 * <p>Change detection uses a two-phase approach for efficiency:
 * <ol>
 *   <li>Quick check: Compare file size and modification time</li>
 *   <li>Full check: If quick check indicates possible change, compare content hash</li>
 * </ol>
 *
 * <p>This class also detects:
 * <ul>
 *   <li>New files (present now, not in previous state)</li>
 *   <li>Deleted files (in previous state, not present now)</li>
 *   <li>Modified files (content hash changed)</li>
 * </ul>
 */
public class ChangeDetector {

    private final Path protoRoot;
    private final IncrementalState previousState;

    /**
     * Create a new change detector.
     *
     * @param protoRoot root directory for proto files
     * @param previousState previous incremental state (may be empty)
     */
    public ChangeDetector(Path protoRoot, IncrementalState previousState) {
        this.protoRoot = Objects.requireNonNull(protoRoot, "protoRoot must not be null");
        this.previousState = Objects.requireNonNull(previousState, "previousState must not be null");
    }

    /**
     * Detect changed proto files.
     *
     * @param currentProtos set of current proto file paths (absolute)
     * @return change detection result
     * @throws IOException if files cannot be read
     */
    public ChangeResult detectChanges(Set<Path> currentProtos) throws IOException {
        Objects.requireNonNull(currentProtos, "currentProtos must not be null");

        Set<String> added = new HashSet<>();
        Set<String> modified = new HashSet<>();
        Set<String> deleted = new HashSet<>();
        Map<String, FileFingerprint> currentFingerprints = new HashMap<>();

        Map<String, FileFingerprint> previousFingerprints = previousState.protoFingerprints();
        Set<String> currentPaths = new HashSet<>();

        for (Path protoFile : currentProtos) {
            String relativePath = protoRoot.relativize(protoFile)
                .toString().replace('\\', '/');
            currentPaths.add(relativePath);

            FileFingerprint previous = previousFingerprints.get(relativePath);
            FileFingerprint current = FileFingerprint.compute(protoFile, protoRoot);
            currentFingerprints.put(relativePath, current);

            if (previous == null) {
                // New file
                added.add(relativePath);
            } else if (!current.matches(previous)) {
                // Content changed
                modified.add(relativePath);
            }
            // else: unchanged
        }

        // Check for deleted files
        for (String previousPath : previousFingerprints.keySet()) {
            if (!currentPaths.contains(previousPath)) {
                deleted.add(previousPath);
            }
        }

        return new ChangeResult(added, modified, deleted, currentFingerprints);
    }

    /**
     * Compute fingerprints for all proto files without comparing to previous state.
     *
     * @param protoFiles set of proto file paths (absolute)
     * @return map of relative path to fingerprint
     * @throws IOException if files cannot be read
     */
    public Map<String, FileFingerprint> computeFingerprints(Set<Path> protoFiles) throws IOException {
        Objects.requireNonNull(protoFiles, "protoFiles must not be null");

        Map<String, FileFingerprint> fingerprints = new HashMap<>();

        for (Path protoFile : protoFiles) {
            FileFingerprint fp = FileFingerprint.compute(protoFile, protoRoot);
            fingerprints.put(fp.relativePath(), fp);
        }

        return fingerprints;
    }

    /**
     * Quick check if any file might have changed based on size and timestamp.
     * This is useful for a fast preliminary check before full analysis.
     *
     * @param currentProtos set of current proto file paths
     * @return true if any file might have changed
     * @throws IOException if files cannot be read
     */
    public boolean mightHaveChanges(Set<Path> currentProtos) throws IOException {
        Map<String, FileFingerprint> previousFingerprints = previousState.protoFingerprints();

        // Different number of files means definite change
        if (currentProtos.size() != previousFingerprints.size()) {
            return true;
        }

        for (Path protoFile : currentProtos) {
            String relativePath = protoRoot.relativize(protoFile)
                .toString().replace('\\', '/');

            FileFingerprint previous = previousFingerprints.get(relativePath);
            if (previous == null) {
                // New file
                return true;
            }

            // Quick check using size and timestamp
            long currentSize = java.nio.file.Files.size(protoFile);
            long currentModified = java.nio.file.Files.getLastModifiedTime(protoFile).toMillis();

            if (currentSize != previous.fileSize() || currentModified != previous.lastModified()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Result of change detection.
     *
     * @param added files that are new (not in previous state)
     * @param modified files that have changed content
     * @param deleted files that were removed (in previous state but not now)
     * @param currentFingerprints computed fingerprints for all current files
     */
    public record ChangeResult(
        Set<String> added,
        Set<String> modified,
        Set<String> deleted,
        Map<String, FileFingerprint> currentFingerprints
    ) {

        /**
         * Check if there are any changes.
         *
         * @return true if any files were added, modified, or deleted
         */
        public boolean hasChanges() {
            return !added.isEmpty() || !modified.isEmpty() || !deleted.isEmpty();
        }

        /**
         * Get all changed files (added + modified).
         * Deleted files are not included as they no longer exist.
         *
         * @return set of changed file paths
         */
        public Set<String> getChangedFiles() {
            Set<String> result = new HashSet<>(added);
            result.addAll(modified);
            return result;
        }

        /**
         * Get total number of changes.
         *
         * @return count of added + modified + deleted
         */
        public int totalChanges() {
            return added.size() + modified.size() + deleted.size();
        }

        /**
         * Check if there are any deleted files.
         * Deleted files typically require full regeneration.
         *
         * @return true if any files were deleted
         */
        public boolean hasDeleted() {
            return !deleted.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("ChangeResult{added=%d, modified=%d, deleted=%d}",
                added.size(), modified.size(), deleted.size());
        }
    }
}
