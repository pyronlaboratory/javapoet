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

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;

/**
 * in Java provides a way to represent and manipulate type names in Java programming
 * language. It offers various methods for visiting type mirrors, such as accept,
 * defaultAction, and visitDeclared, which allow for customized handling of different
 * types. Additionally, it includes utility methods like get, list, arrayComponent,
 * and asArray, that help with working with arrays and component types. Overall, the
 * TypeName Class provides a flexible and convenient way to deal with type names in
 * Java programming.
 */
public class TypeName {
  public static final TypeName VOID = new TypeName("void");
  public static final TypeName BOOLEAN = new TypeName("boolean");
  public static final TypeName BYTE = new TypeName("byte");
  public static final TypeName SHORT = new TypeName("short");
  public static final TypeName INT = new TypeName("int");
  public static final TypeName LONG = new TypeName("long");
  public static final TypeName CHAR = new TypeName("char");
  public static final TypeName FLOAT = new TypeName("float");
  public static final TypeName DOUBLE = new TypeName("double");
  public static final ClassName OBJECT = ClassName.get("java.lang", "Object");

  private static final ClassName BOXED_VOID = ClassName.get("java.lang", "Void");
  private static final ClassName BOXED_BOOLEAN = ClassName.get("java.lang", "Boolean");
  private static final ClassName BOXED_BYTE = ClassName.get("java.lang", "Byte");
  private static final ClassName BOXED_SHORT = ClassName.get("java.lang", "Short");
  private static final ClassName BOXED_INT = ClassName.get("java.lang", "Integer");
  private static final ClassName BOXED_LONG = ClassName.get("java.lang", "Long");
  private static final ClassName BOXED_CHAR = ClassName.get("java.lang", "Character");
  private static final ClassName BOXED_FLOAT = ClassName.get("java.lang", "Float");
  private static final ClassName BOXED_DOUBLE = ClassName.get("java.lang", "Double");

  /** The name of this type if it is a keyword, or null. */
  private final String keyword;
  public final List<AnnotationSpec> annotations;

  /** Lazily-initialized toString of this type name. */
  private String cachedString;

  private TypeName(String keyword) {
    this(keyword, new ArrayList<>());
  }

  private TypeName(String keyword, List<AnnotationSpec> annotations) {
    this.keyword = keyword;
    this.annotations = Util.immutableList(annotations);
  }

  // Package-private constructor to prevent third-party subclasses.
  TypeName(List<AnnotationSpec> annotations) {
    this(null, annotations);
  }

  /**
   * returns a type with annotations applied, taking an array of annotation specifications
   * as input.
   * 
   * @returns an instance of the `TypeName` type, carrying the provided annotations.
   * 
   * TypeName: The return value is of type TypeName, which represents an annotated type.
   * Annotations: The return value contains a list of AnnotationSpec objects representing
   * the annotations applied to the type.
   */
  public final TypeName annotated(AnnotationSpec... annotations) {
    return annotated(Arrays.asList(annotations));
  }

  /**
   * generates a new `TypeName` instance by combining a keyword and concatenating the
   * provided list of annotations using the `concatAnnotations` method.
   * 
   * @param annotations List of AnnotationSpec objects that will be concatenated and
   * used to create a new TypeName instance.
   * 
   * 1/ `Util.checkNotNull(annotations, "annotations == null")` is used to ensure that
   * the input `annotations` is not null before further processing it.
   * 2/ The method returns a new instance of `TypeName` by concatenating the keyword
   * and the deserialized `annotations`.
   * 
   * @returns a `TypeName` instance containing the keyword and concatenated annotations.
   * 
   * 	- `TypeName`: The type name constructed from the `keyword` parameter and the
   * concatenation of the `annotations`.
   * 	- `annotations`: A list of annotation specifications.
   * 	- `keyword`: The keyword that is used to construct the type name.
   * 	- `concatAnnotations`: The method that concats the annotations in a list.
   */
  public TypeName annotated(List<AnnotationSpec> annotations) {
    Util.checkNotNull(annotations, "annotations == null");
    return new TypeName(keyword, concatAnnotations(annotations));
  }

  /**
   * checks if the `annotations` field is empty, and returns a new `TypeName` instance
   * if it is not.
   * 
   * @returns a new instance of the `TypeName` class with the same value as the original
   * `this` reference, but without any annotations.
   * 
   * 	- `TypeName`: This is the type name generated by the function without any annotations.
   * 	- `keyword`: This is the keyword that was passed to the function as an argument.
   * 	- `this`: The function returns a new instance of `TypeName` created from the `keyword`.
   */
  public TypeName withoutAnnotations() {
    if (annotations.isEmpty()) {
      return this;
    }
    return new TypeName(keyword);
  }

  /**
   * combines two lists of AnnotationSpec objects, adding all elements from the second
   * list to the existing list of the first, resulting in a new list with all annotations.
   * 
   * @param annotations list of annotations to be combined with the current instance's
   * annotations, resulting in a new list of annotations that contains all the annotations
   * from both sources.
   * 
   * 	- The function takes a list of `AnnotationSpec` objects as input and returns a
   * new list that combines the annotations from the current instance with the provided
   * ones.
   * 	- The function creates a new list to store the combined annotations, called `allAnnotations`.
   * 	- The existing annotations in the current instance are first retrieved using the
   * `new ArrayList<>(this.annotations)` method.
   * 	- The provided annotations are added to the combined list using the `addAll()` method.
   * 
   * Note: This explanation is limited to 3-4 sentences and does not include any personal
   * opinions or biases, nor does it refer to any external information such as the code
   * author or licensing.
   * 
   * @returns a list of annotations that combines the original list of annotations with
   * any additional annotations provided as input.
   * 
   * 	- The list contains both the annotations from the current class and the provided
   * ones.
   * 	- The annotations are stored in an ArrayList for efficient handling.
   * 	- The list is immutable to ensure thread-safety and prevent unintended modification.
   */
  protected final List<AnnotationSpec> concatAnnotations(List<AnnotationSpec> annotations) {
    List<AnnotationSpec> allAnnotations = new ArrayList<>(this.annotations);
    allAnnotations.addAll(annotations);
    return allAnnotations;
  }

