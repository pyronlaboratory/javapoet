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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * tests the various ways in which annotated types can be used in Java Poet. It covers
 * cases where the annotated type is an enclosing or nested type, a parameterized
 * type, an array type, and a varargs parameter. It also tests the behavior of
 * withoutAnnotations() on annotated enclosing and nested types, as well as on annotated
 * arrays and multidimensional arrays. Additionally, it covers the use of annotated
 * types in variance annotations and in varargs parameters.
 */
public class AnnotatedTypeNameTest {

  private final static String NN = NeverNull.class.getCanonicalName();
  private final AnnotationSpec NEVER_NULL = AnnotationSpec.builder(NeverNull.class).build();
  private final static String TUA = TypeUseAnnotation.class.getCanonicalName();
  private final AnnotationSpec TYPE_USE_ANNOTATION =
      AnnotationSpec.builder(TypeUseAnnotation.class).build();

  @Target(ElementType.TYPE_USE)
  public @interface NeverNull {}

  @Target(ElementType.TYPE_USE)
  public @interface TypeUseAnnotation {}


  /**
   * attempts to use an uninitialized `AnnotationSpec` array, which will result in a
   * `NullPointerException` when accessed.
   */
  @Test(expected=NullPointerException.class) public void nullAnnotationArray() {
    TypeName.BOOLEAN.annotated((AnnotationSpec[]) null);
  }

  /**
   * tests whether a `NullPointerException` is thrown when attempting to use an annotated
   * `TypeName` with a null list of annotations.
   */
  @Test(expected=NullPointerException.class) public void nullAnnotationList() {
    TypeName.DOUBLE.annotated((List<AnnotationSpec>) null);
  }

  /**
   * in Java tests whether a `TypeName` object is annotated with the specified value
   * and returns an annotated `TypeName` object if it is not already annotated.
   */
  @Test public void annotated() {
    TypeName simpleString = TypeName.get(String.class);
    assertFalse(simpleString.isAnnotated());
    assertEquals(simpleString, TypeName.get(String.class));

    TypeName annotated = simpleString.annotated(NEVER_NULL);
    assertTrue(annotated.isAnnotated());
    assertEquals(annotated, annotated.annotated());
  }

  /**
   * takes a `TypeName` object as input and returns an annotated version of it, using
   * the `TYPE_USE_ANNOTATION` annotation. The resulting `TypeName` object has the same
   * type as the original input, but with an additional annotation added.
   */
  @Test public void annotatedType() {
    TypeName type = TypeName.get(String.class);
    TypeName actual = type.annotated(TYPE_USE_ANNOTATION);
    assertThat(actual.toString()).isEqualTo("java.lang. @" + TUA + " String");
  }

  /**
   * annotates a `TypeName` instance with two different annotations: `NEVER_NULL` and
   * `TYPE_USE_ANNOTATION`. The resulting `TypeName` object is then compared to its
   * expected string representation using `assertThat()`.
   */
  @Test public void annotatedTwice() {
    TypeName type = TypeName.get(String.class);
    TypeName actual =
        type.annotated(NEVER_NULL)
            .annotated(TYPE_USE_ANNOTATION);
    assertThat(actual.toString())
        .isEqualTo("java.lang. @" + NN + " @" + TUA + " String");
  }

  /**
   * uses Parameterized Type Name to get the type of a list of strings with an annotation.
   */
  @Test public void annotatedParameterizedType() {
    TypeName type = ParameterizedTypeName.get(List.class, String.class);
    TypeName actual = type.annotated(TYPE_USE_ANNOTATION);
    assertThat(actual.toString()).isEqualTo("java.util. @" + TUA + " List<java.lang.String>");
  }

  /**
   * tests whether the parameterized type name of a method return type is correctly
   * generated based on an annotation.
   */
  @Test public void annotatedArgumentOfParameterizedType() {
    TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
    TypeName actual = ParameterizedTypeName.get(ClassName.get(List.class), type);
    assertThat(actual.toString()).isEqualTo("java.util.List<java.lang. @" + TUA + " String>");
  }

