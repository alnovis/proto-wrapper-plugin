package space.alnovis.protowrapper.generator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedMessage;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GeneratorUtils}.
 */
class GeneratorUtilsTest {

    @Nested
    class VersionCheckTests {

        @Test
        void buildVersionCheck_singleVersion_returnsSimpleCheck() {
            String result = GeneratorUtils.buildVersionCheck(Set.of("v1"));
            assertThat(result).isEqualTo("getWrapperVersion() == 1");
        }

        @Test
        void buildVersionCheck_multipleVersions_returnsOrCondition() {
            String result = GeneratorUtils.buildVersionCheck(Set.of("v1", "v2"));
            assertThat(result).contains("getWrapperVersion() == 1");
            assertThat(result).contains("getWrapperVersion() == 2");
            assertThat(result).contains("||");
        }

        @Test
        void buildVersionCheck_emptySet_returnsTrue() {
            String result = GeneratorUtils.buildVersionCheck(Set.of());
            assertThat(result).isEqualTo("true");
        }

        @Test
        void buildVersionCheck_nullSet_returnsTrue() {
            String result = GeneratorUtils.buildVersionCheck(null);
            assertThat(result).isEqualTo("true");
        }

        @Test
        void buildVersionCheck_complexVersionNumber_extractsCorrectly() {
            String result = GeneratorUtils.buildVersionCheck(Set.of("v202"));
            assertThat(result).isEqualTo("getWrapperVersion() == 202");
        }

        @ParameterizedTest
        @CsvSource({
            "v1, 1",
            "v2, 2",
            "v10, 10",
            "v202, 202",
            "version1, 1"
        })
        void extractVersionNumber_variousFormats_extractsCorrectly(String version, String expected) {
            assertThat(GeneratorUtils.extractVersionNumber(version)).isEqualTo(expected);
        }

        @Test
        void extractVersionNumber_null_returnsEmpty() {
            assertThat(GeneratorUtils.extractVersionNumber(null)).isEmpty();
        }

        @Test
        void extractVersionInt_validVersion_returnsInt() {
            assertThat(GeneratorUtils.extractVersionInt("v42")).isEqualTo(42);
        }

        @Test
        void extractVersionInt_invalidVersion_returnsZero() {
            assertThat(GeneratorUtils.extractVersionInt("invalid")).isZero();
        }
    }

    @Nested
    class StringCheckTests {

        @ParameterizedTest
        @NullAndEmptySource
        void isNullOrEmpty_nullOrEmpty_returnsTrue(String input) {
            assertThat(GeneratorUtils.isNullOrEmpty(input)).isTrue();
        }

        @Test
        void isNullOrEmpty_nonEmpty_returnsFalse() {
            assertThat(GeneratorUtils.isNullOrEmpty("test")).isFalse();
        }

