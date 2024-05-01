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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * in Java is a builder class for creating instances of `Parameter` elements in the
 * Java compiler's AST. It provides methods for setting the name, type, annotations,
 * and modifiers of a parameter, as well as emitting the parameter in code format.
 * The class also provides utility methods for working with parameters in various
 * ways, such as getting or building a parameter instance.
 */
public final class ParameterSpec {
  public final String name;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final TypeName type;
  public final CodeBlock javadoc;

  private ParameterSpec(Builder builder) {
    this.name = checkNotNull(builder.name, "name == null");
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.type = checkNotNull(builder.type, "type == null");
    this.javadoc = builder.javadoc.build();
  }

  /**
   * checks if a given `Modifier` is present in an array of modifiers.
   * 
   * @param modifier Modifier object to be checked if it is present in the set of
   * Modifiers stored in the `modifiers` field, and the function returns a boolean value
   * indicating whether the Modifier is present in the set or not.
   * 
   * Returns boolean: Whether the input `modifier` is present in the `modifiers` collection.
   * 
   * `modifier`: A deserialized object of type `Modifier`. The properties of this object
   * may include attributes such as name, description, and any other relevant details
   * related to the modifier.
   * 
   * @returns a boolean value indicating whether the specified modifier is present in
   * the collection of modifiers.
   */
  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  /**
   * writes Java code to a `CodeWriter`. It first emits annotations, modifiers, and
   * type information, followed by the emission of a method name.
   * 
   * @param codeWriter output stream where the bytecode will be written.
   * 
   * 	- `codeWriter`: This is an instance of `CodeWriter`, which is the output destination
   * for the generated Java code. It allows for the emission of code in a human-readable
   * format.
   * 	- `annotations`: This is a list of `Annotation` objects, representing the annotations
   * to be emitted with the generated code.
   * 	- `modifiers`: This is a list of `Modifier` objects, representing the modifiers
   * (such as `public`, `private`, etc.) to be emitted with the generated code.
   * 	- `type`: This is an instance of `TypeName`, representing the type of the emitted
   * code. It can be either a primitive type or a reference type.
   * 	- `name`: This is a `String` object, representing the name of the emitted code.
   * 	- `varargs`: This is a `boolean` value, indicating whether the emitted code should
   * include a varargs parameter list. If `true`, a `TypeName` instance representing
   * the array type will be emitted as well.
   * 
   * @param varargs type of an array and its value is emitted with emit(codeWriter, true).
   */
  void emit(CodeWriter codeWriter, boolean varargs) throws IOException {
    codeWriter.emitAnnotations(annotations, true);
    codeWriter.emitModifiers(modifiers);
    if (varargs) {
      TypeName.asArray(type).emit(codeWriter, true);
    } else {
      type.emit(codeWriter);
    }
    codeWriter.emit(" $L", name);
  }

