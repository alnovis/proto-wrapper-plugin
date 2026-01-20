package io.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.alnovis.protowrapper.model.ProtoSyntax.PROTO2;

/**
 * Represents information about a protobuf message type.
 */
public class MessageInfo {

    private final String name;
    private final String fullName;
    private final String packageName;
    private final String sourceFileName;
    private final List<FieldInfo> fields;
    private final List<MessageInfo> nestedMessages;
    private final List<EnumInfo> nestedEnums;
    private final List<OneofInfo> oneofGroups;
    private final boolean isMapEntry;
    private final ProtoSyntax syntax;

    /**
     * Create a MessageInfo from a protobuf descriptor.
     *
     * @param proto the message descriptor
     * @param packageName the Java package name
     */
    public MessageInfo(DescriptorProto proto, String packageName) {
        this(proto, packageName, packageName + "." + proto.getName(), null, PROTO2);
    }

    /**
     * Create a MessageInfo from a protobuf descriptor with source file.
     *
     * @param proto the message descriptor
     * @param packageName the Java package name
     * @param sourceFileName the source proto file name
     */
    public MessageInfo(DescriptorProto proto, String packageName, String sourceFileName) {
        this(proto, packageName, packageName + "." + proto.getName(), sourceFileName, PROTO2);
    }

    /**
     * Create a MessageInfo from a protobuf descriptor with source file and syntax.
     *
     * @param proto the message descriptor
     * @param packageName the Java package name
     * @param sourceFileName the source proto file name
     * @param syntax the proto syntax version
     */
    public MessageInfo(DescriptorProto proto, String packageName, String sourceFileName, ProtoSyntax syntax) {
        this(proto, packageName, packageName + "." + proto.getName(), sourceFileName, syntax);
    }

    private MessageInfo(DescriptorProto proto, String packageName, String fullName, String sourceFileName, ProtoSyntax syntax) {
        this.name = proto.getName();
        this.fullName = fullName;
        this.packageName = packageName;
        this.sourceFileName = sourceFileName;
        this.isMapEntry = proto.getOptions().getMapEntry();
        this.syntax = syntax;

        // Extract oneof groups first (needed for field creation)
        this.oneofGroups = extractOneofGroups(proto);

        // Build map of entry type names to their descriptors for map field detection
        Map<String, DescriptorProto> mapEntries = proto.getNestedTypeList().stream()
                .filter(nested -> nested.getOptions().getMapEntry())
                .collect(Collectors.toMap(DescriptorProto::getName, Function.identity()));

        // Create fields with oneof information, map entries, and syntax info
        this.fields = createFieldsWithOneofInfo(proto, this.oneofGroups, mapEntries, syntax);

        this.nestedMessages = proto.getNestedTypeList().stream()
                .filter(nested -> !nested.getOptions().getMapEntry()) // Skip map entries
                .map(nested -> new MessageInfo(nested, packageName, fullName + "." + nested.getName(), sourceFileName, syntax))
                .toList();

        this.nestedEnums = proto.getEnumTypeList().stream()
                .map(EnumInfo::new)
                .toList();
    }

    /**
     * Extracts oneof groups from a DescriptorProto.
     * Filters out synthetic oneofs (used for proto3 optional fields).
     *
     * @param proto the message descriptor
     * @return list of oneof groups
     */
    private static List<OneofInfo> extractOneofGroups(DescriptorProto proto) {
        List<OneofDescriptorProto> oneofProtos = proto.getOneofDeclList();
        if (oneofProtos.isEmpty()) {
            return List.of();
        }

        List<OneofInfo> result = new ArrayList<>();

        for (int i = 0; i < oneofProtos.size(); i++) {
            OneofDescriptorProto oneofProto = oneofProtos.get(i);
            String oneofName = oneofProto.getName();

            // Skip synthetic oneofs (proto3 optional fields create synthetic oneofs starting with "_")
            if (isSyntheticOneof(oneofName, proto, i)) {
                continue;
            }

            // Find all field numbers in this oneof
            List<Integer> fieldNumbers = new ArrayList<>();
            for (FieldDescriptorProto field : proto.getFieldList()) {
                if (field.hasOneofIndex() && field.getOneofIndex() == i) {
                    fieldNumbers.add(field.getNumber());
                }
            }

            if (!fieldNumbers.isEmpty()) {
                result.add(new OneofInfo(oneofName, i, fieldNumbers));
            }
        }

        return result;
    }

    /**
     * Checks if a oneof is synthetic (created for proto3 optional fields).
     * Synthetic oneofs have names starting with "_" and contain exactly one optional field.
     *
     * @param oneofName the oneof name
     * @param proto the message descriptor
     * @param oneofIndex the oneof index
     * @return true if the oneof is synthetic
     */
    private static boolean isSyntheticOneof(String oneofName, DescriptorProto proto, int oneofIndex) {
        // Synthetic oneofs start with underscore
        if (!oneofName.startsWith("_")) {
            return false;
        }

        // Count fields in this oneof
        int fieldCount = 0;
        for (FieldDescriptorProto field : proto.getFieldList()) {
            if (field.hasOneofIndex() && field.getOneofIndex() == oneofIndex) {
                fieldCount++;
            }
        }

        // Synthetic oneofs have exactly one field
        return fieldCount == 1;
    }