        @Test
        void isNullOrEmpty_whitespace_returnsFalse() {
            assertThat(GeneratorUtils.isNullOrEmpty("   ")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void isNullOrBlank_nullEmptyOrBlank_returnsTrue(String input) {
            assertThat(GeneratorUtils.isNullOrBlank(input)).isTrue();
        }

        @Test
        void isNullOrBlank_nonBlank_returnsFalse() {
            assertThat(GeneratorUtils.isNullOrBlank("test")).isFalse();
        }

        @Test
        void nullToEmpty_null_returnsEmpty() {
            assertThat(GeneratorUtils.nullToEmpty(null)).isEmpty();
        }

        @Test
        void nullToEmpty_value_returnsValue() {
            assertThat(GeneratorUtils.nullToEmpty("test")).isEqualTo("test");
        }

        @Test
        void emptyToNull_empty_returnsNull() {
            assertThat(GeneratorUtils.emptyToNull("")).isNull();
        }

        @Test
        void emptyToNull_value_returnsValue() {
            assertThat(GeneratorUtils.emptyToNull("test")).isEqualTo("test");
        }
    }

    @Nested
    class StringUtilityTests {

        @ParameterizedTest
        @CsvSource({
            "hello, Hello",
            "HELLO, HELLO",
            "a, A",
            "helloWorld, HelloWorld"
        })
        void capitalize_variousInputs_capitalizesCorrectly(String input, String expected) {
            assertThat(GeneratorUtils.capitalize(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void capitalize_nullOrEmpty_returnsSame(String input) {
            assertThat(GeneratorUtils.capitalize(input)).isEqualTo(input);
        }

        @ParameterizedTest
        @CsvSource({
            "Hello, hello",
            "HELLO, hELLO",
            "A, a"
        })
        void uncapitalize_variousInputs_uncapitalizesCorrectly(String input, String expected) {
            assertThat(GeneratorUtils.uncapitalize(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "field_name, fieldName",
            "some_long_name, someLongName",
            "already, already",
            "a_b_c, aBC"
        })
        void snakeToCamel_variousInputs_convertsCorrectly(String input, String expected) {
            assertThat(GeneratorUtils.snakeToCamel(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "field_name, FieldName",
            "some_long_name, SomeLongName"
        })
        void snakeToPascal_variousInputs_convertsCorrectly(String input, String expected) {
            assertThat(GeneratorUtils.snakeToPascal(input)).isEqualTo(expected);
        }
    }

    @Nested
    class TypeClassificationTests {

        @Test
        void isPrimitiveOrPrimitiveLike_primitiveField_returnsTrue() {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("value")
                    .setNumber(1)
                    .setType(Type.TYPE_INT32)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            FieldInfo field = new FieldInfo(proto);

            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLike(field)).isTrue();
        }

        @Test
        void isPrimitiveOrPrimitiveLike_stringField_returnsTrue() {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("name")
                    .setNumber(1)
                    .setType(Type.TYPE_STRING)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            FieldInfo field = new FieldInfo(proto);

            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLike(field)).isTrue();
        }

        @Test
        void isPrimitiveOrPrimitiveLike_bytesField_returnsTrue() {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("data")
                    .setNumber(1)
                    .setType(Type.TYPE_BYTES)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            FieldInfo field = new FieldInfo(proto);

            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLike(field)).isTrue();
        }

        @Test
        void isPrimitiveOrPrimitiveLike_messageField_returnsFalse() {
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("nested")
                    .setNumber(1)
                    .setType(Type.TYPE_MESSAGE)
                    .setTypeName("NestedMessage")
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            FieldInfo field = new FieldInfo(proto);

            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLike(field)).isFalse();
        }

        @Test
        void isPrimitiveOrPrimitiveLike_null_returnsFalse() {
            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLike(null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"int", "long", "double", "boolean", "String", "byte[]", "ByteString"})
        void isPrimitiveOrPrimitiveLikeType_primitiveTypes_returnsTrue(String type) {
            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLikeType(type)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Money", "Order", "CustomMessage"})
        void isPrimitiveOrPrimitiveLikeType_messageTypes_returnsFalse(String type) {
            assertThat(GeneratorUtils.isPrimitiveOrPrimitiveLikeType(type)).isFalse();
        }
    }

    @Nested
    class MessageHierarchyTests {

        @Test
        void collectMessageHierarchyNames_topLevel_returnsSingleName() {
            MergedMessage message = new MergedMessage("Order");

            List<String> names = GeneratorUtils.collectMessageHierarchyNames(message);

            assertThat(names).containsExactly("Order");
        }

        @Test
        void collectMessageHierarchyNames_nested_returnsHierarchy() {
            MergedMessage parent = new MergedMessage("Order");
            MergedMessage child = new MergedMessage("Item");
            parent.addNestedMessage(child);

            List<String> names = GeneratorUtils.collectMessageHierarchyNames(child);

            assertThat(names).containsExactly("Order", "Item");
        }

        @Test
        void collectMessageHierarchyNames_deeplyNested_returnsFullHierarchy() {
            MergedMessage root = new MergedMessage("Order");
            MergedMessage middle = new MergedMessage("Item");
            MergedMessage leaf = new MergedMessage("Detail");
            root.addNestedMessage(middle);
            middle.addNestedMessage(leaf);

            List<String> names = GeneratorUtils.collectMessageHierarchyNames(leaf);

            assertThat(names).containsExactly("Order", "Item", "Detail");
        }

        @Test
        void buildNestedBuilderMethodName_nested_returnsCorrectName() {
            MergedMessage parent = new MergedMessage("Order");
            MergedMessage child = new MergedMessage("Item");
            parent.addNestedMessage(child);

            String methodName = GeneratorUtils.buildNestedBuilderMethodName(child);

            assertThat(methodName).isEqualTo("newOrderItemBuilder");
        }

        @Test
        void buildNestedQualifiedName_nested_returnsDotSeparated() {
            MergedMessage parent = new MergedMessage("Order");
            MergedMessage child = new MergedMessage("Item");
            parent.addNestedMessage(child);

            String qualifiedName = GeneratorUtils.buildNestedQualifiedName(child);

            assertThat(qualifiedName).isEqualTo("Order.Item");
        }

        @Test
        void buildNestedInterfaceName_nested_returnsConcatenated() {
            MergedMessage parent = new MergedMessage("Order");
            MergedMessage child = new MergedMessage("Item");
            parent.addNestedMessage(child);

            String interfaceName = GeneratorUtils.buildNestedInterfaceName(child);

            assertThat(interfaceName).isEqualTo("OrderItem");
        }
    }
}
