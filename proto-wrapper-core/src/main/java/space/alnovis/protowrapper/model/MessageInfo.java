package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;

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

        this.fields = proto.getFieldList().stream()
                .map(FieldInfo::new)
                .collect(Collectors.toList());

        this.nestedMessages = proto.getNestedTypeList().stream()
                .filter(nested -> !nested.getOptions().getMapEntry()) // Skip map entries
                .map(nested -> new MessageInfo(nested, packageName, fullName + "." + nested.getName(), sourceFileName))
                .collect(Collectors.toList());

        this.nestedEnums = proto.getEnumTypeList().stream()
                .map(EnumInfo::new)
                .collect(Collectors.toList());
    }

    // Constructor for merged messages
    public MessageInfo(String name, String fullName, String packageName,
                       List<FieldInfo> fields, List<MessageInfo> nestedMessages,
                       List<EnumInfo> nestedEnums) {
        this.name = name;
        this.fullName = fullName;
        this.packageName = packageName;
        this.sourceFileName = null;
        this.fields = new ArrayList<>(fields);
        this.nestedMessages = new ArrayList<>(nestedMessages);
        this.nestedEnums = new ArrayList<>(nestedEnums);
        this.isMapEntry = false;
    }

    /**
     * Get fields sorted by field number.
     */
    public List<FieldInfo> getFieldsSorted() {
        return fields.stream()
                .sorted(Comparator.comparingInt(FieldInfo::getNumber))
                .collect(Collectors.toList());
    }

    /**
     * Get required fields (non-optional, non-repeated).
     */
    public List<FieldInfo> getRequiredFields() {
        return fields.stream()
                .filter(f -> !f.isOptional() && !f.isRepeated())
                .collect(Collectors.toList());
    }

    /**
     * Get optional primitive fields (need has-check pattern).
     */
    public List<FieldInfo> getOptionalPrimitiveFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isPrimitive())
                .collect(Collectors.toList());
    }

    /**
     * Get optional message fields.
     */
    public List<FieldInfo> getOptionalMessageFields() {
        return fields.stream()
                .filter(f -> f.isOptional() && f.isMessage())
                .collect(Collectors.toList());
    }

    /**
     * Get repeated fields (lists).
     */
    public List<FieldInfo> getRepeatedFields() {
        return fields.stream()
                .filter(FieldInfo::isRepeated)
                .collect(Collectors.toList());
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
    public boolean isMapEntry() { return isMapEntry; }

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
