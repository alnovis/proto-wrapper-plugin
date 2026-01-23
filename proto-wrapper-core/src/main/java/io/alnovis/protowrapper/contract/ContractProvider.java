package io.alnovis.protowrapper.contract;

import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * Provides contracts for fields in the wrapper generation system.
 *
 * <p>This class serves as a bridge between the existing code generation system
 * and the contract-based approach. It creates contracts from {@link MergedField}
 * objects, deriving the necessary syntax information from field properties.</p>
 *
 * <h2>Syntax Derivation</h2>
 * <p>Since the merged schema may not directly store {@link ProtoSyntax}, this provider
 * infers the effective syntax from observable properties:</p>
 * <ul>
 *   <li>For scalar (non-message, non-oneof) fields:
 *     <ul>
 *       <li>{@code supportsHasMethod = true} implies explicit presence (Proto2 or Proto3 optional)</li>
 *       <li>{@code supportsHasMethod = false} implies Proto3 implicit presence</li>
 *     </ul>
 *   </li>
 *   <li>For message fields: always have explicit presence</li>
 *   <li>For oneof fields: always have explicit presence</li>
 * </ul>
 *
 * <h2>Caching</h2>
 * <p>Contracts are cached per message to avoid repeated construction. The cache
 * is thread-safe using {@link ConcurrentHashMap}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ContractProvider provider = ContractProvider.getInstance();
 *
 * // Get contract for a specific field
 * MergedFieldContract contract = provider.getContract(field);
 *
 * // Get all contracts for a message
 * Map<MergedField, MergedFieldContract> contracts = provider.getContracts(message);
 * }</pre>
 *
 * @see MergedFieldContract
 * @see FieldContract
 */
public final class ContractProvider {

    private static final ContractProvider INSTANCE = new ContractProvider();

    /**
     * Cache of contracts per MergedMessage.
     * Key is System.identityHashCode to handle messages with same name.
     */
    private final Map<Integer, Map<MergedField, MergedFieldContract>> contractCache;

