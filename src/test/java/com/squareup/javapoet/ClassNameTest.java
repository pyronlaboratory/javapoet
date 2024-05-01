/*
 * Copyright (C) 2014 Google, Inc.
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

import com.google.testing.compile.CompilationRule;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * is an Android library that provides utility functions for working with Java classes.
 * The test class covers various aspects of the ClassName utility class, including:
 * 
 * 	- Getting the canonical name of a class (# classNameFromClass)
 * 	- Getting the peer class of a class (# peerClass)
 * 	- Checking if a class is a rejection type (# fromClassRejectionTypes)
 * 	- Reflection name and canonical name of a class (# reflectionName, # canonicalName)
 * 
 * The test cases cover various scenarios such as:
 * 
 * 	- Getting the className of an object (# classNameFromObject)
 * 	- Getting the className of a class from its type element (# classNameFromTypeElement)
 * 	- Checking if a class is null or empty (# classNameFromClass)
 * 	- Getting the peer class of a class (# peerClass)
 * 	- Checking for rejection types (# fromClassRejectionTypes)
 * 
 * The test class also provides examples of how to use the ClassName utility class
 * in different scenarios, such as:
 * 
 * 	- Getting the canonical name of an object (# reflectionName)
 * 	- Getting the canonical name of a class from its type element (# canonicalName)
 * 
 * Overall, the test class provides a comprehensive coverage of the features and
 * functionality of the ClassName utility class.
 */
@RunWith(JUnit4.class)
public final class ClassNameTest {
  @Rule public CompilationRule compilationRule = new CompilationRule();

  /**
   * tests whether the `ClassName.bestGuess()` method returns the correct class name
   * for the `String` class.
   */
  @Test public void bestGuessForString_simpleClass() {
    assertThat(ClassName.bestGuess(String.class.getName()))
        .isEqualTo(ClassName.get("java.lang", "String"));
  }

  /**
   * predicts a Java class name based on a given string, returning the predicted package
   * name and simple name.
   */
  @Test public void bestGuessNonAscii() {
    ClassName className = ClassName.bestGuess(
        "com.\ud835\udc1andro\ud835\udc22d.\ud835\udc00ctiv\ud835\udc22ty");
    assertEquals("com.\ud835\udc1andro\ud835\udc22d", className.packageName());
    assertEquals("\ud835\udc00ctiv\ud835\udc22ty", className.simpleName());
  }

  /**
   * has an InnerClass nested within it.
   */
  static class OuterClass {
    /**
     * defines a nested class within an outer class with no declared fields or methods.
     */
    static class InnerClass {}
  }

  /**
   * tests the `bestGuess` method's ability to identify the correct class name for a
   * given canonical name, including nested classes within an outer class.
   */
  @Test public void bestGuessForString_nestedClass() {
    assertThat(ClassName.bestGuess(Map.Entry.class.getCanonicalName()))
        .isEqualTo(ClassName.get("java.util", "Map", "Entry"));
    assertThat(ClassName.bestGuess(OuterClass.InnerClass.class.getCanonicalName()))
        .isEqualTo(ClassName.get("com.squareup.javapoet",
            "ClassNameTest", "OuterClass", "InnerClass"));
  }

  /**
   * tests ClassName's ability to correctly guess the fully qualified class name based
   * on a given partial name, with different levels of nesting.
   */
  @Test public void bestGuessForString_defaultPackage() {
    assertThat(ClassName.bestGuess("SomeClass"))
        .isEqualTo(ClassName.get("", "SomeClass"));
    assertThat(ClassName.bestGuess("SomeClass.Nested"))
        .isEqualTo(ClassName.get("", "SomeClass", "Nested"));
    assertThat(ClassName.bestGuess("SomeClass.Nested.EvenMore"))
        .isEqualTo(ClassName.get("", "SomeClass", "Nested", "EvenMore"));
  }

