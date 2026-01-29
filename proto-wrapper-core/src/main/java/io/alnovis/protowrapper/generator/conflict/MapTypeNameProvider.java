package io.alnovis.protowrapper.generator.conflict;

import io.alnovis.protowrapper.model.MapInfo;

/**
 * Utility class for providing type names in map field processing.
 *
 * <p>This class consolidates type name generation logic that was previously
 * in MapFieldHandler to improve separation of concerns.</p>
 *
 * <p>All methods are static and thread-safe.</p>
 *
 * @since 2.3.1
 * @see MapFieldHandler
 */
public final class MapTypeNameProvider {

    private MapTypeNameProvider() {
        // Utility class - no instantiation
    }

    /**
     * Get the wrapper class name for a map value of message type.
     * Returns the fully qualified implementation class name.
     *
     * @param mapInfo Map field information with message value
     * @param ctx Processing context with package information
     * @return Fully qualified wrapper class name (e.g., "com.example.impl.v1.NestedMessage")
     */
    static String getMapValueWrapperClass(MapInfo mapInfo, ProcessingContext ctx) {
        String simpleTypeName = mapInfo.getSimpleValueTypeName();
        String implPackage = ctx.implPackage();
        return implPackage + "." + simpleTypeName;
    }

    /**
     * Get the enum type name for a map value of enum type.
     * Returns the fully qualified API enum class name.
     *
     * @param mapInfo Map field information with enum value
     * @param ctx Processing context with package information
     * @return Fully qualified API enum class name (e.g., "com.example.api.TestEnum")
     */
    static String getMapValueEnumType(MapInfo mapInfo, ProcessingContext ctx) {
        String simpleTypeName = mapInfo.getSimpleValueTypeName();
        String apiPackage = ctx.apiPackage();
        return apiPackage + "." + simpleTypeName;
    }

    /**
     * Get the proto message type name for a map value of message type.
     * Returns the fully qualified proto class name including outer class.
     *
     * @param mapInfo Map field information with message value
     * @param ctx Processing context with package information
     * @return Fully qualified proto class name (e.g., "com.example.proto.v1.OuterClass.NestedMessage")
     */
    static String getMapValueProtoType(MapInfo mapInfo, ProcessingContext ctx) {
        String valueTypeName = mapInfo.getValueTypeName();
        if (valueTypeName == null || valueTypeName.isEmpty()) {
            return "com.google.protobuf.Message";
        }

        // Extract simple type name (e.g., "NestedMessage" from "io.alnovis...NestedMessage")
        String simpleTypeName = mapInfo.getSimpleValueTypeName();

        // Get Java proto package for this version
        String version = ctx.requireVersion();
        String javaProtoPackage = ctx.config().getProtoPackage(version);

        // Try to find outer class using schema
        String outerClassName = CodeGenerationHelper.findOuterClassForType(
                simpleTypeName, ctx.schema(), version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + simpleTypeName;
        }

        // Fallback: use simple type name directly
        return javaProtoPackage + "." + simpleTypeName;
    }

    /**
     * Get the proto enum type name for a map value of enum type.
     * Returns the fully qualified proto enum class name including outer class.
     *
     * @param mapInfo Map field information with enum value
     * @param ctx Processing context with package information
     * @return Fully qualified proto enum class name (e.g., "com.example.proto.v1.OuterClass.TestEnum")
     */
    static String getMapValueProtoEnumType(MapInfo mapInfo, ProcessingContext ctx) {
        String valueTypeName = mapInfo.getValueTypeName();
        if (valueTypeName == null || valueTypeName.isEmpty()) {
            return "Object";
        }

        // Extract simple type name (e.g., "TestEnum" from "io.alnovis...TestEnum")
        String simpleTypeName = mapInfo.getSimpleValueTypeName();

        // Get Java proto package for this version
        String version = ctx.requireVersion();
        String javaProtoPackage = ctx.config().getProtoPackage(version);

        // Try to find outer class using schema
        String outerClassName = CodeGenerationHelper.findOuterClassForEnum(
                simpleTypeName, ctx.schema(), version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + simpleTypeName;
        }

        // Fallback: use simple type name directly
        return javaProtoPackage + "." + simpleTypeName;
    }
}
