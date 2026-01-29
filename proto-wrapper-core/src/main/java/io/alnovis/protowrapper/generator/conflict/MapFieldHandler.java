package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import io.alnovis.protowrapper.model.MapInfo;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static io.alnovis.protowrapper.generator.TypeUtils.*;

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
 * @see io.alnovis.protowrapper.model.MapInfo
 */
public final class MapFieldHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance of the map field handler. */
    public static final MapFieldHandler INSTANCE = new MapFieldHandler();

    private MapFieldHandler() {
        // Singleton
    }

    // Type resolution methods delegated to MapTypeResolver

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.MAP_FIELD;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return field.isMap() && field.getMapInfo() != null;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);

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
        MapInfo unifiedMapInfo = field.getMapInfo();
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);
        TypeName resolvedValueType = MapTypeResolver.getResolvedValueType(field, ctx);
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);
        boolean hasValueConflict = field.hasMapValueConflict();

        // Get version-specific MapInfo for correct proto type detection
        MapInfo versionMapInfo = MapTypeResolver.getVersionSpecificMapInfo(field, unifiedMapInfo, ctx);

        // Proto value type for THIS version (not unified type)
        TypeName protoValueType = parseSimpleType(versionMapInfo.getValueJavaType());

        // Check if this map needs type conversion (benefits from caching)
        boolean useCache = presentInVersion && MapTypeResolver.needsTypeConversion(field, versionMapInfo, resolvedValueType);
        String cacheFieldName = MapTypeResolver.getCacheFieldName(field);

        // Add cache field for maps that need type conversion (volatile for thread safety)
        if (useCache) {
            builder.addField(FieldSpec.builder(mapType, cacheFieldName, Modifier.PRIVATE, Modifier.VOLATILE)
                    .addJavadoc("Cached converted map for lazy evaluation (thread-safe).\n")
                    .build());
        }

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getMapExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(mapType)
                .addParameter(ctx.protoClassName(), "proto");

        if (presentInVersion) {
            // Handle type conversion for map value conflicts
            // Compare proto value type with resolved unified type
            if (hasValueConflict && !resolvedValueType.equals(protoValueType)) {
                // WIDENING: int -> long conversion (only when proto has int, unified has long)
                if (field.getMapValueConflictType() == MergedField.ConflictType.WIDENING
                        && protoValueType.equals(TypeName.INT)) {
                    // Need to convert Map<K, Integer> to Map<K, Long>
                    TypeName keyType = parseSimpleType(versionMapInfo.getKeyJavaType());
                    // Use lazy caching
                    extract.beginControlFlow("if ($N != null)", cacheFieldName);
                    extract.addStatement("return $N", cacheFieldName);
                    extract.endControlFlow();
                    extract.addStatement("$T<$T, $T> source = proto.get$LMap()",
                            Map.class, keyType.box(), protoValueType.box(), versionJavaName);
                    extract.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), resolvedValueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    extract.addStatement("source.forEach((k, v) -> result.put(k, v.longValue()))");
                    extract.addStatement("$N = result", cacheFieldName);
                    extract.addStatement("return result");
                }
                // INT_ENUM: enum -> int conversion (only when proto has enum, unified has int)
                else if (field.getMapValueConflictType() == MergedField.ConflictType.INT_ENUM
                        && versionMapInfo.hasEnumValue()) {
                    // Convert Map<K, EnumType> to Map<K, Integer>
                    TypeName keyType = parseSimpleType(versionMapInfo.getKeyJavaType());
                    // Use lazy caching
                    extract.beginControlFlow("if ($N != null)", cacheFieldName);
                    extract.addStatement("return $N", cacheFieldName);
                    extract.endControlFlow();
                    extract.addStatement("$T<$T, ?> source = proto.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    extract.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), resolvedValueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    extract.addStatement("source.forEach((k, v) -> result.put(k, (($T) v).getNumber()))",
                            ClassName.get("com.google.protobuf", "ProtocolMessageEnum"));
                    extract.addStatement("$N = result", cacheFieldName);
                    extract.addStatement("return result");
                }
                else {
                    // Default: direct return (types match or handled implicitly)
                    extract.addStatement("return proto.get$LMap()", versionJavaName);
                }
            } else {
                // No type conflict - check for WKT conversion
                Optional<WellKnownTypeInfo> wktOpt = MapTypeResolver.getWktForMapValue(versionMapInfo, ctx);
                if (wktOpt.isPresent()) {
                    // WKT map value - need to convert proto WKT to Java type
                    WellKnownTypeInfo wkt = wktOpt.get();
                    TypeName keyType = parseSimpleType(versionMapInfo.getKeyJavaType());
                    extract.addStatement("$T<$T, ?> source = proto.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    extract.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), resolvedValueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    // Generate conversion lambda based on WKT type
                    String conversionCode = MapWktConverter.generateWktMapValueExtraction(wkt);
                    extract.addStatement("source.forEach((k, v) -> result.put(k, $L))", conversionCode);
                    extract.addStatement("return result");
                } else if (versionMapInfo.hasMessageValue()) {
                    // Message map value - wrap each value with impl wrapper class
                    TypeName keyType = parseSimpleType(versionMapInfo.getKeyJavaType());
                    String wrapperClass = MapTypeNameProvider.getMapValueWrapperClass(versionMapInfo, ctx);
                    String protoType = MapTypeNameProvider.getMapValueProtoType(versionMapInfo, ctx);
                    extract.addStatement("$T<$T, ?> source = proto.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    extract.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), resolvedValueType,
                            ClassName.get("java.util", "LinkedHashMap"));
                    extract.addStatement("source.forEach((k, v) -> result.put(k, new $L(($L) v)))", wrapperClass, protoType);
                    extract.addStatement("return result");
                } else if (versionMapInfo.hasEnumValue()) {
                    // Enum map value - convert each value with fromProtoValue
                    TypeName keyType = parseSimpleType(versionMapInfo.getKeyJavaType());
                    String enumType = MapTypeNameProvider.getMapValueEnumType(versionMapInfo, ctx);
                    extract.addStatement("$T<$T, ?> source = proto.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    extract.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), resolvedValueType,
                            ClassName.get("java.util", "LinkedHashMap"));
                    extract.addStatement("source.forEach((k, v) -> result.put(k, $L.fromProtoValue((($T) v).getNumber())))",
                            enumType, ClassName.get("com.google.protobuf", "ProtocolMessageEnum"));
                    extract.addStatement("return result");
                } else {
                    // Scalar - return directly
                    extract.addStatement("return proto.get$LMap()", versionJavaName);
                }
            }
        } else {
            // Return empty map for missing field
            extract.addStatement("return $T.emptyMap()", Collections.class)
                    .addJavadoc("Field not present in this version.\n");
        }

        builder.addMethod(extract.build());
    }

    // WKT conversion methods delegated to MapWktConverter

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = MapTypeResolver.getResolvedValueType(field, ctx);
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
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = MapTypeResolver.getResolvedValueType(field, ctx);
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

        // doGetXxxMap - for inspection
        builder.addMethod(MethodSpec.methodBuilder(field.getMapDoGetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(mapType)
                .build());
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        TypeName keyType = parseSimpleType(mapInfo.getKeyJavaType());
        TypeName valueType = MapTypeResolver.getResolvedValueType(field, ctx);
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);

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

        // getXxxMap() -> Map - for inspection during building
        builder.addMethod(MethodSpec.methodBuilder(field.getMapGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(mapType)
                .addStatement("return $T.unmodifiableMap($L())", Collections.class, field.getMapDoGetMethodName())
                .build());
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        MapInfo unifiedMapInfo = field.getMapInfo();
        TypeName mapType = MapTypeResolver.getResolvedMapType(field, ctx);
        TypeName keyType = parseSimpleType(unifiedMapInfo.getKeyJavaType());
        TypeName valueType = MapTypeResolver.getResolvedValueType(field, ctx);
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);
        boolean hasValueConflict = field.hasMapValueConflict();

        // Get version-specific MapInfo for correct proto type detection
        MapInfo versionMapInfo = MapTypeResolver.getVersionSpecificMapInfo(field, unifiedMapInfo, ctx);
        TypeName protoValueType = MapTypeResolver.getProtoValueType(versionMapInfo, ctx);

        // doPut
        MethodSpec.Builder doPut = MethodSpec.methodBuilder(field.getMapDoPutMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(keyType, "key")
                .addParameter(valueType, "value");

        if (presentInVersion) {
            // Handle type conversion for map value conflicts
            if (hasValueConflict && !valueType.equals(protoValueType)) {
                // WIDENING: long -> int cast with range check
                if (field.getMapValueConflictType() == MergedField.ConflictType.WIDENING
                        && valueType.equals(TypeName.LONG) && protoValueType.equals(TypeName.INT)) {
                    doPut.beginControlFlow("if (value < $T.MIN_VALUE || value > $T.MAX_VALUE)",
                            Integer.class, Integer.class);
                    doPut.addStatement("throw new $T($S + value)",
                            IllegalArgumentException.class, "Value out of int range: ");
                    doPut.endControlFlow();
                    doPut.addStatement("protoBuilder.put$L(key, (int) value)", versionJavaName);
                }
                // INT_ENUM: int -> enum conversion (when version has enum)
                else if (field.getMapValueConflictType() == MergedField.ConflictType.INT_ENUM
                        && versionMapInfo.hasEnumValue()) {
                    // Convert int to proto enum using forNumber() with null check
                    doPut.addStatement("$T enumValue = $T.forNumber(value)", protoValueType, protoValueType);
                    doPut.beginControlFlow("if (enumValue == null)");
                    doPut.addStatement("throw new $T($S + value + $S)",
                            IllegalArgumentException.class,
                            "Invalid enum value ",
                            " for field '" + field.getName() + "'");
                    doPut.endControlFlow();
                    doPut.addStatement("protoBuilder.put$L(key, enumValue)", versionJavaName);
                }
                else {
                    // Default: direct pass (types match or implicit conversion)
                    doPut.addStatement("protoBuilder.put$L(key, value)", versionJavaName);
                }
            } else {
                // No type conflict - check for WKT conversion
                Optional<WellKnownTypeInfo> wktOpt = MapTypeResolver.getWktForMapValue(versionMapInfo, ctx);
                if (wktOpt.isPresent()) {
                    // WKT map value - need to convert Java type to proto WKT
                    WellKnownTypeInfo wkt = wktOpt.get();
                    String buildCode = MapWktConverter.generateWktMapValueBuild(wkt, "value", versionJavaName);
                    doPut.addStatement(buildCode);
                } else if (versionMapInfo.hasMessageValue()) {
                    // Message map value - extract proto from wrapper
                    String protoType = MapTypeNameProvider.getMapValueProtoType(versionMapInfo, ctx);
                    doPut.addStatement("protoBuilder.put$L(key, ($L) extractProto(value))",
                            versionJavaName, protoType);
                } else if (versionMapInfo.hasEnumValue()) {
                    // Enum map value - convert wrapper enum to proto enum
                    String protoEnumType = MapTypeNameProvider.getMapValueProtoEnumType(versionMapInfo, ctx);
                    String enumMethod = CodeGenerationHelper.getEnumFromIntMethod(ctx);
                    doPut.addStatement("protoBuilder.put$L(key, $L.$L(value.getValue()))",
                            versionJavaName, protoEnumType, enumMethod);
                } else {
                    doPut.addStatement("protoBuilder.put$L(key, value)", versionJavaName);
                }
            }
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
            // Handle type conversion for map value conflicts
            if (hasValueConflict && !valueType.equals(protoValueType)) {
                // WIDENING: convert Map<K, Long> to Map<K, Integer>
                if (field.getMapValueConflictType() == MergedField.ConflictType.WIDENING
                        && valueType.equals(TypeName.LONG) && protoValueType.equals(TypeName.INT)) {
                    doPutAll.addStatement("values.forEach((k, v) -> $L(k, v))", field.getMapDoPutMethodName());
                }
                // INT_ENUM: convert Map<K, Integer> to Map<K, EnumType>
                // Delegate to doPut which has validation for invalid enum values
                else if (field.getMapValueConflictType() == MergedField.ConflictType.INT_ENUM
                        && versionMapInfo.hasEnumValue()) {
                    doPutAll.addStatement("values.forEach((k, v) -> $L(k, v))", field.getMapDoPutMethodName());
                }
                else {
                    // Default: direct pass
                    doPutAll.addStatement("protoBuilder.putAll$L(values)", versionJavaName);
                }
            } else {
                // No type conflict - check for WKT conversion
                Optional<WellKnownTypeInfo> wktOpt = MapTypeResolver.getWktForMapValue(versionMapInfo, ctx);
                if (wktOpt.isPresent()) {
                    // WKT - delegate to doPut for each entry (for conversion)
                    doPutAll.addStatement("values.forEach((k, v) -> $L(k, v))", field.getMapDoPutMethodName());
                } else if (versionMapInfo.hasMessageValue()) {
                    // Message - delegate to doPut for each entry (for wrapper -> proto conversion)
                    doPutAll.addStatement("values.forEach((k, v) -> $L(k, v))", field.getMapDoPutMethodName());
                } else if (versionMapInfo.hasEnumValue()) {
                    // Enum - delegate to doPut for each entry (for wrapper -> proto conversion)
                    doPutAll.addStatement("values.forEach((k, v) -> $L(k, v))", field.getMapDoPutMethodName());
                } else {
                    doPutAll.addStatement("protoBuilder.putAll$L(values)", versionJavaName);
                }
            }
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

        // doGetXxxMap
        MethodSpec.Builder doGet = MethodSpec.methodBuilder(field.getMapDoGetMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(mapType);

        if (presentInVersion) {
            // Handle type conversion for map value conflicts
            if (hasValueConflict && !valueType.equals(protoValueType)) {
                // WIDENING: convert Map<K, Integer> to Map<K, Long>
                if (field.getMapValueConflictType() == MergedField.ConflictType.WIDENING
                        && valueType.equals(TypeName.LONG) && protoValueType.equals(TypeName.INT)) {
                    doGet.addStatement("$T<$T, $T> source = protoBuilder.get$LMap()",
                            Map.class, keyType.box(), protoValueType.box(), versionJavaName);
                    doGet.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), valueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    doGet.addStatement("source.forEach((k, v) -> result.put(k, v.longValue()))");
                    doGet.addStatement("return result");
                }
                // INT_ENUM: convert Map<K, EnumType> to Map<K, Integer>
                else if (field.getMapValueConflictType() == MergedField.ConflictType.INT_ENUM
                        && versionMapInfo.hasEnumValue()) {
                    doGet.addStatement("$T<$T, ?> source = protoBuilder.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    doGet.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), valueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    doGet.addStatement("source.forEach((k, v) -> result.put(k, (($T) v).getNumber()))",
                            ClassName.get("com.google.protobuf", "ProtocolMessageEnum"));
                    doGet.addStatement("return result");
                }
                else {
                    // Default: direct return
                    doGet.addStatement("return protoBuilder.get$LMap()", versionJavaName);
                }
            } else {
                // No type conflict - check for WKT conversion
                Optional<WellKnownTypeInfo> wktOpt = MapTypeResolver.getWktForMapValue(versionMapInfo, ctx);
                if (wktOpt.isPresent()) {
                    // WKT map value - need to convert proto WKT to Java type
                    WellKnownTypeInfo wkt = wktOpt.get();
                    doGet.addStatement("$T<$T, ?> source = protoBuilder.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    doGet.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), valueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    String conversionCode = MapWktConverter.generateWktMapValueExtraction(wkt);
                    doGet.addStatement("source.forEach((k, v) -> result.put(k, $L))", conversionCode);
                    doGet.addStatement("return result");
                } else if (versionMapInfo.hasMessageValue()) {
                    // Message map value - wrap each value with impl wrapper class
                    String wrapperClass = MapTypeNameProvider.getMapValueWrapperClass(versionMapInfo, ctx);
                    String protoType = MapTypeNameProvider.getMapValueProtoType(versionMapInfo, ctx);
                    doGet.addStatement("$T<$T, ?> source = protoBuilder.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    doGet.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), valueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    doGet.addStatement("source.forEach((k, v) -> result.put(k, new $L(($L) v)))", wrapperClass, protoType);
                    doGet.addStatement("return result");
                } else if (versionMapInfo.hasEnumValue()) {
                    // Enum map value - convert each value with fromProtoValue
                    String enumType = MapTypeNameProvider.getMapValueEnumType(versionMapInfo, ctx);
                    doGet.addStatement("$T<$T, ?> source = protoBuilder.get$LMap()",
                            Map.class, keyType.box(), versionJavaName);
                    doGet.addStatement("$T<$T, $T> result = new $T<>(source.size())",
                            Map.class, keyType.box(), valueType.box(),
                            ClassName.get("java.util", "LinkedHashMap"));
                    doGet.addStatement("source.forEach((k, v) -> result.put(k, $L.fromProtoValue((($T) v).getNumber())))",
                            enumType, ClassName.get("com.google.protobuf", "ProtocolMessageEnum"));
                    doGet.addStatement("return result");
                } else {
                    doGet.addStatement("return protoBuilder.get$LMap()", versionJavaName);
                }
            }
        } else {
            doGet.addStatement("return $T.emptyMap()", Collections.class);
        }
        builder.addMethod(doGet.build());
    }

    // Type name methods delegated to MapTypeNameProvider
}
