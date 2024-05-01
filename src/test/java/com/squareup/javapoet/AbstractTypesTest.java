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

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.junit.Assert.*;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.junit.Test;

/**
 * tests various features of the Type system in Java, including type mirroring,
 * primitive types, array types, void type, null type, parameterized types, wildcard
 * types, and boxing and unboxing. The test cases cover a range of scenarios, such
 * as retrieving type variables, getting primitive types, getting array types, and more.
 */
public abstract class AbstractTypesTest {
  protected abstract Elements getElements();
  protected abstract Types getTypes();

  /**
   * retrieves a `TypeElement` object representing a class by its canonical name. It
   * does so by calling the `Elements` instance and passing in the class's canonical
   * name as a parameter.
   * 
   * @param clazz Class object to which the TypeElement is to be retrieved.
   * 
   * 	- `Class<?>`: Represents a class in Java. The `clazz` parameter passed to the
   * function is an instance of this class.
   * 	- `getCanonicalName()`: Returns the canonical name of the class, which is a unique
   * identifier for the class.
   * 	- `Elements`: A collection of type elements that represent classes and interfaces
   * in the Java programming language. The `getElements()` method returns a reference
   * to this collection.
   * 
   * @returns a `TypeElement` object representing the class specified by the input
   * parameter `clazz`.
   * 
   * 	- The function returns a `TypeElement` object. This represents a type element in
   * the Java programming language, which can be a class, interface, method, or field.
   * 	- The `TypeElement` object is obtained by calling the
   * `getElements().getTypeElement(clazz.getCanonicalName());` method. This method
   * retrieves a `TypeElement` object for the given class name.
   * 	- The `TypeElement` object contains information about the type element, such as
   * its name, signature, and enclosing element (i.e., the class or interface where it
   * is defined).
   */
  private TypeElement getElement(Class<?> clazz) {
    return getElements().getTypeElement(clazz.getCanonicalName());
  }

  /**
   * retrieves a `TypeMirror` object representing the class passed as argument, by
   * calling the `getElement` function and casting its result to a `Type`.
   * 
   * @param clazz Class object that is to be mirrored.
   * 
   * 	- `Class<?>`: This is the type of the input parameter, which represents a class
   * or interface in Java.
   * 	- `getElement()`: This method returns an object of type `TypeElement`, which
   * represents a class or interface in the Java programming language.
   * 	- `asType()`: The `asType()` method converts the `TypeElement` object into a
   * `TypeMirror`, which is a representation of the original class or interface in the
   * reflection API.
   * 
   * @returns a `TypeMirror` object representing the class passed as parameter.
   * 
   * 	- The output is a `TypeMirror`, which represents a type in the program.
   * 	- The `TypeMirror` object can be used to access various information about the
   * type, such as its name, erasure level, and bounds.
   * 	- The `asType()` method returns the `TypeMirror` representation of the given
   * `Class<?>` parameter.
   */
  private TypeMirror getMirror(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  /**
   * verifies that the type mirrors of objects, characters, and a subclass are equal
   * to their corresponding class names.
   */
  @Test public void getBasicTypeMirror() {
    assertThat(TypeName.get(getMirror(Object.class)))
        .isEqualTo(ClassName.get(Object.class));
    assertThat(TypeName.get(getMirror(Charset.class)))
        .isEqualTo(ClassName.get(Charset.class));
    assertThat(TypeName.get(getMirror(AbstractTypesTest.class)))
        .isEqualTo(ClassName.get(AbstractTypesTest.class));
  }

  /**
   * retrieves a declared type mirror for a set of objects, returning a parameterized
   * type mirror with the set class as the container and Object as the generic type.
   */
  @Test public void getParameterizedTypeMirror() {
    DeclaredType setType =
        getTypes().getDeclaredType(getElement(Set.class), getMirror(Object.class));
    assertThat(TypeName.get(setType))
        .isEqualTo(ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.OBJECT));
  }

