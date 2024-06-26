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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * is a JUnit test class that verifies various character and string literals in Java
 * using the `Util` class. The test class has several methods that test the behavior
 * of the `Util` class's `characterLiteralWithoutSingleQuotes`, `stringLiteralWithDoubleQuotes`,
 * and `stringLiteralWithoutSingleQuotes` methods. These methods are used to encode
 * various characters and strings with different escapes, such as backslash escapes,
 * octal escapes, and Unicode escapes. The test class also verifies the behavior of
 * these methods with different input values, including simple characters, complex
 * strings, and special characters like "€", "☃", "♠", and "♦".
 */
public class UtilTest {
  /**
   * tests various character literals in Java, including single quotes, backslash
   * escapes, octal escapes, and Unicode escapes.
   */
  @Test public void characterLiteral() {
    assertEquals("a", Util.characterLiteralWithoutSingleQuotes('a'));
    assertEquals("b", Util.characterLiteralWithoutSingleQuotes('b'));
    assertEquals("c", Util.characterLiteralWithoutSingleQuotes('c'));
    assertEquals("%", Util.characterLiteralWithoutSingleQuotes('%'));
    // common escapes
    assertEquals("\\b", Util.characterLiteralWithoutSingleQuotes('\b'));
    assertEquals("\\t", Util.characterLiteralWithoutSingleQuotes('\t'));
    assertEquals("\\n", Util.characterLiteralWithoutSingleQuotes('\n'));
    assertEquals("\\f", Util.characterLiteralWithoutSingleQuotes('\f'));
    assertEquals("\\r", Util.characterLiteralWithoutSingleQuotes('\r'));
    assertEquals("\"", Util.characterLiteralWithoutSingleQuotes('"'));
    assertEquals("\\'", Util.characterLiteralWithoutSingleQuotes('\''));
    assertEquals("\\\\", Util.characterLiteralWithoutSingleQuotes('\\'));
    // octal escapes
    assertEquals("\\u0000", Util.characterLiteralWithoutSingleQuotes('\0'));
    assertEquals("\\u0007", Util.characterLiteralWithoutSingleQuotes('\7'));
    assertEquals("?", Util.characterLiteralWithoutSingleQuotes('\77'));
    assertEquals("\\u007f", Util.characterLiteralWithoutSingleQuotes('\177'));
    assertEquals("¿", Util.characterLiteralWithoutSingleQuotes('\277'));
    assertEquals("ÿ", Util.characterLiteralWithoutSingleQuotes('\377'));
    // unicode escapes
    assertEquals("\\u0000", Util.characterLiteralWithoutSingleQuotes('\u0000'));
    assertEquals("\\u0001", Util.characterLiteralWithoutSingleQuotes('\u0001'));
    assertEquals("\\u0002", Util.characterLiteralWithoutSingleQuotes('\u0002'));
    assertEquals("€", Util.characterLiteralWithoutSingleQuotes('\u20AC'));
    assertEquals("☃", Util.characterLiteralWithoutSingleQuotes('\u2603'));
    assertEquals("♠", Util.characterLiteralWithoutSingleQuotes('\u2660'));
    assertEquals("♣", Util.characterLiteralWithoutSingleQuotes('\u2663'));
    assertEquals("♥", Util.characterLiteralWithoutSingleQuotes('\u2665'));
    assertEquals("♦", Util.characterLiteralWithoutSingleQuotes('\u2666'));
    assertEquals("✵", Util.characterLiteralWithoutSingleQuotes('\u2735'));
    assertEquals("✺", Util.characterLiteralWithoutSingleQuotes('\u273A'));
    assertEquals("／", Util.characterLiteralWithoutSingleQuotes('\uFF0F'));
  }

  /**
   * tests various combinations of strings, including empty strings, escaped characters,
   * and new line characters.
   */
  @Test public void stringLiteral() {
    stringLiteral("abc");
    stringLiteral("♦♥♠♣");
    stringLiteral("€\\t@\\t$", "€\t@\t$", " ");
    stringLiteral("abc();\\n\"\n  + \"def();", "abc();\ndef();", " ");
    stringLiteral("This is \\\"quoted\\\"!", "This is \"quoted\"!", " ");
    stringLiteral("e^{i\\\\pi}+1=0", "e^{i\\pi}+1=0", " ");
  }

  /**
   * takes a string as input and passes it to another function named `stringLiteral`.
   * 
   * @param string 2nd and last arguments of the `stringLiteral` method, which are
   * applied to the concatenation operator.
   */
  void stringLiteral(String string) {
    stringLiteral(string, string, " ");
  }

  /**
   * compares a given string value with an expected literal value, using the
   * `Util.stringLiteralWithDoubleQuotes()` method to perform the comparison.
   * 
   * @param expected expected value of the `String` literal being checked by the
   * `assertEquals` method.
   * 
   * @param value string value to be checked against the expected output.
   * 
   * @param indent level of indentation for the generated code summary
   */
  void stringLiteral(String expected, String value, String indent) {
    assertEquals("\"" + expected + "\"", Util.stringLiteralWithDoubleQuotes(value, indent));
  }
}
