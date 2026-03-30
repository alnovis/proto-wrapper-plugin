package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.List;
import java.util.Set;

/**
 * Enriches conflict enums with lookup methods: fromProtoValue, fromProtoValueOrDefault, fromProtoValueOrThrow.
 *
 * <p>Identifies conflict enums by the generic {@code ir.sourceNodeKind} provenance attribute
 * set during lowering. Any EnumClassOp whose sourceNodeKind contains "conflict_enum" gets enriched.
 *
 * @since 3.0.0
 */
public class ConflictEnumEnrichmentPassJava implements Pass {

    @Override public String name() { return "conflict-enum-enrichment"; }
    @Override public String description() { return "Adds fromProtoValue/OrDefault/OrThrow to conflict enums"; }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var transformed = IR.transform(module, op -> {
            if (op instanceof EnumClassOp e && isConflictEnum(e))
                return enrich(e);
            return op;
        });
        return IR.passResult(transformed);
    }

    private boolean isConflictEnum(EnumClassOp e) {
        var sourceKind = e.attributes().getString(LoweringAttributes.SourceNodeKind());
        return sourceKind.isDefined() && sourceKind.get().contains("conflict_enum");
    }

    private EnumClassOp enrich(EnumClassOp e) {
        var enumName = e.name();
        var enumType = Types.named(enumName);

        return Ops.rebuildEnumClass(e)
                .addMethod(buildFromProtoValue(enumType))
                .addMethod(buildFromProtoValueOrDefault(enumType))
                .addMethod(buildFromProtoValueOrThrow(enumType, enumName))
                .build();
    }

    /**
     * {@code public static EnumType fromProtoValue(int value)}
     * <p>Iterates over values(), returns matching constant or null.
     */
    private MethodOp buildFromProtoValue(TypeRef enumType) {
        // for (EnumType e : values()) { if (e.value == value) return e; }
        // return null;
        var loop = Expr.forEach("e", enumType, Expr.call("values"),
                Expr.block(
                        Expr.ifStmt(
                                Expr.binOp(Expr.fieldAccess(Expr.identifier("e"), "value"),
                                        Expr.EQ(), Expr.identifier("value")),
                                Expr.block(Expr.returnStmt(Expr.identifier("e"))))));

        var body = Expr.block(List.of(loop, Expr.returnStmt(Expr.nullLiteral())));

        return Ops.method("fromProtoValue", enumType)
                .modifiers(Set.of(IR.PUBLIC(), IR.STATIC()))
                .addParameter("value", Types.INT())
                .body(body)
                .javadoc("Convert proto enum numeric value to this enum.")
                .build();
    }

    /**
     * {@code public static EnumType fromProtoValueOrDefault(int value, EnumType defaultValue)}
     * <p>Delegates to fromProtoValue, returns default if null.
     */
    private MethodOp buildFromProtoValueOrDefault(TypeRef enumType) {
        // EnumType result = fromProtoValue(value);
        // return result != null ? result : defaultValue;
        var body = Expr.block(List.of(
                Expr.varDecl("result", enumType,
                        Expr.call("fromProtoValue", List.of(Expr.identifier("value")))),
                Expr.returnStmt(
                        Expr.conditional(
                                Expr.binOp(Expr.identifier("result"), Expr.NEQ(), Expr.nullLiteral()),
                                Expr.identifier("result"),
                                Expr.identifier("defaultValue")))));

        return Ops.method("fromProtoValueOrDefault", enumType)
                .modifiers(Set.of(IR.PUBLIC(), IR.STATIC()))
                .addParameter("value", Types.INT())
                .addParameter("defaultValue", enumType)
                .body(body)
                .javadoc("Convert proto enum numeric value to this enum with a default.")
                .build();
    }

    /**
     * {@code public static EnumType fromProtoValueOrThrow(int value)}
     * <p>Delegates to fromProtoValue, throws IllegalArgumentException if null.
     */
    private MethodOp buildFromProtoValueOrThrow(TypeRef enumType, String enumName) {
        // EnumType result = fromProtoValue(value);
        // if (result == null) { throw new IllegalArgumentException("Invalid value " + value + ...); }
        // return result;
        var errorMsg = Expr.binOp(
                Expr.binOp(Expr.literal("\"Invalid value \"", Types.STRING()), Expr.ADD(), Expr.identifier("value")),
                Expr.ADD(),
                Expr.binOp(Expr.literal("\" for " + enumName + ". Valid values: \"", Types.STRING()),
                        Expr.ADD(),
                        Expr.call(Expr.identifier("java.util.Arrays"), "toString",
                                List.of(Expr.call("values")))));

        var body = Expr.block(List.of(
                Expr.varDecl("result", enumType,
                        Expr.call("fromProtoValue", List.of(Expr.identifier("value")))),
                Expr.ifStmt(
                        Expr.binOp(Expr.identifier("result"), Expr.EQ(), Expr.nullLiteral()),
                        Expr.block(Expr.throwStmt(
                                Expr.newInstance(Types.named("IllegalArgumentException"), List.of(errorMsg))))),
                Expr.returnStmt(Expr.identifier("result"))));

        return Ops.method("fromProtoValueOrThrow", enumType)
                .modifiers(Set.of(IR.PUBLIC(), IR.STATIC()))
                .addParameter("value", Types.INT())
                .body(body)
                .javadoc("Convert proto enum numeric value to this enum, throwing if not found.")
                .build();
    }
}
