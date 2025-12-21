package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for scalar WIDENING conflict fields.
 *
 * <p>This handler processes scalar fields where different versions use different
 * numeric types that can be widened (e.g., int in v1, long in v2). The unified
 * type is the wider type (long), and conversions are handled transparently.</p>
 *
 * <p>For extract methods, the DefaultHandler already handles widening through
 * the generateProtoGetterCall. This handler only adds special builder methods
 * with range checking for narrowing operations.</p>
 */
public final class WideningHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final WideningHandler INSTANCE = new WideningHandler();

    private WideningHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // WIDENING extract methods are the same as default - delegate to parent
        TypeName returnType = ctx.parseFieldType(field);
        addAbstractHasMethod(builder, field, ctx);
        addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        // WIDENING extract implementation uses the DefaultHandler's logic
        // The widening cast is handled in generateProtoGetterCall
        DefaultHandler.INSTANCE.addExtractImplementation(builder, field, presentInVersion, ctx);
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // WIDENING getter implementation is the same as default
        DefaultHandler.INSTANCE.addGetterImplementation(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());

        // doSetXxx(widerType)
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(widerType, field.getJavaName())
                .build());

        // doClearXxx()
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("doClear" + capName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Determine the wider type (unified type) and the version's actual type
        String widerType = field.getJavaType();
        TypeName widerTypeName = getWiderPrimitiveType(widerType);

        // Get the version-specific type
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionType = versionField != null ? versionField.getJavaType() : widerType;
        boolean needsNarrowing = !versionType.equals(widerType) &&
                !versionType.equals("Long") && !versionType.equals("Double");

        // doSetXxx(widerType value)
        MethodSpec.Builder doSet = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(widerTypeName, field.getJavaName());

        if (presentInVersion) {
            if (needsNarrowing) {
                addNarrowingSetterBody(doSet, field.getJavaName(), widerType, versionType, versionJavaName, version);
            } else {
                doSet.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSet.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSet.build());

        // doClearXxx()
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED);

            if (presentInVersion) {
                doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
            } else {
                doClear.addComment("Field not present in this version - ignored");
            }
            builder.addMethod(doClear.build());
        }
    }

    private void addNarrowingSetterBody(MethodSpec.Builder doSet, String fieldName,
                                         String widerType, String versionType,
                                         String versionJavaName, String version) {
        if ("long".equals(widerType) || "Long".equals(widerType)) {
            // long -> int narrowing with range check
            doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                    fieldName, Integer.class, fieldName, Integer.class);
            doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                    IllegalArgumentException.class, fieldName, version);
            doSet.endControlFlow();
            doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, fieldName);
        } else if ("double".equals(widerType) || "Double".equals(widerType)) {
            // double -> int narrowing with range check
            doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                    fieldName, Integer.class, fieldName, Integer.class);
            doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                    IllegalArgumentException.class, fieldName, version);
            doSet.endControlFlow();
            doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, fieldName);
        } else {
            // Unknown narrowing - just cast
            doSet.addStatement("protoBuilder.set$L(($L) $L)", versionJavaName, versionType, fieldName);
        }
    }

}
