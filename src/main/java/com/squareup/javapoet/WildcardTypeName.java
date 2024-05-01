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
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.Util.checkArgument;

/**
 * in Java is used to represent an unknown type that can extend or be extended by
 * another type. It has a list of upper and lower bounds, which can be used to infer
 * the type of a variable or expression. The class provides methods for getting the
 * type name based on a javax.lang.model.type.WildcardType mirror or a wildcard name,
 * and it also provides high-level functionality for working with wildcard types.
 */
public final class WildcardTypeName extends TypeName {
  public final List<TypeName> upperBounds;
  public final List<TypeName> lowerBounds;

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds) {
    this(upperBounds, lowerBounds, new ArrayList<>());
  }

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.upperBounds = Util.immutableList(upperBounds);
    this.lowerBounds = Util.immutableList(lowerBounds);

    checkArgument(this.upperBounds.size() == 1, "unexpected extends bounds: %s", upperBounds);
    for (TypeName upperBound : this.upperBounds) {
      checkArgument(!upperBound.isPrimitive() && upperBound != VOID,
          "invalid upper bound: %s", upperBound);
    }
    for (TypeName lowerBound : this.lowerBounds) {
      checkArgument(!lowerBound.isPrimitive() && lowerBound != VOID,
          "invalid lower bound: %s", lowerBound);
    }
  }

  /**
   * takes a list of annotations and returns a wildcard type name constructed from upper
   * and lower bounds and concatenated with the given annotations.
   * 
   * @param annotations list of AnnotationSpec objects to be concatenated with any upper
   * and lower bounds and the resulting WildcardTypeName is returned.
   * 
   * 	- Upper bounds: The type of the upper bound of the wildcard, which is represented
   * by the `upperBounds` parameter.
   * 	- Lower bounds: The type of the lower bound of the wildcard, which is represented
   * by the `lowerBounds` parameter.
   * 	- Concatenated annotations: The concatenation of the input `annotations`, which
   * are represented as a list of `AnnotationSpec` objects, resulting in a new `WildcardTypeName`.
   * 
   * @returns a `WildcardTypeName` object representing the intersection of the upper
   * and lower bounds of the annotations provided.
   * 
   * 	- The `WildcardTypeName` object represents a wildcard type name that consists of
   * an upper bound, lower bound, and concatenated annotations.
   * 	- The `upperBounds` field indicates the upper bound of the wildcard type, which
   * is a class or interface that represents the most specific type that can be used
   * in place of the wildcard type.
   * 	- The `lowerBounds` field indicates the lower bound of the wildcard type, which
   * is a class or interface that represents the least specific type that can be used
   * in conjunction with the wildcard type.
   * 	- The `concatAnnotations` method combines the annotations provided as input into
   * a single list, which is then passed as a parameter to the `WildcardTypeName`
   * constructor. This allows for the addition of additional information about the
   * wildcard type beyond its upper and lower bounds.
   */
  @Override public WildcardTypeName annotated(List<AnnotationSpec> annotations) {
    return new WildcardTypeName(upperBounds, lowerBounds, concatAnnotations(annotations));
  }

  /**
   * returns a `WildcardTypeName` instance that represents the intersection of the upper
   * and lower bounds of the type name.
   * 
   * @returns a `WildcardTypeName` instance representing the intersection of the upper
   * and lower bounds.
   * 
   * 	- The `TypeName` object is a WildcardTypeName instance that represents the
   * intersection of the upper and lower bounds of the function's input parameters.
   * 	- The `upperBounds` parameter is an array of TypeName objects, representing the
   * upper bounds of the function's input parameters.
   * 	- The `lowerBounds` parameter is an array of TypeName objects, representing the
   * lower bounds of the function's input parameters.
   * 
   * The output of the `withoutAnnotations` function can be used to determine the
   * wildcard type of a method's parameter types, which can be useful in various contexts
   * such as type inference and code analysis.
   */
  @Override public TypeName withoutAnnotations() {
    return new WildcardTypeName(upperBounds, lowerBounds);
  }

  /**
   * determines whether a type name is a supertype or an extending type based on the
   * size of its lower and upper bounds, and emits the appropriate syntax accordingly.
   * 
   * @param out output writer where the code is being generated.
   * 
   * 	- `out`: This is an instance of `CodeWriter`, which is a write-only stream for
   * generating Java code.
   * 	- `IOException`: This is a subclass of `Throwable` that represents an error or
   * exception occurring during serialization or deserialization. It can be thrown by
   * the `emit` function if there is an issue with the output stream.
   * 	- `lowerBounds`: This is a collection of type bounds represented as `TypeName`,
   * which are used to generate the code for the deserialized input. The size of this
   * collection is checked in the function to determine the appropriate code to emit.
   * 	- `upperBounds`: This is another collection of type bounds represented as `TypeName`,
   * which are used to generate the code for the deserialized input. The first element
   * of this collection is compared with `TypeName.OBJECT` to determine the appropriate
   * code to emit.
   * 
   * The `emit` function takes the output stream `out` as an argument and returns its
   * output. The properties of the output stream are not explicitly mentioned in the
   * function signature, but they can be inferred from the context of the function.
   * 
   * @returns a Java expression that determines the type of a given variable based on
   * its lower and upper bounds.
   * 
   * 	- If the size of the `lowerBounds` array is 1, the output is "? super $T", where
   * $T is the type of the variable being emitted.
   * 	- If the upper bounds of the `upperBounds` array is equal to `TypeName.OBJECT`,
   * the output is simply "?".
   * 	- Otherwise, the output is "? extends $T", where `$T` is the type of the variable
   * being emitted.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    if (lowerBounds.size() == 1) {
      return out.emit("? super $T", lowerBounds.get(0));
    }
    return upperBounds.get(0).equals(TypeName.OBJECT)
        ? out.emit("?")
        : out.emit("? extends $T", upperBounds.get(0));
  }

  /**
   * creates a wildcard type name by listing the upper bound type and an empty list of
   * types. This allows for any subtype of the upper bound to be returned as the wildcard
   * type name.
   * 
   * @param upperBound maximum possible type that can be substituted by the resulting
   * wildcard type.
   * 
   * 	- The type name is represented by an instance of the `TypeName` class.
   * 	- The `TypeName` object is constructed using a list of `TypeKind` objects that
   * define the type's kind (e.g., `CLASS`, `INTERFACE`, etc.).
   * 	- The `upperBound` parameter is a reference to a `TypeName` object representing
   * the upper bound of the wildcard type.
   * 	- The method returns a new instance of the `WildcardTypeName` class, which
   * represents a type that can be assigned any value that matches the upper bound
   * specified in the `upperBound` parameter.
   * 
   * @returns a wildcard type name representing the least upper bound of the input `TypeName`.
   * 
   * 	- The type name `WildcardTypeName` represents a wildcard type that can subtype
   * any other type.
   * 	- The `upperBound` parameter is passed to the constructor as a list of type names.
   * 	- The resulting wildcard type has a single bound, which is the same as the number
   * of types in the `upperBound` list.
   * 	- The list of bounds for the wildcard type is empty (`Collections.emptyList()`).
   */
  public static WildcardTypeName subtypeOf(TypeName upperBound) {
    return new WildcardTypeName(Collections.singletonList(upperBound), Collections.emptyList());
  }

  /**
   * returns the wildcard type name of an upper-bound type.
   * 
   * @param upperBound supertype of the wildcard type name to be checked for subtyping.
   * 
   * 	- `TypeName.get(upperBound)` returns the `TypeName` object associated with the `upperBound`.
   * 	- The `WildcardTypeName` return value represents a type name that is a wildcard,
   * indicating that it can represent any subtype of the specified type.
   * 
   * @returns a WildcardTypeName object representing the subtype of the provided
   * upper-bound type.
   * 
   * The output is a WildcardTypeName object, indicating that it represents a wildcard
   * type.
   * It is a subtype of the input argument, upperBound, which can be any Type or TypeName.
   * The returned type name can be used in a constraint to specify a type hierarchy or
   * relationship between types.
   */
  public static WildcardTypeName subtypeOf(Type upperBound) {
    return subtypeOf(TypeName.get(upperBound));
  }

  /**
   * creates a wildcard type name by combining an object type and the input parameter's
   * type, creating a hierarchy with the object type as the base.
   * 
   * @param lowerBound supertype of the wildcard type name that is being constructed.
   * 
   * 	- It is of type `TypeName`, which represents a type name in the Java programming
   * language.
   * 	- It contains a list of type arguments, represented by the `Collections.singletonList()`
   * method, which returns a list containing only one element.
   * 	- The list contains the input `lowerBound` itself, which is the type that the
   * wildcard type name will be based on.
   * 
   * The function then creates a new `WildcardTypeName` instance using these properties
   * and returns it.
   * 
   * @returns a wildcard type name representing the least upper bound of the input `lowerBound`.
   * 
   * The output is a WildcardTypeName object that represents a wildcard type, which is
   * a type that can represent any type.
   * 
   * The first element of the List returned by the function is always OBJECT, which
   * serves as the lower bound for the wildcard type.
   * 
   * The second element of the List is the input parameter passed to the function, which
   * acts as the upper bound for the wildcard type.
   * 
   * Therefore, the WildcardTypeName object returned by `supertypeOf` represents a
   * flexible and inclusive type that can encompass any type within its bounds.
   */
  public static WildcardTypeName supertypeOf(TypeName lowerBound) {
    return new WildcardTypeName(Collections.singletonList(OBJECT),
        Collections.singletonList(lowerBound));
  }

  /**
   * takes a `Type` object as input and returns the `WildcardTypeName` of its supertype.
   * 
   * @param lowerBound least-upper-bound type of the input value, which is used to
   * determine the supertype of the value.
   * 
   * 	- The `lowerBound` is a type parameter with the name `WildcardTypeName`.
   * 	- It is a subtype of `TypeName`, which means it is a type that can be used as a
   * type argument for a type parameter.
   * 	- The `lowerBound` has no explicit bounds, indicating that it can potentially be
   * applied to any type.
   * 	- The `lowerBound` is a primitive type, which means it is a basic type such as
   * `int`, `long`, or `double`.
   * 
   * @returns the wildcard type name of the upper bound of the type hierarchy.
   * 
   * 	- The output is a WildcardTypeName, indicating that it represents a type that may
   * contain any subtype of the specified lower bound.
   * 	- The type name returned is determined by the passed in lowerBound, which can be
   * any valid TypeName.
   * 	- The function returns the same result regardless of whether the input is a
   * primitive or non-primitive type.
   */
  public static WildcardTypeName supertypeOf(Type lowerBound) {
    return supertypeOf(TypeName.get(lowerBound));
  }

  /**
   * takes a `WildcardType` object `mirror` and returns a `TypeName` object representing
   * the mirrored type.
   * 
   * @param mirror Java wildcard type.
   * 
   * 	- The input parameter is of type `javax.lang.model.type.WildcardType`, indicating
   * that it can represent any type.
   * 	- The method takes a map as an argument to hold the result of the reflection process.
   * 
   * @returns a `TypeName`.
   * 
   * 	- `TypeName`: The output is of type `TypeName`.
   * 	- `javax.lang.model.type.WildcardType mirror`: This is the input parameter that
   * represents a wildcard type.
   * 	- `LinkedHashMap<>`: This is an empty map used as an argument to the `get` function.
   */
  public static TypeName get(javax.lang.model.type.WildcardType mirror) {
    return get(mirror, new LinkedHashMap<>());
  }

  /**
   * determines the least upper bound (LUB) of a wildcard type mirror based on its
   * extends and super bounds, and returns the corresponding subtype of `Object`.
   * 
   * @param mirror Java language model's type mirror, which contains information about
   * the type of a variable or expression.
   * 
   * 	- `mirror`: This is an instance of the `WildcardType` class, which represents a
   * wildcard type in Java programming language.
   * 	- `extendsBound`: This is a reference to the extends bound of the wildcard type,
   * which is another type mirror that represents the superclass of the wildcard type.
   * If this field is null, it means that the wildcard type does not have an extends bound.
   * 	- `superBound`: This is a reference to the superclass of the wildcard type, which
   * is also a type mirror. If this field is null, it means that the wildcard type has
   * no superclass.
   * 
   * Based on these properties, the function determines the subtype of the input wildcard
   * type and returns a `TypeName` object representing that subtype.
   * 
   * @param typeVariables mapping between the type parameters of the mirrored type and
   * their corresponding type variables, which are used to construct the resulting type
   * name.
   * 
   * 	- Map type: The `typeVariables` parameter is a map containing key-value pairs
   * where each key represents a type parameter element and each value represents the
   * corresponding type variable name.
   * 	- Key-value pair structure: Each key in the map is of type `TypeParameterElement`,
   * which is an abstract type representing a type parameter, while each value is of
   * type `TypeVariableName`, which is an abstract type representing a type variable.
   * 	- Number of elements: The map contains at most `Map` number of key-value pairs,
   * where `Map` is the class representing a map in Java.
   * 
   * The `get` function then proceeds to deserialize the input mirror and returns a `TypeName`.
   * 
   * @returns a `TypeName` instance that represents the result of subtyping the input
   * `WildcardType` with the bound of the extending or supertype, or the object type
   * if no bound is found.
   * 
   * 	- If the `extendsBound` is null, it means that the mirror does not have a bound,
   * and therefore the return type is `subtypeOf(Object.class)`.
   * 	- If the `superBound` is null, it means that the mirror has no supertype, and
   * therefore the return type is the same as the `extendsBound`.
   * 	- Otherwise, the return type is `subtypeOf(TypeName.get(extendsBound, typeVariables))`.
   * 
   * In summary, the `get` function returns a `TypeName` object that represents the
   * subtype of the mirror's bound, taking into account any type variables and bounds
   * provided in the `typeVariables` map.
   */
  static TypeName get(
      javax.lang.model.type.WildcardType mirror,
      Map<TypeParameterElement, TypeVariableName> typeVariables) {
    TypeMirror extendsBound = mirror.getExtendsBound();
    if (extendsBound == null) {
      TypeMirror superBound = mirror.getSuperBound();
      if (superBound == null) {
        return subtypeOf(Object.class);
      } else {
        return supertypeOf(TypeName.get(superBound, typeVariables));
      }
    } else {
      return subtypeOf(TypeName.get(extendsBound, typeVariables));
    }
  }

  /**
   * retrieves a type name associated with a given wildcard name and an empty map as argument.
   * 
   * @param wildcardName name of a type that can be any type, allowing the `get()`
   * method to return the appropriate subtype based on the name provided.
   * 
   * 	- `WildcardType`: This represents a type that can represent any value, including
   * other types and even itself.
   * 	- `LinkedHashMap`: This is an implementation of a map data structure that stores
   * key-value pairs in a linked list, allowing for efficient lookups and removals.
   * 
   * @returns a `TypeName` object representing the resolved type of the wildcard name.
   * 
   * 	- TypeName is the type name that is returned.
   * 	- It is a public static field.
   * 	- The field is initialized with the value of `wildcardName`.
   */
  public static TypeName get(WildcardType wildcardName) {
    return get(wildcardName, new LinkedHashMap<>());
  }

  /**
   * generates a `TypeName` object representing a wildcard type, based on the upper and
   * lower bounds of the wildcard name and a map of type variables.
   * 
   * @param wildcardName wildcard type being evaluated, and its upper and lower bounds
   * are retrieved from the `map` parameter.
   * 
   * 	- The `wildcardName` parameter is of type `WildcardType`, which represents a
   * wildcard type that can represent any type.
   * 	- The `list` method is used to create a list of upper and lower bounds for the
   * wildcard type, based on the `getUpperBounds()` and `getLowerBounds()` methods
   * called on the `wildcardName` object. These bounds determine the types that the
   * wildcard type can represent.
   * 
   * @param map map of type variables and their bounding types, which is used to compute
   * the upper and lower bounds of the wildcard type.
   * 
   * The function takes a `WildcardTypeName` object `wildcardName` as input and a map
   * `map`. The map contains type-variable names as keys and types as values.
   * 
   * @returns a `WildcardTypeName` object representing the intersection of the upper
   * and lower bounds of the wildcard name, based on the provided map.
   * 
   * 	- The output is a `WildcardTypeName` instance, which represents a type name that
   * can be used to generate a wildcard-compatible type.
   * 	- The first two elements of the list passed to the constructor represent the upper
   * and lower bounds of the wildcard, respectively. These are obtained by calling the
   * `getUpperBounds` and `getLowerBounds` methods on the `WildcardTypeName` instance.
   * 	- The remaining elements in the lists are the type variables associated with the
   * wildcard, which are also obtained from the `Map` passed to the constructor. Each
   * element in the list is a `TypeVariableName` instance representing a unique type variable.
   */
  static TypeName get(WildcardType wildcardName, Map<Type, TypeVariableName> map) {
    return new WildcardTypeName(
        list(wildcardName.getUpperBounds(), map),
        list(wildcardName.getLowerBounds(), map));
  }
}
