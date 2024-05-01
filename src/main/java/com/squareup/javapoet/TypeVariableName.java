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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * is used to represent a type variable in Java. It has several fields such as name,
 * bounds, and annotations. The class provides methods for emitting the type variable
 * in a code file, getting the type variable without bounds, and making a new type
 * variable with specific bounds. Additionally, it includes utility methods for working
 * with type variables, such as getting a type variable equivalent to an element or
 * mirror, or creating a new type variable based on a given type mirror.
 */
public final class TypeVariableName extends TypeName {
  public final String name;
  public final List<TypeName> bounds;

  private TypeVariableName(String name, List<TypeName> bounds) {
    this(name, bounds, new ArrayList<>());
  }

  private TypeVariableName(String name, List<TypeName> bounds, List<AnnotationSpec> annotations) {
    super(annotations);
    this.name = checkNotNull(name, "name == null");
    this.bounds = bounds;

    for (TypeName bound : this.bounds) {
      checkArgument(!bound.isPrimitive() && bound != VOID, "invalid bound: %s", bound);
    }
  }

  /**
   * creates a new `TypeVariableName` instance with the given name, bounds, and annotations.
   * 
   * @param annotations list of AnnotationSpec objects that are to be applied to the
   * newly created TypeVariableName object.
   * 
   * 	- `List<AnnotationSpec>` represents an ordered list of annotation specifications,
   * which are collections of key-value pairs used to add annotations to a type or method.
   * 	- `name` is a String representing the name of the type variable being annotated.
   * 	- `bounds` is a String representing the bounds of the type variable, specifying
   * the minimum and maximum values that the variable can take on.
   * 	- The `annotations` parameter is a list of AnnotationSpec objects, each containing
   * an annotation key-value pair.
   * 
   * @returns a `TypeVariableName` object representing the specified type variable with
   * its name, bounds, and annotations.
   * 
   * 	- The output is of type `TypeVariableName`, indicating that it represents a type
   * variable with a specific name, bounds, and annotations.
   * 	- The name property is the unique identifier for the type variable, which is
   * specified in the input parameter list.
   * 	- The bounds property indicates the allowed types for the type variable, which
   * can be either a single type or a range of types as specified in the input parameter
   * list.
   * 	- The annotations property contains a list of annotations associated with the
   * type variable, which can include information about its usage and constraints.
   */
  @Override public TypeVariableName annotated(List<AnnotationSpec> annotations) {
    return new TypeVariableName(name, bounds, annotations);
  }

  /**
   * generates a new `TypeName` object by combining the name and bounds parameters into
   * a single object of type `TypeVariableName`.
   * 
   * @returns a `TypeVariableName` instance representing the type variable `name` with
   * bounds `bounds`.
   * 
   * 	- TypeName: The output is a new TypeVariableName object, which represents a type
   * variable with the given name and bounds.
   * 	- Name: The name of the type variable.
   * 	- Bounds: The bounds of the type variable, which specifies the set of values that
   * the type variable can take.
   */
  @Override public TypeName withoutAnnotations() {
    return new TypeVariableName(name, bounds);
  }

  /**
   * takes a list of type arguments and returns another function that has the same type
   * signature but with the bounds enforced by the passed types.
   * 
   * @returns a `TypeVariableName` instance with the specified bounds.
   * 
   * 	- The return type is `TypeVariableName`, indicating that it is a variable with
   * an unknown type.
   * 	- The input parameter `bounds` is a list of types, which are used to constrain
   * the possible values for the type variable.
   * 	- The function returns a new type variable with the specified bounds, allowing
   * for more flexible and expressive type systems in Java programming.
   */
  public TypeVariableName withBounds(Type... bounds) {
    return withBounds(TypeName.list(bounds));
  }

  /**
   * takes an array of `TypeName` objects as input and returns a new instance of the
   * same type with the provided bounds.
   * 
   * @returns an instance of `TypeVariableName` with the specified `TypeName` arguments.
   * 
   * The `TypeVariableName` with bounds is generated based on the provided `TypeName`
   * parameters.
   * 
   * The `TypeName` parameters are used to define the bounds of the type variable.
   * 
   * The returned value is a `TypeVariableName` object, which represents an uninstantiated
   * type variable with the specified bounds.
   */
  public TypeVariableName withBounds(TypeName... bounds) {
    return withBounds(Arrays.asList(bounds));
  }

