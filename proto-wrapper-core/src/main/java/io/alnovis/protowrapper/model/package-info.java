/**
 * Domain model classes for merged protobuf schemas.
 *
 * <p>This package contains the data model representing a unified schema
 * merged from multiple protobuf versions. These classes are used by both
 * the merger and generator components.</p>
 *
 * <h2>Core Model Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.model.MergedSchema} - Container for all merged types</li>
 *   <li>{@link io.alnovis.protowrapper.model.MergedMessage} - Unified message type</li>
 *   <li>{@link io.alnovis.protowrapper.model.MergedField} - Field with version info and conflict type</li>
 *   <li>{@link io.alnovis.protowrapper.model.MergedEnum} - Unified enum type</li>
 *   <li>{@link io.alnovis.protowrapper.model.MergedOneof} - Oneof group spanning versions</li>
 * </ul>
 *
 * <h2>Raw Schema Classes</h2>
 * <p>These classes represent data extracted directly from proto descriptors:</p>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.model.MessageInfo} - Raw message info</li>
 *   <li>{@link io.alnovis.protowrapper.model.FieldInfo} - Raw field info</li>
 *   <li>{@link io.alnovis.protowrapper.model.EnumInfo} - Raw enum info</li>
 *   <li>{@link io.alnovis.protowrapper.model.MapInfo} - Map field key/value types</li>
 * </ul>
 *
 * <h2>Conflict Resolution Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.model.ConflictEnumInfo} - Enum generated for INT_ENUM conflicts</li>
 * </ul>
 *
 * <h2>Model Relationships</h2>
 * <pre>
 * MergedSchema
 *   |-- MergedMessage (1:N)
 *   |     |-- MergedField (1:N)
 *   |     |     |-- FieldInfo per version
 *   |     |     +-- MapInfo (optional)
 *   |     |-- MergedOneof (1:N)
 *   |     +-- MergedEnum (nested, 1:N)
 *   |-- MergedEnum (top-level, 1:N)
 *   +-- ConflictEnumInfo (1:N)
 * </pre>
 *
 * @see io.alnovis.protowrapper.model.MergedSchema
 * @see io.alnovis.protowrapper.merger.VersionMerger
 */
package io.alnovis.protowrapper.model;
