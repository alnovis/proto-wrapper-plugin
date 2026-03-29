package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.List;
import java.util.Set;

/**
 * Generates ProtocolVersions utility class with version constants.
 *
 * @since 3.0.0
 */
public class ProtocolVersionsPassJava implements Pass {

    @Override public String name() { return "protocol-versions"; }
    @Override public String description() { return "Generates ProtocolVersions utility class with version constants"; }
    @Override public boolean isEnabled(PassContext context) { return !context.getBool("skipProtocolVersions"); }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        var interfaces = IR.collect(module, InterfaceOp.class);
        if (interfaces.isEmpty()) return IR.passResult(module);

        var first = interfaces.get(0);
        var versionsOpt = first.attributes().getStringList(ProtoAttributes.SchemaVersions());
        if (versionsOpt.isEmpty()) return IR.passResult(module);

        var versions = scala.jdk.CollectionConverters.SeqHasAsJava(versionsOpt.get()).asJava();
        if (versions.isEmpty()) return IR.passResult(module);

        String apiPackage = IR.findApiPackage(module);

        var builder = Ops.cls("ProtocolVersions")
                .modifiers(Set.of(IR.PUBLIC(), IR.FINAL()));

        for (var v : versions) {
            builder.addField(Ops.staticFinalField(v.toUpperCase(), Types.STRING(), v));
        }

        builder.addField(Ops.staticFinalField("DEFAULT", Types.STRING(), versions.get(versions.size() - 1)));
        builder.addConstructor(Ops.privateConstructor());

        var file = Ops.file(apiPackage).addType(builder.build()).build();
        return IR.passResult(IR.appendTopLevel(module, List.of(file)));
    }
}