    /**
     * Creates FieldInfo objects with oneof information, map entry access, and syntax info.
     *
     * @param proto the message descriptor
     * @param oneofGroups the list of oneof groups
     * @param mapEntries map of entry type names to their descriptors
     * @param syntax the proto syntax version
     * @return list of field info objects
     */
    private static List<FieldInfo> createFieldsWithOneofInfo(DescriptorProto proto, List<OneofInfo> oneofGroups,
                                                              Map<String, DescriptorProto> mapEntries, ProtoSyntax syntax) {
        // Build a map from field number to oneof info
        Map<Integer, OneofInfo> fieldToOneof = new HashMap<>();
        for (OneofInfo oneof : oneofGroups) {
            for (Integer fieldNumber : oneof.getFieldNumbers()) {
                fieldToOneof.put(fieldNumber, oneof);
            }
        }

        List<FieldInfo> result = new ArrayList<>();
        for (FieldDescriptorProto fieldProto : proto.getFieldList()) {
            OneofInfo oneof = fieldToOneof.get(fieldProto.getNumber());
            if (oneof != null) {
                result.add(new FieldInfo(fieldProto, oneof.getIndex(), oneof.getProtoName(), mapEntries, syntax));
            } else {
                result.add(new FieldInfo(fieldProto, -1, null, mapEntries, syntax));
            }
        }
        return result;
    }

    /**
     * Constructor for merged messages.
     *
     * @param name the message name
     * @param fullName the fully qualified name
     * @param packageName the Java package name
     * @param fields the list of fields
     * @param nestedMessages the list of nested messages
     * @param nestedEnums the list of nested enums
     */
    public MessageInfo(String name, String fullName, String packageName,
                       List<FieldInfo> fields, List<MessageInfo> nestedMessages,
                       List<EnumInfo> nestedEnums) {
        this(name, fullName, packageName, fields, nestedMessages, nestedEnums, List.of());
    }

    /**
     * Constructor for merged messages with oneof groups.
     *
     * @param name the message name
     * @param fullName the fully qualified name
     * @param packageName the Java package name
     * @param fields the list of fields
     * @param nestedMessages the list of nested messages
     * @param nestedEnums the list of nested enums
     * @param oneofGroups the list of oneof groups
     */
    public MessageInfo(String name, String fullName, String packageName,
                       List<FieldInfo> fields, List<MessageInfo> nestedMessages,
                       List<EnumInfo> nestedEnums, List<OneofInfo> oneofGroups) {
        this.name = name;
        this.fullName = fullName;
        this.packageName = packageName;
        this.sourceFileName = null;
        this.fields = new ArrayList<>(fields);
        this.nestedMessages = new ArrayList<>(nestedMessages);
        this.nestedEnums = new ArrayList<>(nestedEnums);
        this.oneofGroups = new ArrayList<>(oneofGroups);
        this.isMapEntry = false;
        this.syntax = PROTO2; // Merged messages default to proto2
    }

    /**
     * Get fields sorted by field number.
     *
     * @return list of fields sorted by number
     */
    public List<FieldInfo> getFieldsSorted() {
        return fields.stream()
                .sorted(Comparator.comparingInt(FieldInfo::getNumber))
                .toList();
    }

    /**
     * Get required fields (non-optional, non-repeated).
     *
     * @return list of required fields
     */
    public List<FieldInfo> getRequiredFields() {
        return fields.stream()
                .filter(f -> !f.isOptional() && !f.isRepeated())
                .toList();
    }

