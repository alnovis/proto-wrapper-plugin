package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.*;

public class CommonMethodsPassJava implements Pass {

    @Override public String name() { return "common-methods"; }
    @Override public String description() { return "Adds equals, hashCode, toString, serialization to abstract classes"; }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var updated = new ArrayList<Operation>();
        for (var op : IR.topLevel(module)) {
            if (op instanceof FileOp file) {
                var ut = new ArrayList<Operation>();
                for (var t : IR.types(file)) {
                    if (t instanceof ClassOp cls && cls.isAbstract()
                            && cls.attributes().contains(ProtoAttributes.PresentInVersions()))
                        ut.add(enrichAbstractClass(cls));
                    else ut.add(t);
                }
                updated.add(Ops.file(file.packageName()).attributes(file.attributes()).types(ut).build());
            } else updated.add(op);
        }
        return IR.passResult(IR.module(module.name(), updated));
    }

    private ClassOp enrichAbstractClass(ClassOp cls) {
        var cn = cls.name();
        var builder = Ops.rebuildClass(cls);

        // getTypedProto
        builder.addMethod(Ops.method("getTypedProto", Types.named("com.google.protobuf.Message"))
                .publicOverride()
                .body(Expr.block(Expr.returnStmt(Expr.identifier("proto"))))
                .build());

        // getWrapperVersionId (abstract)
        builder.addMethod(Ops.method("getWrapperVersionId", Types.STRING())
                .abstractPublic().build());

        // serializeToBytes (protected abstract)
        builder.addMethod(Ops.method("serializeToBytes", Types.BYTES())
                .protectedAbstract().build());

        // toBytes
        builder.addMethod(Ops.method("toBytes", Types.BYTES())
                .publicOverride()
                .body(Expr.block(Expr.returnStmt(Expr.call("serializeToBytes"))))
                .build());

        // toString
        var tsExpr = Expr.binOp(
                Expr.binOp(
                        Expr.binOp(Expr.literal("\"" + cn + "[\"", Types.STRING()), Expr.ADD(), Expr.call("getWrapperVersionId")),
                        Expr.ADD(), Expr.literal("\"] \"", Types.STRING())),
                Expr.ADD(), Expr.call(Expr.identifier("proto"), "toString"));
        builder.addMethod(Ops.method("toString", Types.STRING())
                .publicOverride().addAnnotation("Override")
                .body(Expr.block(Expr.returnStmt(tsExpr)))
                .build());

        // equals
        var eqBody = Expr.block(List.of(
                Expr.ifStmt(Expr.binOp(Expr.thisRef(), Expr.EQ(), Expr.identifier("obj")),
                        Expr.block(Expr.returnStmt(Expr.literal("true", Types.BOOL())))),
                Expr.ifStmt(Expr.binOp(Expr.identifier("obj"), Expr.EQ(), Expr.nullLiteral()),
                        Expr.block(Expr.returnStmt(Expr.literal("false", Types.BOOL())))),
                Expr.returnStmt(Expr.binOp(
                        Expr.call(Expr.call("getWrapperVersionId"), "equals",
                                List.of(Expr.call(Expr.cast(Expr.identifier("obj"), Types.named(cn)), "getWrapperVersionId"))),
                        Expr.AND(),
                        Expr.call(Expr.identifier("proto"), "equals",
                                List.of(Expr.fieldAccess(Expr.cast(Expr.identifier("obj"), Types.named(cn)), "proto")))))));
        builder.addMethod(Ops.method("equals", Types.BOOL())
                .addParameter("obj", Types.named("Object"))
                .publicOverride().addAnnotation("Override")
                .body(eqBody)
                .build());

        // hashCode
        var hcExpr = Expr.binOp(
                Expr.binOp(Expr.literal("31", Types.INT()), Expr.MUL(), Expr.call(Expr.call("getWrapperVersionId"), "hashCode")),
                Expr.ADD(), Expr.call(Expr.identifier("proto"), "hashCode"));
        builder.addMethod(Ops.method("hashCode", Types.INT())
                .publicOverride().addAnnotation("Override")
                .body(Expr.block(Expr.returnStmt(hcExpr)))
                .build());

        return builder.build();
    }
}
