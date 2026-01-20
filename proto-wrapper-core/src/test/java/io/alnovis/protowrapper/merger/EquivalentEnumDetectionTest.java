package io.alnovis.protowrapper.merger;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.MessageInfo;
import io.alnovis.protowrapper.model.MergedSchema;
import io.alnovis.protowrapper.model.MergedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for equivalent enum detection feature.
 *
 * <p>This tests the scenario where a nested enum in one version
 * corresponds to a top-level enum in another version.</p>
 */
class EquivalentEnumDetectionTest {

    private VersionMerger merger;

    @BeforeEach
    void setUp() {
        merger = new VersionMerger();
    }

    @Test
    void shouldDetectEquivalentNestedAndTopLevelEnums() {
        // v1: TaxType is nested in Product
        EnumDescriptorProto nestedEnum = createEnumProto("TaxType", "VAT", 100);
        DescriptorProto messageWithNestedEnum = DescriptorProto.newBuilder()
                .setName("Product")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addEnumType(nestedEnum)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(messageWithNestedEnum, "test"));

        // v2: TaxType is top-level (same values)
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Product")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("TaxType", "VAT", 100)));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // Top-level enum should exist
        assertThat(merged.getEnum("TaxType")).isPresent();

        // Nested enum should be removed from message
        MergedMessage message = merged.getMessage("Product").orElseThrow();
        assertThat(message.getNestedEnums()).isEmpty();

