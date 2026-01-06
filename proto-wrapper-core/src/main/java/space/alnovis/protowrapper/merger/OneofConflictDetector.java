package space.alnovis.protowrapper.merger;

import space.alnovis.protowrapper.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects conflicts in oneof groups across multiple protocol versions.
 *
 * <p>This class analyzes oneof definitions from multiple versions and identifies
 * various types of conflicts that can affect code generation and runtime behavior.</p>
 */
public class OneofConflictDetector {

    /**
     * Detects all conflicts for a merged oneof.
     *
     * @param oneofBuilder the oneof builder with version data
     * @param messageName  the name of the containing message
     * @param allVersions  all versions being merged
     * @return list of detected conflicts
     */
    public List<OneofConflictInfo> detectConflicts(
            MergedOneof.Builder oneofBuilder,
            String messageName,
            Set<String> allVersions) {

        List<OneofConflictInfo> conflicts = new ArrayList<>();
        Map<String, OneofInfo> versionOneofs = oneofBuilder.getVersionOneofs();
        String oneofName = versionOneofs.values().iterator().next().getProtoName();

        // 1. Check for partial existence
        conflicts.addAll(detectPartialExistence(oneofName, messageName, versionOneofs, allVersions));

        // 2. Check for field set differences
        conflicts.addAll(detectFieldSetDifferences(oneofName, messageName, versionOneofs));

        // 3. Check for field type conflicts
        conflicts.addAll(detectFieldTypeConflicts(oneofName, messageName, oneofBuilder.getFields()));

        // 4. Check for field removal
        conflicts.addAll(detectFieldRemoval(oneofName, messageName, versionOneofs));

        return conflicts;
    }

    /**
     * Detects if oneof exists only in some versions.
     *
     * @param oneofName the oneof name
     * @param messageName the message name
     * @param versionOneofs map of version to oneof info
     * @param allVersions all versions being merged
     * @return list of detected conflicts
     */
    private List<OneofConflictInfo> detectPartialExistence(
            String oneofName,
            String messageName,
            Map<String, OneofInfo> versionOneofs,
            Set<String> allVersions) {

        Set<String> presentVersions = versionOneofs.keySet();
        Set<String> missingVersions = new LinkedHashSet<>(allVersions);
        missingVersions.removeAll(presentVersions);

        if (!missingVersions.isEmpty()) {
            return List.of(OneofConflictInfo.builder(OneofConflictType.PARTIAL_EXISTENCE)
                    .oneofName(oneofName)
                    .messageName(messageName)
                    .description("Oneof '" + oneofName + "' exists only in versions: " +
                            presentVersions + ", missing in: " + missingVersions)
                    .affectedVersions(missingVersions)
                    .detail("presentVersions", presentVersions)
                    .detail("missingVersions", missingVersions)
                    .build());
        }
        return Collections.emptyList();
    }

    /**
     * Detects different field sets in oneof across versions.
     *
     * @param oneofName the oneof name
     * @param messageName the message name
     * @param versionOneofs map of version to oneof info
     * @return list of detected conflicts
     */
    private List<OneofConflictInfo> detectFieldSetDifferences(
            String oneofName,
            String messageName,
            Map<String, OneofInfo> versionOneofs) {

        if (versionOneofs.size() <= 1) {
            return Collections.emptyList();
        }

        // Collect all field numbers and track which versions have which
        Map<Integer, Set<String>> fieldToVersions = new LinkedHashMap<>();
        Map<Integer, String> fieldNumberToName = new LinkedHashMap<>();

        for (Map.Entry<String, OneofInfo> entry : versionOneofs.entrySet()) {
            String version = entry.getKey();
            OneofInfo oneof = entry.getValue();
            for (int fieldNumber : oneof.getFieldNumbers()) {
                fieldToVersions.computeIfAbsent(fieldNumber, k -> new LinkedHashSet<>()).add(version);
            }
            // Get field names if available
            for (FieldInfo field : oneof.getFields()) {
                fieldNumberToName.put(field.getNumber(), field.getProtoName());
            }
        }

        // Find fields that are not in all versions
        List<OneofConflictInfo> conflicts = new ArrayList<>();
        Set<String> allVersions = versionOneofs.keySet();

        Map<String, Set<Integer>> versionSpecificFields = new LinkedHashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : fieldToVersions.entrySet()) {
            int fieldNumber = entry.getKey();
            Set<String> versionsWithField = entry.getValue();

            if (!versionsWithField.equals(allVersions)) {
                String fieldName = fieldNumberToName.getOrDefault(fieldNumber, "field_" + fieldNumber);
                for (String version : versionsWithField) {
                    versionSpecificFields.computeIfAbsent(version, k -> new LinkedHashSet<>())
                            .add(fieldNumber);
                }
            }
        }

