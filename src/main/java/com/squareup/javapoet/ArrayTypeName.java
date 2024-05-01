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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * is a subtype of TypeName that represents an array type whose elements are instances
 * of a specified type. It provides methods for creating instances of the class and
 * for emitting Java code to represent the type in a program. The class also includes
 * methods for getting equivalent array types based on mirrors, GenericArrayTypes,
 * and TypeVariables.
 */
public final class ArrayTypeName extends TypeName {
  public final TypeName componentType;

  private ArrayTypeName(TypeName componentType) {
    this(componentType, new ArrayList<>());
  }

  private ArrayTypeName(TypeName componentType, List<AnnotationSpec> annotations) {
    super(annotations);
    this.componentType = checkNotNull(componentType, "rawType == null");
  }

  /**
   * creates a new `ArrayTypeName` instance by combining the component type with the
   * provided annotations using the `concatAnnotations` method.
   * 
   * @param annotations list of AnnotationSpec objects that will be concatenated and
   * used to annotate the component type returned by the function.
   * 
   * 	- `List<AnnotationSpec> annotations`: This represents a list of annotation
   * specifications that can be used to modify or enhance the type of the component
   * being annotated.
   * 	- `componentType`: The component type being annotated, which serves as the base
   * type for the generated array type name.
   * 	- `concatAnnotations(annotations)`: A method that concatenates the annotations
   * in the list, creating a single string representation of the annotations.
   * 
   * @returns an `ArrayTypeName` object representing a type name composed of the component
   * type and the annotations provided.
   * 
   * 	- `ArrayTypeName`: This is the type name of the annotated array, which is a
   * composite type consisting of the component type and any annotations added to it
   * using the `concatAnnotations()` method.
   * 	- `componentType`: The component type of the annotated array, which represents
   * the base type of the elements that make up the array.
   */
  @Override public ArrayTypeName annotated(List<AnnotationSpec> annotations) {
    return new ArrayTypeName(componentType, concatAnnotations(annotations));
  }

  /**
   * generates an array type name based on a provided component type.
   * 
   * @returns an `ArrayTypeName` instance representing the component type.
   * 
   * 	- `TypeName`: This is an instance of the `ArrayTypeName` class, which represents
   * an array type in Java.
   * 	- `componentType`: This is the component type of the array, which is also an
   * instance of a `Class` object representing the underlying type of the elements in
   * the array.
   */
  @Override public TypeName withoutAnnotations() {
    return new ArrayTypeName(componentType);
  }

  /**
   * emits code to be written to a specified output writer, suppressing any output for
   * the default output writer.
   * 
   * @param out output stream where the generated code will be written.
   * 
   * 	- The `out` argument is of type `CodeWriter`, which is an interface for generating
   * Java code.
   * 	- The `emit` function returns a value of type `CodeWriter`, indicating that it
   * generates additional Java code based on the input provided.
   * 	- The function takes a single parameter, `false`, which is a boolean value passed
   * as an argument to indicate whether the generated code should be traced or not.
   * 
   * @returns a `CodeWriter` object.
   * 
   * 	- The function returns an `CodeWriter` object.
   * 	- The return value is determined by the `false` argument passed to the function.
   * 	- The `out` parameter is used as the output stream for writing the code.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    return emit(out, false);
  }

  /**
   * emits a leaf type and then returns an expression involving bracket notation, where
   * `varargs` is a boolean indicating whether the expression should include variable-length
   * arguments.
   * 
   * @param out output stream where the generated code will be written.
   * 
   * 	- `out` is an instance of `CodeWriter`, which is a generic class representing a
   * writer for serializing code.
   * 	- `varargs` is a boolean parameter indicating whether the output should include
   * varargs information.
   * 	- The function returns `emitBrackets`, another function that takes `out` and
   * `varargs` as parameters, and writes the brackets to the output.
   * 
   * @param varargs 0-n arguments passed to the `emitBrackets()` method, which is called
   * recursively within the `emit(CodeWriter out, boolean varargs) throws IOException`.
   * 
   * @returns a bracketed expression.
   * 
   * 	- The `out` parameter represents the output stream where the emitted code will
   * be written.
   * 	- The `varargs` parameter indicates whether the emitted code is a variadic argument
   * list.
   * 
   * The output returned by the `emit` function can be described as follows:
   * 
   * 	- It is an instance of the `CodeWriter` class, which provides methods for writing
   * Java code to an output stream.
   * 	- The `CodeWriter` object has no attributes or properties beyond those defined
   * in its contract.
   */
  CodeWriter emit(CodeWriter out, boolean varargs) throws IOException {
    emitLeafType(out);
    return emitBrackets(out, varargs);
  }

