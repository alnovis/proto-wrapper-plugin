package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MapInfo;
import space.alnovis.protowrapper.model.MergedField;

import static space.alnovis.protowrapper.generator.TypeUtils.*;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.Map;

/**
 * Handler for protobuf map fields.
 *
 * <p>This handler processes protobuf map fields, which are syntactic sugar for
 * a repeated message type containing key-value pairs. Map fields use specialized
 * protobuf API methods that differ from regular repeated fields.</p>
 *
 * <h2>Proto Definition</h2>
 * <pre>
 * message Order {
 *     map&lt;string, int32&gt; item_quantities = 1;
 * }
 * </pre>
 *
 * <h2>Protobuf Representation</h2>
 * <p>Internally, map fields are represented as:</p>
 * <pre>
 * message ItemQuantitiesEntry {
 *     option map_entry = true;
 *     string key = 1;
 *     int32 value = 2;
 * }
 * repeated ItemQuantitiesEntry item_quantities = 1;
 * </pre>
 *
 * <h2>Generated Code</h2>
 * <p><b>Accessor Methods:</b></p>
 * <ul>
 *   <li>{@code Map<String, Integer> getItemQuantitiesMap()} - returns unmodifiable map</li>
 *   <li>{@code int getItemQuantitiesCount()} - returns map size</li>
 *   <li>{@code boolean containsItemQuantities(String key)} - key presence check</li>
 *   <li>{@code Integer getItemQuantitiesOrDefault(String key, Integer defaultValue)}</li>
 *   <li>{@code Integer getItemQuantitiesOrThrow(String key)} - throws if key not found</li>
 * </ul>
 *
 * <p><b>Builder Methods:</b></p>
 * <ul>
 *   <li>{@code Builder putItemQuantities(String key, Integer value)}</li>
 *   <li>{@code Builder putAllItemQuantities(Map<String, Integer> values)}</li>
 *   <li>{@code Builder removeItemQuantities(String key)}</li>
 *   <li>{@code Builder clearItemQuantities()}</li>
 * </ul>
 *
 * <h2>Supported Key Types</h2>
 * <p>Protobuf map keys can be any integral or string type:</p>
 * <ul>
 *   <li>int32, int64, uint32, uint64, sint32, sint64, fixed32, fixed64, sfixed32, sfixed64</li>
 *   <li>bool, string</li>
 * </ul>
 *
 * <h2>Supported Value Types</h2>
 * <p>Protobuf map values can be any scalar or message type (not repeated or map).</p>
 *
 * <h2>Version Handling</h2>
 * <p>If a map field is not present in a version:</p>
 * <ul>
 *   <li>Getters return empty map</li>
 *   <li>Put/remove operations throw {@code UnsupportedOperationException}</li>
 *   <li>Clear operation is no-op (safe)</li>
 * </ul>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see space.alnovis.protowrapper.model.MapInfo
 */
