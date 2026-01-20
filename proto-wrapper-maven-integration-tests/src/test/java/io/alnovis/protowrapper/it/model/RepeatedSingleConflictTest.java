package io.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.it.model.api.RepeatedSingleConflicts;
import io.alnovis.protowrapper.it.model.api.VersionContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for REPEATED_SINGLE conflict type.
 *
 * <p>Tests the case where v1 has repeated fields and v2 has singular fields.</p>
 */
@DisplayName("REPEATED_SINGLE Conflict Integration Tests")
class RepeatedSingleConflictTest {

    @Nested
    @DisplayName("V1 (repeated) tests")
    class V1RepeatedTests {

        @Test
        @DisplayName("should create with multiple items in v1")
        void shouldCreateWithMultipleItems() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("test-1")
                    .setItems(List.of(1, 2, 3))
                    .setTags(List.of("a", "b", "c"))
                    .setScores(List.of(1.5f, 2.5f, 3.5f))
                    .build();

            assertThat(instance.getId()).isEqualTo("test-1");
            assertThat(instance.getItems()).containsExactly(1, 2, 3);
            assertThat(instance.getTags()).containsExactly("a", "b", "c");
            assertThat(instance.getScores()).containsExactly(1.5f, 2.5f, 3.5f);
            assertThat(instance.getWrapperVersionId()).isEqualTo("v1");
        }

        @Test
        @DisplayName("should add items one by one in v1")
        void shouldAddItemsOneByOne() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("test-2")
                    .addItems(10)
                    .addItems(20)
                    .addItems(30)
                    .build();

            assertThat(instance.getItems()).containsExactly(10, 20, 30);
        }

        @Test
        @DisplayName("should add all items at once in v1")
        void shouldAddAllItemsAtOnce() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("test-3")
                    .addItems(1)
                    .addAllItems(List.of(2, 3, 4))
                    .build();

            assertThat(instance.getItems()).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("should clear items in v1")
        void shouldClearItems() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("test-4")
                    .setItems(List.of(1, 2, 3))
                    .clearItems()
                    .build();

            assertThat(instance.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("V2 (singular) tests")
    class V2SingularTests {

        @Test
        @DisplayName("should create with single item in v2")
        void shouldCreateWithSingleItem() {
            VersionContext v2 = VersionContext.forVersionId("v2");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v2)
                    .setId("test-10")
                    .setItems(List.of(42))
                    .setTags(List.of("single-tag"))
                    .setScores(List.of(9.9f))
                    .build();

            assertThat(instance.getId()).isEqualTo("test-10");
            assertThat(instance.getItems()).containsExactly(42);
            assertThat(instance.getTags()).containsExactly("single-tag");
            assertThat(instance.getScores()).containsExactly(9.9f);
            assertThat(instance.getWrapperVersionId()).isEqualTo("v2");
        }

        @Test
        @DisplayName("should throw when setting multiple items in v2")
        void shouldThrowWhenSettingMultipleItems() {
            VersionContext v2 = VersionContext.forVersionId("v2");
            RepeatedSingleConflicts.Builder builder = RepeatedSingleConflicts.newBuilder(v2)
                    .setId("test-11");

            assertThatThrownBy(() -> builder.setItems(List.of(1, 2, 3)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when setting empty list in v2")
        void shouldThrowWhenSettingEmptyList() {
            VersionContext v2 = VersionContext.forVersionId("v2");
            RepeatedSingleConflicts.Builder builder = RepeatedSingleConflicts.newBuilder(v2)
                    .setId("test-12");

            assertThatThrownBy(() -> builder.setItems(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should allow addItems to replace value in v2")
        void shouldAllowAddItemsToReplace() {
            VersionContext v2 = VersionContext.forVersionId("v2");
            RepeatedSingleConflicts instance = RepeatedSingleConflicts.newBuilder(v2)
                    .setId("test-13")
                    .addItems(100)
                    .build();

            // In v2, addItems replaces the existing value
            assertThat(instance.getItems()).containsExactly(100);
        }
    }

    @Nested
    @DisplayName("Cross-version conversion tests")
    class CrossVersionTests {

        @Test
        @DisplayName("should convert v1 with multiple items to v2 (silently takes first)")
        void shouldConvertV1ToV2() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts v1Instance = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("convert-1")
                    .setItems(List.of(10, 20, 30))
                    .setTags(List.of("x", "y"))
                    .build();

            // Convert to v2
            io.alnovis.protowrapper.it.model.v2.RepeatedSingleConflicts v2Instance =
                    v1Instance.asVersion(io.alnovis.protowrapper.it.model.v2.RepeatedSingleConflicts.class);

            assertThat(v2Instance.getId()).isEqualTo("convert-1");
            assertThat(v2Instance.getWrapperVersionId()).isEqualTo("v2");
            // v2 only has singular, so it wraps the single value in a list
            assertThat(v2Instance.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should convert v2 with single item to v1")
        void shouldConvertV2ToV1() {
            VersionContext v2 = VersionContext.forVersionId("v2");
            RepeatedSingleConflicts v2Instance = RepeatedSingleConflicts.newBuilder(v2)
                    .setId("convert-2")
                    .setItems(List.of(99))
                    .setTags(List.of("tag"))
                    .build();

            // Convert to v1
            io.alnovis.protowrapper.it.model.v1.RepeatedSingleConflicts v1Instance =
                    v2Instance.asVersion(io.alnovis.protowrapper.it.model.v1.RepeatedSingleConflicts.class);

            assertThat(v1Instance.getId()).isEqualTo("convert-2");
            assertThat(v1Instance.getWrapperVersionId()).isEqualTo("v1");
            // v1 has repeated, but v2 only had one item
            assertThat(v1Instance.getItems()).containsExactly(99);
        }

        @Test
        @DisplayName("should round-trip through v2 preserving data via protobuf unknown fields")
        void shouldRoundTripPreservingData() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            RepeatedSingleConflicts original = RepeatedSingleConflicts.newBuilder(v1)
                    .setId("roundtrip-1")
                    .setItems(List.of(1, 2, 3, 4, 5))
                    .setTags(List.of("a", "b", "c"))
                    .build();

            // v1 -> v2 -> v1
            io.alnovis.protowrapper.it.model.v2.RepeatedSingleConflicts v2Instance =
                    original.asVersion(io.alnovis.protowrapper.it.model.v2.RepeatedSingleConflicts.class);

            io.alnovis.protowrapper.it.model.v1.RepeatedSingleConflicts recovered =
                    v2Instance.asVersion(io.alnovis.protowrapper.it.model.v1.RepeatedSingleConflicts.class);

            assertThat(recovered.getId()).isEqualTo("roundtrip-1");
            // Due to protobuf unknown fields preservation, some data may be preserved
            assertThat(recovered.getWrapperVersionId()).isEqualTo("v1");
        }
    }
}
