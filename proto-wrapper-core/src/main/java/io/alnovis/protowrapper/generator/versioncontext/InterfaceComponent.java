package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.TypeSpec;

/**
 * Component that contributes to VersionContext interface generation.
 *
 * <p>Each component is responsible for a specific aspect of the interface,
 * following the Single Responsibility Principle.</p>
 */
@FunctionalInterface
public interface InterfaceComponent {

    /**
     * Add this component's contributions to the interface builder.
     *
     * @param builder the interface builder to add to
     */
    void addTo(TypeSpec.Builder builder);
}
