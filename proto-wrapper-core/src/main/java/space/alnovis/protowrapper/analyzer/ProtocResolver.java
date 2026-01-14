package space.alnovis.protowrapper.analyzer;

import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.PluginVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Resolves protoc executable path using the following priority:
 * <ol>
 *   <li>Custom path (if explicitly set)</li>
 *   <li>System PATH (if protoc is installed)</li>
 *   <li>Embedded (download from Maven Central and cache)</li>
 * </ol>
 *
 * <p>Supported platforms:</p>
 * <ul>
 *   <li>Linux x86_64, aarch64</li>
 *   <li>macOS x86_64, aarch64 (Apple Silicon)</li>
 *   <li>Windows x86_64</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * ProtocResolver resolver = new ProtocResolver(logger);
 * Path protoc = resolver.resolve(null);  // Auto-detect
 * // or
 * Path protoc = resolver.resolve("/custom/path/protoc");
 * </pre>
 */
public class ProtocResolver {

    private static final String MAVEN_CENTRAL_BASE_URL =
            "https://repo1.maven.org/maven2/com/google/protobuf/protoc";

    private final PluginLogger logger;
    private final Path cacheDir;
    private String protocVersion;

    /**
     * Creates a resolver with default cache directory.
     *
     * @param logger the logger for output messages
     */
    public ProtocResolver(PluginLogger logger) {
        this(logger, getDefaultCacheDir());
    }

    /**
     * Creates a resolver with custom cache directory.
     *
     * @param logger   the logger for output messages
     * @param cacheDir directory to cache downloaded protoc binaries
     */
    public ProtocResolver(PluginLogger logger, Path cacheDir) {
        this.logger = logger;
        this.cacheDir = cacheDir;
        this.protocVersion = PluginVersion.getProtobufVersion();
    }

    /**
     * Sets the protoc version to download if embedded is used.
     *
     * @param version protoc version (e.g., "4.28.2", "25.1")
     */
    public void setProtocVersion(String version) {
        this.protocVersion = version;
    }

    /**
     * Gets the currently configured protoc version.
     *
     * @return protoc version
     */
    public String getProtocVersion() {
        return protocVersion;
    }

    /**
     * Resolves protoc executable path.
     *
     * <p>Priority:</p>
     * <ol>
     *   <li>Custom path (if provided and valid)</li>
     *   <li>System PATH</li>
     *   <li>Embedded (download if needed)</li>
     * </ol>
     *
     * @param customPath custom path to protoc, or null for auto-detection
     * @return path to protoc executable
     * @throws IOException if protoc cannot be resolved
     */
    public Path resolve(String customPath) throws IOException {
        // 1. Custom path
        if (customPath != null && !customPath.isEmpty()) {
            Path custom = Path.of(customPath);
            if (Files.exists(custom)) {
                if (!Files.isExecutable(custom) && !isWindows()) {
                    custom.toFile().setExecutable(true);
                }
                logger.info("Using custom protoc: " + custom);
                return custom;
            }
            throw new IOException("Custom protoc not found: " + customPath);
        }

        // 2. System PATH
        Path systemProtoc = findSystemProtoc();
        if (systemProtoc != null) {
            logger.info("Using system protoc");
            return systemProtoc;
        }

        // 3. Embedded
        logger.info("System protoc not found, downloading embedded version " + protocVersion);
        return resolveEmbedded();
    }

