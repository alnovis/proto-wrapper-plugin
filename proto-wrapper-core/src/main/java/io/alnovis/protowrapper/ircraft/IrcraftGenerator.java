package io.alnovis.protowrapper.ircraft;

import io.alnovis.ircraft.core.Module;
import io.alnovis.ircraft.core.PassContext;
import io.alnovis.ircraft.dialect.java.emit.DirectJavaEmitter;
import io.alnovis.ircraft.pipeline.prototojava.ProtoToJavaPipeline;
import io.alnovis.ircraft.dialect.proto.ops.SchemaOp;
import io.alnovis.ircraft.dialect.proto.lowering.LoweringConfig;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.PluginLogger;
import io.alnovis.protowrapper.model.MergedSchema;
import scala.collection.immutable.Vector;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

/**
 * Generates Java source files using the ircraft pipeline.
 *
 * <p>Alternative to the legacy JavaPoet-based generation. Uses:
 * <ol>
 *   <li>IrcraftBridge to convert MergedSchema → Proto Dialect IR</li>
 *   <li>ProtoToJavaPipeline to lower Proto → Semantic → Java source</li>
 * </ol>
 *
 * @since 3.0.0
 */
public class IrcraftGenerator {

    private final GeneratorConfig config;
    private final PluginLogger logger;
    private final IrcraftBridge bridge;

    public IrcraftGenerator(GeneratorConfig config, PluginLogger logger) {
        this.config = config;
        this.logger = logger;
        this.bridge = new IrcraftBridge();
    }

    /**
     * Generate Java source files from a MergedSchema using the ircraft pipeline.
     *
     * <p>When {@code config.getCacheDirectory()} is non-null, uses incremental generation:
     * only files for changed entities (plus global files) are written. Unchanged files are skipped.
     *
     * @param schema the merged schema
     * @return number of files generated
     * @throws IOException if file writing fails
     */
    public int generateAll(MergedSchema schema) throws IOException {
        logger.info("[ircraft] Converting MergedSchema to Proto IR...");
        SchemaOp protoIR = bridge.toProtoIR(schema);

        // Convert {version} pattern to %s format for ircraft
        String implPattern = config.getImplPackagePattern().replace("{version}", "%s");

        LoweringConfig loweringConfig = new LoweringConfig(
                config.getApiPackage(),
                implPattern,
                config.isGenerateBuilders(),
                config.isConvertWellKnownTypes(),
                config.isGenerateProtocolVersions(),
                config.isGenerateValidationAnnotations(),
                "Abstract"
        );

        ProtoToJavaPipeline pipeline = new ProtoToJavaPipeline(loweringConfig);

        @SuppressWarnings("unchecked")
        Vector<io.alnovis.ircraft.core.Operation> topLevel =
                (Vector<io.alnovis.ircraft.core.Operation>) (Vector<?>) Vector.from(
                        scala.jdk.CollectionConverters.IterableHasAsScala(
                                java.util.List.<io.alnovis.ircraft.core.Operation>of(protoIR)
                        ).asScala()
                );
        Module module = new Module("proto-wrapper", topLevel,
                io.alnovis.ircraft.core.AttributeMap.empty(),
                scala.Option.empty());

        var passContext = new PassContext(
                scala.collection.immutable.Map$.MODULE$.empty(),
                io.alnovis.ircraft.core.PassLogger.noop()
        );

        Path cacheDir = config.getCacheDirectory();
        var result = cacheDir != null
                ? pipeline.executeIncremental(module, cacheDir, passContext)
                : pipeline.execute(module, passContext);

        if (cacheDir != null) {
            logger.info("[ircraft] Running incremental Proto → Java pipeline...");
        } else {
            logger.info("[ircraft] Running Proto → Java pipeline...");
        }

        if (result.isLeft()) {
            var errors = scala.jdk.CollectionConverters.SeqHasAsJava(result.left().get()).asJava();
            for (var error : errors) {
                logger.error("[ircraft] " + error.toString());
            }
            throw new IOException("ircraft pipeline failed with " + errors.size() + " errors");
        }

        Map<String, String> files = scala.jdk.CollectionConverters.MapHasAsJava(result.toOption().get()).asJava();

        if (cacheDir != null && files.isEmpty()) {
            logger.info("[ircraft] All files up to date, nothing to generate");
            return 0;
        }

        Path outputDir = config.getOutputDirectory();
        int count = 0;
        for (var entry : files.entrySet()) {
            Path filePath = outputDir.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, entry.getValue());
            count++;
        }

        logger.info("[ircraft] Generated " + count + " files" + (cacheDir != null ? " (incremental)" : ""));
        return count;
    }
}
