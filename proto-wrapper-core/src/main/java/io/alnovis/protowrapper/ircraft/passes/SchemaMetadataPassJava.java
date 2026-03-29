package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.expr.Expression;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates SchemaInfo classes per version with runtime schema metadata.
 *
 * <p>Produces per version:
 * <pre>
 * public final class SchemaInfoV1 {
 *   public static final SchemaInfoV1 INSTANCE = new SchemaInfoV1();
 *   public String getVersionId() { return "v1"; }
 *   public List&lt;String&gt; getMessageNames() { return List.of("Money", "Order"); }
 *   public List&lt;String&gt; getEnumNames() { return List.of("Currency"); }
 * }
 * </pre>
 *
 * <p>Conditional: controlled by PassContext "generateSchemaMetadata".
 *
 * @since 3.0.0
 */
public class SchemaMetadataPassJava implements Pass {

    @Override public String name() { return "schema-metadata"; }
    @Override public String description() { return "Generates SchemaInfo classes with runtime schema metadata per version"; }

    @Override
    public boolean isEnabled(PassContext context) {
        return context.getBool("generateSchemaMetadata");
    }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var messageInterfaces = IR.collect(module, InterfaceOp.class);
        messageInterfaces.removeIf(i -> !i.attributes().contains(ProtoAttributes.PresentInVersions()));
        if (messageInterfaces.isEmpty()) return IR.passResult(module);

        var first = messageInterfaces.get(0);
        var versionsOpt = first.attributes().getStringList(ProtoAttributes.SchemaVersions());
        if (versionsOpt.isEmpty()) return IR.passResult(module);

        var versions = scala.jdk.CollectionConverters.SeqHasAsJava(versionsOpt.get()).asJava();

        String apiPackage = IR.findApiPackage(module);
        String metadataPackage = apiPackage.replace(".api", "") + ".metadata";

        var messageNames = messageInterfaces.stream().map(InterfaceOp::name).toList();
        var enumNames = IR.collect(module, EnumClassOp.class).stream().map(EnumClassOp::name).toList();

        var schemaInfoFiles = new ArrayList<FileOp>();
        for (var version : versions) {
            schemaInfoFiles.add(generateSchemaInfo(version, messageNames, enumNames, metadataPackage));
        }

        return IR.passResult(IR.appendTopLevel(module, schemaInfoFiles));
    }

    private FileOp generateSchemaInfo(
            String version,
            List<String> messageNames,
            List<String> enumNames,
            String packageName) {
        String versionSuffix = version.substring(0, 1).toUpperCase() + version.substring(1);
        String className = "SchemaInfo" + versionSuffix;

        var builder = Ops.cls(className)
                .modifiers(java.util.Set.of(IR.PUBLIC(), IR.FINAL()));

        // INSTANCE singleton field
        builder.addField(Ops.staticFinalFieldRaw("INSTANCE", Types.named(className), "new " + className + "()"));
        builder.addConstructor(Ops.privateConstructor());

        // getVersionId
        builder.addMethod(Ops.method("getVersionId", Types.STRING())
                .body(Expr.block(Expr.returnStmt(Expr.literal("\"" + version + "\"", Types.STRING()))))
                .build());

        // getMessageNames
        builder.addMethod(Ops.method("getMessageNames", Types.list(Types.STRING()))
                .body(Expr.block(Expr.returnStmt(listOfLiterals(messageNames))))
                .build());

        // getEnumNames
        builder.addMethod(Ops.method("getEnumNames", Types.list(Types.STRING()))
                .body(Expr.block(Expr.returnStmt(listOfLiterals(enumNames))))
                .build());

        return Ops.file(packageName).addType(builder.build()).build();
    }

    /**
     * Builds {@code java.util.List.of("name1", "name2", ...)} expression.
     */
    private Expression listOfLiterals(List<String> names) {
        var args = names.stream()
                .map(n -> (Expression) Expr.literal("\"" + n + "\"", Types.STRING()))
                .toList();
        return Expr.call(Expr.identifier("java.util.List"), "of", args);
    }
}
