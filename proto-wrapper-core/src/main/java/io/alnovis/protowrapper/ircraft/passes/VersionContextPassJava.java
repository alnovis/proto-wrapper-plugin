package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.*;

/**
 * Generates VersionContext interface and per-version implementations.
 *
 * <p>VersionContext is a factory for wrapping proto messages and parsing bytes. Produces:
 * <ul>
 *   <li>{@code VersionContext} interface with {@code wrapXxx(Message)}, {@code parseXxxFromBytes(byte[])} per message</li>
 *   <li>{@code VersionContextV1}, {@code VersionContextV2}, etc. with concrete implementations</li>
 * </ul>
 *
 * @since 3.0.0
 */
public class VersionContextPassJava implements Pass {

    @Override public String name() { return "version-context"; }
    @Override public String description() { return "Generates VersionContext interface and per-version implementations"; }

    @Override
    public boolean isEnabled(PassContext context) {
        return !context.getBool("skipVersionContext");
    }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        // Collect message interfaces that participate in versioning
        var messageInterfaces = new ArrayList<>(IR.collect(module, InterfaceOp.class));
        messageInterfaces.removeIf(i -> !i.attributes().contains(ProtoAttributes.PresentInVersions()));
        if (messageInterfaces.isEmpty()) return IR.passResult(module);

        var first = messageInterfaces.get(0);
        var versionsOpt = first.attributes().getStringList(ProtoAttributes.SchemaVersions());
        if (versionsOpt.isEmpty()) return IR.passResult(module);

        var versions = scala.jdk.CollectionConverters.SeqHasAsJava(versionsOpt.get()).asJava();
        if (versions.isEmpty()) return IR.passResult(module);

        String apiPackage = IR.findApiPackage(module);

        // Generate VersionContext interface
        var contextInterface = buildContextInterface(messageInterfaces);
        var contextFile = Ops.file(apiPackage).addType(contextInterface).build();

        // Generate per-version implementation classes
        var extra = new ArrayList<Operation>();
        extra.add(contextFile);
        for (var version : versions) {
            extra.add(buildVersionImpl(version, messageInterfaces, apiPackage));
        }

        return IR.passResult(IR.appendTopLevel(module, extra));
    }

    private InterfaceOp buildContextInterface(List<InterfaceOp> messages) {
        var builder = Ops.iface("VersionContext")
                .javadoc("Factory for wrapping proto messages and parsing bytes for a specific version.");

        // getVersionId
        builder.addMethod(Ops.method("getVersionId", Types.STRING())
                .abstractPublic()
                .javadoc("Returns the version identifier for this context.")
                .build());

        // Per message: wrapXxx + parseXxxFromBytes
        for (var iface : messages) {
            var name = iface.name();
            var ifaceType = Types.named(name);

            builder.addMethod(Ops.method("wrap" + name, ifaceType)
                    .addParameter("proto", Types.named("com.google.protobuf.Message"))
                    .abstractPublic()
                    .javadoc("Wraps a proto message as " + name + ".")
                    .build());

            builder.addMethod(Ops.method("parse" + name + "FromBytes", ifaceType)
                    .addParameter("bytes", Types.BYTES())
                    .abstractPublic()
                    .javadoc("Parses bytes into " + name + ".")
                    .build());
        }

        return builder.build();
    }

    private FileOp buildVersionImpl(String version, List<InterfaceOp> messages, String apiPackage) {
        String versionSuffix = version.substring(0, 1).toUpperCase() + version.substring(1);
        String implClassName = "VersionContext" + versionSuffix;

        var builder = Ops.cls(implClassName)
                .modifiers(Set.of(IR.PUBLIC(), IR.FINAL()))
                .addImplements(Types.named("VersionContext"));

        // Singleton: INSTANCE field + private constructor
        builder.addField(Ops.staticFinalFieldRaw(
                "INSTANCE", Types.named(implClassName), "new " + implClassName + "()"));
        builder.addConstructor(Ops.privateConstructor());

        // getVersionId
        builder.addMethod(Ops.method("getVersionId", Types.STRING())
                .publicOverride()
                .body(Expr.block(Expr.returnStmt(Expr.literal("\"" + version + "\"", Types.STRING()))))
                .build());

        // Per message: wrapXxx + parseXxxFromBytes (only if message present in this version)
        for (var iface : messages) {
            var msgVersions = iface.attributes().getStringList(ProtoAttributes.PresentInVersions());
            if (msgVersions.isEmpty()) continue;
            var versionList = scala.jdk.CollectionConverters.SeqHasAsJava(msgVersions.get()).asJava();
            if (!versionList.contains(version)) continue;

            String msgName = iface.name();
            String implType = msgName + versionSuffix;
            String protoType = msgName + "Proto";

            // wrapXxx: return new MoneyV1((MoneyProto) proto)
            builder.addMethod(Ops.method("wrap" + msgName, Types.named(msgName))
                    .addParameter("proto", Types.named("com.google.protobuf.Message"))
                    .publicOverride()
                    .body(Expr.block(
                            Expr.returnStmt(Expr.newInstance(Types.named(implType), List.of(
                                    Expr.cast(Expr.identifier("proto"), Types.named(protoType)))))))
                    .build());

            // parseXxxFromBytes: return new MoneyV1(MoneyProto.parseFrom(bytes))
            builder.addMethod(Ops.method("parse" + msgName + "FromBytes", Types.named(msgName))
                    .addParameter("bytes", Types.BYTES())
                    .publicOverride()
                    .body(Expr.block(
                            Expr.returnStmt(Expr.newInstance(Types.named(implType), List.of(
                                    Expr.call(Expr.identifier(protoType), "parseFrom",
                                            List.of(Expr.identifier("bytes"))))))))
                    .build());
        }

        return Ops.file(apiPackage).addType(builder.build()).build();
    }
}