    private ContractProvider() {
        this.contractCache = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance.
     *
     * @return the contract provider instance
     */
    public static ContractProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Get the contract for a merged field.
     *
     * <p>This method derives syntax information from the field's properties
     * and creates an appropriate contract.</p>
     *
     * @param field the merged field
     * @return the contract for the field
     */
    public MergedFieldContract getContract(MergedField field) {
        Map<String, ProtoSyntax> syntaxPerVersion = deriveSyntaxPerVersion(field);
        return MergedFieldContract.from(field, syntaxPerVersion);
    }

    /**
     * Get contracts for all fields in a message.
     *
     * <p>Results are cached per message identity for performance.</p>
     *
     * @param message the merged message
     * @return map of field to contract
     */
    public Map<MergedField, MergedFieldContract> getContracts(MergedMessage message) {
        int messageId = System.identityHashCode(message);
        return contractCache.computeIfAbsent(messageId, id -> buildContractsForMessage(message));
    }

    /**
     * Get contract for a field within a message context.
     *
     * <p>Uses cached contracts if available.</p>
     *
     * @param message the parent message
     * @param field the field within the message
     * @return the contract for the field
     */
    public MergedFieldContract getContract(MergedMessage message, MergedField field) {
        Map<MergedField, MergedFieldContract> contracts = getContracts(message);
        return contracts.computeIfAbsent(field, this::getContract);
    }

    /**
     * Clear the contract cache.
     *
     * <p>Useful when the schema changes or for memory management.</p>
     */
    public void clearCache() {
        contractCache.clear();
    }

    /**
     * Clear cache for a specific message.
     *
     * @param message the message to clear cache for
     */
    public void clearCache(MergedMessage message) {
        contractCache.remove(System.identityHashCode(message));
    }

    // ==================== Internal Methods ====================

    private Map<MergedField, MergedFieldContract> buildContractsForMessage(MergedMessage message) {
        Map<MergedField, MergedFieldContract> contracts = new LinkedHashMap<>();
        for (MergedField field : message.getFields()) {
            contracts.put(field, getContract(field));
        }
        return contracts;
    }

    /**
     * Derive ProtoSyntax per version from field properties.
     *
     * <p>This is the key method that infers syntax from observable behavior.</p>
     *
     * @param field the merged field
     * @return map of version to inferred syntax
     */
    private Map<String, ProtoSyntax> deriveSyntaxPerVersion(MergedField field) {
        Map<String, ProtoSyntax> result = new LinkedHashMap<>();

        for (String version : field.getPresentInVersions()) {
            Optional<FieldInfo> fieldInfoOpt = field.getFieldForVersion(version);
            if (fieldInfoOpt.isPresent()) {
                ProtoSyntax syntax = deriveSyntax(fieldInfoOpt.get());
                result.put(version, syntax);
            } else {
                // Fallback to PROTO3 if field info not available
                result.put(version, ProtoSyntax.PROTO3);
            }
        }

        return result;
    }

    /**
     * Derive ProtoSyntax from a single FieldInfo.
     *
     * <p>This method uses the detected syntax from the FieldInfo if available,
     * otherwise falls back to inference from observable properties.</p>
     *
     * @param fieldInfo the field info
     * @return the syntax for this field
     * @since 2.2.0 Now uses FieldInfo.getDetectedSyntax() as primary source
     */
    private ProtoSyntax deriveSyntax(FieldInfo fieldInfo) {
        // Use detected syntax from proto file if available
        ProtoSyntax detected = fieldInfo.getDetectedSyntax();
        if (detected != null && !detected.isAuto()) {
            return detected;
        }

        // Fallback to inference from observable properties
        // Repeated fields - doesn't matter, use PROTO3
        if (fieldInfo.isRepeated()) {
            return ProtoSyntax.PROTO3;
        }

        // Message types always have has*() method - use PROTO3 (same behavior)
        if (fieldInfo.getType() == Type.TYPE_MESSAGE) {
            return ProtoSyntax.PROTO3;
        }

        // Oneof fields always have has*() method - use PROTO3
        if (fieldInfo.isInOneof()) {
            return ProtoSyntax.PROTO3;
        }

        // For scalar fields, use supportsHasMethod to determine effective syntax
        if (fieldInfo.supportsHasMethod()) {
            // Has method supported → treat as Proto2 (or Proto3 explicit optional)
            return ProtoSyntax.PROTO2;
        } else {
            // No has method → Proto3 implicit
            return ProtoSyntax.PROTO3;
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Create a FieldMethodNames for a field.
     *
     * @param field the merged field
     * @return the method names
     */
    public FieldMethodNames getMethodNames(MergedField field) {
        return FieldMethodNames.from(field.getJavaName());
    }

    /**
     * Check if a field's contract indicates it should have a has method.
     *
     * @param field the merged field
     * @return true if has method should be generated
     */
    public boolean shouldGenerateHasMethod(MergedField field) {
        return getContract(field).unified().hasMethodExists();
    }

    /**
     * Check if a field's getter should use has-check pattern.
     *
     * @param field the merged field
     * @return true if getter should use has-check
     */
    public boolean shouldUseHasCheckInGetter(MergedField field) {
        return getContract(field).unified().getterUsesHasCheck();
    }

    /**
     * Check if a field is nullable according to its contract.
     *
     * @param field the merged field
     * @return true if field is nullable
     */
    public boolean isNullable(MergedField field) {
        return getContract(field).unified().nullable();
    }

    /**
     * Check if builder setters should be skipped for a field.
     *
     * @param field the merged field
     * @return true if builder setters should be skipped
     */
    public boolean shouldSkipBuilderSetter(MergedField field) {
        return getContract(field).shouldSkipBuilderSetter();
    }
}
