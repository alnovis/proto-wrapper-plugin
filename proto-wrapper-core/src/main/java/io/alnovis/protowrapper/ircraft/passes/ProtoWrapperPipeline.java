package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.DiagnosticMessage;
import io.alnovis.ircraft.core.IrModule;
import io.alnovis.ircraft.core.PassContext;
import io.alnovis.ircraft.core.Pipeline;
import io.alnovis.ircraft.core.emit.Emitter;
import io.alnovis.ircraft.dialect.proto.lowering.LoweringConfig;
import io.alnovis.ircraft.dialect.proto.pipeline.GenericProtoToCodePipeline;
import scala.collection.immutable.List;
import scala.util.Either;

/**
 * Proto-wrapper-specific pipeline: composes generic proto lowering with
 * domain-specific passes for version wrappers, conflict resolution, etc.
 *
 * <p>Uses ircraft's {@link GenericProtoToCodePipeline} as the base (6 generic passes),
 * then appends 7 proto-wrapper-specific passes via {@link Pipeline#andThen}.
 *
 * <p>This class owns the pipeline composition. ircraft knows nothing about proto-wrapper.
 *
 * @since 3.0.0
 */
public class ProtoWrapperPipeline {

    private final GenericProtoToCodePipeline generic;
    private final Emitter emitter;

    public ProtoWrapperPipeline(LoweringConfig config, Emitter emitter) {
        this.generic = new GenericProtoToCodePipeline(config, emitter);
        this.emitter = emitter;
    }

    /**
     * Build the full pipeline: generic passes + proto-wrapper passes.
     *
     * <p>Pass order:
     * <ol>
     *   <li>Generic: ProtoVerifier, ProtoToSemanticLowering, HasMethods, Builder, WKT, Validation</li>
     *   <li>Proto-wrapper: ConflictResolution, ProtoWrapper, CommonMethods, VersionContext,
     *       ProtocolVersions, VersionConversion, SchemaMetadata</li>
     * </ol>
     *
     * <p>All 7 passes are implemented in Java.
     */
    public Pipeline build() {
        return generic.pipeline()
                .andThen(new ConflictResolutionPassJava())
                .andThen(new ProtoWrapperPassJava())
                .andThen(new CommonMethodsPassJava())
                .andThen(new VersionContextPassJava())
                .andThen(new ProtocolVersionsPassJava())
                .andThen(new VersionConversionPassJava())
                .andThen(new SchemaMetadataPassJava());
    }

    /**
     * Run the full pipeline and emit source files.
     */
    @SuppressWarnings("unchecked")
    public Either<List<DiagnosticMessage>, scala.collection.immutable.Map<String, String>> execute(
            IrModule module, PassContext context) {
        var pipeline = build();
        var result = pipeline.run(module, context);
        if (result.hasErrors()) {
            return (Either<List<DiagnosticMessage>, scala.collection.immutable.Map<String, String>>)
                    (Either<?, ?>) scala.util.Left.apply(result.errors());
        }
        return (Either<List<DiagnosticMessage>, scala.collection.immutable.Map<String, String>>)
                (Either<?, ?>) scala.util.Right.apply(emitter.emit(result.module()));
    }
}
