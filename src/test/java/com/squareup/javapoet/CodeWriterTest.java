package com.squareup.javapoet;

import org.junit.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * is a Java class that tests the functionality of the CodeWriter class. The test
 * method, emptyLineInJavaDocDosEndings, takes a CodeBlock object as input and emits
 * Javadoc comments using the CodeWriter class. The resulting output is then compared
 * to a expected output using the assertThat method.
 */
public class CodeWriterTest {

    /**
     * tests whether a single empty line between two lines of Javadoc code is preserved
     * in the output Javadoc document.
     */
    @Test
    public void emptyLineInJavaDocDosEndings() throws IOException {
        CodeBlock javadocCodeBlock = CodeBlock.of("A\r\n\r\nB\r\n");
        StringBuilder out = new StringBuilder();
        new CodeWriter(out).emitJavadoc(javadocCodeBlock);
        assertThat(out.toString()).isEqualTo(
                "/**\n" +
                        " * A\n" +
                        " *\n" +
                        " * B\n" +
                        " */\n");
    }
}