  /**
   * modifies a given `TypeVariableName` by adding the provided `List` of types to its
   * existing `List` of bounds, then returns a new `TypeVariableName` with the updated
   * bounds.
   * 
   * @param bounds list of type variables that will be added to the existing bounds of
   * the current type variable.
   * 
   * 	- `List<? extends TypeName>` represents an unbounded list of type names, which
   * can be any subtype of `TypeName`.
   * 	- `new ArrayList<TypeName>()` creates a new empty list.
   * 	- `addAll(this.bounds)` adds all the elements of the `bounds` field to the new list.
   * 	- `addAll(bounds)` adds all the elements of the `bounds` argument to the new list.
   * 	- `TypeVariableName` is a generic type with three parameters: `name`, `bounds`,
   * and `annotations`.
   * 
   * @returns a new `TypeVariableName` instance with the combined bounds of both the
   * original and provided lists.
   * 
   * 	- Name: The name of the type variable is the same as the input parameter `name`.
   * 	- Bounds: The list of bounds has been modified to include all the elements from
   * both the current bounds and the input `bounds` parameter.
   * 	- Annotations: The annotations remain unchanged and are inherited from the original
   * type variable.
   */
  public TypeVariableName withBounds(List<? extends TypeName> bounds) {
    ArrayList<TypeName> newBounds = new ArrayList<>();
    newBounds.addAll(this.bounds);
    newBounds.addAll(bounds);
    return new TypeVariableName(name, newBounds, annotations);
  }

  /**
   * strips `java.lang.Object` from a list of type names (`bounds`) and returns a new
   * `TypeVariableName` instance with the specified name and a modified list of types
   * that does not include `Object`.
   * 
   * @param name name of the type variable to be created.
   * 
   * @param bounds constraints on the type variable being defined, which are removed
   * from the list if the `OBJECT` element is present.
   * 
   * 1/ `boundsNoObject`: This is a list of type names without the `java.lang.Object`
   * entry.
   * 2/ `Collections.unmodifiableList()`: This ensures that the list cannot be modified
   * after creation, which is a requirement for immutability in Java.
   * 
   * @returns a `TypeVariableName` object representing the specified `name` with a
   * non-empty list of type parameters.
   * 
   * 	- The output is a `TypeVariableName`, which represents a type variable with the
   * given name `name`.
   * 	- The list of type names `boundsNoObject` contains all types in the input list
   * `bounds`, excluding `OBJECT`.
   * 	- The `TypeVariableName` instance is immutable and unmodifiable.
   */
  private static TypeVariableName of(String name, List<TypeName> bounds) {
    // Strip java.lang.Object from bounds if it is present.
    List<TypeName> boundsNoObject = new ArrayList<>(bounds);
    boundsNoObject.remove(OBJECT);
    return new TypeVariableName(name, Collections.unmodifiableList(boundsNoObject));
  }

  /**
   * emits annotations and then indents the code for a specific `name`.
   * 
   * @param out output writer to which the code will be written.
   * 
   * 	- `out`: A CodeWriter object that represents the current position in the code
   * generation process. It can be used to write code to the output stream.
   * 	- `name`: The name of the function or method being generated.
   * 
   * @returns a Java method that writes annotations and indents the code for the given
   * `name`.
   * 
   * 	- `out`: The output writer object that is used to write the code.
   * 	- `name`: The name of the method or class being emitted.
   * 
   * The `emit` function first emits any annotations associated with the method or
   * class, and then returns the output writer object so that it can be indented and
   * more code can be written.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    emitAnnotations(out);
    return out.emitAndIndent(name);
  }

  /**
   * returns a `TypeVariableName` object representing the type variable with the given
   * name and an empty list of constraints.
   * 
   * @param name name of the type variable to be returned by the `get()` function.
   * 
   * @returns a `TypeVariableName` object representing the specified `String` name,
   * with an empty list of arguments.
   * 
   * The TypeVariableName object returned is created with the name parameter as the
   * basis for its identity.
   * 
   * The Collections.emptyList() method creates an empty list as the sole value of the
   * returned object's type parameters.
   */
  public static TypeVariableName get(String name) {
    return TypeVariableName.of(name, Collections.emptyList());
  }

  /**
   * creates a new instance of the `TypeVariableName` class with a specified name and
   * bounds list.
   * 
   * @param name name of the type variable to be created.
   * 
   * @returns a `TypeVariableName` object representing the specified type variable with
   * the given name and bounds.
   * 
   * TypeVariableName is a class that represents a type variable in Java.
   * The name parameter signifies the name of the type variable, which is a string.
   * The bounds parameter is an array of TypeName objects, representing the bounds of
   * the type variable.
   * Each TypeName object in the bounds array represents a bound on the type variable,
   * which can be a class or interface type.
   */
  public static TypeVariableName get(String name, TypeName... bounds) {
    return TypeVariableName.of(name, Arrays.asList(bounds));
  }

  /**
   * returns a `TypeVariableName` object based on a given name and type parameters.
   * 
   * @param name name of the type variable to be created.
   * 
   * @returns a `TypeVariableName` instance representing the combination of the given
   * `name` and `bounds`.
   * 
   * 	- The type variable name is specified using the `TypeVariableName.of()` method.
   * 	- The `name` parameter represents the name of the type variable being generated.
   * 	- The `bounds` parameter represents a list of types that define the bounds of the
   * type variable.
   */
  public static TypeVariableName get(String name, Type... bounds) {
    return TypeVariableName.of(name, TypeName.list(bounds));
  }

