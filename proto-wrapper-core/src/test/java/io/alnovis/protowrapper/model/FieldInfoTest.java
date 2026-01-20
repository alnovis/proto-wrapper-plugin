package io.alnovis.protowrapper.model;

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

    // ========================================================================
    // Tests for supportsHasMethod() - proto2 syntax
    // ========================================================================

    @Test
    void proto2_optionalScalarField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("optional_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 optional scalar fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_requiredScalarField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("required_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 required scalar fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_repeatedScalarField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("repeated_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REPEATED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 repeated fields should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto2_optionalMessageField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("optional_message")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.Nested")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 optional message fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_requiredMessageField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("required_message")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.Nested")
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 required message fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_repeatedMessageField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("repeated_message")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.Nested")
                .setLabel(Label.LABEL_REPEATED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 repeated message fields should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto2_optionalStringField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("address")
                .setNumber(1)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 optional string fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_requiredStringField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("address")
                .setNumber(1)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 required string fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_optionalEnumField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("status")
                .setNumber(1)
                .setType(Type.TYPE_ENUM)
                .setTypeName(".example.Status")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 optional enum fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto2_requiredEnumField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("status")
                .setNumber(1)
                .setType(Type.TYPE_ENUM)
                .setTypeName(".example.Status")
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

        assertThat(field.supportsHasMethod())
                .as("Proto2 required enum fields should support has*() method")
                .isTrue();
    }

    // ========================================================================
    // Tests for supportsHasMethod() - proto3 syntax
    // ========================================================================

    @Test
    void proto3_singularScalarField_shouldNotSupportHasMethod() {
        // In proto3, singular scalar fields (without 'optional' keyword) don't have has*()
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("singular_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL) // proto3 singular fields use LABEL_OPTIONAL but no hasXxx()
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 singular scalar fields (without 'optional' keyword) should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto3_repeatedScalarField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("repeated_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REPEATED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 repeated scalar fields should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto3_messageField_shouldSupportHasMethod() {
        // In proto3, message fields ALWAYS have has*() method
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("nested")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.Nested")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 message fields should support has*() method")
                .isTrue();
    }

    @Test
    void proto3_repeatedMessageField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("items")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".example.Item")
                .setLabel(Label.LABEL_REPEATED)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 repeated message fields should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto3_optionalScalarFieldInOneof_shouldSupportHasMethod() {
        // In proto3, fields in oneof (including synthetic oneofs for 'optional' keyword) have has*()
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("optional_int")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .setOneofIndex(0) // Field is part of a oneof
                .build();

        FieldInfo field = new FieldInfo(proto, 0, "_optional_int", null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 scalar fields in oneof should support has*() method")
                .isTrue();
    }

    @Test
    void proto3_stringFieldInOneof_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("name_or_id")
                .setNumber(1)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_OPTIONAL)
                .setOneofIndex(0)
                .build();

        FieldInfo field = new FieldInfo(proto, 0, "identifier", null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 string fields in oneof should support has*() method")
                .isTrue();
    }

    @Test
    void proto3_singularEnumField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("status")
                .setNumber(1)
                .setType(Type.TYPE_ENUM)
                .setTypeName(".example.Status")
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 singular enum fields should NOT support has*() method")
                .isFalse();
    }

    @Test
    void proto3_enumFieldInOneof_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("status")
                .setNumber(1)
                .setType(Type.TYPE_ENUM)
                .setTypeName(".example.Status")
                .setLabel(Label.LABEL_OPTIONAL)
                .setOneofIndex(0)
                .build();

        FieldInfo field = new FieldInfo(proto, 0, "status_oneof", null, ProtoSyntax.PROTO3);

        assertThat(field.supportsHasMethod())
                .as("Proto3 enum fields in oneof should support has*() method")
                .isTrue();
    }

    // ========================================================================
    // Tests for supportsHasMethod() - all primitive types in proto2
    // ========================================================================

    @Test
    void proto2_allRequiredPrimitiveTypes_shouldSupportHasMethod() {
        Type[] primitiveTypes = {
            Type.TYPE_INT32, Type.TYPE_INT64, Type.TYPE_UINT32, Type.TYPE_UINT64,
            Type.TYPE_SINT32, Type.TYPE_SINT64, Type.TYPE_FIXED32, Type.TYPE_FIXED64,
            Type.TYPE_SFIXED32, Type.TYPE_SFIXED64, Type.TYPE_FLOAT, Type.TYPE_DOUBLE,
            Type.TYPE_BOOL, Type.TYPE_STRING, Type.TYPE_BYTES
        };

        for (Type type : primitiveTypes) {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("field_" + type.name().toLowerCase())
                    .setNumber(1)
                    .setType(type)
                    .setLabel(Label.LABEL_REQUIRED)
                    .build();

            FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

            assertThat(field.supportsHasMethod())
                    .as("Proto2 required %s field should support has*() method", type.name())
                    .isTrue();
        }
    }

    @Test
    void proto2_allOptionalPrimitiveTypes_shouldSupportHasMethod() {
        Type[] primitiveTypes = {
            Type.TYPE_INT32, Type.TYPE_INT64, Type.TYPE_UINT32, Type.TYPE_UINT64,
            Type.TYPE_SINT32, Type.TYPE_SINT64, Type.TYPE_FIXED32, Type.TYPE_FIXED64,
            Type.TYPE_SFIXED32, Type.TYPE_SFIXED64, Type.TYPE_FLOAT, Type.TYPE_DOUBLE,
            Type.TYPE_BOOL, Type.TYPE_STRING, Type.TYPE_BYTES
        };

        for (Type type : primitiveTypes) {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("field_" + type.name().toLowerCase())
                    .setNumber(1)
                    .setType(type)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();

            FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

            assertThat(field.supportsHasMethod())
                    .as("Proto2 optional %s field should support has*() method", type.name())
                    .isTrue();
        }
    }

    @Test
    void proto2_allRepeatedPrimitiveTypes_shouldNotSupportHasMethod() {
        Type[] primitiveTypes = {
            Type.TYPE_INT32, Type.TYPE_INT64, Type.TYPE_UINT32, Type.TYPE_UINT64,
            Type.TYPE_SINT32, Type.TYPE_SINT64, Type.TYPE_FIXED32, Type.TYPE_FIXED64,
            Type.TYPE_SFIXED32, Type.TYPE_SFIXED64, Type.TYPE_FLOAT, Type.TYPE_DOUBLE,
            Type.TYPE_BOOL, Type.TYPE_STRING, Type.TYPE_BYTES
        };

        for (Type type : primitiveTypes) {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("field_" + type.name().toLowerCase())
                    .setNumber(1)
                    .setType(type)
                    .setLabel(Label.LABEL_REPEATED)
                    .build();

            FieldInfo field = new FieldInfo(proto, -1, null, null, ProtoSyntax.PROTO2);

            assertThat(field.supportsHasMethod())
                    .as("Proto2 repeated %s field should NOT support has*() method", type.name())
                    .isFalse();
        }
    }

    // ========================================================================
    // Tests for default constructor (backwards compatibility - defaults to PROTO2)
    // ========================================================================

    @Test
    void defaultConstructor_optionalField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();

        // Using default constructor (no ProtoSyntax specified - defaults to PROTO2)
        FieldInfo field = new FieldInfo(proto);

        assertThat(field.supportsHasMethod())
                .as("Default constructor should use PROTO2 semantics for optional fields")
                .isTrue();
    }

    @Test
    void defaultConstructor_requiredField_shouldSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();

        // Using default constructor (no ProtoSyntax specified - defaults to PROTO2)
        FieldInfo field = new FieldInfo(proto);

        assertThat(field.supportsHasMethod())
                .as("Default constructor should use PROTO2 semantics for required fields")
                .isTrue();
    }

    @Test
    void defaultConstructor_repeatedField_shouldNotSupportHasMethod() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REPEATED)
                .build();

        // Using default constructor (no ProtoSyntax specified - defaults to PROTO2)
        FieldInfo field = new FieldInfo(proto);

        assertThat(field.supportsHasMethod())
                .as("Default constructor should use PROTO2 semantics for repeated fields")
                .isFalse();
    }
}
