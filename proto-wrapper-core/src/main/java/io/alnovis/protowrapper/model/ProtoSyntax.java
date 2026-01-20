package io.alnovis.protowrapper.model;

/**
 * Enumeration representing the Protocol Buffers syntax version.
 * Replaces the boolean isProto3 and int protobufMajorVersion fields
 * with a type-safe, extensible solution.
 */
public enum ProtoSyntax {

    /**
     * Protocol Buffers version 2 syntax.
     * In proto2:
     * - All optional fields have hasXxx() methods
     * - Enums cannot be converted to integers without explicit accessor
     * - Default values must be explicitly specified
     */
    PROTO2("proto2"),

    /**
     * Protocol Buffers version 3 syntax.
     * In proto3:
     * - Scalar fields without 'optional' modifier do NOT have hasXxx() methods
     * - Enums have built-in getNumber() method
     * - Default values are always zero/empty
     */
    PROTO3("proto3"),

    /**
     * Auto-detect syntax from .proto file declarations.
     * Used in configuration to indicate that the syntax should be
     * determined automatically from each .proto file's syntax declaration.
     */
    AUTO(null);

    private final String syntaxString;

    ProtoSyntax(String syntaxString) {
        this.syntaxString = syntaxString;
    }

    /**
     * Returns the syntax string as it appears in .proto files.
     * @return "proto2", "proto3", or null for AUTO
     */
    public String getSyntaxString() {
        return syntaxString;
    }

    /**
     * Checks if this syntax is proto3.
     * @return true if PROTO3, false otherwise
     */
    public boolean isProto3() {
        return this == PROTO3;
    }

    /**
     * Checks if this syntax is proto2.
     * @return true if PROTO2, false otherwise
     */
    public boolean isProto2() {
        return this == PROTO2;
    }

    /**
     * Checks if syntax should be auto-detected.
     * @return true if AUTO, false otherwise
     */
    public boolean isAuto() {
        return this == AUTO;
    }

    /**
     * Parses the syntax string from a .proto file descriptor.
     * Proto files without explicit syntax declaration default to proto2.
     *
     * @param syntaxString the syntax string from FileDescriptorProto.getSyntax()
     * @return PROTO3 if "proto3", PROTO2 otherwise (default for proto2)
     */
    public static ProtoSyntax fromSyntaxString(String syntaxString) {
        if ("proto3".equals(syntaxString)) {
            return PROTO3;
        }
        // Proto2 files may have "proto2" or empty string (no syntax declaration)
        return PROTO2;
    }

    /**
     * Converts from legacy protobufMajorVersion integer.
     * Provided for backward compatibility with existing configurations.
     *
     * @param majorVersion 2 for proto2, 3 for proto3
     * @return corresponding ProtoSyntax value
     * @deprecated Use ProtoSyntax enum directly instead of integer version
     */
    @Deprecated
    public static ProtoSyntax fromMajorVersion(int majorVersion) {
        return majorVersion == 2 ? PROTO2 : PROTO3;
    }
}
