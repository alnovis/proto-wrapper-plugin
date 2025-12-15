package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedSchema.MergedField;
import space.alnovis.protowrapper.model.MergedSchema.MergedMessage;
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

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .build();
        generator = new InterfaceGenerator(config);
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
        message.addField(new MergedField(new FieldInfo(billsProto), "v1"));

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(new MergedField(new FieldInfo(coinsProto), "v1"));

        // Generate
        JavaFile javaFile = generator.generate(message);

        String code = javaFile.toString();

        // Verify
        assertThat(code).contains("public interface Money");
        assertThat(code).contains("long getBills()");
        assertThat(code).contains("int getCoins()");
        assertThat(code).contains("int getWrapperVersion()");
        assertThat(code).contains("byte[] toBytes()");
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
        message.addField(new MergedField(new FieldInfo(activeProto), "v1"));

        JavaFile javaFile = generator.generate(message);
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
        message.addField(new MergedField(new FieldInfo(itemsProto), "v1"));

        JavaFile javaFile = generator.generate(message);
        String code = javaFile.toString();

        assertThat(code).contains("List<Item> getItems()");
    }
}
