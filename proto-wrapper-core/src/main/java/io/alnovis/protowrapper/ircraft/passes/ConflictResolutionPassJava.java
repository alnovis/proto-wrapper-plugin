package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.*;

/**
 * Adds conflict-specific methods (enum helpers, byte accessors, etc.)
 * based on ConflictType attribute on getter methods.
 *
 * @since 3.0.0
 */
public class ConflictResolutionPassJava implements Pass {

    @Override public String name() { return "conflict-resolution"; }
    @Override public String description() { return "Adds conflict-specific methods (enum helpers, byte accessors, etc.)"; }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var updated = new ArrayList<Operation>();
        for (var op : IR.topLevel(module)) {
            if (op instanceof FileOp file) {
                var updatedTypes = new ArrayList<Operation>();
                for (var t : IR.types(file)) {
                    if (t instanceof InterfaceOp iface) updatedTypes.add(enrichInterface(iface));
                    else if (t instanceof ClassOp cls && cls.isAbstract()) updatedTypes.add(enrichAbstractClass(cls));
                    else updatedTypes.add(t);
                }
                updated.add(Ops.file(file.packageName())
                        .attributes(file.attributes()).types(updatedTypes).build());
            } else {
                updated.add(op);
            }
        }
        return IR.passResult(IR.module(module.name(), updated));
    }

    private InterfaceOp enrichInterface(InterfaceOp iface) {
        var additional = new ArrayList<MethodOp>();
        for (var m : IR.methods(iface)) {
            additional.addAll(conflictMethodsForInterface(m));
        }
        if (additional.isEmpty()) return iface;

        var builder = Ops.rebuildIface(iface);
        additional.forEach(builder::addMethod);
        return builder.build();
    }

    private ClassOp enrichAbstractClass(ClassOp cls) {
        var additional = new ArrayList<MethodOp>();
        for (var m : IR.methods(cls)) {
            additional.addAll(conflictMethodsForAbstract(m));
        }
        if (additional.isEmpty()) return cls;

        var builder = Ops.rebuildClass(cls);
        additional.forEach(builder::addMethod);
        return builder.build();
    }

    private List<MethodOp> conflictMethodsForInterface(MethodOp m) {
        var ct = m.attributes().getString(ProtoAttributes.ConflictType()).getOrElse(() -> "None");
        var field = extractFieldName(m.name());
        return switch (ct) {
            case "IntEnum" -> List.of(
                    Ops.method("get" + field + "Enum", Types.named(field + "Enum"))
                            .abstractPublic()
                            .javadoc("Returns the " + field + " value as enum (for INT_ENUM conflict).")
                            .build());
            case "StringBytes" -> List.of(
                    Ops.method("get" + field + "Bytes", Types.BYTES())
                            .abstractPublic()
                            .javadoc("Returns the " + field + " value as bytes (for STRING_BYTES conflict).")
                            .build());
            case "PrimitiveMessage" -> List.of(
                    Ops.method("get" + field + "Message", Types.named("com.google.protobuf.Message"))
                            .abstractPublic()
                            .javadoc("Returns the " + field + " value as message (for PRIMITIVE_MESSAGE conflict).")
                            .build(),
                    Ops.method("supports" + field + "Message", Types.BOOL())
                            .abstractPublic()
                            .javadoc("Returns true if this version supports " + field + " as message type.")
                            .build());
            default -> List.of();
        };
    }

    private List<MethodOp> conflictMethodsForAbstract(MethodOp m) {
        var ct = m.attributes().getString(ProtoAttributes.ConflictType()).getOrElse(() -> "None");
        var field = extractFieldName(m.name());
        return switch (ct) {
            case "IntEnum" -> List.of(
                    Ops.method("extract" + field + "Enum", Types.named(field + "Enum"))
                            .protectedAbstract().build(),
                    delegatingGetter("get" + field + "Enum", Types.named(field + "Enum"), "extract" + field + "Enum"));
            case "StringBytes" -> List.of(
                    Ops.method("extract" + field + "Bytes", Types.BYTES())
                            .protectedAbstract().build(),
                    delegatingGetter("get" + field + "Bytes", Types.BYTES(), "extract" + field + "Bytes"));
            case "PrimitiveMessage" -> List.of(
                    Ops.method("extract" + field + "Message", Types.named("com.google.protobuf.Message"))
                            .protectedAbstract().build(),
                    delegatingGetter("get" + field + "Message", Types.named("com.google.protobuf.Message"), "extract" + field + "Message"),
                    Ops.method("supports" + field + "Message", Types.BOOL())
                            .abstractPublic().build());
            default -> List.of();
        };
    }

    private MethodOp delegatingGetter(String name, TypeRef returnType, String delegateTo) {
        var body = Expr.block(Expr.returnStmt(Expr.call(delegateTo)));
        return Ops.method(name, returnType)
                .publicOverride()
                .body(body)
                .build();
    }

    private String extractFieldName(String methodName) {
        if (methodName.startsWith("get")) return methodName.substring(3);
        if (methodName.startsWith("extract")) return methodName.substring(7);
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }
}
