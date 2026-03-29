package io.alnovis.protowrapper.ircraft.passes;

import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.lowering.ProtoAttributes;
import io.alnovis.ircraft.dialect.semantic.ops.*;
import io.alnovis.ircraft.java.*;

import java.util.ArrayList;

/**
 * Generates ProtoWrapper base interface and updates message interfaces to extend it.
 *
 * @since 3.0.0
 */
public class ProtoWrapperPassJava implements Pass {

    @Override
    public String name() {
        return "proto-wrapper-interface";
    }

    @Override
    public String description() {
        return "Generates ProtoWrapper base interface and updates message interfaces to extend it";
    }

    @Override
    public PassResult run(IrModule module, PassContext context) {
        String apiPackage = IR.findApiPackage(module);

        // Build ProtoWrapper interface
        var protoWrapper = Ops.iface("ProtoWrapper")
                .javadoc("Base interface for all proto wrapper types.")
                .addMethod(Ops.method("getTypedProto", Types.named("com.google.protobuf.Message"))
                        .abstractPublic().javadoc("Returns the underlying proto message.").build())
                .addMethod(Ops.method("getWrapperVersionId", Types.STRING())
                        .abstractPublic().javadoc("Returns the version identifier (e.g., \"v1\", \"v2\").").build())
                .addMethod(Ops.method("toBytes", Types.BYTES())
                        .abstractPublic().javadoc("Serializes the underlying proto to bytes.").build())
                .build();

        var wrapperFile = Ops.file(apiPackage).addType(protoWrapper).build();

        // Update message interfaces to extend ProtoWrapper
        var updatedTopLevel = new ArrayList<Operation>();
        for (var op : IR.topLevel(module)) {
            if (op instanceof FileOp file) {
                var updatedTypes = new ArrayList<Operation>();
                for (var t : IR.types(file)) {
                    if (t instanceof InterfaceOp iface
                            && !iface.name().equals("ProtoWrapper")
                            && iface.attributes().contains(ProtoAttributes.PresentInVersions())) {
                        updatedTypes.add(Ops.rebuildIface(iface)
                                .addExtends(Types.named("ProtoWrapper"))
                                .build());
                    } else {
                        updatedTypes.add(t);
                    }
                }
                updatedTopLevel.add(Ops.file(file.packageName())
                        .attributes(file.attributes())
                        .types(updatedTypes)
                        .build());
            } else {
                updatedTopLevel.add(op);
            }
        }

        updatedTopLevel.add(wrapperFile);
        return IR.passResult(IR.module(module.name(), updatedTopLevel));
    }
}
