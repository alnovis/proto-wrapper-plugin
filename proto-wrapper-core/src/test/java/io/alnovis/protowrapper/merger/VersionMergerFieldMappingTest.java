package io.alnovis.protowrapper.merger;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.merger.VersionMerger.MergerConfig;
import io.alnovis.protowrapper.model.FieldMapping;
import io.alnovis.protowrapper.model.MessageInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FieldMapping feature — name-based and explicit field matching.
 */
class VersionMergerFieldMappingTest {

    @Test
    void shouldMatchFieldsByName_sameNameDifferentNumbers() {
        // v1: parent_order = 17
        // v2: parent_order = 15
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED),
                createField("parent_order", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED),
                createField("parent_order", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Order", "parent_order"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();

        // Should have 2 fields: id (number-matched) and parent_order (name-matched)
        assertThat(order.get().getFields()).hasSize(2);

        // Find the name-matched field
        Optional<MergedField> parentOrder = order.get().getFields().stream()
                .filter(f -> f.getName().equals("parent_order"))
                .findFirst();
        assertThat(parentOrder).isPresent();
        assertThat(parentOrder.get().isNameMapped()).isTrue();
        assertThat(parentOrder.get().getPresentInVersions()).containsExactly("v1", "v2");
        assertThat(parentOrder.get().getNumberForVersion("v1")).isEqualTo(17);
        assertThat(parentOrder.get().getNumberForVersion("v2")).isEqualTo(15);
        assertThat(parentOrder.get().getConflictType()).isEqualTo(MergedField.ConflictType.NONE);
    }

