package io.alnovis.protowrapper.analyzer;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.MessageInfo;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes protobuf descriptor files and extracts message/enum information.
 *
 * <p>Usage:</p>
 * <pre>
 * ProtoAnalyzer analyzer = new ProtoAnalyzer();
 * VersionSchema schema = analyzer.analyze(descriptorFile, "v1");
 * </pre>
 *
 * <p>To generate descriptor file:</p>
 * <pre>
 * protoc --descriptor_set_out=schema.pb --include_imports *.proto
 * </pre>
 */
public class ProtoAnalyzer {

    /**
     * Analyze a protobuf descriptor file.
     *
     * @param descriptorFile Path to .pb descriptor file
     * @param version Version identifier (e.g., "v1")
     * @return VersionSchema containing all messages and enums
     * @throws IOException if file cannot be read
     */
    public VersionSchema analyze(Path descriptorFile, String version) throws IOException {
        return analyze(descriptorFile, version, null);
    }

    /**
     * Analyze a protobuf descriptor file, filtering by source directory.
     *
     * @param descriptorFile Path to .pb descriptor file
     * @param version Version identifier (e.g., "v1")
     * @param sourcePrefix Only include proto files starting with this prefix (e.g., "v1/")
     * @return VersionSchema containing all messages and enums
     * @throws IOException if file cannot be read
     */
    public VersionSchema analyze(Path descriptorFile, String version, String sourcePrefix) throws IOException {
        try (InputStream is = Files.newInputStream(descriptorFile)) {
            FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(is);
            return analyze(descriptorSet, version, sourcePrefix);
        }
    }

    /**
     * Analyze a protobuf descriptor from input stream.
     *
     * @param inputStream Input stream containing descriptor set
     * @param version Version identifier
     * @return VersionSchema
     * @throws IOException if stream cannot be read
     */
    public VersionSchema analyze(InputStream inputStream, String version) throws IOException {
        FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(inputStream);
        return analyze(descriptorSet, version, null);
    }

    /**
     * Analyze a FileDescriptorSet directly.
     *
     * @param descriptorSet Protobuf descriptor set
     * @param version Version identifier
     * @return VersionSchema
     */
    public VersionSchema analyze(FileDescriptorSet descriptorSet, String version) {
        return analyze(descriptorSet, version, null);
    }

    /**
     * Analyze a FileDescriptorSet with source file filtering.
     *
     * @param descriptorSet Protobuf descriptor set
     * @param version Version identifier
     * @param sourcePrefix Only include proto files starting with this prefix (e.g., "v1/"), null for no filtering
     * @return VersionSchema
     */
    public VersionSchema analyze(FileDescriptorSet descriptorSet, String version, String sourcePrefix) {
        VersionSchema schema = new VersionSchema(version);

        boolean hasProto2 = false;
        boolean hasProto3 = false;

        for (FileDescriptorProto fileProto : descriptorSet.getFileList()) {
            String packageName = fileProto.getPackage();
            String sourceFileName = fileProto.getName();

            // Skip google protobuf internal types
            if (packageName.startsWith("google.protobuf")) {
                continue;
            }

            // Filter by source prefix if specified (only include files from the version directory)
            if (sourcePrefix != null && !sourceFileName.startsWith(sourcePrefix)) {
                continue;
            }

            // Determine proto syntax version
            ProtoSyntax syntax = ProtoSyntax.fromSyntaxString(fileProto.getSyntax());

            // Track syntax for version-level detection
            if (syntax == ProtoSyntax.PROTO2) {
                hasProto2 = true;
            } else if (syntax == ProtoSyntax.PROTO3) {
                hasProto3 = true;
            }

            // Process top-level messages
            for (DescriptorProto messageProto : fileProto.getMessageTypeList()) {
                MessageInfo messageInfo = new MessageInfo(messageProto, packageName, sourceFileName, syntax);
                if (!messageInfo.isMapEntry()) {
                    schema.addMessage(messageInfo);
                }
            }

            // Process top-level enums
            for (EnumDescriptorProto enumProto : fileProto.getEnumTypeList()) {
                schema.addEnum(new EnumInfo(enumProto, sourceFileName));
            }
        }

        // Determine dominant syntax for this version
        // If all files are proto3, use PROTO3; otherwise use PROTO2 (conservative)
        if (hasProto3 && !hasProto2) {
            schema.setDetectedSyntax(ProtoSyntax.PROTO3);
        } else {
            schema.setDetectedSyntax(ProtoSyntax.PROTO2);
        }

        return schema;
    }