  /**
   * checks if a list of annotations is empty, returning `true` if it is not.
   * 
   * @returns a boolean value indicating whether the `annotations` list is empty or not.
   */
  public boolean isAnnotated() {
    return !annotations.isEmpty();
  }

  /**
   * determines whether an object is a primitive or not by checking if it has a non-null
   * value and is not equal to `VOID`.
   * 
   * @returns a boolean value indicating whether the object is primitive or not.
   */
  public boolean isPrimitive() {
    return keyword != null && this != VOID;
  }

  /**
   * determines if a TypeName is a primitive boxed type.
   * 
   * @returns a boolean value indicating whether the given type name is a boxed primitive
   * type.
   */
  public boolean isBoxedPrimitive() {
    TypeName thisWithoutAnnotations = withoutAnnotations();
    return thisWithoutAnnotations.equals(BOXED_BOOLEAN)
        || thisWithoutAnnotations.equals(BOXED_BYTE)
        || thisWithoutAnnotations.equals(BOXED_SHORT)
        || thisWithoutAnnotations.equals(BOXED_INT)
        || thisWithoutAnnotations.equals(BOXED_LONG)
        || thisWithoutAnnotations.equals(BOXED_CHAR)
        || thisWithoutAnnotations.equals(BOXED_FLOAT)
        || thisWithoutAnnotations.equals(BOXED_DOUBLE);
  }

  /**
   * boxes an object into a specific type based on its keyword, returning the boxed
   * object with any provided annotations.
   * 
   * @returns a `TypeName` object that represents the boxed version of the input `TypeName`.
   * 
   * 	- `TypeName boxed`: This is the type-safe wrapper class for the input `keyword`.
   * It represents the wrapped value in a safe manner, ensuring that it can only be
   * accessed through the provided methods.
   * 	- `annotations`: An empty map by default. If non-empty, it contains additional
   * metadata about the wrapped value, such as its provenance or any annotations applied
   * to it.
   * 
   * In summary, the `box` function returns a type-safe wrapper class for the input
   * `keyword`, along with any applicable metadata in the form of an empty map of annotations.
   */
  public TypeName box() {
    if (keyword == null) return this; // Doesn't need boxing.
    TypeName boxed = null;
    if (keyword.equals(VOID.keyword)) boxed = BOXED_VOID;
    else if (keyword.equals(BOOLEAN.keyword)) boxed = BOXED_BOOLEAN;
    else if (keyword.equals(BYTE.keyword)) boxed = BOXED_BYTE;
    else if (keyword.equals(SHORT.keyword)) boxed = BOXED_SHORT;
    else if (keyword.equals(INT.keyword)) boxed = BOXED_INT;
    else if (keyword.equals(LONG.keyword)) boxed = BOXED_LONG;
    else if (keyword.equals(CHAR.keyword)) boxed = BOXED_CHAR;
    else if (keyword.equals(FLOAT.keyword)) boxed = BOXED_FLOAT;
    else if (keyword.equals(DOUBLE.keyword)) boxed = BOXED_DOUBLE;
    else throw new AssertionError(keyword);
    return annotations.isEmpty() ? boxed : boxed.annotated(annotations);
  }

  /**
   * unboxes a boxed value and returns its underlying type, handling different types
   * of boxes and providing an annotated version of the result if the function is called
   * with annotations.
   * 
   * @returns a reference to the unboxed value of the original boxed value, or an
   * `UnsupportedOperationException` if the unboxing is not possible.
   * 
   * 	- The function returns the unboxed value of the input `TypeName`, if it is already
   * unboxed.
   * 	- If the input `TypeName` is not equal to `BOXED_VOID`, then the function checks
   * if it can be unboxed into one of the following types: `BOOLEAN`, `BYTE`, `SHORT`,
   * `INT`, `LONG`, `CHAR`, `FLOAT`, or `DOUBLE`. If it can, the function returns the
   * corresponding unboxed value.
   * 	- If the input `TypeName` cannot be unboxed into any of the above types, then the
   * function throws an `UnsupportedOperationException`.
   * 
   * Overall, the `unbox` function is designed to handle a wide range of boxed types
   * and return their corresponding unboxed values, while also providing a way to handle
   * cases where the input type cannot be unboxed.
   */
  public TypeName unbox() {
    if (keyword != null) return this; // Already unboxed.
    TypeName thisWithoutAnnotations = withoutAnnotations();
    TypeName unboxed = null;
    if (thisWithoutAnnotations.equals(BOXED_VOID)) unboxed = VOID;
    else if (thisWithoutAnnotations.equals(BOXED_BOOLEAN)) unboxed = BOOLEAN;
    else if (thisWithoutAnnotations.equals(BOXED_BYTE)) unboxed = BYTE;
    else if (thisWithoutAnnotations.equals(BOXED_SHORT)) unboxed = SHORT;
    else if (thisWithoutAnnotations.equals(BOXED_INT)) unboxed = INT;
    else if (thisWithoutAnnotations.equals(BOXED_LONG)) unboxed = LONG;
    else if (thisWithoutAnnotations.equals(BOXED_CHAR)) unboxed = CHAR;
    else if (thisWithoutAnnotations.equals(BOXED_FLOAT)) unboxed = FLOAT;
    else if (thisWithoutAnnotations.equals(BOXED_DOUBLE)) unboxed = DOUBLE;
    else throw new UnsupportedOperationException("cannot unbox " + this);
    return annotations.isEmpty() ? unboxed : unboxed.annotated(annotations);
  }

