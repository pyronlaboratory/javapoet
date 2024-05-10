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
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

import static com.squareup.javapoet.Util.characterLiteralWithoutSingleQuotes;
import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * in Java is used to represent an annotation and its associated values. It has various
 * methods for adding members to the annotation, such as addMember(String name, String
 * format, Object... args) for adding a member with a specific format and arguments,
 * or addMemberForValue(String memberName, Object value) for adding a member based
 * on the class of the given object. The Builder class is used to create a new
 * AnnotationSpec instance, and the Visitor class is used as an annotation value
 * visitor that adds members to the builder instance.
 */
public final class AnnotationSpec {
  public static final String VALUE = "value";

  public final TypeName type;
  public final Map<String, List<CodeBlock>> members;

  private AnnotationSpec(Builder builder) {
    this.type = builder.type;
    this.members = Util.immutableMultimap(builder.members);
  }

 
  void emit(CodeWriter codeWriter, boolean inline) throws IOException {
    String whitespace = inline ? "" : "\n";
    String memberSeparator = inline ? ", " : ",\n";
    if (members.isEmpty()) {
      // @Singleton
      codeWriter.emit("@$T", type);
    } else if (members.size() == 1 && members.containsKey("value")) {
      // @Named("foo")
      codeWriter.emit("@$T(", type);
      emitAnnotationValues(codeWriter, whitespace, memberSeparator, members.get("value"));
      codeWriter.emit(")");
    } else {
      // Inline:
      //   @Column(name = "updated_at", nullable = false)
      //
      // Not inline:
      //   @Column(
      //       name = "updated_at",
      //       nullable = false
      //   )
      codeWriter.emit("@$T(" + whitespace, type);
      codeWriter.indent(2);
      for (Iterator<Map.Entry<String, List<CodeBlock>>> i
          = members.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry<String, List<CodeBlock>> entry = i.next();
        codeWriter.emit("$L = ", entry.getKey());
        emitAnnotationValues(codeWriter, whitespace, memberSeparator, entry.getValue());
        if (i.hasNext()) codeWriter.emit(memberSeparator);
      }
      codeWriter.unindent(2);
      codeWriter.emit(whitespace + ")");
    }
  }

  /**
   * indents and emits the values of an annotation using a provided member separator.
   * It also handles when there is only one value to emit.
   * 
   * @param codeWriter code generator that emits the Java code into a file or other
   * output source.
   * 
   * 	- `codeWriter`: A CodeWriter object that is used to write Java code.
   * 	- `whitespace`: A string that represents the indentation level for the annotated
   * values.
   * 	- `memberSeparator`: A string that represents the separator between member values.
   * 	- `values`: A list of CodeBlock objects that contain the annotated values.
   * 
   * @param whitespace 2-3 spaces used to indent the code blocks in the output.
   * 
   * @param memberSeparator character or sequence of characters used to separate each
   * member of the list of CodeBlocks emitted by the function.
   * 
   * @param values list of `CodeBlock` objects to be emitted.
   * 
   * 	- `CodeBlock` is the type of the elements in the list.
   * 	- `List<CodeBlock>` represents a collection of CodeBlock objects.
   * 	- `values.size()` returns the number of elements in the list.
   * 	- `values.get(0)` returns the first element of the list.
   * 	- `whitespace` is a string that represents the amount of indentation required for
   * each block of code.
   * 	- `memberSeparator` is a string that separates the elements of the list when they
   * are emitted.
   */
  private void emitAnnotationValues(CodeWriter codeWriter, String whitespace,
      String memberSeparator, List<CodeBlock> values) throws IOException {
    if (values.size() == 1) {
      codeWriter.indent(2);
      codeWriter.emit(values.get(0));
      codeWriter.unindent(2);
      return;
    }

    codeWriter.emit("{" + whitespace);
    codeWriter.indent(2);
    boolean first = true;
    for (CodeBlock codeBlock : values) {
      if (!first) codeWriter.emit(memberSeparator);
      codeWriter.emit(codeBlock);
      first = false;
    }
    codeWriter.unindent(2);
    codeWriter.emit(whitespace + "}");
  }

