package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents the merged schema from multiple protobuf versions.
 * Contains unified message definitions that work across all versions.
 */
public class MergedSchema {

    private final List<String> versions;
    private final Map<String, MergedMessage> messages;
    private final Map<String, MergedEnum> enums;
    // Maps nested enum path to equivalent top-level enum name
    // E.g., "Product.CategoryEnum" -> "CategoryEnum"
    private final Map<String, String> equivalentEnumMappings;
    // Conflict enums for INT_ENUM type conflicts
    // Maps "MessageName.fieldName" -> ConflictEnumInfo
    private final Map<String, ConflictEnumInfo> conflictEnums;

    public MergedSchema(List<String> versions) {
        this.versions = new ArrayList<>(versions);
        this.messages = new LinkedHashMap<>();
        this.enums = new LinkedHashMap<>();
        this.equivalentEnumMappings = new LinkedHashMap<>();
        this.conflictEnums = new LinkedHashMap<>();
    }

    /**
     * Register an equivalent enum mapping (nested -> top-level).
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @param topLevelName Simple name like "CategoryEnum"
     */
    public void addEquivalentEnumMapping(String nestedPath, String topLevelName) {
        equivalentEnumMappings.put(nestedPath, topLevelName);
    }

    /**
     * Check if a nested enum has an equivalent top-level enum.
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @return true if there's an equivalent top-level enum
     */
    public boolean hasEquivalentTopLevelEnum(String nestedPath) {
        return equivalentEnumMappings.containsKey(nestedPath);
    }

    /**
     * Get the top-level enum name for a nested enum path.
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @return Top-level enum name or null if no mapping exists
     */
    public String getEquivalentTopLevelEnum(String nestedPath) {
        return equivalentEnumMappings.get(nestedPath);
    }

    /**
     * Get all equivalent enum mappings.
     */
    public Map<String, String> getEquivalentEnumMappings() {
        return Collections.unmodifiableMap(equivalentEnumMappings);
    }

    public void addMessage(MergedMessage message) {
        messages.put(message.getName(), message);
    }

    public void addEnum(MergedEnum enumInfo) {
        enums.put(enumInfo.getName(), enumInfo);
    }

    public Optional<MergedMessage> getMessage(String name) {
        return Optional.ofNullable(messages.get(name));
    }

    public Optional<MergedEnum> getEnum(String name) {
        return Optional.ofNullable(enums.get(name));
    }

    public List<String> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public Collection<MergedMessage> getMessages() {
        return Collections.unmodifiableCollection(messages.values());
    }

    public Collection<MergedEnum> getEnums() {
        return Collections.unmodifiableCollection(enums.values());
    }

    /**
     * Add a conflict enum for an INT_ENUM type conflict field.
     * @param info Conflict enum information
     */
    public void addConflictEnum(ConflictEnumInfo info) {
        conflictEnums.put(info.getFullPath(), info);
    }

    /**
     * Get conflict enum info for a field.
     * @param messageName Message name
     * @param fieldName Field name
     * @return ConflictEnumInfo if exists
     */
    public Optional<ConflictEnumInfo> getConflictEnum(String messageName, String fieldName) {
        return Optional.ofNullable(conflictEnums.get(messageName + "." + fieldName));
    }

    /**
     * Get all conflict enums.
     * @return Collection of all ConflictEnumInfo
     */
    public Collection<ConflictEnumInfo> getConflictEnums() {
        return Collections.unmodifiableCollection(conflictEnums.values());
    }

    /**
     * Check if a field has a conflict enum.
     * @param messageName Message name
     * @param fieldName Field name
     * @return true if conflict enum exists
     */
    public boolean hasConflictEnum(String messageName, String fieldName) {
        return conflictEnums.containsKey(messageName + "." + fieldName);
    }

    /**
     * Find a nested message by path like "Order.ShippingInfo".
     * @param nestedPath Path like "ParentMessage.NestedMessage"
     * @return The nested message if found
     */
    public Optional<MergedMessage> findMessageByPath(String nestedPath) {
        if (nestedPath == null || nestedPath.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = nestedPath.split("\\.");
        if (parts.length == 0) {
            return Optional.empty();
        }

        // Find the top-level message
        MergedMessage current = messages.get(parts[0]);
        if (current == null) {
            return Optional.empty();
        }

        // Navigate through nested messages
        for (int i = 1; i < parts.length; i++) {
            Optional<MergedMessage> nested = current.findNestedMessage(parts[i]);
            if (!nested.isPresent()) {
                return Optional.empty();
            }
            current = nested.get();
        }

        return Optional.of(current);
    }

    /**
     * Find a nested enum by path like "Order.Item.ItemTypeEnum".
     * @param nestedPath Path like "ParentMessage.NestedEnum"
     * @return The nested enum if found
     */
    public Optional<MergedEnum> findEnumByPath(String nestedPath) {
        if (nestedPath == null || nestedPath.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = nestedPath.split("\\.");
        if (parts.length < 2) {
            // No path, check top-level enums
            return getEnum(nestedPath);
        }

        // Navigate to parent message
        MergedMessage parent = messages.get(parts[0]);
        if (parent == null) {
            return Optional.empty();
        }

        // Navigate through nested messages until we reach the enum's parent
        for (int i = 1; i < parts.length - 1; i++) {
            Optional<MergedMessage> nested = parent.findNestedMessage(parts[i]);
            if (!nested.isPresent()) {
                return Optional.empty();
            }
            parent = nested.get();
        }

        // Find the enum in the last parent
        return parent.findNestedEnum(parts[parts.length - 1]);
    }
}