  /**
   * compares an object with another object or null, determining equality based on class
   * and string representation.
   * 
   * @param o object being compared to the current object, and is used in the comparison
   * with `equals()` method.
   * 
   * 	- If `this` and `o` are different objects, return `false`.
   * 	- If `o` is null, return `false`.
   * 	- If the classes of the two objects are not the same, return `false`.
   * 	- Return `true` if the strings representing the objects are equal.
   * 
   * @returns a boolean value indicating whether the object is equal to another object.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its input, which is a string representation of the object
   * itself.
   * 
   * @returns the result of calling `toString.hashCode()` on the object.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of the current object by invoking its `emit`
   * method and returning the resulting string.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, false);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * generates a `ParameterSpec` instance for a given variable element. It checks that
   * the element is a parameter, retrieves the type and name of the parameter, and adds
   * any necessary modifiers to create the final `ParameterSpec`.
   * 
   * @param element ElementKind.PARAMETER, which is passed to the function to construct
   * a new ParameterSpec object.
   * 
   * 	- `checkArgument`: This is an instance method that takes a single argument `String
   * message`, which is not included in this response due to the limitation of 100
   * words. The purpose of this method is to check if the `element.getKind()` returns
   * a value equal to `ElementKind.PARAMETER`. If it does not, then an error message
   * will be thrown.
   * 	- `element.getKind()`: This property returns the kind of element represented by
   * `element`, which can only be `ElementKind.PARAMETER`.
   * 	- `element.asType()`: This property returns the type of the element represented
   * by `element`, using a TypeName object to represent the type.
   * 	- `element.getModifiers()`: This property returns an integer value representing
   * the modifiers (access level) of the element represented by `element`.
   * 	- `name`: This property represents the simple name of the element represented by
   * `element`.
   * 
   * @returns a `ParameterSpec` object representing the parameter with the specified
   * type and name.
   * 
   * 	- The output is of type `ParameterSpec`, which represents a parameter of a method
   * or constructor.
   * 	- The type of the parameter is specified by the `TypeName` field, which contains
   * the fully qualified name of the parameter's type.
   * 	- The name of the parameter is specified by the `name` field, which is a string
   * that corresponds to the simple name of the parameter.
   * 	- The modifiers of the parameter are specified by the `addModifiers` method, which
   * adds the modifiers (such as `public`, `private`, etc.) to the parameter.
   * 
   * The function does not include any information about the parameter's annotations,
   * as copying them can be incorrect and is deliberately avoided.
   */
  public static ParameterSpec get(VariableElement element) {
    checkArgument(element.getKind().equals(ElementKind.PARAMETER), "element is not a parameter");

    TypeName type = TypeName.get(element.asType());
    String name = element.getSimpleName().toString();
    // Copying parameter annotations can be incorrect so we're deliberately not including them.
    // See https://github.com/square/javapoet/issues/482.
    return ParameterSpec.builder(type, name)
        .addModifiers(element.getModifiers())
        .build();
  }

  /**
   * returns a list of `ParameterSpec` objects representing the parameters of an
   * executable element, such as a method, based on the variables declared in its parameters.
   * 
   * @param method ExecutableElement for which the method parameters are being retrieved.
   * 
   * 	- Method `getParameters()` returns a collection of `VariableElement` objects
   * representing the method's parameters.
   * 	- Each `VariableElement` object has attributes such as `name`, `type`, and `modifiers`.
   * 	- The `parametersOf` function iterates over these `VariableElement` objects and
   * adds each one to a list, which is returned as the final result.
   * 
   * @returns a list of `ParameterSpec` objects representing the parameters of the given
   * executable element.
   * 
   * 	- The output is of type `List<ParameterSpec>`.
   * 	- The list contains references to each parameter of the given executable element,
   * as obtained from its parameters.
   * 	- Each reference in the list is an instance of `ParameterSpec`, which provides
   * information about a method parameter.
   */
  static List<ParameterSpec> parametersOf(ExecutableElement method) {
    List<ParameterSpec> result = new ArrayList<>();
    for (VariableElement parameter : method.getParameters()) {
      result.add(ParameterSpec.get(parameter));
    }
    return result;
  }

  /**
   * checks if a given parameter name is valid in Java by verifying if it ends with
   * ".this" and is an identifier or equal to "this".
   * 
   * @param name name of a method or field parameter, and the function determines if
   * it is a valid identifier.
   * 
   * @returns a boolean value indicating whether the given parameter name is valid or
   * not.
   */
  private static boolean isValidParameterName(String name) {
    // Allow "this" for explicit receiver parameters
    // See https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.1.
    if (name.endsWith(".this")) {
      return SourceVersion.isIdentifier(name.substring(0, name.length() - ".this".length()));
    }
    return name.equals("this") || SourceVersion.isName(name);
  }

