package io.alnovis.protowrapper.ircraft;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.core.TypeRef$;
import io.alnovis.ircraft.dialect.proto.ops.*;
import io.alnovis.ircraft.dialect.proto.types.ConflictType;
import io.alnovis.protowrapper.model.*;

import java.util.*;

/**
 * Bridge between proto-wrapper-plugin's MergedSchema and ircraft's Proto Dialect IR.
 *
 * <p>Converts Java model classes (MergedSchema, MergedMessage, etc.)
 * into ircraft SchemaOp tree using direct case class construction.</p>
 *
 * @since 3.0.0
 */
public class IrcraftBridge {

    /**
     * Convert a MergedSchema to an ircraft SchemaOp.
     */
    public SchemaOp toProtoIR(MergedSchema schema) {
        var messages = new ArrayList<MessageOp>();
        for (MergedMessage msg : schema.getMessages()) {
            messages.add(convertMessage(msg));
        }

        var enums = new ArrayList<EnumOp>();
        for (MergedEnum en : schema.getEnums()) {
            enums.add(convertEnum(en));
        }

        return new SchemaOp(
                scalaList(schema.getVersions()),
                scala.collection.immutable.Map$.MODULE$.empty(),
                scalaVector(messages),
                scalaVector(enums),
                scalaVector(List.of()),       // conflictEnums (TODO: convert from schema)
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private MessageOp convertMessage(MergedMessage msg) {
        var fields = new ArrayList<FieldOp>();
        for (MergedField field : msg.getFields()) {
            fields.add(convertField(field));
        }

        var oneofs = new ArrayList<OneofOp>();
        for (MergedOneof oneof : msg.getOneofGroups()) {
            oneofs.add(convertOneof(oneof));
        }

        var nestedMessages = new ArrayList<MessageOp>();
        for (MergedMessage nested : msg.getNestedMessages()) {
            nestedMessages.add(convertMessage(nested));
        }

        var nestedEnums = new ArrayList<EnumOp>();
        for (MergedEnum nested : msg.getNestedEnums()) {
            nestedEnums.add(convertEnum(nested));
        }

        return new MessageOp(
                msg.getName(),
                scalaSet(msg.getPresentInVersions()),
                scalaVector(fields),
                scalaVector(oneofs),
                scalaVector(nestedMessages),
                scalaVector(nestedEnums),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private FieldOp convertField(MergedField field) {
        return new FieldOp(
                field.getName(),
                field.getJavaName(),
                field.getNumber(),
                mapType(field.getJavaType()),
                mapConflictType(field.getConflictType()),
                scalaSet(field.getPresentInVersions()),
                field.isOptional(),
                field.isRepeated(),
                field.isMap(),
                scala.collection.immutable.Map$.MODULE$.empty(), // typesPerVersion
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private OneofOp convertOneof(MergedOneof oneof) {
        var fields = new ArrayList<FieldOp>();
        for (MergedField field : oneof.getFields()) {
            fields.add(convertField(field));
        }

        return new OneofOp(
                oneof.getProtoName(),
                oneof.getJavaName(),
                oneof.getCaseEnumName(),
                scalaSet(oneof.getPresentInVersions()),
                scalaVector(fields),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private EnumOp convertEnum(MergedEnum en) {
        var values = new ArrayList<EnumValueOp>();
        for (MergedEnumValue value : en.getValues()) {
            values.add(new EnumValueOp(
                    value.getName(),
                    value.getNumber(),
                    scalaSet(value.getPresentInVersions()),
                    AttributeMap.empty(),
                    scala.Option.empty()
            ));
        }

        return new EnumOp(
                en.getName(),
                scalaSet(en.getPresentInVersions()),
                scalaVector(values),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    // ── Type mapping ─────────────────────────────────────────────────────

    private static final TypeRef$ TR = TypeRef$.MODULE$;

    private TypeRef mapType(String javaType) {
        if (javaType == null) return TR.STRING();
        return switch (javaType) {
            case "int" -> TR.INT();
            case "long" -> TR.LONG();
            case "float" -> TR.FLOAT();
            case "double" -> TR.DOUBLE();
            case "boolean" -> TR.BOOL();
            case "String" -> TR.STRING();
            case "byte[]" -> TR.BYTES();
            default -> new TypeRef.NamedType(javaType);
        };
    }

    private ConflictType mapConflictType(MergedField.ConflictType ct) {
        if (ct == null) return ConflictType.valueOf("None");
        return switch (ct) {
            case NONE -> ConflictType.valueOf("None");
            case INT_ENUM -> ConflictType.valueOf("IntEnum");
            case ENUM_ENUM -> ConflictType.valueOf("EnumEnum");
            case WIDENING -> ConflictType.valueOf("Widening");
            case FLOAT_DOUBLE -> ConflictType.valueOf("FloatDouble");
            case SIGNED_UNSIGNED -> ConflictType.valueOf("SignedUnsigned");
            case REPEATED_SINGLE -> ConflictType.valueOf("RepeatedSingle");
            case NARROWING -> ConflictType.valueOf("Narrowing");
            case STRING_BYTES -> ConflictType.valueOf("StringBytes");
            case PRIMITIVE_MESSAGE -> ConflictType.valueOf("PrimitiveMessage");
            case OPTIONAL_REQUIRED -> ConflictType.valueOf("OptionalRequired");
            case INCOMPATIBLE -> ConflictType.valueOf("Incompatible");
        };
    }

    // ── Scala collection helpers ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> scala.collection.immutable.List<T> scalaList(Collection<T> javaCollection) {
        return scala.jdk.CollectionConverters.ListHasAsScala(
                new ArrayList<>(javaCollection)
        ).asScala().toList();
    }

    @SuppressWarnings("unchecked")
    private <T> scala.collection.immutable.Vector<T> scalaVector(List<T> javaList) {
        return scala.jdk.CollectionConverters.ListHasAsScala(javaList)
                .asScala().toVector();
    }

    @SuppressWarnings("unchecked")
    private <T> scala.collection.immutable.Set<T> scalaSet(Set<T> javaSet) {
        return scala.jdk.CollectionConverters.SetHasAsScala(javaSet)
                .asScala().toSet();
    }
}
