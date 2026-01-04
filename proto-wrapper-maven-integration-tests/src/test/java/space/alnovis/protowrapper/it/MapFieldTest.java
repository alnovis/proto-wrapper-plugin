package space.alnovis.protowrapper.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.MapConflictMessage;
import space.alnovis.protowrapper.it.model.api.MapTestMessage;
import space.alnovis.protowrapper.it.model.api.VersionContext;
import space.alnovis.protowrapper.it.proto.v1.MapTest;
import space.alnovis.protowrapper.it.proto.v2.MapConflicts;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static space.alnovis.protowrapper.it.proto.v2.MapConflicts.TaskPriority.*;

/**
 * Integration tests for map field support.
 */
class MapFieldTest {

    @Nested
    class V1MapFieldTests {

        @Test
        void readEmptyMap() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder().build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            assertThat(wrapper.getCountsMap()).isEmpty();
            assertThat(wrapper.getCountsCount()).isEqualTo(0);
            assertThat(wrapper.containsCounts("key")).isFalse();
        }

        @Test
        void readMapWithEntries() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder()
                    .putCounts("apple", 5)
                    .putCounts("banana", 3)
                    .build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            assertThat(wrapper.getCountsMap()).hasSize(2);
            assertThat(wrapper.getCountsCount()).isEqualTo(2);
            assertThat(wrapper.containsCounts("apple")).isTrue();
            assertThat(wrapper.containsCounts("orange")).isFalse();
            assertThat(wrapper.getCountsOrDefault("apple", 0)).isEqualTo(5);
            assertThat(wrapper.getCountsOrDefault("orange", 10)).isEqualTo(10);
            assertThat(wrapper.getCountsOrThrow("banana")).isEqualTo(3);
        }

        @Test
        void getOrThrow_throwsForMissingKey() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder().build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            assertThatThrownBy(() -> wrapper.getCountsOrThrow("missing"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing");
        }

        @Test
        void mapIsUnmodifiable() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder()
                    .putCounts("key", 1)
                    .build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            Map<String, Integer> map = wrapper.getCountsMap();
            assertThatThrownBy(() -> map.put("new", 2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void multipleMapFields() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder()
                    .putCounts("item", 10)
                    .putLabels("color", "red")
                    .putIndexedNames(1L, "first")
                    .putIndexedNames(2L, "second")
                    .build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            assertThat(wrapper.getCountsMap()).containsEntry("item", 10);
            assertThat(wrapper.getLabelsMap()).containsEntry("color", "red");
            assertThat(wrapper.getIndexedNamesMap()).hasSize(2);
            assertThat(wrapper.getIndexedNamesOrThrow(1L)).isEqualTo("first");
        }
    }

    @Nested
    class BuilderMapTests {

        @Test
        void buildWithMapEntries() {
            VersionContext ctx = VersionContext.forVersion(1);
            MapTestMessage msg = MapTestMessage.newBuilder(ctx)
                    .putCounts("key1", 100)
                    .putCounts("key2", 200)
                    .build();

            assertThat(msg.getCountsMap()).hasSize(2);
            assertThat(msg.getCountsOrThrow("key1")).isEqualTo(100);
            assertThat(msg.getCountsOrThrow("key2")).isEqualTo(200);
        }

        @Test
        void buildWithPutAll() {
            VersionContext ctx = VersionContext.forVersion(1);
            MapTestMessage msg = MapTestMessage.newBuilder(ctx)
                    .putAllCounts(Map.of("a", 1, "b", 2, "c", 3))
                    .build();

            assertThat(msg.getCountsMap()).hasSize(3);
            assertThat(msg.getCountsCount()).isEqualTo(3);
        }

        @Test
        void builderRemoveEntry() {
            VersionContext ctx = VersionContext.forVersion(1);
            MapTestMessage msg = MapTestMessage.newBuilder(ctx)
                    .putCounts("keep", 1)
                    .putCounts("remove", 2)
                    .removeCounts("remove")
                    .build();

            assertThat(msg.getCountsMap()).hasSize(1);
            assertThat(msg.containsCounts("keep")).isTrue();
            assertThat(msg.containsCounts("remove")).isFalse();
        }

        @Test
        void builderClearMap() {
            VersionContext ctx = VersionContext.forVersion(1);
            MapTestMessage msg = MapTestMessage.newBuilder(ctx)
                    .putCounts("key1", 1)
                    .putCounts("key2", 2)
                    .clearCounts()
                    .build();

            assertThat(msg.getCountsMap()).isEmpty();
        }

        @Test
        void builderGetMapForInspection() {
            VersionContext ctx = VersionContext.forVersion(1);
            MapTestMessage.Builder builder = MapTestMessage.newBuilder(ctx)
                    .putCounts("key", 42);

            Map<String, Integer> currentMap = builder.getCountsMap();
            assertThat(currentMap).containsEntry("key", 42);
        }
    }

    @Nested
    class VersionSpecificMapTests {

        @Test
        void v2OnlyField_returnsEmptyInV1() {
            MapTest.MapTestMessage proto = MapTest.MapTestMessage.newBuilder().build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(proto);

            // scores field only exists in v2
            assertThat(wrapper.getScoresMap()).isEmpty();
            assertThat(wrapper.supportsScores()).isFalse();
        }

        @Test
        void v2OnlyField_worksInV2() {
            space.alnovis.protowrapper.it.proto.v2.MapTest.MapTestMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.MapTest.MapTestMessage.newBuilder()
                            .putScores("math", 95.5)
                            .putScores("science", 88.0)
                            .build();
            MapTestMessage wrapper = new space.alnovis.protowrapper.it.model.v2.MapTestMessage(proto);

            assertThat(wrapper.getScoresMap()).hasSize(2);
            assertThat(wrapper.getScoresOrThrow("math")).isEqualTo(95.5);
            assertThat(wrapper.supportsScores()).isTrue();
        }
    }

    @Nested
    class NonSequentialEnumTests {
        /**
         * Tests that non-sequential enum values are correctly converted using getNumber()
         * instead of ordinal(). The TaskPriority enum has values: 0, 10, 50, 100.
         * Using ordinal() would incorrectly return 0, 1, 2, 3.
         */
        @Test
        void readNonSequentialEnumFromV2() {
            // Create v2 proto with non-sequential enum values
            MapConflicts.MapConflictMessage v2Proto = MapConflicts.MapConflictMessage.newBuilder()
                    .putPriorityMap("low", TASK_PRIORITY_LOW)       // getNumber=10, ordinal=1
                    .putPriorityMap("medium", TASK_PRIORITY_MEDIUM) // getNumber=50, ordinal=2
                    .putPriorityMap("high", TASK_PRIORITY_HIGH)     // getNumber=100, ordinal=3
                    .build();

            MapConflictMessage wrapper =
                    new space.alnovis.protowrapper.it.model.v2.MapConflictMessage(v2Proto);

            // These values MUST be 10, 50, 100 (getNumber), NOT 1, 2, 3 (ordinal)
            assertThat(wrapper.getPriorityMapOrThrow("low")).isEqualTo(10);
            assertThat(wrapper.getPriorityMapOrThrow("medium")).isEqualTo(50);
            assertThat(wrapper.getPriorityMapOrThrow("high")).isEqualTo(100);
        }

        @Test
        void builderNonSequentialEnumFromV2() {
            // Build from V2 using int values
            MapConflictMessage msg =
                    space.alnovis.protowrapper.it.model.v2.MapConflictMessage.newBuilder()
                            .putPriorityMap("task1", 10)   // TASK_PRIORITY_LOW
                            .putPriorityMap("task2", 100)  // TASK_PRIORITY_HIGH
                            .build();

            // Values must be preserved correctly
            assertThat(msg.getPriorityMapOrThrow("task1")).isEqualTo(10);
            assertThat(msg.getPriorityMapOrThrow("task2")).isEqualTo(100);
        }

        @Test
        void builderGetMapReturnsCorrectValues() {
            // Build from V2 and check intermediate state
            MapConflictMessage.Builder builder =
                    space.alnovis.protowrapper.it.model.v2.MapConflictMessage.newBuilder()
                            .putPriorityMap("item", 50);   // TASK_PRIORITY_MEDIUM

            // Builder.getMap must return 50 (getNumber), not 2 (ordinal)
            assertThat(builder.getPriorityMapMap().get("item")).isEqualTo(50);
        }

        @Test
        void crossVersionConversion_preservesNonSequentialValues() {
            // Create v2 proto with non-sequential enum
            MapConflicts.MapConflictMessage v2Proto = MapConflicts.MapConflictMessage.newBuilder()
                    .putPriorityMap("key", TASK_PRIORITY_HIGH) // 100
                    .build();

            MapConflictMessage v2Wrapper =
                    new space.alnovis.protowrapper.it.model.v2.MapConflictMessage(v2Proto);

            // Convert to v1 and back
            MapConflictMessage v1Wrapper =
                    v2Wrapper.asVersion(space.alnovis.protowrapper.it.model.v1.MapConflictMessage.class);

            // Value must be 100, not 3
            assertThat(v1Wrapper.getPriorityMapOrThrow("key")).isEqualTo(100);
        }

        @Test
        void putInvalidEnumValue_throwsException() {
            // TaskPriority valid values are: 0, 10, 50, 100
            // Value 999 is invalid and should throw IllegalArgumentException
            MapConflictMessage.Builder builder =
                    space.alnovis.protowrapper.it.model.v2.MapConflictMessage.newBuilder();

            assertThatThrownBy(() -> builder.putPriorityMap("key", 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999")
                    .hasMessageContaining("priority_map");
        }

        @Test
        void putAllInvalidEnumValue_throwsException() {
            // putAll should also validate enum values
            MapConflictMessage.Builder builder =
                    space.alnovis.protowrapper.it.model.v2.MapConflictMessage.newBuilder();

            assertThatThrownBy(() -> builder.putAllPriorityMap(Map.of("valid", 10, "invalid", 777)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("777");
        }
    }

    @Nested
    class CrossVersionTests {

        @Test
        void v1ToV2Conversion_preservesCommonMaps() {
            MapTest.MapTestMessage v1Proto = MapTest.MapTestMessage.newBuilder()
                    .putCounts("item", 5)
                    .putLabels("key", "value")
                    .build();
            MapTestMessage v1Wrapper = new space.alnovis.protowrapper.it.model.v1.MapTestMessage(v1Proto);

            MapTestMessage v2Wrapper = v1Wrapper.asVersion(
                    space.alnovis.protowrapper.it.model.v2.MapTestMessage.class);

            assertThat(v2Wrapper.getCountsMap()).containsEntry("item", 5);
            assertThat(v2Wrapper.getLabelsMap()).containsEntry("key", "value");
            assertThat(v2Wrapper.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        void v2ToV1Conversion_preservesCommonMaps() {
            space.alnovis.protowrapper.it.proto.v2.MapTest.MapTestMessage v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.MapTest.MapTestMessage.newBuilder()
                            .putCounts("product", 10)
                            .putScores("test", 99.9)
                            .build();
            MapTestMessage v2Wrapper = new space.alnovis.protowrapper.it.model.v2.MapTestMessage(v2Proto);

            MapTestMessage v1Wrapper = v2Wrapper.asVersion(
                    space.alnovis.protowrapper.it.model.v1.MapTestMessage.class);

            assertThat(v1Wrapper.getCountsMap()).containsEntry("product", 10);
            assertThat(v1Wrapper.getScoresMap()).isEmpty(); // v2-only field not accessible in v1
            assertThat(v1Wrapper.getWrapperVersion()).isEqualTo(1);
        }
    }
}