        if (!versionSpecificFields.isEmpty()) {
            StringBuilder desc = new StringBuilder("Different fields in oneof across versions: ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Set<Integer>> entry : versionSpecificFields.entrySet()) {
                String version = entry.getKey();
                Set<Integer> fields = entry.getValue();
                List<String> fieldNames = fields.stream()
                        .map(n -> fieldNumberToName.getOrDefault(n, "field_" + n))
                        .toList();
                parts.add(version + " has " + fieldNames);
            }
            desc.append(String.join("; ", parts));

            conflicts.add(OneofConflictInfo.builder(OneofConflictType.FIELD_SET_DIFFERENCE)
                    .oneofName(oneofName)
                    .messageName(messageName)
                    .description(desc.toString())
                    .affectedVersions(versionSpecificFields.keySet())
                    .detail(OneofConflictInfo.DETAIL_VERSION_SPECIFIC_FIELDS, versionSpecificFields)
                    .build());
        }

        return conflicts;
    }

    /**
     * Detects type conflicts in oneof fields.
     *
     * @param oneofName the oneof name
     * @param messageName the message name
     * @param fields the merged fields in the oneof
     * @return list of detected conflicts
     */
    private List<OneofConflictInfo> detectFieldTypeConflicts(
            String oneofName,
            String messageName,
            List<MergedField> fields) {

        List<OneofConflictInfo> conflicts = new ArrayList<>();

        for (MergedField field : fields) {
            if (field.getConflictType() != null &&
                    field.getConflictType() != MergedField.ConflictType.NONE) {

                conflicts.add(OneofConflictInfo.builder(OneofConflictType.FIELD_TYPE_CONFLICT)
                        .oneofName(oneofName)
                        .messageName(messageName)
                        .description("Field '" + field.getName() + "' has type conflict: " +
                                field.getConflictType())
                        .affectedVersions(field.getPresentInVersions())
                        .detail(OneofConflictInfo.DETAIL_FIELD_NAME, field.getName())
                        .detail(OneofConflictInfo.DETAIL_FIELD_CONFLICT_TYPE, field.getConflictType())
                        .build());
            }
        }

        return conflicts;
    }

    /**
     * Detects fields that were removed from oneof in some versions.
     *
     * @param oneofName the oneof name
     * @param messageName the message name
     * @param versionOneofs map of version to oneof info
     * @return list of detected conflicts
     */
    private List<OneofConflictInfo> detectFieldRemoval(
            String oneofName,
            String messageName,
            Map<String, OneofInfo> versionOneofs) {

        if (versionOneofs.size() <= 1) {
            return Collections.emptyList();
        }

        // Sort versions to compare chronologically
        List<String> sortedVersions = versionOneofs.keySet().stream()
                .sorted(Comparator.comparingInt(this::extractVersionNumber))
                .toList();

        // Build field number to name mapping from available fields
        Map<Integer, String> fieldNumberToName = new LinkedHashMap<>();
        for (OneofInfo oneof : versionOneofs.values()) {
            for (FieldInfo field : oneof.getFields()) {
                fieldNumberToName.put(field.getNumber(), field.getProtoName());
            }
        }

        // Check if any fields disappear in later versions
        Set<Integer> allFieldsSeen = new LinkedHashSet<>();
        Map<Integer, String> firstSeenVersion = new LinkedHashMap<>();
        Map<Integer, String> lastSeenVersion = new LinkedHashMap<>();

        for (String version : sortedVersions) {
            OneofInfo oneof = versionOneofs.get(version);
            for (int fieldNumber : oneof.getFieldNumbers()) {
                if (!allFieldsSeen.contains(fieldNumber)) {
                    firstSeenVersion.put(fieldNumber, version);
                }
                lastSeenVersion.put(fieldNumber, version);
                allFieldsSeen.add(fieldNumber);
            }
        }

        // Find fields that were removed (last seen before latest version)
        String latestVersion = sortedVersions.get(sortedVersions.size() - 1);
        List<String> removedFields = new ArrayList<>();
        Set<String> affectedVersions = new LinkedHashSet<>();

        for (Map.Entry<Integer, String> entry : lastSeenVersion.entrySet()) {
            int fieldNumber = entry.getKey();
            String lastVersion = entry.getValue();
            if (!lastVersion.equals(latestVersion)) {
                String fieldName = fieldNumberToName.get(fieldNumber);
                removedFields.add(fieldName + " (last in " + lastVersion + ")");
                affectedVersions.add(lastVersion);
            }
        }

        if (!removedFields.isEmpty()) {
            return List.of(OneofConflictInfo.builder(OneofConflictType.FIELD_REMOVED)
                    .oneofName(oneofName)
                    .messageName(messageName)
                    .description("Fields removed in newer versions: " + String.join(", ", removedFields))
                    .affectedVersions(affectedVersions)
                    .detail(OneofConflictInfo.DETAIL_REMOVED_FIELDS, removedFields)
                    .build());
        }

        return Collections.emptyList();
    }

    /**
     * Detects if oneofs were renamed between versions (same field numbers, different names).
     *
     * @param oneofsByVersion map of version -> list of oneofs in that version's message
     * @param messageName     the message name
     * @return detected rename conflicts with merged oneof name suggestions
     */
    public List<RenamedOneofGroup> detectRenamedOneofs(
            Map<String, List<OneofInfo>> oneofsByVersion,
            String messageName) {

        List<RenamedOneofGroup> renamedGroups = new ArrayList<>();

        // Build a map of field number sets to oneof names by version
        Map<Set<Integer>, Map<String, String>> fieldSetToVersionNames = new LinkedHashMap<>();

        for (Map.Entry<String, List<OneofInfo>> entry : oneofsByVersion.entrySet()) {
            String version = entry.getKey();
            for (OneofInfo oneof : entry.getValue()) {
                Set<Integer> fieldNumbers = new LinkedHashSet<>(oneof.getFieldNumbers());
                fieldSetToVersionNames
                        .computeIfAbsent(fieldNumbers, k -> new LinkedHashMap<>())
                        .put(version, oneof.getProtoName());
            }
        }

        // Find field sets with multiple different names
        for (Map.Entry<Set<Integer>, Map<String, String>> entry : fieldSetToVersionNames.entrySet()) {
            Set<Integer> fieldNumbers = entry.getKey();
            Map<String, String> versionToName = entry.getValue();

            Set<String> uniqueNames = new LinkedHashSet<>(versionToName.values());
            if (uniqueNames.size() > 1) {
                // This is a rename - same field numbers but different names
                renamedGroups.add(new RenamedOneofGroup(
                        fieldNumbers,
                        versionToName,
                        messageName
                ));
            }
        }

        return renamedGroups;
    }

    /**
     * Detects fields that moved in or out of oneof between versions.
     *
     * @param allMessageFields all fields from all versions of a message
     * @param oneofsByVersion  oneofs by version
     * @param messageName      message name
     * @param allVersions      all versions
     * @return detected conflicts
     */
    public List<OneofConflictInfo> detectFieldMembershipChanges(
            Map<Integer, Map<String, FieldInfo>> allMessageFields,
            Map<String, List<OneofInfo>> oneofsByVersion,
            String messageName,
            Set<String> allVersions) {

        List<OneofConflictInfo> conflicts = new ArrayList<>();

        // Build a map of field number -> version -> is_in_oneof
        Map<Integer, Map<String, Boolean>> fieldOneofMembership = new LinkedHashMap<>();

        for (String version : allVersions) {
            Set<Integer> oneofFieldNumbers = new HashSet<>();
            List<OneofInfo> versionOneofs = oneofsByVersion.getOrDefault(version, Collections.emptyList());
            for (OneofInfo oneof : versionOneofs) {
                oneofFieldNumbers.addAll(oneof.getFieldNumbers());
            }

            Map<String, FieldInfo> versionFields = allMessageFields.getOrDefault(version, Collections.emptyMap());
            for (Map.Entry<Integer, Map<String, FieldInfo>> fieldEntry : allMessageFields.entrySet()) {
                int fieldNumber = fieldEntry.getKey();
                if (fieldEntry.getValue().containsKey(version)) {
                    fieldOneofMembership
                            .computeIfAbsent(fieldNumber, k -> new LinkedHashMap<>())
                            .put(version, oneofFieldNumbers.contains(fieldNumber));
                }
            }
        }

        // Find fields where membership changes
        for (Map.Entry<Integer, Map<String, Boolean>> entry : fieldOneofMembership.entrySet()) {
            int fieldNumber = entry.getKey();
            Map<String, Boolean> versionMembership = entry.getValue();

            Set<String> inOneofVersions = new LinkedHashSet<>();
            Set<String> regularVersions = new LinkedHashSet<>();

            for (Map.Entry<String, Boolean> vm : versionMembership.entrySet()) {
                if (vm.getValue()) {
                    inOneofVersions.add(vm.getKey());
                } else {
                    regularVersions.add(vm.getKey());
                }
            }

            if (!inOneofVersions.isEmpty() && !regularVersions.isEmpty()) {
                // Field is in oneof in some versions, regular in others
                String fieldName = findFieldName(allMessageFields, fieldNumber);

                conflicts.add(OneofConflictInfo.builder(OneofConflictType.FIELD_MEMBERSHIP_CHANGE)
                        .oneofName("<multiple>")
                        .messageName(messageName)
                        .description("Field '" + fieldName + "' is in oneof in versions: " +
                                inOneofVersions + ", but regular field in: " + regularVersions)
                        .affectedVersions(allVersions)
                        .detail(OneofConflictInfo.DETAIL_FIELD_NAME, fieldName)
                        .detail(OneofConflictInfo.DETAIL_IN_ONEOF_VERSIONS, inOneofVersions)
                        .detail(OneofConflictInfo.DETAIL_REGULAR_VERSIONS, regularVersions)
                        .build());
            }
        }

        return conflicts;
    }

    /**
     * Detects field number changes for fields with the same name in oneof.
     *
     * @param oneofBuilder the oneof builder
     * @param messageName  message name
     * @return detected conflicts
     */
    public List<OneofConflictInfo> detectFieldNumberChanges(
            MergedOneof.Builder oneofBuilder,
            String messageName) {

        List<OneofConflictInfo> conflicts = new ArrayList<>();
        Map<String, OneofInfo> versionOneofs = oneofBuilder.getVersionOneofs();
        String oneofName = versionOneofs.values().iterator().next().getProtoName();

        // Group field numbers by field name across versions
        Map<String, Map<String, Integer>> fieldNameToVersionNumbers = new LinkedHashMap<>();

        for (Map.Entry<String, OneofInfo> entry : versionOneofs.entrySet()) {
            String version = entry.getKey();
            for (FieldInfo field : entry.getValue().getFields()) {
                fieldNameToVersionNumbers
                        .computeIfAbsent(field.getProtoName(), k -> new LinkedHashMap<>())
                        .put(version, field.getNumber());
            }
        }

        // Find fields with different numbers across versions
        for (Map.Entry<String, Map<String, Integer>> entry : fieldNameToVersionNumbers.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Integer> versionNumbers = entry.getValue();

            Set<Integer> uniqueNumbers = new LinkedHashSet<>(versionNumbers.values());
            if (uniqueNumbers.size() > 1) {
                conflicts.add(OneofConflictInfo.builder(OneofConflictType.FIELD_NUMBER_CHANGE)
                        .oneofName(oneofName)
                        .messageName(messageName)
                        .description("Field '" + fieldName + "' has different numbers: " + versionNumbers)
                        .affectedVersions(versionNumbers.keySet())
                        .detail(OneofConflictInfo.DETAIL_FIELD_NAME, fieldName)
                        .detail("versionNumbers", versionNumbers)
                        .build());
            }
        }

        return conflicts;
    }

    private String findFieldName(Map<Integer, Map<String, FieldInfo>> allFields, int fieldNumber) {
        Map<String, FieldInfo> versionFields = allFields.get(fieldNumber);
        if (versionFields != null && !versionFields.isEmpty()) {
            return versionFields.values().iterator().next().getProtoName();
        }
        return "field_" + fieldNumber;
    }

    private int extractVersionNumber(String version) {
        String numStr = version.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Represents a group of oneofs that were renamed across versions.
     */
    public record RenamedOneofGroup(
            Set<Integer> fieldNumbers,
            Map<String, String> versionToName,
            String messageName
    ) {
        /**
         * Get all unique names this oneof had across versions.
         *
         * @return set of unique names
         */
        public Set<String> getAllNames() {
            return new LinkedHashSet<>(versionToName.values());
        }

        /**
         * Get the most common name (or first if tied).
         *
         * @return the most common name
         */
        public String getMostCommonName() {
            Map<String, Long> nameCounts = versionToName.values().stream()
                    .collect(Collectors.groupingBy(n -> n, Collectors.counting()));
            return nameCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(versionToName.values().iterator().next());
        }

        /**
         * Create a conflict info for this renamed group.
         *
         * @return the conflict info
         */
        public OneofConflictInfo toConflictInfo() {
            return OneofConflictInfo.builder(OneofConflictType.RENAMED)
                    .oneofName(getMostCommonName())
                    .messageName(messageName)
                    .description("Oneof renamed across versions: " + versionToName)
                    .affectedVersions(versionToName.keySet())
                    .detail("versionToName", versionToName)
                    .detail("allNames", getAllNames())
                    .build();
        }
    }
}
