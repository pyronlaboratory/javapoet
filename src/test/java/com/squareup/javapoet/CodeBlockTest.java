/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * tests various scenarios involving the `CodeBlock` class in Android Studio. It
 * includes testing for invalid format strings, missing format types, and too many
 * statement enters. The test class also covers joining multiple code blocks with
 * different formats, clearing a code block, and more. The tests are designed to
 * ensure that the CodeBlock class functions correctly and provides accurate results
 * when used in various situations.
 */
public final class CodeBlockTest {
  /**
   * tests the equality and hash code consistency of `CodeBlock` objects. It compares
   * two objects, checks if they are equal using the `equals()` method, and verifies
   * that their hash codes are the same using the `hashCode()` method.
   */
  @Test public void equalsAndHashCode() {
    CodeBlock a = CodeBlock.builder().build();
    CodeBlock b = CodeBlock.builder().build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = CodeBlock.builder().add("$L", "taco").build();
    b = CodeBlock.builder().add("$L", "taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  /**
   * creates a new `CodeBlock` object representing the string "delicious taco". The
   * resulting code block can be used for further manipulation or evaluation.
   */
  @Test public void of() {
    CodeBlock a = CodeBlock.of("$L taco", "delicious");
    assertThat(a.toString()).isEqualTo("delicious taco");
  }

  /**
   * tests whether a `CodeBlock` is empty or not by asserting if its size is zero or
   * if it contains only whitespace characters.
   */
  @Test public void isEmpty() {
    assertTrue(CodeBlock.builder().isEmpty());
    assertTrue(CodeBlock.builder().add("").isEmpty());
    assertFalse(CodeBlock.builder().add(" ").isEmpty());
  }

  /**
   * tests whether indexing a code block with invalid characters is thrown an `IllegalArgumentException`.
   */
  @Test public void indentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1>", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  /**
   * tests whether indented code can be indexed using the `CodeBlock.builder()` method.
   * It fails if any indentation is attempted, indicating that indices are not allowed
   * for indented code.
   */
  @Test public void deindentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1<", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  /**
   * tests whether attempting to index a dollar sign escape sequence with a number
   * results in an `IllegalArgumentException`.
   */
  @Test public void dollarSignEscapeCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1$", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  /**
   * checks if an illegal argument is raised when attempting to build a code block with
   * an indexed statement beginning with `$`.
   */
  @Test public void statementBeginningCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1[", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  /**
   * checks if an illegal argument exception is thrown when attempting to create a code
   * block with an invalid indexing notation.
   */
  @Test public void statementEndingCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1]", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  /**
   * tests whether a format string can be indexed to extract a variable value.
   */
  @Test public void nameFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1N", "taco").build();
    assertThat(block.toString()).isEqualTo("taco");
  }

  /**
   * tests whether a literal string can be indexed using the `$1L` placeholder, resulting
   * in the actual value being returned.
   */
  @Test public void literalFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1L", "taco").build();
    assertThat(block.toString()).isEqualTo("taco");
  }

