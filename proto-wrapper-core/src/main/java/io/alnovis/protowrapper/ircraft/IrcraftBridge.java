package io.alnovis.protowrapper.ircraft;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.core.TypeRef$;
import io.alnovis.ircraft.dialect.proto.ops.*;
import io.alnovis.ircraft.dialect.proto.types.ConflictType;
import io.alnovis.ircraft.dialect.proto.types.ProtoSyntax;
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

        // Convert conflict enums
        var conflictEnums = new ArrayList<ConflictEnumOp>();
        for (ConflictEnumInfo cei : schema.getConflictEnums()) {
            conflictEnums.add(convertConflictEnum(cei));
        }

        // Convert version syntax map
        var syntaxMap = new HashMap<String, ProtoSyntax>();
        for (String version : schema.getVersions()) {
            var ps = schema.getVersionSyntax(version);
            if (ps != null) {
                syntaxMap.put(version, mapProtoSyntax(ps));
            }
        }

        // Build attributes with equivalentEnumMappings
        var attrs = AttributeMap.empty();
        var eqMappings = schema.getEquivalentEnumMappings();
        if (eqMappings != null && !eqMappings.isEmpty()) {
            var entries = new ArrayList<String>();
            for (var entry : eqMappings.entrySet()) {
                entries.add(entry.getKey() + "=" + entry.getValue());
            }
            attrs = attrs.$plus(new Attribute.StringListAttr(
                    "proto.equivalentEnumMappings",
                    scalaList(entries)
            ));
        }

        return new SchemaOp(
                scalaList(schema.getVersions()),
                scalaMap(syntaxMap),
                scalaVector(List.of(
                        region("messages", messages),
                        region("enums", enums),
                        region("conflictEnums", conflictEnums)
                )),
                attrs,
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
                scalaVector(List.of(
                        region("fields", fields),
                        region("oneofs", oneofs),
                        region("nestedMessages", nestedMessages),
                        region("nestedEnums", nestedEnums)
                )),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private FieldOp convertField(MergedField field) {
        // Build attributes for WKT and version-specific type info
        var attrs = AttributeMap.empty();

        // Well-known type
        var wkt = field.getWellKnownType();
        if (wkt != null) {
            attrs = attrs.$plus(new Attribute.StringAttr("proto.wellKnownType", wkt.name()));
        }

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
                scalaMap(field.getTypesPerVersion()),
                attrs,
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
                scalaVector(List.of(region("fields", fields))),
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
                scalaVector(List.of(region("values", values))),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private ConflictEnumOp convertConflictEnum(ConflictEnumInfo cei) {
        var values = new ArrayList<EnumValueOp>();
        for (ConflictEnumInfo.EnumValue value : cei.getValues()) {
            values.add(new EnumValueOp(
                    value.name(),
                    value.number(),
                    scalaSet(Set.of()), // conflict enum values are version-agnostic
                    AttributeMap.empty(),
                    scala.Option.empty()
            ));
        }

        return new ConflictEnumOp(
                cei.getFieldName(),
                cei.getEnumName(),
                cei.getMessageName(),
                scalaVector(List.of(region("values", values))),
                AttributeMap.empty(),
                scala.Option.empty()
        );
    }

    private ProtoSyntax mapProtoSyntax(io.alnovis.protowrapper.model.ProtoSyntax ps) {
        return switch (ps) {
            case PROTO2 -> ProtoSyntax.valueOf("Proto2");
            default -> ProtoSyntax.valueOf("Proto3");
        };
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

    // ── Scala collection & region helpers ─────────────────────────────────

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

    @SuppressWarnings("unchecked")
    private <K, V> scala.collection.immutable.Map<K, V> scalaMap(Map<K, V> javaMap) {
        var result = (scala.collection.immutable.Map<K, V>) scala.collection.immutable.Map$.MODULE$.empty();
        for (var entry : javaMap.entrySet()) {
            result = result.$plus(new scala.Tuple2<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Region region(String name, List<? extends Operation> ops) {
        var scalaOps = scala.jdk.CollectionConverters.ListHasAsScala(
                (List<Operation>) (List<?>) ops
        ).asScala().toVector();
        return new Region(name, scalaOps);
    }
}