  /**
   * recursively retrieves a type variable value by traversing the tree representation
   * of a given type variable reference, reaching the root element and returning its value.
   * 
   * @param mirror type variable to which the method's result should be restricted.
   * 
   * 	- `mirror`: A `TypeVariableName` object that represents a type variable in the
   * input deserialized data.
   * 
   * @returns a `TypeVariableName`.
   * 
   * 	- The output is of type `TypeVariableName`.
   * 	- It is generated by calling the internal method `get` on the input parameter
   * `mirror`, which is an instance of `TypeParameterElement`.
   * 	- The output refers to a type variable name.
   */
  public static TypeVariableName get(TypeVariable mirror) {
    return get((TypeParameterElement) mirror.asElement());
  }

  /**
   * generates a new `TypeVariableName` instance based on a given `TypeParameterElement`.
   * It retrieves the element's bounds and adds them to a list, then creates a new
   * instance of `TypeVariableName` with the list of bounds. Finally, it returns the
   * new instance.
   * 
   * @param mirror TypeParameterElement that the function operates on, providing the
   * necessary context for the function to access and manipulate the type variables
   * associated with it.
   * 
   * 	- `mirror` is of type `TypeVariable`, indicating that it is a type variable with
   * an unresolved type.
   * 	- `asElement()` method is called on `mirror`, which returns a `TypeParameterElement`.
   * 	- `typeVariables` is a map containing the type parameters and their corresponding
   * type variables.
   * 	- The `get` method retrieves the type variable associated with a given type
   * parameter element using the `typeVariables` map. If the type variable is not found
   * in the map, it is created and added to the map.
   * 	- The `bounds` field of the type parameter element is a list of type names, which
   * are used to constrain the types that can be assigned to the type variable.
   * 	- The `visibleBounds` field is an unmodifiable list of type names, which represents
   * the bounds of the type variable in the context of the type parameter element.
   * 
   * @param typeVariables map of type parameters and their corresponding type variables,
   * which is used to look up the type variable for each type parameter element in the
   * mirror object.
   * 
   * 	- `typeVariables`: A map containing type variables and their corresponding values.
   * 	- `element`: The element being processed, which is a `TypeParameterElement`.
   * 	- `mirror`: The mirrored object representing the element.
   * 	- `bounds`: A list of type names representing the bounds of the element.
   * 	- `visibleBounds`: An unmodifiable list of the same as `bounds`, used for the
   * type variable name.
   * 
   * @returns a `TypeVariableName` instance representing the type variable with a bound
   * list that is an unmodifiable list of type names.
   * 
   * 	- `TypeVariableName` represents a type variable with the given element's name and
   * bounds.
   * 	- The `typeVariables` map is used to store the type variables created for each element.
   * 	- The `bounds` list contains the bounds of the type variable, which is an
   * unmodifiable list to ensure immutability.
   * 	- The `visibleBounds` list is a subset of the `bounds` list that contains only
   * the visible bounds, which are the bounds that can be seen by the caller.
   * 	- The `OBJECT` element is removed from the `bounds` list to avoid including it
   * in the returned type variable.
   */
  static TypeVariableName get(
      TypeVariable mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
    TypeParameterElement element = (TypeParameterElement) mirror.asElement();
    TypeVariableName typeVariableName = typeVariables.get(element);
    if (typeVariableName == null) {
      // Since the bounds field is public, we need to make it an unmodifiableList. But we control
      // the List that that wraps, which means we can change it before returning.
      List<TypeName> bounds = new ArrayList<>();
      List<TypeName> visibleBounds = Collections.unmodifiableList(bounds);
      typeVariableName = new TypeVariableName(element.getSimpleName().toString(), visibleBounds);
      typeVariables.put(element, typeVariableName);
      for (TypeMirror typeMirror : element.getBounds()) {
        bounds.add(TypeName.get(typeMirror, typeVariables));
      }
      bounds.remove(OBJECT);
    }
    return typeVariableName;
  }

