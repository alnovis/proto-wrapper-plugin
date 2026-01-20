/**
 * Exception hierarchy for proto-wrapper errors.
 *
 * <p>This package provides a structured exception hierarchy for all errors
 * that can occur during schema merging, code generation, and runtime
 * wrapper operations.</p>
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * {@link io.alnovis.protowrapper.exception.ProtoWrapperException} (base)
 * +-- {@link io.alnovis.protowrapper.exception.SchemaValidationException}
 * |   +-- {@link io.alnovis.protowrapper.exception.FieldConflictException}
 * |   +-- {@link io.alnovis.protowrapper.exception.OneofConflictException}
 * +-- {@link io.alnovis.protowrapper.exception.ConversionException}
 * |   +-- {@link io.alnovis.protowrapper.exception.EnumValueNotSupportedException}
 * |   +-- {@link io.alnovis.protowrapper.exception.TypeRangeException}
 * |   +-- {@link io.alnovis.protowrapper.exception.FieldNotAvailableException}
 * +-- {@link io.alnovis.protowrapper.exception.VersionNotSupportedException}
 * </pre>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.exception.ErrorCode} - Unique error identifiers</li>
 *   <li>{@link io.alnovis.protowrapper.exception.ErrorContext} - Structured error context</li>
 *   <li>{@link io.alnovis.protowrapper.exception.ProtoWrapperException} - Base exception</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     wrapper.setStatus(StatusEnum.DELETED);
 * } catch (ProtoWrapperException e) {
 *     ErrorCode code = e.getErrorCode();
 *     ErrorContext ctx = e.getContext();
 *
 *     switch (code) {
 *         case CONVERSION_ENUM_VALUE_NOT_SUPPORTED -> {
 *             EnumValueNotSupportedException evns = (EnumValueNotSupportedException) e;
 *             log.warn("Enum {} not valid, valid values: {}",
 *                 evns.getEnumValue(), evns.getValidValues());
 *         }
 *         case CONVERSION_FIELD_NOT_AVAILABLE -> {
 *             FieldNotAvailableException fna = (FieldNotAvailableException) e;
 *             log.warn("Field {} only in versions: {}",
 *                 fna.getFieldName(), fna.getAvailableInVersions());
 *         }
 *         default -> log.error("Proto error: {}", e.getMessage());
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Codes</h2>
 * <p>Each exception carries an {@link io.alnovis.protowrapper.exception.ErrorCode}
 * for programmatic error handling:</p>
 * <ul>
 *   <li>SCHEMA-* codes for validation errors</li>
 *   <li>CONV-* codes for conversion errors</li>
 *   <li>VER-* codes for version errors</li>
 *   <li>FIELD-* codes for field-specific errors</li>
 * </ul>
 *
 * @see io.alnovis.protowrapper.exception.ProtoWrapperException
 * @see io.alnovis.protowrapper.exception.ErrorCode
 */
package io.alnovis.protowrapper.exception;
