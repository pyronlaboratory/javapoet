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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * tests various aspects of the TypeName class, including its ability to handle generic
 * types, inner classes, and primitive types. It also includes methods for comparing
 * and hashing TypeNames, as well as checking if a TypeName is null or not. Additionally,
 * it includes some assertions to ensure that the behavior of the TypeName class is
 * as expected.
 */
public class TypeNameTest {

  private static final AnnotationSpec ANNOTATION_SPEC = AnnotationSpec.builder(ClassName.OBJECT).build();

  /**
   * takes an array of objects of type `E` and returns the value of the first element
   * in the array.
   * 
   * @param values array of elements that will be returned by the function, with type
   * parameterized as `E`.
   * 
   * 	- Type: `E` is a type parameter that represents an enumeration type.
   * 	- Length: The length of the `values` array is 1, indicating that there is only
   * one element in the array.
   * 	- Element: The element at index 0 of the `values` array is the value returned by
   * the function, which is the same as the first element of the array.
   * 
   * @returns the value of the first element in the input array of type `E`.
   * 
   * The function returns an element from an array of a generic type `E`, which is a
   * type parameter.
   * 
   * The array `values` can have any number of elements, and each element has a type
   * of `E`.
   * 
   * The first element of the array is returned by the function.
   */
  protected <E extends Enum<E>> E generic(E[] values) {
    return values[0];
  }

  /**
   * is a Java class with inner classes and generic types for testing purposes.
   */
  protected static class TestGeneric<T> {
    /**
     * is a nested inner class of the Outer Class, with its own fields and methods.
     */
    class Inner {}

    /**
     * is a generic class in Java that has a single inner class with no fields or methods
     * and is used for generic type parameters.
     */
    class InnerGeneric<T2> {}

    /**
     * is a nested non-generic class from the given file.
     */
    static class NestedNonGeneric {}
  }

  /**
   * returns `null`.
   * 
   * @returns `null`.
   */
  protected static TestGeneric<String>.Inner testGenericStringInner() {
    return null;
  }

  /**
   * returns `null`.
   * 
   * @returns `null`.
   * 
   * 	- The output is of type `TestGeneric<Integer>.Inner`.
   * 	- It is `null`, indicating that no inner class was generated for the given input.
   * 	- The lack of an inner class means that there is no explicit implementation of
   * the interface for the given type parameter.
   */
  protected static TestGeneric<Integer>.Inner testGenericIntInner() {
    return null;
  }

  /**
   * generates a `TestGeneric.InnerGeneric` object with a `Long` value.
   * 
   * @returns `null`.
   * 
   * The output is a `TestGeneric.InnerGeneric` object, which represents a generic inner
   * class in Java. This class has a single field named `longValue`, which is of type
   * `Long`. The field is declared as `static`, indicating that it belongs to the class
   * itself rather than an instance of the class.
   * 
   * The return value of the function is `null`, indicating that no instance of this
   * inner class was created during its execution.
   */
  protected static TestGeneric<Short>.InnerGeneric<Long> testGenericInnerLong() {
    return null;
  }

  /**
   * returns a `TestGeneric.InnerGeneric` object of type `Integer`.
   * 
   * @returns `null`.
   * 
   * The output is of type `TestGeneric<Short>.InnerGeneric<Integer>`. This means that
   * the output is an instance of the inner generic class of the outer class `TestGeneric`,
   * which is of type `Short`. The inner generic class has a type parameter of `Integer`.
   * 
   * The output is `null`, indicating that no object was returned by the function.
   * 
   * The return type of the function is inferred to be `TestGeneric<Short>.InnerGeneric<Integer>`,
   * based on the definition of the function and the types of the classes involved.
   */
  protected static TestGeneric<Short>.InnerGeneric<Integer> testGenericInnerInt() {
    return null;
  }

  /**
   * returns `null`.
   * 
   * @returns `null`.
   * 
   * The return type is `TestGeneric.NestedNonGeneric`, indicating that the function
   * returns an instance of a class that inherits from `TestGeneric` and has a non-generic
   * nested class.
   * 
   * The null return value suggests that the function does not have a defined behavior
   * for the case where no object is returned, which could indicate a potential bug or
   * oversight in the code.
   * 
   * The lack of any explicit type annotations on the function's output suggests that
   * the function may be intended to work with instances of any class that inherits
   * from `TestGeneric` and has a non-generic nested class, without requiring any
   * specific type information for the returned object.
   */
  protected static TestGeneric.NestedNonGeneric testNestedNonGeneric() {
    return null;
  }