    /**
     * Get optional primitive fields (need has-check pattern).
     *
     * @return list of optional primitive fields
     */
    public List<FieldInfo> getOptionalPrimitiveFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isPrimitive())
                .toList();
    }

    /**
     * Get optional message fields.
     *
     * @return list of optional message fields
     */
    public List<FieldInfo> getOptionalMessageFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isMessage())
                .toList();
    }

    /**
     * Get repeated fields (lists), excluding map fields.
     *
     * @return list of repeated fields
     */
    public List<FieldInfo> getRepeatedFields() {
        return fields.stream()
                .filter(f -> f.isRepeated() && !f.isMap())
                .toList();
    }

    /**
     * Get map fields.
     *
     * @return list of map fields
     */
    public List<FieldInfo> getMapFields() {
        return fields.stream()
                .filter(FieldInfo::isMap)
                .toList();
    }

    /**
     * Find field by name.
     *
     * @param name the field name
     * @return the field if found
     */
    public Optional<FieldInfo> findField(String name) {
        return fields.stream()
                .filter(f -> f.getProtoName().equals(name) || f.getJavaName().equals(name))
                .findFirst();
    }

    /**
     * Find field by number.
     *
     * @param number the field number
     * @return the field if found
     */
    public Optional<FieldInfo> findFieldByNumber(int number) {
        return fields.stream()
                .filter(f -> f.getNumber() == number)
                .findFirst();
    }

    /**
     * Check if this message has nested types.
     *
     * @return true if there are nested types
     */
    public boolean hasNestedTypes() {
        return !nestedMessages.isEmpty() || !nestedEnums.isEmpty();
    }

    /**
     * Get all nested messages recursively.
     *
     * @return list of all nested messages
     */
    public List<MessageInfo> getAllNestedMessages() {
        List<MessageInfo> result = new ArrayList<>(nestedMessages);
        for (MessageInfo nested : nestedMessages) {
            result.addAll(nested.getAllNestedMessages());
        }
        return result;
    }

    /**
     * Generate interface name (e.g., "Order" -> "Order").
     *
     * @return the interface name
     */
    public String getInterfaceName() {
        return name;
    }

    /**
     * Generate abstract class name (e.g., "Order" -> "AbstractOrder").
     *
     * @return the abstract class name
     */
    public String getAbstractClassName() {
        return "Abstract" + name;
    }

    /**
     * Generate version-specific class name (e.g., "Order", "v1" -> "OrderV1").
     *
     * @param version the version identifier
     * @return the version-specific class name
     */
    public String getVersionClassName(String version) {
        return name + version.toUpperCase();
    }

    /** @return the message name */
    public String getName() { return name; }
    /** @return the fully qualified name */
    public String getFullName() { return fullName; }
    /** @return the Java package name */
    public String getPackageName() { return packageName; }
    /** @return the source proto file name */
    public String getSourceFileName() { return sourceFileName; }
    /** @return unmodifiable list of fields */
    public List<FieldInfo> getFields() { return Collections.unmodifiableList(fields); }
    /** @return unmodifiable list of nested messages */
    public List<MessageInfo> getNestedMessages() { return Collections.unmodifiableList(nestedMessages); }
    /** @return unmodifiable list of nested enums */
    public List<EnumInfo> getNestedEnums() { return Collections.unmodifiableList(nestedEnums); }
    /** @return unmodifiable list of oneof groups */
    public List<OneofInfo> getOneofGroups() { return Collections.unmodifiableList(oneofGroups); }
    /** @return true if this is a map entry message */
    public boolean isMapEntry() { return isMapEntry; }
    /** @return the proto syntax version */
    public ProtoSyntax getSyntax() { return syntax; }
    /** @return true if proto3 syntax */
    public boolean isProto3() { return syntax.isProto3(); }

    /**
     * Check if this message has any oneof groups.
     *
     * @return true if there are oneof groups
     */
    public boolean hasOneofGroups() {
        return !oneofGroups.isEmpty();
    }

    /**
     * Find oneof group by name.
     *
     * @param name the oneof name
     * @return the oneof if found
     */
    public Optional<OneofInfo> findOneofByName(String name) {
        return oneofGroups.stream()
                .filter(o -> o.getProtoName().equals(name) || o.getJavaName().equals(name))
                .findFirst();
    }

    /**
     * Find oneof group containing a specific field.
     *
     * @param field the field to search for
     * @return the oneof containing the field, if found
     */
    public Optional<OneofInfo> findOneofForField(FieldInfo field) {
        return oneofGroups.stream()
                .filter(o -> o.containsField(field.getNumber()))
                .findFirst();
    }

    /**
     * Get fields that are part of a specific oneof group.
     *
     * @param oneof the oneof group
     * @return list of fields in the oneof
     */
    public List<FieldInfo> getFieldsInOneof(OneofInfo oneof) {
        return fields.stream()
                .filter(f -> oneof.containsField(f.getNumber()))
                .toList();
    }

    /**
     * Get fields that are NOT part of any oneof group.
     *
     * @return list of non-oneof fields
     */
    public List<FieldInfo> getNonOneofFields() {
        return fields.stream()
                .filter(f -> !f.isInOneof())
                .toList();
    }

    /**
     * Get the outer class name derived from the source file name.
     * E.g., "common.proto" -> "Common", "user_request.proto" -> "UserRequest"
     *
     * @return the outer class name
     */
    public String getOuterClassName() {
        if (sourceFileName == null) {
            return null;
        }
        // Remove path prefix if present (e.g., "v1/common.proto" -> "common.proto")
        String fileName = sourceFileName;
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        // Remove .proto extension
        if (fileName.endsWith(".proto")) {
            fileName = fileName.substring(0, fileName.length() - 6);
        }
        // Convert snake_case to PascalCase
        return toPascalCase(fileName);
    }

    private String toPascalCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return String.format("MessageInfo[%s, %d fields, %d nested]",
            name, fields.size(), nestedMessages.size());
    }
}
