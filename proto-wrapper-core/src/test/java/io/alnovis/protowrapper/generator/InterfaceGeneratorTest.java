package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.assertj.core.api.Assertions.assertThat;

class InterfaceGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private InterfaceGenerator generator;
    private MergedSchema schema;
    private GenerationContext ctx;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .build();
        generator = new InterfaceGenerator(config);
        schema = new MergedSchema(Arrays.asList("v1", "v2"));
        ctx = GenerationContext.create(schema, config);
    }

    @Test
    void shouldGenerateSimpleInterface() {
        // Create a simple Money message
        MergedMessage message = new MergedMessage("Money");
        message.addVersion("v1");
        message.addVersion("v2");

        // Add fields
        FieldDescriptorProto billsProto = FieldDescriptorProto.newBuilder()
                .setName("bills")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(billsProto)).build());

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(coinsProto)).build());

        // Generate
        JavaFile javaFile = generator.generate(message, ctx);

        String code = javaFile.toString();

        // Verify
        assertThat(code).contains("public interface Money extends ProtoWrapper");
        assertThat(code).contains("long getBills()");
        assertThat(code).contains("int getCoins()");
        // getWrapperVersion() and toBytes() are now inherited from ProtoWrapper
    }

    @Test
    void shouldGenerateOptionalFieldWithHasMethod() {
        MergedMessage message = new MergedMessage("State");
        message.addVersion("v1");

        FieldDescriptorProto activeProto = FieldDescriptorProto.newBuilder()
                .setName("active")
                .setNumber(1)
                .setType(Type.TYPE_BOOL)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(activeProto)).build());

        JavaFile javaFile = generator.generate(message, ctx);
        String code = javaFile.toString();

        assertThat(code).contains("Boolean isActive()");
        assertThat(code).contains("boolean hasActive()");
    }

    @Test
    void shouldGenerateListField() {
        MergedMessage message = new MergedMessage("Request");
        message.addVersion("v1");

        FieldDescriptorProto itemsProto = FieldDescriptorProto.newBuilder()
                .setName("items")
                .setNumber(1)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".org.example.Item")
                .setLabel(Label.LABEL_REPEATED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(itemsProto)).build());

        JavaFile javaFile = generator.generate(message, ctx);
        String code = javaFile.toString();

        assertThat(code).contains("List<Item> getItems()");
    }

    @Test
    void shouldGenerateParseFromBytesMethod() {
        MergedMessage message = new MergedMessage("Money");
        message.addVersion("v1");
        message.addVersion("v2");

        FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                .setName("amount")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(amountProto)).build());

        JavaFile javaFile = generator.generate(message, ctx);
        String code = javaFile.toString();

        // Verify parseFromBytes static method is generated
        assertThat(code).contains("static Money parseFromBytes(VersionContext ctx, byte[] bytes)");
        assertThat(code).contains("throws InvalidProtocolBufferException");
        assertThat(code).contains("return ctx.parseMoneyFromBytes(bytes)");
        // Verify JavaDoc
        assertThat(code).contains("Parse bytes into a Money using the specified version context");
        assertThat(code).contains("@param ctx Version context determining the protocol version");
        assertThat(code).contains("@param bytes Serialized protobuf data");
    }

    @Test
    void shouldGenerateParseFromBytesEvenWithoutBuilders() {
        // Create config without builders
        GeneratorConfig configNoBuilders = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .generateBuilders(false)
                .build();
        InterfaceGenerator generatorNoBuilders = new InterfaceGenerator(configNoBuilders);
        MergedSchema schemaNoBuilders = new MergedSchema(Arrays.asList("v1"));
        GenerationContext ctxNoBuilders = GenerationContext.create(schemaNoBuilders, configNoBuilders);

        MergedMessage message = new MergedMessage("Order");
        message.addVersion("v1");

        FieldDescriptorProto idProto = FieldDescriptorProto.newBuilder()
                .setName("id")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(idProto)).build());

        JavaFile javaFile = generatorNoBuilders.generate(message, ctxNoBuilders);
        String code = javaFile.toString();

        // parseFromBytes should still be generated
        assertThat(code).contains("static Order parseFromBytes(VersionContext ctx, byte[] bytes)");
        assertThat(code).contains("return ctx.parseOrderFromBytes(bytes)");
        // But newBuilder should NOT be generated
        assertThat(code).doesNotContain("static Builder newBuilder");
    }
}
