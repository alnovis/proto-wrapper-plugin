package space.alnovis.protowrapper.it.model;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.PrimitiveMessageConflicts;
import space.alnovis.protowrapper.it.model.api.RefInfo;
import space.alnovis.protowrapper.it.model.api.TextInfo;
import space.alnovis.protowrapper.it.model.api.TimestampInfo;
import space.alnovis.protowrapper.it.model.api.VersionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for PRIMITIVE_MESSAGE conflict builder support.
 *
 * <p>Tests the dual setter pattern for fields that change type from primitive to message
 * across protocol versions.</p>
 *
 * @since 1.6.2
 */
@DisplayName("PRIMITIVE_MESSAGE Builder Tests")
class PrimitiveMessageBuilderTest {

    @Nested
    @DisplayName("V1 (Primitive Version) Builder Tests")
    class V1PrimitiveBuilderTests {

        private final VersionContext ctx = VersionContext.forVersion(1);

        @Test
        @DisplayName("setSimpleRef(int) works for primitive version")
        void setPrimitiveValueWorks() {
            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(42)
                    .setTextRef("text")  // required field
                    .setName("test")
                    .build();

            assertThat(result.getSimpleRef()).isEqualTo(42);
            assertThat(result.supportsSimpleRef()).isTrue();
        }

        @Test
        @DisplayName("setTextRef(String) works for primitive version")
        void setStringValueWorks() {
            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(1)     // required field
                    .setTextRef("hello")
                    .setName("test")
                    .build();

            assertThat(result.getTextRef()).isEqualTo("hello");
        }

