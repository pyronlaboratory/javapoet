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

import java.io.IOException;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * is an implementation of Appendable that tracks and manages the writing of text to
 * a buffer, with features like line wrapping, spaces, and flushing. It has methods
 * for emitting characters, lastChar, buffer, lastEmittedChar, append, lastChar,
 * flush, and more.
 */
final class LineWrapper {
  private final RecordingAppendable out;
  private final String indent;
  private final int columnLimit;
  private boolean closed;

  /** Characters written since the last wrapping space that haven't yet been flushed. */
  private final StringBuilder buffer = new StringBuilder();

  /** The number of characters since the most recent newline. Includes both out and the buffer. */
  private int column = 0;

  /**
   * -1 if we have no buffering; otherwise the number of {@code indent}s to write after wrapping.
   */
  private int indentLevel = -1;

  /**
   * Null if we have no buffering; otherwise the type to pass to the next call to {@link #flush}.
   */
  private FlushType nextFlush;

  LineWrapper(Appendable out, String indent, int columnLimit) {
    checkNotNull(out, "out == null");
    this.out = new RecordingAppendable(out);
    this.indent = indent;
    this.columnLimit = columnLimit;
  }

  /**
   * retrieves the last character from a given stream `out`.
   * 
   * @returns the last character of the input stream `out`.
   * 
   * The output is a character object `out.lastChar`.
   * It represents the last character read from the input stream `out`.
   * The character is an element of the ASCII character set.
   */
  char lastChar() {
    return out.lastChar;
  }

  /**
   * appends a string to a buffer, checking for overflow and wrapping as needed before
   * writing to an output stream.
   * 
   * @param s string to be appended to the current line of text, which can potentially
   * cause the line to overflow and require wrapping.
   */
  void append(String s) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (nextFlush != null) {
      int nextNewline = s.indexOf('\n');

      // If s doesn't cause the current line to cross the limit, buffer it and return. We'll decide
      // whether or not we have to wrap it later.
      if (nextNewline == -1 && column + s.length() <= columnLimit) {
        buffer.append(s);
        column += s.length();
        return;
      }

      // Wrap if appending s would overflow the current line.
      boolean wrap = nextNewline == -1 || column + nextNewline > columnLimit;
      flush(wrap ? FlushType.WRAP : nextFlush);
    }

