package io.alnovis.protowrapper.contract;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.util.Objects;

/**
 * Immutable contract defining field behavior in the wrapper API.
 *
 * <p>This record is the <b>SINGLE SOURCE OF TRUTH</b> for how a field should behave.
 * All code generation decisions are derived from this contract.</p>
 *
 * <h2>Contract Matrix</h2>
 * <p>The contract is computed from the following input dimensions:</p>
 * <ul>
 *   <li>{@link #cardinality()} - singular, repeated, or map</li>
 *   <li>{@link #typeCategory()} - scalar, message, or enum</li>
 *   <li>{@link #presence()} - how field presence is tracked</li>
 *   <li>{@link #inOneof()} - whether field is part of a oneof group</li>
 * </ul>
 *
 * <h2>Derived Behavior</h2>
 * <p>From the input dimensions, the following behavior is computed:</p>
 * <ul>
 *   <li>{@link #hasMethodExists()} - whether has*() method is available</li>
 *   <li>{@link #getterUsesHasCheck()} - whether getter uses has-check pattern</li>
 *   <li>{@link #nullable()} - whether getter can return null</li>
 *   <li>{@link #defaultValueWhenUnset()} - what to return when field is not set</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldContract contract = FieldContract.from(fieldInfo, ProtoSyntax.PROTO3);
 *
 * if (contract.hasMethodExists()) {
 *     // Generate has*() method
 * }
 *
 * if (contract.getterUsesHasCheck()) {
 *     // Generate: return has ? value : null
 * } else {
 *     // Generate: return value
 * }
 * }</pre>
 *
 * @param cardinality field cardinality (singular, repeated, map)
 * @param typeCategory field type category (scalar, message, enum)
 * @param presence field presence semantics
 * @param inOneof whether field is part of a oneof group
 * @param hasMethodExists whether has*() method is available in generated proto
 * @param getterUsesHasCheck whether getter should check has*() before returning
 * @param nullable whether getter can return null
 * @param defaultValueWhenUnset expression for default value when field is not set
 *
 * @see FieldCardinality
 * @see FieldTypeCategory
 * @see FieldPresence
 */