  /**
   * tests whether a wildcard type name can be used to retrieve the supertype of a given
   * type, using an annotation to specify the desired type name.
   */
  @Test public void annotatedWildcardTypeNameWithSuper() {
    TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
    TypeName actual = WildcardTypeName.supertypeOf(type);
    assertThat(actual.toString()).isEqualTo("? super java.lang. @" + TUA + " String");
  }

  /**
   * tests whether a wildcard type name can be used to represent a subtype of a class
   * with an annotation.
   */
  @Test public void annotatedWildcardTypeNameWithExtends() {
    TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
    TypeName actual = WildcardTypeName.subtypeOf(type);
    assertThat(actual.toString()).isEqualTo("? extends java.lang. @" + TUA + " String");
  }

  /**
   * tests whether five different types are equivalent according to Java type annotations.
   */
  @Test public void annotatedEquivalence() {
    annotatedEquivalence(TypeName.VOID);
    annotatedEquivalence(ArrayTypeName.get(Object[].class));
    annotatedEquivalence(ClassName.get(Object.class));
    annotatedEquivalence(ParameterizedTypeName.get(List.class, Object.class));
    annotatedEquivalence(TypeVariableName.get(Object.class));
    annotatedEquivalence(WildcardTypeName.get(Object.class));
  }

  /**
   * checks if two types are annotated equivalents, and performs various assertions to
   * verify their equivalence.
   * 
   * @param type TypeName object that is being tested for various properties and equality.
   * 
   * 	- `isAnnotated()`: This method returns a boolean indicating whether the `type`
   * is annotated or not.
   * 	- `equals()` and `hashCode()`: These methods define how similar objects are
   * compared and their hash codes are generated. They work with both unannotated and
   * annotated types.
   * 	- `annotated(TYPE_USE_ANNOTATION)`: This method returns a new `type` object that
   * is the same as the original `type` but with the `TYPE_USE_ANNOTATION` annotation
   * added.
   * 	- `notEquals()`: This method returns a boolean indicating whether the `type` is
   * not equal to the annotated version of itself.
   * 
   * These properties demonstrate how the `type` object can be used in various ways,
   * including checking its annotated status and comparing it to both unannotated and
   * annotated versions of itself.
   */
  private void annotatedEquivalence(TypeName type) {
    assertFalse(type.isAnnotated());
    assertEquals(type, type);
    assertEquals(type.annotated(TYPE_USE_ANNOTATION), type.annotated(TYPE_USE_ANNOTATION));
    assertNotEquals(type, type.annotated(TYPE_USE_ANNOTATION));
    assertEquals(type.hashCode(), type.hashCode());
    assertEquals(type.annotated(TYPE_USE_ANNOTATION).hashCode(),
        type.annotated(TYPE_USE_ANNOTATION).hashCode());
    assertNotEquals(type.hashCode(), type.annotated(TYPE_USE_ANNOTATION).hashCode());
  }

  // https://github.com/square/javapoet/issues/431
  /**
   * determines the toString representation of a `TypeName` object by recursively
   * traversing its nested types and applying an annotation to each type name.
   */
  @Test public void annotatedNestedType() {
    TypeName type = TypeName.get(Map.Entry.class).annotated(TYPE_USE_ANNOTATION);
    assertThat(type.toString()).isEqualTo("java.util.Map. @" + TUA + " Entry");
  }

  /**
   * extracts the fully qualified name of a type, including its enclosing and nested
   * types, using Java's TypeUseAnnotations feature.
   */
  @Test public void annotatedEnclosingAndNestedType() {
    TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
        .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
    assertThat(type.toString()).isEqualTo("java.util. @" + TUA + " Map. @" + TUA + " Entry");
  }

  // https://github.com/square/javapoet/issues/431
  /**
   * verifies that a given type name represents a nested parameterized type with the
   * specified annotations.
   */
  @Test public void annotatedNestedParameterizedType() {
    TypeName type = ParameterizedTypeName.get(Map.Entry.class, Byte.class, Byte.class)
        .annotated(TYPE_USE_ANNOTATION);
    assertThat(type.toString())
        .isEqualTo("java.util.Map. @" + TUA + " Entry<java.lang.Byte, java.lang.Byte>");
  }

