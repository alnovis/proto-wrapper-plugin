package space.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CacheLock}.
 */
class CacheLockTest {

    @TempDir
    Path tempDir;

    @Test
    void acquire_createsLockFile() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        try (CacheLock lock = CacheLock.acquire(cacheDir)) {
            assertThat(lock).isNotNull();
            assertThat(lock.getLockFile()).exists();
            assertThat(lock.getLockFile().getFileName().toString()).isEqualTo(".lock");
        }
    }

    @Test
    void acquire_createsCacheDirectoryIfNotExists() throws IOException {
        Path nonExistent = tempDir.resolve("non-existent-cache");
        assertThat(nonExistent).doesNotExist();

        try (CacheLock lock = CacheLock.acquire(nonExistent)) {
            assertThat(nonExistent).exists();
        }
    }

    @Test
    void acquire_releasesLockOnClose() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        CacheLock lock1 = CacheLock.acquire(cacheDir);
        lock1.close();

        // Should be able to acquire again after close
        try (CacheLock lock2 = CacheLock.acquire(cacheDir)) {
            assertThat(lock2).isNotNull();
        }
    }

    @Test
    void tryAcquire_returnsNullWhenLocked() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        try (CacheLock lock1 = CacheLock.acquire(cacheDir)) {
            // Try to acquire from same thread - should return null
            CacheLock lock2 = CacheLock.tryAcquire(cacheDir);
            assertThat(lock2).isNull();
        }
    }

    @Test
    void tryAcquire_succeedsWhenNotLocked() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        try (CacheLock lock = CacheLock.tryAcquire(cacheDir)) {
            assertThat(lock).isNotNull();
        }
    }

    @Test
    void isLocked_returnsTrueWhenLocked() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        try (CacheLock lock = CacheLock.acquire(cacheDir)) {
            assertThat(CacheLock.isLocked(cacheDir)).isTrue();
        }
    }

    @Test
    void isLocked_returnsFalseWhenNotLocked() throws IOException {
        Path cacheDir = tempDir.resolve("cache");
        Files.createDirectories(cacheDir);

        assertThat(CacheLock.isLocked(cacheDir)).isFalse();
    }

    @Test
    void acquire_throwsOnTimeout() {
        Path cacheDir = tempDir.resolve("cache");

        try (CacheLock lock1 = CacheLock.acquire(cacheDir)) {
            // Try to acquire with short timeout
            assertThatThrownBy(() -> CacheLock.acquire(cacheDir, 100))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to acquire cache lock");
        } catch (IOException e) {
            // Unexpected exception
            throw new RuntimeException(e);
        }
    }

    @Test
    void acquire_supportsMultipleSequentialLocks() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        for (int i = 0; i < 5; i++) {
            try (CacheLock lock = CacheLock.acquire(cacheDir)) {
                assertThat(lock).isNotNull();
            }
        }
    }

    @Test
    void acquire_concurrentThreadsWaitForLock() throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        int threadCount = 3;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    try (CacheLock lock = CacheLock.acquire(cacheDir, 5000)) {
                        int concurrent = currentConcurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, concurrent));

                        // Simulate work
                        Thread.sleep(50);

                        currentConcurrent.decrementAndGet();
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log but don't fail
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();

        // Wait for completion
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // All threads should have succeeded
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Max concurrent should be 1 (lock is exclusive)
        assertThat(maxConcurrent.get()).isEqualTo(1);
    }

    @Test
    void close_canBeCalledMultipleTimes() throws IOException {
        Path cacheDir = tempDir.resolve("cache");

        CacheLock lock = CacheLock.acquire(cacheDir);
        lock.close();
        lock.close(); // Should not throw
        lock.close(); // Should not throw
    }
}
