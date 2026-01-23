package io.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.FieldMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SuspectedRenumber record.
 */
class SuspectedRenumberTest {

    @Nested
    @DisplayName("Record components")
    class RecordComponentTests {

        @Test
        @DisplayName("Should store all components correctly")
        void shouldStoreAllComponents() {
            FieldInfo v1 = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "parent_ref", 3, 5, v1, v2,
                SuspectedRenumber.Confidence.HIGH
            );

            assertThat(sr.messageName()).isEqualTo("Order");
            assertThat(sr.fieldName()).isEqualTo("parent_ref");
            assertThat(sr.v1Number()).isEqualTo(3);
            assertThat(sr.v2Number()).isEqualTo(5);
            assertThat(sr.v1Field()).isEqualTo(v1);
            assertThat(sr.v2Field()).isEqualTo(v2);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should support MEDIUM confidence")
        void shouldSupportMediumConfidence() {
            FieldInfo v1 = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "amount", 4, 6, v1, v2,
                SuspectedRenumber.Confidence.MEDIUM
            );

            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }
    }

    @Nested
    @DisplayName("toSuggestedMapping()")
    class ToSuggestedMappingTests {

        @Test
        @DisplayName("Should create FieldMapping with correct message and field name")
        void shouldCreateFieldMappingWithCorrectNames() {
            FieldInfo v1 = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "parent_ref", 3, 5, v1, v2,
                SuspectedRenumber.Confidence.HIGH
            );

            FieldMapping mapping = sr.toSuggestedMapping("v1", "v2");

            assertThat(mapping.getMessage()).isEqualTo("Order");
            assertThat(mapping.getFieldName()).isEqualTo("parent_ref");
        }

        @Test
        @DisplayName("Should create FieldMapping with version-specific numbers")
        void shouldCreateFieldMappingWithVersionNumbers() {
            FieldInfo v1 = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "amount", 4, 6, v1, v2,
                SuspectedRenumber.Confidence.MEDIUM
            );

            FieldMapping mapping = sr.toSuggestedMapping("v1", "v2");

            assertThat(mapping.getVersionNumbers()).containsEntry("v1", 4);
            assertThat(mapping.getVersionNumbers()).containsEntry("v2", 6);
        }

        @Test
        @DisplayName("Should use provided version names in mapping")
        void shouldUseProvidedVersionNames() {
            FieldInfo v1 = createField("ref", 10, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("ref", 15, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Payment", "ref", 10, 15, v1, v2,
                SuspectedRenumber.Confidence.HIGH
            );

            FieldMapping mapping = sr.toSuggestedMapping("production", "development");

            assertThat(mapping.getVersionNumbers()).containsEntry("production", 10);
            assertThat(mapping.getVersionNumbers()).containsEntry("development", 15);
        }
    }

    @Nested
    @DisplayName("getDescription()")
    class GetDescriptionTests {

        @Test
        @DisplayName("Should format description with message, field, numbers, and confidence")
        void shouldFormatDescriptionCorrectly() {
            FieldInfo v1 = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "parent_ref", 3, 5, v1, v2,
                SuspectedRenumber.Confidence.HIGH
            );

            String desc = sr.getDescription();

            assertThat(desc).isEqualTo("Order.parent_ref: #3 -> #5 [HIGH]");
        }

        @Test
        @DisplayName("Should format MEDIUM confidence description")
        void shouldFormatMediumConfidenceDescription() {
            FieldInfo v1 = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2 = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            SuspectedRenumber sr = new SuspectedRenumber(
                "Order", "amount", 4, 6, v1, v2,
                SuspectedRenumber.Confidence.MEDIUM
            );

            String desc = sr.getDescription();

            assertThat(desc).isEqualTo("Order.amount: #4 -> #6 [MEDIUM]");
        }
    }

    @Nested
    @DisplayName("Confidence enum")
    class ConfidenceEnumTests {

        @Test
        @DisplayName("HIGH confidence should exist")
        void highConfidenceShouldExist() {
            assertThat(SuspectedRenumber.Confidence.HIGH).isNotNull();
        }

        @Test
        @DisplayName("MEDIUM confidence should exist")
        void mediumConfidenceShouldExist() {
            assertThat(SuspectedRenumber.Confidence.MEDIUM).isNotNull();
        }

        @Test
        @DisplayName("Should have exactly two confidence levels")
        void shouldHaveExactlyTwoLevels() {
            assertThat(SuspectedRenumber.Confidence.values()).hasSize(2);
        }
    }

    // ========== Helper Methods ==========

    private FieldInfo createField(String name, int number, Type type, Label label) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(label)
            .build();
        return new FieldInfo(proto);
    }
}