  /**
   * tests whether the `@SuppressWarnings` annotation is properly processed by the Java
   * compiler. It does this by checking the types of the class and its fields, and
   * asserting that they match expected values.
   */
  @Test public void errorTypes() {
    JavaFileObject hasErrorTypes =
        JavaFileObjects.forSourceLines(
            "com.squareup.tacos.ErrorTypes",
            "package com.squareup.tacos;",
            "",
            "@SuppressWarnings(\"hook-into-compiler\")",
            "class ErrorTypes {",
            "  Tacos tacos;",
            "  Ingredients.Guacamole guacamole;",
            "}");
    Compilation compilation = javac().withProcessors(new AbstractProcessor() {
      /**
       * analyzes the type elements within a given Java class and returns a boolean value
       * indicating whether the top-level error type and member error type are equal to
       * "Tacos" and "Guacamole", respectively.
       * 
       * @param set set of type elements that are being analyzed for the presence of error
       * types.
       * 
       * 	- `set` is a set of type elements that contain the definitions of various types
       * in the program being analyzed.
       * 	- The elements in `set` are of type `TypeElement`, which represents a type
       * definition in the program.
       * 	- The function takes two parameters: `roundEnvironment` and `processingEnv`. These
       * parameters provide information about the analysis environment, such as the location
       * of the types in the program and the API used for type checking.
       * 
       * In summary, `set` is a set of type elements that contain definitions of various
       * types in the program being analyzed, and the function processes these elements to
       * extract information about the types.
       * 
       * @param roundEnvironment collection of all classes and interfaces visible to the
       * current processing context, which is used by the function to identify and extract
       * relevant type information.
       * 
       * 	- `processingEnv`: The processing environment provides utilities for working with
       * elements in the Java source code.
       * 	- `getElementUtils()`: Returns a utility class that provides methods for querying
       * and manipulating type and variable elements in the source code.
       * 	- `set`: A set of type elements representing the types of classes, interfaces,
       * or other types defined in the source code.
       * 	- `roundEnvironment`: Represents the current round of compilation, providing
       * access to information about the classes, interfaces, and other types being compiled.
       * 
       * The function then processes the input `set` and returns a boolean value indicating
       * whether the processing was successful.
       * 
       * @returns a boolean value indicating that the analysis was successful.
       * 
       * 	- The input set contains TypeElements representing classes, interfaces, and other
       * types.
       * 	- The `roundEnvironment` parameter represents the set of types encountered during
       * type checking.
       * 	- The `processingEnv` provides utilities for working with types and elements.
       * 	- The `getElementUtils()` method of the `processingEnv` returns an instance of
       * Elements, which contains methods for querying and manipulating types and elements.
       * 	- The `TypeElement` class represents a top-level type declaration in the source
       * code, such as a class, interface, or enum.
       * 	- The `List<VariableElement>` fieldsIn(Class<E>... classes) method returns a list
       * of all variable declarations declared within the specified classes.
       * 	- The `asType()` method converts a TypeElement to a full Type object, which can
       * be used for further manipulation or comparison.
       * 	- The `assertThat()` method is used to check that two objects are equal, throwing
       * an AssertionError if they are not.
       * 	- The `ClassName` class represents a class or interface name.
       * 
       * In summary, the `process` function takes a set of TypeElements as input and processes
       * them using the `roundEnvironment` and `processingEnv` parameters. It then returns
       * `false`, indicating that no errors were found in the input.
       */
      @Override
      public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        TypeElement classFile =
            processingEnv.getElementUtils().getTypeElement("com.squareup.tacos.ErrorTypes");
        List<VariableElement> fields = fieldsIn(classFile.getEnclosedElements());
        ErrorType topLevel = (ErrorType) fields.get(0).asType();
        ErrorType member = (ErrorType) fields.get(1).asType();

        assertThat(TypeName.get(topLevel)).isEqualTo(ClassName.get("", "Tacos"));
        assertThat(TypeName.get(member)).isEqualTo(ClassName.get("Ingredients", "Guacamole"));
        return false;
      }

      /**
       * returns a set containing only the String value "*". This indicates that the function
       * supports all types of annotations.
       * 
       * @returns a set containing only the string `"*"`, indicating that the function
       * supports all types of annotations.
       * 
       * 	- The returned Set contains only one element, which is "*" (a wildcard character).
       * 	- This indicates that the function supports all types of annotations.
       * 	- The absence of any other elements in the Set means that no other types of
       * annotations are supported.
       * 	- The use of a wildcard character allows for maximum flexibility in terms of the
       * types of annotations that can be used with this function.
       */
      @Override
      public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
      }
    }).compile(hasErrorTypes);

    assertThat(compilation).failed();
  }

  /**
   * is a generic class with five type parameters and three interfaces implemented,
   * defining a hierarchy of types that extend or implement various classes and interfaces.
   */
  static class Parameterized<
      Simple,
      ExtendsClass extends Number,
      ExtendsInterface extends Runnable,
      ExtendsTypeVariable extends Simple,
      Intersection extends Number & Runnable,
      IntersectionOfInterfaces extends Runnable & Serializable> {}

  /**
   * tests the type mirrors of five type variables based on their declarations in a
   * Java class. It asserts that each type variable's mirror matches its declared name
   * and bounds.
   */
  @Test public void getTypeVariableTypeMirror() {
    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();

    // Members of converted types use ClassName and not Class<?>.
    ClassName number = ClassName.get(Number.class);
    ClassName runnable = ClassName.get(Runnable.class);
    ClassName serializable = ClassName.get(Serializable.class);

    assertThat(TypeName.get(typeVariables.get(0).asType()))
        .isEqualTo(TypeVariableName.get("Simple"));
    assertThat(TypeName.get(typeVariables.get(1).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsClass", number));
    assertThat(TypeName.get(typeVariables.get(2).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsInterface", runnable));
    assertThat(TypeName.get(typeVariables.get(3).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsTypeVariable", TypeVariableName.get("Simple")));
    assertThat(TypeName.get(typeVariables.get(4).asType()))
        .isEqualTo(TypeVariableName.get("Intersection", number, runnable));
    assertThat(TypeName.get(typeVariables.get(5).asType()))
        .isEqualTo(TypeVariableName.get("IntersectionOfInterfaces", runnable, serializable));
    assertThat(((TypeVariableName) TypeName.get(typeVariables.get(4).asType())).bounds)
        .containsExactly(number, runnable);
  }

  /**
   * defines a recursive type parameter T with a generic constraint of Map<List<T>, Set<T[]>>.
   */
  static class Recursive<T extends Map<List<T>, Set<T[]>>> {}

  /**
   * returns a TypeMirror representing the type of a recursive class, and then checks
   * the bounds of a type variable within that type.
   */
  @Test
  public void getTypeVariableTypeMirrorRecursive() {
    TypeMirror typeMirror = getElement(Recursive.class).asType();
    ParameterizedTypeName typeName = (ParameterizedTypeName) TypeName.get(typeMirror);
    String className = Recursive.class.getCanonicalName();
    assertThat(typeName.toString()).isEqualTo(className + "<T>");

    TypeVariableName typeVariableName = (TypeVariableName) typeName.typeArguments.get(0);

    try {
      typeVariableName.bounds.set(0, null);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    assertThat(typeVariableName.toString()).isEqualTo("T");
    assertThat(typeVariableName.bounds.toString())
        .isEqualTo("[java.util.Map<java.util.List<T>, java.util.Set<T[]>>]");
  }

  /**
   * tests whether the primitive type mirrors the expected types: BOOLEAN, BYTE, SHORT,
   * INT, LONG, CHAR, FLOAT and DOUBLE.
   */
  @Test public void getPrimitiveTypeMirror() {
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(TypeName.BOOLEAN);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(TypeName.BYTE);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(TypeName.SHORT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(TypeName.INT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(TypeName.LONG);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(TypeName.CHAR);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(TypeName.FLOAT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(TypeName.DOUBLE);
  }

  /**
   * verifies that the mirrored type of an array of objects is equal to the array type
   * of `Object`.
   */
  @Test public void getArrayTypeMirror() {
    assertThat(TypeName.get(getTypes().getArrayType(getMirror(Object.class))))
        .isEqualTo(ArrayTypeName.of(ClassName.OBJECT));
  }

  /**
   * asserts that the type mirror returned by `TypeName.get` for the no-type with
   * `TypeKind.VOID` is equal to `TypeName.VOID`.
   */
  @Test public void getVoidTypeMirror() {
    assertThat(TypeName.get(getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(TypeName.VOID);
  }

  /**
   * tests whether a `TypeName` can be created from a null type by calling
   * `get(getTypes().getNullType());` and verifying an `IllegalArgumentException` is
   * thrown when it fails.
   */
  @Test public void getNullTypeMirror() {
    try {
      TypeName.get(getTypes().getNullType());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * checks if a given `ParameterizedTypeName` object represents a map of two classes,
   * and returns a string representation of the type that matches the expected format.
   */
  @Test public void parameterizedType() throws Exception {
    ParameterizedTypeName type = ParameterizedTypeName.get(Map.class, String.class, Long.class);
    assertThat(type.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Long>");
  }

  /**
   * verifies that the `ArrayTypeName` object created from a String class produces
   * "java.lang.String[]" as its toString result.
   */
  @Test public void arrayType() throws Exception {
    ArrayTypeName type = ArrayTypeName.of(String.class);
    assertThat(type.toString()).isEqualTo("java.lang.String[]");
  }

  /**
   * tests whether a `WildcardTypeName` instance represents a subtype of `CharSequence`.
   * It does this by asserting that the resulting string is equal to `"?" + " extends
   * " + "java.lang.CharSequence".
   */
  @Test public void wildcardExtendsType() throws Exception {
    WildcardTypeName type = WildcardTypeName.subtypeOf(CharSequence.class);
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  /**
   * checks if a `WildcardTypeName` object is a subtype of `Object`. It does this by
   * calling `toString()` on the `WildcardTypeName` object and verifying that the
   * resulting string is equal to "?".
   */
  @Test public void wildcardExtendsObject() throws Exception {
    WildcardTypeName type = WildcardTypeName.subtypeOf(Object.class);
    assertThat(type.toString()).isEqualTo("?");
  }

  /**
   * determines the wildcard super type of a class and returns it as a string, which
   * is equivalent to "? super java.lang.String".
   */
  @Test public void wildcardSuperType() throws Exception {
    WildcardTypeName type = WildcardTypeName.supertypeOf(String.class);
    assertThat(type.toString()).isEqualTo("? super java.lang.String");
  }

  /**
   * tests whether a wildcard type's toString() method returns a valid and expected
   * value, specifically "?".
   */
  @Test public void wildcardMirrorNoBounds() throws Exception {
    WildcardType wildcard = getTypes().getWildcardType(null, null);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("?");
  }

  /**
   * extracts a wildcard type from an element's type, and then checks if the resulting
   * type name is equal to `"?" followed by "extends java.lang.CharSequence"`.
   */
  @Test public void wildcardMirrorExtendsType() throws Exception {
    Types types = getTypes();
    Elements elements = getElements();
    TypeMirror charSequence = elements.getTypeElement(CharSequence.class.getName()).asType();
    WildcardType wildcard = types.getWildcardType(charSequence, null);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  /**
   * tests whether a wildcard type can be used to reflect a subtype of an existing type.
   * It does so by creating a wildcard type and comparing its toString representation
   * with the expected output, which is "? super java.lang.String".
   */
  @Test public void wildcardMirrorSuperType() throws Exception {
    Types types = getTypes();
    Elements elements = getElements();
    TypeMirror string = elements.getTypeElement(String.class.getName()).asType();
    WildcardType wildcard = types.getWildcardType(null, string);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("? super java.lang.String");
  }

  /**
   * checks if the toString method of a TypeVariableName object returns the expected
   * value, which is the name of the type variable ("T" in this case).
   */
  @Test public void typeVariable() throws Exception {
    TypeVariableName type = TypeVariableName.get("T", CharSequence.class);
    assertThat(type.toString()).isEqualTo("T"); // (Bounds are only emitted in declaration.)
  }

  /**
   * tests whether the result of calling `TypeName.box()` is equal to the expected class
   * name for various types, including primitive and reference types.
   */
  @Test public void box() throws Exception {
    assertThat(TypeName.INT.box()).isEqualTo(ClassName.get(Integer.class));
    assertThat(TypeName.VOID.box()).isEqualTo(ClassName.get(Void.class));
    assertThat(ClassName.get(Integer.class).box()).isEqualTo(ClassName.get(Integer.class));
    assertThat(ClassName.get(Void.class).box()).isEqualTo(ClassName.get(Void.class));
    assertThat(TypeName.OBJECT.box()).isEqualTo(TypeName.OBJECT);
    assertThat(ClassName.get(String.class).box()).isEqualTo(ClassName.get(String.class));
  }

  /**
   * tests whether a type name can be unboxed to its corresponding primitive type, or
   * throws an exception if it cannot be unboxed. It also checks for invalid type names
   * and throws exceptions accordingly.
   */
  @Test public void unbox() throws Exception {
    assertThat(TypeName.INT).isEqualTo(TypeName.INT.unbox());
    assertThat(TypeName.VOID).isEqualTo(TypeName.VOID.unbox());
    assertThat(ClassName.get(Integer.class).unbox()).isEqualTo(TypeName.INT.unbox());
    assertThat(ClassName.get(Void.class).unbox()).isEqualTo(TypeName.VOID.unbox());
    try {
      TypeName.OBJECT.unbox();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    try {
      ClassName.get(String.class).unbox();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }
}