  /**
   * generates a new `Builder` instance for a given type and name, adding any specified
   * modifiers to the builder's state.
   * 
   * @param type Java class or interface that the `Builder` object will be built for.
   * 
   * 	- The first parameter `type` is not null and is of type `TypeName`.
   * 	- The second parameter `name` is a string that represents a valid name for the builder.
   * 	- The third parameter `modifiers` is an array of modifier types that can be added
   * to the builder.
   * 
   * @param name name of the builder to be created and is used to initialize the `Builder`
   * object.
   * 
   * @returns a new `Builder` instance with the specified type, name, and modifiers.
   * 
   * 	- The `Builder` object returned by the function is responsible for creating a new
   * instance of the specified `TypeName`.
   * 	- The `type` parameter is not null, and its validity is verified through a check.
   * 	- The `name` parameter is a string that represents the name of the type being
   * created, and it must be a valid name according to a provided validation function.
   * If the name is invalid, an error message is generated along with the input value.
   * 	- The `modifiers` parameter is an array of `Modifier` objects representing the
   * access modifiers for the new instance.
   * 
   * Overall, the `builder` function appears to be a factory method that creates a new
   * instance of a specific type and allows for customization of its access modifiers
   * through the `modifiers` parameter.
   */
  public static Builder builder(TypeName type, String name, Modifier... modifiers) {
    checkNotNull(type, "type == null");
    checkArgument(isValidParameterName(name), "not a valid name: %s", name);
    return new Builder(type, name)
        .addModifiers(modifiers);
  }

  /**
   * creates a new instance of the `Builder` class, initialized with the specified type,
   * name, and modifiers.
   * 
   * @param type type of object that will be built by the `Builder` instance returned
   * by the function.
   * 
   * 	- The `Type` parameter is an instance of the `Type` class, which represents a
   * type in the program.
   * 	- The `name` parameter is a string that specifies the name of the type.
   * 	- The `modifiers` parameter is an array of modifiers that can be used to customize
   * the creation of the builder object.
   * 
   * @param name name of the class that the `Builder` instance will build.
   * 
   * @returns a `Builder` instance of the specified type and name, along with any
   * additional modifiers.
   * 
   * 	- `Type`: This is the type of builder being returned, which can be either `Builder`
   * or `ModuleBuilder`.
   * 	- `Name`: This is the name of the builder, which is a string.
   * 	- `Modifiers`: These are the modifiers associated with the builder, which can
   * include any combination of the following: `PUBLIC`, `PROTECTED`, `PRIVATE`, `FINAL`,
   * and `ABSTRACT`.
   */
  public static Builder builder(Type type, String name, Modifier... modifiers) {
    return builder(TypeName.get(type), name, modifiers);
  }

  /**
   * allows for building a new instance of the `Builder` class with the same type and
   * name as the current instance, providing a convenient way to create a copy of the
   * current builder for further modification or customization.
   * 
   * @returns a new instance of the `Builder` class with the same type and name as the
   * original builder.
   * 
   * 	- `type`: The type of the builder object that is being returned.
   * 	- `name`: The name of the builder object that is being returned.
   */
  public Builder toBuilder() {
    return toBuilder(type, name);
  }

  /**
   * generates a new instance of the `Builder` class with customized type, name and
   * annotations/modifiers from the original builder instance.
   * 
   * @param type Java type of the class being built, which is used to determine the
   * parameters and fields of the resulting Builder instance.
   * 
   * 1/ `TypeName`: This represents the type of the Java class that is being constructed
   * using the builder.
   * 2/ `String name`: This represents the name of the class, which is used to set the
   * name of the generated class.
   * 
   * @param name name of the class that the `Builder` instance is building.
   * 
   * @returns a new `Builder` instance with updated annotations and modifiers.
   * 
   * 	- The `Builder` object returned is a new instance of the `Builder` class, created
   * using the `new Builder(TypeName type, String name)` constructor.
   * 	- The `annotations` field of the returned builder contains a list of all the
   * annotations applied to the original builder.
   * 	- The `modifiers` field of the returned builder contains a list of all the modifiers
   * applied to the original builder.
   * 
   * Overall, the `toBuilder` function returns a new `Builder` instance that contains
   * a copy of the annotations and modifiers from the original builder, allowing the
   * builder to be reused or modified as needed.
   */
  Builder toBuilder(TypeName type, String name) {
    Builder builder = new Builder(type, name);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    return builder;
  }

