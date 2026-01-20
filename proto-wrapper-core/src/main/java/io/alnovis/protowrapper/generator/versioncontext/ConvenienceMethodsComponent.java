package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;

/**
 * Component that generates convenience methods for VersionContext interface.
 *
 * <p>Generates convenience methods for specific message types when applicable:</p>
 * <ul>
 *   <li>zeroMoney() - create Money with zero value</li>
 *   <li>createMoney(long, int) - create Money with bills and coins</li>
 * </ul>
 *
 * <p>Only generated when builders are enabled and Money message has
 * bills and coins fields.</p>
 */
public class ConvenienceMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new ConvenienceMethodsComponent.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public ConvenienceMethodsComponent(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        if (!config.isGenerateBuilders()) {
            return;
        }

        // Add Money convenience methods if Money message exists with bills/coins
        schema.getMessages().stream()
                .filter(m -> m.getName().equals("Money"))
                .findFirst()
                .ifPresent(money -> addMoneyMethods(builder, money));
    }

    private void addMoneyMethods(TypeSpec.Builder builder, MergedMessage money) {
        boolean hasBills = money.getFields().stream().anyMatch(f -> f.getName().equals("bills"));
        boolean hasCoins = money.getFields().stream().anyMatch(f -> f.getName().equals("coins"));

        if (!hasBills || !hasCoins) {
            return;
        }

        ClassName moneyType = ClassName.get(config.getApiPackage(), "Money");

        // zeroMoney()
        builder.addMethod(MethodSpec.methodBuilder("zeroMoney")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(moneyType)
                .addJavadoc("Create a Money with zero value.\n")
                .addJavadoc("@return Money with bills=0 and coins=0\n")
                .addStatement("return newMoneyBuilder().setBills(0L).setCoins(0).build()")
                .build());

        // createMoney(long bills, int coins)
        builder.addMethod(MethodSpec.methodBuilder("createMoney")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(moneyType)
                .addParameter(TypeName.LONG, "bills")
                .addParameter(TypeName.INT, "coins")
                .addJavadoc("Create a Money with specified values.\n")
                .addJavadoc("@param bills Number of bills\n")
                .addJavadoc("@param coins Number of coins\n")
                .addJavadoc("@return Money instance\n")
                .addStatement("return newMoneyBuilder().setBills(bills).setCoins(coins).build()")
                .build());
    }
}
