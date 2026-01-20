package io.alnovis.protowrapper.contract;

import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.util.*;

/**
 * Contract for a field merged from multiple protocol versions.
 *
 * <p>This record combines individual {@link FieldContract}s from each version
 * into a unified contract that governs code generation for the wrapper API.</p>
 *
 * <h2>Merge Rules</h2>
 * <ul>
 *   <li><b>hasMethodExists:</b> true only if ALL versions support has*() method</li>
 *   <li><b>getterUsesHasCheck:</b> true only if hasMethodExists AND any version is nullable</li>
 *   <li><b>nullable:</b> true if ANY version is nullable (union semantics)</li>
 *   <li><b>cardinality:</b> REPEATED wins over SINGULAR (for REPEATED_SINGLE conflicts)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MergedFieldContract contract = MergedFieldContract.from(mergedField, syntaxPerVersion);
 *
 * if (contract.unified().hasMethodExists()) {
 *     // Generate has*() method
 * }
 *
 * // Check presence in specific version
 * if (contract.isPresentIn("v2")) {
 *     FieldContract v2Contract = contract.contractForVersion("v2").get();
 * }
 * }</pre>
 *
 * @param versionContracts individual contracts per version (version -> contract)
 * @param unified the merged/unified contract for wrapper generation
 * @param presentInVersions set of versions where this field is present
 * @param conflictType type of conflict between versions (from MergedField)
 *
 * @see FieldContract
 * @see MergedField
 */