  /**
   * emits a leaf type based on the input `componentType`. If the input is an array,
   * it recursively calls itself on each component type. Otherwise, it emits the entire
   * component type.
   * 
   * @param out output writer where the type emitted by the function is being written.
   * 
   * 	- `out` is an instance of `CodeWriter`.
   * 	- It represents the current node being emitted in the AST.
   * 	- The `emitLeafType` method is called recursively on the component type of the
   * current node, if it is not null.
   * 	- The component type is represented by `componentType`, which is passed as an
   * argument to the function.
   * 
   * @returns a leaf type representing an array of a specific component type, or the
   * component type itself if it is not an array.
   * 
   * 	- If `TypeName.asArray(componentType) != null`, then the returned output is an
   * array of the component type.
   * 	- Otherwise, the returned output is the component type itself.
   * 
   * The output of the `emitLeafType` function can be further processed or used as input
   * for other methods, depending on the context in which it is being emitted.
   */
  private CodeWriter emitLeafType(CodeWriter out) throws IOException {
    if (TypeName.asArray(componentType) != null) {
      return TypeName.asArray(componentType).emitLeafType(out);
    }
    return componentType.emit(out);
  }

  /**
   * emits brackets around a component type's array representation, taking into account
   * whether it is a vararg and whether it has any annotations.
   * 
   * @param out CodeWriter object where the emitted code will be written.
   * 
   * 1/ It is an instance of `CodeWriter`.
   * 2/ It has a `write` method that takes a string argument and writes it to the output
   * stream.
   * 3/ The `write` method is defined in the `IOException` class, indicating that any
   * exception thrown by the method will be caught and handled by the caller.
   * 4/ The function returns an instance of `CodeWriter`, which allows for chaining of
   * method calls.
   * 
   * @param varargs variadic argument list for the `emitBrackets()` method, which
   * determines whether the final bracket should be denoted with an ellipsis (`...`)
   * or a square bracket `[]`.
   * 
   * @returns a bracket symbol ("[") followed by any annotations or array elements, and
   * then the component type's brackets emission.
   * 
   * 	- If `varargs` is `true`, the output will be "..." instead of "[]".
   * 	- The componentType is emitted using `emitBrackets`.
   * 	- The method returns the emitted output.
   */
  private CodeWriter emitBrackets(CodeWriter out, boolean varargs) throws IOException {
    if (isAnnotated()) {
      out.emit(" ");
      emitAnnotations(out);
    }

    if (TypeName.asArray(componentType) == null) {
      // Last bracket.
      return out.emit(varargs ? "..." : "[]");
    }
    out.emit("[]");
    return TypeName.asArray(componentType) .emitBrackets(out, varargs);
  }


  /**
   * creates a new `ArrayTypeName` object by wrapping an existing `TypeName` object,
   * typically used for building complex types such as arrays or collections.
   * 
   * @param componentType type of elements that make up the array being created, which
   * is used to construct the resulting `ArrayTypeName`.
   * 
   * 	- The type of the component is an `ArrayTypeName`, which indicates that it is a
   * collection of elements of a specific type.
   * 	- The type of the elements in the collection is specified by the `componentType`
   * parameter, which can be any valid Java type.
   * 	- The `of` function creates a new instance of the `ArrayTypeName` class and sets
   * its `componentType` field to the value of `componentType`.
   * 
   * @returns a new `ArrayTypeName` instance representing the specified `TypeName`
   * component type.
   * 
   * 	- The `ArrayTypeName` object that is created represents an array type.
   * 	- The `componentType` parameter passed to the function is used as the base type
   * of the array.
   * 	- The resulting array type name is a composite type consisting of the prefix
   * "Array", followed by the component type name.
   */
  public static ArrayTypeName of(TypeName componentType) {
    return new ArrayTypeName(componentType);
  }

  /**
   * returns an instance of the `ArrayTypeName` class based on the specified `componentType`.
   * It uses the `TypeName.get()` method to retrieve the type name of the component
   * type and then creates an instance of the `ArrayTypeName` class with that type name.
   * 
   * @param componentType type of the component being used to create an instance of the
   * `ArrayTypeName` class.
   * 
   * 	- TypeName.get(componentType) is used to get the type name of the input `componentType`.
   * 	- The returned value is an `ArrayTypeName`.
   * 
   * @returns an `ArrayTypeName` object representing the specified type.
   * 
   * 	- The output is an `ArrayTypeName`, which indicates that it represents an array
   * type.
   * 	- The type name is specified by the `TypeName.get()` method, which returns a
   * `TypeName` object representing the type.
   * 	- The type name can be used to retrieve information about the type, such as its
   * name, kind, and other attributes.
   */
  public static ArrayTypeName of(Type componentType) {
    return of(TypeName.get(componentType));
  }

