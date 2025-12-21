package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

/**
 * Sealed base class providing common functionality for conflict handlers.
 *
 * <p>Contains utility methods for generating common method patterns
 * and extracting field information.</p>
 *
 * <p>This is a sealed class that permits only the known handler implementations,
 * ensuring type safety and preventing accidental external extensions.</p>
 */
public abstract sealed class AbstractConflictHandler permits
        IntEnumHandler, StringBytesHandler, WideningHandler,
        PrimitiveMessageHandler, RepeatedConflictHandler, DefaultHandler {

    /**
     * Add an abstract extractHas method for optional fields.
     */
    protected void addAbstractHasMethod(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoType(), "proto")
                    .build());
        }
    }

    /**
     * Add an abstract extract method.
     */
    protected void addAbstractExtractMethod(TypeSpec.Builder builder, MergedField field,
                                             TypeName returnType, ProcessingContext ctx) {
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(ctx.protoType(), "proto")
                .build());
    }

    /**
     * Add a concrete has method implementation for present fields.
     */
    protected void addHasMethodImpl(TypeSpec.Builder builder, MergedField field,
                                     String versionJavaName, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addStatement("return proto.has$L()", versionJavaName)
                    .build());
        }
    }

    /**
     * Add a concrete has method implementation returning false (field not present).
     */
    protected void addMissingHasMethodImpl(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addStatement("return false")
                    .addJavadoc("Field not present in this version.\n")
                    .build());
        }
    }

    /**
     * Add a getter implementation that delegates to extract methods.
     */
    protected void addStandardGetterImpl(TypeSpec.Builder builder, MergedField field,
                                          TypeName returnType, ProcessingContext ctx) {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);

        if (field.needsHasCheck()) {
            getter.addStatement("return $L(proto) ? $L(proto) : null",
                    field.getExtractHasMethodName(), field.getExtractMethodName());
        } else {
            getter.addStatement("return $L(proto)", field.getExtractMethodName());
        }

        builder.addMethod(getter.build());
    }

    /**
     * Add has method implementation to abstract class.
     */
    protected void addHasMethodToAbstract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            MethodSpec has = MethodSpec.methodBuilder("has" + ctx.capitalize(field.getJavaName()))
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.BOOLEAN)
                    .addStatement("return $L(proto)", field.getExtractHasMethodName())
                    .build();
            builder.addMethod(has);
        }
    }

    /**
     * Get the field type for the current version.
     */
    protected FieldInfo getVersionField(MergedField field, ProcessingContext ctx) {
        String version = ctx.version();
        return version != null ? field.getVersionFields().get(version) : null;
    }

    /**
     * Extract element type from List type.
     */
    protected TypeName extractListElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName parameterized
                && !parameterized.typeArguments.isEmpty()) {
            return parameterized.typeArguments.get(0);
        }
        return ClassName.get(Object.class);
    }

    /**
     * Get the wider primitive type for widening operations.
     */
    protected TypeName getWiderPrimitiveType(String javaType) {
        return switch (javaType) {
            case "long", "Long" -> TypeName.LONG;
            case "double", "Double" -> TypeName.DOUBLE;
            case "int", "Integer" -> TypeName.INT;
            default -> TypeName.LONG;
        };
    }
}