    @Test
    void shouldMatchFieldsByExplicitNumbers() {
        // v1: amount = 17 (int32)
        // v2: amount = 15 (int32)
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Payment",
                createField("amount", 17, Type.TYPE_INT32, Label.LABEL_REQUIRED)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Payment",
                createField("amount", 15, Type.TYPE_INT32, Label.LABEL_REQUIRED)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Payment", "amount",
                Map.of("v1", 17, "v2", 15)));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> payment = schema.getMessage("Payment");
        assertThat(payment).isPresent();
        assertThat(payment.get().getFields()).hasSize(1);

        MergedField amount = payment.get().getFields().get(0);
        assertThat(amount.getName()).isEqualTo("amount");
        assertThat(amount.isNameMapped()).isTrue();
        assertThat(amount.getPresentInVersions()).containsExactly("v1", "v2");
        assertThat(amount.getNumberForVersion("v1")).isEqualTo(17);
        assertThat(amount.getNumberForVersion("v2")).isEqualTo(15);
    }

    @Test
    void shouldRemoveOverriddenFieldsFromNumberBasedMatching() {
        // v1: shift_doc = 15 (int32), parent_order = 17 (message)
        // v2: parent_order = 15 (message)
        // Without mapping: field 15 gets type conflict (int32 vs message)
        // With mapping: parent_order matches by name, shift_doc is v1-only
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("shift_doc", 15, Type.TYPE_INT32, Label.LABEL_OPTIONAL),
                createField("parent_order", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("parent_order", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Order", "parent_order"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();
        assertThat(order.get().getFields()).hasSize(2);

        // parent_order should be present in both versions without type conflict
        Optional<MergedField> parentOrder = order.get().getFields().stream()
                .filter(f -> f.getName().equals("parent_order"))
                .findFirst();
        assertThat(parentOrder).isPresent();
        assertThat(parentOrder.get().isNameMapped()).isTrue();
        assertThat(parentOrder.get().getPresentInVersions()).containsExactlyInAnyOrder("v1", "v2");
        assertThat(parentOrder.get().getConflictType()).isEqualTo(MergedField.ConflictType.NONE);

        // shift_doc should be v1-only
        Optional<MergedField> shiftDoc = order.get().getFields().stream()
                .filter(f -> f.getName().equals("shift_doc"))
                .findFirst();
        assertThat(shiftDoc).isPresent();
        assertThat(shiftDoc.get().isNameMapped()).isFalse();
        assertThat(shiftDoc.get().getPresentInVersions()).containsExactly("v1");
    }

    @Test
    void shouldDetectTypeConflictOnNameMatchedField() {
        // v1: parent_order = 17 (int32)
        // v2: parent_order = 15 (message)
        // Type conflict should still be detected even for name-matched fields
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("parent_order", 17, Type.TYPE_INT32, Label.LABEL_OPTIONAL)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("parent_order", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Order", "parent_order"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();

        Optional<MergedField> parentOrder = order.get().getFields().stream()
                .filter(f -> f.getName().equals("parent_order"))
                .findFirst();
        assertThat(parentOrder).isPresent();
        assertThat(parentOrder.get().isNameMapped()).isTrue();
        assertThat(parentOrder.get().getConflictType()).isEqualTo(MergedField.ConflictType.PRIMITIVE_MESSAGE);
    }

    @Test
    void shouldWarnOnMappingWithNoMatch() {
        // No field named "nonexistent" in the message
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Order", "nonexistent"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1));

        // Should not crash, field is just not matched
        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();
        assertThat(order.get().getFields()).hasSize(1);
        assertThat(order.get().getFields().get(0).getName()).isEqualTo("id");
    }

    @Test
    void shouldIgnoreMappingForWrongMessage() {
        // Mapping for "Payment" should not affect "Order"
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("parent_order", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("parent_order", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Payment", "parent_order"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();
        // Without proper mapping, fields have different numbers - should be 2 separate version-specific fields
        assertThat(order.get().getFields()).hasSize(2);
    }

    @Test
    void shouldHandleMappingMatchingSingleField() {
        // Only v1 has the field - mapping should warn but not crash
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("parent_order", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED)));

        MergerConfig config = new MergerConfig();
        config.addFieldMapping(new FieldMapping("Order", "parent_order"));

        VersionMerger merger = new VersionMerger(config);
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();

        // parent_order should still exist as v1-only (not name-mapped since only 1 match)
        Optional<MergedField> parentOrder = order.get().getFields().stream()
                .filter(f -> f.getName().equals("parent_order"))
                .findFirst();
        assertThat(parentOrder).isPresent();
        assertThat(parentOrder.get().isNameMapped()).isFalse();
        assertThat(parentOrder.get().getPresentInVersions()).containsExactly("v1");
    }

    @Test
    void shouldNotBeNameMappedWithoutConfig() {
        // Normal number-based merge - isNameMapped should be false
        VersionSchema v1 = createVersionSchema("v1");
        v1.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED)));

        VersionSchema v2 = createVersionSchema("v2");
        v2.addMessage(createMessage("Order",
                createField("id", 1, Type.TYPE_INT64, Label.LABEL_REQUIRED)));

        VersionMerger merger = new VersionMerger();
        MergedSchema schema = merger.merge(Arrays.asList(v1, v2));

        Optional<MergedMessage> order = schema.getMessage("Order");
        assertThat(order).isPresent();
        assertThat(order.get().getFields()).hasSize(1);
        assertThat(order.get().getFields().get(0).isNameMapped()).isFalse();
        assertThat(order.get().getFields().get(0).getNumberPerVersion()).isEmpty();
    }

    // ========================= Helper Methods =========================

    private VersionSchema createVersionSchema(String version) {
        return new VersionSchema(version);
    }

    private MessageInfo createMessage(String name, FieldDescriptorProto... fields) {
        DescriptorProto.Builder builder = DescriptorProto.newBuilder().setName(name);
        for (FieldDescriptorProto field : fields) {
            builder.addField(field);
        }
        return new MessageInfo(builder.build(), "test.proto");
    }

    private FieldDescriptorProto createField(String name, int number, Type type, Label label) {
        return FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .setLabel(label)
                .build();
    }
}