        // Mapping should be registered
        assertThat(merged.hasEquivalentTopLevelEnum("Product.TaxType")).isTrue();
        assertThat(merged.getEquivalentTopLevelEnum("Product.TaxType")).isEqualTo("TaxType");
    }

    @Test
    void shouldNotMergeNonEquivalentEnums() {
        // v1: TaxType nested with value VAT=100
        EnumDescriptorProto nestedEnum = createEnumProto("TaxType", "VAT", 100);
        DescriptorProto messageWithNestedEnum = DescriptorProto.newBuilder()
                .setName("Response")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addEnumType(nestedEnum)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(messageWithNestedEnum, "test"));

        // v2: TaxType top-level with DIFFERENT value
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Response")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("TaxType", "VAT", 200))); // Different number!

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // Both enums should exist (not merged)
        assertThat(merged.getEnum("TaxType")).isPresent();

        // Nested enum should remain in message
        MergedMessage message = merged.getMessage("Response").orElseThrow();
        assertThat(message.getNestedEnums()).hasSize(1);

        // No mapping should be registered
        assertThat(merged.hasEquivalentTopLevelEnum("Response.TaxType")).isFalse();
    }

    @Test
    void shouldNotMergeEnumsWithDifferentNames() {
        // v1: StatusType nested
        EnumDescriptorProto nestedEnum = createEnumProto("StatusType", "ACTIVE", 1);
        DescriptorProto messageWithNestedEnum = DescriptorProto.newBuilder()
                .setName("Response")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addEnumType(nestedEnum)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(messageWithNestedEnum, "test"));

        // v2: DifferentName top-level (won't match)
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Response")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("DifferentName", "ACTIVE", 1)));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // Nested enum should remain
        MergedMessage message = merged.getMessage("Response").orElseThrow();
        assertThat(message.getNestedEnums()).hasSize(1);
        assertThat(message.getNestedEnums().get(0).getName()).isEqualTo("StatusType");
    }

    @Test
    void shouldHandleMultipleEquivalentEnums() {
        // v1: Two nested enums
        EnumDescriptorProto nestedEnum1 = createEnumProto("TaxType", "VAT", 100);
        EnumDescriptorProto nestedEnum2 = createEnumProto("PaymentType", "CASH", 0, "CARD", 1);
        DescriptorProto message = DescriptorProto.newBuilder()
                .setName("Transaction")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addEnumType(nestedEnum1)
                .addEnumType(nestedEnum2)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(message, "test"));

        // v2: Both as top-level
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Transaction")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("TaxType", "VAT", 100)));
        v2.addEnum(new EnumInfo(createEnumProto("PaymentType", "CASH", 0, "CARD", 1)));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // Both top-level enums should exist
        assertThat(merged.getEnum("TaxType")).isPresent();
        assertThat(merged.getEnum("PaymentType")).isPresent();

        // No nested enums in message
        MergedMessage txn = merged.getMessage("Transaction").orElseThrow();
        assertThat(txn.getNestedEnums()).isEmpty();

        // Both mappings should be registered
        assertThat(merged.hasEquivalentTopLevelEnum("Transaction.TaxType")).isTrue();
        assertThat(merged.hasEquivalentTopLevelEnum("Transaction.PaymentType")).isTrue();
    }

    @Test
    void shouldPreserveNonEquivalentNestedEnums() {
        // v1: Two nested enums, only one has top-level equivalent
        EnumDescriptorProto nestedEnum1 = createEnumProto("TaxType", "VAT", 100);
        EnumDescriptorProto nestedEnum2 = createEnumProto("LocalType", "TYPE_A", 0);
        DescriptorProto message = DescriptorProto.newBuilder()
                .setName("Data")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addEnumType(nestedEnum1)
                .addEnumType(nestedEnum2)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(message, "test"));

        // v2: Only TaxType as top-level
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Data")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("TaxType", "VAT", 100)));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // TaxType should be top-level
        assertThat(merged.getEnum("TaxType")).isPresent();

        // LocalType should remain nested
        MergedMessage data = merged.getMessage("Data").orElseThrow();
        assertThat(data.getNestedEnums()).hasSize(1);
        assertThat(data.getNestedEnums().get(0).getName()).isEqualTo("LocalType");

        // Only TaxType mapping
        assertThat(merged.hasEquivalentTopLevelEnum("Data.TaxType")).isTrue();
        assertThat(merged.hasEquivalentTopLevelEnum("Data.LocalType")).isFalse();
    }

    @Test
    void shouldNotMergeDeeplyNestedEnumsWithTopLevel() {
        // Current limitation: equivalent enum detection only works for enums
        // directly nested in top-level messages, not for deeply nested enums.
        // This test documents this behavior.

        // Create a deeply nested structure: Request.Item.ItemType
        EnumDescriptorProto nestedEnum = createEnumProto("ItemType", "PRODUCT", 0, "SERVICE", 1);
        DescriptorProto innerMessage = DescriptorProto.newBuilder()
                .setName("Item")
                .addField(createField("name", 1, Type.TYPE_STRING))
                .addEnumType(nestedEnum)
                .build();
        DescriptorProto outerMessage = DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addNestedType(innerMessage)
                .build();

        VersionSchema v1 = new VersionSchema("v1");
        v1.addMessage(new MessageInfo(outerMessage, "test"));

        // v2: ItemType as top-level (same values, but won't be merged with deeply nested)
        VersionSchema v2 = new VersionSchema("v2");
        v2.addMessage(new MessageInfo(DescriptorProto.newBuilder()
                .setName("Request")
                .addField(createField("id", 1, Type.TYPE_INT64))
                .addNestedType(DescriptorProto.newBuilder()
                        .setName("Item")
                        .addField(createField("name", 1, Type.TYPE_STRING)))
                .build(), "test"));
        v2.addEnum(new EnumInfo(createEnumProto("ItemType", "PRODUCT", 0, "SERVICE", 1)));

        MergedSchema merged = merger.merge(Arrays.asList(v1, v2));

        // Top-level enum should exist
        assertThat(merged.getEnum("ItemType")).isPresent();

        // Check nested structure
        MergedMessage request = merged.getMessage("Request").orElseThrow();
        assertThat(request.getNestedMessages()).hasSize(1);

        MergedMessage item = request.getNestedMessages().get(0);
        assertThat(item.getName()).isEqualTo("Item");
        // Deeply nested enum is NOT merged - this is the current limitation
        assertThat(item.getNestedEnums()).hasSize(1);
        assertThat(item.getNestedEnums().get(0).getName()).isEqualTo("ItemType");
    }

    // Helper methods

    private FieldDescriptorProto createField(String name, int number, Type type) {
        return FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
    }

    private EnumDescriptorProto createEnumProto(String name, Object... valuesAndNumbers) {
        EnumDescriptorProto.Builder builder = EnumDescriptorProto.newBuilder()
                .setName(name);
        for (int i = 0; i < valuesAndNumbers.length; i += 2) {
            builder.addValue(EnumValueDescriptorProto.newBuilder()
                    .setName((String) valuesAndNumbers[i])
                    .setNumber((Integer) valuesAndNumbers[i + 1]));
        }
        return builder.build();
    }
}