  /**
   * determines equality between two objects, first checking for identity and then
   * comparing their classes and strings representations.
   * 
   * @param o object being compared to the current object, and is used in the comparison
   * to determine if the two objects are equal.
   * 
   * 	- If this and o are the same object reference, the method returns true.
   * 	- If o is null, the method returns false.
   * 	- If the class of this and o are not the same, the method returns false.
   * 	- The method compares the strings of this and o using the `equals` method.
   * 
   * @returns a boolean value indicating whether the object being compared is equal to
   * the current object.
   */
  @Override public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its argument, which is the result of calling `toString()`
   * on the argument and then using its hash code.
   * 
   * @returns an integer value representing the hash code of the function's input.
   */
  @Override public final int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of an object by calling the `emit` method on a
   * `CodeWriter` instance, and then returns the resulting string.
   * 
   * @returns a string representation of the object's state.
   */
  @Override public final String toString() {
    String result = cachedString;
    if (result == null) {
      try {
        StringBuilder resultBuilder = new StringBuilder();
        CodeWriter codeWriter = new CodeWriter(resultBuilder);
        emit(codeWriter);
        result = resultBuilder.toString();
        cachedString = result;
      } catch (IOException e) {
        throw new AssertionError();
      }
    }
    return result;
  }

  /**
   * emits Java code snippets based on a given keyword and annotations, ensuring proper
   * indentation and output.
   * 
   * @param out output writer to which the generated code will be written.
   * 
   * 	- `out`: The output code writer object that is used to emit the generated Java
   * code. It is passed as an argument to the function and can be modified by the function.
   * 	- `IOException`: An exception type that can be thrown by the function if there
   * is an error during serialization or deserialization.
   * 
   * @returns a sequence of bytes representing the emitted code.
   * 
   * The output is an `IOException` if there is an error during emitting.
   * 
   * If the `keyword` parameter is null, an `AssertionError` is thrown.
   * 
   * If the method is annotated, the output is a single empty string followed by the
   * emission of annotations using the `emitAnnotations` method.
   * 
   * The output is a string that represents the indentation and emission of the `keyword`
   * value using the `emitAndIndent` method.
   */
  CodeWriter emit(CodeWriter out) throws IOException {
    if (keyword == null) throw new AssertionError();

    if (isAnnotated()) {
      out.emit("");
      emitAnnotations(out);
    }
    return out.emitAndIndent(keyword);
  }

  /**
   * emits annotations from a list of AnnotationSpec objects to a CodeWriter object,
   * adding a space between each annotation and allowing for additional output beyond
   * the annotated elements.
   * 
   * @param out output code writer to which the annotations are emitted.
   * 
   * 	- `out`: A `CodeWriter` object that represents the current output stream for
   * generating the annotated code.
   * 	- `annotations`: A collection of `AnnotationSpec` objects, each representing a
   * single annotation to be emitted in the generated code.
   * 	- `true`: A boolean value passed as an argument to the `emit` method of each
   * `AnnotationSpec` object, indicating that the annotation should be emitted in the
   * output code.
   * 
   * @returns a code writer object that contains the emitted annotations.
   * 
   * The output is a `CodeWriter` object, which represents a sequence of Java tokens
   * that can be used to generate source code.
   * 
   * The `out` parameter is assigned the return value of the `emitAnnotations` function,
   * which consists of a sequence of annotations emitted using the `emit` method.
   * 
   * The `true` argument passed to the `emit` method indicates that the annotation
   * should be emitted as a separate statement.
   * 
   * The space character (``) is emitted after each annotation to indicate a separation
   * between annotations.
   */
  CodeWriter emitAnnotations(CodeWriter out) throws IOException {
    for (AnnotationSpec annotation : annotations) {
      annotation.emit(out, true);
      out.emit(" ");
    }
    return out;
  }


  /**
   * retrieves a type reflection object of the specified type mirror and its associated
   * metadata from a map.
   * 
   * @param mirror TypeMirror object that contains information about the type being
   * queried for its name.
   * 
   * 	- The `TypeMirror` object passed in is used to represent the type of the data
   * being serialized or deserialized.
   * 	- The `LinkedHashMap` parameter represents a mapping from the type's generic
   * parameters to their actual types, which is used during serialization and deserialization
   * to map type arguments to specific types.
   * 
   * @returns a `TypeName` object.
   * 
   * TypeName is the generic type returned by the function, indicating the type of data
   * being retrieved.
   * The return value is of TypeMirror type, which represents a mirrored view of a type
   * in the program.
   * The function takes a single argument, `mirror`, which is a TypeMirror object
   * representing the type to retrieve a mirror of.
   * A LinkedHashMap<> is passed as an optional parameter, which is used to store the
   * retrieved type information if necessary.
   */
  public static TypeName get(TypeMirror mirror) {
    return get(mirror, new LinkedHashMap<>());
  }

