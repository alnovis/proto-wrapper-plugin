package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;

/**
 * Base interface for all code generators.
 *
 * <p>Provides common contract for generators that produce Java source files
 * from merged protobuf schemas.</p>
 *
 * @param <T> The input type for generation (e.g., MergedMessage, MergedEnum)
 */
public interface CodeGenerator<T> {

    /**
     * Get the generator configuration.
     *
     * @return GeneratorConfig used by this generator
     */
    GeneratorConfig getConfig();

    /**
     * Write a generated JavaFile to the output directory.
     *
     * @param javaFile The file to write
     * @throws IOException if writing fails
     */
    void writeToFile(JavaFile javaFile) throws IOException;
}
