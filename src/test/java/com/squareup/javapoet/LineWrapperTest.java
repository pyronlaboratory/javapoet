/*
 * Copyright (C) 2016 Square, Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * tests various scenarios related to wrapping and embedding newlines in a string
 * buffer. The tests include:
 * 
 * 	- Overlong lines without leading space are wrapped to the next line.
 * 	- Overlong lines with leading space are wrapped to the next line.
 * 	- Lines with zero width spaces are embedded in the current line without breaking.
 * 	- No wrap is performed when embedding newlines.
 * 	- Wrap is performed when embedding newlines.
 * 	- No wrap is performed when embedding multiple newlines.
 * 	- Wrap is performed when embedding multiple newlines.
 * 
 * These tests ensure that the LineWrapper class behaves correctly in various scenarios
 * and is a reliable tool for working with string buffers and newlines.
 */
@RunWith(JUnit4.class)
public final class LineWrapperTest {
  /**
   * takes a `StringBuffer` object named `out`, and a spacing parameter `wrappingSpace`,
   * and then wraps the contents of `out` to a new line when it reaches a certain number
   * of characters, which is specified by the `wrappingSpace` parameter.
   */
  @Test public void wrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghij");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghij");
  }

  /**
   * takes a `StringBuffer` object as input and wraps the contents within 10 spaces,
   * without modifying its original length.
   */
  @Test public void noWrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi");
  }

  /**
   * appends a string to a `StringBuffer` while wrapping it with a `LineWrapper` object,
   * allowing for zero-width spaces and preventing newline wraps.
   */
  @Test public void zeroWidthNoWrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghij");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij");
  }

  /**
   * wraps a given string with a zero-width space and then appends it to a StringBuffer,
   * ensuring that the resulting string is at most 10 lines long.
   */
  @Test public void nospaceWrapMax() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghijk");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghijk");
  }

  /**
   * writes a sequence of strings to a writer, with each string separated by a wrapping
   * space and followed by a newline. The resulting output is then asserted to be equal
   * to a expected string.
   */
  @Test public void multipleWrite() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("ab");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("cd");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("ef");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("gh");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("ij");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("kl");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("mn");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("op");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("qr");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("ab cd ef\n  gh ij kl\n  mn op qr");
  }

  /**
   * appends a sequence of strings to a `StringBuffer`, wrapping each line with a space
   * and then closes the buffer, returning its contents as a single string.
   */
  @Test public void fencepost() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.append("fghij");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("k");
    lineWrapper.append("lmnop");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\n    klmnop");
  }

  /**
   * appends a string to a line buffer, followed by a zero-width space, and then another
   * string. It then closes the line buffer and asserts that the resulting string is
   * equal to the expected output.
   */
  @Test public void fencepostZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.append("fghij");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("k");
    lineWrapper.append("lmnop");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\n    klmnop");
  }

  /**
   * appends a string to a StringBuffer object, then calls `close()` on the buffer and
   * asserts that the resulting string is equal to the original input.
   */
  @Test public void overlyLongLinesWithoutLeadingSpace() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghijkl");
  }

  /**
   * wraps a string into lines with a maximum width of 10 characters, inserting a space
   * before each line if necessary.
   */
  @Test public void overlyLongLinesWithLeadingSpace() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("\n    abcdefghijkl");
  }

  /**
   * wraps a `StringBuffer` with a `LineWrapper` and uses its `zeroWidthSpace` method
   * to insert a leading zero-width space character into the buffer, followed by the
   * string "abcdefghijkl". The function then calls the `close()` method on the
   * `LineWrapper` to retrieve the wrapped string.
   */
  @Test public void overlyLongLinesWithLeadingZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghijkl");
  }

  /**
   * tests whether an embedded newline is wrapped correctly by a LineWrapper.
   */
  @Test public void noWrapEmbeddedNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\njklmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\njklmnopqrstuvwxy");
  }

  /**
   * takes a `StringBuffer` object and wraps any lines within it with a specified
   * wrapping space, while preserving the original newlines.
   */
  @Test public void wrapEmbeddedNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghij\nklmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghij\nklmnopqrstuvwxy");
  }

  /**
   * wraps a string buffer with a LineWrapper and appends a series of characters to it,
   * then uses the zeroWidthSpace method to remove any unnecessary newlines before
   * returning the result.
   */
  @Test public void noWrapEmbeddedNewlines_ZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghij\nklmn");
    lineWrapper.append("opqrstuvwxyz");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\nklmnopqrstuvwxyz");
  }

  /**
   * wraps a string with embedded newlines and zero-width spaces to create a new string
   * with the same content but without line breaks.
   */
  @Test public void wrapEmbeddedNewlines_ZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghijk\nlmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghijk\nlmnopqrstuvwxy");
  }

  /**
   * takes a `StringBuffer` object and wraps multiple newlines within a specified
   * wrapping space, without wrapping single newline characters.
   */
  @Test public void noWrapMultipleNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\nklmnopq\nr");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("stuvwxyz");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nr stuvwxyz");
  }

  /**
   * takes a `StringBuffer` as input and wraps multiple newlines in it with a specified
   * wrapping space, then returns the wrapped string buffer.
   */
  @Test public void wrapMultipleNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\nklmnopq\nrs");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("tuvwxyz1");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nrs\n    tuvwxyz1");
  }
}