  /**
   * verifies that the generic type name of a method is correctly generated based on
   * its parameter types, including the presence of the "Enum" suffix.
   */
  @Test public void genericType() throws Exception {
    Method recursiveEnum = getClass().getDeclaredMethod("generic", Enum[].class);
    TypeName.get(recursiveEnum.getReturnType());
    TypeName.get(recursiveEnum.getGenericReturnType());
    TypeName genericTypeName = TypeName.get(recursiveEnum.getParameterTypes()[0]);
    TypeName.get(recursiveEnum.getGenericParameterTypes()[0]);

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).contains("Enum");
  }

  /**
   * verifies that the inner class of a generic method has the correct type information
   * and is properly reflected in its name.
   */
  @Test public void innerClassInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericStringInner");
    TypeName.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeName.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeName.get(genericStringInner.getGenericReturnType()),
        TypeName.get(getClass().getDeclaredMethod("testGenericIntInner").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + "<java.lang.String>.Inner");
  }

  /**
   * tests whether the generic return type of a method is properly resolved and reflected
   * in the class name. It also verifies that the generic argument is correctly presented
   * in the class name.
   */
  @Test public void innerGenericInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericInnerLong");
    TypeName.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeName.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeName.get(genericStringInner.getGenericReturnType()),
        TypeName.get(getClass().getDeclaredMethod("testGenericInnerInt").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + "<java.lang.Short>.InnerGeneric<java.lang.Long>");
  }

  /**
   * verifies that a static method with generic parameters has its type name properly
   * constructed and reflected in the class name.
   */
  @Test public void innerStaticInGenericType() throws Exception {
    Method staticInGeneric = getClass().getDeclaredMethod("testNestedNonGeneric");
    TypeName.get(staticInGeneric.getReturnType());
    TypeName typeName = TypeName.get(staticInGeneric.getGenericReturnType());

    // Make sure there are no generic arguments
    assertThat(typeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + ".NestedNonGeneric");
  }

  /**
   * verifies that two objects of primitive types have the same value and returns their
   * hash code.
   */
  @Test public void equalsAndHashCodePrimitive() {
    assertEqualsHashCodeAndToString(TypeName.BOOLEAN, TypeName.BOOLEAN);
    assertEqualsHashCodeAndToString(TypeName.BYTE, TypeName.BYTE);
    assertEqualsHashCodeAndToString(TypeName.CHAR, TypeName.CHAR);
    assertEqualsHashCodeAndToString(TypeName.DOUBLE, TypeName.DOUBLE);
    assertEqualsHashCodeAndToString(TypeName.FLOAT, TypeName.FLOAT);
    assertEqualsHashCodeAndToString(TypeName.INT, TypeName.INT);
    assertEqualsHashCodeAndToString(TypeName.LONG, TypeName.LONG);
    assertEqualsHashCodeAndToString(TypeName.SHORT, TypeName.SHORT);
    assertEqualsHashCodeAndToString(TypeName.VOID, TypeName.VOID);
  }

  /**
   * compares and verifies the hash codes and toString representations of two types:
   * `ArrayTypeName.of(Object.class)` and `TypeName.get(Object[].class)`.
   */
  @Test public void equalsAndHashCodeArrayTypeName() {
    assertEqualsHashCodeAndToString(ArrayTypeName.of(Object.class),
        ArrayTypeName.of(Object.class));
    assertEqualsHashCodeAndToString(TypeName.get(Object[].class),
        ArrayTypeName.of(Object.class));
  }

  /**
   * tests whether the class names of `Object`, `TypeName.get(Object.class)`, and
   * `ClassName.bestGuess("java.lang.Object")` are equal, have the same hash code, and
   * can be represented as a string.
   */
  @Test public void equalsAndHashCodeClassName() {
    assertEqualsHashCodeAndToString(ClassName.get(Object.class), ClassName.get(Object.class));
    assertEqualsHashCodeAndToString(TypeName.get(Object.class), ClassName.get(Object.class));
    assertEqualsHashCodeAndToString(ClassName.bestGuess("java.lang.Object"),
        ClassName.get(Object.class));
  }

  /**
   * tests the ` equals`, `hashCode`, and `toString` methods of `ParameterizedTypeName`
   * instances, comparing them to known values for different types.
   */
  @Test public void equalsAndHashCodeParameterizedTypeName() {
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(Object.class),
        ParameterizedTypeName.get(Object.class));
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(Set.class, UUID.class),
        ParameterizedTypeName.get(Set.class, UUID.class));
    assertNotEquals(ClassName.get(List.class), ParameterizedTypeName.get(List.class,
        String.class));
  }

  /**
   * tests whether two instances of `TypeVariableName` have the same value and hash
   * code by comparing them with each other and a reference implementation.
   */
  @Test public void equalsAndHashCodeTypeVariableName() {
    assertEqualsHashCodeAndToString(TypeVariableName.get(Object.class),
        TypeVariableName.get(Object.class));
    TypeVariableName typeVar1 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeVariableName typeVar2 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    assertEqualsHashCodeAndToString(typeVar1, typeVar2);
  }

  /**
   * tests equality and hash code generation for wildcard type names using subtyping
   * and supertyping relationships.
   */
  @Test public void equalsAndHashCodeWildcardTypeName() {
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Object.class),
        WildcardTypeName.subtypeOf(Object.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Serializable.class),
        WildcardTypeName.subtypeOf(Serializable.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.supertypeOf(String.class),
        WildcardTypeName.supertypeOf(String.class));
  }

  /**
   * tests whether a type is primitive or not.
   */
  @Test public void isPrimitive() throws Exception {
    assertThat(TypeName.INT.isPrimitive()).isTrue();
    assertThat(ClassName.get("java.lang", "Integer").isPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "String").isPrimitive()).isFalse();
    assertThat(TypeName.VOID.isPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Void").isPrimitive()).isFalse();
  }

  /**
   * tests whether a type is boxed primitive or not. It checks if the type is marked
   * as boxed primitive using an annotation, and returns a boolean indicating whether
   * it is.
   */
  @Test public void isBoxedPrimitive() throws Exception {
    assertThat(TypeName.INT.isBoxedPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Integer").isBoxedPrimitive()).isTrue();
    assertThat(ClassName.get("java.lang", "String").isBoxedPrimitive()).isFalse();
    assertThat(TypeName.VOID.isBoxedPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Void").isBoxedPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Integer")
            .annotated(ANNOTATION_SPEC).isBoxedPrimitive()).isTrue();
  }

  /**
   * verifies that the annotation of a primitive type is correctly boxed to a corresponding
   * annotated class in Java.
   */
  @Test public void canBoxAnnotatedPrimitive() throws Exception {
    assertThat(TypeName.BOOLEAN.annotated(ANNOTATION_SPEC).box()).isEqualTo(
            ClassName.get("java.lang", "Boolean").annotated(ANNOTATION_SPEC));
  }

  /**
   * tests whether an annotated primitive can be successfully unboxed to its original
   * type.
   */
  @Test public void canUnboxAnnotatedPrimitive() throws Exception {
    assertThat(ClassName.get("java.lang", "Boolean").annotated(ANNOTATION_SPEC)
            .unbox()).isEqualTo(TypeName.BOOLEAN.annotated(ANNOTATION_SPEC));
  }

  /**
   * verifies that two objects have the same toString representation, equal value, and
   * distinct hash codes.
   * 
   * @param a 1st object to be compared with the `b` object in the function.
   * 
   * 	- `TypeName`: This is the class that represents the type of the object being tested.
   * 	- `toString()`: The `toString()` method returns a string representation of the object.
   * 	- `equals(Object)`: The `equals()` method compares the object with another object
   * for equality.
   * 	- `hashCode()`: The `hashCode()` method returns an integer value that represents
   * the object's hash code.
   * 	- `null`: The `null` value is used to test whether the object being tested is
   * equal to null or not.
   * 
   * @param b 2nd object to be compared with `a`, and is used in the assertion statements
   * to verify that `a.toString()`, `a.equals(b)`, and `a.hashCode()` all return expected
   * values.
   * 
   * 	- `TypeName` is a class that represents a type name in Java. It has various
   * attributes such as `toString()`, `equals()`, and `hashCode()`.
   * 	- The `assertEquals()` method is used to compare the values of two objects, and
   * it throws an `AssertionError` if the comparison fails.
   * 	- The `assertThat()` method is used to check that a given object is equal to
   * another object, and it throws an `AssertionError` if the comparison fails.
   * 	- The `hashCode()` method returns the hash code of the object, which is used for
   * comparing objects in Java.
   * 	- The `equals()` method compares two objects for equality, and it returns a boolean
   * value indicating whether the objects are equal or not.
   * 	- The `assertFalse()` method asserts that a given condition is false, and it
   * throws an `AssertionError` if the condition is not met.
   * 
   * In summary, the `assertEqualsHashCodeAndToString` function checks that two
   * deserialized inputs have the same toString(), are equal to each other using the
   * `equals()` method, have the same hash code using the `hashCode()` method, and are
   * not equal to null using the `equals(null)` method.
   */
  private void assertEqualsHashCodeAndToString(TypeName a, TypeName b) {
    assertEquals(a.toString(), b.toString());
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertFalse(a.equals(null));
  }
}
