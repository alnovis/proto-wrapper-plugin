package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldInfoTest {

    @Test
    void shouldConvertProtoNameToJavaName() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("user_request_id")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        assertThat(field.getProtoName()).isEqualTo("user_request_id");
        assertThat(field.getJavaName()).isEqualTo("userRequestId");
        assertThat(field.getGetterName()).isEqualTo("getUserRequestId");
    }

    @Test
    void shouldIdentifyOptionalPrimitive() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("active")
                .setNumber(1)
                .setType(Type.TYPE_BOOL)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        assertThat(field.isOptional()).isTrue();
        assertThat(field.isPrimitive()).isTrue();
        assertThat(field.getGetterName()).isEqualTo("isActive");
        assertThat(field.getGetterType()).isEqualTo("Boolean"); // Boxed for optional
    }

    @Test
    void shouldIdentifyRepeatedField() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("items")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".org.example.Item")
                .setLabel(Label.LABEL_REPEATED)
                .build();

        FieldInfo field = new FieldInfo(proto);

        assertThat(field.isRepeated()).isTrue();
        assertThat(field.isMessage()).isTrue();
        assertThat(field.getJavaType()).isEqualTo("java.util.List<Item>");
    }

    @Test
    void shouldGenerateExtractMethodNames() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("billing_status")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        assertThat(field.getExtractMethodName()).isEqualTo("extractBillingStatus");
        assertThat(field.getExtractHasMethodName()).isEqualTo("extractHasBillingStatus");
    }

    @Test
    void shouldMapProtoTypesToJavaTypes() {
        assertJavaType(Type.TYPE_INT32, "int");
        assertJavaType(Type.TYPE_INT64, "long");
        assertJavaType(Type.TYPE_BOOL, "boolean");
        assertJavaType(Type.TYPE_STRING, "String");
        assertJavaType(Type.TYPE_DOUBLE, "double");
        assertJavaType(Type.TYPE_FLOAT, "float");
        assertJavaType(Type.TYPE_BYTES, "byte[]");
    }

    private void assertJavaType(Type protoType, String expectedJavaType) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("field")
                .setNumber(1)
                .setType(protoType)
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        FieldInfo field = new FieldInfo(proto);
        assertThat(field.getJavaType()).isEqualTo(expectedJavaType);
    }

    @Test
    void shouldExtractNestedTypePath() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("reg_info")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.proto.v1.Order.ShippingInfo")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        // Same version - should work
        assertThat(field.extractNestedTypePath("example.proto.v1"))
                .isEqualTo("Order.ShippingInfo");
    }

    @Test
    void shouldExtractNestedTypePathVersionIndependent() {
        // Field type from v2
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("reg_info")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.proto.v2.Order.ShippingInfo")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        // Proto package from v1 - should still work due to version-independent matching
        assertThat(field.extractNestedTypePath("example.proto.v1"))
                .isEqualTo("Order.ShippingInfo");

        // Also works with v2
        assertThat(field.extractNestedTypePath("example.proto.v2"))
                .isEqualTo("Order.ShippingInfo");

        // And any other version
        assertThat(field.extractNestedTypePath("example.proto.v999"))
                .isEqualTo("Order.ShippingInfo");
    }

    @Test
    void shouldFallbackToSimpleNameForNonMatchingPackage() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("item")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".other.package.Item")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        // Different package - should fallback to simple name
        assertThat(field.extractNestedTypePath("example.proto.v1"))
                .isEqualTo("Item");
    }

    @Test
    void shouldHandleNullProtoPackage() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("item")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.proto.v1.Item")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto);

        assertThat(field.extractNestedTypePath(null)).isEqualTo("Item");
        assertThat(field.extractNestedTypePath("")).isEqualTo("Item");
    }
}
