package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.ops.SchemaOp;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.*;

/**
 * Generates SchemaDiffV1ToV2 classes for consecutive version pairs.
 *
 * <p>Uses {@link SchemaDiffApi} to compute diffs from IR attributes
 * (presentInVersions, typesPerVersion) rather than from the Java model.
 *
 * <p>Receives SchemaOp in constructor since it is no longer available in the module
 * after ProtoToSemanticLowering replaces it with FileOps.
 *
 * @since 3.0.0
 */
public class SchemaDiffPassJava implements Pass {

    private final SchemaOp schemaOp;

    public SchemaDiffPassJava(SchemaOp schemaOp) {
        this.schemaOp = schemaOp;
    }

    @Override public String name() { return "schema-diff"; }
    @Override public String description() { return "Generates SchemaDiff classes for version pairs from IR"; }

    @Override
    public boolean isEnabled(PassContext context) {
        return context.getBool("generateSchemaMetadata");
    }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var versions = scala.jdk.CollectionConverters.SeqHasAsJava(schemaOp.versions()).asJava();
        if (versions.size() < 2) return IR.passResult(module);

        String apiPackage = IR.findApiPackage(module);
        String metadataPackage = apiPackage.replace(".api", "") + ".metadata";

        var diffFiles = new ArrayList<FileOp>();
        for (int i = 0; i < versions.size() - 1; i++) {
            String v1 = versions.get(i);
            String v2 = versions.get(i + 1);
            var diff = SchemaDiffApi.diffVersions(schemaOp, v1, v2);
            if (diff.hasChanges()) {
                diffFiles.add(generateDiffClass(diff, metadataPackage));
            }
        }

        if (diffFiles.isEmpty()) return IR.passResult(module);
        return IR.passResult(IR.appendTopLevel(module, diffFiles));
    }

    private FileOp generateDiffClass(JVersionDiff diff, String packageName) {
        String v1Suffix = capitalize(diff.fromVersion());
        String v2Suffix = capitalize(diff.toVersion());
        String className = "SchemaDiff" + v1Suffix + "To" + v2Suffix;

        var builder = Ops.cls(className)
                .modifiers(Set.of(IR.PUBLIC(), IR.FINAL()));

        // Singleton
        builder.addField(Ops.staticFinalFieldRaw("INSTANCE", Types.named(className), "new " + className + "()"));
        builder.addConstructor(Ops.privateConstructor());

        // Version constants
        builder.addField(Ops.staticFinalField("FROM_VERSION", Types.STRING(), diff.fromVersion()));
        builder.addField(Ops.staticFinalField("TO_VERSION", Types.STRING(), diff.toVersion()));

        // getFromVersion / getToVersion
        builder.addMethod(Ops.method("getFromVersion", Types.STRING())
                .body(Expr.block(Expr.returnStmt(Expr.identifier("FROM_VERSION"))))
                .build());
        builder.addMethod(Ops.method("getToVersion", Types.STRING())
                .body(Expr.block(Expr.returnStmt(Expr.identifier("TO_VERSION"))))
                .build());

        // getFieldChanges -- returns list of change descriptions
        var fieldChangeArgs = new ArrayList<io.alnovis.ircraft.dialect.semantic.expr.Expression>();
        for (var mc : diff.messageChanges()) {
            for (var fc : mc.fieldChanges()) {
                if (!"Unchanged".equals(fc.changeType())) {
                    fieldChangeArgs.add(Expr.literal("\"" + mc.name() + "." + fc.fieldName()
                            + ": " + fc.migrationHint() + "\"", Types.STRING()));
                }
            }
        }
        builder.addMethod(Ops.method("getFieldChanges", Types.list(Types.STRING()))
                .body(Expr.block(Expr.returnStmt(
                        Expr.call(Expr.identifier("java.util.List"), "of", fieldChangeArgs))))
                .build());

        // getEnumChanges -- returns list of change descriptions
        var enumChangeArgs = new ArrayList<io.alnovis.ircraft.dialect.semantic.expr.Expression>();
        for (var ec : diff.enumChanges()) {
            if (!"Present".equals(ec.changeType())) {
                enumChangeArgs.add(Expr.literal("\"" + ec.name() + ": " + ec.changeType() + "\"", Types.STRING()));
            }
        }
        builder.addMethod(Ops.method("getEnumChanges", Types.list(Types.STRING()))
                .body(Expr.block(Expr.returnStmt(
                        Expr.call(Expr.identifier("java.util.List"), "of", enumChangeArgs))))
                .build());

        return Ops.file(packageName).addType(builder.build()).build();
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
