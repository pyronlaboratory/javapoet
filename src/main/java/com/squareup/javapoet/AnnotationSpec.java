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
import java.io.StringWriter;
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

import static com.squareup.javapoet.Util.checkNotNull;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;

/** A generated annotation on a declaration. */
public final class AnnotationSpec {
  public final TypeName type;
  public final Map<String, List<CodeBlock>> members;

  private AnnotationSpec(Builder builder) {
    this.type = builder.type;
    this.members = Util.immutableMultimap(builder.members);
  }

  /**
   * Generates high-quality documentation for given code, using a template and passing
   * it to a `CodeWriter`. It handles different types of annotations (singleton, named,
   * and inline) and formats them according to the template.
   * 
   * @param codeWriter 3rd party library that generates the documentation for the given
   * code.
   * 
   * @param inline boolean value indicating whether the annotation values should be
   * written directly inside the method signature or indented below it, respectively.
   */
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
   * Emits annotation values in a code writer, indented according to the size of the
   * list and separated by a member separator if there are multiple values.
   * 
   * @param codeWriter output stream where the annotated code should be written.
   * 
   * @param whitespace indentation level for the emission of code blocks, which is used
   * to group related code blocks together and improve readability.
   * 
   * @param memberSeparator string that separates each member in the list of values
   * when they are emitted to the code writer.
   * 
   * @param values List of CodeBlock objects that emitting annotation values for
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
   * Returns an `AnnotationSpec` object representing the given `Annotation`.
   * 
   * @param annotation Annotation object to be retrieved as an AnnotationSpec object
   * by the `get()` method.
   * 
   * @returns an `AnnotationSpec` object.
   */
  public static AnnotationSpec get(Annotation annotation) {
    return get(annotation, false);
  }

  /**
   * Retrieves the values of an Annotation using reflection and builds a `AnnotationSpec`
   * object based on the retrieved values.
   * 
   * @param annotation annotation object to be reflected upon, providing the method
   * names and values to include or exclude when building the `AnnotationSpec`.
   * 
   * @param includeDefaultValues default values for each method and if they match the
   * value returned from the `Method.invoke()` method, then skip adding them to the builder.
   * 
   * @returns an `AnnotationSpec` object representing the annotations of the given
   * annotation instance.
   */
  public static AnnotationSpec get(Annotation annotation, boolean includeDefaultValues) {
    Builder builder = builder(annotation.annotationType());
    try {
      Method[] methods = annotation.annotationType().getDeclaredMethods();
      Arrays.sort(methods, new Comparator<Method>() {
        /**
         * Compares two `Method` objects based on their names, returning a negative integer
         * value if the first method name is shorter than the second, a positive integer value
         * if the first method name is longer than the second, and zero if the method names
         * are equal.
         * 
         * @param m1 1st method being compared to the 2nd method in the compare() function.
         * 
         * @param m2 2nd method being compared to the 1st method `m1`.
         * 
         * @returns a numerical value indicating the comparison result between two method names.
         */
        @Override
        public int compare(Method m1, Method m2) {
          return m1.getName().compareTo(m2.getName());
        }
      });
      for (Method method : methods) {
        Object value = method.invoke(annotation);
        if (!includeDefaultValues) {
          if (Objects.deepEquals(value, method.getDefaultValue())) {
            continue;
          }
        }
        if (value.getClass().isArray()) {
          for (int i = 0; i < Array.getLength(value); i++) {
            builder.addValue(method.getName(), Array.get(value, i));
          }
          continue;
        }
        if (value instanceof Annotation) {
          builder.addMember(method.getName(), "$L", get((Annotation) value));
          continue;
        }
        builder.addValue(method.getName(), value);
      }
    } catch (Exception e) {
      throw new RuntimeException("Reflecting " + annotation + " failed!", e);
    }
    return builder.build();
  }

