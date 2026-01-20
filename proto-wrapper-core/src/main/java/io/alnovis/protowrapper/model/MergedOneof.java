package io.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged oneof group from multiple protocol versions.
 *
 * <p>Contains the unified oneof definition with field information
 * that works across all versions where this oneof is present.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * MergedOneof oneof = MergedOneof.builder("payment_method")
 *     .addVersionOneof("v1", oneofInfoV1)
 *     .addVersionOneof("v2", oneofInfoV2)
 *     .addField(creditCardField)
 *     .addField(bankTransferField)
 *     .build();
 * </pre>
 */
public class MergedOneof {

    private final String protoName;
    private final String javaName;
    private final String caseEnumName;
    private final Set<String> presentInVersions;
    private final Map<String, OneofInfo> versionOneofs;
    private final List<MergedField> fields;
    private final Set<Integer> allFieldNumbers;
    private final List<OneofConflictInfo> conflicts;
    private final Set<String> mergedFromNames; // For renamed oneofs

    private MergedOneof(Builder builder) {
        this.protoName = builder.protoName;
        this.javaName = builder.javaName;
        this.caseEnumName = builder.javaName + "Case";
        this.presentInVersions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.versionOneofs.keySet()));
        this.versionOneofs = Collections.unmodifiableMap(new LinkedHashMap<>(builder.versionOneofs));
        this.fields = Collections.unmodifiableList(new ArrayList<>(builder.fields));
        this.conflicts = Collections.unmodifiableList(new ArrayList<>(builder.conflicts));
        this.mergedFromNames = Collections.unmodifiableSet(new LinkedHashSet<>(builder.mergedFromNames));

        // Collect all field numbers from all versions
        Set<Integer> numbers = new LinkedHashSet<>();
        for (OneofInfo oneof : versionOneofs.values()) {
            numbers.addAll(oneof.getFieldNumbers());
        }
        for (MergedField field : fields) {
            numbers.add(field.getNumber());
        }
        this.allFieldNumbers = Collections.unmodifiableSet(numbers);
    }

    /**
     * Create a new builder for MergedOneof.
     *
     * @param protoName the proto name of the oneof (e.g., "payment_method")
     * @return new Builder instance
     */
    public static Builder builder(String protoName) {
        return new Builder(protoName);
    }

    /**
     * Returns the proto name of this oneof (e.g., "payment_method").
     *
     * @return the proto name
     */
    public String getProtoName() {
        return protoName;
    }

    /**
     * Returns the Java-style name (e.g., "PaymentMethod").
     *
     * @return the Java-style name
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * Returns the name of the Case enum (e.g., "PaymentMethodCase").
     *
     * @return the Case enum name
     */
    public String getCaseEnumName() {
        return caseEnumName;
    }

    /**
     * Returns the getter name for the case discriminator (e.g., "getPaymentMethodCase").
     *
     * @return the case getter method name
     */
    public String getCaseGetterName() {
        return "get" + javaName + "Case";
    }

    /**
     * Returns the extract method name for the case discriminator (e.g., "extractPaymentMethodCase").
     *
     * @return the extract case method name
     */
    public String getExtractCaseMethodName() {
        return "extract" + javaName + "Case";
    }

    /**
     * Returns the clear method name for the entire oneof (e.g., "clearPaymentMethod").
     *
     * @return the clear method name
     */
    public String getClearMethodName() {
        return "clear" + javaName;
    }

    /**
     * Returns the abstract clear method name (e.g., "doClearPaymentMethod").
     *
     * @return the abstract clear method name
     */
    public String getDoClearMethodName() {
        return "doClear" + javaName;
    }

    /**
     * Returns the "NOT_SET" constant name for the Case enum (e.g., "PAYMENT_METHOD_NOT_SET").
     *
     * @return the NOT_SET constant name
     */
    public String getNotSetConstantName() {
        return toScreamingSnakeCase(protoName) + "_NOT_SET";
    }

    /**
     * Returns the versions where this oneof is present.
     *
     * @return unmodifiable set of version identifiers
     */
    public Set<String> getPresentInVersions() {
        return presentInVersions;
    }

    /**
     * Returns the oneof info for a specific version.
     *
     * @param version the version identifier
     * @return the OneofInfo for the version, or null if not present
     */
    public OneofInfo getOneofForVersion(String version) {
        return versionOneofs.get(version);
    }

    /**
     * Returns the merged fields in this oneof.
     *
     * @return unmodifiable list of merged fields
     */
    public List<MergedField> getFields() {
        return fields;
    }

    /**
     * Returns all field numbers across all versions.
     *
     * @return unmodifiable set of field numbers
     */
    public Set<Integer> getAllFieldNumbers() {
        return allFieldNumbers;
    }

    /**
     * Returns all detected conflicts for this oneof.
     *
     * @return unmodifiable list of conflicts
     */
    public List<OneofConflictInfo> getConflicts() {
        return conflicts;
    }

    /**
     * Checks if this oneof has any conflicts.
     *
     * @return true if there are conflicts
     */
    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    /**
     * Returns conflicts of a specific type.
     *
     * @param type the conflict type to filter by
     * @return list of conflicts of the specified type
     */
    public List<OneofConflictInfo> getConflictsOfType(OneofConflictType type) {
        return conflicts.stream()
                .filter(c -> c.getType() == type)
                .toList();
    }

    /**
     * Checks if this oneof has a specific type of conflict.
     *
     * @param type the conflict type to check for
     * @return true if there is a conflict of the specified type
     */
    public boolean hasConflictOfType(OneofConflictType type) {
        return conflicts.stream().anyMatch(c -> c.getType() == type);
    }

    /**
     * Returns names of oneofs that were merged into this one (for renamed oneofs).
     *
     * @return unmodifiable set of original oneof names
     */
    public Set<String> getMergedFromNames() {
        return mergedFromNames;
    }

    /**
     * Checks if this oneof was merged from multiple differently-named oneofs.
     *
     * @return true if merged from multiple names
     */
    public boolean wasMergedFromMultipleNames() {
        return mergedFromNames.size() > 1;
    }

    /**
     * Checks if a field number belongs to this oneof in any version.
     *
     * @param fieldNumber the field number to check
     * @return true if the field number belongs to this oneof
     */
    public boolean containsField(int fieldNumber) {
        return allFieldNumbers.contains(fieldNumber);
    }

    /**
     * Checks if this oneof is present in all given versions.
     *
     * @param allVersions the set of all versions to check against
     * @return true if present in all versions
     */
    public boolean isUniversal(Set<String> allVersions) {
        return presentInVersions.containsAll(allVersions);
    }

    /**
     * Checks if this oneof has different fields in different versions.
     *
     * @return true if field composition differs between versions
     */
    public boolean hasFieldDifferences() {
        if (versionOneofs.size() <= 1) {
            return false;
        }
        Set<Integer> referenceNumbers = null;
        for (OneofInfo oneof : versionOneofs.values()) {
            Set<Integer> currentNumbers = new HashSet<>(oneof.getFieldNumbers());
            if (referenceNumbers == null) {
                referenceNumbers = currentNumbers;
            } else if (!referenceNumbers.equals(currentNumbers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns versions where this oneof is NOT present.
     *
     * @param allVersions the set of all versions to check against
     * @return set of versions where this oneof is missing
     */
    public Set<String> getMissingVersions(Set<String> allVersions) {
        Set<String> missing = new LinkedHashSet<>(allVersions);
        missing.removeAll(presentInVersions);
        return missing;
    }

    /**
     * Returns a list of case enum constants for this oneof.
     * Each constant corresponds to a field in the oneof, plus NOT_SET.
     *
     * @return list of case constants
     */
    public List<CaseConstant> getCaseConstants() {
        List<CaseConstant> constants = new ArrayList<>();

        // Add a constant for each field
        for (MergedField field : fields) {
            String constantName = toScreamingSnakeCase(field.getName());
            constants.add(new CaseConstant(constantName, field.getNumber(), field));
        }

        // Add NOT_SET constant
        constants.add(new CaseConstant(getNotSetConstantName(), 0, null));

        return constants;
    }

    private static String toScreamingSnakeCase(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }
        return result.toString();
    }

    /**
     * Converts a proto name (snake_case) to PascalCase.
     */
    private static String toPascalCase(String protoName) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : protoName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return String.format("MergedOneof[%s, fields=%d, versions=%s]",
                protoName, fields.size(), presentInVersions);
    }

    /**
     * Represents a case constant in the generated Case enum.
     *
     * @param name the constant name
     * @param fieldNumber the field number (0 for NOT_SET)
     * @param field the associated field, or null for NOT_SET
     */
    public record CaseConstant(
            String name,
            int fieldNumber,
            MergedField field
    ) {
        /**
         * Checks if this is the NOT_SET constant.
         *
         * @return true if this represents NOT_SET
         */
        public boolean isNotSet() {
            return field == null;
        }
    }

    /**
     * Builder for creating immutable MergedOneof instances.
     */
    public static class Builder {
        private final String protoName;
        private final String javaName;
        private final Map<String, OneofInfo> versionOneofs = new LinkedHashMap<>();
        private final List<MergedField> fields = new ArrayList<>();
        private final List<OneofConflictInfo> conflicts = new ArrayList<>();
        private final Set<String> mergedFromNames = new LinkedHashSet<>();

        /**
         * Create a new Builder.
         *
         * @param protoName the proto name of the oneof
         */
        private Builder(String protoName) {
            this.protoName = protoName;
            this.javaName = toPascalCase(protoName);
            this.mergedFromNames.add(protoName);
        }

        /**
         * Add a version's oneof info.
         *
         * @param version the version identifier
         * @param oneof the oneof info for this version
         * @return this builder for chaining
         */
        public Builder addVersionOneof(String version, OneofInfo oneof) {
            versionOneofs.put(version, oneof);
            return this;
        }

        /**
         * Add a merged field to this oneof.
         *
         * @param field the field to add
         * @return this builder for chaining
         */
        public Builder addField(MergedField field) {
            fields.add(field);
            return this;
        }

        /**
         * Add multiple merged fields to this oneof.
         *
         * @param fieldsToAdd the fields to add
         * @return this builder for chaining
         */
        public Builder addFields(Collection<MergedField> fieldsToAdd) {
            fields.addAll(fieldsToAdd);
            return this;
        }

        /**
         * Add a detected conflict.
         *
         * @param conflict the conflict info to add
         * @return this builder for chaining
         */
        public Builder addConflict(OneofConflictInfo conflict) {
            conflicts.add(conflict);
            return this;
        }

        /**
         * Add multiple detected conflicts.
         *
         * @param conflictsToAdd the conflicts to add
         * @return this builder for chaining
         */
        public Builder addConflicts(Collection<OneofConflictInfo> conflictsToAdd) {
            conflicts.addAll(conflictsToAdd);
            return this;
        }

        /**
         * Add a name that this oneof was merged from (for renamed oneofs).
         *
         * @param name the original oneof name
         * @return this builder for chaining
         */
        public Builder addMergedFromName(String name) {
            mergedFromNames.add(name);
            return this;
        }

        /**
         * Get current fields (for conflict detection during build).
         *
         * @return unmodifiable list of fields
         */
        public List<MergedField> getFields() {
            return Collections.unmodifiableList(fields);
        }

        /**
         * Get current version oneofs (for conflict detection during build).
         *
         * @return unmodifiable map of version to OneofInfo
         */
        public Map<String, OneofInfo> getVersionOneofs() {
            return Collections.unmodifiableMap(versionOneofs);
        }

        /**
         * Build the immutable MergedOneof.
         *
         * @return the built MergedOneof instance
         * @throws IllegalStateException if no version oneofs have been added
         */
        public MergedOneof build() {
            if (versionOneofs.isEmpty()) {
                throw new IllegalStateException("At least one version oneof must be added");
            }
            return new MergedOneof(this);
        }
    }
}