  /**
   * verifies that an annotated type name can be transformed into a non-annotated type
   * name while preserving the nested and enclosing types.
   */
  @Test public void withoutAnnotationsOnAnnotatedEnclosingAndNestedType() {
    TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
        .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
    assertThat(type.isAnnotated()).isTrue();
    assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
  }

  /**
   * tests if an annotated type can be converted to its unannotated equivalent using
   * the `withoutAnnotations()` method, and verifies that the resulting type is equal
   * to the expected one.
   */
  @Test public void withoutAnnotationsOnAnnotatedEnclosingType() {
    TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
        .nestedClass("Entry");
    assertThat(type.isAnnotated()).isTrue();
    assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
  }

  /**
   * tests whether an annotated nested type has its annotations removed, resulting in
   * a type that is equal to the underlying type of the annotated class.
   */
  @Test public void withoutAnnotationsOnAnnotatedNestedType() {
    TypeName type = ((ClassName) TypeName.get(Map.class))
        .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
    assertThat(type.isAnnotated()).isTrue();
    assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
  }

  // https://github.com/square/javapoet/issues/614
   /**
    * returns the annotated array type name for an object class, using the `ArrayTypeName.of()`
    * method and the `TYPE_USE_ANNOTATION` annotation.
    */
   @Test public void annotatedArrayType() {
    TypeName type = ArrayTypeName.of(ClassName.get(Object.class)).annotated(TYPE_USE_ANNOTATION);
    assertThat(type.toString()).isEqualTo("java.lang.Object @" + TUA + " []");
  }

  /**
   * verifies that the toString() method of an array type returns a string representation
   * of the element type, including any annotations using the `@` symbol.
   */
  @Test public void annotatedArrayElementType() {
    TypeName type = ArrayTypeName.of(ClassName.get(Object.class).annotated(TYPE_USE_ANNOTATION));
    assertThat(type.toString()).isEqualTo("java.lang. @" + TUA + " Object[]");
  }

  // https://github.com/square/javapoet/issues/614
  /**
   * creates an annotated array type name consisting of a nested array type name and a
   * generic type parameter, resulting in a multidimensional array type name with an annotation.
   */
  @Test public void annotatedOuterMultidimensionalArrayType() {
    TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class)))
        .annotated(TYPE_USE_ANNOTATION);
    assertThat(type.toString()).isEqualTo("java.lang.Object @" + TUA + " [][]");
  }

  // https://github.com/square/javapoet/issues/614
  /**
   * verifies that the toString method of an inner multidimensional array type returns
   * a string representation of the form "typeName []", where typeName is the name of
   * the outer array type and TUA is an annotation.
   */
  @Test public void annotatedInnerMultidimensionalArrayType() {
    TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class))
        .annotated(TYPE_USE_ANNOTATION));
    assertThat(type.toString()).isEqualTo("java.lang.Object[] @" + TUA + " []");
  }

  // https://github.com/square/javapoet/issues/614
  /**
   * generates a method signature with an annotated array type parameter and varags
   * parameter, resulting in a string representation of the method definition containing
   * the expected parameter types and annotations.
   */
  @Test public void annotatedArrayTypeVarargsParameter() {
    TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class)))
        .annotated(TYPE_USE_ANNOTATION);
    MethodSpec varargsMethod = MethodSpec.methodBuilder("m")
        .addParameter(
            ParameterSpec.builder(type, "p")
                .build())
        .varargs()
        .build();
    assertThat(varargsMethod.toString()).isEqualTo(""
        + "void m(java.lang.Object @" + TUA + " []... p) {\n"
        + "}\n");
  }

  // https://github.com/square/javapoet/issues/614
  /**
   * defines a method with an array type annotated with the `TYPE_USE_ANNOTATION`.
   */
  @Test public void annotatedArrayTypeInVarargsParameter() {
    TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class))
        .annotated(TYPE_USE_ANNOTATION));
    MethodSpec varargsMethod = MethodSpec.methodBuilder("m")
        .addParameter(
            ParameterSpec.builder(type, "p")
                .build())
        .varargs()
        .build();
    assertThat(varargsMethod.toString()).isEqualTo(""
        + "void m(java.lang.Object[] @" + TUA + " ... p) {\n"
        + "}\n");
  }
}
