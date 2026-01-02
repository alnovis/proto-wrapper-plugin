/**
 * Proto file analysis and parsing.
 *
 * <p>This package provides functionality for parsing protobuf definition files
 * using the protoc compiler and extracting structured information about
 * messages, fields, enums, and other proto constructs.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.analyzer.ProtoAnalyzer} - Parses proto files
 *       and extracts schema information</li>
 *   <li>{@link space.alnovis.protowrapper.analyzer.ProtocExecutor} - Executes the protoc
 *       compiler to generate FileDescriptorSet</li>
 * </ul>
 *
 * <h2>Processing Flow</h2>
 * <pre>
 * .proto files
 *     |
 *     v
 * ProtocExecutor (runs protoc --descriptor_set_out)
 *     |
 *     v
 * FileDescriptorSet (binary proto)
 *     |
 *     v
 * ProtoAnalyzer (extracts MessageInfo, FieldInfo, EnumInfo)
 *     |
 *     v
 * VersionSchema (structured data for merger)
 * </pre>
 *
 * <h2>Requirements</h2>
 * <p>The protoc compiler must be available on the system PATH or configured
 * explicitly in the plugin settings.</p>
 *
 * @see space.alnovis.protowrapper.analyzer.ProtoAnalyzer
 * @see space.alnovis.protowrapper.merger.VersionMerger
 */
package space.alnovis.protowrapper.analyzer;
