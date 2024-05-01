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

import com.google.testing.compile.CompilationRule;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import org.junit.Before;
import org.junit.Rule;
import javax.lang.model.element.Modifier;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.squareup.javapoet.TestUtil.findFirst;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.junit.Assert.fail;

/**
 * is a test class for the ParameterSpec class in the Java programming language. It
 * provides various methods to test the behavior of ParameterSpec, including testing
 * its equals and hash code methods, receiver parameter instance method, receiver
 * parameter nested class method, keyword name, null annotations addition, field
 * variable element, parameter variable element, adding non-final modifier, modifying
 * annotations, and modifying modifiers. The tests cover various aspects of the
 * ParameterSpec class, such as its behavior when dealing with different types of
 * elements, modifiers, and annotations.
 */
public class ParameterSpecTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private Elements elements;

  /**
   * sets the `elements` field to the `Elements` returned by the `compilation.getElements()`
   * method.
   */
  @Before public void setUp() {
    elements = compilation.getElements();
  }

  /**
   * retrieves a `TypeElement` object representing a class type from an elements map
   * based on the class's canonical name.
   * 
   * @param clazz Class<?> object that the function should return the TypeElement for.
   * 
   * 	- `clazz`: A reference to a class object representing a type in the program's grammar.
   * 	- `elements`: An instance of `TypeElementMap`, which maps a class name to its
   * corresponding type element representation.
   * 
   * @returns a `TypeElement` object representing the specified class.
   * 
   * 	- The `TypeElement` object represents a type in the Java language.
   * 	- The `Class<?>` parameter passed to the function is used to identify the type
   * element being retrieved.
   * 	- The function returns a `TypeElement` object that corresponds to the specified
   * class.
   * 	- The `getTypeElement` method returns a reference to a specific type element
   * within the elements collection, which is a map of class names to their corresponding
   * type elements.
   */
  private TypeElement getElement(Class<?> clazz) {
    return elements.getTypeElement(clazz.getCanonicalName());
  }

  /**
   * tests whether two `ParameterSpec` objects are equal and have the same hash code,
   * as well as comparing their toString representations.
   */
  @Test public void equalsAndHashCode() {
    ParameterSpec a = ParameterSpec.builder(int.class, "foo").build();
    ParameterSpec b = ParameterSpec.builder(int.class, "foo").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
    a = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
    b = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  /**
   * tests whether a `ParameterSpec.Builder` instance builds a parameter spec with the
   * expected name when passed an instance of `int`.
   */
  @Test public void receiverParameterInstanceMethod() {
    ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "this");
    assertThat(builder.build().name).isEqualTo("this");
  }

  /**
   * tests whether a `ParameterSpec.Builder` produces the expected name for a nested
   * class parameter.
   */
  @Test public void receiverParameterNestedClass() {
    ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "Foo.this");
    assertThat(builder.build().name).isEqualTo("Foo.this");
  }

  /**
   * tests whether an invalid name can be passed to `ParameterSpec.builder`.
   */
  @Test public void keywordName() {
    try {
      ParameterSpec.builder(int.class, "super");
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("not a valid name: super");
    }
  }

  /**
   * tests whether an attempt to add annotations to a parameter specification with null
   * annotations will throw an exception.
   */
  @Test public void nullAnnotationsAddition() {
    try {
      ParameterSpec.builder(int.class, "foo").addAnnotations(null);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  /**
   * has a single field named "name" of type String.
   * Fields:
   * 	- name (String): in the VariableElementFieldClass class represents a string value.
   */
  final class VariableElementFieldClass {
    String name;
  }

  /**
   * tests whether a given element is a method or not by trying to obtain its parameter
   * and failing if it's not.
   */
  @Test public void fieldVariableElement() {
    TypeElement classElement = getElement(VariableElementFieldClass.class);
    List<VariableElement> methods = fieldsIn(elements.getAllMembers(classElement));
    VariableElement element = findFirst(methods, "name");

    try {
      ParameterSpec.get(element);
      fail();
    } catch (IllegalArgumentException exception) {
      assertThat(exception).hasMessageThat().isEqualTo("element is not a parameter");
    }
  }

  /**
   * is a Java class that contains a single method, "foo", which takes a nullable string
   * parameter. The method is marked as final and has a single annotated modifier, @Nullable.
   */
  final class VariableElementParameterClass {
    /**
     * takes a `@Nullable` string parameter `bar` and performs some action based on its
     * value.
     * 
     * @param bar nullable value that is passed to the `foo` function.
     */
    public void foo(@Nullable final String bar) {
    }
  }

  /**
   * retrieves and verifies the type of a method parameter using `ParameterSpec`.
   */
  @Test public void parameterVariableElement() {
    TypeElement classElement = getElement(VariableElementParameterClass.class);
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    ExecutableElement element = findFirst(methods, "foo");
    VariableElement parameterElement = element.getParameters().get(0);

    assertThat(ParameterSpec.get(parameterElement).toString())
        .isEqualTo("java.lang.String arg0");
  }

  /**
   * tests whether adding a non-final modifier to a parameter throws an exception.
   */
  @Test public void addNonFinalModifier() {
    List<Modifier> modifiers = new ArrayList<>();
    modifiers.add(Modifier.FINAL);
    modifiers.add(Modifier.PUBLIC);

    try {
      ParameterSpec.builder(int.class, "foo")
          .addModifiers(modifiers);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("unexpected parameter modifier: public");
    }
  }

  /**
   * allows for modifying the annotations of a `ParameterSpec`. It removes an annotation
   * from the list of annotations associated with the parameter, and verifies that the
   * updated list of annotations has only one element.
   */
  @Test public void modifyAnnotations() {
    ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "foo")
            .addAnnotation(Override.class)
            .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  /**
   * modifies the modifiers of a `ParameterSpec`. Specifically, it removes the second
   * modifier from a list of modifiers, leaving only the `PUBLIC` modifier.
   */
  @Test public void modifyModifiers() {
    ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "foo")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }
}