  /**
   * retrieves an `AnnotationSpec` instance based on a given `Annotation` object and a
   * boolean parameter indicating whether to include default values.
   * 
   * @param annotation annotation to be fetched.
   * 
   * 	- It is an AnnotationSpec object representing a custom annotation.
   * 	- The `get` method returns this AnnotationSpec object after performing any necessary
   * initialization based on the provided `boolean` parameter.
   * 	- The `annotation` argument is passed as a reference to the function, allowing
   * it to be manipulated or utilized further within the code.
   * 
   * @returns an `AnnotationSpec` object containing the specified annotation's metadata.
   * 
   * 1/ The type of the output is `AnnotationSpec`, which is an object representing an
   * annotation specification in Java.
   * 2/ The input parameter `annotation` is passed to the function and used to generate
   * the output.
   * 3/ The second input parameter `recursive` has a default value of `false`, indicating
   * that the function does not perform recursive queries for annotations.
   */
  public static AnnotationSpec get(Annotation annotation) {
    return get(annotation, false);
  }

  /**
   * retrieves the values of an annotation and its members based on a specified method
   * name and compares them to the default value before adding them to a builder object
   * for further processing.
   * 
   * @param annotation annotation object whose members will be retrieved and included
   * in the resulting AnnotationSpec.
   * 
   * 	- `annotation`: A `Annotation` object that represents the annotation to be deserialized.
   * 	- `includeDefaultValues`: A boolean indicating whether default values should be
   * included in the summary.
   * 	- `methods`: An array of `Method` objects representing the methods available on
   * the `annotation` class.
   * 	- `value`: The value of the `method` invocation for the current iteration, which
   * can be an array or a single object instance.
   * 
   * @param includeDefaultValues state of whether to include default values for each
   * method when building the AnnotationSpec.
   * 
   * @returns an `AnnotationSpec` object representing the annotations of the given class.
   * 
   * 	- The output is an `AnnotationSpec` object, which represents an annotation specification.
   * 	- The `builder` parameter is used to create an empty annotation builder instance,
   * which is then populated with the annotations from the `annotation` parameter.
   * 	- The `includeDefaultValues` parameter is used to determine whether default values
   * should be included in the output. If set to `true`, default values will be included
   * for any methods that do not have a non-default value.
   * 	- The `Method[]` array contains the declared methods of the annotation type, which
   * are sorted alphabetically using the `Comparator.comparing(Method::getName)`
   * comparison method.
   * 	- For each method in the `Methods` array, the `Object` value is retrieved by
   * invoking the `Method.invoke()` method on the `annotation` object. If the
   * `includeDefaultValues` parameter is set to `true`, any default values will be
   * ignored. Otherwise, the output will include both the non-default and default values
   * for each method.
   * 	- If the `value` is an array, it is looped over using the `for` loop, and each
   * element is added to the `builder` instance using the appropriate method name
   * (`addMemberForValue()` or `addMember()`). The type of the value is ignored, as
   * only the method name and value are used.
   * 	- If the `value` is an annotation instance, it is recursively processed by calling
   * the `get()` function again with the same parameters. The resulting annotation
   * specification will be added to the `builder` instance using the `addMember()` method.
   */
  public static AnnotationSpec get(Annotation annotation, boolean includeDefaultValues) {
    Builder builder = builder(annotation.annotationType());
    try {
      Method[] methods = annotation.annotationType().getDeclaredMethods();
      Arrays.sort(methods, Comparator.comparing(Method::getName));
      for (Method method : methods) {
        Object value = method.invoke(annotation);
        if (!includeDefaultValues) {
          if (Objects.deepEquals(value, method.getDefaultValue())) {
            continue;
          }
        }
        if (value.getClass().isArray()) {
          for (int i = 0; i < Array.getLength(value); i++) {
            builder.addMemberForValue(method.getName(), Array.get(value, i));
          }
          continue;
        }
        if (value instanceof Annotation) {
          builder.addMember(method.getName(), "$L", get((Annotation) value));
          continue;
        }
        builder.addMemberForValue(method.getName(), value);
      }
    } catch (Exception e) {
      throw new RuntimeException("Reflecting " + annotation + " failed!", e);
    }
    return builder.build();
  }

