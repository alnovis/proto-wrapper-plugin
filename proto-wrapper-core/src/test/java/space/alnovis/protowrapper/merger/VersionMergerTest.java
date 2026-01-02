package space.alnovis.protowrapper.merger;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.model.EnumInfo;
import space.alnovis.protowrapper.model.MessageInfo;
import space.alnovis.protowrapper.model.MergedSchema;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionMergerTest {

    private VersionMerger merger;

    @BeforeEach
    void setUp() {
        merger = new VersionMerger();
    }

    @Test
    void shouldThrowExceptionForEmptySchemas() {
        assertThatThrownBy(() -> merger.merge(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one schema required");
    }

    @Test
    void shouldMergeSingleVersionSchema() {
        VersionSchema schema = createVersionSchema("v1");
        schema.addMessage(createSimpleMessage("Money", "amount", Type.TYPE_INT64));

        MergedSchema merged = merger.merge(Collections.singletonList(schema));

        assertThat(merged.getVersions()).containsExactly("v1");
        assertThat(merged.getMessages()).hasSize(1);

        Optional<MergedMessage> money = merged.getMessage("Money");
        assertThat(money).isPresent();
        assertThat(money.get().getPresentInVersions()).containsExactly("v1");
        assertThat(money.get().getFields()).hasSize(1);
        assertThat(money.get().getFields().get(0).getName()).isEqualTo("amount");
    }

    @Test
    void shouldMergeMultipleVersionsWithCommonMessage() {
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createSimpleMessage("Money", "amount", Type.TYPE_INT64));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createSimpleMessage("Money", "amount", Type.TYPE_INT64));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        assertThat(merged.getVersions()).containsExactly("v1", "v2");

        Optional<MergedMessage> money = merged.getMessage("Money");
        assertThat(money).isPresent();
        assertThat(money.get().getPresentInVersions()).containsExactly("v1", "v2");
    }

    @Test
    void shouldMergeMessagePresentOnlyInOneVersion() {
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createSimpleMessage("OldMessage", "field1", Type.TYPE_STRING));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createSimpleMessage("NewMessage", "field1", Type.TYPE_STRING));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        assertThat(merged.getMessage("OldMessage")).isPresent();
        assertThat(merged.getMessage("OldMessage").get().getPresentInVersions())
                .containsExactly("v1");

        assertThat(merged.getMessage("NewMessage")).isPresent();
        assertThat(merged.getMessage("NewMessage").get().getPresentInVersions())
                .containsExactly("v2");
    }

    @Test
    void shouldMergeFieldsFromDifferentVersions() {
        DescriptorProto.Builder v1Builder = DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED))
                .addField(createField("name", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL));

        DescriptorProto.Builder v2Builder = DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED))
                .addField(createField("name", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL))
                .addField(createField("description", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL));

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder.build(), "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder.build(), "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage request = merged.getMessage("Request").orElseThrow();
        assertThat(request.getFields()).hasSize(3);

        // id and name should be in both versions
        MergedField idField = findFieldByName(request, "id");
        assertThat(idField.getPresentInVersions()).containsExactly("v1", "v2");

        // description should only be in v2
        MergedField descField = findFieldByName(request, "description");
        assertThat(descField.getPresentInVersions()).containsExactly("v2");
    }

    @Test
    void shouldMergeEnums() {
        VersionSchema v1 = createVersionSchema("v1");
        v1.addEnum(createEnum("PaymentType", "CASH", 0, "CARD", 1));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addEnum(createEnum("PaymentType", "CASH", 0, "CARD", 1, "TARE", 2));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedEnum> paymentType = merged.getEnum("PaymentType");
        assertThat(paymentType).isPresent();
        assertThat(paymentType.get().getPresentInVersions()).containsExactly("v1", "v2");
        assertThat(paymentType.get().getValues()).hasSize(3);

        // TARE should only be in v2
        assertThat(paymentType.get().getValues().stream()
                .filter(v -> v.getName().equals("TARE"))
                .findFirst()
                .orElseThrow()
                .getPresentInVersions())
                .containsExactly("v2");
    }

    @Test
    void shouldMergeNestedMessages() {
        DescriptorProto nested = DescriptorProto.newBuilder()
                .setName("Item")
                .addField(createField("name", 1, Type.TYPE_STRING, Label.LABEL_REQUIRED))
                .build();

        DescriptorProto parent = DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED))
                .addNestedType(nested)
                .build();

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(parent, "test.proto"));

        MergedSchema merged = merger.merge(Collections.singletonList(v1));

        MergedMessage request = merged.getMessage("Request").orElseThrow();
        assertThat(request.getNestedMessages()).hasSize(1);
        assertThat(request.getNestedMessages().get(0).getName()).isEqualTo("Item");
    }

    @Test
    void shouldMergeNestedEnums() {
        EnumDescriptorProto nestedEnum = EnumDescriptorProto.newBuilder()
                .setName("ItemType")
                .addValue(EnumValueDescriptorProto.newBuilder().setName("PRODUCT").setNumber(0))
                .addValue(EnumValueDescriptorProto.newBuilder().setName("SERVICE").setNumber(1))
                .build();

        DescriptorProto parent = DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED))
                .addEnumType(nestedEnum)
                .build();

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(parent, "test.proto"));

        MergedSchema merged = merger.merge(Collections.singletonList(v1));

        MergedMessage request = merged.getMessage("Request").orElseThrow();
        assertThat(request.getNestedEnums()).hasSize(1);
        assertThat(request.getNestedEnums().get(0).getName()).isEqualTo("ItemType");
        assertThat(request.getNestedEnums().get(0).getValues()).hasSize(2);
    }

    @Test
    void shouldDetectTypeConflict() {
        DescriptorProto.Builder v1Builder = DescriptorProto.newBuilder()
                .setName("Data")
                .addField(createField("value", 1, Type.TYPE_INT32, Label.LABEL_REQUIRED));

        DescriptorProto.Builder v2Builder = DescriptorProto.newBuilder()
                .setName("Data")
                .addField(createField("value", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED));

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder.build(), "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder.build(), "test.proto"));

        // Should not throw but logs warning
        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage data = merged.getMessage("Data").orElseThrow();
        assertThat(data.getFields()).hasSize(1);
        // For WIDENING conflicts, the wider type (long) is used
        assertThat(data.getFields().get(0).getJavaType()).isEqualTo("long");
        assertThat(data.getFields().get(0).getConflictType()).isEqualTo(MergedField.ConflictType.WIDENING);
    }

    @Test
    void shouldDetectFloatDoubleConflict() {
        DescriptorProto.Builder v1Builder = DescriptorProto.newBuilder()
                .setName("Sensor")
                .addField(createField("temperature", 1, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL));

        DescriptorProto.Builder v2Builder = DescriptorProto.newBuilder()
                .setName("Sensor")
                .addField(createField("temperature", 1, Type.TYPE_DOUBLE, Label.LABEL_OPTIONAL));

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder.build(), "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder.build(), "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage sensor = merged.getMessage("Sensor").orElseThrow();
        assertThat(sensor.getFields()).hasSize(1);

        MergedField tempField = sensor.getFields().get(0);
        // For FLOAT_DOUBLE conflicts, the wider type (double) is used
        assertThat(tempField.getJavaType()).isEqualTo("double");
        assertThat(tempField.getConflictType()).isEqualTo(MergedField.ConflictType.FLOAT_DOUBLE);
    }

    @Test
    void shouldDetectFloatDoubleConflictWithMultipleVersions() {
        // v1: float, v2: double, v3: float
        DescriptorProto.Builder v1Builder = DescriptorProto.newBuilder()
                .setName("Measurement")
                .addField(createField("value", 1, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL));

        DescriptorProto.Builder v2Builder = DescriptorProto.newBuilder()
                .setName("Measurement")
                .addField(createField("value", 1, Type.TYPE_DOUBLE, Label.LABEL_OPTIONAL));

        DescriptorProto.Builder v3Builder = DescriptorProto.newBuilder()
                .setName("Measurement")
                .addField(createField("value", 1, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL));

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder.build(), "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder.build(), "test.proto"));

        VersionSchema v3 = createVersionSchema("v3");
        v3.addMessage(new MessageInfo(v3Builder.build(), "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2, v3));

        MergedMessage measurement = merged.getMessage("Measurement").orElseThrow();
        MergedField valueField = measurement.getFields().get(0);

        // Should be FLOAT_DOUBLE conflict with double as unified type
        assertThat(valueField.getConflictType()).isEqualTo(MergedField.ConflictType.FLOAT_DOUBLE);
        assertThat(valueField.getJavaType()).isEqualTo("double");
        assertThat(valueField.getPresentInVersions()).containsExactly("v1", "v2", "v3");
    }

    @Test
    void shouldDetectSignedUnsignedConflict() {
        // v1: int32 (signed)
        // v2: uint32 (unsigned)
        DescriptorProto v1Builder = DescriptorProto.newBuilder()
                .setName("Counter")
                .addField(createField("value", 1, Type.TYPE_INT32, Label.LABEL_REQUIRED))
                .build();

        DescriptorProto v2Builder = DescriptorProto.newBuilder()
                .setName("Counter")
                .addField(createField("value", 1, Type.TYPE_UINT32, Label.LABEL_REQUIRED))
                .build();

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder, "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder, "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage counter = merged.getMessage("Counter").orElseThrow();
        MergedField valueField = counter.getFields().get(0);

        // Should be SIGNED_UNSIGNED conflict with long as unified type
        assertThat(valueField.getConflictType()).isEqualTo(MergedField.ConflictType.SIGNED_UNSIGNED);
        assertThat(valueField.getJavaType()).isEqualTo("long");
        assertThat(valueField.getPresentInVersions()).containsExactly("v1", "v2");
    }

    @Test
    void shouldDetectSignedUnsignedConflictWithSint32() {
        // v1: sint32 (ZigZag encoding)
        // v2: int32 (regular varint)
        DescriptorProto v1Builder = DescriptorProto.newBuilder()
                .setName("Delta")
                .addField(createField("offset", 1, Type.TYPE_SINT32, Label.LABEL_REQUIRED))
                .build();

        DescriptorProto v2Builder = DescriptorProto.newBuilder()
                .setName("Delta")
                .addField(createField("offset", 1, Type.TYPE_INT32, Label.LABEL_REQUIRED))
                .build();

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder, "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder, "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage delta = merged.getMessage("Delta").orElseThrow();
        MergedField offsetField = delta.getFields().get(0);

        // Should be SIGNED_UNSIGNED conflict (different wire encodings)
        assertThat(offsetField.getConflictType()).isEqualTo(MergedField.ConflictType.SIGNED_UNSIGNED);
        assertThat(offsetField.getJavaType()).isEqualTo("long");
    }

    @Test
    void shouldDetectSignedUnsignedConflict64Bit() {
        // v1: int64 (signed)
        // v2: uint64 (unsigned)
        DescriptorProto v1Builder = DescriptorProto.newBuilder()
                .setName("BigCounter")
                .addField(createField("value", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED))
                .build();

        DescriptorProto v2Builder = DescriptorProto.newBuilder()
                .setName("BigCounter")
                .addField(createField("value", 1, Type.TYPE_UINT64, Label.LABEL_REQUIRED))
                .build();

        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(new MessageInfo(v1Builder, "test.proto"));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(new MessageInfo(v2Builder, "test.proto"));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        MergedMessage counter = merged.getMessage("BigCounter").orElseThrow();
        MergedField valueField = counter.getFields().get(0);

        // Should be SIGNED_UNSIGNED conflict with long as unified type
        assertThat(valueField.getConflictType()).isEqualTo(MergedField.ConflictType.SIGNED_UNSIGNED);
        assertThat(valueField.getJavaType()).isEqualTo("long");
    }

    @Test
    void shouldMergeWithConfig() {
        VersionMerger.MergerConfig config = new VersionMerger.MergerConfig()
                .addFieldNameMapping("old_name", "newName")
                .addTypeConflictResolution("value", "Long")
                .excludeMessage("ExcludedMessage")
                .excludeField("Data.excludedField");

        assertThat(config.getJavaName("old_name")).isEqualTo("newName");
        assertThat(config.getJavaName("unknown")).isNull();
        assertThat(config.resolveTypeConflict("value", "int", "long")).isEqualTo("Long");
        assertThat(config.isMessageExcluded("ExcludedMessage")).isTrue();
        assertThat(config.isMessageExcluded("OtherMessage")).isFalse();
        assertThat(config.isFieldExcluded("Data", "excludedField")).isTrue();
        assertThat(config.isFieldExcluded("Data", "otherField")).isFalse();
    }

    // Helper methods

    private VersionSchema createVersionSchema(String version) {
        return new VersionSchema(version);
    }

    private MessageInfo createSimpleMessage(String name, String fieldName, Type fieldType) {
        DescriptorProto proto = DescriptorProto.newBuilder()
                .setName(name)
                .addField(createField(fieldName, 1, fieldType, Label.LABEL_REQUIRED))
                .build();
        return new MessageInfo(proto, "test.proto");
    }

    private FieldDescriptorProto createField(String name, int number, Type type, Label label) {
        return FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .setLabel(label)
                .build();
    }

    private EnumInfo createEnum(String name, Object... valuesAndNumbers) {
        EnumDescriptorProto.Builder builder = EnumDescriptorProto.newBuilder()
                .setName(name);
        for (int i = 0; i < valuesAndNumbers.length; i += 2) {
            builder.addValue(EnumValueDescriptorProto.newBuilder()
                    .setName((String) valuesAndNumbers[i])
                    .setNumber((Integer) valuesAndNumbers[i + 1]));
        }
        return new EnumInfo(builder.build());
    }

    private MergedField findFieldByName(MergedMessage message, String name) {
        return message.getFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
    }
}