  /**
   * Generates a documentation-quality annotation object for the given annotation
   * instance, using the element values of the annotation to construct the object's
   * fields and methods.
   * 
   * @param annotation annotation for which the method generates documentation.
   * 
   * @returns an `AnnotationSpec` object representing the specified annotation.
   */
  public static AnnotationSpec get(AnnotationMirror annotation) {
    TypeElement element = (TypeElement) annotation.getAnnotationType().asElement();
    AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get(element));
    Visitor visitor = new Visitor(builder);
    for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
      String name = executableElement.getSimpleName().toString();
      AnnotationValue value = annotation.getElementValues().get(executableElement);
      value.accept(visitor, new Entry(name, value));
    }
    return builder.build();
  }

  /**
   * Creates a new `Builder` instance for a given type.
   * 
   * @param type ClassName to be built using the `Builder` class.
   * 
   * @returns a `Builder` object of the specified type.
   */
  public static Builder builder(ClassName type) {
    checkNotNull(type, "type == null");
    return new Builder(type);
  }

  /**
   * Creates a new instance of a custom class, specified by the `type` parameter, using
   * the `Builder` interface.
   * 
   * @param type class to which the builder will be created, and it is used to generate
   * a builder object of that class.
   * 
   * @returns a `Builder` instance of the specified class type.
   */
  public static Builder builder(Class<?> type) {
    return builder(ClassName.get(type));
  }

  public Builder toBuilder() {
    Builder builder = new Builder(type);
    for (Map.Entry<String, List<CodeBlock>> entry : members.entrySet()) {
      builder.members.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return builder;
  }

  /**
   * Compares the object being checked to the current object, checks if they are the
   * same object, and then checks if their classes are the same. If those conditions
   * are met, it returns true. Otherwise, it returns false.
   * 
   * @param o object being checked for equality with the current object.
   * 
   * @returns a boolean value indicating whether the object is equal to the current instance.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      codeWriter.emit("$L", this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static final class Builder {
    private final TypeName type;
    private final Map<String, List<CodeBlock>> members = new LinkedHashMap<>();

    private Builder(TypeName type) {
      this.type = type;
    }

    /**
     * Adds a member to a Builder object with a specified name and format for the member
     * value, using a CodeBlock object to hold the format and arguments.
     * 
     * @param name name of the member to be added.
     * 
     * @param format format string to be used for adding the member to the builder's
     * internal state.
     * 
     * @param args 0 or more arguments to be passed to the format string specified by the
     * `name` parameter when calling the `CodeBlock.builder().add(format, args).build()`
     * method.
     * 
     * @returns a `CodeBlock` object containing the specified code and arguments.
     */
    public Builder addMember(String name, String format, Object... args) {
      return addMember(name, CodeBlock.builder().add(format, args).build());
    }

    /**
     * Allows for the addition of a new member to an existing list of CodeBlocks, storing
     * the updated list under the specified name in the member map.
     * 
     * @param name name of the member to be added to the builder's list of members.
     * 
     * @param codeBlock code block that is to be added to the list of members associated
     * with the specified `name`.
     * 
     * @returns a reference to the modified `Builder` object.
     */
    public Builder addMember(String name, CodeBlock codeBlock) {
      List<CodeBlock> values = members.get(name);
      if (values == null) {
        values = new ArrayList<>();
        members.put(name, values);
      }
      values.add(codeBlock);
      return this;
    }

    /**
     * Adds a member to a class, taking into account the type of the provided `value`.
     * It returns the updated class with the added member.
     * 
     * @param memberName name of the member being added to the builder.
     * 
     * @param value object being added to the class as a member field and determines the
     * type of the member field based on its data type.
     * 
     * @returns a string representation of the given object.
     */
    private Builder addValue(String memberName, Object value) {
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
      if (value instanceof Character) {
        String literal = CodeWriter.stringLiteral(value.toString(), "");
        literal = literal.substring(1, literal.length() - 1);
        if (literal.equals("\\\"")) literal = "\"";
        if (literal.equals("'")) literal = "\\'";
        return addMember(memberName, "'$L'", literal);
      }
      return addMember(memberName, "$L", value);
    }

    public AnnotationSpec build() {
      return new AnnotationSpec(this);
    }
  }

  private static class Entry {
    final String name;
    final AnnotationValue value;

    Entry(String name, AnnotationValue value) {
      this.name = name;
      this.value = value;
    }
  }

  /**
   * Annotation value visitor adding members to the given builder instance.
   */
  private static class Visitor extends SimpleAnnotationValueVisitor7<Builder, Entry> {
    final Builder builder;

    Visitor(Builder builder) {
      super(builder);
      this.builder = builder;
    }

    /**
     * Adds a member to a builder object with the name provided in the `Entry` object and
     * the value provided in the `entry.value`.
     * 
     * @param o object being built with the addition of the member specified by the `Entry`
     * object passed as an argument to the function.
     * 
     * @param entry entry object that contains the name and value of a member to be added
     * to the builder.
     * 
     * @returns a `Builder` instance with an added `member` field containing the specified
     * name and value.
     */
    @Override protected Builder defaultAction(Object o, Entry entry) {
      return builder.addMember(entry.name, "$L", entry.value);
    }

    /**
     * Adds a member to a builder object with the name provided in the `Entry` entry and
     * the value obtained by calling the `get` method on an `AnnotationMirror` parameter.
     * 
     * @param a AnnotationMirror object being visited by the `visitAnnotation()` method.
     * 
     * @param entry name of an annotation that is being visited by the `visitAnnotation()`
     * method.
     * 
     * @returns a `Builder` instance with an added member of the specified name and type.
     */
    @Override public Builder visitAnnotation(AnnotationMirror a, Entry entry) {
      return builder.addMember(entry.name, "$L", get(a));
    }

    /**
     * Adds an enum constant to a generated builder object. It takes the enum constant's
     * name, type, and simple name as input, and returns a builder with the added constant.
     * 
     * @param c `VariableElement` object that is being visited by the `visitEnumConstant`
     * method.
     * 
     * @param entry entry object passed to the `visitEnumConstant` method, providing the
     * name of the enum constant being processed.
     * 
     * @returns a code snippet that adds an enum constant to a Java class with the given
     * name and type.
     */
    @Override public Builder visitEnumConstant(VariableElement c, Entry entry) {
      return builder.addMember(entry.name, "$T.$L", c.asType(), c.getSimpleName());
    }

    /**
     * Adds a member to a builders with name entry and type mirror as argument
     * 
     * @param t TypeMirror to be processed and is used to add the corresponding type
     * information to the builder object.
     * 
     * @param entry entry object containing the name and type of a class member to be
     * added to the builder.
     * 
     * @returns a `Builder` object containing a member reference to the specified type.
     */
    @Override public Builder visitType(TypeMirror t, Entry entry) {
      return builder.addMember(entry.name, "$T.class", t);
    }

    /**
     * Processes a list of annotation values and recursively visits each value, passing
     * it to the `accept` method along with a new entry object representing the current
     * element name.
     * 
     * @param values list of AnnotationValue objects that are to be visited and transformed
     * by the visitor method.
     * 
     * @param entry entry being visited and provides the name of the entry to be used as
     * the key for the new annotation value.
     * 
     * @returns a new `Builder` instance with updated annotation values.
     */
    @Override public Builder visitArray(List<? extends AnnotationValue> values, Entry entry) {
      for (AnnotationValue value : values) {
        value.accept(this, new Entry(entry.name, value));
      }
      return builder;
    }
  }
}
