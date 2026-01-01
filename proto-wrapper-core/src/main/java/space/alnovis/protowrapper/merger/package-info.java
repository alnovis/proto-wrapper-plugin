/**
 * Version schema merging and conflict detection.
 *
 * <p>This package merges multiple protobuf schema versions into a unified
 * schema, detecting and classifying type conflicts between versions.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.merger.VersionMerger} - Merges multiple
 *       version schemas into unified {@link space.alnovis.protowrapper.model.MergedSchema}</li>
 *   <li>{@link space.alnovis.protowrapper.merger.MergerConfig} - Configuration options
 *       for the merging process</li>
 * </ul>
 *
 * <h2>Merging Process</h2>
 * <ol>
 *   <li>Collect all message names across versions</li>
 *   <li>For each message, merge fields by field number</li>
 *   <li>Detect type conflicts (INT_ENUM, STRING_BYTES, WIDENING, etc.)</li>
 *   <li>Detect equivalent enums (nested vs top-level)</li>
 *   <li>Generate conflict enum info for INT_ENUM conflicts</li>
 * </ol>
 *
 * <h2>Conflict Types</h2>
 * <ul>
 *   <li><b>NONE</b> - Same type across all versions</li>
 *   <li><b>INT_ENUM</b> - int in some versions, enum in others</li>
 *   <li><b>STRING_BYTES</b> - string in some versions, bytes in others</li>
 *   <li><b>WIDENING</b> - int32 in some versions, int64 in others</li>
 *   <li><b>PRIMITIVE_MESSAGE</b> - primitive in some, message in others</li>
 *   <li><b>INCOMPATIBLE</b> - Cannot be unified (e.g., int vs string)</li>
 * </ul>
 *
 * @see space.alnovis.protowrapper.merger.VersionMerger
 * @see space.alnovis.protowrapper.model.MergedField.ConflictType
 */
package space.alnovis.protowrapper.merger;