    /**
     * Resolves embedded protoc, downloading if necessary.
     *
     * @return path to embedded protoc executable
     * @throws IOException if download fails
     */
    public Path resolveEmbedded() throws IOException {
        String classifier = detectClassifier();
        String fileName = buildFileName(classifier);
        Path cachedProtoc = cacheDir.resolve(fileName);

        // Check cache
        if (Files.exists(cachedProtoc)) {
            if (!Files.isExecutable(cachedProtoc) && !isWindows()) {
                cachedProtoc.toFile().setExecutable(true);
            }
            logger.debug("Using cached protoc: " + cachedProtoc);
            return cachedProtoc;
        }

        // Download
        String url = buildDownloadUrl(classifier);
        logger.info("Downloading protoc from Maven Central...");
        logger.debug("URL: " + url);

        Files.createDirectories(cacheDir);

        Path tempFile = cacheDir.resolve(fileName + ".tmp");
        try {
            downloadFile(url, tempFile);
            Files.move(tempFile, cachedProtoc, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Failed to download protoc from " + url + ": " + e.getMessage(), e);
        }

        // Make executable on Unix
        if (!isWindows()) {
            cachedProtoc.toFile().setExecutable(true);
        }

        logger.info("Downloaded protoc " + protocVersion + " to: " + cachedProtoc);
        return cachedProtoc;
    }

    /**
     * Checks if system protoc is available and returns a virtual path to it.
     *
     * @return Path.of("protoc") if available, null otherwise
     */
    Path findSystemProtoc() {
        try {
            ProcessBuilder pb = new ProcessBuilder("protoc", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return Path.of("protoc");
            }
        } catch (Exception e) {
            // Not found in PATH
        }
        return null;
    }

    /**
     * Downloads a file from URL to destination.
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        URI uri = URI.create(urlString);
        URL url = uri.toURL();

        try (InputStream in = url.openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Builds the download URL for protoc.
     */
    String buildDownloadUrl(String classifier) {
        // Format: https://repo1.maven.org/maven2/com/google/protobuf/protoc/4.28.2/protoc-4.28.2-linux-x86_64.exe
        return String.format("%s/%s/protoc-%s-%s.exe",
                MAVEN_CENTRAL_BASE_URL, protocVersion, protocVersion, classifier);
    }

    /**
     * Builds the cached file name.
     */
    String buildFileName(String classifier) {
        if (isWindows()) {
            return String.format("protoc-%s-%s.exe", protocVersion, classifier);
        } else {
            return String.format("protoc-%s-%s", protocVersion, classifier);
        }
    }

    /**
     * Detects OS and architecture classifier for Maven artifact.
     *
     * @return classifier like "linux-x86_64", "osx-aarch_64", "windows-x86_64"
     * @throws UnsupportedOperationException if platform is not supported
     */
    String detectClassifier() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        String osClassifier = detectOsClassifier(os);
        String archClassifier = detectArchClassifier(arch);

        return osClassifier + "-" + archClassifier;
    }

    private String detectOsClassifier(String os) {
        if (os.contains("linux")) {
            return "linux";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "osx";
        } else if (os.contains("windows")) {
            return "windows";
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported operating system: " + os + ". " +
                            "Please install protoc manually and set protocPath.");
        }
    }

    private String detectArchClassifier(String arch) {
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "x86_64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "aarch_64";
        } else if (arch.contains("x86") || arch.equals("i386") || arch.equals("i686")) {
            // 32-bit x86 - try x86_64, may work on 64-bit OS
            return "x86_64";
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported architecture: " + arch + ". " +
                            "Please install protoc manually and set protocPath.");
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
    }

    /**
     * Gets the default cache directory.
     *
     * <ul>
     *   <li>Linux/macOS: ~/.cache/proto-wrapper/protoc/</li>
     *   <li>Windows: %LOCALAPPDATA%\proto-wrapper\protoc\</li>
     * </ul>
     */
    static Path getDefaultCacheDir() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home");

        if (os.contains("windows")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isEmpty()) {
                return Path.of(localAppData, "proto-wrapper", "protoc");
            }
            return Path.of(userHome, "AppData", "Local", "proto-wrapper", "protoc");
        } else {
            // Linux, macOS
            String xdgCache = System.getenv("XDG_CACHE_HOME");
            if (xdgCache != null && !xdgCache.isEmpty()) {
                return Path.of(xdgCache, "proto-wrapper", "protoc");
            }
            return Path.of(userHome, ".cache", "proto-wrapper", "protoc");
        }
    }

    /**
     * Clears the protoc cache directory.
     *
     * @throws IOException if cache cannot be cleared
     */
    public void clearCache() throws IOException {
        if (Files.exists(cacheDir)) {
            try (var files = Files.list(cacheDir)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            logger.info("Cleared protoc cache: " + cacheDir);
        }
    }

    /**
     * Gets the cache directory path.
     *
     * @return cache directory
     */
    public Path getCacheDir() {
        return cacheDir;
    }
}
