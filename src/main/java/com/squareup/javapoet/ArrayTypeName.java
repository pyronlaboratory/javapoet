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

public final class ArrayTypeName extends TypeName {
  public final TypeName componentType;

  private ArrayTypeName(TypeName componentType) {
    this(componentType, new ArrayList<AnnotationSpec>());
  }

  private ArrayTypeName(TypeName componentType, List<AnnotationSpec> annotations) {
    super(annotations);
    this.componentType = checkNotNull(componentType, "rawType == null");
  }

  /**
   * Creates a new `ArrayTypeName` instance by combining a specified `componentType`
   * with any provided `AnnotationSpec` instances.
   * 
   * @param annotations list of AnnotationSpecs that will be applied to the returned ArrayTypeName.
   * 
   * @returns an `ArrayTypeName` object representing the component type and its annotations.
   */
  @Override public ArrayTypeName annotated(List<AnnotationSpec> annotations) {
    return new ArrayTypeName(componentType, annotations);
  }

  /**
   * Emits annotations for a given component type.
   * 
   * @param out output writer where the annotations are being emitted.
   * 
   * @returns a `$T[]` annotation.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    return emitAnnotations(out).emit("$T[]", componentType);
  }

  /**
   * Creates an instance of the `ArrayTypeName` class with the specified `componentType`.
   * 
   * @param componentType type of components that will be included in the array returned
   * by the `of()` function.
   * 
   * @returns an `ArrayTypeName` object representing the specified `TypeName`.
   */
  public static ArrayTypeName of(TypeName componentType) {
    return new ArrayTypeName(componentType);
  }

  /**
   * Generates a documentation for code given to it.
   * 
   * @param componentType type of the component being worked with, which is used to
   * determine the appropriate name for the array type returned by the function.
   * 
   * @returns an array type name.
   */
  public static ArrayTypeName of(Type componentType) {
    return of(TypeName.get(componentType));
  }

  /**
   * Retrieves an ArrayTypeName by passing a mirror object and an empty map as parameters.
   * 
   * @param mirror type mirror for which the `get` method is called, and it is used to
   * provide information about the type mirror that is necessary for the method to
   * return the correct `ArrayTypeName`.
   * 
   * @returns an `ArrayTypeName`.
   */
  public static ArrayTypeName get(ArrayType mirror) {
    return get(mirror, new LinkedHashMap<TypeParameterElement, TypeVariableName>());
  }

  /**
   * Creates an array type name by mirroring the component type and passing it to a new
   * instance of `ArrayTypeName`.
   * 
   * @param mirror array being wrapped with the `ArrayTypeName`.
   * 
   * @param typeVariables Map of <TypeParameterElement, TypeVariableName> that contains
   * information about the types of the elements in the mirrored array.
   * 
   * @returns a new `ArrayTypeName` object created from the component type of the mirror
   * and the type variables provided.
   */
  static ArrayTypeName get(
      ArrayType mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
    return new ArrayTypeName(get(mirror.getComponentType(), typeVariables));
  }

  /**
   * Retrieves an array type name based on a generic array type and any type variables
   * involved.
   * 
   * @param type generic array type to which the documentation should be generated.
   * 
   * @returns an array type name.
   */
  public static ArrayTypeName get(GenericArrayType type) {
    return get(type, new LinkedHashMap<Type, TypeVariableName>());
  }

  /**
   * Returns an `ArrayTypeName` instance based on a given `GenericArrayType` and a `Map`
   * of type variables, by recursively calling itself on the component type of the array
   * and the given map.
   * 
   * @param type generic type of the array being generated, which is used to determine
   * the type of the elements in the array.
   * 
   * @param map mapping between type variables and their corresponding names.
   * 
   * @returns an `ArrayTypeName` object representing the component type of the given
   * generic array type.
   */
  static ArrayTypeName get(GenericArrayType type, Map<Type, TypeVariableName> map) {
    return ArrayTypeName.of(get(type.getGenericComponentType(), map));
  }
}
