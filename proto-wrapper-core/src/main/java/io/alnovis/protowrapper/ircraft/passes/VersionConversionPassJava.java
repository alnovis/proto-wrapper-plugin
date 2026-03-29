package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.List;

/**
 * Adds version conversion methods to interfaces and abstract classes.
 *
 * <p>Adds to interface:
 * <ul>
 *   <li>{@code asVersion(VersionContext)} -- converts wrapper to another version</li>
 *   <li>{@code getFieldsInaccessibleInVersion(String)} -- lists fields missing in target version</li>
 * </ul>
 *
 * <p>Adds to abstract class:
 * <ul>
 *   <li>Concrete {@code asVersion} delegating to VersionContext</li>
 *   <li>Abstract {@code getFieldsInaccessibleInVersion}</li>
 * </ul>
 *
 * @since 3.0.0
 */
public class VersionConversionPassJava implements Pass {

    @Override public String name() { return "version-conversion"; }
    @Override public String description() { return "Adds asVersion() and getFieldsInaccessibleInVersion() methods"; }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        // Deep bottom-up transform -- handles nested types correctly
        var transformed = IR.transform(module, op -> {
            if (op instanceof InterfaceOp iface
                    && iface.attributes().contains(ProtoAttributes.PresentInVersions()))
                return enrichInterface(iface);
            if (op instanceof ClassOp cls && cls.isAbstract()
                    && cls.attributes().contains(ProtoAttributes.PresentInVersions()))
                return enrichAbstractClass(cls);
            return op;
        });
        return IR.passResult(transformed);
    }

    private InterfaceOp enrichInterface(InterfaceOp iface) {
        return Ops.rebuildIface(iface)
                .addMethod(Ops.method("asVersion", Types.named(iface.name()))
                        .addParameter("targetContext", Types.named("VersionContext"))
                        .abstractPublic()
                        .javadoc("Converts this wrapper to another version using the given VersionContext.")
                        .build())
                .addMethod(Ops.method("getFieldsInaccessibleInVersion", Types.list(Types.STRING()))
                        .addParameter("targetVersionId", Types.STRING())
                        .abstractPublic()
                        .javadoc("Returns field names that are not accessible in the target version.")
                        .build())
                .build();
    }

    private ClassOp enrichAbstractClass(ClassOp cls) {
        String ifaceName = extractInterfaceName(cls);

        // asVersion -- concrete: delegates to targetContext.wrap<Iface>(getTypedProto())
        var asVersionBody = Expr.block(
                Expr.returnStmt(
                        Expr.call(Expr.identifier("targetContext"), "wrap" + ifaceName,
                                List.of(Expr.call("getTypedProto")))));

        return Ops.rebuildClass(cls)
                .addMethod(Ops.method("asVersion", Types.named(ifaceName))
                        .addParameter("targetContext", Types.named("VersionContext"))
                        .publicOverride()
                        .body(asVersionBody)
                        .build())
                .addMethod(Ops.method("getFieldsInaccessibleInVersion", Types.list(Types.STRING()))
                        .addParameter("targetVersionId", Types.STRING())
                        .abstractPublic()
                        .build())
                .build();
    }

    private String extractInterfaceName(ClassOp cls) {
        var implTypes = IR.implementsTypes(cls);
        if (implTypes.isEmpty()) return "Object";
        var first = implTypes.get(0);
        if (first instanceof TypeRef.NamedType nt) return nt.fqn();
        return "Object";
    }
}