public final class MapFieldHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final MapFieldHandler INSTANCE = new MapFieldHandler();

    private MapFieldHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return field.isMap() && field.getMapInfo() != null;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = createMapType(mapInfo);

        // Add abstract extractXxxMap(proto) method
        builder.addMethod(MethodSpec.methodBuilder(field.getMapExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(mapType)
                .addParameter(ctx.protoType(), "proto")
                .build());
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = createMapType(mapInfo);
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getMapExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(mapType)
                .addParameter(ctx.protoClassName(), "proto");

        if (presentInVersion) {
            // Return the proto's map (already unmodifiable in protobuf)
            extract.addStatement("return proto.get$LMap()", versionJavaName);
        } else {
            // Return empty map for missing field
            extract.addStatement("return $T.emptyMap()", Collections.class)
                    .addJavadoc("Field not present in this version.\n");
        }

        builder.addMethod(extract.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = createMapType(mapInfo);
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = parseSimpleType(mapInfo.getValueJavaType());
        String capitalizedName = ctx.capitalize(field.getJavaName());

        // 1. getXxxMap() - delegate to extract
        builder.addMethod(MethodSpec.methodBuilder(field.getMapGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(mapType)
                .addStatement("return $T.unmodifiableMap($L(proto))",
                        Collections.class, field.getMapExtractMethodName())
                .build());

        // 2. getXxxCount()
        builder.addMethod(MethodSpec.methodBuilder(field.getMapCountMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(TypeName.INT)
                .addStatement("return $L(proto).size()", field.getMapExtractMethodName())
                .build());

        // 3. containsXxx(key)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapContainsMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(keyType, "key")
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L(proto).containsKey(key)", field.getMapExtractMethodName())
                .build());

        // 4. getXxxOrDefault(key, defaultValue)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapGetOrDefaultMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(keyType, "key")
                .addParameter(valueType, "defaultValue")
                .returns(valueType)
                .addStatement("$T map = $L(proto)", mapType, field.getMapExtractMethodName())
                .addStatement("return map.containsKey(key) ? map.get(key) : defaultValue")
                .build());

        // 5. getXxxOrThrow(key)
        builder.addMethod(MethodSpec.methodBuilder(field.getMapGetOrThrowMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(keyType, "key")
                .returns(valueType)
                .addStatement("$T map = $L(proto)", mapType, field.getMapExtractMethodName())
                .beginControlFlow("if (!map.containsKey(key))")
                .addStatement("throw new $T($S + key)", IllegalArgumentException.class, "Key not found: ")
                .endControlFlow()
                .addStatement("return map.get(key)")
                .build());
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = createMapType(mapInfo);
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = parseSimpleType(mapInfo.getValueJavaType());
        String capitalizedName = ctx.capitalize(field.getJavaName());

        // doPut
        builder.addMethod(MethodSpec.methodBuilder(field.getMapDoPutMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .addParameter(valueType, "value")
                .build());

        // doPutAll
        builder.addMethod(MethodSpec.methodBuilder(field.getMapDoPutAllMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(mapType, "values")
                .build());

        // doRemove
        builder.addMethod(MethodSpec.methodBuilder(field.getMapDoRemoveMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(keyType, "key")
                .build());

        // doClear
        builder.addMethod(MethodSpec.methodBuilder(field.getMapDoClearMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build());
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = parseSimpleType(mapInfo.getValueJavaType());
        TypeName mapType = createMapType(mapInfo);

        // put(key, value) -> Builder
        builder.addMethod(MethodSpec.methodBuilder(field.getMapPutMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(keyType, "key")
                .addParameter(valueType, "value")
                .returns(builderReturnType)
                .addStatement("$L(key, value)", field.getMapDoPutMethodName())
                .addStatement("return this")
                .build());

        // putAll(map) -> Builder
        builder.addMethod(MethodSpec.methodBuilder(field.getMapPutAllMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(mapType, "values")
                .returns(builderReturnType)
                .addStatement("$L(values)", field.getMapDoPutAllMethodName())
                .addStatement("return this")
                .build());

        // remove(key) -> Builder
        builder.addMethod(MethodSpec.methodBuilder(field.getMapRemoveMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(keyType, "key")
                .returns(builderReturnType)
                .addStatement("$L(key)", field.getMapDoRemoveMethodName())
                .addStatement("return this")
                .build());

        // clear() -> Builder
        builder.addMethod(MethodSpec.methodBuilder(field.getMapClearMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderReturnType)
                .addStatement("$L()", field.getMapDoClearMethodName())
                .addStatement("return this")
                .build());
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = createMapType(mapInfo);
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = parseSimpleType(mapInfo.getValueJavaType());
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        // doPut
        MethodSpec.Builder doPut = MethodSpec.methodBuilder(field.getMapDoPutMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(keyType, "key")
                .addParameter(valueType, "value");

        if (presentInVersion) {
            doPut.addStatement("protoBuilder.put$L(key, value)", versionJavaName);
        } else {
            doPut.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Field '" + field.getName() + "' is not available in this version");
        }
        builder.addMethod(doPut.build());

        // doPutAll
        MethodSpec.Builder doPutAll = MethodSpec.methodBuilder(field.getMapDoPutAllMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(mapType, "values");

        if (presentInVersion) {
            doPutAll.addStatement("protoBuilder.putAll$L(values)", versionJavaName);
        } else {
            doPutAll.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Field '" + field.getName() + "' is not available in this version");
        }
        builder.addMethod(doPutAll.build());

        // doRemove
        MethodSpec.Builder doRemove = MethodSpec.methodBuilder(field.getMapDoRemoveMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(keyType, "key");

        if (presentInVersion) {
            doRemove.addStatement("protoBuilder.remove$L(key)", versionJavaName);
        } else {
            doRemove.addStatement("throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Field '" + field.getName() + "' is not available in this version");
        }
        builder.addMethod(doRemove.build());

        // doClear
        MethodSpec.Builder doClear = MethodSpec.methodBuilder(field.getMapDoClearMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        if (presentInVersion) {
            doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
        } else {
            // No-op for missing field - clearing a non-existent field is safe
            doClear.addComment("Field not present in this version - no-op");
        }
        builder.addMethod(doClear.build());
    }
}
