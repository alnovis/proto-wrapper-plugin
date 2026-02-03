/**
 * Type references for the IR.
 *
 * <p>This package contains the type system for the Intermediate Representation.
 * All types are represented by implementations of the sealed interface {@link TypeRef}.
 *
 * <h2>Type Hierarchy</h2>
 * <pre>
 * TypeRef (sealed)
 * ├── PrimitiveType - int, long, boolean, etc.
 * ├── ClassType     - Class/interface types with optional type arguments
 * ├── ArrayType     - Array types
 * ├── WildcardType  - Wildcard types (?, ? extends T, ? super T)
 * ├── TypeVariable  - Generic type parameters (T, E)
 * └── VoidType      - Void return type
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use the Types DSL for convenience
 * TypeRef intType = Types.INT;
 * TypeRef stringType = Types.STRING;
 * TypeRef listOfString = Types.list(Types.STRING);
 * TypeRef mapType = Types.map(Types.STRING, Types.INT);
 *
 * // Or create directly
 * TypeRef customType = ClassType.of("com.example.MyClass");
 * TypeRef arrayType = ArrayType.of(Types.INT);
 * TypeRef wildcard = WildcardType.extendsType(Types.type("java.lang.Number"));
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Types
 * @since 2.4.0
 */
package io.alnovis.protowrapper.ir.type;