  /**
   * tests the `assertBestGuessThrows()` method by providing confusing input for the
   * method to handle, including empty strings, dots, abbreviations, and invalid class
   * names.
   */
  @Test public void bestGuessForString_confusingInput() {
    assertBestGuessThrows("");
    assertBestGuessThrows(".");
    assertBestGuessThrows(".Map");
    assertBestGuessThrows("java");
    assertBestGuessThrows("java.util");
    assertBestGuessThrows("java.util.");
    assertBestGuessThrows("java..util.Map.Entry");
    assertBestGuessThrows("java.util..Map.Entry");
    assertBestGuessThrows("java.util.Map..Entry");
    assertBestGuessThrows("com.test.$");
    assertBestGuessThrows("com.test.LooksLikeAClass.pkg");
    assertBestGuessThrows("!@#$gibberish%^&*");
  }

  /**
   * tests whether an IllegalArgumentException is thrown when attempting to call
   * `ClassName.bestGuess(String)` with a invalid input parameter.
   * 
   * @param s string that will be passed to the `bestGuess()` method, which is then
   * used to throw an `IllegalArgumentException`.
   */
  private void assertBestGuessThrows(String s) {
    try {
      ClassName.bestGuess(s);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * creates a nested class within an existing class, and verifies that the resulting
   * class name is correct.
   */
  @Test public void createNestedClass() {
    ClassName foo = ClassName.get("com.example", "Foo");
    ClassName bar = foo.nestedClass("Bar");
    assertThat(bar).isEqualTo(ClassName.get("com.example", "Foo", "Bar"));
    ClassName baz = bar.nestedClass("Baz");
    assertThat(baz).isEqualTo(ClassName.get("com.example", "Foo", "Bar", "Baz"));
  }

  /**
   * has a static inner class $Inner.
   */
  static class $Outer {
    /**
     * is an inner class within a larger class, providing a scope for its members to
     * access and use the outer class's variables and methods without exposing them to
     * the outer class's outer environment.
     */
    static class $Inner {}
  }

  /**
   * retrieves the class name from a TypeElement object,
   *     performs assertions to validate the resulting string is correct, and
   *     does not provide any additional information beyond what is required for the test.
   */
  @Test public void classNameFromTypeElement() {
    Elements elements = compilationRule.getElements();
    TypeElement object = elements.getTypeElement(Object.class.getCanonicalName());
    assertThat(ClassName.get(object).toString()).isEqualTo("java.lang.Object");
    TypeElement outer = elements.getTypeElement($Outer.class.getCanonicalName());
    assertThat(ClassName.get(outer).toString()).isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
    TypeElement inner = elements.getTypeElement($Outer.$Inner.class.getCanonicalName());
    assertThat(ClassName.get(inner).toString()).isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
  }

  /**
   * verifies that the `ClassName` class does not use the `getKind()` method when
   * generating class names from `TypeElements`. It tests this by calling `ClassName.get()`
   * on three type elements and comparing the resulting class names to expected values.
   */
  @Test public void classNameFromTypeElementDoesntUseGetKind() {
    Elements elements = compilationRule.getElements();
    TypeElement object = elements.getTypeElement(Object.class.getCanonicalName());
    assertThat(ClassName.get(preventGetKind(object)).toString())
        .isEqualTo("java.lang.Object");
    TypeElement outer = elements.getTypeElement($Outer.class.getCanonicalName());
    assertThat(ClassName.get(preventGetKind(outer)).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
    TypeElement inner = elements.getTypeElement($Outer.$Inner.class.getCanonicalName());
    assertThat(ClassName.get(preventGetKind(inner)).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
  }

  /**
   * transforms a given `TypeElement` object into a mocked version that throws an
   * `AssertionError` when its `getKind()` method is called, while still returning the
   * original object's enclosing element.
   * 
   * @param object TypeElement to be wrapped with Mockito's spy, which allows to override
   * its `getKind()` method and throw an `AssertionError` when it is invoked.
   * 
   * 	- `spy`: A spied version of `object` created using Mockito.
   * 	- `getKind()`: A method on `spy` that throws an `AssertionError` when called.
   * 	- `enclosingElement`: An optional attribute of `spy` that returns the enclosing
   * element of `spy`, which can be a type element.
   * 	- `callRealMethod()`: A method on the `invocation` object that calls the real
   * method of the target object and returns its result.
   * 
   * @returns a spied `TypeElement` object that throws an `AssertionError` when called.
   * 
   * 	- The function returns a spied version of the `object` parameter, denoted by `spy`.
   * 	- The `getKind()` method on the spied object is stubbed to throw an `AssertionError`,
   * indicating that it is not possible to call this method safely.
   * 	- The `getEnclosingElement()` method on the spied object is overloaded with a new
   * answer that first calls the original method to retrieve the enclosing element, and
   * then recursively applies the same logic to retrieve the enclosing element of the
   * enclosing element. This allows the function to continue chaining the `getEnclosingElement()`
   * method until it reaches the root element.
   * 	- The function returns the spied object, which is a mocked version of the original
   * `object` parameter.
   */
  private TypeElement preventGetKind(TypeElement object) {
    TypeElement spy = Mockito.spy(object);
    when(spy.getKind()).thenThrow(new AssertionError());
    when(spy.getEnclosingElement()).thenAnswer(invocation -> {
      Object enclosingElement = invocation.callRealMethod();
      return enclosingElement instanceof TypeElement
          ? preventGetKind((TypeElement) enclosingElement)
          : enclosingElement;
    });
    return spy;
  }

  /**
   * verifies that the toString method of the Class class returns the correct fully
   * qualified name of a class or inner class.
   */
  @Test public void classNameFromClass() {
    assertThat(ClassName.get(Object.class).toString())
        .isEqualTo("java.lang.Object");
    assertThat(ClassName.get(OuterClass.InnerClass.class).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest.OuterClass.InnerClass");
    assertThat((ClassName.get(new Object() {}.getClass())).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest$1");
    assertThat((ClassName.get(new Object() { Object inner = new Object() {}; }.inner.getClass())).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest$2$1");
    assertThat((ClassName.get($Outer.class)).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
    assertThat((ClassName.get($Outer.$Inner.class)).toString())
        .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
  }

  /**
   * compares the peer class of an object with a given string representation, and asserts
   * they are equal.
   */
  @Test public void peerClass() {
    assertThat(ClassName.get(Double.class).peerClass("Short"))
        .isEqualTo(ClassName.get(Short.class));
    assertThat(ClassName.get("", "Double").peerClass("Short"))
        .isEqualTo(ClassName.get("", "Short"));
    assertThat(ClassName.get("a.b", "Combo", "Taco").peerClass("Burrito"))
        .isEqualTo(ClassName.get("a.b", "Combo", "Burrito"));
  }

  /**
   * tests whether ClassName.get() method throws an IllegalArgumentException when passed
   * invalid class types, including int, void, and Object[].
   */
  @Test public void fromClassRejectionTypes() {
    try {
      ClassName.get(int.class);
      fail();
    } catch (IllegalArgumentException ignored) {
    }
    try {
      ClassName.get(void.class);
      fail();
    } catch (IllegalArgumentException ignored) {
    }
    try {
      ClassName.get(Object[].class);
      fail();
    } catch (IllegalArgumentException ignored) {
    }
  }

  /**
   * returns the simplified name of a class or interface, based on its package and
   * simple name.
   */
  @Test
  public void reflectionName() {
    assertEquals("java.lang.Object", TypeName.OBJECT.reflectionName());
    assertEquals("java.lang.Thread$State", ClassName.get(Thread.State.class).reflectionName());
    assertEquals("java.util.Map$Entry", ClassName.get(Map.Entry.class).reflectionName());
    assertEquals("Foo", ClassName.get("", "Foo").reflectionName());
    assertEquals("Foo$Bar$Baz", ClassName.get("", "Foo", "Bar", "Baz").reflectionName());
    assertEquals("a.b.c.Foo$Bar$Baz", ClassName.get("a.b.c", "Foo", "Bar", "Baz").reflectionName());
  }

  /**
   * generates a unique and standardized name for a given class, interface, or primitive
   * type based on its fully qualified name.
   */
  @Test
  public void canonicalName() {
    assertEquals("java.lang.Object", TypeName.OBJECT.canonicalName());
    assertEquals("java.lang.Thread.State", ClassName.get(Thread.State.class).canonicalName());
    assertEquals("java.util.Map.Entry", ClassName.get(Map.Entry.class).canonicalName());
    assertEquals("Foo", ClassName.get("", "Foo").canonicalName());
    assertEquals("Foo.Bar.Baz", ClassName.get("", "Foo", "Bar", "Baz").canonicalName());
    assertEquals("a.b.c.Foo.Bar.Baz", ClassName.get("a.b.c", "Foo", "Bar", "Baz").canonicalName());
  }
}