  /**
   * is a utility class that provides a simple and flexible way to build a `ParameterSpec`
   * instance. It allows for adding annotations, modifiers, and Javadoc to the parameter,
   * as well as modifying the type and name of the parameter. The built `ParameterSpec`
   * can then be used to generate code for a method or field declaration.
   */
  public static final class Builder {
    private final TypeName type;
    private final String name;
    private final CodeBlock.Builder javadoc = CodeBlock.builder();

    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();

    private Builder(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    /**
     * adds documentation to the builder's Javadoc.
     * 
     * @param format format of the Javadoc documentation that is being added to the builder
     * object.
     * 
     * @returns a new instance of the `Builder` class with the added Javadoc comments.
     * 
     * The `javadoc` field is used to store the added Javadoc information.
     * The `format` argument represents the Javadoc format string used to generate the documentation.
     * The `args` argument is an array of objects that contain the additional parameters
     * for the Javadoc format string.
     */
    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
      return this;
    }

    /**
     * adds a code block to thejavadoc list of a builder object, returning the modified
     * builder.
     * 
     * @param block Java code to be added to the existing Javadoc documentation of the
     * `Builder` class.
     * 
     * 	- `block`: The CodeBlock object that represents the Javadoc content to be added
     * to the builder.
     * 	- `javadoc`: A reference to a mutable list where the Javadoc content is accumulated.
     * 
     * @returns a modified instance of the `Builder` class with the added Javadoc code.
     * 
     * 	- The `javadoc` field is an instance of `CodeBlock`, which represents a block of
     * Java code that has been documented using Javadoc comments.
     * 	- The `add` method adds the provided `block` to the `javadoc` field, allowing the
     * user to accumulate multiple blocks of code for documentation purposes.
     * 	- The `return` statement returns the modified `Builder` instance, which can be
     * used to continue building the Java code.
     */
    public Builder addJavadoc(CodeBlock block) {
      javadoc.add(block);
      return this;
    }