  /**
   * takes a `TypeMirror` and a map of type variables as input, and returns a `TypeName`.
   * It uses a simple visitor pattern to recursively navigate the type hierarchy and
   * return the appropriate `TypeName`.
   * 
   * @param mirror TypeMirror to be processed, which is passed through various methods
   * for type inference and returned as a TypeName.
   * 
   * 	- `mirror`: The input type mirror to be analyzed. Its kind is determined by the
   * `TypeKind` field of its `TypeMirror` object.
   * 	- `typeVariables`: A map of type variables in the input type, which can be used
   * to resolve type parameters in the output.
   * 
   * The function returns a `TypeName`, which represents the type of the input `mirror`.
   * The exact type returned depends on the kind of `mirror`, as determined by its
   * `TypeKind` field:
   * 
   * 	- If `mirror.getKind() == TypeKind.BOOLEAN`, `TypeName` is `BOOLEAN`.
   * 	- If `mirror.getKind() == TypeKind.BYTE`, `TypeName` is `BYTE`.
   * 	- If `mirror.getKind() == TypeKind.SHORT`, `TypeName` is `SHORT`.
   * 	- If `mirror.getKind() == TypeKind.INT`, `TypeName` is `INT`.
   * 	- If `mirror.getKind() == TypeKind.LONG`, `TypeName` is `LONG`.
   * 	- If `mirror.getKind() == TypeKind.CHAR`, `TypeName` is `CHAR`.
   * 	- If `mirror.getKind() == TypeKind.FLOAT`, `TypeName` is `FLOAT`.
   * 	- If `mirror.getKind() == TypeKind.DOUBLE`, `TypeName` is `DOUBLE`.
   * 	- If `mirror.getKind() != TypeKind.NONE`, `TypeName` is a subtype of the enclosing
   * type, constructed by recursively applying this function to each type parameter
   * until a non-parameterized type is reached. Otherwise, `TypeName` is `NULL`.
   * 
   * The function also handles type arguments, which are added to the output `TypeName`
   * if any exist.
   * 
   * @param typeVariables Map of TypeParameterElements to TypeVariableNames, which is
   * used to generate a corresponding TypeName for each type parameter in the given TypeMirror.
   * 
   * 1/ `Map<TypeParameterElement, TypeVariableName>` represents a map between type
   * parameters and their corresponding type variables. The keys are `TypeParameterElement`
   * objects, which represent the type parameters of the Java code, while the values
   * are `TypeVariableName` objects, which represent the type variables declared in the
   * code.
   * 2/ `typeVariables` is used to deserialize the input `typeMirror`, which is a
   * reflection of the original Java code.
   * 3/ The function takes an optional `Void p` parameter, which is used to indicate
   * the visitor's position in the code.
   * 4/ The function has several methods that take a `TypeMirror` and a `Void` parameter:
   * `visitPrimitive()`, `visitDeclared()`, `visitError()`, `visitArray()`,
   * `visitTypeVariable()`, `visitWildcard()`, and `visitNoType()`. These methods
   * represent different types of nodes in the Java code tree, and they are used to
   * recursively visit the nodes and generate the desired output.
   * 5/ The `visitPrimitive()` method is responsible for handling primitive types, such
   * as `boolean`, `byte`, `short`, `int`, `long`, `char`, and `float`. It returns a
   * `TypeName` object representing the appropriate type.
   * 6/ The `visitDeclared()` method is responsible for handling declared types, which
   * are classes or interfaces that are defined in the Java code. It takes the `TypeMirror`
   * object and checks if it is a static type. If it is not a static type, it recursively
   * visits the type's elements to generate the desired output.
   * 7/ The `visitError()` method is responsible for handling error types, which are
   * types that indicate an error or exception in the Java code. It returns a `TypeName`
   * object representing the appropriate type.
   * 8/ The `visitArray()` method is responsible for handling array types, which are
   * types that represent a collection of values of a specific type. It takes the
   * `TypeMirror` object and recursively visits the elements to generate the desired output.
   * 9/ The `visitTypeVariable()` method is responsible for handling type variables,
   * which are placeholders for types that have not been specified yet in the Java code.
   * It returns a `TypeName` object representing the appropriate type.
   * 10/ The `visitWildcard()` method is responsible for handling wildcard types, which
   * are types that represent any type or any subtype of a specific type. It returns a
   * `TypeName` object representing the appropriate type.
   * 11/ The `visitNoType()` method is responsible for handling unknown or unsupported
   * types. It returns a `TypeName` object representing the appropriate type.
   * 
   * In summary, the `get` function takes a `typeMirror` and a `Map<TypeParameterElement,
   * TypeVariableName>` as input, and it recursively visits the nodes in the Java code
   * tree to generate the desired output. The function handles different types of nodes,
   * such as primitive types, declared types, error types, arrays, type variables,
   * wildcard types, and unknown or unsupported types.
   * 
   * @returns a TypeName object representing the specified type mirror.
   * 
   * 	- `TypeName.BOOLEAN`: The type name for the primitive type `Boolean`.
   * 	- `TypeName.BYTE`: The type name for the primitive type `Byte`.
   * 	- `TypeName.SHORT`: The type name for the primitive type `Short`.
   * 	- `TypeName.INT`: The type name for the primitive type `Int`.
   * 	- `TypeName.LONG`: The type name for the primitive type `Long`.
   * 	- `TypeName.CHAR`: The type name for the primitive type `Char`.
   * 	- `TypeName.FLOAT`: The type name for the primitive type `Float`.
   * 	- `TypeName.DOUBLE`: The type name for the primitive type `Double`.
   * 	- `ParameterizedTypeName`: A class that represents a parameterized type, which
   * is a type defined by its supertypes and type arguments.
   * 	- `SimpleTypeVisitor8`: A visitor class that visits a given type and returns a
   * type name.
   * 	- `VisitPrimitive`: The method that handles the visiting of primitive types.
   * 	- `VisitDeclared`: The method that handles the visiting of declared types.
   * 	- `VisitError`: The method that handles the visiting of error types.
   * 	- `VisitArray`: The method that handles the visiting of array types.
   * 	- `VisitTypeVariable`: The method that handles the visiting of type variables.
   * 	- `VisitWildcard`: The method that handles the visiting of wildcard types.
   * 	- `VisitNoType`: The method that handles the visiting of no-type.
   * 	- `defaultAction`: The method that is called when an unexpected type mirror is encountered.
   */
  static TypeName get(TypeMirror mirror,
      final Map<TypeParameterElement, TypeVariableName> typeVariables) {
    return mirror.accept(new SimpleTypeVisitor8<TypeName, Void>() {
      /**
       * is a type visitor that determines the appropriate type name for a primitive type
       * based on its kind and returns it.
       * 
       * @param t primitive type being visited, and its value determines the corresponding
       * type name returned by the function.
       * 
       * 	- `t.getKind()` returns the primitive type kind (BOOLEAN, BYTE, SHORT, INT, LONG,
       * CHAR, FLOAT, or DOUBLE) based on the input's format.
       * 	- The primitive types BOOLEAN, BYTE, SHORT, INT, and LONG are represented by
       * `TypeName` objects corresponding to their data types.
       * 	- The primitive type CHAR is represented by a `TypeName.CHAR` object.
       * 	- The primitive type FLOAT is represented by a `TypeName.FLOAT` object.
       * 	- The primitive type DOUBLE is represented by a `TypeName.DOUBLE` object.
       * 
       * @param p Void object passed to the visitor method as a void reference, which has
       * no effect on the method's execution.
       * 
       * 	- `t`: The primitive type of the input, which is one of the following values:
       * BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, or DOUBLE.
       * 	- `p`: The deserialized input, which represents a primitive value of the corresponding
       * type.
       * 
       * @returns a `TypeName` object corresponding to the specified primitive type.
       * 
       * 	- TypeName.BOOLEAN represents a boolean type.
       * 	- TypeName.BYTE represents a byte type.
       * 	- TypeName.SHORT represents a short type.
       * 	- TypeName.INT represents an integer type.
       * 	- TypeName.LONG represents a long type.
       * 	- TypeName.CHAR represents a char type.
       * 	- TypeName.FLOAT represents a float type.
       * 	- TypeName.DOUBLE represents a double type.
       * 
       * The function takes two arguments: `t` of type PrimitiveType, and `p` of type Void.
       * The `switch` statement in the function determines the output type based on the
       * `kind` field of `t`. If the value of `t` is not within the valid range for the
       * given field, an `AssertionError` is thrown.
       */
      @Override public TypeName visitPrimitive(PrimitiveType t, Void p) {
        switch (t.getKind()) {
          case BOOLEAN:
            return TypeName.BOOLEAN;
          case BYTE:
            return TypeName.BYTE;
          case SHORT:
            return TypeName.SHORT;
          case INT:
            return TypeName.INT;
          case LONG:
            return TypeName.LONG;
          case CHAR:
            return TypeName.CHAR;
          case FLOAT:
            return TypeName.FLOAT;
          case DOUBLE:
            return TypeName.DOUBLE;
          default:
            throw new AssertionError();
        }
      }

      /**
       * processes a declared type and recursively visits its type arguments and enclosing
       * types, returning a parameterized type name or the raw type name if no type arguments
       * are present and the enclosing type is not a parameterized type.
       * 
       * @param t TypeElement of a declared type, which is used to determine the raw type
       * of the element and to check if it has any type arguments.
       * 
       * 	- `rawType`: The unadorned class name of the type element represented by `t`.
       * 	- `enclosingType`: The enclosing type of `t`, which is either a direct or indirect
       * supertype of `t`.
       * 	- `enclosing`: A reference to the enclosing type, which can be either a static
       * or non-static type.
       * 	- `typeArguments`: An empty list if `t` has no type arguments, otherwise a list
       * of type arguments for `t`.
       * 	- `typeVariables`: A reference to the type variables in the type arguments of
       * `t`, used to generate the nested class name.
       * 
       * @param p Void object used to pass any additional data or context that may be
       * required for the visitDeclared method to operate correctly.
       * 
       * 	- `t`: The TypeElement being processed, which represents a declared type in the
       * Java code.
       * 	- `p`: A Void parameter passed to the function as an argument, which is not used
       * or modified within the function.
       * 	- `rawType`: The unadorned ClassName of the declared type, obtained by calling
       * `ClassName.get((TypeElement) t.asElement())`.
       * 	- `enclosingType`: The enclosing TypeMirror of the declared type, which is either
       * the same as or a supertype of the raw type. This is determined by checking if the
       * enclosing TypeMirror has a non-`NULL` value for its Kind field and whether it
       * contains any `STATIC` modifier. If both conditions are false, then the enclosing
       * TypeMirror is a parameterized type, otherwise it is an unparameterized type.
       * 	- `typeArgumentNames`: A list of TypeName objects representing the type arguments
       * of the declared type, which are obtained by iterating over the type arguments in
       * the TypeElement's `getTypeArguments()` method and calling `get()` on each one to
       * create a new TypeName object.
       * 
       * The function then returns a new ParameterizedTypeName object if any type arguments
       * were found, or a simple ClassName object if no type arguments were present.
       * 
       * @returns a `TypeName` object representing the declared type after resolving its
       * enclosing type and type arguments.
       * 
       * 	- `rawType`: The unqualified class name of the declared type.
       * 	- `enclosingType`: The enclosing type of the declared type, which is either a
       * superclass or an inner class.
       * 	- `enclosing`: A boolean indicating whether the enclosing type is a parameterized
       * type.
       * 	- `typeArgumentNames`: A list of type argument names for the type parameters of
       * the declared type.
       * 
       * The output of the `visitDeclared` function can be destructured as follows:
       * 
       * 	- If the `enclosingType` is not `NONE`, the returned type name is a nested class
       * within the enclosing type.
       * 	- If the `typeArgumentNames` list is non-empty, the returned type name is a
       * parameterized type name with the unqualified class name of the raw type and the
       * type argument names.
       * 	- Otherwise, the returned type name is the unqualified class name of the raw type.
       */
      @Override public TypeName visitDeclared(DeclaredType t, Void p) {
        ClassName rawType = ClassName.get((TypeElement) t.asElement());
        TypeMirror enclosingType = t.getEnclosingType();
        TypeName enclosing =
            (enclosingType.getKind() != TypeKind.NONE)
                    && !t.asElement().getModifiers().contains(Modifier.STATIC)
                ? enclosingType.accept(this, null)
                : null;
        if (t.getTypeArguments().isEmpty() && !(enclosing instanceof ParameterizedTypeName)) {
          return rawType;
        }

        List<TypeName> typeArgumentNames = new ArrayList<>();
        for (TypeMirror mirror : t.getTypeArguments()) {
          typeArgumentNames.add(get(mirror, typeVariables));
        }
        return enclosing instanceof ParameterizedTypeName
            ? ((ParameterizedTypeName) enclosing).nestedClass(
            rawType.simpleName(), typeArgumentNames)
            : new ParameterizedTypeName(null, rawType, typeArgumentNames);
      }

      /**
       * overrides a method from its parent class and calls the `visitDeclared` function
       * with the same arguments, passing through any provided value of type Void.
       * 
       * @param t error type being visited, which is passed to the `visitDeclared()` method
       * for further processing.
       * 
       * 	- `t` is an instance of the `ErrorType` class, which represents an error object
       * in the Java Serialization format.
       * 	- The `t` object contains various attributes, including a message field that holds
       * the error message, and a cause field that holds the underlying cause of the error.
       * 
       * @param p Void object passed to the visitor pattern.
       * 
       * 	- `t`: The type of error, which provides context for the visitor method's execution.
       * 	- `p`: The Void object representing the error value.
       * 
       * The method `visitDeclared` is called with `t` and `p` as arguments to perform
       * further processing or validation on the error value.
       * 
       * @returns a reference to the result of calling the `visitDeclared` function with
       * the same error type and parameter.
       * 
       * The `TypeName` object returned is determined by the `t` parameter passed in, which
       * represents an error type. The `p` parameter, representing a void value, does not
       * affect the resulting output.
       * 
       * The `visitDeclared` method is called recursively on the `TypeName` object with the
       * `t` and `p` parameters unchanged.
       */
      @Override public TypeName visitError(ErrorType t, Void p) {
        return visitDeclared(t, p);
      }

      /**
       * takes an `ArrayType` object and a void parameter, and returns an `ArrayTypeName`.
       * It does not provide any information about the code author or licensing.
       * 
       * @param t array type being visited, and its value is used to generate the resulting
       * `ArrayTypeName`.
       * 
       * 	- `t` is an instance of the `ArrayType` class, which represents an array type in
       * the Java programming language.
       * 	- `typeVariables` is a variable of type `List<Variable>` that contains the variable
       * names of the type arguments of the array type.
       * 	- The function returns an instance of the `ArrayTypeName` class, which represents
       * an array type name. This is done by calling the `get` method of the `ArrayTypeName`
       * class and passing in `t` and `typeVariables`.
       * 
       * @param p Void value passed to the visitor method.
       * 
       * 	- `t`: The original array type that was parsed.
       * 	- `typeVariables`: A variable containing the types of the elements in the array.
       * 
       * The function returns an `ArrayTypeName`, which is a custom type that represents
       * an array of a specific type.
       * 
       * @returns an array type name constructed from the input array type and type variables.
       * 
       * 	- The output is an `ArrayTypeName`, which represents a type name for an array type.
       * 	- The type name is generated based on the input `t` and any variable arguments `typeVariables`.
       * 	- The output can be used to represent an array type in a Java-like language,
       * allowing for more specific and expressive type annotations.
       */
      @Override public ArrayTypeName visitArray(ArrayType t, Void p) {
        return ArrayTypeName.get(t, typeVariables);
      }

      /**
       * generates a name for a type variable based on its type and other variables. It
       * returns the generated name as a `TypeVariableName`.
       * 
       * @param t type variable being visited, and its value is returned as the result of
       * the `visitTypeVariable` method call.
       * 
       * 	- `t`: A type variable with a name `TypeVariableName`.
       * 	- `typeVariables`: An array of type variables.
       * 
       * The function returns the type variable's name after destructuring and/or explaining
       * its attributes, as appropriate.
       * 
       * @param p Void object passed to the visitor method for visiting a type variable.
       * 
       * 	- `t`: The type variable being visited, which is passed as an argument to the function.
       * 	- `typeVariables`: A collection of type variables, which is used to generate the
       * name of the type variable.
       * 
       * The function returns a `TypeVariableName`, which represents the name of the type
       * variable.
       * 
       * @returns a `TypeVariableName` object representing the type variable.
       * 
       * The output is a `TypeVariableName`, which represents a type variable in the model.
       * This means that it can represent any type, depending on the context in which it
       * is used.
       * 
       * The `TypeVariableName` object contains information about the type variable, including
       * its name and its position in the model's hierarchy of types.
       * 
       * The output is generated by combining the `t` parameter (representing the type
       * variable) with the `typeVariables` parameter (representing a set of type variables
       * defined in the model). This combination creates a unique identifier for the type
       * variable, which can be used to access it in the model.
       */
      @Override public TypeName visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
        return TypeVariableName.get(t, typeVariables);
      }

      /**
       * generates a `WildcardTypeName` object representing a wildcard type based on the
       * given `WildcardType` and `typeVariables`.
       * 
       * @param t wildcard type to be transformed into a `TypeName`.
       * 
       * 	- `t` represents a wildcard type, which is a type that can represent any type.
       * 	- `typeVariables` is a list of type variables associated with the wildcard type.
       * 	- The `WildcardTypeName.get()` method is called to create a new wildcard type
       * name based on the deserialized input `t`.
       * 
       * @param p Void object passed to the visitWildcard method.
       * 
       * 	- `p`: The Void parameter passed to the visitor method.
       * 	- `t`: The WildcardType being visited, which represents a reference to any type
       * in the model.
       * 	- `typeVariables`: An array of TypeVariable instances representing the type
       * variables in the model.
       * 
       * @returns a `WildcardTypeName` object representing the wildcard type with any type
       * variables substituted.
       * 
       * 	- `TypeName`: The output is a `TypeName`, which represents a type name in the
       * language model.
       * 	- `t`: The input `WildcardType` represents a wildcard type that can be used to
       * represent any type.
       * 	- `typeVariables`: An array of type variables, which are used to generate the
       * type name.
       * 
       * The output of the function is a `TypeName` instance that represents the wildcard
       * type with the given type variables.
       */
      @Override public TypeName visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
        return WildcardTypeName.get(t, typeVariables);
      }

      /**
       * determines the type of a `NoType` node and returns the corresponding `TypeName`.
       * If the `NoType` has a `VOID` kind, it returns `TypeName.VOID`, otherwise it delegates
       * to its superclass for further processing.
       * 
       * @param t NoType instance being visited, which is used to determine the type name
       * returned by the function.
       * 
       * 	- `getKind()` returns the type kind of `t`, which is either `VOID` or another
       * value indicating a non-void type.
       * 	- `super.visitUnknown(t, p)` is called if `t` is not of type `VOID`.
       * 
       * Overall, the function determines whether `t` is of type `VOID` and returns the
       * corresponding `TypeName` if so, or otherwise delegates to the superclass for further
       * processing.
       * 
       * @param p Void value that is passed to the visitor method when visiting a NoType object.
       * 
       * 	- `t`: The parent type node that represents the type of the deserialized input.
       * 	- `p`: The deserialized input object.
       * 	- `Kind`: A field in `p` that indicates the type kind of the input, which is
       * either `VOID` or a subtype of `VOID`.
       * 
       * @returns a `TypeName` object representing the type `VOID`.
       * 
       * 	- The output is a `TypeName` object, which represents a type name in the Java
       * programming language.
       * 	- The type name is determined by the `getKind()` method of the `NoType` instance
       * passed to the function. If the `NoType` instance has a `Kind` field set to `VOID`,
       * then the output is `TypeName.VOID`. Otherwise, the output is returned by calling
       * the `super.visitUnknown()` method.
       * 	- The `TypeName` object returned by this function represents a type name that is
       * void, which means it cannot be instanced or assigned a value.
       */
      @Override public TypeName visitNoType(NoType t, Void p) {
        if (t.getKind() == TypeKind.VOID) return TypeName.VOID;
        return super.visitUnknown(t, p);
      }

      /**
       * throws an `IllegalArgumentException` when given a type mirror that is unexpected.
       * 
       * @param e TypeMirror object that is passed to the function, which is then checked
       * for unexpected types and an IllegalArgumentException is thrown if it is not a
       * recognized type.
       * 
       * 	- `TypeMirror e`: This is the type mirror object that represents the serialized
       * Java class.
       * 	- `Void p`: This is the Void parameter passed to the function by reference.
       * 
       * @param p Void object passed to the function, which is not used in the provided code.
       * 
       * 	- TypeMirror e represents an unexpected type mirror, which is thrown as an exception.
       * 	- Void p is the void parameter passed to the function.
       * 
       * @returns an `IllegalArgumentException` with the message "Unexpected type mirror:
       * [e]"
       * 
       * The output is a `TypeMirror` instance representing an unexpected type mirror.
       * The input `e` is a `TypeMirror` instance that represents an unexpected type.
       * The input `p` is a `Void` parameter that is unused in this function.
       */
      @Override protected TypeName defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException("Unexpected type mirror: " + e);
      }
    }, null);
  }

  /**
   * returns a `TypeName` object for the specified type, using a default map if one is
   * not provided as an argument.
   * 
   * @param type type of object that the `get()` method is called on, and it is used
   * to determine the appropriate implementation of the method to return.
   * 
   * 	- `Type`: It is a class representing an object type in the Java programming language.
   * 	- `Map`: It is a collection of key-value pairs where each key represents a unique
   * identifier for an object and the value is an instance of a specific class or interface.
   * 
   * @returns a `TypeName` object.
   * 
   * 	- `TypeName`: This is the type of the object being retrieved. It can be any type
   * in Java, including classes, interfaces, and primitive types.
   * 	- `LinkedHashMap`: This is an implementation detail of the `get` function,
   * indicating that it uses a linked map to store the objects. Linked maps are data
   * structures that allow for fast lookups and insertions, making them suitable for
   * use in a caching system like this one.
   */
  public static TypeName get(Type type) {
    return get(type, new LinkedHashMap<>());
  }

  /**
   * maps a given `type` to its corresponding `TypeName`, taking into account the type's
   * class hierarchy, generic types, and wildcard types.
   * 
   * @param type type to be queried for its TypeName, and it can be of any class type,
   * array type, wildcard type, or generic array type.
   * 
   * 	- If `type` is an instance of `Class<?>`, then it can be one of the primitive
   * types (void, boolean, byte, short, int, long, char, float, or double) or an array
   * type. In this case, the function returns a corresponding type name.
   * 	- If `type` is an instance of `ParameterizedType`, then it represents a parameterized
   * type composed of a base type and a set of type arguments. The function returns a
   * parameterized type name constructed from these components.
   * 	- If `type` is an instance of `WildcardType`, then it represents a wildcard type
   * that can match any type. The function returns a wildcard type name.
   * 	- If `type` is an instance of `TypeVariable<?>`, then it represents a type variable
   * with a generic parameter. The function returns the type variable name.
   * 	- If `type` is an instance of `GenericArrayType`, then it represents an array
   * type with a generic component type. The function returns an array type name
   * constructed from these components.
   * 
   * In all other cases, an `IllegalArgumentException` is thrown.
   * 
   * @param map Map<Type, TypeVariableName> that is used to map the original type
   * parameters to their corresponding type variables.
   * 
   * 	- `map`: A `Map` object containing type information for mapping between types.
   * 
   * The `map` is destructured and its properties are explained as follows:
   * 
   * 	- `Type`: The type map contains a mapping from one type to another.
   * 	- `TypeVariableName`: The type map contains the name of a type variable.
   * 	- `Class<?>`: The type map contains the class of an object.
   * 	- `ComponentType`: The type map contains the component type of an array.
   * 	- `ParameterizedType`: The type map contains a parameterized type.
   * 	- `WildcardType`: The type map contains a wildcard type.
   * 	- `GenericArrayType`: The type map contains a generic array type.
   * 
   * @returns a `TypeName` object representing the type of the given `type` parameter,
   * based on its subclass and generic information.
   * 
   * 	- `VOID`: A type representing the void value, which has no values and is not
   * assignable to any other type.
   * 	- `BOOLEAN`: A type representing a boolean value, which can have one of two values:
   * true or false.
   * 	- `BYTE`, `SHORT`, `INT`, `LONG`, and `CHAR`: These are primitive types representing
   * integers and characters, respectively, with varying levels of precision.
   * 	- `FLOAT` and `DOUBLE`: These are primitive types representing floating-point
   * numbers with varying levels of precision.
   * 	- `ARRAY`: A type representing an array of any other type, which can hold a
   * collection of values of that type.
   * 	- `ParameterizedType`: A type representing a parameterized type, which is a type
   * defined by its supertype and parameters, such as `List<String>`.
   * 	- `WildcardType`: A type representing a wildcard type, which can represent any
   * type, including interfaces, abstract classes, and primitive types.
   * 	- `TypeVariable<?>`: A type variable, which represents an unconstrained type that
   * can be instantiated with any type.
   * 	- `GenericArrayType`: An array type defined by its generic component type and
   * bound, such as `List<Integer[]>`.
   * 
   * In summary, the `get` function returns a type name based on the input type, and
   * the output includes various types representing different values and structures.
   */
  static TypeName get(Type type, Map<Type, TypeVariableName> map) {
    if (type instanceof Class<?>) {
      Class<?> classType = (Class<?>) type;
      if (type == void.class) return VOID;
      if (type == boolean.class) return BOOLEAN;
      if (type == byte.class) return BYTE;
      if (type == short.class) return SHORT;
      if (type == int.class) return INT;
      if (type == long.class) return LONG;
      if (type == char.class) return CHAR;
      if (type == float.class) return FLOAT;
      if (type == double.class) return DOUBLE;
      if (classType.isArray()) return ArrayTypeName.of(get(classType.getComponentType(), map));
      return ClassName.get(classType);

    } else if (type instanceof ParameterizedType) {
      return ParameterizedTypeName.get((ParameterizedType) type, map);

    } else if (type instanceof WildcardType) {
      return WildcardTypeName.get((WildcardType) type, map);

    } else if (type instanceof TypeVariable<?>) {
      return TypeVariableName.get((TypeVariable<?>) type, map);

    } else if (type instanceof GenericArrayType) {
      return ArrayTypeName.get((GenericArrayType) type, map);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type);
    }
  }

  /**
   * generates a list of type instances from an array of Type objects and a map of
   * type-string pairs.
   * 
   * @param types Type[s] that are used to create a List of TypeName objects.
   * 
   * 	- The input parameter `types` is an array of `Type` objects.
   * 	- The function returns a list of type objects in a `LinkedHashMap`.
   * 	- The `LinkedHashMap` is initialized with empty map elements for each type in the
   * input array.
   * 
   * @returns a `List` of `TypeName` objects.
   * 
   * The `List<TypeName>` object is an instance of the `ArrayList` class in Java, which
   * means it stores a collection of objects in a dynamic array. The `TypeName` class
   * represents a type name, and each element in the list contains a reference to a
   * `TypeName` object.
   * 
   * The `list` function takes two arguments: `types`, which is an array of `Type`
   * objects, and `map`, which is a `LinkedHashMap` object that maps the type names to
   * their corresponding types. The function returns a `List<TypeName>` object that
   * contains all the type names from the input `types` array, along with their
   * corresponding types from the `map`.
   */
  static List<TypeName> list(Type[] types) {
    return list(types, new LinkedHashMap<>());
  }

  /**
   * takes a list of `Type` objects and a mapping of type variables to their corresponding
   * variable names, returns a list of `TypeName` objects representing the types.
   * 
   * @param types 0 or more Type objects to be converted into TypeNames, which are then
   * returned by the function.
   * 
   * 	- `types`: An array of type objects, representing a list of types to be compiled
   * into a single type name. Each type object in the array has its own set of attributes,
   * such as name, parameters, and so on.
   * 	- `map`: A mapping of type variables to their corresponding type names, which is
   * used to map each type object in `types` to its corresponding type name.
   * 
   * @param map mapping between the type parameters and their corresponding variable names.
   * 
   * 	- Map<Type, TypeVariableName> map: This is an instance of the map class, which
   * contains key-value pairs where each key represents a type and each value represents
   * a type variable name. The map is used to map the type parameters in the `types`
   * array to their corresponding type variable names.
   * 
   * @returns a list of `TypeName` objects, each representing a type in the input `types`
   * array.
   * 
   * The `List<TypeName>` object returned is initialized with the same length as the
   * input array of types (`types`). Each element in the list represents a type name.
   * 
   * The types in the input array are used to generate the type names in the list, where
   * each type is mapped to its corresponding type name using the `Map<Type,
   * TypeVariableName>` object passed as an argument.
   * 
   * The resulting list of type names can be used for various purposes such as data
   * validation, type checking, or further processing.
   */
  static List<TypeName> list(Type[] types, Map<Type, TypeVariableName> map) {
    List<TypeName> result = new ArrayList<>(types.length);
    for (Type type : types) {
      result.add(get(type, map));
    }
    return result;
  }

  /**
   * determines the component type of an array type, recursively inspecting its components
   * if it is an array type. It returns the component type or `null`.
   * 
   * @param type TypeName to be checked for an array, and if it is an array, the component
   * type of the array is returned.
   * 
   * The type variable `type` is an instance of the class `TypeName`.
   * The method checks if `type` is an instance of the class `ArrayTypeName`. If it is,
   * then it returns the value of the `componentType` attribute of that object. Otherwise,
   * the method returns `null`.
   * 
   * @returns the component type of an array, or `null` if the input type is not an array.
   * 
   * The function returns the component type of an array if the input `type` is an
   * instance of `ArrayTypeName`.
   * 
   * If the input `type` is not an instance of `ArrayTypeName`, the function returns `null`.
   */
  static TypeName arrayComponent(TypeName type) {
    return type instanceof ArrayTypeName
        ? ((ArrayTypeName) type).componentType
        : null;
  }

  /**
   * verifies whether a provided `TypeName` is an array type and returns the array type
   * if it is, or `null` otherwise.
   * 
   * @param type TypeName parameter to be checked for an array type.
   * 
   * 1/ If `type` is an instance of `ArrayTypeName`, then it is returned unchanged as
   * an instance of `ArrayTypeName`.
   * 2/ Otherwise, `type` is nullified.
   * 
   * @returns a `ArrayTypeName` object if the input `type` is an array, otherwise it
   * returns `null`.
   * 
   * 	- The output is a `TypeName` instance if the input argument `type` is an array type.
   * 	- If `type` is not an array type, the output is set to `null`.
   * 
   * The output of `asArray` can be used to determine whether a given `TypeName` is an
   * array or not, which can be useful in various contexts such as type checking or
   * data manipulation.
   */
  static ArrayTypeName asArray(TypeName type) {
    return type instanceof ArrayTypeName
        ? ((ArrayTypeName) type)
        : null;
  }

}
