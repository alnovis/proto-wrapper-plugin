package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.ProtobufConstants.MESSAGE_CLASS;

/**
 * Component that generates wrap and parse methods for VersionContext interface.
 *
 * <p>Generates for each message:</p>
 * <ul>
 *   <li>wrapXxx(Message) - wrap a proto message</li>
 *   <li>parseXxxFromBytes(byte[]) - parse bytes and wrap</li>
 * </ul>
 */
public class WrapMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new WrapMethodsComponent.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public WrapMethodsComponent(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        ClassName invalidProtocolBufferException = ClassName.get(
                "com.google.protobuf", "InvalidProtocolBufferException");

        for (MergedMessage message : schema.getMessages()) {
            ClassName returnType = ClassName.get(config.getApiPackage(), message.getInterfaceName());

            // Check if message exists in all versions
            boolean existsInAllVersions = message.getPresentInVersions().containsAll(schema.getVersions());

            // wrapXxx(Message) method
            builder.addMethod(createWrapMethod(message, returnType, existsInAllVersions));

            // parseXxxFromBytes(byte[]) method
            builder.addMethod(createParseFromBytesMethod(message, returnType,
                    invalidProtocolBufferException, existsInAllVersions));
        }
    }

    private MethodSpec createWrapMethod(MergedMessage message, ClassName returnType,
                                         boolean existsInAllVersions) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("wrap" + message.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(MESSAGE_CLASS, "proto")
                .addJavadoc("Wrap a proto $L message.\n", message.getName())
                .addJavadoc("@param proto Proto message\n")
                .addJavadoc("@return Wrapped $L, or null if proto is null\n", message.getName());

        applyVersionAvailability(methodBuilder, message, existsInAllVersions);
        return methodBuilder.build();
    }

    private MethodSpec createParseFromBytesMethod(MergedMessage message, ClassName returnType,
                                                   ClassName exceptionType, boolean existsInAllVersions) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("parse" + message.getName() + "FromBytes")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                .addException(exceptionType)
                .addJavadoc("Parse bytes and wrap as $L.\n", message.getName())
                .addJavadoc("@param bytes Protobuf-encoded bytes\n")
                .addJavadoc("@return Wrapped $L, or null if bytes is null\n", message.getName())
                .addJavadoc("@throws InvalidProtocolBufferException if bytes cannot be parsed\n");

        applyVersionAvailability(methodBuilder, message, existsInAllVersions);
        return methodBuilder.build();
    }

    /**
     * Apply abstract or default modifier based on version availability.
     */
    private void applyVersionAvailability(MethodSpec.Builder methodBuilder, MergedMessage message,
                                           boolean existsInAllVersions) {
        if (existsInAllVersions) {
            methodBuilder.addModifiers(Modifier.ABSTRACT);
        } else {
            methodBuilder.addModifiers(Modifier.DEFAULT);
            methodBuilder.addJavadoc("@apiNote Present only in versions: $L\n", message.getPresentInVersions());
            methodBuilder.addStatement("throw new $T($S + $S)",
                    UnsupportedOperationException.class,
                    message.getName() + " is not available in this version. Present in: ",
                    message.getPresentInVersions().toString());
        }
    }
}
