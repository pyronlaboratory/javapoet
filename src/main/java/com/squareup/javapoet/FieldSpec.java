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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/**
 * in Java is a class that represents a field in a class. It has several methods and
 * fields to define and manipulate fields. The Builder class is used to create a
 * FieldSpec, which can be modified using various methods such as addJavadoc,
 * addAnnotations, addModifiers, and initializer. The FieldSpec class also provides
 * static methods for building FieldSpecs.
 */
public final class FieldSpec {
  public final TypeName type;
  public final String name;
  public final CodeBlock javadoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final CodeBlock initializer;

  private FieldSpec(Builder builder) {
    this.type = checkNotNull(builder.type, "type == null");
    this.name = checkNotNull(builder.name, "name == null");
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.initializer = (builder.initializer == null)
        ? CodeBlock.builder().build()
        : builder.initializer;
  }

  /**
   * checks if a given `Modifier` is present in an array of modifiers.
   * 
   * @param modifier modifier to be checked for presence in the `modifiers` set, and
   * the function returns `true` if the modifier is present in the set and `false` otherwise.
   * 
   * The parameter `modifier` is an object of the class `Modifier`. This class has
   * several attributes, such as `name`, `value`, and `type`, which can be used to
   * describe various characteristics of a modifier. The `contains` method is used to
   * check if the `modifier` object is present in a collection of modifiers called
   * `modifiers`. Therefore, the function returns a boolean value indicating whether
   * the specified modifier is present in the collection or not.
   * 
   * @returns a boolean value indicating whether the provided modifier is present in
   * the `modifiers` set.
   */
  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  /**
   * writes Java source code to a CodeWriter object, including Javadoc comments,
   * annotations, modifiers, and the code for a field declaration.
   * 
   * @param codeWriter Java compiler's output stream, which is used to write the compiled
   * code to.
   * 
   * 	- `codeWriter`: A `CodeWriter` object that is used to generate the Java code.
   * 	- `javadoc`: A `String` containing the Javadoc documentation for the method.
   * 	- `annotations`: A `Set` of `Modifier` objects representing the annotations for
   * the method.
   * 	- `modifiers`: A `Set` of `Modifier` objects representing the modifiers for the
   * method, including both explicit and implicit ones.
   * 	- `type`: A `String` containing the type of the method's return value.
   * 	- `name`: A `String` containing the name of the method.
   * 	- `initializer`: A `String` containing the initializer block for the method, if
   * it exists.
   * 
   * @param implicitModifiers set of modifiers that are automatically applied to the
   * member without needing to be explicitly specified.
   * 
   * 	- The `Set<Modifier>` type indicates that it is a set of modifiers, which are
   * used to annotate classes, methods, or fields in Java.
   * 	- The `implicitModifiers` variable refers to a set of modifiers that are not
   * explicitly provided by the caller but are automatically generated based on the
   * input data.
   * 	- The `javadoc`, `annotations`, and `modifiers` variables represent the different
   * types of information that can be emitted (generated) by the `codeWriter`.
   * 	- The `$T $L` syntax is used to generate code for type-safe method calls, which
   * ensures that the correct type of object is passed as a parameter.
   * 	- The `initializer` variable represents the initialization code that is generated
   * for a class or field. If it is not empty, it means that there is some initializing
   * code that needs to be executed when the class or field is created.
   */
  void emit(CodeWriter codeWriter, Set<Modifier> implicitModifiers) throws IOException {
    codeWriter.emitJavadoc(javadoc);
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);
    codeWriter.emit("$T $L", type, name);
    if (!initializer.isEmpty()) {
      codeWriter.emit(" = ");
      codeWriter.emit(initializer);
    }
    codeWriter.emit(";\n");
  }

  /**
   * compares an object to the current object, checks if they are the same, and if not,
   * checks if the other object is null, and then checks if their classes are the same
   * before checking for a string match.
   * 
   * @param o object being compared to the current instance, which is being checked for
   * equality.
   * 
   * The function initializes the variable 'o' to an object other than itself. Then it
   * returns true if this object is equal to 'o', false otherwise. If 'o' is null, then
   * the function returns false directly without any further processing. The class of
   * 'o' and the deserialized input 'this' are different, so the third condition is not
   * satisfied. Therefore, the fourth condition is evaluated next.
   * 
   * The variable 'o' represents an object of an unknown class, whereas 'this' represents
   * an object of the same class as the calling function. The method compares two objects
   * using the `toString()` method to generate a string representation of each object
   * and compare them directly.
   * 
   * @returns a boolean value indicating whether the object being compared is the same
   * as the current object.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its argument, which is the result of calling `toString()`
   * on the argument and then hash-coding it.
   * 
   * @returns an integer value that represents the hash code of the object being represented.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of its input using a `CodeWriter`. The resulting
   * string represents the source code of the class that contains the `toString` method.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, Collections.emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * creates a new instance of a `Builder` class with predefined parameters, such as
   * type and name, and allows for additional modifiers to be added.
   * 
   * @param type type of object that the `Builder` instance will create.
   * 
   * The input `type` is not null and has been verified through a check.
   * The name of the type is provided as an argument, and it must be a valid name
   * according to the `SourceVersion` class. If the input name is invalid, an exception
   * will be thrown with a custom message containing the invalid name.
   * The function returns a new `Builder` object instance that can be used to construct
   * a new `Type` object with the specified `type`, `name`, and optional `modifiers`.
   * 
   * @param name name of the builder being created and is validated to ensure it is a
   * valid name.
   * 
   * @returns a new `Builder` instance for creating a class with the specified type and
   * name, along with any additional modifiers.
   * 
   * 	- `TypeName type`: The type of the builder object created.
   * 	- `String name`: The name of the builder object created.
   * 	- `Modifier... modifiers`: The modifiers associated with the builder object.
   */
  public static Builder builder(TypeName type, String name, Modifier... modifiers) {
    checkNotNull(type, "type == null");
    checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
    return new Builder(type, name)
        .addModifiers(modifiers);
  }

  /**
   * creates a new instance of a `Builder` class based on a given type, name, and modifiers.
   * 
   * @param type type of the object being created as a Builder, and it is used to
   * determine the appropriate constructor to call when creating the object.
   * 
   * 	- The type variable `type` is an instance of the `Type` class, which represents
   * a fundamental data type in Java, such as `int`, `String`, or `Object`.
   * 	- The `name` parameter is a string that provides a human-readable name for the type.
   * 	- The `modifiers` array contains the access modifiers for the type, such as
   * `public`, `protected`, or `private`.
   * 
   * @param name name of the builder to be created.
   * 
   * @returns a `Builder` instance representing a new instance of the specified type
   * with the provided name and modifiers.
   * 
   * 	- `Type`: The type of the builder being created, which can be any valid Java type.
   * 	- `Name`: A string representing the name of the builder class.
   * 	- `Modifiers`: An array of modifier constants representing the access modifiers
   * for the builder class.
   */
  public static Builder builder(Type type, String name, Modifier... modifiers) {
    return builder(TypeName.get(type), name, modifiers);
  }

  /**
   * generates a new `Builder` instance with modified values for type, name, Javadoc,
   * annotations, modifiers, and initializer, allowing for incremental changes to an
   * object's configuration.
   * 
   * @returns a new `Builder` object containing the same properties as the original `Builder`.
   * 
   * 	- The Builder object returned is a new instance of `Builder`, initialized with
   * the type, name, and other attributes of the original `Builder` object.
   * 	- The `javadoc` attribute of the returned `Builder` object contains the same list
   * of Javadoc comments as the original `Builder`.
   * 	- The `annotations` attribute of the returned `Builder` object contains the same
   * list of annotations as the original `Builder`.
   * 	- The `modifiers` attribute of the returned `Builder` object contains the same
   * list of modifiers (public, protected, private) as the original `Builder`.
   * 	- If the `initializer` attribute of the original `Builder` is not empty, it is
   * included in the returned `Builder` object. Otherwise, it is set to `null`.
   */
  public Builder toBuilder() {
    Builder builder = new Builder(type, name);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.initializer = initializer.isEmpty() ? null : initializer;
    return builder;
  }

  /**
   * is a utility class that provides a way to build a `FieldSpec` object step by step,
   * allowing for customization of various attributes such as annotations, modifiers,
   * and initializer code. The class provides a fluent interface for building the
   * `FieldSpec`, allowing users to add or modify different aspects of the field
   * definition in a flexible manner.
   */
  public static final class Builder {
    private final TypeName type;
    private final String name;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private CodeBlock initializer = null;

    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();

    private Builder(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    /**
     * adds Javadoc comments to the current builder object using a specified format and
     * arguments.
     * 
     * @param format format of the Javadoc documentation to be added to the current builder
     * object.
     * 
     * @returns a documentation comment added to the class or method being built.
     * 
     * 	- The `javadoc` field is a list that contains the added Javadoc comments.
     * 	- The method returns a `Builder` object, which allows chaining of additional
     * methods to modify the Java class.
     */
    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
      return this;
    }

    /**
     * adds a code block to the javadoc of a builder, allowing for the documentation to
     * be built and modified easily.
     * 
     * @param block Java code that is to be added to the existing Javadoc of the current
     * builder instance, as part of the `addJavadoc` method call.
     * 
     * 	- `block`: The input parameter is a `CodeBlock` object that contains the Java
     * code to be documented using Javadoc.
     * 
     * @returns a reference to the `javadoc` collection, which has been updated to include
     * the provided `CodeBlock`.
     * 
     * 	- `javadoc`: This is a `List<CodeBlock>` object that contains the Javadoc blocks
     * added to the builder.
     * 	- `this`: The current builder instance, which can be used to chain multiple method
     * calls together or return a new instance with modified settings.
     */
    public Builder addJavadoc(CodeBlock block) {
      javadoc.add(block);
      return this;
    }

    /**
     * modifies a builder instance by adding an iterable collection of annotation
     * specifications to its list of annotations.
     * 
     * @param annotationSpecs Iterable of AnnotationSpec objects that will be added to
     * the `annotations` list of the Builder object.
     * 
     * 	- `checkArgument`: A method that verifies if the input `annotationSpecs` is not
     * null.
     * 	- `for`: A loop construct used to iterate over the elements of `annotationSpecs`.
     * 	- `this.annotations`: The annotations collection of the current builder object,
     * which is updated with each iteration of the loop.
     * 
     * @returns a reference to the modified `Builder` object, allowing the caller to
     * continue modifying it.
     * 
     * 	- `this`: The current instance of the `Builder` class is returned as the output.
     * 	- `annotations`: A list of `AnnotationSpec` objects is added to the instance's
     * internal `annotations` field.
     * 	- `null`: The input `Iterable` of `AnnotationSpec` objects is checked for nullity
     * before adding any elements to the internal list.
     */
    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    /**
     * adds an AnnotationSpec to the builder's annotations list, returning the updated
     * builder instance.
     * 
     * @param annotationSpec AnnotationSpec object that adds to the builder's list of annotations.
     * 
     * 	- `this.annotations.add(annotationSpec)` - Adds the `annotationSpec` to the
     * existing list of annotations associated with this builder instance.
     * 
     * The `annotationSpec` object contains various attributes or properties that define
     * its behavior and purpose, such as:
     * 
     * 	- `type`: The type of annotation being added, which determines how it will be
     * displayed in the code.
     * 	- `value`: The value of the annotation, which provides additional information
     * about the annotated element.
     * 	- `description`: A brief description of the annotation and its purpose.
     * 	- `modifiers`: The modifiers of the annotation, such as `public`, `private`, or
     * `protected`.
     * 
     * @returns a reference to the newly added annotation.
     * 
     * 	- `this`: The builder object that is being updated with the new annotation.
     * 	- `annotationSpec`: The annotation to be added to the builder's list of annotations.
     */
    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    /**
     * adds an annotation to the builder instance, by adding it to the `annotations` list
     * and returning the modified builder instance.
     * 
     * @param annotation ClassName of an annotation to be added to the builder's annotations
     * list.
     * 
     * The `ClassName` annotation is deserialized into the `AnnotationSpec.builder()`
     * method, which builds an instance of the `AnnotationSpec` class. This instance
     * represents a single annotation to be added to the object being built. The `build()`
     * method returns an instance of `AnnotationSpec`.
     * 
     * @returns a modified builder instance with an added annotation.
     * 
     * 	- `this`: This refers to the current instance of the `Builder` class being used.
     * 	- `annotations`: This is a list of `AnnotationSpec` objects, added using the `add`
     * method. Each element in the list represents an annotation added to the underlying
     * class.
     */
    public Builder addAnnotation(ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    /**
     * adds an annotation to a class, using the provided Class object as a reference.
     * 
     * @param annotation Class object of an annotation that the `addAnnotation()` method
     * adds to the Java object being built.
     * 
     * Class<?> annotation: The input is a class object representing an annotation.
     * ClassName getter method: This method returns the name of the annotation class.
     * 
     * @returns a new `Builder` instance with the specified annotation added to its list
     * of annotations.
     * 
     * The returned output is a `Builder` instance, which is an immutable object that
     * allows for further method calls to be chained together.
     * The `Class<?>` parameter passed to the function is used to create a new annotation
     * instance, which is then returned as the output.
     * The `ClassName.get()` method is used to obtain the fully qualified name of the
     * specified annotation class, which is then used to create the new annotation instance.
     */
    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    /**
     * allows for the addition of multiple modifiers to a builder object through the use
     * of an array of Modifier objects, effectively modifying the class that the builder
     * is representing.
     * 
     * @returns a modified builder instance with the provided modifiers added to its
     * `modifiers` collection.
     * 
     * The `addModifiers` function modifies the existing modifiers of a `Builder`.
     * It takes an array of `Modifier` objects as input and adds them to the existing
     * modifiers of the builder.
     * The added modifiers are stored in the `modifiers` list, which is a field of the
     * `Builder` class.
     * Therefore, the output of the function is a reference to the modified `Builder`.
     */
    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    /**
     * creates a new instance of the `Builder` class using a given string format and arguments.
     * 
     * @param format formatting string for the code block to be generated by the `initializer`
     * function.
     * 
     * @returns a `Builder` instance.
     * 
     * 	- The returned object is of type `Builder`, which is an immutable class in Java
     * for creating objects.
     * 	- The `format` parameter is passed as an argument to the `CodeBlock.of()` method
     * to create a new code block with the specified format and arguments.
     * 	- The `Object... args` parameter is also passed as arguments to the `CodeBlock.of()`
     * method, indicating the list of arguments to be used in the code block.
     */
    public Builder initializer(String format, Object... args) {
      return initializer(CodeBlock.of(format, args));
    }

    /**
     * sets a new initializer for an instance of a `Builder` class. It checks if the
     * initializer has already been set and throws an exception if it has, then sets the
     * new code block as the initializer using `checkNotNull`.
     * 
     * @param codeBlock code to be initialized with the builder object's initializer method.
     * 
     * 	- The `checkState()` method is used to check whether the `initializer` field has
     * already been set. If it has, an exception is thrown.
     * 	- The `checkNotNull()` method is used to verify that the `codeBlock` parameter
     * is not null. If it is null, a runtime error is thrown.
     * 	- The `this.initializer` field is assigned the deserialized input `codeBlock`.
     * 
     * @returns a reference to the `CodeBlock` object that was provided as an argument.
     * 
     * 	- `this.initializer`: The initialized `Builder` instance.
     * 	- `codeBlock`: The code block that was passed to the initializer function for initialization.
     * 	- `checkState()` and `checkNotNull()`: Utility methods used for checking the state
     * of the object and nullness of objects, respectively.
     */
    public Builder initializer(CodeBlock codeBlock) {
      checkState(this.initializer == null, "initializer was already set");
      this.initializer = checkNotNull(codeBlock, "codeBlock == null");
      return this;
    }

    /**
     * returns a new `FieldSpec` instance based on the current object, creating a
     * self-contained representation of the class's fields and methods for further use
     * or distribution.
     * 
     * @returns a new `FieldSpec` instance created from the current object.
     * 
     * 	- The output is a `FieldSpec` object representing the field specification for
     * this instance.
     * 	- The `FieldSpec` object has several attributes and methods that can be used to
     * manipulate and analyze the field specification.
     * 	- These attributes and methods include the field name, type, and position in the
     * message, among others.
     */
    public FieldSpec build() {
      return new FieldSpec(this);
    }
  }
}
