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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/**
 * Builder is a tool for creating and customizing method declarations in Kotlin
 * programming language. It allows developers to add various elements such as parameters,
 * return type, throws clause, body code, and comments to their methods. The builder
 * provides a fluent interface for configuring these elements and builds the resulting
 * MethodSpec instance at the end of the build process.
 */
public final class MethodSpec {
  static final String CONSTRUCTOR = "<init>";

  public final String name;
  public final CodeBlock javadoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final List<TypeVariableName> typeVariables;
  public final TypeName returnType;
  public final List<ParameterSpec> parameters;
  public final boolean varargs;
  public final List<TypeName> exceptions;
  public final CodeBlock code;
  public final CodeBlock defaultValue;

  private MethodSpec(Builder builder) {
    CodeBlock code = builder.code.build();
    checkArgument(code.isEmpty() || !builder.modifiers.contains(Modifier.ABSTRACT),
        "abstract method %s cannot have code", builder.name);
    checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
        "last parameter of varargs method %s must be an array", builder.name);

    this.name = checkNotNull(builder.name, "name == null");
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.typeVariables = Util.immutableList(builder.typeVariables);
    this.returnType = builder.returnType;
    this.parameters = Util.immutableList(builder.parameters);
    this.varargs = builder.varargs;
    this.exceptions = Util.immutableList(builder.exceptions);
    this.defaultValue = builder.defaultValue;
    this.code = code;
  }

  /**
   * determines if the last parameter of a list is an array type by checking if the
   * last parameter's type is an array and not null.
   * 
   * @param parameters list of parameter specifications for a method, and is used to
   * determine if the last parameter is an array type.
   * 
   * 	- `!parameters.isEmpty()` - indicates that there is at least one parameter in the
   * list.
   * 	- `(parameters.get(parameters.size() - 1).type)` - represents the type of the
   * last element in the `parameters` list.
   * 	- `TypeName.asArray((parameters.get(parameters.size() - 1).type))` - returns the
   * array type of the last element's type, or null if it is not an array.
   * 
   * @returns a boolean value indicating whether the last parameter is an array.
   */
  private boolean lastParameterIsArray(List<ParameterSpec> parameters) {
    return !parameters.isEmpty()
        && TypeName.asArray((parameters.get(parameters.size() - 1).type)) != null;
  }

  /**
   * generates Java code for a method based on its parameters, return type, and modifiers.
   * It calls `codeWriter.emitJavadoc()` to generate Javadoc comments,
   * `codeWriter.emitAnnotations()` to generate annotations, and `codeWriter.emitModifiers()`
   * to generate modifier information. It also emits the method's name, parameters,
   * default value, and throws clauses.
   * 
   * @param codeWriter Java compiler API that is being used to generate the code, and
   * it is used to emit the code for the function.
   * 
   * 	- `codeWriter`: This is an instance of the `CodeWriter` class, which is responsible
   * for writing Java code to a output stream. It takes various parameters such as
   * `enclosingName`, `implicitModifiers`, `typeVariables`, `parameters`, `defaultValue`,
   * and `exceptions` to generate high-quality summaries of Java code.
   * 	- `enclosingName`: This is the name of the enclosing type, which is used to
   * identify the class or method being documented.
   * 	- `implicitModifiers`: This is a set of modifiers that are automatically added
   * to the method signature when generating Javadoc. These modifiers are usually not
   * present in the original source code.
   * 	- `typeVariables`: This is a set of type variables that are used to represent the
   * types of the parameters in the method signature.
   * 	- `parameters`: This is an iterator over the parameters of the method, which
   * includes their names, types, and default values.
   * 	- `defaultValue`: This is the default value of the method, which is emitted as
   * part of the Javadoc documentation.
   * 	- `exceptions`: This is a set of exception types that are thrown by the method,
   * which are emitted as part of the Javadoc documentation.
   * 
   * The `emit` function performs various operations on these inputs to generate
   * high-quality summaries of Java code. These operations include:
   * 
   * 1/ Emitting the Javadoc documentation for the method signature, including the name,
   * return type, and parameter types.
   * 2/ Emitting any annotations or modifiers that are associated with the method.
   * 3/ Emitting any type variables that are used in the method signature.
   * 4/ Emitting any parameters of the method, including their names, types, and default
   * values.
   * 5/ Emitting any exceptions that are thrown by the method.
   * 6/ Indenting or un indenting the code within the method body, depending on whether
   * the method is declared as `native` or not.
   * 7/ Writing the method body in a separate line, followed by an empty line.
   * 
   * Overall, the `emit` function takes care of all the necessary operations to generate
   * high-quality summaries of Java code, while also ensuring that the output is
   * well-formatted and easy to read.
   * 
   * @param enclosingName name of the outermost class or interface that contains the
   * method, and is used to emit the correct enclosing type in the Javadoc comment.
   * 
   * @param implicitModifiers set of modifiers that are automatically added to the
   * method declaration without being explicitly specified by the user, which can help
   * simplify the method definition and reduce potential errors.
   * 
   * 	- `implicitModifiers`: A `Set` containing modifiers that are applied to the method
   * without an explicit declaration. These modifiers include `public`, `protected`,
   * and `private`. (In 3 sentences)
   * 	- `typeVariables`: A collection of type variables that are used in the method
   * signature. These type variables are emitted by the code generator as part of the
   * method implementation. (In 3 sentences)
   * 	- `parameters`: An `ArrayList` containing information about the method's parameters,
   * including their names, types, and whether they are varargs. (In 4 sentences)
   * 	- `defaultValue`: The default value assigned to the method, if any. This can be
   * a constant or an expression that is evaluated at compile-time. (In 3 sentences)
   * 	- `exceptions`: An array of `TypeName` objects representing the exceptions that
   * the method can throw. These exceptions are emitted by the code generator as part
   * of the method implementation. (In 4 sentences)
   */
  void emit(CodeWriter codeWriter, String enclosingName, Set<Modifier> implicitModifiers)
      throws IOException {
    codeWriter.emitJavadoc(javadocWithParameters());
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);

    if (!typeVariables.isEmpty()) {
      codeWriter.emitTypeVariables(typeVariables);
      codeWriter.emit(" ");
    }

    if (isConstructor()) {
      codeWriter.emit("$L($Z", enclosingName);
    } else {
      codeWriter.emit("$T $L($Z", returnType, name);
    }

    boolean firstParameter = true;
    for (Iterator<ParameterSpec> i = parameters.iterator(); i.hasNext(); ) {
      ParameterSpec parameter = i.next();
      if (!firstParameter) codeWriter.emit(",").emitWrappingSpace();
      parameter.emit(codeWriter, !i.hasNext() && varargs);
      firstParameter = false;
    }

    codeWriter.emit(")");

    if (defaultValue != null && !defaultValue.isEmpty()) {
      codeWriter.emit(" default ");
      codeWriter.emit(defaultValue);
    }

    if (!exceptions.isEmpty()) {
      codeWriter.emitWrappingSpace().emit("throws");
      boolean firstException = true;
      for (TypeName exception : exceptions) {
        if (!firstException) codeWriter.emit(",");
        codeWriter.emitWrappingSpace().emit("$T", exception);
        firstException = false;
      }
    }

    if (hasModifier(Modifier.ABSTRACT)) {
      codeWriter.emit(";\n");
    } else if (hasModifier(Modifier.NATIVE)) {
      // Code is allowed to support stuff like GWT JSNI.
      codeWriter.emit(code);
      codeWriter.emit(";\n");
    } else {
      codeWriter.emit(" {\n");

      codeWriter.indent();
      codeWriter.emit(code, true);
      codeWriter.unindent();

      codeWriter.emit("}\n");
    }
    codeWriter.popTypeVariables(typeVariables);
  }

  /**
   * generates a Javadoc code block for a method, adding a new line before the `@param`
   * section for each parameter with non-empty `javadoc` field.
   * 
   * @returns a CodeBlock representation of the method's documentation with parameter
   * information included.
   * 
   * 	- The output is a `CodeBlock.Builder` instance, which represents a block of Java
   * code that can be used in Javadoc documentation.
   * 	- The `builder` parameter is used to construct the `CodeBlock.Builder`, and it
   * is initialized with the `javadoc` parameter value.
   * 	- The `emitTagNewline` variable is set to `true` by default, indicating that a
   * new line should be emitted before the `@param` section. However, this behavior can
   * be modified by setting the `emitTagNewline` parameter to `false`.
   * 	- The `parameters` array contains each method parameter's `ParameterSpec` object,
   * which includes information such as the parameter name and Javadoc documentation.
   * Each `ParameterSpec` object is passed to the `add()` method of the `CodeBlock.Builder`
   * instance, where it is added to the output code block.
   * 	- The `@param` tag is used to indicate that a parameter is being documented. The
   * `$L` placeholder is replaced with the actual parameter name and Javadoc documentation
   * during document generation.
   */
  private CodeBlock javadocWithParameters() {
    CodeBlock.Builder builder = javadoc.toBuilder();
    boolean emitTagNewline = true;
    for (ParameterSpec parameterSpec : parameters) {
      if (!parameterSpec.javadoc.isEmpty()) {
        // Emit a new line before @param section only if the method javadoc is present.
        if (emitTagNewline && !javadoc.isEmpty()) builder.add("\n");
        emitTagNewline = false;
        builder.add("@param $L $L", parameterSpec.name, parameterSpec.javadoc);
      }
    }
    return builder.build();
  }

  /**
   * checks if a given `Modifier` is present in an array of modifiers.
   * 
   * @param modifier Modifier object to be checked for presence in the `modifiers`
   * collection, and the function returns a boolean value indicating whether the modifier
   * is present in the collection.
   * 
   * The `Modifier` object passed to the function is checked if it contains any values
   * inside the `contains` method.
   * 
   * @returns a boolean value indicating whether the specified modifier is present in
   * the `modifiers` set.
   */
  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  /**
   * determines if a given variable `name` represents the constructor keyword.
   * 
   * @returns a boolean value indicating whether the given name is equal to `CONSTRUCTOR`.
   */
  public boolean isConstructor() {
    return name.equals(CONSTRUCTOR);
  }

  /**
   * compares an object to the current object, checking for equivalence based on class
   * and string representation.
   * 
   * @param o object being compared to the current object, and is used in the comparison
   * to determine if the two objects are equal.
   * 
   * 	- If this equals o, return true indicating that the two objects are equal.
   * 	- If o is null, return false to indicate that the object is null and cannot be compared.
   * 	- If the class types do not match, return false to indicate that the objects are
   * not of the same type.
   * 	- Compare the strings of both objects using the `equals` method.
   * 
   * @returns a boolean value indicating whether the object being compared is equal to
   * the current object.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its input, which is a string representation of the object
   * itself, calculated using the `toString()` method.
   * 
   * @returns an integer value that represents the unique identity of the object.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of the class, which includes the constructor and
   * any other information that is relevant to the class's identity.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, "Constructor", Collections.emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * creates a new `Builder` instance with the given `name`.
   * 
   * @param name name of the builder to be created.
   * 
   * @returns a new instance of the `Builder` class with the given `name`.
   * 
   * The `Builder` object returned is an instance of the `Builder` class, which represents
   * a builder pattern in Java. It provides a way to construct complex objects step-by-step
   * without having to create multiple intermediate objects.
   * 
   * The `name` parameter passed to the function is used as the name of the Builder
   * instance. This name can be used to identify the specific instance of the Builder
   * class being constructed.
   * 
   * The `new Builder(name)` syntax creates a new instance of the Builder class and
   * returns it, making it available for further construction or use.
   */
  public static Builder methodBuilder(String name) {
    return new Builder(name);
  }

  /**
   * creates a new instance of the `Builder` class, using the specified constructor.
   * 
   * @returns a new `Builder` instance with a reference to the `CONSTRUCTOR` constructor.
   * 
   * The returned object is a `Builder`, which is an immutable class used to construct
   * other objects.
   * The `CONSTRUCTOR` field is a static final variable that contains the name of the
   * constructor to use when building the object.
   * Therefore, the output provided by the function is a Builder object with a predetermined
   * constructor.
   */
  public static Builder constructorBuilder() {
    return new Builder(CONSTRUCTOR);
  }

  /**
   * takes an `ExecutableElement` as input, checks its validity, and builds a `MethodSpec`
   * representing the override with the same name, return type, and parameters as the
   * original method.
   * 
   * @param method execution element to be overridden, and it is checked for null before
   * proceeding with the override generation process.
   * 
   * 1/ `checkNotNull`: This method checks that the input `method` is not null before
   * proceeding with its analysis.
   * 2/ `getEnclosingElement`: This method returns the enclosing element of the input
   * `method`, which can provide additional context about the method's location within
   * a class hierarchy.
   * 3/ `getModifiers`: This method returns a set of modifiers that are associated with
   * the input `method`. These modifiers can be used to determine the access level of
   * the method.
   * 4/ `getSimpleName`: This method returns the simple name of the input `method`,
   * which can be used to identify the method without its qualifying class name.
   * 5/ `addAnnotation`: This method adds an annotation to the builder, in this case,
   * `Override.class`.
   * 6/ `addModifiers`: This method adds a set of modifiers to the builder, which are
   * derived from the input `method`s modifiers.
   * 7/ `addTypeVariable`: This method adds a type variable to the builder, which is
   * associated with each of the input `typeParameterElement`s.
   * 8/ `addParameters`: This method adds the parameters of the input `method` to the
   * builder, along with any varargs information.
   * 9/ `addException`: This method adds an exception type to the builder, which is
   * associated with each of the input `thrownType`s.
   * 
   * Overall, this function provides a way to generate a `MethodSpec` instance that can
   * be used to create a new method with the same signature as the input `method`, while
   * also providing additional context about the input `method`s properties and attributes.
   * 
   * @returns a `MethodSpec.Builder` instance that represents an overridden method with
   * specified modifiers, return type, and parameters.
   * 
   * 	- The `MethodSpec.Builder` object is used to create a new method descriptor that
   * overrides an existing method.
   * 	- The `addAnnotation` method adds an `@Override` annotation to the new method,
   * indicating that it overrides an existing method.
   * 	- The `addModifiers` method sets the modifiers of the new method to the same value
   * as the original method.
   * 	- The `addTypeVariable` method adds type variables from the original method's
   * type parameters to the new method's type parameters.
   * 	- The `addParameters` method adds the parameters of the original method to the
   * new method. If the original method is varargs, the new method will also be varargs.
   * 	- The `addException` method adds any thrown types from the original method to the
   * new method.
   * 	- The `returns` method sets the return type of the new method to the same type
   * as the original method's return type.
   * 
   * In summary, the `overriding` function takes an ExecutableElement and creates a new
   * MethodSpec.Builder that overrides an existing method with the same name, modifiers,
   * and type parameters, but with different type variables or parameters, or both.
   */
  public static Builder overriding(ExecutableElement method) {
    checkNotNull(method, "method == null");

    Element enclosingClass = method.getEnclosingElement();
    if (enclosingClass.getModifiers().contains(Modifier.FINAL)) {
      throw new IllegalArgumentException("Cannot override method on final class " + enclosingClass);
    }

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)
        || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    methodBuilder.addAnnotation(Override.class);

    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    modifiers.remove(Modifier.DEFAULT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    methodBuilder.returns(TypeName.get(method.getReturnType()));
    methodBuilder.addParameters(ParameterSpec.parametersOf(method));
    methodBuilder.varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    return methodBuilder;
  }

  /**
   * takes an `ExecutableElement`, `DeclaredType`, and `Types` as input, and returns a
   * `Builder` object that can be used to modify the method's parameters, return type,
   * and thrown exceptions.
   * 
   * @param method ExecutableElement that is being overridden.
   * 
   * 	- `ExecutableElement method`: This is an ExecutableElement that represents a
   * method in the Java code.
   * 	- `DeclaredType enclosing`: This is a DeclaredType that represents the enclosing
   * type of the method, such as a class or interface.
   * 	- `Types types`: This is a Types object that contains the method's parameter and
   * return types as TypeMirrors.
   * 
   * The method itself is not destructured, but its various properties are explained above.
   * 
   * @param enclosing declaring class of the `method`, which is used to determine the
   * type of the `types` argument and to identify the method's member-level access.
   * 
   * 	- `enclosing`: A declared type representing the enclosing element (i.e., the class
   * or interface) where the method is defined.
   * 	- `types`: A list of types representing the types of the method's parameters,
   * return type, and thrown exceptions.
   * 
   * The function then performs the following actions:
   * 
   * 1/ Deserializes the input `executableType` to an `ExecutableType` object.
   * 2/ Gets the parameter types of the executable type using its `getParameterTypes()`
   * method.
   * 3/ Gets the thrown exception types of the executable type using its `getThrownTypes()`
   * method.
   * 4/ Deserializes the input `resolvedReturnType` to a `TypeMirror` object representing
   * the return type of the method.
   * 5/ Creates a new builder instance for the method override.
   * 6/ Sets the return type of the builder to the deserialized `TypeMirror` object.
   * 7/ Iterates over the parameters of the executable type and creates a new parameter
   * spec using the deserialized parameter type. The parameter spec is then added to
   * the builder instance.
   * 8/ Clears any previously added exceptions to avoid conflicts with the new exceptions
   * added in the next step.
   * 9/ Iterates over the thrown exception types of the executable type and adds each
   * one as an exception to the builder instance.
   * 
   * Finally, the function returns the modified builder instance representing the method
   * override.
   * 
   * @param types type information of the method being overridden, which is used to
   * determine the types of the parameters and the return value of the builder.
   * 
   * 	- `asMemberOf(enclosing, method)`: Returns an ExecutableType object representing
   * the execution of the specified method in the enclosing DeclaredType.
   * 	- `getParameterTypes()`: Returns a list of TypeMirror objects representing the
   * parameter types of the specified method.
   * 	- `getThrownTypes()`: Returns a list of TypeMirror objects representing the thrown
   * types of the specified method.
   * 	- `getReturnType()`: Returns a TypeMirror object representing the return type of
   * the specified method.
   * 
   * The `overriding` function then uses these properties to create a new Builder
   * instance and configure its parameters, exceptions, and return type accordingly.
   * 
   * @returns a `Builder` object for creating a new method implementation with updated
   * type information.
   * 
   * 	- `builder`: This is an instance of `Builder`, which is used to create a new
   * subclass of the enclosing type.
   * 	- `resolvedReturnType`: This is the resolved return type of the `method`, which
   * is used to set the return type of the newly created subclass.
   * 	- `parameters`: This is a list of `ParameterSpec` instances, which represent the
   * parameters of the `method`. Each parameter has its own `type` attribute, which is
   * set to the resolved parameter type of the corresponding parameter in the `method`.
   * 	- `exceptions`: This is an empty list, indicating that no exceptions are thrown
   * by the `method`.
   * 
   * The `overriding` function takes three arguments: `method`, `enclosing`, and `types`.
   * The `method` argument is an `ExecutableElement` representing the method to be
   * overridden. The `enclosing` argument is a `DeclaredType` representing the enclosing
   * type of the method, which is used to determine the scope of the newly created
   * subclass. The `types` argument is a `Types` instance representing the types of the
   * `method`, which contains information about the method's return type and parameters.
   */
  public static Builder overriding(
      ExecutableElement method, DeclaredType enclosing, Types types) {
    ExecutableType executableType = (ExecutableType) types.asMemberOf(enclosing, method);
    List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
    List<? extends TypeMirror> resolvedThrownTypes = executableType.getThrownTypes();
    TypeMirror resolvedReturnType = executableType.getReturnType();

    Builder builder = overriding(method);
    builder.returns(TypeName.get(resolvedReturnType));
    for (int i = 0, size = builder.parameters.size(); i < size; i++) {
      ParameterSpec parameter = builder.parameters.get(i);
      TypeName type = TypeName.get(resolvedParameterTypes.get(i));
      builder.parameters.set(i, parameter.toBuilder(type, parameter.name).build());
    }
    builder.exceptions.clear();
    for (int i = 0, size = resolvedThrownTypes.size(); i < size; i++) {
      builder.addException(TypeName.get(resolvedThrownTypes.get(i)));
    }

    return builder;
  }

  /**
   * creates a new `Builder` instance with updated fields based on the current class's
   * attributes. It allows for modifying the class without creating a new instance.
   * 
   * @returns a new instance of the `Builder` class with the given fields initialized.
   * 
   * 1/ Builder instance: The function returns a new instance of `Builder`.
   * 2/ Name field: The `name` field is set to the current class name.
   * 3/ Javadoc field: The `javadoc` field is added with the current class's Javadoc comment.
   * 4/ Annotations field: The `annotations` field is added with the current class's annotations.
   * 5/ Modifiers field: The `modifiers` field is added with the current class's modifiers.
   * 6/ Type variables field: The `typeVariables` field is added with the current class's
   * type variables.
   * 7/ Return type field: The `returnType` field is set to the current class's return
   * type.
   * 8/ Parameters field: The `parameters` field is added with the current class's parameters.
   * 9/ Exceptions field: The `exceptions` field is added with the current class's exceptions.
   * 10/ Code field: The `code` field is added with the current class's code.
   * 11/ Varargs field: The `varargs` field is set to the current class's varargs flag.
   * 12/ Default value field: The `defaultValue` field is set to the current class's
   * default value.
   * 
   * Overall, the `toBuilder` function provides a way to create a new instance of the
   * same class with modified fields or attributes, which can be useful for various
   * purposes such as creating a copy of the class with specific modifications or
   * building a new class instance from scratch based on an existing one.
   */
  public Builder toBuilder() {
    Builder builder = new Builder(name);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.typeVariables.addAll(typeVariables);
    builder.returnType = returnType;
    builder.parameters.addAll(parameters);
    builder.exceptions.addAll(exceptions);
    builder.code.add(code);
    builder.varargs = varargs;
    builder.defaultValue = defaultValue;
    return builder;
  }

  /**
   * in Kotlin is a design pattern used for creating objects step-by-step, allowing for
   * more flexibility and customization during the creation process. It provides a set
   * of methods for adding different components to a method spec, such as name, return
   * type, parameters, and code blocks. The builder class allows for a high degree of
   * customization, making it easier to create complex classes with many options.
   */
  public static final class Builder {
    private String name;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private TypeName returnType;
    private final Set<TypeName> exceptions = new LinkedHashSet<>();
    private final CodeBlock.Builder code = CodeBlock.builder();
    private boolean varargs;
    private CodeBlock defaultValue;

    public final List<TypeVariableName> typeVariables = new ArrayList<>();
    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();
    public final List<ParameterSpec> parameters = new ArrayList<>();

    private Builder(String name) {
      setName(name);
    }

    /**
     * sets the `name` field of a `Builder` instance to a given string value, checking
     * for nullness and validity of the input name.
     * 
     * @param name name of the constructor and validates it to ensure it is either
     * "CONSTRUCTOR" or a valid type name.
     * 
     * @returns a `Builder` object with updated name and return type properties.
     * 
     * 	- `this`: refers to the current instance of the `Builder` class.
     * 	- `name`: the name parameter passed to the function, which is assigned to the
     * `name` field of the `Builder` instance.
     * 	- `returnType`: the type of the return value, which is set to `null` if the `name`
     * parameter is equal to `CONSTRUCTOR`, and otherwise set to `TypeName.VOID`.
     */
    public Builder setName(String name) {
      checkNotNull(name, "name == null");
      checkArgument(name.equals(CONSTRUCTOR) || SourceVersion.isName(name),
          "not a valid name: %s", name);
      this.name = name;
      this.returnType = name.equals(CONSTRUCTOR) ? null : TypeName.VOID;
      return this;
    }

    /**
     * allows for adding Javadoc comments to the current builder instance.
     * 
     * @param format formatting options for the Javadoc documentation that will be generated
     * by the `add()` method.
     * 
     * @returns a documentation comment added to the class or method being built.
     * 
     * 	- `format`: The format of the Javadoc documentation to be added.
     * 	- `args`: The arguments for the Javadoc documentation.
     * 
     * The `addJavadoc` function takes two parameters, `format` and `args`, which are
     * used to add Javadoc documentation to the builder. The `format` parameter is a
     * string representing the format of the Javadoc documentation, while the `args`
     * parameter is an array of objects containing the arguments for the Javadoc documentation.
     * 
     * The function returns the builder itself, indicating that the modification has been
     * applied to the current instance of the builder.
     */
    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
      return this;
    }

    /**
     * adds a `CodeBlock` to the `javadoc` collection, returning a modified `Builder` object.
     * 
     * @param block Java code to be added to the existing Javadoc documentation of the
     * current object instance, and is added to the `javadoc` list.
     * 
     * 	- `block`: A `CodeBlock` object representing the Javadoc code to be added to the
     * builder.
     * 	- `javadoc`: A list of `CodeBlock` objects that contain the Javadoc documentation
     * for the class being built.
     * 
     * @returns a reference to the `javadoc` object, which contains the added code block.
     * 
     * 	- `javadoc`: A reference to an `ArrayList` object that stores the added `CodeBlock`
     * objects.
     * 	- `this`: A reference to the current instance of the `Builder` class, which is
     * used to chain method calls together.
     */
    public Builder addJavadoc(CodeBlock block) {
      javadoc.add(block);
      return this;
    }

    /**
     * allows for the addition of multiple annotation specifications to a builder, which
     * can then be used to modify an object's behavior or add additional information to
     * its metadata.
     * 
     * @param annotationSpecs Iterable of AnnotationSpec objects that are added to the
     * annotations field of the builder instance.
     * 
     * 	- `annotationSpecs` is an iterable collection of `AnnotationSpec` objects.
     * 	- Each `AnnotationSpec` in the iteration represents a single annotation that can
     * be added to the builder's annotations list.
     * 	- The `checkArgument` method is used to ensure that the input `annotationSpecs`
     * is not null before proceeding with its iteration.
     * 
     * @returns a reference to the modified `Builder` object, allowing for further method
     * calls without creating a new instance.
     * 
     * 	- The `checkArgument` method is used to verify that the `annotationSpecs` parameter
     * is not null before proceeding with the addition of annotations.
     * 	- The `for` loop iterates over the `annotationSpecs` collection and adds each
     * `AnnotationSpec` element to the `annotations` list.
     * 	- The `return this;` statement returns the updated `Builder` object.
     */
    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    /**
     * adds an annotation to a `Builder` instance, allowing for the modification of its
     * annotations.
     * 
     * @param annotationSpec annotation to be added to the Java object being built by the
     * `Builder` class, and is used to modify the object's annotations.
     * 
     * 	- `this.annotations.add(annotationSpec)` adds the given annotation to the list
     * of annotations associated with the current builder instance.
     * 
     * @returns a reference to the newly added annotation.
     * 
     * 	- The `this` reference is used to modify the original builder instance.
     * 	- The `annotations` list is modified by adding the provided `AnnotationSpec` object.
     * 	- The `return this;` statement returns the modified builder instance for further
     * modifications.
     */
    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    /**
     * allows for the addition of an annotation to a Builder object, by taking a class
     * name as input and adding an AnnotationSpec instance built using that class name.
     * 
     * @param annotation AnnotationSpec to be added to the builder object, which is then
     * appended to the list of annotations associated with the current instance being built.
     * 
     * 	- `ClassName`: This represents the fully qualified name of an annotation class.
     * 	- `AnnotationSpec.builder()`: This is a method that builds an instance of
     * `AnnotationSpec`, which is used to represent an annotation in the Java programming
     * language.
     * 	- `build()`: This is a method that creates a new instance of `AnnotationSpec`
     * based on the input `annotation`.
     * 
     * @returns a modified instance of the `Builder` class with an additional `Annotation`
     * added to its list of annotations.
     * 
     * 	- `this`: Represents the current `Builder` instance being manipulated.
     * 	- `annotations`: A list of `AnnotationSpec` objects that have been added to the
     * current `Builder`.
     * 	- `build()`: The method that builds an `AnnotationSpec` object from the specified
     * annotation.
     */
    public Builder addAnnotation(ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    /**
     * adds an annotation to a `Builder` object, where the annotation is specified by its
     * class name.
     * 
     * @param annotation Class object of an annotation to be added to the builder's model.
     * 
     * Class<?> annotation: Represents the Java class that defines an annotation.
     * ClassName getter: A method that returns the fully qualified name of a class.
     * 
     * @returns a new `Builder` instance with an added annotation.
     * 
     * The `Class<?>` annotation passed to the function is used as the key for retrieving
     * the corresponding annotation class from the `ClassName.get()` method. The resulting
     * annotation class is then returned as the output of the function.
     */
    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    /**
     * allows for the addition of one or more `Modifier` objects to an existing `Builder`
     * instance, by checking that the input is not null and then appending it to the
     * current modifiers list.
     * 
     * @returns a modified builder instance with the added modifiers.
     * 
     * 	- The function takes a variable number of `Modifier` objects as input through the
     * `Modifier...` parameter.
     * 	- The function checks if the input `modifiers` parameter is null before adding
     * it to the `modifiers` field of the builder object.
     * 	- The `Collections.addAll()` method is used to add the input `modifiers` to the
     * `modifiers` field of the builder object.
     * 	- The function returns the builder object itself, indicating that the modification
     * has been applied successfully.
     */
    public Builder addModifiers(Modifier... modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    /**
     * adds an iterable of modifiers to a builder instance, validating the input and
     * adding each modifier to its internal list.
     * 
     * @param modifiers iterable of Modifier objects to be added to the current instance
     * of the Builder, which is used to modify the behavior of the class being built.
     * 
     * 	- `modifiers` is an iterable collection of `Modifier` objects that represent
     * custom modifiers for the builder.
     * 	- Each `Modifier` object has a unique name and an optional description.
     * 	- The `addModifiers` function adds each `Modifier` object to the `modifiers` list
     * of the current builder instance.
     * 	- The `modifiers` list is immutable, meaning once an element is added, it cannot
     * be removed or modified.
     * 
     * @returns a modified instance of the `Builder` class with additional modifiers added
     * to its `modifiers` list.
     * 
     * 	- The method adds an iterable collection of modifiers to the builder's list of modifiers.
     * 	- The modifiers are added in the form of `Modifier` objects.
     * 	- The `modifiers` list is not modified directly within the method, instead, a new
     * `Modifier` object is created and added to the list each time.
     * 	- The method returns the builder instance after adding the modifiers, allowing
     * for chaining of additional methods.
     */
    public Builder addModifiers(Iterable<Modifier> modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      for (Modifier modifier : modifiers) {
        this.modifiers.add(modifier);
      }
      return this;
    }

    /**
     * adds an iterable list of type variables to the builder's type variables list.
     * 
     * @param typeVariables iterable of type variables to be added to the builder's list
     * of type variables.
     * 
     * 	- `typeVariables` is an iterable collection of `TypeVariableName` objects, which
     * represents a set of type variables associated with the builder.
     * 	- The function checks if `typeVariables` is null before proceeding, ensuring that
     * the method is called with a valid input.
     * 	- The function iterates over each `TypeVariableName` object in `typeVariables`,
     * adding it to the `typeVariables` set of the current builder instance.
     * 
     * @returns a reference to the updated `Builder` object.
     * 
     * 	- The `checkArgument` method is used to validate that the input `typeVariables`
     * is not null before modifying its internal state.
     * 	- The `for` loop iterates over the elements of the `typeVariables` collection and
     * adds each element to the `typeVariables` field of the current builder instance.
     * 	- The `return this;` statement returns the updated builder instance, allowing the
     * method to be called chainingly.
     */
    public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
      checkArgument(typeVariables != null, "typeVariables == null");
      for (TypeVariableName typeVariable : typeVariables) {
        this.typeVariables.add(typeVariable);
      }
      return this;
    }

    /**
     * adds a type variable to the builder's list of type variables.
     * 
     * @param typeVariable name of a type variable to be added to the builder's list of
     * type variables.
     * 
     * 	- `TypeVariableName`: This represents the name of the type variable being added
     * to the builder.
     * 	- `typeVariables`: A list that contains the type variables associated with the builder.
     * 
     * In this function, the `typeVariables` list is updated by adding a new element
     * `typeVariable`. The function then returns the builder instance, indicating that
     * the method execution has completed successfully.
     * 
     * @returns a reference to the specified `TypeVariableName`.
     * 
     * 	- The `typeVariables` field is added to the builder's internal state, indicating
     * that the type variable has been successfully added.
     * 	- The method returns a reference to the same builder instance, allowing for
     * chaining of additional methods.
     * 	- No information about the code author or licensing is provided as it is not
     * relevant to the function's functionality.
     */
    public Builder addTypeVariable(TypeVariableName typeVariable) {
      typeVariables.add(typeVariable);
      return this;
    }

    /**
     * allows for setting the return type of a builder object, ensuring that the constructor
     * does not have a return type.
     * 
     * @param returnType type of data that the builder method will return after it has
     * completed its operation, which is specified in the function's name.
     * 
     * 	- The `returnType` is assigned to the builder instance variable `returnType`.
     * 	- The `checkState()` method checks that the `name` field does not equal `CONSTRUCTOR`,
     * indicating that a constructor call was attempted instead of a regular function call.
     * 
     * @returns a builder instance with a specified return type.
     * 
     * 	- The return type is specified as `TypeName`, which indicates that the method
     * returns a value of a specific type.
     * 	- The variable `returnType` is assigned the return type, indicating the expected
     * data type of the return value.
     * 	- The `checkState` method is used to ensure that the `name` field does not equal
     * `CONSTRUCTOR`, which suggests that the method is only intended to be called on
     * instances of the `Builder` class and not on the `Builder` constructor itself.
     */
    public Builder returns(TypeName returnType) {
      checkState(!name.equals(CONSTRUCTOR), "constructor cannot have return type.");
      this.returnType = returnType;
      return this;
    }

    /**
     * is a builder method that takes a `Type` parameter and returns a `TypeName` object
     * based on the specified type.
     * 
     * @param returnType type of value that the `Builder` class will return after calling
     * the `returns()` method.
     * 
     * `returnType`: A Type parameter, which indicates the type of data that will be
     * returned by this builder.
     * 
     * The `TypeName` class is used to convert the `returnType` parameter into a `TypeName`
     * object, which can be used to create a new instance of the desired type.
     * 
     * @returns a `Type` object of the specified type.
     * 
     * 	- The return type is specified as `TypeName.get(returnType)`, indicating that the
     * type of the returned value is determined by the `returnType` parameter.
     * 	- The `returnType` parameter is a class or interface type, suggesting that the
     * `returns` function returns an object of this type.
     * 	- The use of `TypeName.get(returnType)` implies that the returned value is of a
     * specific named type, rather than an unnamed type.
     */
    public Builder returns(Type returnType) {
      return returns(TypeName.get(returnType));
    }

    /**
     * allows adding an iterable of `ParameterSpec` objects to a builder instance, which
     * adds them to the builder's `parameters` collection.
     * 
     * @param parameterSpecs Iterable<ParameterSpec> of parameters to be added to the
     * builder object.
     * 
     * 	- `parameterSpecs` is an iterable collection of `ParameterSpec` objects.
     * 	- Each element in the `parameterSpecs` collection represents a single parameter
     * to be added to the builder.
     * 	- The `checkArgument` method is called to verify that the input `parameterSpecs`
     * is not null before attempting to add its elements to the builder's `parameters` list.
     * 
     * @returns a modified instance of the `Builder` class with additional `ParameterSpec`
     * objects added to its `parameters` list.
     * 
     * The method takes an iterable parameter `parameterSpecs` and adds each `ParameterSpec`
     * to the `parameters` list of the `Builder`. The list is not modified if the input
     * parameter `parameterSpecs` is null. The `add` method is called for each `ParameterSpec`
     * in the iterable, adding it to the `parameters` list.
     * 
     * The returned output is a reference to the same `Builder` object, allowing for
     * chaining of method calls.
     */
    public Builder addParameters(Iterable<ParameterSpec> parameterSpecs) {
      checkArgument(parameterSpecs != null, "parameterSpecs == null");
      for (ParameterSpec parameterSpec : parameterSpecs) {
        this.parameters.add(parameterSpec);
      }
      return this;
    }

    /**
     * adds a new parameter to the builder instance, allowing for further customization
     * of the resulting object.
     * 
     * @param parameterSpec specification of a parameter to be added to the builder object,
     * which can include information such as the name, type, and default value of the parameter.
     * 
     * 	- The `this` keyword in the function signature indicates that the method is used
     * to modify the current instance of the `Builder` class.
     * 	- The `parameters` field is an array of `ParameterSpec` objects that stores the
     * parameters added to the builder instance.
     * 	- The method adds a new `ParameterSpec` object to the `parameters` array by using
     * the `add` method and returning the resulting modified builder instance.
     * 
     * @returns a reference to the newly added parameter specification object.
     * 
     * The `addParameter` method adds a new parameter to the builder's list of parameters.
     * The parameter is represented by a `ParameterSpec` object passed as an argument to
     * the method.
     * 
     * The method returns a reference to the same `Builder` instance, indicating that the
     * method can be called multiple times without creating a new instance of the builder.
     */
    public Builder addParameter(ParameterSpec parameterSpec) {
      this.parameters.add(parameterSpec);
      return this;
    }

    /**
     * builds and returns a new `ParameterSpec` object representing a parameter of the
     * specified type, name, and modifiers.
     * 
     * @param type type of the parameter to be added.
     * 
     * 	- The `TypeName` type represents a custom class type that can be used to define
     * a parameter in the API.
     * 	- The `name` argument is a string that specifies the name of the parameter.
     * 	- The `modifiers` argument is an array of `Modifier` objects, which represent the
     * access modifiers for the parameter.
     * 
     * @param name name of the parameter to be added.
     * 
     * @returns a new `ParameterSpec` object built with the provided type, name, and modifiers.
     * 
     * 	- The output is a `Builder`, which means it is an object that can be used to build
     * a new instance of a class.
     * 	- The `TypeName` parameter represents the type of the parameter being added, while
     * the `String` name represents the name of the parameter.
     * 	- The `Modifier...` parameters represent the access modifiers for the parameter,
     * which determine how the code can be accessed or modified.
     */
    public Builder addParameter(TypeName type, String name, Modifier... modifiers) {
      return addParameter(ParameterSpec.builder(type, name, modifiers).build());
    }

    /**
     * adds a new parameter to a `Builder` object, specifying its type, name, and modifiers.
     * 
     * @param type type of the parameter being added to the builder object.
     * 
     * The `Type` object is a composite type that represents a parameter with a specified
     * name and optional modifiers. The `TypeName.get()` method is used to convert the
     * `type` object into a `TypeName`, which contains information about the type's name,
     * kind, and other attributes.
     * 
     * @param name name of the parameter to be added.
     * 
     * @returns a new `Parameter` instance with the specified type, name, and modifiers.
     * 
     * 	- The `Type` field represents the type of the parameter, which can be any valid
     * Java type (e.g., `String`, `Integer`, `Float`).
     * 	- The `name` field is the name of the parameter, which is a string value.
     * 	- The `modifiers` field is an array of modifiers that can be applied to the
     * parameter, such as `public`, `private`, `protected`, etc.
     * 
     * Overall, the `addParameter` function returns a `Builder` object that allows for
     * adding parameters to a `Builder` instance.
     */
    public Builder addParameter(Type type, String name, Modifier... modifiers) {
      return addParameter(TypeName.get(type), name, modifiers);
    }

    /**
     * allows for variable-length argument lists, returning a new instance with the
     * provided arguments.
     * 
     * @returns a `Builder` instance with the `true` argument passed to it.
     * 
     * The function returns a `Builder` object, which is an immutable class that allows
     * for the construction of objects in a flexible and modular way. The return value
     * of `varargs` is always a `Builder`, regardless of the input arguments provided.
     * 
     * The `varargs` function takes an optional boolean parameter `true`, which determines
     * whether the output should be a builder or not. If present, it must be a `Boolean`
     * object with a `value` field containing either `true` or `false`.
     */
    public Builder varargs() {
      return varargs(true);
    }

    /**
     * allows the builder to set a boolean value for the `varargs` field, which controls
     * whether or not the resulting object will have the `varargs` method.
     * 
     * @param varargs boolean value that determines whether the builder should allow
     * variable arguments in its construction.
     * 
     * @returns a reference to the current instance of the `Builder` class, allowing for
     * chaining method calls.
     * 
     * The `varargs` function returns a `Builder` object, which represents an immutable
     * snapshot of the current state of the builder.
     * The `varargs` parameter is a boolean value that indicates whether or not varargs
     * should be enabled for this builder. When set to true, varargs will be enabled;
     * when set to false, varargs will be disabled.
     * No other attributes are defined for this function.
     */
    public Builder varargs(boolean varargs) {
      this.varargs = varargs;
      return this;
    }

    /**
     * allows adding multiple exceptions to a builder object for an application. It takes
     * an iterable of types as input and adds each type to the builder's exceptions list.
     * 
     * @param exceptions Iterable of TypeName objects to be added to the builder's list
     * of exceptions, which is used to configure the exception handling behavior of the
     * resulting Builder.
     * 
     * 1/ `exceptions != null`: This check ensures that the input `exceptions` is not
     * null or empty before adding any elements to the `exceptions` list.
     * 2/ `TypeName exception`: This variable represents each element in the input
     * `exceptions` collection, which can contain any subtype of `TypeName`.
     * 3/ `this.exceptions.add(exception)`: Adds each `TypeName` element from the input
     * `exceptions` collection to the internal `exceptions` list of the `Builder` object.
     * 
     * @returns a modified builder instance with added exception types.
     * 
     * The method `addExceptions` takes an iterable parameter `exceptions`, which is not
     * null according to the check provided in the code. The method then adds each
     * `TypeName` element from the exceptions iterable to the `exceptions` field of the
     * builder object, using the `add()` method. As a result, the `exceptions` field of
     * the builder now contains all the added exception types.
     */
    public Builder addExceptions(Iterable<? extends TypeName> exceptions) {
      checkArgument(exceptions != null, "exceptions == null");
      for (TypeName exception : exceptions) {
        this.exceptions.add(exception);
      }
      return this;
    }

    /**
     * adds a type name to the builder's list of exceptions, allowing for more customization
     * options when building an object.
     * 
     * @param exception type of exception to be added to the list of exceptions maintained
     * by the builder object.
     * 
     * The `TypeName` parameter `exception` is an object that represents an exception
     * type. It can have various attributes such as the name of the class or interface
     * representing the exception, its serialized form, and any additional information
     * required for deserialization.
     * 
     * @returns a reference to the added `TypeName` object.
     * 
     * 	- `this`: The current builder instance.
     * 	- `exceptions`: A collection of exceptions added to the builder.
     * 	- `TypeName exception`: The type name of the exception being added.
     */
    public Builder addException(TypeName exception) {
      this.exceptions.add(exception);
      return this;
    }

    /**
     * adds a specified `Type` object to the builder's exceptions, returning the modified
     * builder for further modifications.
     * 
     * @param exception type of exception to be added to the builder, and is used to
     * identify the corresponding exception in the `addException` method.
     * 
     * TypeName.get(exception) returns the internal form of the `exception` object, which
     * contains information about its type and other attributes.
     * The return value is a new builder instance with the added exception.
     * 
     * @returns a reference to the added exception type.
     * 
     * The input `Type exception` is passed through the `TypeName.get()` method to obtain
     * the corresponding `TypeName` object. This method returns a `TypeName` object that
     * represents the type of the provided exception.
     * 
     * The returned output is a `Builder` object, which is an immutable object that can
     * be used to build other objects of the same class. The `Builder` class provides a
     * way to construct objects in a flexible and modular manner, allowing for the creation
     * of complex objects from simpler ones.
     */
    public Builder addException(Type exception) {
      return addException(TypeName.get(exception));
    }

    /**
     * allows you to add code to a builder object by providing a string format and one
     * or more argument objects.
     * 
     * @param format format string that the `code` field will be appended with.
     * 
     * @returns a modified builder instance with the added code.
     * 
     * The `Builder` object is modified when the `addCode` method is called, with the
     * specified format string and arguments.
     * 
     * The `code` field of the `Builder` object is appended with the given format string
     * and arguments, making it a dynamic property of the object.
     * 
     * The return value of the `addCode` method is the same `Builder` object, indicating
     * that the method returns itself for further modification.
     */
    public Builder addCode(String format, Object... args) {
      code.add(format, args);
      return this;
    }

    /**
     * allows adding named code to a `code` object. It takes a format string and a map
     * of argument values as input and adds them to the `code` object's named code list.
     * 
     * @param format name of a code template to be added to the builder's code.
     * 
     * @param args map of key-value pairs that will be used to format the code when the
     * `addNamed()` method is called.
     * 
     * 	- `format`: A String parameter representing the code format to be added.
     * 	- `args`: A Map object containing key-value pairs of arbitrary data types, which
     * can be customized as per the requirement.
     * 
     * @returns a builder instance with a named code element added to it.
     * 
     * The `code` field is added with the specified format and arguments using the
     * `addNamed` method.
     * The `this` keyword refers to the current builder object, indicating that the method
     * is being called on it.
     */
    public Builder addNamedCode(String format, Map<String, ?> args) {
      code.addNamed(format, args);
      return this;
    }

    /**
     * adds a `CodeBlock` object to the current instance of the `Builder` class, allowing
     * for the modification and expansion of the codebase.
     * 
     * @param codeBlock code to be added to the `code` variable of the `Builder` class.
     * 
     * 	- `code`: A mutable reference to the code container that is being built.
     * 	- `codeBlock`: A CodeBlock object that represents a single block of code in the
     * program. It has various attributes such as `lineNumber`, `startLine`, `endLine`,
     * and `content`.
     * 
     * @returns a reference to the modified `Code` object.
     * 
     * The `addCode` method in the `Builder` class adds a `CodeBlock` object to the code
     * contained within the current builder instance. The method simply adds the provided
     * `CodeBlock` object to the existing code sequence maintained by the builder, without
     * modifying it in any way. Therefore, the output of the `addCode` method is the same
     * builder instance that was passed as an argument, with the added `CodeBlock`.
     */
    public Builder addCode(CodeBlock codeBlock) {
      code.add(codeBlock);
      return this;
    }

    /**
     * adds a comment to the code with the specified format and arguments, appending it
     * to the existing comments.
     * 
     * @param format string to be added as a comment to the code.
     * 
     * @returns a comment added to the code with the specified format and arguments.
     * 
     * 	- `format`: A String parameter representing the comment format.
     * 	- `args`: An array of Objects that are used as arguments for the comment format.
     * 
     * The output is a concatenation of the `format` String and the `args` array, separated
     * by a newline character. The resulting output is added to the code contents.
     */
    public Builder addComment(String format, Object... args) {
      code.add("// " + format + "\n", args);
      return this;
    }

    /**
     * in Java allows for setting a default value for a builder object's properties. The
     * function takes a format string and zero or more arguments to use when constructing
     * the default value.
     * 
     * @param format format of the default value to be generated by the `defaultValue()`
     * method.
     * 
     * @returns a `Builder` instance with a default value set using the specified format
     * and arguments.
     * 
     * 	- The output is of type `Builder`, indicating that it can be used to build a new
     * object instance using the builder pattern.
     * 	- The output has a single method, `defaultValue`, which takes a `String` format
     * and an arbitrary number of `Object` arguments, indicating that it can set a default
     * value for any type of object.
     * 	- The method returns a new `Builder` instance, allowing the caller to continue
     * building a new object instance using the previously created builder.
     */
    public Builder defaultValue(String format, Object... args) {
      return defaultValue(CodeBlock.of(format, args));
    }

    /**
     * sets the default value for a `Builder` object. It checks if the default value has
     * already been set and then sets it to the provided `CodeBlock` parameter.
     * 
     * @param codeBlock code to be set as the default value of the builder's `defaultValue`
     * field.
     * 
     * 	- `checkState(this.defaultValue == null, "defaultValue was already set")` - Ensures
     * that the default value has not been set previously.
     * 	- `checkNotNull(codeBlock, "codeBlock == null")` - Checks if the `codeBlock` is
     * null, and throws an exception if it is.
     * 
     * @returns a reference to the provided `CodeBlock`.
     * 
     * 	- The `checkState` method is used to ensure that the `defaultValue` field is not
     * already set before assigning a new value to it.
     * 	- The `checkNotNull` method is used to check if the `codeBlock` parameter is null,
     * and throw an exception if it is.
     * 	- The `this` keyword is used to reference the current object instance.
     * 	- The `return this;` statement returns the modified object instance.
     */
    public Builder defaultValue(CodeBlock codeBlock) {
      checkState(this.defaultValue == null, "defaultValue was already set");
      this.defaultValue = checkNotNull(codeBlock, "codeBlock == null");
      return this;
    }

    /**
     * allows for the creation of a control flow object and passes it to its superclass's
     * beginControlFlow method with the specified control flow string and arguments.
     * 
     * @param controlFlow control flow of the program that is to be executed when the
     * `beginControlFlow` method is called.
     * 
     * @returns a method call to `code.beginControlFlow` with the specified control flow
     * and arguments.
     * 
     * The `controlFlow` parameter is of type String, which indicates that the method
     * takes a control flow statement as an argument. The `Object... args` parameter is
     * used to pass additional arguments to the `beginControlFlow` method.
     * 
     * The `code` variable is a reference to the code object that is being built by the
     * builder. By calling `code.beginControlFlow`, the builder is starting the control
     * flow of the code.
     * 
     * The return type of the method is `this`, which indicates that the builder instance
     * is being returned as the output of the method.
     */
    public Builder beginControlFlow(String controlFlow, Object... args) {
      code.beginControlFlow(controlFlow, args);
      return this;
    }

    /**
     * takes a `CodeBlock` parameter and returns a builder for controlling the control
     * flow of the given code block.
     * 
     * @param codeBlock code to be executed as part of control flow.
     * 
     * 	- `codeBlock`: The input code block that contains the control flow statements to
     * be executed.
     * 	- Type: CodeBlock
     * 	- Description: Represents a block of code that can contain any valid Java code,
     * including control flow statements such as if/else statements and loops.
     * 
     * @returns a new control flow instance with the specified label.
     * 
     * 	- `$L`: This represents the label that is assigned to the beginning of the control
     * flow.
     * 	- `codeBlock`: This is the code block that is being processed by the `beginControlFlow`
     * function.
     * 
     * These properties describe the structure and content of the output generated by the
     * `beginControlFlow` function.
     */
    public Builder beginControlFlow(CodeBlock codeBlock) {
      return beginControlFlow("$L", codeBlock);
    }

    /**
     * modifies the control flow of a builder by setting a new value for the `controlFlow`
     * field.
     * 
     * @param controlFlow control flow of the program after calling the `nextControlFlow`
     * method.
     * 
     * @returns a new control flow instance.
     * 
     * 	- The `controlFlow` parameter is a String that determines the control flow of the
     * application.
     * 	- The `args` parameter is an array of Objects that contain additional data used
     * by the control flow.
     * 	- The return type of the function is a `Builder` object, which is an intermediate
     * representation of the application's state that allows for further modification
     * before the final build.
     */
    public Builder nextControlFlow(String controlFlow, Object... args) {
      code.nextControlFlow(controlFlow, args);
      return this;
    }

    /**
     * takes a `CodeBlock` input and returns a new builder object with the specified label
     * ("$L").
     * 
     * @param codeBlock code that will be executed after the control flow in the current
     * block is completed.
     * 
     * 	- `$L`: This is an unqualified label reference in the function signature, indicating
     * that the `codeBlock` parameter is expected to be a code block with at least one
     * line of code.
     * 	- `codeBlock`: This parameter represents a code block, which is a sequence of
     * zero or more lines of Java code delimited by newline characters (`\n`). The code
     * in the `codeBlock` may contain any valid Java syntax elements, including expressions,
     * statements, and declarations.
     * 
     * @returns a new control flow object.
     * 
     * 	- `$L`: This is a variable that holds the value of the `codeBlock` parameter
     * passed to the function.
     * 	- `CodeBlock`: This is a class that represents a block of code in the programming
     * language. The `nextControlFlow` function returns an instance of this class.
     */
    public Builder nextControlFlow(CodeBlock codeBlock) {
      return nextControlFlow("$L", codeBlock);
    }

    /**
     * terminates the control flow of the Java code, effectively ending the execution of
     * the program.
     * 
     * @returns a builder instance with the control flow ended.
     * 
     * The `endControlFlow` function returns a `Builder` object, indicating that the
     * control flow of the code has been ended.
     * This Builder object is a reference to the original code, allowing further modifications
     * to be made to it.
     * No information about the author or licensing of the code is provided in the returned
     * output.
     */
    public Builder endControlFlow() {
      code.endControlFlow();
      return this;
    }

    /**
     * is a builder method that enables control flow to end in Java. It takes a `controlFlow`
     * parameter and zero or more `args`.
     * 
     * @param controlFlow flow of control that should be executed after the method call,
     * allowing users to specify specific actions or blocks of code to execute.
     * 
     * @returns a builder instance with the control flow action executed.
     * 
     * The `controlFlow` parameter is a string that represents the control flow of the program.
     * The `Object... args` parameter is an array of objects that contain additional
     * information about the control flow.
     */
    public Builder endControlFlow(String controlFlow, Object... args) {
      code.endControlFlow(controlFlow, args);
      return this;
    }

    /**
     * returns a builder object to which additional code can be added before the control
     * flow is terminated.
     * 
     * @param codeBlock code to be executed when control flow is reached, which the
     * function then returns after adding the appropriate labels and statements.
     * 
     * 	- `codeBlock`: A `CodeBlock` object containing the Java code to be executed.
     * 
     * @returns a builder instance with the specified label attached to the code block.
     * 
     * 	- $L: The value returned by the function is a string containing the name of the
     * control flow.
     * 	- codeBlock: The parameter passed to the function is a CodeBlock object representing
     * the code block for which control flow should be ended.
     */
    public Builder endControlFlow(CodeBlock codeBlock) {
      return endControlFlow("$L", codeBlock);
    }

    /**
     * allows for adding a statement to an existing builder object. The statement can be
     * added using a format string and any number of argument objects.
     * 
     * @param format format of the statement to be added to the builder, allowing the
     * user to specify the structure of the statement.
     * 
     * @returns a new statement added to the codebase.
     * 
     * 	- The output is a method that takes two parameters: 'format' and 'args'.
     * 	- The 'format' parameter is a string representing a message or statement to be
     * added to the code.
     * 	- The 'args' parameter is an array of objects representing any data or values
     * required by the message or statement.
     */
    public Builder addStatement(String format, Object... args) {
      code.addStatement(format, args);
      return this;
    }

    /**
     * adds a new statement to the code block of the builder object, allowing for the
     * modification of the code structure.
     * 
     * @param codeBlock Java code block that is added to the current code object by the
     * `addStatement()` method.
     * 
     * 	- `codeBlock`: The CodeBlock instance that contains the Java code to be added to
     * the `code`.
     * 	- `this`: Referencing the current Builder instance.
     * 
     * @returns a modified instance of the `Builder` class, allowing further method calls
     * to be added.
     * 
     * The `addStatement` function adds a code block to the code object. The code block
     * is an immutable representation of Java code. The function returns a reference to
     * itself, allowing for chaining of method calls to build a complete program.
     */
    public Builder addStatement(CodeBlock codeBlock) {
      code.addStatement(codeBlock);
      return this;
    }

    /**
     * creates a new instance of the `MethodSpec` class by returning an existing object
     * of type `MethodSpec`.
     * 
     * @returns a new `MethodSpec` object.
     * 
     * MethodSpec is the class that was returned as the output.
     */
    public MethodSpec build() {
      return new MethodSpec(this);
    }
  }
}
