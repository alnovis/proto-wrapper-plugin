package io.alnovis.protowrapper.contract;

/**
 * Represents the cardinality (multiplicity) of a protobuf field.
 *
 * <p>This is one of the key dimensions in the {@link FieldContract} matrix
 * that determines field behavior.</p>
 */
public enum FieldCardinality {

    /**
     * Single value field.
     * In proto2: optional or required field.
     * In proto3: field without 'repeated' keyword.
     */
    SINGULAR,

    /**
     * List of values.
     * Defined with 'repeated' keyword in .proto file.
     * Always returns a List, never null.
     */
    REPEATED,

    /**
     * Key-value pairs.
     * Defined with 'map&lt;K, V&gt;' syntax in .proto file.
     * Technically a special case of repeated (repeated MapEntry message).
     * Always returns a Map, never null.
     */
    MAP
}