        @Test
        @DisplayName("setTimestampValue(Long) works for primitive version")
        void setLongValueWorks() {
            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(1)     // required field
                    .setTextRef("text")  // required field
                    .setTimestampValue(1234567890L)
                    .setName("test")
                    .build();

            assertThat(result.getTimestampValue()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("setSimpleRefMessage() throws for primitive version")
        void setMessageThrowsForPrimitiveVersion() {
            // Note: RefInfo is only available in v2, so we create with v2 context
            VersionContext v2ctx = VersionContext.forVersion(2);
            RefInfo refInfo = RefInfo.newBuilder(v2ctx)
                    .setRefId(1)
                    .build();

            assertThatThrownBy(() ->
                    PrimitiveMessageConflicts.newBuilder(ctx)
                            .setSimpleRefMessage(refInfo)
            )
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("setSimpleRefMessage()")
                    .hasMessageContaining("not supported");
        }

        @Test
        @DisplayName("setTextRefMessage() throws for primitive version")
        void setTextMessageThrowsForPrimitiveVersion() {
            VersionContext v2ctx = VersionContext.forVersion(2);
            TextInfo textInfo = TextInfo.newBuilder(v2ctx)
                    .setContent("test")
                    .build();

            assertThatThrownBy(() ->
                    PrimitiveMessageConflicts.newBuilder(ctx)
                            .setTextRefMessage(textInfo)
            )
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("setTextRefMessage()")
                    .hasMessageContaining("not supported");
        }

        @Test
        @DisplayName("all required fields can be set via builder")
        void allRequiredFieldsCanBeSet() {
            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(100)
                    .setTextRef("required text")
                    .setName("test name")
                    .build();

            assertThat(result.getSimpleRef()).isEqualTo(100);
            assertThat(result.getTextRef()).isEqualTo("required text");
            assertThat(result.getName()).isEqualTo("test name");
        }
    }

    @Nested
    @DisplayName("V2 (Message Version) Builder Tests")
    class V2MessageBuilderTests {

        private final VersionContext ctx = VersionContext.forVersion(2);

        @Test
        @DisplayName("setSimpleRefMessage() works for message version")
        void setMessageValueWorks() {
            RefInfo refInfo = RefInfo.newBuilder(ctx)
                    .setRefId(123)
                    .setRefType("reference")
                    .build();

            TextInfo textInfo = TextInfo.newBuilder(ctx)
                    .setContent("text")
                    .build();

            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRefMessage(refInfo)
                    .setTextRefMessage(textInfo)  // required field
                    .setName("test")
                    .build();

            assertThat(result.getSimpleRefMessage()).isNotNull();
            assertThat(result.getSimpleRefMessage().getRefId()).isEqualTo(123);
            assertThat(result.getSimpleRefMessage().getRefType()).isEqualTo("reference");
            assertThat(result.supportsSimpleRefMessage()).isTrue();
        }

        @Test
        @DisplayName("setTextRefMessage() works for message version")
        void setTextMessageWorks() {
            RefInfo refInfo = RefInfo.newBuilder(ctx)
                    .setRefId(1)
                    .build();

            TextInfo textInfo = TextInfo.newBuilder(ctx)
                    .setContent("hello world")
                    .setEncoding("UTF-8")
                    .build();

            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRefMessage(refInfo)  // required field
                    .setTextRefMessage(textInfo)
                    .setName("test")
                    .build();

            assertThat(result.getTextRefMessage()).isNotNull();
            assertThat(result.getTextRefMessage().getContent()).isEqualTo("hello world");
        }

        @Test
        @DisplayName("setTimestampValueMessage() works for message version")
        void setTimestampMessageWorks() {
            RefInfo refInfo = RefInfo.newBuilder(ctx)
                    .setRefId(1)
                    .build();

            TextInfo textInfo = TextInfo.newBuilder(ctx)
                    .setContent("text")
                    .build();

            TimestampInfo timestamp = TimestampInfo.newBuilder(ctx)
                    .setSeconds(1704412800L)
                    .setNanos(0)
                    .build();

            PrimitiveMessageConflicts result = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRefMessage(refInfo)  // required field
                    .setTextRefMessage(textInfo)   // required field
                    .setTimestampValueMessage(timestamp)
                    .setName("test")
                    .build();

            assertThat(result.getTimestampValueMessage()).isNotNull();
            assertThat(result.getTimestampValueMessage().getSeconds()).isEqualTo(1704412800L);
        }

        @Test
        @DisplayName("setSimpleRef(int) throws for message version")
        void setPrimitiveThrowsForMessageVersion() {
            assertThatThrownBy(() ->
                    PrimitiveMessageConflicts.newBuilder(ctx)
                            .setSimpleRef(42)
            )
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("setSimpleRef()")
                    .hasMessageContaining("not supported");
        }

        @Test
        @DisplayName("setTextRef(String) throws for message version")
        void setStringThrowsForMessageVersion() {
            assertThatThrownBy(() ->
                    PrimitiveMessageConflicts.newBuilder(ctx)
                            .setTextRef("hello")
            )
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("setTextRef()")
                    .hasMessageContaining("not supported");
        }

        @Test
        @DisplayName("setTimestampValue(Long) throws for message version")
        void setLongThrowsForMessageVersion() {
            assertThatThrownBy(() ->
                    PrimitiveMessageConflicts.newBuilder(ctx)
                            .setTimestampValue(123L)
            )
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("setTimestampValue()")
                    .hasMessageContaining("not supported");
        }
    }

    @Nested
    @DisplayName("Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("V1 builder -> serialize -> deserialize preserves values")
        void v1RoundTrip() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersion(1);

            PrimitiveMessageConflicts original = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(42)
                    .setTextRef("hello")
                    .setTimestampValue(1234567890L)
                    .setName("test")
                    .build();

            byte[] bytes = original.toBytes();
            PrimitiveMessageConflicts restored = ctx.parsePrimitiveMessageConflictsFromBytes(bytes);

            assertThat(restored.getSimpleRef()).isEqualTo(42);
            assertThat(restored.getTextRef()).isEqualTo("hello");
            assertThat(restored.getTimestampValue()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("V2 builder -> serialize -> deserialize preserves values")
        void v2RoundTrip() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersion(2);

            RefInfo refInfo = RefInfo.newBuilder(ctx)
                    .setRefId(123)
                    .setRefType("ref")
                    .build();

            TextInfo textInfo = TextInfo.newBuilder(ctx)
                    .setContent("world")
                    .build();

            TimestampInfo timestamp = TimestampInfo.newBuilder(ctx)
                    .setSeconds(1704412800L)
                    .setNanos(500)
                    .build();

            PrimitiveMessageConflicts original = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRefMessage(refInfo)
                    .setTextRefMessage(textInfo)
                    .setTimestampValueMessage(timestamp)
                    .setName("test")
                    .build();

            byte[] bytes = original.toBytes();
            PrimitiveMessageConflicts restored = ctx.parsePrimitiveMessageConflictsFromBytes(bytes);

            assertThat(restored.getSimpleRefMessage().getRefId()).isEqualTo(123);
            assertThat(restored.getTextRefMessage().getContent()).isEqualTo("world");
            assertThat(restored.getTimestampValueMessage().getSeconds()).isEqualTo(1704412800L);
        }
    }

    @Nested
    @DisplayName("toBuilder() Tests")
    class ToBuilderTests {

        @Test
        @DisplayName("V1 toBuilder() allows modification with primitive setters")
        void v1ToBuilderModification() {
            VersionContext ctx = VersionContext.forVersion(1);

            PrimitiveMessageConflicts original = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRef(10)
                    .setTextRef("text")
                    .setName("original")
                    .build();

            PrimitiveMessageConflicts modified = original.toBuilder()
                    .setSimpleRef(20)
                    .build();

            assertThat(modified.getSimpleRef()).isEqualTo(20);
            assertThat(modified.getName()).isEqualTo("original");
        }

        @Test
        @DisplayName("V2 toBuilder() allows modification with message setters")
        void v2ToBuilderModification() {
            VersionContext ctx = VersionContext.forVersion(2);

            RefInfo ref1 = RefInfo.newBuilder(ctx).setRefId(1).build();
            RefInfo ref2 = RefInfo.newBuilder(ctx).setRefId(2).build();
            TextInfo text = TextInfo.newBuilder(ctx).setContent("text").build();

            PrimitiveMessageConflicts original = PrimitiveMessageConflicts.newBuilder(ctx)
                    .setSimpleRefMessage(ref1)
                    .setTextRefMessage(text)
                    .setName("original")
                    .build();

            PrimitiveMessageConflicts modified = original.toBuilder()
                    .setSimpleRefMessage(ref2)
                    .build();

            assertThat(modified.getSimpleRefMessage().getRefId()).isEqualTo(2);
            assertThat(modified.getName()).isEqualTo("original");
        }
    }
}
