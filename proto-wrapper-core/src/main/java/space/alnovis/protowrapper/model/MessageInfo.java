package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;

import java.util.*;
import java.util.stream.Collectors;

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

    public MessageInfo(DescriptorProto proto, String packageName) {
        this(proto, packageName, packageName + "." + proto.getName(), null);
    }

    public MessageInfo(DescriptorProto proto, String packageName, String sourceFileName) {
        this(proto, packageName, packageName + "." + proto.getName(), sourceFileName);
    }

    private MessageInfo(DescriptorProto proto, String packageName, String fullName, String sourceFileName) {
        this.name = proto.getName();
        this.fullName = fullName;
        this.packageName = packageName;
        this.sourceFileName = sourceFileName;
        this.isMapEntry = proto.getOptions().getMapEntry();

        // Extract oneof groups first (needed for field creation)
        this.oneofGroups = extractOneofGroups(proto);

        // Create fields with oneof information
        this.fields = createFieldsWithOneofInfo(proto, this.oneofGroups);

        this.nestedMessages = proto.getNestedTypeList().stream()
                .filter(nested -> !nested.getOptions().getMapEntry()) // Skip map entries
                .map(nested -> new MessageInfo(nested, packageName, fullName + "." + nested.getName(), sourceFileName))
                .toList();

        this.nestedEnums = proto.getEnumTypeList().stream()
                .map(EnumInfo::new)
                .toList();
    }

    /**
     * Extracts oneof groups from a DescriptorProto.
     * Filters out synthetic oneofs (used for proto3 optional fields).
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
     * Creates FieldInfo objects with oneof information.
     */
    private static List<FieldInfo> createFieldsWithOneofInfo(DescriptorProto proto, List<OneofInfo> oneofGroups) {
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
                result.add(new FieldInfo(fieldProto, oneof.getIndex(), oneof.getProtoName()));
            } else {
                result.add(new FieldInfo(fieldProto));
            }
        }
        return result;
    }

    // Constructor for merged messages
    public MessageInfo(String name, String fullName, String packageName,
                       List<FieldInfo> fields, List<MessageInfo> nestedMessages,
                       List<EnumInfo> nestedEnums) {
        this(name, fullName, packageName, fields, nestedMessages, nestedEnums, List.of());
    }

    // Constructor for merged messages with oneof groups
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
    }

    /**
     * Get fields sorted by field number.
     */
    public List<FieldInfo> getFieldsSorted() {
        return fields.stream()
                .sorted(Comparator.comparingInt(FieldInfo::getNumber))
                .toList();
    }

    /**
     * Get required fields (non-optional, non-repeated).
     */
    public List<FieldInfo> getRequiredFields() {
        return fields.stream()
                .filter(f -> !f.isOptional() && !f.isRepeated())
                .toList();
    }

    /**
     * Get optional primitive fields (need has-check pattern).
     */
    public List<FieldInfo> getOptionalPrimitiveFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isPrimitive())
                .toList();
    }

    /**
     * Get optional message fields.
     */
    public List<FieldInfo> getOptionalMessageFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isMessage())
                .toList();
    }

    /**
     * Get repeated fields (lists).
     */
    public List<FieldInfo> getRepeatedFields() {
        return fields.stream()
                .filter(FieldInfo::isRepeated)
                .toList();
    }

    /**
     * Find field by name.
     */
    public Optional<FieldInfo> findField(String name) {
        return fields.stream()
                .filter(f -> f.getProtoName().equals(name) || f.getJavaName().equals(name))
                .findFirst();
    }

    /**
     * Find field by number.
     */
    public Optional<FieldInfo> findFieldByNumber(int number) {
        return fields.stream()
                .filter(f -> f.getNumber() == number)
                .findFirst();
    }

    /**
     * Check if this message has nested types.
     */
    public boolean hasNestedTypes() {
        return !nestedMessages.isEmpty() || !nestedEnums.isEmpty();
    }

    /**
     * Get all nested messages recursively.
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
     */
    public String getInterfaceName() {
        return name;
    }

    /**
     * Generate abstract class name (e.g., "Order" -> "AbstractOrder").
     */
    public String getAbstractClassName() {
        return "Abstract" + name;
    }

    /**
     * Generate version-specific class name (e.g., "Order", "v1" -> "OrderV1").
     */
    public String getVersionClassName(String version) {
        return name + version.toUpperCase();
    }

    // Getters
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getPackageName() { return packageName; }
    public String getSourceFileName() { return sourceFileName; }
    public List<FieldInfo> getFields() { return Collections.unmodifiableList(fields); }
    public List<MessageInfo> getNestedMessages() { return Collections.unmodifiableList(nestedMessages); }
    public List<EnumInfo> getNestedEnums() { return Collections.unmodifiableList(nestedEnums); }
    public List<OneofInfo> getOneofGroups() { return Collections.unmodifiableList(oneofGroups); }
    public boolean isMapEntry() { return isMapEntry; }

    /**
     * Check if this message has any oneof groups.
     */
    public boolean hasOneofGroups() {
        return !oneofGroups.isEmpty();
    }

    /**
     * Find oneof group by name.
     */
    public Optional<OneofInfo> findOneofByName(String name) {
        return oneofGroups.stream()
                .filter(o -> o.getProtoName().equals(name) || o.getJavaName().equals(name))
                .findFirst();
    }

    /**
     * Find oneof group containing a specific field.
     */
    public Optional<OneofInfo> findOneofForField(FieldInfo field) {
        return oneofGroups.stream()
                .filter(o -> o.containsField(field.getNumber()))
                .findFirst();
    }

    /**
     * Get fields that are part of a specific oneof group.
     */
    public List<FieldInfo> getFieldsInOneof(OneofInfo oneof) {
        return fields.stream()
                .filter(f -> oneof.containsField(f.getNumber()))
                .toList();
    }

    /**
     * Get fields that are NOT part of any oneof group.
     */
    public List<FieldInfo> getNonOneofFields() {
        return fields.stream()
                .filter(f -> !f.isInOneof())
                .toList();
    }

    /**
     * Get the outer class name derived from the source file name.
     * E.g., "common.proto" -> "Common", "user_request.proto" -> "UserRequest"
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