public record MergedFieldContract(
        Map<String, FieldContract> versionContracts,
        FieldContract unified,
        Set<String> presentInVersions,
        MergedField.ConflictType conflictType
) {

    /**
     * Compact constructor with validation.
     */
    public MergedFieldContract {
        Objects.requireNonNull(versionContracts, "versionContracts must not be null");
        Objects.requireNonNull(unified, "unified must not be null");
        Objects.requireNonNull(presentInVersions, "presentInVersions must not be null");
        Objects.requireNonNull(conflictType, "conflictType must not be null");

        if (versionContracts.isEmpty()) {
            throw new IllegalArgumentException("versionContracts must not be empty");
        }
        if (presentInVersions.isEmpty()) {
            throw new IllegalArgumentException("presentInVersions must not be empty");
        }

        // Defensive copies
        versionContracts = Collections.unmodifiableMap(new LinkedHashMap<>(versionContracts));
        presentInVersions = Collections.unmodifiableSet(new LinkedHashSet<>(presentInVersions));
    }

    /**
     * Creates a MergedFieldContract from a MergedField.
     *
     * <p>This factory method:</p>
     * <ol>
     *   <li>Creates FieldContract for each version's FieldInfo</li>
     *   <li>Merges contracts according to the merge rules</li>
     *   <li>Handles conflict types appropriately</li>
     * </ol>
     *
     * @param mergedField the merged field definition
     * @param syntaxPerVersion map of version to proto syntax
     * @return the merged field contract
     */
    public static MergedFieldContract from(MergedField mergedField, Map<String, ProtoSyntax> syntaxPerVersion) {
        Objects.requireNonNull(mergedField, "mergedField must not be null");
        Objects.requireNonNull(syntaxPerVersion, "syntaxPerVersion must not be null");

        Map<String, FieldContract> contracts = new LinkedHashMap<>();

        // Create contract for each version
        for (Map.Entry<String, FieldInfo> entry : mergedField.getVersionFields().entrySet()) {
            String version = entry.getKey();
            FieldInfo fieldInfo = entry.getValue();
            ProtoSyntax syntax = syntaxPerVersion.getOrDefault(version, ProtoSyntax.PROTO3);

            FieldContract contract = FieldContract.from(fieldInfo, syntax);
            contracts.put(version, contract);
        }

        // Merge contracts
        FieldContract unified = mergeContracts(contracts.values(), mergedField);

        return new MergedFieldContract(
                contracts,
                unified,
                mergedField.getPresentInVersions(),
                mergedField.getConflictType()
        );
    }

    /**
     * Creates a MergedFieldContract from a MergedField using default syntax (proto3).
     *
     * @param mergedField the merged field definition
     * @return the merged field contract
     */
    public static MergedFieldContract from(MergedField mergedField) {
        Map<String, ProtoSyntax> defaultSyntax = new HashMap<>();
        for (String version : mergedField.getPresentInVersions()) {
            defaultSyntax.put(version, ProtoSyntax.PROTO3);
        }
        return from(mergedField, defaultSyntax);
    }

    /**
     * Merges multiple contracts into a unified contract.
     *
     * @param contracts the contracts to merge
     * @param mergedField the merged field (for additional context)
     * @return the unified contract
     */
    private static FieldContract mergeContracts(Collection<FieldContract> contracts, MergedField mergedField) {
        if (contracts.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge empty contract collection");
        }

        if (contracts.size() == 1) {
            return contracts.iterator().next();
        }

        // Determine cardinality: REPEATED/MAP wins over SINGULAR
        FieldCardinality cardinality = mergeCardinality(contracts, mergedField);

        // Determine type category: use first (they should match for non-conflicting fields)
        FieldTypeCategory typeCategory = contracts.iterator().next().typeCategory();

        // For PRIMITIVE_MESSAGE conflicts, prefer MESSAGE
        if (mergedField.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
            typeCategory = FieldTypeCategory.MESSAGE;
        }

        // Determine presence: most restrictive wins for hasMethodExists
        FieldPresence presence = mergePresence(contracts);

        // inOneof: true if ANY version has it in oneof
        boolean inOneof = contracts.stream().anyMatch(FieldContract::inOneof);

        // hasMethodExists: true only if ALL versions support has*()
        // This is critical for generating correct code
        boolean hasMethodExists = contracts.stream().allMatch(FieldContract::hasMethodExists);

        // For REPEATED/MAP, never has method
        if (cardinality != FieldCardinality.SINGULAR) {
            hasMethodExists = false;
        }

        // getterUsesHasCheck: true if hasMethodExists AND any version is nullable
        boolean anyNullable = contracts.stream().anyMatch(FieldContract::nullable);
        boolean getterUsesHasCheck = hasMethodExists && anyNullable;

        // nullable: true if ANY version is nullable (union semantics)
        boolean nullable = anyNullable;

        // For REPEATED/MAP, never nullable (empty collection instead)
        if (cardinality != FieldCardinality.SINGULAR) {
            nullable = false;
        }

        // Default value: based on final unified state
        FieldContract.DefaultValue defaultValue = computeUnifiedDefaultValue(
                cardinality, typeCategory, nullable, contracts);

        return new FieldContract(
                cardinality,
                typeCategory,
                presence,
                inOneof,
                hasMethodExists,
                getterUsesHasCheck,
                nullable,
                defaultValue
        );
    }

    /**
     * Merges cardinality: REPEATED/MAP wins over SINGULAR.
     */
    private static FieldCardinality mergeCardinality(Collection<FieldContract> contracts, MergedField mergedField) {
        // Check for REPEATED_SINGLE conflict
        if (mergedField.getConflictType() == MergedField.ConflictType.REPEATED_SINGLE) {
            return FieldCardinality.REPEATED;
        }

        // Check if any is repeated or map
        for (FieldContract contract : contracts) {
            if (contract.cardinality() == FieldCardinality.MAP) {
                return FieldCardinality.MAP;
            }
            if (contract.cardinality() == FieldCardinality.REPEATED) {
                return FieldCardinality.REPEATED;
            }
        }

        return FieldCardinality.SINGULAR;
    }

    /**
     * Merges presence: determines the "most restrictive" presence for unified behavior.
     */
    private static FieldPresence mergePresence(Collection<FieldContract> contracts) {
        // If any is PROTO2_REQUIRED, treat as PROTO2_OPTIONAL for unified (since other versions may be optional)
        // Otherwise use the first presence

        boolean hasProto2 = contracts.stream()
                .anyMatch(c -> c.presence().isProto2());
        boolean hasProto3Explicit = contracts.stream()
                .anyMatch(c -> c.presence() == FieldPresence.PROTO3_EXPLICIT_OPTIONAL);

        if (hasProto2) {
            return FieldPresence.PROTO2_OPTIONAL;
        } else if (hasProto3Explicit) {
            return FieldPresence.PROTO3_EXPLICIT_OPTIONAL;
        } else {
            return FieldPresence.PROTO3_IMPLICIT;
        }
    }

    /**
     * Computes unified default value based on merged state.
     */
    private static FieldContract.DefaultValue computeUnifiedDefaultValue(
            FieldCardinality cardinality,
            FieldTypeCategory typeCategory,
            boolean nullable,
            Collection<FieldContract> contracts) {

        if (cardinality == FieldCardinality.REPEATED) {
            return FieldContract.DefaultValue.EMPTY_LIST;
        }
        if (cardinality == FieldCardinality.MAP) {
            return FieldContract.DefaultValue.EMPTY_MAP;
        }
        if (nullable) {
            return FieldContract.DefaultValue.NULL;
        }

        // Use first contract's default as fallback
        return contracts.iterator().next().defaultValueWhenUnset();
    }

    // ==================== Convenience Methods ====================

    /**
     * Check if field is present in a specific version.
     *
     * @param version the version to check
     * @return true if field is present in that version
     */
    public boolean isPresentIn(String version) {
        return presentInVersions.contains(version);
    }

    /**
     * Get contract for a specific version.
     *
     * @param version the version
     * @return Optional containing the contract, or empty if not present
     */
    public Optional<FieldContract> contractForVersion(String version) {
        return Optional.ofNullable(versionContracts.get(version));
    }

    /**
     * Check if this field exists in all given versions.
     *
     * @param allVersions all available versions
     * @return true if field is present in all versions
     */
    public boolean isUniversal(Set<String> allVersions) {
        return presentInVersions.containsAll(allVersions);
    }

    /**
     * Check if has*() method is available in a specific version.
     *
     * @param version the version to check
     * @return true if has*() is available in that version
     */
    public boolean hasMethodAvailableIn(String version) {
        FieldContract contract = versionContracts.get(version);
        return contract != null && contract.hasMethodExists();
    }

    /**
     * Check if there's any conflict between versions.
     *
     * @return true if there's a type conflict
     */
    public boolean hasConflict() {
        return conflictType != MergedField.ConflictType.NONE;
    }

    /**
     * Check if builder setters should be skipped for this field.
     *
     * @return true if setters should not be generated due to conflict type
     */
    public boolean shouldSkipBuilderSetter() {
        return conflictType.shouldSkipBuilderSetter();
    }

    /**
     * Get the number of versions where this field is present.
     *
     * @return count of versions
     */
    public int versionCount() {
        return presentInVersions.size();
    }

    /**
     * Returns a human-readable description of this merged contract.
     *
     * @return formatted string describing the contract
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("MergedFieldContract[\n");
        sb.append("  unified: ").append(unified.describe()).append("\n");
        sb.append("  versions: ").append(presentInVersions).append("\n");
        if (conflictType != MergedField.ConflictType.NONE) {
            sb.append("  conflict: ").append(conflictType).append("\n");
        }
        for (Map.Entry<String, FieldContract> entry : versionContracts.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().describe()).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