  /**
   * generates an `AnnotationSpec` instance for a given `AnnotationMirror`. It creates
   * a `Builder` instance and recursively visits each element value using a `Visitor`
   * class, adding the values to the `Builder`. The resulting `AnnotationSpec` instance
   * is returned.
   * 
   * @param annotation annotation for which the method is generating an AnnotationSpec.
   * 
   * 	- `TypeElement element`: Refers to the type element of the annotation type.
   * 	- `AnnotationSpec.Builder builder`: A builder for creating an annotation spec.
   * 	- `Visitor visitor`: An object that traverses the annotations on the element and
   * updates the `builder`.
   * 	- `executableElement`: Iterates over the key-value pairs of the annotation's
   * elements, where each key is a simple name of an executable element.
   * 	- `AnnotationValue value`: The current annotation value being processed in the iteration.
   * 
   * @returns an `AnnotationSpec` instance representing the annotation with its element
   * values applied.
   * 
   * 	- The output is an instance of `AnnotationSpec`, which represents an annotation
   * on a type or method.
   * 	- The type of the `AnnotationSpec` is determined by the `TypeElement` parameter
   * passed to the function, which represents the type or method for which the annotation
   * is defined.
   * 	- The `Builder` instance used to construct the `AnnotationSpec` contains information
   * about the annotation, including its name and any element values it may have.
   * 	- The `Visitor` instance passed to the `accept` method of the `AnnotationValue`
   * objects is responsible for visiting each element value in the annotation and adding
   * it to the `Builder`.
   * 	- Each element value is represented by an `ExecutableElement` object, which
   * contains information about the name and value of the element.
   * 	- The `accept` method is called on each element value in the annotation, and its
   * `visit` method is used to determine how the element value should be added to the
   * `Builder`.
   */
  public static AnnotationSpec get(AnnotationMirror annotation) {
    TypeElement element = (TypeElement) annotation.getAnnotationType().asElement();
    AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get(element));
    Visitor visitor = new Visitor(builder);
    for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
      String name = executableElement.getSimpleName().toString();
      AnnotationValue value = annotation.getElementValues().get(executableElement);
      value.accept(visitor, name);
    }
    return builder.build();
  }

  /**
   * creates a new instance of the `Builder` class with the specified type parameter.
   * It checks that the type parameter is not null and then returns a new Builder instance.
   * 
   * @param type class name of an object that the `Builder` instance will be created
   * for, and is used to initialize the new `Builder` instance with the appropriate type.
   * 
   * 	- `type` is not null, ensured by the `checkNotNull` method call.
   * 	- The `Builder` constructor is called with `type` as its argument, initiating a
   * new instance of the builder class.
   * 
   * @returns a new instance of the `Builder` class initialized with the given type.
   * 
   * 	- The `ClassName` type parameter is null-checked before returning a new instance
   * of the `Builder` class.
   * 	- A new instance of the `Builder` class is created and returned with the given `type`.
   * 	- The `Builder` class represents a builder pattern, allowing for the creation of
   * objects without affecting the original state of the object being built.
   */
  public static Builder builder(ClassName type) {
    checkNotNull(type, "type == null");
    return new Builder(type);
  }

  /**
   * creates a new instance of a class, given its fully qualified name.
   * 
   * @param type Class object that defines the Java class to be built by the `Builder`
   * object returned by the function.
   * 
   * 	- `Class<?>` represents the type of object to be created by the builder.
   * 	- `ClassName.get(type)` returns a string representation of the class name of the
   * input `type`.
   * 
   * @returns a new `Builder` instance of the specified class type.
   * 
   * 1/ The type parameter `type` represents the class to be built.
   * 2/ The method `ClassName.get(type)` is used to obtain the fully qualified name of
   * the class.
   * 3/ The returned `Builder` instance is an immutable reference to the class, allowing
   * modifications to the class definition without affecting any existing instances.
   */
  public static Builder builder(Class<?> type) {
    return builder(ClassName.get(type));
  }

  /**
   * creates a new instance of the `Builder` class with the same type as the original
   * object, and replicates its members into a new list.
   * 
   * @returns a new `Builder` object containing copies of the original members.
   * 
   * The output is a `Builder` object representing the same class as the original
   * instance, but with all members recursively converted to lists.
   * The `Builder` object has a `type` field indicating the type of the original instance.
   * The `members` field contains maps with key-value pairs representing each member
   * in the original instance's members, where each map contains a string key and a
   * list value.
   */
  public Builder toBuilder() {
    Builder builder = new Builder(type);
    for (Map.Entry<String, List<CodeBlock>> entry : members.entrySet()) {
      builder.members.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return builder;
  }

  /**
   * checks whether an object is equal to the current object based on the object's class
   * and a string representation comparison.
   * 
   * @param o object being compared to the current object, and is used to determine if
   * the two objects are equal.
   * 
   * 	- If this object is the same as `o`, the method returns `true`.
   * 	- If `o` is null, the method returns `false`.
   * 	- If the classes of this and `o` are different, the method returns `false`.
   * 	- Otherwise, the method compares the strings representing this and `o`. If they
   * are equal, the method returns `true`. Otherwise, it returns `false`.
   * 
   * @returns a boolean value indicating whether the object is equal to the current object.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its input, which is a string representation of the current
   * object.
   * 
   * @returns the result of hashing the `toString()` representation of the object.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of an object. It uses a `CodeWriter` to generate
   * the string, and catches any exceptions that may occur during the process.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      codeWriter.emit("$L", this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * is a utility class for creating AnnotationSpec instances in Java. It provides a
   * structured way to add members (i.e., annotations) to an annotation spec instance,
   * using a builder pattern. The class has several methods for adding members of
   * different types, such as strings, numbers, classes, enums, and characters, as well
   * as a `build()` method to create the final AnnotationSpec instance.
   */
  public static final class Builder {
    private final TypeName type;

    public final Map<String, List<CodeBlock>> members = new LinkedHashMap<>();

    private Builder(TypeName type) {
      this.type = type;
    }

    /**
     * allows for the addition of a member to an object of the `Builder` class, using a
     * specified name and format string along with arbitrary arguments.
     * 
     * @param name name of the member to be added.
     * 
     * @param format code snippet that will be executed when the member is added to the
     * application.
     * 
     * @returns a new `Builder` object with the specified member added to its configuration.
     * 
     * 	- The `Builder` type hint indicates that the function returns an instance of a
     * `Builder` class.
     * 	- The `String` parameter `name` represents the name of the member to be added.
     * 	- The `String` parameter `format` represents the format string for the member addition.
     * 	- The `Object...` parameter `args` represents the arguments passed to the
     * `CodeBlock.of()` method, which creates a new `CodeBlock` instance from the format
     * string and arguments.
     * 
     * The output of the function is a new `CodeBlock` instance that contains the added
     * member.
     */
    public Builder addMember(String name, String format, Object... args) {
      return addMember(name, CodeBlock.of(format, args));
    }

    /**
     * adds a code block to an existing list of code blocks for a given member name in a
     * builder object.
     * 
     * @param name name of the member to be added to the list of code blocks associated
     * with the builder object.
     * 
     * @param codeBlock code snippet to be added to the member list for the given `name`.
     * 
     * 	- `name`: A string representing the name of the member to be added.
     * 	- `codeBlock`: The CodeBlock object containing the code to be executed for the
     * named member.
     * 
     * @returns a modified instance of the `Builder` class, with the added code block
     * associated with the specified name.
     * 
     * 	- `List<CodeBlock> values`: This is a list of code blocks associated with the
     * given name.
     * 	- `k -> new ArrayList<>()`: This is an implementation of the `computeIfAbsent`
     * method that returns a newly created list if the key is not present in the map, or
     * the existing list associated with the key if it already exists.
     * 	- `this`: This refers to the current instance of the `Builder` class.
     */
    public Builder addMember(String name, CodeBlock codeBlock) {
      List<CodeBlock> values = members.computeIfAbsent(name, k -> new ArrayList<>());
      values.add(codeBlock);
      return this;
    }

    /**
     * adds a member to a builder instance with a specific name and value. It checks for
     * null values, valid types, and enforces naming conventions before adding the member.
     * 
     * @param memberName name of the member being added to the builder.
     * 
     * @param value object to be added as a member of a class, and its type is checked
     * against expected types based on the member name.
     * 
     * 	- If `value` is an instance of `Class<?>`, it represents a class name and is
     * treated as such in the function.
     * 	- If `value` is an instance of `Enum`, it represents an enum value and its name
     * is obtained using `getClass()` and `name()`.
     * 	- If `value` is a string, it is treated as a literal string.
     * 	- If `value` is a floating-point number (i.e., `Float` or `Double`), it is treated
     * as such in the function.
     * 	- If `value` is an integer (i.e., `Integer`, `Long`, or `Short`), it is treated
     * as such in the function.
     * 	- If `value` is a character (i.e., `Character`), it is represented by a single
     * quote followed by the character value, without any surrounding single quotes.
     * 	- Otherwise, `value` is treated as an arbitrary object and is passed through
     * unchanged to the function's `addMember()` method call.
     * 
     * @returns a member with the specified name and value, depending on the type of the
     * value.
     * 
     * 	- If `value` is an instance of `Class<?>`, the function returns an instance of `Class<?>`.
     * 	- If `value` is an instance of `Enum`, the function returns an instance of `String`
     * representing the name of the enum.
     * 	- If `value` is an instance of `String`, the function returns an instance of `String`.
     * 	- If `value` is an instance of `Float`, the function returns an instance of `Long`.
     * 	- If `value` is an instance of `Long`, the function returns an instance of `Long`.
     * 	- If `value` is a character, the function returns an instance of `Character`.
     * 	- Otherwise, the function returns an instance of `Object`.
     */
    Builder addMemberForValue(String memberName, Object value) {
      checkNotNull(memberName, "memberName == null");
      checkNotNull(value, "value == null, constant non-null value expected for %s", memberName);
      checkArgument(SourceVersion.isName(memberName), "not a valid name: %s", memberName);
      if (value instanceof Class<?>) {
        return addMember(memberName, "$T.class", value);
      }
      if (value instanceof Enum) {
        return addMember(memberName, "$T.$L", value.getClass(), ((Enum<?>) value).name());
      }
      if (value instanceof String) {
        return addMember(memberName, "$S", value);
      }
      if (value instanceof Float) {
        return addMember(memberName, "$Lf", value);
      }
      if (value instanceof Long) {
        return addMember(memberName, "$LL", value);
      }
      if (value instanceof Character) {
        return addMember(memberName, "'$L'", characterLiteralWithoutSingleQuotes((char) value));
      }
      return addMember(memberName, "$L", value);
    }

    /**
     * generates an `AnnotationSpec` object representing the current instance of the
     * `AnnotationSpec` class, based on the members of the `members` map.
     * 
     * @returns an instance of the `AnnotationSpec` class.
     * 
     * 	- The output is an instance of the `AnnotationSpec` class.
     * 	- The output has a reference to the enclosing `AnnotationSpec` object, which is
     * the container for the annotation specifications.
     * 	- The output has a set of methods that validate the input parameters, including
     * `checkNotNull`, `checkArgument`, and `checkState`. These methods are used to ensure
     * that the input parameters meet certain conditions before the annotation specification
     * is created.
     */
    public AnnotationSpec build() {
      for (String name : members.keySet()) {
        checkNotNull(name, "name == null");
        checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
      }
      return new AnnotationSpec(this);
    }
  }

  /**
   * is an implementation of `SimpleAnnotationValueVisitor8` that adds members to a
   * given builder instance for each visited annotation value. It has a constructor
   * that takes a `Builder` instance and performs additional actions based on the visited
   * annotation values, such as adding members for enum constants, types, and array
   * elements. The visitor class also provides default actions for handling non-enum
   * constant, non-type, and non-array elements.
   */
  private static class Visitor extends SimpleAnnotationValueVisitor8<Builder, String> {
    final Builder builder;

    Visitor(Builder builder) {
      super(builder);
      this.builder = builder;
    }

    /**
     * takes an object and a string as input and returns a `Builder` instance with a
     * `memberForValue` added to it with the given name and value.
     * 
     * @param o value to be added as a member to the builder object in the `defaultAction()`
     * function.
     * 
     * 	- `Object o`: This is the object that is being serialized and deserialized. Its
     * properties are not explicitly stated.
     * 	- `String name`: The name of the member being added to the builder.
     * 
     * @param name member value that will be added to the `Builder` instance by the function.
     * 
     * @returns a `Builder` instance with an added `memberForValue`.
     * 
     * 	- `Object o`: The parameter passed to the function, which is an instance of some
     * class.
     * 	- `String name`: The parameter passed to the function as well, representing the
     * name of the member being added to the builder.
     * 	- `Builder builder`: The object that the `defaultAction` function modifies by
     * adding a new member to it with the specified name and parameter `o`.
     */
    @Override protected Builder defaultAction(Object o, String name) {
      return builder.addMemberForValue(name, o);
    }

    /**
     * transforms an annotation mirror object into a member of a builder object, using
     * the `addMember` method. The annotation mirror object is passed as an argument,
     * along with its name and a reference to the builder object.
     * 
     * @param a AnnotationMirror object being processed and provides it to the `get()`
     * method to retrieve its value.
     * 
     * 	- `a` represents an annotation mirror object.
     * 	- `name` refers to the name of the annotation.
     * 	- `$L` is a reference to the fully qualified name of the annotated element type.
     * 	- `get(a)` returns the value of the annotation, which can be a member of the
     * annotation or a nested annotation.
     * 
     * @param name name of the annotation being visited by the `visitAnnotation` method.
     * 
     * @returns a Builder instance containing the specified annotation and its member value.
     */
    @Override public Builder visitAnnotation(AnnotationMirror a, String name) {
      return builder.addMember(name, "$L", get(a));
    }

    /**
     * processes an enum constant and adds it to a `Builder` object, using the constant's
     * name and type.
     * 
     * @param c enum constant that is being visited and is used to generate the corresponding
     * member of the builder object.
     * 
     * 	- `c`: Represents an `VariableElement` object, which contains information about
     * an element in the Java code being analyzed.
     * 	- `name`: Refers to the simple name of the enum constant being analyzed.
     * 	- `asType()`: Returns the type of the enum constant, which is a `$T` type.
     * 	- `getSimpleName()`: Gets the simple name of the enum constant.
     * 
     * @param name name of the enum constant being processed, which is used to generate
     * the appropriate code for adding the constant to the builder.
     * 
     * @returns a `Builder` object with a single member element representing an enum
     * constant, where the element's type and name are specified.
     * 
     * 	- `builder`: This is an instance of the `Builder` class, which represents the
     * current state of the Java compiler's build process.
     * 	- `name`: This is the name of the enum constant being visited.
     * 	- `$T.$L`: This is a string representation of the type of the enum constant,
     * followed by the name of the enum constant.
     * 	- `c.asType()`: This returns the resolved type of the enum constant, which is
     * used in the string representation.
     * 	- `c.getSimpleName()`: This returns the simple name of the enum constant, which
     * is also used in the string representation.
     */
    @Override public Builder visitEnumConstant(VariableElement c, String name) {
      return builder.addMember(name, "$T.$L", c.asType(), c.getSimpleName());
    }

    /**
     * adds a member to a builder with the given type mirror and name.
     * 
     * @param t TypeMirror object that is being visited by the `visitType()` method, which
     * adds it as a member of the builder's type hierarchy with the specified name.
     * 
     * 	- `t`: The TypeMirror object passed as an argument to the `visitType` method. It
     * represents the type of the serialized data.
     * 	- `name`: A string variable representing the name of the visited element, which
     * is typically the name of a class or interface.
     * 
     * By using the `addMember` method, the builder creates a new member with the specified
     * name and type `$T.class`, which represents the type of the deserialized input.
     * 
     * @param name name of the type mirror being visited and is used to determine the
     * appropriate qualifier for the returned `Builder`.
     * 
     * @returns a `Builder` object containing a member of type `String` with the name
     * `name` and type reference `$T.class`.
     * 
     * 	- `builder`: This is a reference to an instance of the `Builder` class, which is
     * used to build the Java object.
     * 	- `t`: This is a reference to the `TypeMirror` object representing the type being
     * visited.
     * 	- `name`: This is the name of the type as it appears in the source code.
     * 	- `$T.class`: This is the fully qualified name of the class representing the type,
     * including the package name.
     */
    @Override public Builder visitType(TypeMirror t, String name) {
      return builder.addMember(name, "$T.class", t);
    }

    /**
     * visits each element in a list of AnnotationValue objects and invokes the accept
     * method on each element with the provided name. It then returns the original builder
     * object unchanged.
     * 
     * @param values list of AnnotationValue objects that will be processed by the visitor
     * pattern.
     * 
     * 	- The `List<? extends AnnotationValue>` parameter represents an array of annotation
     * values that are being traversed.
     * 	- Each element in the list is an instance of `AnnotationValue`, which has its own
     * set of properties and attributes.
     * 	- The `accept()` method is called on each element with the given name, indicating
     * the name of the annotation value being traversed.
     * 
     * @param name name of the annotation value being visited.
     * 
     * @returns a modified builder instance with the visited annotations applied.
     * 
     * The function takes two parameters: `values`, which is a list of `AnnotationValue`
     * objects, and `name`, which is the name of the array being visited. The function
     * iterates over the elements in the `values` list using a recursive visit method,
     * applying the visit method to each element and passing it the current `name`.
     * Finally, the function returns the original `Builder` object.
     * 
     * The output of the function is a `Builder` object that has been updated by applying
     * the visit methods to all the elements in the `values` list. The `Builder` object
     * can be further modified by calling other visit methods on it.
     */
    @Override public Builder visitArray(List<? extends AnnotationValue> values, String name) {
      for (AnnotationValue value : values) {
        value.accept(this, name);
      }
      return builder;
    }
  }
}
