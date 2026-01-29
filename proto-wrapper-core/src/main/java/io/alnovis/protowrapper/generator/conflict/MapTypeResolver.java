package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MapInfo;
import io.alnovis.protowrapper.model.MergedField;

import java.util.Optional;

import static io.alnovis.protowrapper.generator.TypeUtils.*;

/**
 * Utility class for resolving types in map field processing.
 *
 * <p>This class consolidates type resolution logic that was previously
 * in MapFieldHandler to improve separation of concerns.</p>
 *
 * <p>All methods are static and thread-safe.</p>
 *
 * @since 2.3.1
 * @see MapFieldHandler
 */
public final class MapTypeResolver {

    private MapTypeResolver() {
        // Utility class - no instantiation
    }

    /**
     * Get the proto value type for a map field, handling enum types correctly.
     * For enums, constructs the full class name using the proto outer class.
     *
     * @param mapInfo the map info
     * @param ctx the processing context
     * @return the proto value type
     */
    static TypeName getProtoValueType(MapInfo mapInfo, ProcessingContext ctx) {
        if (mapInfo.hasEnumValue()) {
            // For enum types, construct full class name: OuterClass.EnumName
            ClassName protoClass = ctx.protoClassName();
            String outerClass = protoClass.simpleNames().get(0);
            String enumName = mapInfo.getSimpleValueTypeName();
            return ClassName.get(protoClass.packageName(), outerClass, enumName);
        }
        return parseSimpleType(mapInfo.getValueJavaType());
    }

    /**
     * Get the appropriate map type, considering map value conflicts and WKT.
     * If there's a WIDENING or INT_ENUM conflict in map values, uses the resolved type.
     * If convertWellKnownTypes is enabled, converts WKT values to Java types.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the resolved map type
     */
    static TypeName getResolvedMapType(MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        if (field.hasMapValueConflict() && field.getResolvedMapValueType() != null) {
            return createMapTypeWithResolvedValue(mapInfo, field.getResolvedMapValueType());
        }
        // Check for WKT map values
        boolean convertWkt = ctx.config() != null && ctx.config().isConvertWellKnownTypes();
        return createMapTypeWithWkt(mapInfo, convertWkt);
    }

    /**
     * Get the appropriate value type, considering map value conflicts and WKT.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the resolved value type
     */
    static TypeName getResolvedValueType(MergedField field, ProcessingContext ctx) {
        MapInfo mapInfo = field.getMapInfo();
        if (field.hasMapValueConflict() && field.getResolvedMapValueType() != null) {
            return parseSimpleType(field.getResolvedMapValueType());
        }
        // Check for WKT map values
        boolean convertWkt = ctx.config() != null && ctx.config().isConvertWellKnownTypes();
        return parseMapValueTypeWithWkt(mapInfo, convertWkt);
    }

    /**
     * Check if map value is a Well-Known Type that should be converted.
     *
     * @param mapInfo the map info
     * @param ctx the processing context
     * @return true if map value is a WKT and conversion is enabled
     */
    static boolean isWktMapValue(MapInfo mapInfo, ProcessingContext ctx) {
        if (ctx.config() == null || !ctx.config().isConvertWellKnownTypes()) {
            return false;
        }
        return isMapValueWellKnownType(mapInfo);
    }

    /**
     * Get WellKnownTypeInfo for map value if applicable.
     *
     * @param mapInfo the map info
     * @param ctx the processing context
     * @return optional containing WKT info if applicable
     */
    static Optional<WellKnownTypeInfo> getWktForMapValue(MapInfo mapInfo, ProcessingContext ctx) {
        if (!isWktMapValue(mapInfo, ctx)) {
            return Optional.empty();
        }
        return getMapValueWellKnownType(mapInfo);
    }

    /**
     * Get version-specific MapInfo for the current processing context.
     * Falls back to unified MapInfo if version-specific is not available.
     *
     * @param field the merged field
     * @param unifiedMapInfo the unified map info from merged field
     * @param ctx the processing context with version info
     * @return version-specific MapInfo or unified MapInfo as fallback
     */
    static MapInfo getVersionSpecificMapInfo(MergedField field, MapInfo unifiedMapInfo, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField != null) {
            MapInfo versionMapInfo = versionField.getMapInfo();
            if (versionMapInfo != null) {
                return versionMapInfo;
            }
        }
        return unifiedMapInfo;
    }

    /**
     * Get the cache field name for a map field.
     *
     * @param field the merged field
     * @return the cache field name
     */
    static String getCacheFieldName(MergedField field) {
        return "cached" + capitalize(field.getJavaName()) + "Map";
    }

    /**
     * Capitalize the first letter of a string.
     *
     * @param s the string to capitalize
     * @return the capitalized string
     */
    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Check if this map field needs type conversion and thus can benefit from caching.
     *
     * @param field the merged field
     * @param versionMapInfo the version-specific map info
     * @param resolvedValueType the resolved value type
     * @return true if type conversion is needed
     */
    static boolean needsTypeConversion(MergedField field, MapInfo versionMapInfo, TypeName resolvedValueType) {
        if (!field.hasMapValueConflict()) {
            return false;
        }
        TypeName protoValueType = parseSimpleType(versionMapInfo.getValueJavaType());
        if (resolvedValueType.equals(protoValueType)) {
            return false;
        }
        // WIDENING: int -> long conversion
        if (field.getMapValueConflictType() == MergedField.ConflictType.WIDENING
                && protoValueType.equals(TypeName.INT)) {
            return true;
        }
        // INT_ENUM: enum -> int conversion
        return field.getMapValueConflictType() == MergedField.ConflictType.INT_ENUM
                && versionMapInfo.hasEnumValue();
    }
}