    /**
     * modifies the builder by adding the specified iterable of annotation specs to its
     * list of annotations.
     * 
     * @param annotationSpecs Iterable of AnnotationSpecs that will be added to the
     * object's annotations.
     * 
     * 	- The input is an iterable collection of `AnnotationSpec` objects.
     * 	- Each `AnnotationSpec` has a non-null reference as its property.
     * 	- The function iterates over each `AnnotationSpec` and adds it to the internal
     * annotations list of the `Builder` instance.
     * 
     * @returns a builder instance with added annotations.
     * 
     * The method returns a `Builder` instance with the added annotations.
     * The `checkArgument` method is called to validate that the `annotationSpecs` parameter
     * is not null before adding the annotations.
     * The `for` loop iterates over the `annotationSpecs` list and adds each `AnnotationSpec`
     * element to the `annotations` list of the current `Builder` instance.
     */
    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    /**
     * adds an annotation to the builder's annotations list, allowing for customization
     * of the generated code.
     * 
     * @param annotationSpec AnnotationSpec object that adds to the builder's annotations.
     * 
     * 	- `this.annotations.add(annotationSpec)` adds the `annotationSpec` to the existing
     * list of annotations associated with the current builder instance.
     * 
     * @returns a reference to the updated `Builder` instance with the added annotation.
     * 
     * This function returns a reference to itself, allowing the caller to continue
     * building the object.
     * The `annotations` field is added to the current state of the builder, which can
     * be used to add additional annotations later.
     * The `this` keyword indicates that the method returns an instance of the class being
     * built, rather than a new object.
     */
    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    /**
     * allows for the addition of an annotation to a `Builder` instance, by taking a
     * `ClassName` parameter and appending an `AnnotationSpec` object to the `annotations`
     * list.
     * 
     * @param annotation AnnotationSpec to be added to the builder, allowing the caller
     * to customize the annotations associated with the object being built.
     * 
     * The `ClassName` annotation is an object that represents a class name. It has no
     * attributes or properties.
     * 
     * @returns a modified builder instance with an additional annotation added to its
     * list of annotations.
     * 
     * 	- `this`: This refers to the current instance of the `Builder` class being used
     * to build the object.
     * 	- `annotations`: A list of `AnnotationSpec` objects that represent the annotations
     * added to the object. Each `AnnotationSpec` object is built using the `build()`
     * method and contains information about a single annotation, such as its type, value,
     * and any additional metadata.
     */
    public Builder addAnnotation(ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    /**
     * adds an annotation to a `Builder` instance. The annotation can be any subclass of
     * `Class<?>` and is passed as a parameter to the function. The function returns the
     * `Builder` instance with the added annotation.
     * 
     * @param annotation Class object of the annotation to be added to the builder.
     * 
     * Class<?> annotation: The type of annotation that is being added to the builder.
     * 
     * ClassName.get(annotation): A method that returns a string representing the fully
     * qualified name of the specified class.
     * 
     * The explanation provided above refers to the properties and attributes of the input
     * `annotation` without providing a summary or mentioning any additional information.
     * 
     * @returns a new instance of the `Builder` class with the specified annotation added.
     * 
     * The method returns a `Builder` object after adding an annotation to the builder's
     * model.
     * 
     * The annotation added is specified by the `Class` parameter passed in the constructor
     * call. This means that the annotation's class must be provided as a string argument
     * when calling the method.
     * 
     * The `ClassName.get()` method is used to obtain the fully qualified name of the
     * annotation class, which is then passed as the return value to the builder.
     */
    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    /**
     * adds a list of modifiers to an instance of the `Builder` class, increasing its modularity.
     * 
     * @returns a modified builder instance with the added modifiers.
     * 
     * The `addModifiers` function takes an array of `Modifier` objects as input and adds
     * them to the existing modifiers collection of the `Builder` object.
     * 
     * The `modifiers` collection is a set-like container that stores the modifiers added
     * to the class, and it is not modified during the execution of the function.
     * 
     * The `addAll` method is used to add the provided `Modifier` objects to the existing
     * modifiers collection.
     * 
     * The `return this;` statement at the end of the function indicates that the original
     * `Builder` object is being returned as the output, which can be further modified
     * and used for other purposes.
     */
    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    /**
     * adds a collection of modifiers to a builder object, checks for unexpected modifiers,
     * and returns the updated builder.
     * 
     * @param modifiers iterable of Modifier objects that will be added to the builder's
     * modifiers list.
     * 
     * 	- The method checks that `modifiers` is not null before iterating over it.
     * 	- For each `Modifier` object in `modifiers`, the method checks if it is not equal
     * to `Modifier.FINAL`. If so, an `IllegalStateException` is thrown.
     * 	- Otherwise, the method adds the `Modifier` object to the `modifiers` field of
     * the current builder instance.
     * 
     * @returns a reference to the original `Builder` instance, after applying the provided
     * modifiers.
     * 
     * The function takes an iterable of Modifier objects as input, which are added to
     * the modifiers collection of the Builder object.
     * 
     * The modifiers collection is a set of Modifier objects that represent the access
     * modifiers for the class being built.
     * 
     * The function checks that the input modifiers are not null before adding them to
     * the collection.
     * 
     * If any of the input modifiers do not equal Modifier.FINAL, a IllegalStateException
     * is thrown. This ensures that only valid modifiers are added to the collection.
     */
    public Builder addModifiers(Iterable<Modifier> modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      for (Modifier modifier : modifiers) {
        if (!modifier.equals(Modifier.FINAL)) {
          throw new IllegalStateException("unexpected parameter modifier: " + modifier);
        }
        this.modifiers.add(modifier);
      }
      return this;
    }

    /**
     * creates a new instance of `ParameterSpec`, returning it.
     * 
     * @returns a new `ParameterSpec` instance created from the current object.
     * 
     * The `ParameterSpec` object constructed is an instance of `ParameterSpec`, which
     * represents a parameter in a method signature or a constructor argument. It contains
     * information about the parameter's name, type, and modifiers. The `ParameterSpec`
     * object is immutable.
     */
    public ParameterSpec build() {
      return new ParameterSpec(this);
    }
  }
}
