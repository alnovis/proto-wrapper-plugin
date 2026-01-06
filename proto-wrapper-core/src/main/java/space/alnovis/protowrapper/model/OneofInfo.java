package space.alnovis.protowrapper.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents information about a protobuf oneof group.
 *
 * <p>A oneof group is a set of fields where at most one field can be set at a time.
 * Setting one field automatically clears all other fields in the oneof group.</p>
 *
 * <p>Example proto definition:</p>
 * <pre>{@code
 * message Payment {
 *     string id = 1;
 *     oneof method {
 *         CreditCard credit_card = 2;
 *         BankTransfer bank_transfer = 3;
 *         Crypto crypto = 4;
 *     }
 * }
 * }</pre>
 */
public class OneofInfo {

    private final String protoName;
    private final String javaName;
    private final int index;
    private final List<Integer> fieldNumbers;
    private final List<FieldInfo> fields;

    /**
     * Creates a new OneofInfo.
     *
     * @param protoName the proto name of the oneof (e.g., "method")
     * @param javaName the Java-style name (e.g., "Method")
     * @param index the 0-based index of this oneof in the containing message
     * @param fieldNumbers the field numbers of fields in this oneof
     */
    public OneofInfo(String protoName, String javaName, int index, List<Integer> fieldNumbers) {
        this(protoName, javaName, index, fieldNumbers, List.of());
    }

    /**
     * Creates a new OneofInfo with fields.
     *
     * @param protoName the proto name of the oneof (e.g., "method")
     * @param javaName the Java-style name (e.g., "Method")
     * @param index the 0-based index of this oneof in the containing message
     * @param fieldNumbers the field numbers of fields in this oneof
     * @param fields the actual field objects in this oneof
     */
    public OneofInfo(String protoName, String javaName, int index, List<Integer> fieldNumbers, List<FieldInfo> fields) {
        this.protoName = Objects.requireNonNull(protoName, "protoName must not be null");
        this.javaName = Objects.requireNonNull(javaName, "javaName must not be null");
        this.index = index;
        this.fieldNumbers = List.copyOf(fieldNumbers);
        this.fields = List.copyOf(fields);
    }

    /**
     * Creates a new OneofInfo with auto-generated Java name.
     *
     * @param protoName the proto name of the oneof (e.g., "method")
     * @param index the 0-based index of this oneof in the containing message
     * @param fieldNumbers the field numbers of fields in this oneof
     */
    public OneofInfo(String protoName, int index, List<Integer> fieldNumbers) {
        this(protoName, toPascalCase(protoName), index, fieldNumbers, List.of());
    }

    /**
     * Creates a new OneofInfo with auto-generated Java name and fields.
     *
     * @param protoName the proto name of the oneof (e.g., "method")
     * @param index the 0-based index of this oneof in the containing message
     * @param fieldNumbers the field numbers of fields in this oneof
     * @param fields the actual field objects in this oneof
     */
    public OneofInfo(String protoName, int index, List<Integer> fieldNumbers, List<FieldInfo> fields) {
        this(protoName, toPascalCase(protoName), index, fieldNumbers, fields);
    }

    /**
     * Converts a proto name (snake_case) to PascalCase.
     *
     * @param protoName the proto name to convert
     * @return the PascalCase name
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
     * Returns the 0-based index of this oneof in the containing message.
     *
     * @return the 0-based index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the field numbers of fields in this oneof.
     *
     * @return list of field numbers
     */
    public List<Integer> getFieldNumbers() {
        return fieldNumbers;
    }

    /**
     * Returns the fields in this oneof.
     * May be empty if fields were not provided during construction.
     *
     * @return list of fields
     */
    public List<FieldInfo> getFields() {
        return fields;
    }

    /**
     * Returns the name of the Case enum for this oneof.
     * E.g., for oneof "method" returns "MethodCase".
     *
     * @return the Case enum name
     */
    public String getCaseEnumName() {
        return javaName + "Case";
    }

    /**
     * Returns the getter name for the case discriminator.
     * E.g., for oneof "method" returns "getMethodCase".
     *
     * @return the case getter method name
     */
    public String getCaseGetterName() {
        return "get" + javaName + "Case";
    }

    /**
     * Returns the clear method name for the entire oneof.
     * E.g., for oneof "method" returns "clearMethod".
     *
     * @return the clear method name
     */
    public String getClearMethodName() {
        return "clear" + javaName;
    }

    /**
     * Returns the extract method name for the case discriminator.
     * E.g., for oneof "method" returns "extractMethodCase".
     *
     * @return the extract case method name
     */
    public String getExtractCaseMethodName() {
        return "extract" + javaName + "Case";
    }

    /**
     * Returns the "NOT_SET" constant name for the Case enum.
     * E.g., for oneof "method" returns "METHOD_NOT_SET".
     *
     * @return the NOT_SET constant name
     */
    public String getNotSetConstantName() {
        return toScreamingSnakeCase(protoName) + "_NOT_SET";
    }

    /**
     * Converts a proto name to SCREAMING_SNAKE_CASE.
     *
     * @param protoName the proto name to convert
     * @return the SCREAMING_SNAKE_CASE name
     */
    private static String toScreamingSnakeCase(String protoName) {
        return protoName.toUpperCase();
    }

    /**
     * Checks if a field number belongs to this oneof.
     *
     * @param fieldNumber the field number to check
     * @return true if the field number is in this oneof
     */
    public boolean containsField(int fieldNumber) {
        return fieldNumbers.contains(fieldNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OneofInfo oneofInfo = (OneofInfo) o;
        return index == oneofInfo.index &&
               Objects.equals(protoName, oneofInfo.protoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protoName, index);
    }

    @Override
    public String toString() {
        return String.format("OneofInfo[%s index=%d fields=%s]", protoName, index, fieldNumbers);
    }
}