    /**
     * Represents the schema of a single protobuf version.
     */
    public static class VersionSchema {
        private final String version;
        private final Map<String, MessageInfo> messages;
        private final Map<String, EnumInfo> enums;
        private ProtoSyntax detectedSyntax;

        /**
         * Create a new VersionSchema for the specified version.
         *
         * @param version the version identifier
         */
        public VersionSchema(String version) {
            this.version = version;
            this.messages = new LinkedHashMap<>();
            this.enums = new LinkedHashMap<>();
            this.detectedSyntax = ProtoSyntax.PROTO2; // default
        }

        /**
         * Add a message to this schema.
         *
         * @param message the message to add
         */
        public void addMessage(MessageInfo message) {
            messages.put(message.getName(), message);
        }

        /**
         * Add an enum to this schema.
         *
         * @param enumInfo the enum to add
         */
        public void addEnum(EnumInfo enumInfo) {
            enums.put(enumInfo.getName(), enumInfo);
        }

        /**
         * Returns the version identifier.
         *
         * @return the version identifier
         */
        public String getVersion() {
            return version;
        }

        /**
         * Returns the detected proto syntax for this version.
         *
         * @return the detected syntax (PROTO2 or PROTO3)
         */
        public ProtoSyntax getDetectedSyntax() {
            return detectedSyntax;
        }

        /**
         * Sets the detected proto syntax for this version.
         *
         * @param syntax the detected syntax
         */
        public void setDetectedSyntax(ProtoSyntax syntax) {
            this.detectedSyntax = syntax != null ? syntax : ProtoSyntax.PROTO2;
        }

        /**
         * Get a message by name.
         *
         * @param name the message name
         * @return an Optional containing the message if found
         */
        public Optional<MessageInfo> getMessage(String name) {
            return Optional.ofNullable(messages.get(name));
        }

        /**
         * Get an enum by name.
         *
         * @param name the enum name
         * @return an Optional containing the enum if found
         */
        public Optional<EnumInfo> getEnum(String name) {
            return Optional.ofNullable(enums.get(name));
        }

        /**
         * Returns all messages in this schema.
         *
         * @return unmodifiable collection of messages
         */
        public Collection<MessageInfo> getMessages() {
            return Collections.unmodifiableCollection(messages.values());
        }

        /**
         * Returns all enums in this schema.
         *
         * @return unmodifiable collection of enums
         */
        public Collection<EnumInfo> getEnums() {
            return Collections.unmodifiableCollection(enums.values());
        }

        /**
         * Returns all message names in this schema.
         *
         * @return unmodifiable set of message names
         */
        public Set<String> getMessageNames() {
            return Collections.unmodifiableSet(messages.keySet());
        }

        /**
         * Returns all enum names in this schema.
         *
         * @return unmodifiable set of enum names
         */
        public Set<String> getEnumNames() {
            return Collections.unmodifiableSet(enums.keySet());
        }

        /**
         * Find all messages that match a pattern.
         *
         * @param pattern Simple pattern with * wildcard
         * @return Matching messages
         */
        public List<MessageInfo> findMessages(String pattern) {
            String regex = pattern.replace("*", ".*");
            return messages.values().stream()
                    .filter(m -> m.getName().matches(regex))
                    .toList();
        }

        /**
         * Get statistics about this schema.
         *
         * @return formatted statistics string
         */
        public String getStats() {
            int totalFields = messages.values().stream()
                    .mapToInt(m -> m.getFields().size())
                    .sum();
            int totalNested = messages.values().stream()
                    .mapToInt(m -> m.getNestedMessages().size())
                    .sum();

            return String.format("Version %s: %d messages, %d enums, %d total fields, %d nested types",
                    version, messages.size(), enums.size(), totalFields, totalNested);
        }

        @Override
        public String toString() {
            return getStats();
        }
    }
}
