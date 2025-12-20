package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MergedSchemaTest {

    private MergedSchema schema;

    @BeforeEach
    void setUp() {
        schema = new MergedSchema(Arrays.asList("v1", "v2"));
    }

    @Test
    void shouldStoreVersions() {
        assertThat(schema.getVersions()).containsExactly("v1", "v2");
    }

    @Test
    void shouldAddAndRetrieveMessages() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");
        schema.addMessage(message);

        assertThat(schema.getMessage("Request")).isPresent();
        assertThat(schema.getMessage("Unknown")).isEmpty();
        assertThat(schema.getMessages()).hasSize(1);
    }

    @Test
    void shouldAddAndRetrieveEnums() {
        MergedEnum enumType = new MergedEnum("Status");
        enumType.addVersion("v1");
        schema.addEnum(enumType);

        assertThat(schema.getEnum("Status")).isPresent();
        assertThat(schema.getEnum("Unknown")).isEmpty();
        assertThat(schema.getEnums()).hasSize(1);
    }

    // Equivalent enum mapping tests

    @Test
    void shouldAddEquivalentEnumMapping() {
        schema.addEquivalentEnumMapping("Response.TaxType", "TaxType");

        assertThat(schema.hasEquivalentTopLevelEnum("Response.TaxType")).isTrue();
        assertThat(schema.getEquivalentTopLevelEnum("Response.TaxType")).isEqualTo("TaxType");
    }

    @Test
    void shouldReturnFalseForNonExistentMapping() {
        assertThat(schema.hasEquivalentTopLevelEnum("Unknown.Type")).isFalse();
        assertThat(schema.getEquivalentTopLevelEnum("Unknown.Type")).isNull();
    }

    @Test
    void shouldReturnAllEquivalentMappings() {
        schema.addEquivalentEnumMapping("Response.TaxType", "TaxType");
        schema.addEquivalentEnumMapping("Request.PaymentType", "PaymentType");

        assertThat(schema.getEquivalentEnumMappings())
                .hasSize(2)
                .containsEntry("Response.TaxType", "TaxType")
                .containsEntry("Request.PaymentType", "PaymentType");
    }

    // findMessageByPath tests

    @Test
    void shouldFindTopLevelMessage() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");
        schema.addMessage(message);

        Optional<MergedMessage> found = schema.findMessageByPath("Request");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Request");
    }

    @Test
    void shouldFindNestedMessage() {
        MergedMessage parent = new MergedMessage("Order");
        parent.addVersion("v1");

        MergedMessage nested = new MergedMessage("ShippingInfo");
        nested.addVersion("v1");
        parent.addNestedMessage(nested);

        schema.addMessage(parent);

        Optional<MergedMessage> found = schema.findMessageByPath("Order.ShippingInfo");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ShippingInfo");
    }

    @Test
    void shouldFindDeeplyNestedMessage() {
        MergedMessage level1 = new MergedMessage("Outer");
        level1.addVersion("v1");

        MergedMessage level2 = new MergedMessage("Middle");
        level2.addVersion("v1");
        level1.addNestedMessage(level2);

        MergedMessage level3 = new MergedMessage("Inner");
        level3.addVersion("v1");
        level2.addNestedMessage(level3);

        schema.addMessage(level1);

        Optional<MergedMessage> found = schema.findMessageByPath("Outer.Middle.Inner");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Inner");
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");
        schema.addMessage(message);

        assertThat(schema.findMessageByPath("Unknown")).isEmpty();
        assertThat(schema.findMessageByPath("Request.Unknown")).isEmpty();
        assertThat(schema.findMessageByPath(null)).isEmpty();
        assertThat(schema.findMessageByPath("")).isEmpty();
    }

    // findEnumByPath tests

    @Test
    void shouldFindTopLevelEnum() {
        MergedEnum enumType = new MergedEnum("PaymentType");
        enumType.addVersion("v1");
        schema.addEnum(enumType);

        Optional<MergedEnum> found = schema.findEnumByPath("PaymentType");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("PaymentType");
    }

    @Test
    void shouldFindNestedEnum() {
        MergedMessage parent = new MergedMessage("Request");
        parent.addVersion("v1");

        MergedEnum nestedEnum = new MergedEnum("ItemType");
        nestedEnum.addVersion("v1");
        parent.addNestedEnum(nestedEnum);

        schema.addMessage(parent);

        Optional<MergedEnum> found = schema.findEnumByPath("Request.ItemType");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ItemType");
    }

    @Test
    void shouldReturnEmptyForNonExistentEnumPath() {
        assertThat(schema.findEnumByPath("Unknown")).isEmpty();
        assertThat(schema.findEnumByPath(null)).isEmpty();
        assertThat(schema.findEnumByPath("")).isEmpty();
    }

    // MergedMessage tests

    @Test
    void shouldSetParentForNestedMessages() {
        MergedMessage parent = new MergedMessage("Parent");
        MergedMessage nested = new MergedMessage("Nested");
        parent.addNestedMessage(nested);

        assertThat(nested.getParent()).isEqualTo(parent);
        assertThat(nested.isNested()).isTrue();
        assertThat(parent.isNested()).isFalse();
    }

    @Test
    void shouldGenerateFlattenedNames() {
        MergedMessage parent = new MergedMessage("Report");
        MergedMessage nested = new MergedMessage("Section");
        parent.addNestedMessage(nested);

        assertThat(nested.getFlattenedName()).isEqualTo("ReportSection");
        assertThat(nested.getFlattenedAbstractClassName()).isEqualTo("AbstractReportSection");
        assertThat(nested.getFlattenedVersionClassName("v2")).isEqualTo("ReportSectionV2");
    }

    @Test
    void shouldGetQualifiedInterfaceName() {
        MergedMessage level1 = new MergedMessage("Request");
        MergedMessage level2 = new MergedMessage("Item");
        MergedMessage level3 = new MergedMessage("Tax");
        level1.addNestedMessage(level2);
        level2.addNestedMessage(level3);

        assertThat(level3.getQualifiedInterfaceName()).isEqualTo("Request.Item.Tax");
        assertThat(level2.getQualifiedInterfaceName()).isEqualTo("Request.Item");
        assertThat(level1.getQualifiedInterfaceName()).isEqualTo("Request");
    }

    @Test
    void shouldGetTopLevelParent() {
        MergedMessage level1 = new MergedMessage("Request");
        MergedMessage level2 = new MergedMessage("Item");
        MergedMessage level3 = new MergedMessage("Tax");
        level1.addNestedMessage(level2);
        level2.addNestedMessage(level3);

        assertThat(level3.getTopLevelParent()).isEqualTo(level1);
        assertThat(level2.getTopLevelParent()).isEqualTo(level1);
        assertThat(level1.getTopLevelParent()).isEqualTo(level1);
    }

    @Test
    void shouldGetCommonFields() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");
        message.addVersion("v2");

        FieldInfo field1Info = createFieldInfo("id", 1, Type.TYPE_INT64);
        MergedField field1 = new MergedField(field1Info, "v1");
        field1.addVersion("v2", field1Info);
        message.addField(field1);

        FieldInfo field2Info = createFieldInfo("name", 2, Type.TYPE_STRING);
        MergedField field2 = new MergedField(field2Info, "v2"); // Only v2
        message.addField(field2);

        assertThat(message.getCommonFields()).hasSize(1);
        assertThat(message.getCommonFields().get(0).getName()).isEqualTo("id");
    }

    @Test
    void shouldRemoveNestedEnum() {
        MergedMessage message = new MergedMessage("Request");
        MergedEnum enum1 = new MergedEnum("Type1");
        MergedEnum enum2 = new MergedEnum("Type2");
        message.addNestedEnum(enum1);
        message.addNestedEnum(enum2);

        assertThat(message.getNestedEnums()).hasSize(2);

        message.removeNestedEnum(enum1);

        assertThat(message.getNestedEnums()).hasSize(1);
        assertThat(message.getNestedEnums().get(0).getName()).isEqualTo("Type2");
    }

    @Test
    void shouldFindNestedEnumRecursive() {
        MergedMessage parent = new MergedMessage("Parent");
        MergedMessage nested = new MergedMessage("Nested");
        parent.addNestedMessage(nested);

        MergedEnum deepEnum = new MergedEnum("DeepEnum");
        nested.addNestedEnum(deepEnum);

        Optional<MergedEnum> found = parent.findNestedEnumRecursive("DeepEnum");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("DeepEnum");
    }

    @Test
    void shouldConvertSnakeCaseToPascalCase() {
        MergedMessage message = new MergedMessage("Test");
        message.addSourceFile("v1", "v1/user_request.proto");

        String outerClassName = message.getOuterClassName("v1");
        assertThat(outerClassName).isEqualTo("UserRequest");
    }

    // MergedField tests

    @Test
    void shouldGenerateGetterName() {
        FieldInfo boolField = createFieldInfo("active", 1, Type.TYPE_BOOL);
        MergedField boolMerged = new MergedField(boolField, "v1");
        assertThat(boolMerged.getGetterName()).isEqualTo("isActive");

        FieldInfo intField = createFieldInfo("count", 2, Type.TYPE_INT32);
        MergedField intMerged = new MergedField(intField, "v1");
        assertThat(intMerged.getGetterName()).isEqualTo("getCount");
    }

    @Test
    void shouldCheckIfFieldIsUniversal() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");
        message.addVersion("v2");

        FieldInfo fieldInfo = createFieldInfo("id", 1, Type.TYPE_INT64);
        MergedField field = new MergedField(fieldInfo, "v1");
        field.addVersion("v2", fieldInfo);

        assertThat(field.isUniversal(message.getPresentInVersions())).isTrue();

        MergedField partialField = new MergedField(fieldInfo, "v1");
        assertThat(partialField.isUniversal(message.getPresentInVersions())).isFalse();
    }

    // MergedEnumValue tests

    @Test
    void shouldTrackEnumValueVersions() {
        EnumInfo.EnumValue value = new EnumInfo.EnumValue("CASH", 0);
        MergedEnumValue mergedValue = new MergedEnumValue(value, "v1");
        mergedValue.addVersion("v2");

        assertThat(mergedValue.getName()).isEqualTo("CASH");
        assertThat(mergedValue.getJavaName()).isEqualTo("CASH");
        assertThat(mergedValue.getNumber()).isEqualTo(0);
        assertThat(mergedValue.getPresentInVersions()).containsExactly("v1", "v2");
    }

    // Helper methods

    private FieldInfo createFieldInfo(String name, int number, Type type) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        return new FieldInfo(proto);
    }
}