  /**
   * tests whether a string can be indexed using dollar-formatting syntax, with the
   * result being a single character.
   */
  @Test public void stringFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1S", "taco").build();
    assertThat(block.toString()).isEqualTo("\"taco\"");
  }

  /**
   * tests whether a `CodeBlock` can be indexed using `$1T`. The function successfully
   * asserts that the output is equal to `java.lang.String`.
   */
  @Test public void typeFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1T", String.class).build();
    assertThat(block.toString()).isEqualTo("java.lang.String");
  }

  /**
   * takes a `Map<String, Object>` and converts it into a `CodeBlock` with a named
   * argument called `$text:S`. The resulting `CodeBlock` string is equal to the value
   * of the `text` key in the map.
   */
  @Test public void simpleNamedArgument() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "taco");
    CodeBlock block = CodeBlock.builder().addNamed("$text:S", map).build();
    assertThat(block.toString()).isEqualTo("\"taco\"");
  }

  /**
   * takes a map with a single key-value pair, where the key is `"text"` and the value
   * is a string `"tacos"`. The function then uses a code block to concatenate the value
   * of the `$text:S` variable with the text `"Do you like "`, followed by another
   * concatenation with the `$text:S` variable again. Finally, the function asserts
   * that the resulting string is equal to the expected output.
   */
  @Test public void repeatedNamedArgument() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "tacos");
    CodeBlock block = CodeBlock.builder()
        .addNamed("\"I like \" + $text:S + \". Do you like \" + $text:S + \"?\"", map)
        .build();
    assertThat(block.toString()).isEqualTo(
        "\"I like \" + \"tacos\" + \". Do you like \" + \"tacos\" + \"?\"");
  }

  /**
   * generates a code block with a named argument and no arguments, resulting in a
   * string representation of "tacos for $3.50".
   */
  @Test public void namedAndNoArgFormat() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "tacos");
    CodeBlock block = CodeBlock.builder()
        .addNamed("$>\n$text:L for $$3.50", map).build();
    assertThat(block.toString()).isEqualTo("\n  tacos for $3.50");
  }

  /**
   * tests whether an IllegalArgumentException is thrown when a named argument is missing
   * from a `CodeBlock`.
   */
  @Test public void missingNamedArgument() {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      CodeBlock.builder().addNamed("$text:S", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Missing named argument for $text");
    }
  }

  /**
   * tests whether an exception is thrown when passing a non-lowercase argument to a
   * named code block.
   */
  @Test public void lowerCaseNamed() {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("Text", "tacos");
      CodeBlock block = CodeBlock.builder().addNamed("$Text:S", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("argument 'Text' must start with a lowercase character");
    }
  }

  /**
   * maps named arguments to a Java `CodeBlock`, which prints the provided message to
   * the console using `System.out.println()`.
   */
  @Test public void multipleNamedArguments() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("pipe", System.class);
    map.put("text", "tacos");

    CodeBlock block = CodeBlock.builder()
        .addNamed("$pipe:T.out.println(\"Let's eat some $text:L\");", map)
        .build();

    assertThat(block.toString()).isEqualTo(
        "java.lang.System.out.println(\"Let's eat some tacos\");");
  }

  /**
   * takes a `Map<String, Object>` and creates a `CodeBlock` object with a named new
   * line comment containing the key-value pair "clazz" with the value `T`. The resulting
   * `CodeBlock` string is asserted to be equal to "java.lang.Integer\n".
   */
  @Test public void namedNewline() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("clazz", Integer.class);
    CodeBlock block = CodeBlock.builder().addNamed("$clazz:T\n", map).build();
    assertThat(block.toString()).isEqualTo("java.lang.Integer\n");
  }

  /**
   * tests whether an illegal argument exception is thrown when adding a named parameter
   * with a dangling `$` symbol at the end of the name.
   */
  @Test public void danglingNamed() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("clazz", Integer.class);
    try {
      CodeBlock.builder().addNamed("$clazz:T$", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling $ at end");
    }
  }

  /**
   * tests whether an illegal argument exception is thrown when passing more than one
   * argument to a code block with index 2.
   */
  @Test public void indexTooHigh() {
    try {
      CodeBlock.builder().add("$2T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 2 for '$2T' not in range (received 1 arguments)");
    }
  }

  /**
   * verifies that an illegal argument exception is thrown when passing a non-zero index
   * to the `$0T` builder.
   */
  @Test public void indexIsZero() {
    try {
      CodeBlock.builder().add("$0T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 0 for '$0T' not in range (received 1 arguments)");
    }
  }

  /**
   * tests whether an index is negative by attempting to create a code block with a
   * negative index and checking if an `IllegalArgumentException` is thrown when building
   * the code block.
   */
  @Test public void indexIsNegative() {
    try {
      CodeBlock.builder().add("$-1T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$-1T'");
    }
  }

  /**
   * tests if an illegal argument exception is thrown when dangling format characters
   * are encountered in a code block builder.
   */
  @Test public void indexWithoutFormatType() {
    try {
      CodeBlock.builder().add("$1", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling format characters in '$1'");
    }
  }

  /**
   * tests whether an illegal argument exception is thrown when an invalid format string
   * is passed to the `CodeBlock.builder().add()` method without the `String.class`
   * type at the end.
   */
  @Test public void indexWithoutFormatTypeNotAtStringEnd() {
    try {
      CodeBlock.builder().add("$1 taco", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$1 taco'");
    }
  }

  /**
   * tests whether an illegal argument exception is thrown when a code block with index
   * 1 is created without any arguments.
   */
  @Test public void indexButNoArguments() {
    try {
      CodeBlock.builder().add("$1T").build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 1 for '$1T' not in range (received 0 arguments)");
    }
  }

  /**
   * tests whether an attempt to use a dangling format character ($) without any
   * formatting arguments leads to an `IllegalArgumentException`.
   */
  @Test public void formatIndicatorAlone() {
    try {
      CodeBlock.builder().add("$", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling format characters in '$'");
    }
  }

  /**
   * tests whether an attempt to use a invalid format string will throw an `IllegalArgumentException`.
   */
  @Test public void formatIndicatorWithoutIndexOrFormatType() {
    try {
      CodeBlock.builder().add("$ tacoString", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$ tacoString'");
    }
  }

  /**
   * tests whether the same index can be used with different formats.
   */
  @Test public void sameIndexCanBeUsedWithDifferentFormats() {
    CodeBlock block = CodeBlock.builder()
        .add("$1T.out.println($1S)", ClassName.get(System.class))
        .build();
    assertThat(block.toString()).isEqualTo("java.lang.System.out.println(\"java.lang.System\")");
  }

  /**
   * tests whether an illegal state exception is thrown when too many statement enters
   * are present in a code block.
   */
  @Test public void tooManyStatementEnters() {
    CodeBlock codeBlock = CodeBlock.builder().add("$[$[").build();
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("statement enter $[ followed by statement enter $[");
    }
  }

  /**
   * tests whether an code block can be executed without a corresponding `statement
   * Enter` statement.
   */
  @Test public void statementExitWithoutStatementEnter() {
    CodeBlock codeBlock = CodeBlock.builder().add("$]").build();
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("statement exit $] has no matching statement enter $[");
    }
  }

  /**
   * takes a list of `CodeBlock` objects and combines them using the specified concatenation
   * operator, resulting in a new `CodeBlock` object representing the joined code.
   */
  @Test public void join() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = CodeBlock.join(codeBlocks, " || ");
    assertThat(joined.toString()).isEqualTo("\"hello\" || world.World || need tacos");
  }

  /**
   * takes a list of `CodeBlock` objects and returns a new `CodeBlock` object that
   * represents the concatenation of the elements in the list using the "||" operator.
   */
  @Test public void joining() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
    assertThat(joined.toString()).isEqualTo("\"hello\" || world.World || need tacos");
  }

  /**
   * takes a list of `CodeBlock` objects and returns a single `CodeBlock` object
   * representing the concatenation of the elements in the list using the `||` operator.
   */
  @Test public void joiningSingle() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
    assertThat(joined.toString()).isEqualTo("\"hello\"");
  }

  /**
   * collects a list of `CodeBlock` objects and joins them together using the specified
   * prefix and suffix, resulting in a single `CodeBlock` object with the combined text.
   */
  @Test public void joiningWithPrefixAndSuffix() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || ", "start {", "} end"));
    assertThat(joined.toString()).isEqualTo("start {\"hello\" || world.World || need tacos} end");
  }

  /**
   * removes all statements from a code block, leaving it empty.
   */
  @Test public void clear() {
    CodeBlock block = CodeBlock.builder()
        .addStatement("$S", "Test string")
        .clear()
        .build();

    assertThat(block.toString()).isEmpty();
  }
}