public record FieldContract(
        // Input dimensions
        FieldCardinality cardinality,
        FieldTypeCategory typeCategory,
        FieldPresence presence,
        boolean inOneof,

        // Derived behavior
        boolean hasMethodExists,
        boolean getterUsesHasCheck,
        boolean nullable,
        DefaultValue defaultValueWhenUnset
) {

    /**
     * Represents the default value to return when a field is not set.
     */
    public enum DefaultValue {
        /** Return null */
        NULL("null"),
        /** Return 0 (for int types) */
        ZERO("0"),
        /** Return 0L (for long types) */
        ZERO_LONG("0L"),
        /** Return 0.0f (for float) */
        ZERO_FLOAT("0.0f"),
        /** Return 0.0 (for double) */
        ZERO_DOUBLE("0.0"),
        /** Return false (for boolean) */
        FALSE("false"),
        /** Return empty string "" */
        EMPTY_STRING("\"\""),
        /** Return empty byte array */
        EMPTY_BYTES("new byte[0]"),
        /** Return first enum value */
        FIRST_ENUM_VALUE("/* first enum value */"),
        /** Return empty immutable list */
        EMPTY_LIST("java.util.List.of()"),
        /** Return empty immutable map */
        EMPTY_MAP("java.util.Map.of()");

        private final String expression;

        DefaultValue(String expression) {
            this.expression = expression;
        }

        /**
         * @return Java expression for this default value
         */
        public String expression() {
            return expression;
        }
    }

    /**
     * Creates a FieldContract from a FieldInfo and syntax.
     *
     * <p>This is the main factory method that implements the contract matrix logic.
     * All behavior is computed deterministically from the input parameters.</p>
     *
     * @param field the field information
     * @param syntax the proto syntax (PROTO2 or PROTO3)
     * @return the computed contract
     * @throws NullPointerException if field or syntax is null
     */
    public static FieldContract from(FieldInfo field, ProtoSyntax syntax) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(syntax, "syntax must not be null");

        // Determine input dimensions
        FieldCardinality cardinality = determineCardinality(field);
        FieldTypeCategory typeCategory = determineTypeCategory(field);
        FieldPresence presence = determinePresence(field, syntax);
        boolean inOneof = field.getOneofIndex() >= 0;

        // Compute derived behavior from contract matrix
        boolean hasMethodExists = computeHasMethodExists(cardinality, typeCategory, presence, inOneof);
        boolean getterUsesHasCheck = computeGetterUsesHasCheck(cardinality, typeCategory, presence, inOneof, hasMethodExists);
        boolean nullable = computeNullable(cardinality, typeCategory, presence, inOneof);
        DefaultValue defaultValue = computeDefaultValue(cardinality, typeCategory, field.getType(), nullable);

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
     * Creates a FieldContract directly from a FieldDescriptorProto.
     *
     * @param proto the field descriptor
     * @param syntax the proto syntax
     * @param oneofIndex the oneof index (-1 if not in oneof)
     * @return the computed contract
     */
    public static FieldContract fromDescriptor(FieldDescriptorProto proto, ProtoSyntax syntax, int oneofIndex) {
        Objects.requireNonNull(proto, "proto must not be null");
        Objects.requireNonNull(syntax, "syntax must not be null");

        // Determine input dimensions directly from descriptor
        FieldCardinality cardinality = determineCardinalityFromProto(proto);
        FieldTypeCategory typeCategory = FieldTypeCategory.fromProtoType(proto.getType());
        FieldPresence presence = determinePresenceFromProto(proto, syntax, oneofIndex);
        boolean inOneof = oneofIndex >= 0;

        // Compute derived behavior
        boolean hasMethodExists = computeHasMethodExists(cardinality, typeCategory, presence, inOneof);
        boolean getterUsesHasCheck = computeGetterUsesHasCheck(cardinality, typeCategory, presence, inOneof, hasMethodExists);
        boolean nullable = computeNullable(cardinality, typeCategory, presence, inOneof);
        DefaultValue defaultValue = computeDefaultValue(cardinality, typeCategory, proto.getType(), nullable);

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

    // ==================== Input Dimension Computation ====================

    private static FieldCardinality determineCardinality(FieldInfo field) {
        if (field.isMap()) {
            return FieldCardinality.MAP;
        } else if (field.isRepeated()) {
            return FieldCardinality.REPEATED;
        } else {
            return FieldCardinality.SINGULAR;
        }
    }

    private static FieldCardinality determineCardinalityFromProto(FieldDescriptorProto proto) {
        // Note: Map detection requires additional context (map entry descriptor)
        // For now, treat as repeated if LABEL_REPEATED
        if (proto.getLabel() == Label.LABEL_REPEATED) {
            return FieldCardinality.REPEATED; // May be MAP, caller should check
        }
        return FieldCardinality.SINGULAR;
    }

    private static FieldTypeCategory determineTypeCategory(FieldInfo field) {
        return FieldTypeCategory.fromProtoType(field.getType());
    }

    private static FieldPresence determinePresence(FieldInfo field, ProtoSyntax syntax) {
        if (syntax.isProto2()) {
            if (field.getLabel() == Label.LABEL_REQUIRED) {
                return FieldPresence.PROTO2_REQUIRED;
            }
            return FieldPresence.PROTO2_OPTIONAL;
        } else {
            // Proto3
            // Check if field has explicit optional (indicated by being in synthetic oneof)
            if (field.getOneofIndex() >= 0) {
                // Could be explicit optional or real oneof
                // For synthetic oneofs (proto3 optional), treat as explicit optional
                return FieldPresence.PROTO3_EXPLICIT_OPTIONAL;
            }
            return FieldPresence.PROTO3_IMPLICIT;
        }
    }

    private static FieldPresence determinePresenceFromProto(FieldDescriptorProto proto, ProtoSyntax syntax, int oneofIndex) {
        if (syntax.isProto2()) {
            if (proto.getLabel() == Label.LABEL_REQUIRED) {
                return FieldPresence.PROTO2_REQUIRED;
            }
            return FieldPresence.PROTO2_OPTIONAL;
        } else {
            // Proto3
            if (oneofIndex >= 0) {
                return FieldPresence.PROTO3_EXPLICIT_OPTIONAL;
            }
            return FieldPresence.PROTO3_IMPLICIT;
        }
    }

    // ==================== Derived Behavior Computation ====================

    /**
     * CONTRACT MATRIX RULE 1: has*() Method Availability
     *
     * <pre>
     * has*() exists IF:
     *   - NOT repeated AND NOT map
     *   - AND (in oneof
     *          OR proto2
     *          OR message type
     *          OR proto3 explicit optional)
     * </pre>
     */
    private static boolean computeHasMethodExists(
            FieldCardinality cardinality,
            FieldTypeCategory typeCategory,
            FieldPresence presence,
            boolean inOneof) {

        // Rule: Repeated and map fields NEVER have has*() method
        if (cardinality == FieldCardinality.REPEATED || cardinality == FieldCardinality.MAP) {
            return false;
        }

        // Rule: Fields in oneof always have has*() method
        if (inOneof) {
            return true;
        }

        // Rule: Message types always have has*() method (in both proto2 and proto3)
        if (typeCategory == FieldTypeCategory.MESSAGE) {
            return true;
        }

        // Rule: Proto2 non-repeated fields always have has*() method
        if (presence.isProto2()) {
            return true;
        }

        // Rule: Proto3 explicit optional has has*() method
        // Proto3 implicit (no optional keyword) - scalars do NOT have has*()
        return presence == FieldPresence.PROTO3_EXPLICIT_OPTIONAL;
    }

    /**
     * CONTRACT MATRIX RULE 2: Getter Uses Has-Check Pattern
     *
     * <pre>
     * Use has-check pattern IF:
     *   - has*() method exists
     *   - AND NOT required (required fields always have value)
     *   - AND (message type OR nullable)
     * </pre>
     *
     * <p>The has-check pattern is: {@code return has*() ? value : null}</p>
     */
    private static boolean computeGetterUsesHasCheck(
            FieldCardinality cardinality,
            FieldTypeCategory typeCategory,
            FieldPresence presence,
            boolean inOneof,
            boolean hasMethodExists) {

        // Rule: If no has*() method, can't use has-check pattern
        if (!hasMethodExists) {
            return false;
        }

        // Rule: Required fields always have value, no need for has-check
        if (presence == FieldPresence.PROTO2_REQUIRED) {
            return false;
        }

        // Rule: Repeated/map fields don't use has-check (return empty collection)
        if (cardinality != FieldCardinality.SINGULAR) {
            return false;
        }

        // Rule: Message types use has-check to return null when unset
        if (typeCategory == FieldTypeCategory.MESSAGE) {
            return true;
        }

        // Rule: Nullable scalars use has-check
        // This includes: proto2 optional, proto3 explicit optional, oneof
        return presence.scalarNullable() || inOneof;
    }

    /**
     * CONTRACT MATRIX RULE 3: Nullable
     *
     * <pre>
     * Field is nullable IF:
     *   - Singular AND NOT required
     *   - AND (has*() exists for scalar types
     *          OR is message type)
     * </pre>
     */
    private static boolean computeNullable(
            FieldCardinality cardinality,
            FieldTypeCategory typeCategory,
            FieldPresence presence,
            boolean inOneof) {

        // Rule: Repeated and map fields never return null (empty collection instead)
        if (cardinality != FieldCardinality.SINGULAR) {
            return false;
        }

        // Rule: Required fields never return null
        if (presence == FieldPresence.PROTO2_REQUIRED) {
            return false;
        }

        // Rule: Message types are nullable (return null when unset)
        if (typeCategory == FieldTypeCategory.MESSAGE) {
            return true;
        }

        // Rule: Oneof fields are always nullable
        if (inOneof) {
            return true;
        }

        // Rule: Proto2 optional and proto3 explicit optional are nullable
        return presence.scalarNullable();
    }

    /**
     * CONTRACT MATRIX RULE 4: Default Value When Unset
     *
     * <p>Determines what the getter returns when the field is not set.</p>
     */
    private static DefaultValue computeDefaultValue(
            FieldCardinality cardinality,
            FieldTypeCategory typeCategory,
            Type protoType,
            boolean nullable) {

        // Rule: Repeated fields return empty list
        if (cardinality == FieldCardinality.REPEATED) {
            return DefaultValue.EMPTY_LIST;
        }

        // Rule: Map fields return empty map
        if (cardinality == FieldCardinality.MAP) {
            return DefaultValue.EMPTY_MAP;
        }

        // Rule: Nullable fields return null
        if (nullable) {
            return DefaultValue.NULL;
        }

        // Rule: Non-nullable fields return type-specific default
        return switch (typeCategory) {
            case SCALAR_NUMERIC -> numericDefault(protoType);
            case SCALAR_STRING -> DefaultValue.EMPTY_STRING;
            case SCALAR_BYTES -> DefaultValue.EMPTY_BYTES;
            case ENUM -> DefaultValue.FIRST_ENUM_VALUE;
            case MESSAGE -> DefaultValue.NULL; // Should not reach here if nullable logic is correct
        };
    }

    private static DefaultValue numericDefault(Type protoType) {
        return switch (protoType) {
            case TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32 -> DefaultValue.ZERO;
            case TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64 -> DefaultValue.ZERO_LONG;
            case TYPE_FLOAT -> DefaultValue.ZERO_FLOAT;
            case TYPE_DOUBLE -> DefaultValue.ZERO_DOUBLE;
            case TYPE_BOOL -> DefaultValue.FALSE;
            default -> DefaultValue.ZERO; // Fallback
        };
    }

    // ==================== Convenience Methods ====================

    /**
     * @return true if this field is singular (not repeated or map)
     */
    public boolean isSingular() {
        return cardinality == FieldCardinality.SINGULAR;
    }

    /**
     * @return true if this field is repeated (including maps)
     */
    public boolean isRepeated() {
        return cardinality == FieldCardinality.REPEATED;
    }

    /**
     * @return true if this field is a map
     */
    public boolean isMap() {
        return cardinality == FieldCardinality.MAP;
    }

    /**
     * @return true if this field is a message type
     */
    public boolean isMessage() {
        return typeCategory == FieldTypeCategory.MESSAGE;
    }

    /**
     * @return true if this field is an enum type
     */
    public boolean isEnum() {
        return typeCategory == FieldTypeCategory.ENUM;
    }

    /**
     * @return true if this field is a scalar type
     */
    public boolean isScalar() {
        return typeCategory.isScalar();
    }

    /**
     * @return true if this is a proto3 field with implicit presence (no has*() for scalars)
     */
    public boolean isProto3Implicit() {
        return presence == FieldPresence.PROTO3_IMPLICIT;
    }

    /**
     * Returns a human-readable description of this contract for debugging.
     *
     * @return formatted string describing the contract
     */
    public String describe() {
        return String.format(
                "FieldContract[%s %s %s%s] -> has=%s, hasCheck=%s, nullable=%s, default=%s",
                presence,
                cardinality,
                typeCategory,
                inOneof ? " (oneof)" : "",
                hasMethodExists,
                getterUsesHasCheck,
                nullable,
                defaultValueWhenUnset
        );
    }
}