  /**
   * returns a `TypeVariableName` object representing a type variable with the given
   * name and bounds.
   * 
   * @param element TypeParameterElement for which the type variable is being generated.
   * 
   * 	- `element.getSimpleName().toString()` extracts the name of the type variable.
   * 	- `element.getBounds()` returns a list of type mirrors representing the bounds
   * of the type variable.
   * 
   * The type variable is then constructed by combining the name and bounds using the
   * `TypeVariableName.of` method.
   * 
   * @returns a `TypeVariableName` object representing the type variable with the given
   * name and bounds.
   * 
   * 	- `TypeVariableName.of(name, boundsTypeNames)` is a `TypeVariableName` object
   * that represents a type variable with the given name and bounds type names.
   * 	- `name` is a `String` representing the name of the type variable.
   * 	- `boundsTypeNames` is a `List<TypeName>` containing the types that the type
   * variable can take, according to its bounds.
   * 
   * The output of the `get` function is a `TypeVariableName` object with the given properties.
   */
  public static TypeVariableName get(TypeParameterElement element) {
    String name = element.getSimpleName().toString();
    List<? extends TypeMirror> boundsMirrors = element.getBounds();

    List<TypeName> boundsTypeNames = new ArrayList<>();
    for (TypeMirror typeMirror : boundsMirrors) {
      boundsTypeNames.add(TypeName.get(typeMirror));
    }

    return TypeVariableName.of(name, boundsTypeNames);
  }

  /**
   * returns a `TypeVariableName` object based on the provided `TypeVariable` and an
   * empty map.
   * 
   * @param type type variable to be looked up in the map.
   * 
   * 	- `type` is an instance of `TypeVariable`, which represents a variable type in
   * Java reflection.
   * 	- It has a generic type parameter, indicated by the `<>` symbol, which indicates
   * that the type parameter is not specified at compile-time but can be inferred from
   * the context.
   * 	- The function takes another argument, `map`, which is an instance of `LinkedHashMap`.
   * This map contains key-value pairs representing the attributes of the deserialized
   * input type.
   * 
   * @returns a `TypeVariableName`.
   * 
   * 	- TypeVariableName is the type variable name that is being retrieved.
   * 	- The return value is of type TypeVariableName.
   * 	- The type parameter of the function, `type`, represents the type of the type
   * variable being retrieved.
   * 	- The function uses a LinkedHashMap to store the type variable, which is used to
   * retrieve the type variable's name.
   */
  public static TypeVariableName get(java.lang.reflect.TypeVariable<?> type) {
    return get(type, new LinkedHashMap<>());
  }

  /**
   * maps a type variable to a unique name based on its bounds, and stores it in a map
   * for future use.
   * 
   * @param type type variable for which a new name is being generated.
   * 
   * 1/ The `type` parameter is an instance of `java.lang.reflect.TypeVariable<?>`,
   * indicating that it is a type variable with an unknown type.
   * 2/ The `map` parameter is a mapping of types to their corresponding type variables.
   * 3/ The function returns a new type variable instance with the same name as the
   * input `type`.
   * 4/ If the input `type` has no bound, the function creates a new list of type names
   * and adds it to the map as the type variable's bounds.
   * 5/ If the input `type` has bounds, the function iterates over them and adds each
   * bound to the list of type names. The `OBJECT` bound is always removed from the list.
   * 
   * The return value of the function is a new instance of `TypeVariableName`.
   * 
   * @param map Map<Type, TypeVariableName> that contains mappings of types to their
   * corresponding type variable names.
   * 
   * 	- `map` is a `Map` object that stores type variables and their corresponding names.
   * 	- The `map` has a key-value pair structure, where each key represents a type
   * variable and its corresponding value represents the name of the type variable.
   * 	- The `map` is immutable, meaning it cannot be modified once it is created.
   * 	- The `map` contains a list of `TypeName` objects that represent the bounds of
   * each type variable. These bounds can be used to determine the scope of the type variable.
   * 
   * @returns a `TypeVariableName` object representing the given type variable with its
   * bounds.
   * 
   * 	- `result`: The TypeVariableName object that is created and returned by the function.
   * 	- `map`: A map that contains mappings between types and TypeVariableNames.
   * 	- `type`: The type variable for which a TypeVariableName is being sought.
   * 	- `bounds`: A list of type bounds associated with the type variable.
   * 	- `visibleBounds`: An immutable view of the `bounds` list, excluding the `OBJECT`
   * bound.
   * 
   * The function first checks if a TypeVariableName already exists in the `map` for
   * the given type variable. If one does not exist, it creates a new TypeVariableName
   * object with the type variable's name and a list of type bounds associated with it.
   * The function then adds the newly created TypeVariableName to the `map`. Finally,
   * the function returns the newly created TypeVariableName object.
   */
  static TypeVariableName get(java.lang.reflect.TypeVariable<?> type,
      Map<Type, TypeVariableName> map) {
    TypeVariableName result = map.get(type);
    if (result == null) {
      List<TypeName> bounds = new ArrayList<>();
      List<TypeName> visibleBounds = Collections.unmodifiableList(bounds);
      result = new TypeVariableName(type.getName(), visibleBounds);
      map.put(type, result);
      for (Type bound : type.getBounds()) {
        bounds.add(TypeName.get(bound, map));
      }
      bounds.remove(OBJECT);
    }
    return result;
  }
}
