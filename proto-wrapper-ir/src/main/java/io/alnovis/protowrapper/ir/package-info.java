/**
 * Intermediate Representation (IR) for code generation.
 *
 * <p>This package contains a language-agnostic abstract syntax tree (AST) for
 * representing Java code. The IR is designed to be independent of any specific
 * code generation framework (JavaPoet, KotlinPoet, etc.) and can be emitted
 * to multiple target languages.
 *
 * <h2>Package Structure</h2>
 * <p>The IR is organized into the following subpackages:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.type} - Type references (TypeRef, ClassType, PrimitiveType, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr} - Expressions (Expression, MethodCallExpr, BinaryExpr, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt} - Statements (Statement, IfStmt, ForEachStmt, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl} - Declarations (ClassDecl, MethodDecl, FieldDecl, etc.)</li>
 * </ul>
 *
 * <h2>Type References</h2>
 * <p>Type references are represented by the sealed interface {@link io.alnovis.protowrapper.ir.type.TypeRef}:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.type.PrimitiveType} - Primitive types (int, long, boolean, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.type.ClassType} - Class/interface types with optional type arguments</li>
 *   <li>{@link io.alnovis.protowrapper.ir.type.ArrayType} - Array types</li>
 *   <li>{@link io.alnovis.protowrapper.ir.type.WildcardType} - Wildcard types (?, ? extends T, ? super T)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.type.TypeVariable} - Generic type parameters (T, E)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.type.VoidType} - Void return type</li>
 * </ul>
 *
 * <h2>Expressions</h2>
 * <p>Expressions are represented by the sealed interface {@link io.alnovis.protowrapper.ir.expr.Expression}:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.LiteralExpr} - Literal values (strings, numbers, null)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.VarRefExpr} - Variable references</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.ThisExpr} - {@code this} reference</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.TypeRefExpr} - Type reference for static access</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.FieldAccessExpr} - Field access (obj.field)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.MethodCallExpr} - Method invocation</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.ConstructorCallExpr} - Constructor call (new)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.BinaryExpr} - Binary operations (+, -, ==, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.UnaryExpr} - Unary operations (!, -, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.TernaryExpr} - Ternary conditional (? :)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.CastExpr} - Type cast</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.InstanceOfExpr} - instanceof check</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.LambdaExpr} - Lambda expression</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.ArrayInitExpr} - Array initializer</li>
 * </ul>
 *
 * <h2>Statements</h2>
 * <p>Statements are represented by the sealed interface {@link io.alnovis.protowrapper.ir.stmt.Statement}:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ReturnStmt} - Return statement</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.IfStmt} - If-else statement</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ForEachStmt} - Enhanced for loop</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ForStmt} - Traditional for loop</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.WhileStmt} - While loop</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.VarDeclStmt} - Variable declaration</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.AssignStmt} - Assignment</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ExpressionStmt} - Expression as statement</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ThrowStmt} - Throw statement</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.TryCatchStmt} - Try-catch-finally</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.BlockStmt} - Block of statements</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.BreakStmt} - Break statement</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.ContinueStmt} - Continue statement</li>
 * </ul>
 *
 * <h2>Member Declarations</h2>
 * <p>Members are represented by the sealed interface {@link io.alnovis.protowrapper.ir.decl.MemberDeclaration}:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.FieldDecl} - Field declaration</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.MethodDecl} - Method declaration</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.ConstructorDecl} - Constructor declaration</li>
 * </ul>
 *
 * <h2>Type Declarations</h2>
 * <p>Types are represented by the sealed interface {@link io.alnovis.protowrapper.ir.decl.TypeDeclaration}:
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.InterfaceDecl} - Interface declaration</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.ClassDecl} - Class declaration</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.EnumDecl} - Enum declaration</li>
 * </ul>
 *
 * <h2>Supporting Types</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.ParameterDecl} - Method/constructor parameter</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.AnnotationSpec} - Annotation specification</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.EnumConstant} - Enum constant</li>
 *   <li>{@link io.alnovis.protowrapper.ir.stmt.CatchClause} - Catch clause for try-catch</li>
 *   <li>{@link io.alnovis.protowrapper.ir.decl.Modifier} - Java modifiers (public, static, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.BinaryOp} - Binary operators</li>
 *   <li>{@link io.alnovis.protowrapper.ir.expr.UnaryOp} - Unary operators</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability</strong>: All IR nodes are immutable (records with defensive copying)</li>
 *   <li><strong>Sealed hierarchies</strong>: Type hierarchies use sealed interfaces for exhaustive pattern matching</li>
 *   <li><strong>Null safety</strong>: Required fields are validated; optional fields may be null</li>
 *   <li><strong>Language-agnostic</strong>: No dependencies on JavaPoet or other frameworks</li>
 * </ul>
 *
 * @see io.alnovis.protowrapper.dsl
 * @since 2.4.0
 */
package io.alnovis.protowrapper.ir;
