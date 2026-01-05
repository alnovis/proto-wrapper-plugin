package space.alnovis.protowrapper.incremental;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * File-based lock for thread-safe cache access in parallel builds.
 *
 * <p>Uses Java's {@link FileLock} mechanism to provide process-level
 * locking for the incremental cache state file. This prevents concurrent
 * builds from corrupting the cache.
 *
 * <p>Example usage:
 * <pre>
 * try (CacheLock lock = CacheLock.acquire(cacheDir, logger)) {
 *     // Perform cache operations
 *     state.writeTo(cacheFile);
 * }
 * </pre>
 *
 * <p>If the lock cannot be acquired within the timeout, an IOException is thrown.
 *
 * @see IncrementalStateManager
 */
public final class CacheLock implements Closeable {

    private static final String LOCK_FILE = ".lock";
    private static final long DEFAULT_TIMEOUT_MS = 30_000; // 30 seconds
    private static final long RETRY_INTERVAL_MS = 100;

    private final Path lockFile;
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final FileLock lock;

    private CacheLock(Path lockFile, RandomAccessFile raf, FileChannel channel, FileLock lock) {
        this.lockFile = lockFile;
        this.raf = raf;
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Acquire a lock on the cache directory.
     *
     * <p>This method will block until the lock is acquired or timeout is reached.
     * Uses the default timeout of 30 seconds.
     *
     * @param cacheDirectory directory containing cache files
     * @return acquired lock (must be closed after use)
     * @throws IOException if lock cannot be acquired within timeout
     */
    public static CacheLock acquire(Path cacheDirectory) throws IOException {
        return acquire(cacheDirectory, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Acquire a lock on the cache directory with custom timeout.
     *
     * @param cacheDirectory directory containing cache files
     * @param timeoutMs maximum time to wait for lock in milliseconds
     * @return acquired lock (must be closed after use)
     * @throws IOException if lock cannot be acquired within timeout
     */
    public static CacheLock acquire(Path cacheDirectory, long timeoutMs) throws IOException {
        Objects.requireNonNull(cacheDirectory, "cacheDirectory must not be null");

        // Ensure cache directory exists
        Files.createDirectories(cacheDirectory);

        Path lockFile = cacheDirectory.resolve(LOCK_FILE);

        // Create lock file if it doesn't exist
        if (!Files.exists(lockFile)) {
            try {
                Files.createFile(lockFile);
            } catch (IOException e) {
                // File may have been created by another process, ignore
            }
        }

        RandomAccessFile raf = null;
        FileChannel channel = null;
        FileLock lock = null;

        try {
            raf = new RandomAccessFile(lockFile.toFile(), "rw");
            channel = raf.getChannel();

            long startTime = System.currentTimeMillis();
            long elapsed = 0;

            // Try to acquire lock with timeout
            while (elapsed < timeoutMs) {
                try {
                    lock = channel.tryLock();
                    if (lock != null) {
                        return new CacheLock(lockFile, raf, channel, lock);
                    }
                } catch (OverlappingFileLockException e) {
                    // Lock held by this JVM (same process, different thread)
                    // Shouldn't happen in normal Maven/Gradle usage, but handle it
                }

                // Wait and retry
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for cache lock", e);
                }

                elapsed = System.currentTimeMillis() - startTime;
            }

            // Timeout reached
            throw new IOException(String.format(
                "Failed to acquire cache lock within %d ms. " +
                "Another build may be running. Lock file: %s",
                timeoutMs, lockFile));

        } catch (IOException e) {
            // Clean up on failure
            closeQuietly(lock);
            closeQuietly(channel);
            closeQuietly(raf);
            throw e;
        }
    }

    /**
     * Try to acquire lock without waiting.
     *
     * @param cacheDirectory directory containing cache files
     * @return acquired lock, or null if lock is not available
     * @throws IOException if lock file cannot be accessed
     */
    public static CacheLock tryAcquire(Path cacheDirectory) throws IOException {
        Objects.requireNonNull(cacheDirectory, "cacheDirectory must not be null");

        // Ensure cache directory exists
        Files.createDirectories(cacheDirectory);

        Path lockFile = cacheDirectory.resolve(LOCK_FILE);

        // Create lock file if it doesn't exist
        if (!Files.exists(lockFile)) {
            try {
                Files.createFile(lockFile);
            } catch (IOException e) {
                // File may have been created by another process, ignore
            }
        }

        RandomAccessFile raf = null;
        FileChannel channel = null;
        FileLock lock = null;

        try {
            raf = new RandomAccessFile(lockFile.toFile(), "rw");
            channel = raf.getChannel();

            try {
                lock = channel.tryLock();
                if (lock != null) {
                    return new CacheLock(lockFile, raf, channel, lock);
                }
            } catch (OverlappingFileLockException e) {
                // Lock held by this JVM
            }

            // Lock not available
            closeQuietly(channel);
            closeQuietly(raf);
            return null;

        } catch (IOException e) {
            closeQuietly(lock);
            closeQuietly(channel);
            closeQuietly(raf);
            throw e;
        }
    }

    /**
     * Check if the cache is currently locked by another process.
     *
     * @param cacheDirectory directory containing cache files
     * @return true if locked, false otherwise
     */
    public static boolean isLocked(Path cacheDirectory) {
        try (CacheLock lock = tryAcquire(cacheDirectory)) {
            return lock == null;
        } catch (IOException e) {
            return true; // Assume locked if we can't check
        }
    }

    /**
     * Get the path to the lock file.
     *
     * @return lock file path
     */
    public Path getLockFile() {
        return lockFile;
    }

    @Override
    public void close() throws IOException {
        IOException firstException = null;

        // Release lock
        if (lock != null && lock.isValid()) {
            try {
                lock.release();
            } catch (IOException e) {
                firstException = e;
            }
        }

        // Close channel
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        // Close file
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        if (firstException != null) {
            throw firstException;
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // Ignore
            }
        }
    }

    private static void closeQuietly(FileLock lock) {
        if (lock != null && lock.isValid()) {
            try {
                lock.release();
            } catch (IOException ignored) {
                // Ignore
            }
        }
    }
}
