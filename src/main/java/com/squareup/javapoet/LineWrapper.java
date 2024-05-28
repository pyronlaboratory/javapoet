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
 * Implements soft line wrapping on an appendable. To use, append characters using {@link #append}
 * or soft-wrapping spaces using {@link #wrappingSpace}.
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
   * Retrieves the last character of a given string or buffer object `out`.
   * 
   * @returns the last character of a string `out`.
   */
  char lastChar() {
    return out.lastChar;
  }

  /**
   * Appends a string to an output stream, checking for overflow and wrapping as needed.
   * It returns after each successful append.
   * 
   * @param s string to be appended to the buffer.
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
   * Increments a column and sets the next flush type to `SPACE`. It also updates the
   * `indentLevel` variable with the given value.
   * 
   * @param indentLevel level of indentation to apply to the current line of code being
   * wrapped, and is used to determine the appropriate amount of space to add to the
   * line for proper formatting.
   */
  void wrappingSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (this.nextFlush != null) flush(nextFlush);
    column++; // Increment the column even though the space is deferred to next call to flush().
    this.nextFlush = FlushType.SPACE;
    this.indentLevel = indentLevel;
  }

  /**
   * Updates the current instance's indent level and flushes the buffer if necessary,
   * ensuring the next call to the function will have a new line.
   * 
   * @param indentLevel level of indentation for the output, and it is used to control
   * the amount of space added before each line of output in the `zeroWidthSpace` function.
   */
  void zeroWidthSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (column == 0) return;
    if (this.nextFlush != null) flush(nextFlush);
    this.nextFlush = FlushType.EMPTY;
    this.indentLevel = indentLevel;
  }

  /**
   * Flushed any pending data and marked the stream as closed.
   */
  void close() throws IOException {
    if (nextFlush != null) flush(nextFlush);
    closed = true;
  }

  /**
   * Modifies output based on the provided ` flushType`. It appends a newline, spaces,
   * or does nothing depending on the type, then resets the buffer and nextFlush.
   * 
   * @param flushType type of flush operation to perform, which can be either `WRAP`,
   * `SPACE`, or `EMPTY`, and determines how much space to add to the buffer before
   * appending its contents.
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

  /** A delegating {@link Appendable} that records info about the chars passing through it. */
  static final class RecordingAppendable implements Appendable {
    private final Appendable delegate;

    char lastChar = Character.MIN_VALUE;

    RecordingAppendable(Appendable delegate) {
      this.delegate = delegate;
    }

    /**
     * Adds a sequence of characters to an `Appendable` object, while maintaining the
     * last character of the previous append operation.
     * 
     * @param csq 0-length sequence of characters to be appended to the output stream.
     * 
     * @returns the result of calling the delegated `append` method with the provided `CharSequence`.
     */
    @Override public Appendable append(CharSequence csq) throws IOException {
      int length = csq.length();
      if (length != 0) {
        lastChar = csq.charAt(length - 1);
      }
      return delegate.append(csq);
    }

    /**
     * Takes a subsequence of a given `CharSequence` and returns a new instance of
     * `Appendable` with the appended subsequence.
     * 
     * @param csq CharSequence that is being operated on by the `append()` method.
     * 
     * @param start 0-based index of the subsequence within the original sequence that
     * the method seeks to append.
     * 
     * @param end position of the last character to be appended in the subsequence returned
     * by `csq.subSequence()`.
     * 
     * @returns a new instance of the `Appendable` interface, which represents the appended
     * sequence.
     */
    @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
      CharSequence sub = csq.subSequence(start, end);
      return append(sub);
    }

    /**
     * Appends the character `c` to the contents of its delegating function, `delegate`.
     * 
     * @param c 8-bit binary code that is appended to the output of the `delegate` method,
     * which is also passed as an argument to the `append` method.
     * 
     * @returns a single character.
     */
    @Override public Appendable append(char c) throws IOException {
      lastChar = c;
      return delegate.append(c);
    }
  }
}
