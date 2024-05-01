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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * is used to represent a parameterized type in Java, which is a type that takes one
 * or more type arguments. It extends the TypeName class and provides additional
 * methods for creating and manipulating parameterized types. The class has several
 * constructors that allow for creation of parameterized types with different levels
 * of nesting and type arguments. Additionally, it provides methods for emitting the
 * type to a CodeWriter object, as well as methods for creating nested classes and
 * getting equivalent parameterized types.
 */
public final class ParameterizedTypeName extends TypeName {
  private final ParameterizedTypeName enclosingType;
  public final ClassName rawType;
  public final List<TypeName> typeArguments;

  ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
      List<TypeName> typeArguments) {
    this(enclosingType, rawType, typeArguments, new ArrayList<>());
  }

  private ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
      List<TypeName> typeArguments, List<AnnotationSpec> annotations) {
    super(annotations);
    this.rawType = checkNotNull(rawType, "rawType == null").annotated(annotations);
    this.enclosingType = enclosingType;
    this.typeArguments = Util.immutableList(typeArguments);

    checkArgument(!this.typeArguments.isEmpty() || enclosingType != null,
        "no type arguments: %s", rawType);
    for (TypeName typeArgument : this.typeArguments) {
      checkArgument(!typeArgument.isPrimitive() && typeArgument != VOID,
          "invalid type parameter: %s", typeArgument);
    }
  }

  /**
   * creates a `ParameterizedTypeName` object by combining the enclosing type, raw type,
   * and type arguments with any annotations provided in the input list.
   * 
   * @param annotations list of AnnotationSpecs to be concatenated and used as type
   * arguments for the `ParameterizedTypeName`.
   * 
   * The first argument is a list of `AnnotationSpec` objects, which represent the
   * annotations applied to the type.
   * 
   * The second argument is a `TypeName` object, representing the enclosing type.
   * 
   * The third argument is a `TypeName` object, representing the raw type being parameterized.
   * 
   * The fourth argument is an array of `TypeName` objects, representing the type arguments.
   * 
   * @returns a `ParameterizedTypeName` object representing the combination of an
   * enclosing type, raw type, and type arguments, along with any annotations provided.
   * 
   * 1/ The first parameter is `enclosingType`, which represents the type of the class
   * that contains the annotated method.
   * 2/ The second parameter is `rawType`, which is the original type of the method
   * before any annotations were applied.
   * 3/ The third parameter is `typeArguments`, which are the type arguments provided
   * for the annotated method.
   * 4/ The fourth parameter is `concatAnnotations`, which is a collection of annotations
   * that are concatenated with the existing annotations on the method.
   */
  @Override public ParameterizedTypeName annotated(List<AnnotationSpec> annotations) {
    return new ParameterizedTypeName(
        enclosingType, rawType, typeArguments, concatAnnotations(annotations));
  }

  /**
   * generates a `ParameterizedTypeName` object representing the original type without
   * annotations and type arguments.
   * 
   * @returns a `ParameterizedTypeName` object representing the type without annotations.
   * 
   * 	- `enclosingType`: The type of the enclosing class or interface that contains the
   * method.
   * 	- `rawType`: The underlying type of the parameterized type, without any annotations.
   * 	- `typeArguments`: A list of type arguments used to generate the parameterized type.
   * 	- `ArrayList`: An empty list indicating that no additional type arguments were provided.
   */
  @Override
  public TypeName withoutAnnotations() {
    return new ParameterizedTypeName(
        enclosingType, rawType.withoutAnnotations(), typeArguments, new ArrayList<>());
  }

  /**
   * generates Java code for a method based on its metadata, emitting the method name,
   * enclosing type, and type arguments. It also handles annotations and indents the
   * output accordingly.
   * 
   * @param out output stream where the generated code will be written.
   * 
   * 	- `out` is an instance of `CodeWriter`, which is used to generate Java code for
   * the current method or class.
   * 	- `enclosingType` is a field of type `Class<T>` that represents the enclosing
   * type of the current method or class, where `T` is the raw type of the current
   * element being deserialized. This field is optional and may be null if the current
   * element is not nested within another type.
   * 	- `rawType` is a field of type `Class<T>` that represents the raw type of the
   * current element being deserialized, which is the type of the element before any
   * annotations or type arguments are applied.
   * 	- `typeArguments` is a list of `TypeName` objects that represent the type arguments
   * for the current method or class. These type arguments may be used to specify generic
   * types or constraints for the current element.
   * 
   * In summary, `out` is an instance of `CodeWriter` that generates Java code for the
   * current method or class, while `enclosingType`, `rawType`, and `typeArguments` are
   * fields that provide information about the current element being deserialized.
   * 
   * @returns a Java code snippet that represents the type of a class or interface and
   * its type arguments, if any.
   * 
   * 	- The output is an `IOException` object, which indicates that any exception thrown
   * by the method must be caught and handled appropriately.
   * 	- The first line of the output emits the `enclosingType`, followed by a period
   * (`.`). This suggests that the `enclosingType` variable may hold a reference to a
   * nested class or interface, and the period is used to indicate the separation between
   * the outer and inner types.
   * 	- If the `isAnnotated()` method returns true, an empty space (``) is emitted
   * immediately after the `.` symbol. This suggests that the `emitAnnotations()` method
   * may be called to emit any annotations associated with the current type.
   * 	- The `rawType.simpleName()` method is called and emitted as part of the output.
   * This suggests that the `rawType` variable holds a reference to a class or interface,
   * and the simple name of the type is being emitted for further processing.
   * 	- If the `typeArguments` list is not empty, an opening `("<")` symbol is emitted
   * followed by a space character (``). This indicates that the method is emitting
   * type arguments for the current type, which may be nested or nested inside other types.
   * 	- The `out` object is used to emit the output, which is an `IOException` instance
   * in this case.
   * 
   * Overall, the output of the `emit` function provides information about the enclosing
   * type, any annotations associated with the type, and any type arguments that may
   * be present.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    if (enclosingType != null) {
      enclosingType.emit(out);
      out.emit(".");
      if (isAnnotated()) {
        out.emit(" ");
        emitAnnotations(out);
      }
      out.emit(rawType.simpleName());
    } else {
      rawType.emit(out);
    }
    if (!typeArguments.isEmpty()) {
      out.emitAndIndent("<");
      boolean firstParameter = true;
      for (TypeName parameter : typeArguments) {
        if (!firstParameter) out.emitAndIndent(", ");
        parameter.emit(out);
        firstParameter = false;
      }
      out.emitAndIndent(">");
    }
    return out;
  }

  /**
   * creates a new `ParameterizedTypeName` instance by combining the current class with
   * a nested class name and two lists of type arguments.
   * 
   * @param name name of the nested class to be created.
   * 
   * @returns a `ParameterizedTypeName` object representing a nested class within a
   * larger type hierarchy.
   * 
   * 	- `ParameterizedTypeName`: This class represents a parameterized type name, which
   * is a composite type that consists of a base type and a list of type arguments. In
   * this case, the base type is `this`, which refers to the outer type of the function,
   * and the type arguments are `rawType.nestedClass(name)` and two lists of type parameters.
   * 	- `rawType`: This represents the raw type of the function, which is a reference
   * to the type of the outer function.
   * 	- `name`: This is the input parameter passed to the function, which is checked
   * for nullability using the `checkNotNull` method.
   */
  public ParameterizedTypeName nestedClass(String name) {
    checkNotNull(name, "name == null");
    return new ParameterizedTypeName(this, rawType.nestedClass(name), new ArrayList<>(),
        new ArrayList<>());
  }

  /**
   * creates a `ParameterizedTypeName` object by combining the current class with a
   * nested class from the raw type, along with type arguments and an empty list of
   * generic arguments.
   * 
   * @param name name of the nested class that is being created.
   * 
   * @param typeArguments type arguments for the nested class, which are used to generate
   * the corresponding generic type declaration.
   * 
   * 	- `typeArguments`: A list of `TypeName` objects, which represent the type arguments
   * passed to the constructor of the `ParameterizedTypeName`.
   * 	- `rawType`: A reference to the enclosing `TypeName`, representing the type of
   * the outermost class or interface.
   * 	- `nestedClass`: A reference to a nested class within the enclosing `TypeName`.
   * 
   * @returns a `ParameterizedTypeName` object representing a nested class within a
   * larger type.
   * 
   * 	- The return type is `ParameterizedTypeName`, which represents a parameterized
   * type with a raw type, a nested class, and type arguments.
   * 	- The first argument is `this`, which refers to the enclosing type.
   * 	- The second argument is a `rawType`, which is the unparameterized type of the
   * nested class.
   * 	- The third argument is a `List` of `TypeName` objects, which are the type arguments
   * for the parameterized type.
   * 	- The fourth argument is an empty list, which is used to represent the generic
   * type parameters of the returned type.
   */
  public ParameterizedTypeName nestedClass(String name, List<TypeName> typeArguments) {
    checkNotNull(name, "name == null");
    return new ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments,
        new ArrayList<>());
  }

  /**
   * creates a `ParameterizedTypeName` instance by passing the `rawType`, and any number
   * of `TypeName` arguments to its constructor.
   * 
   * @param rawType unqualified name of the type that is being parameterized.
   * 
   * 	- `rawType`: A deserialized input object representing the original class type.
   * 	- `typeArguments`: An array of type arguments for the parameterized type.
   * 
   * @returns a `ParameterizedTypeName` object representing a parameterized type with
   * the specified raw type and type arguments.
   * 
   * 	- The output is a `ParameterizedTypeName`, which means it represents a type that
   * is parameterized with specific type arguments.
   * 	- The `rawType` parameter is null, indicating that the resulting type name does
   * not contain any information about its own type parameters.
   * 	- The `typeArguments` parameter is an array of `TypeName` objects, representing
   * the type arguments that are used to define the type.
   */
  public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
    return new ParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
  }

  /**
   * creates a `ParameterizedTypeName` object that represents a parameterized type name
   * based on a raw type and zero or more type arguments.
   * 
   * @param rawType base type of the parameterized type to be created.
   * 
   * 	- `rawType`: The raw type parameter is provided as an argument to the method.
   * 	- `typeArguments`: A list of type arguments is passed as an array to the method.
   * 
   * @returns a `ParameterizedTypeName` object representing the specified raw type and
   * type arguments.
   * 
   * The return type is `ParameterizedTypeName`, which represents a parameterized type
   * name. This type consists of three parts: the type name, the generic parameters,
   * and the actual type arguments.
   * 
   * The first argument is `null`, which indicates that the type name is not initialized.
   * 
   * The second argument is `ClassName.get(rawType)`, which is the fully qualified class
   * name of the raw type. This is the type that will be used as the base type for the
   * parameterized type.
   * 
   * The third argument is a list of `Type...` arguments, which represent the generic
   * parameters and actual type arguments for the parameterized type. These are the
   * types that will be applied to the type name to create the parameterized type.
   */
  public static ParameterizedTypeName get(Class<?> rawType, Type... typeArguments) {
    return new ParameterizedTypeName(null, ClassName.get(rawType), list(typeArguments));
  }

  /**
   * returns a `ParameterizedTypeName` instance based on a provided `ParameterizedType`.
   * It creates an empty map if none is provided as an argument.
   * 
   * @param type type to which the method will create a parameterized type name.
   * 
   * The `ParameterizedTypeName` object returned by `get(type)` can be used to represent
   * any type that is a subclass of `ParameterizedType`. This means that it can represent
   * any type that has been parameterized with additional information, such as a generic
   * type with type arguments. The `type` parameter passed into the function is an
   * instance of `ParameterizedType`, which provides information about the type's
   * structure and constraints. Specifically, `type` contains information about the
   * type's name, its classloader, and any type parameters it may have.
   * 
   * @returns a `ParameterizedTypeName` object.
   * 
   * 	- The `ParameterizedTypeName` object is generated based on the `type` parameter
   * passed to the function.
   * 	- The `LinkedHashMap` argument used in the function is not mentioned in the return
   * statement.
   * 	- The returned `ParameterizedTypeName` object represents a parameterized type,
   * which can be further queried for its properties and attributes using various methods.
   */
  public static ParameterizedTypeName get(ParameterizedType type) {
    return get(type, new LinkedHashMap<>());
  }

  /**
   * generates a `ParameterizedTypeName` instance based on a given `ParameterizedType`
   * and `Map` of type variables. It returns a nested class structure with the raw type
   * name, the owner type, and the type arguments.
   * 
   * @param type parameterized type being processed, which is used to determine the
   * owner type and type arguments of the nested class.
   * 
   * 	- The `RawType` of `type` is obtained using `ClassName.get((Class<?>) type.getRawType())`.
   * 	- A check is performed to determine if the `OwnerType` of `type` is a
   * `ParameterizedType`, and if it's not, `null` is returned.
   * 	- The list of `ActualTypeArguments` of `type` is obtained using
   * `TypeName.list(type.getActualTypeArguments(), map)`.
   * 	- A new `ParameterizedTypeName` instance is created by combining the `RawType`,
   * `OwnerType`, and `ActualTypeArguments`.
   * 
   * @param map mapping of type variables to their actual types.
   * 
   * 	- `map` is a `Map<Type, TypeVariableName>` that contains information about the
   * type parameters of a `ParameterizedType`.
   * 	- The keys of `map` are `Type`, which represents the basic types in Java (e.g.,
   * `java.lang.String`).
   * 	- The values of `map` are `TypeVariableName`, which represents the type variables
   * declared in the `ParameterizedType`.
   * 	- The `map` object can be null if the `ParameterizedType` does not have any type
   * parameters.
   * 
   * @returns a `ParameterizedTypeName` object representing the parameterized type with
   * its owner type and type arguments.
   * 
   * 	- The ParameterizedTypeName object is created by combining the owner type (if
   * present), the raw type, and the list of type arguments.
   * 	- If the owner type is not null, it is used to create a nested class within the
   * raw type.
   * 	- The resulting ParameterizedTypeName object represents a parameterized type with
   * a nested class and type arguments.
   * 	- The `get` function returns a new instance of ParameterizedTypeName each time
   * it is called, even if the input parameters remain the same.
   * 	- The `get` function can be used to create a wide range of parameterized types,
   * including those with complex hierarchical structures.
   */
  static ParameterizedTypeName get(ParameterizedType type, Map<Type, TypeVariableName> map) {
    ClassName rawType = ClassName.get((Class<?>) type.getRawType());
    ParameterizedType ownerType = (type.getOwnerType() instanceof ParameterizedType)
        && !Modifier.isStatic(((Class<?>) type.getRawType()).getModifiers())
        ? (ParameterizedType) type.getOwnerType() : null;
    List<TypeName> typeArguments = TypeName.list(type.getActualTypeArguments(), map);
    return (ownerType != null)
        ? get(ownerType, map).nestedClass(rawType.simpleName(), typeArguments)
        : new ParameterizedTypeName(null, rawType, typeArguments);
  }
}