  /**
   * retrieves an array type name associated with a given mirror array and caches it
   * for later use.
   * 
   * @param mirror 2D array data structure that is being transformed into an object of
   * the `ArrayTypeName` class.
   * 
   * 	- The method takes an instance of `ArrayType`, which is a class representing an
   * array of values.
   * 	- The method returns an instance of `ArrayTypeName`, which represents the type
   * of the values in the array.
   * 
   * @returns an instance of `ArrayTypeName`.
   * 
   * 	- The output is an instance of the `ArrayTypeName` class.
   * 	- It represents a name for an array type in the given mirror.
   * 	- The name is generated based on the mirror's element type and any applicable
   * annotations or other metadata.
   */
  public static ArrayTypeName get(ArrayType mirror) {
    return get(mirror, new LinkedHashMap<>());
  }

  /**
   * creates a new `ArrayTypeName` instance by mirroring the given `ArrayType` and
   * recursively calling itself with the component type.
   * 
   * @param mirror type of the array being created, which is used to determine the
   * component type of the resulting array.
   * 
   * 	- `mirror`: This is an instance of the `ArrayType` class, representing an array
   * type in the Java programming language.
   * 	- `getComponentType()`: This method returns the component type of the array, which
   * is used to create a new `ArrayTypeName` instance.
   * 
   * @param typeVariables mapping between type parameters and type variables that are
   * used to create the new array type name returned by the function.
   * 
   * 	- `Map<TypeParameterElement, TypeVariableName>`: A mapping between type parameters
   * and their corresponding type variables. Each key-value pair in the map represents
   * a type parameter and its associated type variable.
   * 	- `TypeParameterElement`: A class that represents a type parameter in the input
   * type.
   * 	- `TypeVariableName`: A class that represents a type variable in the input type.
   * 
   * @returns a new `ArrayTypeName` instance created from the component type of the
   * mirrored array and the given type variables.
   * 
   * 	- The output is an instance of the `ArrayTypeName` class.
   * 	- The type of the array is determined by calling the `getComponentType` method
   * on the input mirror and passing it as a parameter to the `ArrayTypeName` constructor.
   * 	- The type variables map provided in the function is used to construct the type
   * name of the array.
   */
  static ArrayTypeName get(
      ArrayType mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
    return new ArrayTypeName(get(mirror.getComponentType(), typeVariables));
  }

  /**
   * returns an `ArrayTypeName` object based on a generic `ArrayType` parameter and an
   * empty map.
   * 
   * @param type generic array type that is to be checked for existence and returned
   * as an instance of `ArrayTypeName`.
   * 
   * 	- The function takes an instance of the `GenericArrayType` class as its parameter.
   * 	- This type represents an array of objects that have a specific structure defined
   * by their elements' types.
   * 	- The method returns an instance of the `ArrayTypeName` class, which is a more
   * detailed representation of the input array type.
   * 
   * @returns an instance of `ArrayTypeName`.
   * 
   * 	- The `ArrayTypeName` object is created using the `GenericArrayType` parameter
   * and an empty `LinkedHashMap`.
   * 	- The resulting `ArrayTypeName` object represents a generic array type with no
   * explicit dimensions.
   * 	- The `get` function returns this object directly, without performing any additional
   * processing or manipulation of the input parameters.
   */
  public static ArrayTypeName get(GenericArrayType type) {
    return get(type, new LinkedHashMap<>());
  }

  /**
   * takes a `GenericArrayType` and a `Map` of type variables, and returns an `ArrayTypeName`
   * representing the array's component type. It recursively calls itself on the component
   * type to generate the final array type name.
   * 
   * @param type generic component type of the array being created, which is used to
   * generate the corresponding array type name.
   * 
   * 1/ `type`: The type parameter of the method, which is an instance of `GenericArrayType`.
   * 2/ `map`: A map containing information about the type variables and their corresponding
   * names.
   * 3/ `get(type.getGenericComponentType(), map)`: The generic component type of the
   * `type` parameter, which is used to deserialize a specific sub-array type.
   * 
   * @param map mapping between type variables and their corresponding names, which is
   * used to resolve the generic types in the `get()` method call.
   * 
   * 	- `map` is a map containing key-value pairs of type `Type` and `TypeVariableName`.
   * 	- The `Type` values in the map represent the types of the generic components of
   * the array, while the `TypeVariableName` values represent the name of the type
   * variables used in the array's generic components.
   * 
   * @returns an `ArrayTypeName` object representing the array type of the given generic
   * component type and mapping.
   * 
   * 	- The output is an `ArrayTypeName` object, indicating that it represents an array
   * type.
   * 	- The type of the array is determined by the `type` parameter, which is a
   * `GenericArrayType` object.
   * 	- The `Map` parameter `map` contains mappings between types and type variables,
   * which are used to construct the array type.
   * 	- The output is created by calling the `of` method on an instance of `AbstractTypeName`,
   * passing in the result of calling `get` on the component type of the array and the
   * `map`.
   */
  static ArrayTypeName get(GenericArrayType type, Map<Type, TypeVariableName> map) {
    return ArrayTypeName.of(get(type.getGenericComponentType(), map));
  }
}