    out.append(s);
    int lastNewline = s.lastIndexOf('\n');
    column = lastNewline != -1
        ? s.length() - lastNewline - 1
        : column + s.length();
  }

  /**
   * updates the state of a buffer and determines whether to flush it immediately or
   * defer it to the next call. It also maintains the column number and indent level.
   * 
   * @param indentLevel level of indentation that should be applied to the text being
   * written, and is used to control the amount of space added between lines of output.
   */
  void wrappingSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (this.nextFlush != null) flush(nextFlush);
    column++; // Increment the column even though the space is deferred to next call to flush().
    this.nextFlush = FlushType.SPACE;
    this.indentLevel = indentLevel;
  }

  /**
   * manages the indentation level and flushes the output when necessary, ensuring
   * proper formatting.
   * 
   * @param indentLevel 0-based level of indentation for the next line of text to be
   * written by the `zeroWidthSpace` function.
   */
  void zeroWidthSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (column == 0) return;
    if (this.nextFlush != null) flush(nextFlush);
    this.nextFlush = FlushType.EMPTY;
    this.indentLevel = indentLevel;
  }

  /**
   * flushes any remaining data to be written to a file and sets a flag indicating that
   * the stream is closed.
   */
  void close() throws IOException {
    if (nextFlush != null) flush(nextFlush);
    closed = true;
  }

  /**
   * determines how to handle a flush operation based on a `FlushType` parameter and
   * performs the appropriate action, such as appending a newline, space, or doing nothing.
   * 
   * @param flushType type of flush operation to perform on the output stream, which
   * can be either WRAP, SPACE, or EMPTY, and the function performs the appropriate
   * action based on the value of `flushType`.
   * 
   * 	- `WRAP`: This is the most common type of flush, where the output buffer is
   * appended with an extra newline character and indentation space for each level of
   * nesting.
   * 	- `SPACE`: This type of flush inserts a single space character at the end of the
   * output buffer.
   * 	- `EMPTY`: This type of flush does nothing and can be used when no further action
   * is required.
   * 	- `Unknown FlushType`: If the `flushType` parameter is not one of the recognized
   * values, an `IllegalArgumentException` will be thrown.
   */
  private void flush(FlushType flushType) throws IOException {
    switch (flushType) {
      case WRAP:
        out.append('\n');
        for (int i = 0; i < indentLevel; i++) {
          out.append(indent);
        }
        column = indentLevel * indent.length();
        column += buffer.length();
        break;
      case SPACE:
        out.append(' ');
        break;
      case EMPTY:
        break;
      default:
        throw new IllegalArgumentException("Unknown FlushType: " + flushType);
    }

    out.append(buffer);
    buffer.delete(0, buffer.length());
    indentLevel = -1;
    nextFlush = null;
  }

  private enum FlushType {
    WRAP, SPACE, EMPTY;
  }

  /**
   * is an implementing class of Appendable that keeps track of the characters passed
   * through it by storing the last character and the original sequence of characters.
   * This information can be used for debugging or logging purposes.
   */
  static final class RecordingAppendable implements Appendable {
    private final Appendable delegate;

    char lastChar = Character.MIN_VALUE;

    RecordingAppendable(Appendable delegate) {
      this.delegate = delegate;
    }

    /**
     * appends a character sequence to a `Appendable` object, maintaining the last
     * character's value.
     * 
     * @param csq character sequence to be appended to the current buffer of an Appendable
     * object.
     * 
     * 1/ `length`: The length of the input `csq` is checked and verified before proceeding
     * with the delegated append operation.
     * 2/ `lastChar`: If `csq` has a non-zero length, the last character of the sequence
     * is stored in the `lastChar` variable for further processing.
     * 3/ `delegate`: The `append` method calls the corresponding delegate method to
     * perform the actual appending operation.
     * 
     * @returns a sequence of characters generated by invoking the `delegate.append`
     * method with the input parameter `csq`.
     * 
     * 	- The `lastChar` variable holds the value of the last character appended to the
     * buffer.
     * 	- The `length` variable stores the total number of characters appended to the buffer.
     * 	- The `delegate` variable is an instance of a delegate class that represents the
     * actual append functionality.
     */
    @Override public Appendable append(CharSequence csq) throws IOException {
      int length = csq.length();
      if (length != 0) {
        lastChar = csq.charAt(length - 1);
      }
      return delegate.append(csq);
    }

    /**
     * takes a subsequence of a `CharSequence` object and returns a new `Appendable`
     * object that represents the appended sequence.
     * 
     * @param csq character sequence to be appended.
     * 
     * 	- The `subSequence` method extracts a subset of the original sequence with the
     * specified start and end indices.
     * 	- The resulting subsequence can be used as an argument for other append methods.
     * 
     * @param start 0-based index of the portion of the original CharSequence to be appended.
     * 
     * @param end 1-based index of the last character to be appended to the buffer.
     * 
     * @returns a new instance of the `Appendable` interface, which represents the appended
     * sequence of characters.
     * 
     * 	- The function returns an `Appendable` object, which is an interface in Java for
     * objects that can be appended to another sequence.
     * 	- The `subSequence` method is called on the input `CharSequence` object, passing
     * in the `start` and `end` parameters as arguments. This method returns a new
     * `CharSequence` object representing the subsequence of the original sequence between
     * the specified indices.
     * 	- The returned `Appendable` object is then used to append the subsequence to
     * another sequence.
     */
    @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
      CharSequence sub = csq.subSequence(start, end);
      return append(sub);
    }

    /**
     * overrides the `append` method of a `Appendable` object, appending the character
     * `c` to the contents of the object and then delegating the call to the underlying
     * `Delegate` object for further processing.
     * 
     * @param c character to be appended to the output of the `delegate.append()` method.
     * 
     * @returns a character added to the current line of text being written.
     * 
     * The `lastChar` field maintains the last character appended to the appendable object,
     * which can be accessed and modified throughout the execution of the function. The
     * `delegate` field is responsible for handling the actual appending operation by
     * calling its own `append` method with the passed `c` parameter.
     */
    @Override public Appendable append(char c) throws IOException {
      lastChar = c;
      return delegate.append(c);
    }
  }
}
