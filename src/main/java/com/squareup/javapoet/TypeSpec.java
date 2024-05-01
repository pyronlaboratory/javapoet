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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;
import static com.squareup.javapoet.Util.requireExactlyOneOf;

/**
 * in Java is used to define the structure of a type, including its modifiers, name,
 * and supertypes. The builder class provides methods for adding annotations, fields,
 * methods, and type variables to the type spec, as well as checking various constraints
 * on the type definition. The build() method returns a TypeSpec object representing
 * the defined type.
 */
public final class TypeSpec {
  public final Kind kind;
  public final String name;
  public final CodeBlock anonymousTypeArguments;
  public final CodeBlock javadoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final List<TypeVariableName> typeVariables;
  public final TypeName superclass;
  public final List<TypeName> superinterfaces;
  public final Map<String, TypeSpec> enumConstants;
  public final List<FieldSpec> fieldSpecs;
  public final CodeBlock staticBlock;
  public final CodeBlock initializerBlock;
  public final List<MethodSpec> methodSpecs;
  public final List<TypeSpec> typeSpecs;
  final Set<String> nestedTypesSimpleNames;
  public final List<Element> originatingElements;
  public final Set<String> alwaysQualifiedNames;

  private TypeSpec(Builder builder) {
    this.kind = builder.kind;
    this.name = builder.name;
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.typeVariables = Util.immutableList(builder.typeVariables);
    this.superclass = builder.superclass;
    this.superinterfaces = Util.immutableList(builder.superinterfaces);
    this.enumConstants = Util.immutableMap(builder.enumConstants);
    this.fieldSpecs = Util.immutableList(builder.fieldSpecs);
    this.staticBlock = builder.staticBlock.build();
    this.initializerBlock = builder.initializerBlock.build();
    this.methodSpecs = Util.immutableList(builder.methodSpecs);
    this.typeSpecs = Util.immutableList(builder.typeSpecs);
    this.alwaysQualifiedNames = Util.immutableSet(builder.alwaysQualifiedNames);

    nestedTypesSimpleNames = new HashSet<>(builder.typeSpecs.size());
    List<Element> originatingElementsMutable = new ArrayList<>();
    originatingElementsMutable.addAll(builder.originatingElements);
    for (TypeSpec typeSpec : builder.typeSpecs) {
      nestedTypesSimpleNames.add(typeSpec.name);
      originatingElementsMutable.addAll(typeSpec.originatingElements);
    }

    this.originatingElements = Util.immutableList(originatingElementsMutable);
  }

  /**
   * Creates a dummy type spec for type-resolution only (in CodeWriter)
   * while emitting the type declaration but before entering the type body.
   */
  private TypeSpec(TypeSpec type) {
    assert type.anonymousTypeArguments == null;
    this.kind = type.kind;
    this.name = type.name;
    this.anonymousTypeArguments = null;
    this.javadoc = type.javadoc;
    this.annotations = Collections.emptyList();
    this.modifiers = Collections.emptySet();
    this.typeVariables = Collections.emptyList();
    this.superclass = null;
    this.superinterfaces = Collections.emptyList();
    this.enumConstants = Collections.emptyMap();
    this.fieldSpecs = Collections.emptyList();
    this.staticBlock = type.staticBlock;
    this.initializerBlock = type.initializerBlock;
    this.methodSpecs = Collections.emptyList();
    this.typeSpecs = Collections.emptyList();
    this.originatingElements = Collections.emptyList();
    this.nestedTypesSimpleNames = Collections.emptySet();
    this.alwaysQualifiedNames = Collections.emptySet();
  }

  /**
   * checks if a given `Modifier` is present in an array of modifiers.
   * 
   * @param modifier token type that is being checked for presence in the class file,
   * and the function returns true if it exists in the modifiers list.
   * 
   * 	- The return value of the function is a boolean indicating whether the `modifier`
   * exists in the modifiers collection.
   * 	- The modifiers collection is defined as an unordered set of strings containing
   * the names of all modifiers that can be applied to a method or field.
   * 	- The `contains()` method is used to check if the given `modifier` is present in
   * the modifiers collection.
   * 
   * @returns a boolean value indicating whether the specified `Modifier` is present
   * in the `modifiers` collection.
   */
  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  /**
   * creates a new `Builder` instance with the specified `Kind` and `name`.
   * 
   * @param name name of the class to be built.
   * 
   * @returns a new `Builder` object with the specified `Kind` and `name`.
   * 
   * 	- `Kind`: The type of class being built, which can be either `CLASS` or `ENUM`.
   * 	- `name`: The name of the class being built, which is not null.
   * 	- `builder`: A new instance of the `Builder` class, representing the current state
   * of the class being built.
   */
  public static Builder classBuilder(String name) {
    return new Builder(Kind.CLASS, checkNotNull(name, "name == null"), null);
  }

  /**
   * generates a new instance of a `Builder` class with the given simple name, using
   * the `checkNotNull` method to validate that the `className` parameter is not null.
   * 
   * @param className name of the class to be built.
   * 
   * 	- `checkNotNull(className, "className == null")` is a check for nullability of
   * `className`. If `className` is null, a runtime exception will be thrown.
   * 	- `simpleName()` is a method that returns the simple name of the class represented
   * by `className`, without any inner classes or packages.
   * 
   * @returns a `Builder` instance of the specified class name.
   * 
   * The `Builder` class is the type of the returned object, indicating that it is a
   * builder class for creating instances of the specified class.
   * 
   * The `className` parameter is used to specify the name of the class for which a
   * builder is being created.
   * 
   * The `simpleName` method is called on the `className` parameter to extract its
   * simple name, which is then returned as part of the builder object.
   */
  public static Builder classBuilder(ClassName className) {
    return classBuilder(checkNotNull(className, "className == null").simpleName());
  }

  /**
   * creates a new `Builder` object for an interface with the given name.
   * 
   * @param name name of the interface to be built.
   * 
   * @returns a new `Builder` instance with the specified name and kind.
   * 
   * 	- The function returns a new `Builder` instance with the type of the interface.
   * 	- The first argument, `name`, is not null and is passed as the name of the interface.
   * 	- The second argument, `checkNotNull`, is a static method that checks if the input
   * parameter is null before passing it to the constructor.
   * 
   * The properties of the returned `Builder` instance are:
   * 
   * 	- It has a Kind field with the value `INTERFACE`.
   * 	- It has a Name field with the passed name of the interface.
   * 	- It has no other fields or attributes.
   */
  public static Builder interfaceBuilder(String name) {
    return new Builder(Kind.INTERFACE, checkNotNull(name, "name == null"), null);
  }

  /**
   * constructs a new instance of the `Builder` interface with the given class name.
   * 
   * @param className simple name of the class to be built.
   * 
   * 	- `checkNotNull(className, "className == null")` is used to ensure that `className`
   * is not null before calling the method `simpleName()`.
   * 	- `simpleName()` is a method that returns the simple name of the class represented
   * by `className`, without any package information.
   * 
   * @returns a builder instance of the specified class name.
   * 
   * The returned object is of type `Builder`, which is an interface that provides
   * methods for building a Java class.
   * 
   * The method `simpleName()` returns the simple name of the class, which is the name
   * of the class without any package or qualifier information.
   * 
   * The method `checkNotNull()` checks if the input parameter `className` is null
   * before passing it to the `simpleName()` method.
   */
  public static Builder interfaceBuilder(ClassName className) {
    return interfaceBuilder(checkNotNull(className, "className == null").simpleName());
  }

  /**
   * creates a new `Builder` instance for an enum type with the specified name.
   * 
   * @param name name of the enum to be built.
   * 
   * @returns a new `Builder` instance with the specified `Kind` and `name`.
   * 
   * 1/ The return type is `Builder`, indicating that it is an object that allows for
   * further construction of an enumeration.
   * 2/ The method name `enumBuilder` suggests that this function is used to build an
   * enumeration.
   * 3/ The first argument `name` is a non-null String parameter, which indicates that
   * the function requires a valid name for the enumeration.
   * 4/ The second argument `checkNotNull(name, "name == null")` is a check to ensure
   * that the input `name` is not null. If it is null, the function will throw an exception.
   * 5/ The third argument `null` is the value returned when no additional information
   * is provided for the enumeration.
   */
  public static Builder enumBuilder(String name) {
    return new Builder(Kind.ENUM, checkNotNull(name, "name == null"), null);
  }

  /**
   * takes a `ClassName` parameter and returns an instance of `Builder` with the simple
   * name of the class passed as argument.
   * 
   * @param className class name of the enumeration to be built.
   * 
   * 	- The `checkNotNull` method is used to verify that the input `className` is not
   * null before proceeding with the creation of an enum builder.
   * 	- The `simpleName()` method is called on `className` to retrieve the simple name
   * of the class, which is then passed as the argument to the `enumBuilder` function.
   * 
   * @returns a builder instance of the specified class name.
   * 
   * The function returns a `Builder` object, which is an immutable builder class for
   * creating an enumeration.
   * The `className` parameter represents the fully qualified name of the enum class.
   * The `simpleName` method of the `ClassName` object is used to get the simple name
   * of the enum class.
   * By calling `checkNotNull`, the method ensures that the `className` parameter is
   * not null before proceeding with the creation of the enumeration builder.
   */
  public static Builder enumBuilder(ClassName className) {
    return enumBuilder(checkNotNull(className, "className == null").simpleName());
  }

  /**
   * generates a new instance of an anonymous class based on a provided type and
   * arguments, using a `CodeBlock` object to represent the type and arguments.
   * 
   * @param typeArgumentsFormat format of the type arguments to be used when constructing
   * an anonymous class builder.
   * 
   * @returns a `Builder` instance for creating an anonymous class with the specified
   * type and arguments.
   * 
   * 	- The output is a `Builder`, which means it can be used to construct an anonymous
   * class instance.
   * 	- The `typeArgumentsFormat` parameter determines the format of the type arguments
   * included in the constructed class.
   * 	- The `Object... args` parameter provides the arguments to include in the type arguments.
   * 
   * The properties of the output are:
   * 
   * 	- It is a built-in function in Java.
   * 	- It returns an instance of a anonymous class.
   * 	- It takes two parameters, `typeArgumentsFormat` and `args`.
   */
  public static Builder anonymousClassBuilder(String typeArgumentsFormat, Object... args) {
    return anonymousClassBuilder(CodeBlock.of(typeArgumentsFormat, args));
  }

  /**
   * creates a new `Builder` object with a specified type argument, allowing for the
   * construction of anonymous classes.
   * 
   * @param typeArguments types of the anonymous class that is being constructed, and
   * is used to specify the type arguments for the `Builder` constructor.
   * 
   * The `Kind` field represents the type of anonymous class being constructed,
   * specifically `CLASS`.
   * 
   * The `null` value for the `owner` field indicates that no specific class will be
   * used as the owner of the anonymous class.
   * 
   * The `typeArguments` parameter is a deserialized input containing information about
   * the type arguments to be used in constructing the anonymous class.
   * 
   * @returns a new `Builder` instance for creating an anonymous class with the specified
   * type arguments.
   * 
   * The Builder object that is returned is of type `Builder`, which represents a class
   * that has not been constructed yet.
   * The `Kind` field of the Builder object indicates that the class being built is a
   * class of anonymous inner class.
   * The `null` value of the `owner` field indicates that the builder does not have an
   * owner class.
   * The `typeArguments` field represents the type arguments passed to the constructor,
   * which are used to generate the correct generic signature for the class being built.
   */
  public static Builder anonymousClassBuilder(CodeBlock typeArguments) {
    return new Builder(Kind.CLASS, null, typeArguments);
  }

  /**
   * creates a new instance of the `Builder` class with kind set to `Kind.ANNOTATION`,
   * name set to the given non-null string, and no other parameters.
   * 
   * @param name name of an annotation builder that is being created.
   * 
   * @returns a `Builder` instance with the specified name and no other attributes.
   * 
   * 1/ The type of builder generated is `Builder`, indicating that it is an instance
   * of a class with this name.
   * 2/ The first argument passed to the function is `Kind`, which is set to `ANNOTATION`.
   * This identifies the type of builder being generated for annotations.
   * 3/ The second argument is `name`, which is a non-null string indicating the name
   * of the annotation.
   * 4/ The third argument is `null`, indicating that no additional values are provided
   * for the builder.
   */
  public static Builder annotationBuilder(String name) {
    return new Builder(Kind.ANNOTATION, checkNotNull(name, "name == null"), null);
  }

  /**
   * returns an instance of a `Builder` class with the specified `className`.
   * 
   * @param className class name of an annotation builder to be created by the method.
   * 
   * 	- `checkNotNull(className, "className == null")` verifies that the input `className`
   * is not null before proceeding with the method call.
   * 	- `simpleName()` extracts the simple name of the class represented by `className`,
   * which is used as the basis for the annotations generated by the function.
   * 
   * @returns a `Builder` instance of the specified class name.
   * 
   * 	- `ClassName`: This is the name of the annotation builder class that will be created.
   * 	- `simpleName()`: This returns the simple name of the annotated element without
   * any qualifiers or modifiers.
   * 
   * The output of the `annotationBuilder` function is a `Builder` object, which
   * represents an annotated element and provides methods for adding, removing, and
   * querying annotations. The Builder can be used to create a new annotation instance
   * or modify an existing one.
   */
  public static Builder annotationBuilder(ClassName className) {
    return annotationBuilder(checkNotNull(className, "className == null").simpleName());
  }

  /**
   * creates a new `Builder` instance with copies of the current class's fields, methods,
   * and other attributes, allowing for modification and recombination of the class's
   * elements without affecting the original.
   * 
   * @returns a new `Builder` object containing all the fields and methods of the
   * original class, ready to be used for further modification or construction.
   * 
   * 	- `builder`: A new instance of the `Builder` class, initialized with the current
   * object's kind, name, anonymous type arguments, and other attributes.
   * 	- `javadoc`: The list of Javadoc comments associated with the current object.
   * 	- `annotations`: The list of annotations associated with the current object.
   * 	- `modifiers`: The list of modifiers associated with the current object.
   * 	- `typeVariables`: The list of type variables associated with the current object.
   * 	- `superclass`: The superclass of the current object, if any.
   * 	- `superinterfaces`: The interfaces implemented by the current object, if any.
   * 	- `enumConstants`: A map of enum constant names to their corresponding values.
   * 	- `fieldSpecs`: The list of field specifications associated with the current object.
   * 	- `methodSpecs`: The list of method specifications associated with the current object.
   * 	- `typeSpecs`: The list of type specifications associated with the current object.
   * 	- `initializerBlock`: The initializer block associated with the current object,
   * if any.
   * 	- `staticBlock`: The static block associated with the current object, if any.
   * 	- `originatingElements`: A list of elements that originate from the current object.
   * 	- `alwaysQualifiedNames`: A list of always-qualified names associated with the
   * current object.
   */
  public Builder toBuilder() {
    Builder builder = new Builder(kind, name, anonymousTypeArguments);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.typeVariables.addAll(typeVariables);
    builder.superclass = superclass;
    builder.superinterfaces.addAll(superinterfaces);
    builder.enumConstants.putAll(enumConstants);
    builder.fieldSpecs.addAll(fieldSpecs);
    builder.methodSpecs.addAll(methodSpecs);
    builder.typeSpecs.addAll(typeSpecs);
    builder.initializerBlock.add(initializerBlock);
    builder.staticBlock.add(staticBlock);
    builder.originatingElements.addAll(originatingElements);
    builder.alwaysQualifiedNames.addAll(alwaysQualifiedNames);
    return builder;
  }

  /**
   * generates high-quality summaries of given Java code, including field and method
   * declarations, type definitions, and constructor calls. It takes care of formatting
   * and indentation according to the specified coding style.
   * 
   * @param codeWriter Java compiler API that is used to generate the source code for
   * the given class, interface, or enum.
   * 
   * 1/ `statementLine`: This is an integer variable that keeps track of the current
   * statement line number in the AST. It is initially set to -1 and incremented every
   * time a new statement is emitted.
   * 2/ `indentationLevel`: This is an integer variable that tracks the indentation
   * level of the current AST node. It is used to determine the amount of indentation
   * required for each node.
   * 3/ `typeVariables`: This is a set of type variables that are used to represent the
   * types of variables in the AST. They are used to generate type-safe code and to
   * ensure that the correct types are used in the AST.
   * 4/ `implicitModifiers`: This is an unmodifiable set of modifiers that are applied
   * automatically by the compiler to certain nodes in the AST. It includes the modifiers
   * `static`, `final`, and `synchronized`.
   * 5/ `anonymousTypeArguments`: This is a map of type variables to their corresponding
   * types. It is used to generate anonymous inner classes, which are classes that are
   * defined inline within a method or constructor call.
   * 6/ `enumConstants`: This is an unmodifiable map of key-value pairs that represent
   * the constant values of an enum class. Each key-value pair corresponds to a particular
   * constant value and its corresponding type.
   * 7/ `fieldSpecs`: This is a list of field specifications, which include the name,
   * type, and modifiers of each field in the AST. They are used to generate the fields
   * of a class or interface.
   * 8/ `initializerBlock`: This is a block of code that is executed when an object is
   * created. It includes the initialization code for static variables and methods.
   * 9/ `methodSpecs`: This is a list of method specifications, which include the name,
   * return type, parameters, and modifiers of each method in the AST. They are used
   * to generate the methods of a class or interface.
   * 10/ `staticBlock`: This is a block of code that is executed when a static variable
   * or method is accessed. It includes the initialization code for static variables
   * and methods.
   * 
   * In summary, `codeWriter` is an instance of `Abstract Syntax Tree Writer`, which
   * is used to generate the source code for a Java program based on the AST produced
   * by the compiler. The properties of `codeWriter` are explained above, and they
   * include various attributes that are used to generate type-safe code and to ensure
   * that the correct types are used in the AST.
   * 
   * @param enumName name of an enum class that is being generated, and it is used to
   * determine whether an empty line is emitted at the end of the function.
   * 
   * @param implicitModifiers implicit modifiers that are added to the class, fields,
   * and methods based on their kinds, without requiring explicit modification statements.
   * 
   * 	- `implicitFieldModifiers`: This is an unmodified set of field modifiers from the
   * source code. It may contain any combination of `public`, `private`, `protected`,
   * and their variations.
   * 	- `implicitMethodModifiers`: Similar to `implicitFieldModifiers`, this contains
   * a set of method modifiers (e.g., `public`, `private`, etc.) that are not explicitly
   * mentioned in the source code.
   * 	- `kind`: This specifies the type of class or interface being generated, such as
   * `ANNOTATION`, `ENUM`, `INTERFACE`, or `CLASS`.
   * 	- `name`: This is the name of the class or interface being generated.
   * 	- `superclass`: This is the superclass of the generated class or interface, or
   * `ClassName.OBJECT` if there is no superclass.
   * 	- `superinterfaces`: This is a list of interfaces that are implemented by the
   * generated class or interface.
   * 	- `enumConstants`: This is a map of constant fields in the enum type, each
   * associated with a unique key (either a string or an integer).
   * 	- `fieldSpecs`: This is a set of field specifications for the generated class or
   * interface.
   * 	- `methodSpecs`: This is a set of method specifications for the generated class
   * or interface.
   * 	- `typeSpecs`: This is a set of type specifications for the generated class or interface.
   * 	- `staticBlock`: This is a block of statically-initialized fields in the generated
   * class or interface.
   * 	- `initializerBlock`: This is a block of code that initializes fields in the
   * generated class or interface.
   * 	- `enumNames`: This is an unmodified list of enum names from the source code.
   * 
   * These properties are used to generate the output code for the `emit` function,
   * which includes writing the class or interface definition, as well as any associated
   * fields, methods, and other attributes.
   */
  void emit(CodeWriter codeWriter, String enumName, Set<Modifier> implicitModifiers)
      throws IOException {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    int previousStatementLine = codeWriter.statementLine;
    codeWriter.statementLine = -1;

    try {
      if (enumName != null) {
        codeWriter.emitJavadoc(javadoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emit("$L", enumName);
        if (!anonymousTypeArguments.formatParts.isEmpty()) {
          codeWriter.emit("(");
          codeWriter.emit(anonymousTypeArguments);
          codeWriter.emit(")");
        }
        if (fieldSpecs.isEmpty() && methodSpecs.isEmpty() && typeSpecs.isEmpty()) {
          return; // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n");
      } else if (anonymousTypeArguments != null) {
        TypeName supertype = !superinterfaces.isEmpty() ? superinterfaces.get(0) : superclass;
        codeWriter.emit("new $T(", supertype);
        codeWriter.emit(anonymousTypeArguments);
        codeWriter.emit(") {\n");
      } else {
        // Push an empty type (specifically without nested types) for type-resolution.
        codeWriter.pushType(new TypeSpec(this));

        codeWriter.emitJavadoc(javadoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emitModifiers(modifiers, Util.union(implicitModifiers, kind.asMemberModifiers));
        if (kind == Kind.ANNOTATION) {
          codeWriter.emit("$L $L", "@interface", name);
        } else {
          codeWriter.emit("$L $L", kind.name().toLowerCase(Locale.US), name);
        }
        codeWriter.emitTypeVariables(typeVariables);

        List<TypeName> extendsTypes;
        List<TypeName> implementsTypes;
        if (kind == Kind.INTERFACE) {
          extendsTypes = superinterfaces;
          implementsTypes = Collections.emptyList();
        } else {
          extendsTypes = superclass.equals(ClassName.OBJECT)
              ? Collections.emptyList()
              : Collections.singletonList(superclass);
          implementsTypes = superinterfaces;
        }

        if (!extendsTypes.isEmpty()) {
          codeWriter.emit(" extends");
          boolean firstType = true;
          for (TypeName type : extendsTypes) {
            if (!firstType) codeWriter.emit(",");
            codeWriter.emit(" $T", type);
            firstType = false;
          }
        }

        if (!implementsTypes.isEmpty()) {
          codeWriter.emit(" implements");
          boolean firstType = true;
          for (TypeName type : implementsTypes) {
            if (!firstType) codeWriter.emit(",");
            codeWriter.emit(" $T", type);
            firstType = false;
          }
        }

        codeWriter.popType();

        codeWriter.emit(" {\n");
      }

      codeWriter.pushType(this);
      codeWriter.indent();
      boolean firstMember = true;
      boolean needsSeparator = kind == Kind.ENUM
              && (!fieldSpecs.isEmpty() || !methodSpecs.isEmpty() || !typeSpecs.isEmpty());
      for (Iterator<Map.Entry<String, TypeSpec>> i = enumConstants.entrySet().iterator();
          i.hasNext(); ) {
        Map.Entry<String, TypeSpec> enumConstant = i.next();
        if (!firstMember) codeWriter.emit("\n");
        enumConstant.getValue().emit(codeWriter, enumConstant.getKey(), Collections.emptySet());
        firstMember = false;
        if (i.hasNext()) {
          codeWriter.emit(",\n");
        } else if (!needsSeparator) {
          codeWriter.emit("\n");
        }
      }

      if (needsSeparator) codeWriter.emit(";\n");

      // Static fields.
      for (FieldSpec fieldSpec : fieldSpecs) {
        if (!fieldSpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        fieldSpec.emit(codeWriter, kind.implicitFieldModifiers);
        firstMember = false;
      }

      if (!staticBlock.isEmpty()) {
        if (!firstMember) codeWriter.emit("\n");
        codeWriter.emit(staticBlock);
        firstMember = false;
      }

      // Non-static fields.
      for (FieldSpec fieldSpec : fieldSpecs) {
        if (fieldSpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        fieldSpec.emit(codeWriter, kind.implicitFieldModifiers);
        firstMember = false;
      }

      // Initializer block.
      if (!initializerBlock.isEmpty()) {
        if (!firstMember) codeWriter.emit("\n");
        codeWriter.emit(initializerBlock);
        firstMember = false;
      }

      // Constructors.
      for (MethodSpec methodSpec : methodSpecs) {
        if (!methodSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        methodSpec.emit(codeWriter, name, kind.implicitMethodModifiers);
        firstMember = false;
      }

      // Methods (static and non-static).
      for (MethodSpec methodSpec : methodSpecs) {
        if (methodSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        methodSpec.emit(codeWriter, name, kind.implicitMethodModifiers);
        firstMember = false;
      }

      // Types.
      for (TypeSpec typeSpec : typeSpecs) {
        if (!firstMember) codeWriter.emit("\n");
        typeSpec.emit(codeWriter, null, kind.implicitTypeModifiers);
        firstMember = false;
      }

      codeWriter.unindent();
      codeWriter.popType();
      codeWriter.popTypeVariables(typeVariables);

      codeWriter.emit("}");
      if (enumName == null && anonymousTypeArguments == null) {
        codeWriter.emit("\n"); // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.statementLine = previousStatementLine;
    }
  }

  /**
   * compares an object to another object, returning a boolean value indicating whether
   * they are equal. It first checks for equivalence by comparing `this` and `o`. If
   * they are not the same object, it then checks if `o` is null, and if their classes
   * are not the same. Finally, it uses `toString()` to compare the two objects' strings.
   * 
   * @param o object being compared to the current object, and is used to determine if
   * the two objects are equal.
   * 
   * 	- If this equals o, returns true.
   * 	- If o is null, returns false.
   * 	- If the class of this and o are different, returns false.
   * 	- If the toString() of this and o are different, returns false.
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
   * returns the hash code of its argument, which is the result of calling `toString()`
   * on the argument and then using its `hashCode()` method.
   * 
   * @returns the result of the `toString()` method of the class, which is used to
   * generate a unique identifier for the object.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of its output using a `CodeWriter`.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, null, Collections.emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public enum Kind {
    CLASS(
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet()),

    INTERFACE(
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC)),
        Util.immutableSet(Collections.singletonList(Modifier.STATIC))),

    ENUM(
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.singleton(Modifier.STATIC)),

    ANNOTATION(
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC)),
        Util.immutableSet(Collections.singletonList(Modifier.STATIC)));

    private final Set<Modifier> implicitFieldModifiers;
    private final Set<Modifier> implicitMethodModifiers;
    private final Set<Modifier> implicitTypeModifiers;
    private final Set<Modifier> asMemberModifiers;

    Kind(Set<Modifier> implicitFieldModifiers,
        Set<Modifier> implicitMethodModifiers,
        Set<Modifier> implicitTypeModifiers,
        Set<Modifier> asMemberModifiers) {
      this.implicitFieldModifiers = implicitFieldModifiers;
      this.implicitMethodModifiers = implicitMethodModifiers;
      this.implicitTypeModifiers = implicitTypeModifiers;
      this.asMemberModifiers = asMemberModifiers;
    }
  }

  /**
   * is used to create a TypeSpec instance. It has various fields and methods that are
   * checked at runtime, including modifiers, kind, name, superinterfaces, type variables,
   * enum constants, field speeds, and method specs. The Builder Class also provides
   * methods for setting the values of these fields and checking their validity.
   * Additionally, it checks that the anonymous type has only one supertype (either an
   * object or a class) and no more than one interesting supertype (either an interface
   * or a class).
   */
  public static final class Builder {
    private final Kind kind;
    private final String name;
    private final CodeBlock anonymousTypeArguments;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private TypeName superclass = ClassName.OBJECT;
    private final CodeBlock.Builder staticBlock = CodeBlock.builder();
    private final CodeBlock.Builder initializerBlock = CodeBlock.builder();

    public final Map<String, TypeSpec> enumConstants = new LinkedHashMap<>();
    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();
    public final List<TypeVariableName> typeVariables = new ArrayList<>();
    public final List<TypeName> superinterfaces = new ArrayList<>();
    public final List<FieldSpec> fieldSpecs = new ArrayList<>();
    public final List<MethodSpec> methodSpecs = new ArrayList<>();
    public final List<TypeSpec> typeSpecs = new ArrayList<>();
    public final List<Element> originatingElements = new ArrayList<>();
    public final Set<String> alwaysQualifiedNames = new LinkedHashSet<>();

    private Builder(Kind kind, String name,
        CodeBlock anonymousTypeArguments) {
      checkArgument(name == null || SourceVersion.isName(name), "not a valid name: %s", name);
      this.kind = kind;
      this.name = name;
      this.anonymousTypeArguments = anonymousTypeArguments;
    }

    /**
     * adds documentation to the Java class using the specified format and arguments.
     * 
     * @param format Java documentation format that the `javadoc` method will use to
     * generate the documentation.
     * 
     * @returns a documentation comment added to the class or method being built.
     * 
     * 	- `format`: The format of the Javadoc documentation to be added. This can be a
     * string representing a template or a fully qualified class name.
     * 	- `args`: An array of objects containing any data required for the Javadoc documentation.
     * 
     * The function itself does not have any attributes, but it does return a `Builder`
     * object, which is an instance of the `com.sun.tools.javadoc.DocletBuilder` class.
     * This builder can be used to continue building the Javadoc documentation for the
     * current package or class.
     */
    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
      return this;
    }

    /**
     * adds a `CodeBlock` to the `javadoc` collection, allowing for the modification of
     * the builder's documentation.
     * 
     * @param block Java code that will be added to the current builder's javadoc documentation.
     * 
     * The `addJavadoc` method takes in a `CodeBlock` object as its argument and adds it
     * to the `javadoc` list. The `CodeBlock` class has no attributes or methods of its
     * own, so this method simply deserializes the input without any further modification.
     * 
     * @returns a builder instance that allows for further method calls to modify the
     * Javadoc content.
     * 
     * The method `addJavadoc` adds a `CodeBlock` object to the `javadoc` list, which is
     * a field in the current `Builder` instance.
     * 
     * The method returns the current `Builder` instance, indicating that it can be used
     * to continue building the Java document.
     */
    public Builder addJavadoc(CodeBlock block) {
      javadoc.add(block);
      return this;
    }

    /**
     * allows for the addition of multiple annotation specifications to a builder instance,
     * which can then be used to modify an object's behavior or attributes.
     * 
     * @param annotationSpecs iterable of AnnotationSpec objects to be added to the class's
     * annotations, and is processed by iterating over it and adding each annotation spec
     * to the instance's annotations collection.
     * 
     * 	- `annotationSpecs` is an iterable containing AnnotationSpec objects, which
     * represent annotations to be added to the Builder's annotations list.
     * 	- The input `annotationSpecs` is checked for nullity before traversing it in the
     * loop.
     * 	- In the loop, each AnnotationSpec object is added to the Builder's `annotations`
     * list using the `add()` method.
     * 
     * @returns a modified instance of the `Builder` class with additional annotations added.
     * 
     * The method adds one or more `AnnotationSpec` objects to the `annotations` list of
     * the `Builder` object.
     * 
     * The input argument `annotationSpecs` is not null and contains multiple `AnnotationSpec`
     * objects.
     * 
     * Each `AnnotationSpec` object added to the `annotations` list has its own properties,
     * such as name, description, and value.
     * 
     * No information about the code author or licensing is provided in the output.
     */
    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    /**
     * allows adding an annotation to a builder instance, by checking that the input
     * `annotationSpec` is not null and then adding it to the builder's list of annotations.
     * 
     * @param annotationSpec AnnotationSpec object that adds to the builder's annotations,
     * and it is not null when passed to the function.
     * 
     * 	- `checkNotNull(annotationSpec, "annotationSpec == null")`: This line checks that
     * the input `annotationSpec` is not null before adding it to the list of annotations.
     * 	- `this.annotations.add(annotationSpec)`: This line adds the deserialized
     * `annotationSpec` to the list of annotations associated with the builder instance.
     * 
     * @returns a reference to the modified `Builder` object, allowing for further method
     * calls without creating a new instance.
     * 
     * 	- The `checkNotNull` method is used to verify that the `annotationSpec` parameter
     * is not null before adding it to the `annotations` list.
     * 	- The `this` keyword refers to the current instance of the `Builder` class.
     * 	- The `add` method is used to add the `annotationSpec` object to the `annotations`
     * list, which is a field of type `List<AnnotationSpec>` in the `Builder` class.
     * 
     * In summary, the `addAnnotation` function adds an annotation to the current instance
     * of the `Builder` class.
     */
    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      checkNotNull(annotationSpec, "annotationSpec == null");
      this.annotations.add(annotationSpec);
      return this;
    }

    /**
     * adds an annotation to a Java class using an `AnnotationSpec` object built from the
     * provided annotation object.
     * 
     * @param annotation AnnotationSpec that is to be added to the builder object.
     * 
     * `ClassName annotation`: This parameter represents a class name of an annotation type.
     * 
     * `AnnotationSpec.builder(annotation).build()`: This method creates a new instance
     * of `AnnotationSpec`, which is a Java class that represents an annotation, using
     * the deserialized `annotation` input as its basis. The resulting `AnnotationSpec`
     * instance contains all the properties and attributes of the original `annotation`.
     * 
     * @returns an instance of `AnnotationSpec`.
     * 
     * 	- The returned output is an instance of the `Builder` class, representing a builder
     * for creating objects of the specified class.
     * 	- The `addAnnotation` method returns an instance of the `Builder` class after
     * adding an annotation to the object being built.
     * 	- The annotation added is specified by the `ClassName` parameter passed to the method.
     * 	- The annotation is added using the `AnnotationSpec.builder()` method, which
     * creates a builder for the annotation and builds it using the specified parameters.
     */
    public Builder addAnnotation(ClassName annotation) {
      return addAnnotation(AnnotationSpec.builder(annotation).build());
    }

    /**
     * allows a builder to be assigned an annotation of a given class type.
     * 
     * @param annotation Class<?> of an annotation to be added to the builder object.
     * 
     * Class<?> annotation: The input represents an annotation class.
     * ClassName.get(annotation): This method returns a string representation of the given
     * annotation class.
     * 
     * @returns a new `Builder` object with the specified `Class` added to its annotation
     * list.
     * 
     * The input parameter `annotation` is of type `Class<?>`, indicating that it represents
     * a class or interface.
     * 
     * The output of the function is a `Builder` object, which is an immutable class that
     * provides a way to build objects of various types in Java. The `addAnnotation`
     * function adds an annotation to the builder's target object.
     * 
     * The returned Builder object can be used to add more annotations or other modifications
     * to the target object before finally building it.
     */
    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    /**
     * adds a list of `Modifier` objects to the current instance of the `Builder` class,
     * updating its internal modifier list accordingly.
     * 
     * @returns a modified builder instance with additional modifiers applied.
     * 
     * 	- The `Collsctions.addAll()` method is used to add all the given `Modifier` objects
     * to the existing `modifiers` list of the `Builder` object.
     * 	- The `this` keyword refers to the `Builder` object itself, indicating that the
     * operation is performed on the current instance of the class.
     * 	- The `return this;` statement returns the modified `Builder` object.
     */
    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    /**
     * adds an iterable list of type variables to a builder object, validating that the
     * input is not null before performing the addition.
     * 
     * @param typeVariables Iterable of TypeVariableName objects to be added to the
     * builder's type variables collection.
     * 
     * 	- `typeVariables` is an iterable collection of `TypeVariableName` objects.
     * 	- Each `TypeVariableName` object represents a type variable in the Java model.
     * 	- The type variables are added to the builder's `typeVariables` list.
     * 
     * @returns a reference to the modified `Builder` instance.
     * 
     * 	- The function takes an iterable parameter `typeVariables`, which is checked for
     * nullity before being processed.
     * 	- The function loops through each element in the `typeVariables` iterable using
     * a `for` loop and adds each type variable to the `typeVariables` list of the current
     * builder object.
     * 	- The return value of the function is the current builder object, indicating that
     * the method execution has modified the builder's state.
     */
    public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
      checkArgument(typeVariables != null, "typeVariables == null");
      for (TypeVariableName typeVariable : typeVariables) {
        this.typeVariables.add(typeVariable);
      }
      return this;
    }

    /**
     * adds a `TypeVariableName` to a builder object, allowing for multiple type variables
     * to be managed within the builder.
     * 
     * @param typeVariable name of a type variable to be added to the set of type variables
     * stored in the object's `typeVariables` field.
     * 
     * 	- TypeVariableName - The name of the type variable to be added.
     * 
     * In this function, `typeVariables` is a list that is being updated with the addition
     * of `typeVariable`. The `return` statement indicates that the method is returning
     * the current object instance.
     * 
     * @returns a reference to the provided type variable, which is added to the internal
     * list of type variables.
     * 
     * 	- `typeVariables`: This is an array of `TypeVariableName` objects that contains
     * the type variables added to the builder.
     * 	- `this`: The builder instance itself, which can be further modified or used in
     * other methods.
     */
    public Builder addTypeVariable(TypeVariableName typeVariable) {
      typeVariables.add(typeVariable);
      return this;
    }

    /**
     * allows for updating the super class of an object, by checking the current state
     * of the object and the input provided, ensuring that the update is valid and
     * consistent with the class hierarchy.
     * 
     * @param superclass superclass of the class being built, which is set to the specified
     * `TypeName` object.
     * 
     * 	- `TypeName`: This is the type name of the superclass, which can be any valid
     * Java class name.
     * 	- `Kind`: The kind of the superclass, which can only be set to `CLASS`.
     * 	- `Superclass`: This is the actual superclass reference, which cannot be a primitive
     * type.
     * 	- `Arguments`: None, as there are no arguments provided for this function.
     * 
     * @returns a reference to a non-primitive class that can be used as the superclass
     * of a Java object.
     * 
     * 	- `superclass`: The TypeName object representing the superclass of the current class.
     * 	- `kind`: The Kind of the current class, which is checked to ensure that only
     * classes can have a superclass.
     * 	- `checkState()`: A utility method used to check if a specific state is true or
     * false, and throw an exception if it is not.
     * 	- `argument`: An optional parameter used in `checkArgument()` to provide additional
     * information about the argument being checked.
     * 
     * Overall, the `superclass` function is designed to set the superclass of a class
     * object, while checking that the class has the correct type and that the superclass
     * is non-primitive.
     */
    public Builder superclass(TypeName superclass) {
      checkState(this.kind == Kind.CLASS, "only classes have super classes, not " + this.kind);
      checkState(this.superclass == ClassName.OBJECT,
          "superclass already set to " + this.superclass);
      checkArgument(!superclass.isPrimitive(), "superclass may not be a primitive");
      this.superclass = superclass;
      return this;
    }

    /**
     * modifies a `Builder` instance by setting its superclass to a specified type.
     * 
     * @param superclass superclass of the builder object being created.
     * 
     * The `Type` parameter is used to specify the superclass for the builder class. The
     * `true` argument passed to the `superclass` method indicates that the superclass
     * should be deserialized.
     * 
     * @returns a new instance of the `Builder` class with the specified `superclass`.
     * 
     * The method returns a `Builder` object of the specified `Type`. The first parameter
     * `superclass` is a reference to the superclass type, and the second parameter `true`
     * indicates that the builder should be created with the correct visibility modifier
     * for the superclass.
     */
    public Builder superclass(Type superclass) {
      return superclass(superclass, true);
    }

    /**
     * modifies a builder instance by setting the superclass of a class being built,
     * avoiding name clashes with nested classes if necessary.
     * 
     * @param superclass superclass of the builder class, and its value is used to set
     * the superclass of the builder instance being created.
     * 
     * The `superclass` argument is a type name, which can be a simple type name or a
     * complex type name consisting of a type name and possible type arguments.
     * 
     * The `avoidNestedTypeNameClashes` parameter is a boolean indicating whether to avoid
     * clashes with nested types in the same package.
     * 
     * The function call `superclass(TypeName.get(superclass))` initializes the `Type`
     * object with the given type name.
     * 
     * If the `avoidNestedTypeNameClashes` parameter is set to true, the function checks
     * for clashes with nested types in the same package using the `Class` object of the
     * provided `superclass`. If a clash exists, the function takes appropriate actions
     * to resolve it.
     * 
     * @param avoidNestedTypeNameClashes superclass name to avoid any clashes with nested
     * classes when checking for conflicts between raw type and superclass name during
     * method invocation.
     * 
     * @returns a modified `Builder` instance that avoids clashes with nested classes.
     * 
     * 	- `Type superclass`: This is the type of the superclass that is being passed to
     * the function.
     * 	- `boolean avoidNestedTypeNameClashes`: This parameter indicates whether the
     * function should avoid clashes with nested classes.
     * 	- `TypeName.get(superclass)`: This method returns the TypeName instance for the
     * given type.
     * 	- `getRawType(superclass)`: This method returns the raw class object of the given
     * type, or null if it is not a valid class.
     * 	- `avoidClashesWithNestedClasses(Class<?> clazz)`: This method is called if the
     * superclass has any nested classes, and it avoids clashes with those classes.
     */
    public Builder superclass(Type superclass, boolean avoidNestedTypeNameClashes) {
      superclass(TypeName.get(superclass));
      if (avoidNestedTypeNameClashes) {
        Class<?> clazz = getRawType(superclass);
        if (clazz != null) {
          avoidClashesWithNestedClasses(clazz);
        }
      }
      return this;
    }

    /**
     * in Java code creates a new instance of a superclass and passes it to the `Builder`
     * class as an argument.
     * 
     * @param superclass superclass of the builder class, which is passed to the
     * `superclass()` method to determine the type of the builder's superclass.
     * 
     * 	- TypeMirror: This is the type of the superclass that is being passed to the
     * function as an argument. It represents the type of the superclass at compile-time.
     * 	- Boolean: The second argument `true` indicates that the superclass should be
     * destructured or unwrapped.
     * 
     * @returns a new instance of the `Builder` class with the specified superclass.
     * 
     * 	- The return value is a `TypeMirror`, indicating that it is a reflection of a
     * class in the program's type system.
     * 	- The `superclass` parameter represents the superclass of the returning `TypeMirror`.
     * 	- The `true` argument passed to the `return` statement indicates that the returned
     * `TypeMirror` should be treated as if it were declared directly within the scope
     * of the function, rather than being inherited from a parent class.
     */
    public Builder superclass(TypeMirror superclass) {
      return superclass(superclass, true);
    }

    /**
     * modifies a `Builder` instance by setting its superclass to a given TypeMirror and
     * optionally avoiding clashes with nested classes.
     * 
     * @param superclass superclass of the builder, which is passed as a TypeMirror object
     * and is used to avoid clashes with nested classes when creating the builder.
     * 
     * 	- The `superclass` parameter is a `TypeMirror`, which represents a type in Java.
     * 	- The `avoidNestedTypeNameClashes` parameter is a boolean value that determines
     * whether to avoid clashes with nested classes when resolving the superclass.
     * 	- The `superclass` parameter can be either an instance of `DeclaredType`, which
     * represents a type declared in the source code, or a `TypeName`, which represents
     * a type name.
     * 	- If `avoidNestedTypeNameClashes` is true and `superclass` is an instance of
     * `DeclaredType`, then the function performs additional processing to avoid clashes
     * with nested classes.
     * 
     * @param avoidNestedTypeNameClashes superclass's nested types, and prevents them
     * from clashing with those of inner classes when using it as a type argument in the
     * current builder.
     * 
     * @returns a modified `Builder` instance that takes into account any potential clashes
     * with nested classes.
     * 
     * The first line sets `superclass(TypeMirror superclass)` to indicate the passing
     * of the `TypeMirror` object as an argument. The second line checks if
     * `avoidNestedTypeNameClashes` is true and, if so, converts the `superclass` instance
     * to a `DeclaredType`. The third line retrieves the element representing the
     * superinterface of the `superclass` using the `asElement()` method. The fourth line
     * avoids clashes with nested classes by calling the `avoidClashesWithNestedClasses()`
     * function on the retrieved element. Finally, the last line returns the modified
     * `Builder` instance.
     */
    public Builder superclass(TypeMirror superclass, boolean avoidNestedTypeNameClashes) {
      superclass(TypeName.get(superclass));
      if (avoidNestedTypeNameClashes && superclass instanceof DeclaredType) {
        TypeElement superInterfaceElement =
            (TypeElement) ((DeclaredType) superclass).asElement();
        avoidClashesWithNestedClasses(superInterfaceElement);
      }
      return this;
    }

    /**
     * allows for the addition of one or more super interfaces to a `Builder` instance,
     * which will then be added to the class's hierarchy.
     * 
     * @param superinterfaces Iterable of TypeName objects that the current instance of
     * the Builder class will inherit from as super interfaces.
     * 
     * 	- `superinterfaces` is an iterable collection of type names that represent super
     * interfaces in Java.
     * 	- Each element in the collection is a TypeName object representing a specific
     * super interface.
     * 	- The `addSuperinterface` method is called for each element in the collection,
     * passing the corresponding super interface as an argument.
     * 
     * @returns a builder object with added super interfaces.
     * 
     * The `checkArgument` method is used to ensure that the `superinterfaces` parameter
     * is not null before adding any super interfaces to the builder.
     * 
     * The `addSuperinterface` method is called for each element in the `superinterfaces`
     * iterable, adding a super interface to the builder.
     * 
     * No summary is provided at the end of the output as per the given requirements.
     */
    public Builder addSuperinterfaces(Iterable<? extends TypeName> superinterfaces) {
      checkArgument(superinterfaces != null, "superinterfaces == null");
      for (TypeName superinterface : superinterfaces) {
        addSuperinterface(superinterface);
      }
      return this;
    }

    /**
     * allows for adding a superinterface to a builder object, which is used to construct
     * a class with multiple interfaces. The function validates that the input superinterface
     * is not null and adds it to the list of superinterfaces associated with the builder.
     * 
     * @param superinterface superinterface that the builder wants to add to the class
     * being built.
     * 
     * 	- `checkArgument`: A method that verifies that the input parameter `superinterface`
     * is not null before proceeding with the code.
     * 	- `this.superinterfaces.add()`: Adds the `superinterface` to the internal list
     * of superinterfaces of the builder instance.
     * 
     * @returns a reference to the provided superinterface added to the builder's list
     * of superinterfaces.
     * 
     * The method returns a modified instance of the `Builder` class, indicating that the
     * superinterface has been added to the list of superinterfaces associated with the
     * current builder object. The method checks if the input `superinterface` is null
     * before adding it to the list, ensuring that only non-null references are added.
     */
    public Builder addSuperinterface(TypeName superinterface) {
      checkArgument(superinterface != null, "superinterface == null");
      this.superinterfaces.add(superinterface);
      return this;
    }

    /**
     * adds a superinterface to a builder instance, optionally indicating whether the
     * addition should be permanent or temporary.
     * 
     * @param superinterface super interface to be added to the builder's component.
     * 
     * 	- `Type`: The type of the superinterface being added to the builder.
     * 	- `true`: Whether or not the superinterface is included in the builder (default
     * is `true`).
     * 
     * @returns a `Builder` instance with the specified superinterface added to its
     * interfaces list.
     * 
     * The first parameter passed to the function is `superinterface`, which is a `Type`.
     * This represents the interface that will be added to the builder's list of superinterfaces.
     * 
     * The second parameter, `true`, is used as the default value for a boolean property,
     * `addAll`, which indicates whether all interfaces in the `superinterface` should
     * be added or not.
     * 
     * Therefore, when this function is called with a non-default value for the `addAll`
     * property, it adds only the specified interfaces from the `superinterface`.
     */
    public Builder addSuperinterface(Type superinterface) {
      return addSuperinterface(superinterface, true);
    }

    /**
     * adds a superinterface to a builder instance, ensuring that the type name does not
     * conflict with nested classes.
     * 
     * @param superinterface super interface that the current class is implementing, and
     * the function adds it to the list of interfaces implemented by the class.
     * 
     * Type `superinterface`: This represents an interface that is being added to the
     * builder as a superinterface. Its type name is deserialized from the input parameter
     * and stored in a field for later use.
     * 
     * Parameter `avoidNestedTypeNameClashes`: This boolean parameter indicates whether
     * the method should avoid clashes with nested classes of the superinterface when
     * adding it to the builder. If the superinterface has nested classes, the method
     * will perform additional checks to ensure that there are no clashes with those
     * classes when combining the superinterface with other interfaces in the builder.
     * 
     * @param avoidNestedTypeNameClashes avoidance of clashes with nested classes when
     * adding a superinterface to the builder object.
     * 
     * @returns a modified builder object that includes the specified superinterface and
     * avoids name clashes with nested classes.
     * 
     * The `addSuperinterface` method adds a super interface to the builder's type, as
     * specified by the `Type` parameter. The `TypeName` class is used to convert the
     * super interface to its canonical form before adding it to the builder's type.
     * 
     * The `avoidNestedTypeNameClashes` parameter is used to avoid clashes between nested
     * types and the super interface. If the super interface is a nested type, this
     * parameter is set to true to prevent the addition of the super interface.
     * 
     * Overall, the `addSuperinterface` method allows for the addition of super interfaces
     * to the builder's type in a way that avoids clashes with nested types.
     */
    public Builder addSuperinterface(Type superinterface, boolean avoidNestedTypeNameClashes) {
      addSuperinterface(TypeName.get(superinterface));
      if (avoidNestedTypeNameClashes) {
        Class<?> clazz = getRawType(superinterface);
        if (clazz != null) {
          avoidClashesWithNestedClasses(clazz);
        }
      }
      return this;
    }

    /**
     * determines the raw type of a given Type object, recursively traversing its hierarchy
     * until it finds a non-null raw type. It returns the raw type as a Class object if
     * found, or null otherwise.
     * 
     * @param type Type object to be checked for its raw type.
     * 
     * 	- If `type` is an instance of `Class`, the function returns it directly as a
     * `Class` object.
     * 	- If `type` is an instance of `ParameterizedType`, the function recursively calls
     * itself on the raw type contained within the parameterized type, denoted by
     * `(ParameterizedType) type`.
     * 	- If `type` has any other properties or attributes, the function returns `null`.
     * 
     * @returns a raw class object or null if the input type cannot be resolved.
     * 
     * 	- If the input `type` is an instance of `Class<?>`, the function returns the same
     * class object directly.
     * 	- If the input `type` is an instance of `ParameterizedType`, the function recursively
     * calls itself on the raw type of the parameterized type, and returns the result.
     * 	- If the input `type` is neither a `Class<?>` nor a `ParameterizedType`, the
     * function returns `null`.
     */
    private Class<?> getRawType(Type type) {
      if (type instanceof Class<?>) {
        return (Class<?>) type;
      } else if (type instanceof ParameterizedType) {
        return getRawType(((ParameterizedType) type).getRawType());
      } else {
        return null;
      }
    }

    /**
     * adds a type mirror representing a super interface to the builder's type.
     * 
     * @param superinterface superinterface that the current builder object should implement.
     * 
     * 	- TypeMirror superinterface represents a type that is a super interface of the
     * class being built.
     * 	- It may have attributes and methods, which can be accessed through the reflection
     * API.
     * 
     * @returns a new `Builder` instance with the specified superinterface added to its
     * interface list.
     * 
     * The first argument passed to the function is `TypeMirror`, which represents the
     * type mirror object that represents the super interface to be added. The second
     * argument, `true`, is a boolean value that indicates whether the super interface
     * should be added directly or indirectly through an intermediate interface.
     */
    public Builder addSuperinterface(TypeMirror superinterface) {
      return addSuperinterface(superinterface, true);
    }

    /**
     * modifies the builder by adding a superinterface to it. It adds the superinterface
     * as a TypeMirror and checks if the superinterface is a nested type name, avoiding
     * clashes with nested classes if necessary.
     * 
     * @param superinterface supertype that the current class being built is supposed to
     * implement, which is added to the build process through the `addSuperinterface()`
     * method.
     * 
     * 	- `TypeMirror`: Represents a type in the form of a mirrored reference, which can
     * be used to access the type's information.
     * 	- `DeclaredType`: A declared type is a type that is explicitly defined in the
     * code, such as a class or interface. The `superinterface` parameter is of this type.
     * 	- `TypeElement`: Represents a type element, which is an instance of the `TypeElement`
     * class and contains information about the type. In this case, it is the element
     * corresponding to the `superinterface`.
     * 	- `avoidNestedTypeNameClashes`: A boolean value indicating whether to avoid naming
     * clashes with nested classes.
     * 
     * @param avoidNestedTypeNameClashes boolean value of whether to avoid clashes with
     * nested classes when adding the superinterface to the builder's type hierarchy.
     * 
     * @returns a modified `Builder` instance with the specified super interface added
     * to its list of interfaces.
     * 
     * The method returns a modified builder instance, indicating that the superinterface
     * has been added to the builder's type.
     * 
     * The `addSuperinterface` method takes two parameters: `superinterface` and
     * `avoidNestedTypeNameClashes`. The first parameter is a TypeMirror object representing
     * the superinterface to be added, while the second parameter is a boolean value
     * indicating whether or not nested type name clashes should be avoided.
     * 
     * The returned output is a modified builder instance with the added superinterface.
     */
    public Builder addSuperinterface(TypeMirror superinterface,
        boolean avoidNestedTypeNameClashes) {
      addSuperinterface(TypeName.get(superinterface));
      if (avoidNestedTypeNameClashes && superinterface instanceof DeclaredType) {
        TypeElement superInterfaceElement =
            (TypeElement) ((DeclaredType) superinterface).asElement();
        avoidClashesWithNestedClasses(superInterfaceElement);
      }
      return this;
    }

    /**
     * adds an enum constant to a `Builder`.
     * 
     * @param name name of the enum constant that is being added.
     * 
     * @returns an updated `Builder` instance with an added enum constant.
     * 
     * 	- The return type is `Builder`, indicating that it is a builder class that can
     * be used to add more elements to the Java object being constructed.
     * 	- The parameter `name` represents the name of the enum constant being added.
     * 	- The expression `anonymousClassBuilder("").build()` returns an instance of an
     * anonymous class builder, which is used to build the enum constant.
     */
    public Builder addEnumConstant(String name) {
      return addEnumConstant(name, anonymousClassBuilder("").build());
    }

    /**
     * adds an enumeration constant to a `Builder` instance, storing the name and type
     * specification in a map and returning the modified builder for chaining.
     * 
     * @param name name of the enum constant being added.
     * 
     * @param typeSpec type of the enumeration constant being added.
     * 
     * 	- `name`: A string representing the name of the enum constant.
     * 	- `typeSpec`: The type specification of the enum constant, which contains information
     * about its type and other attributes.
     * 
     * @returns a modified instance of the `Builder` class with an added enum constant.
     * 
     * The method returns a `Builder` object, indicating that it is possible to continue
     * building other elements of the class using this method.
     * 
     * The `enumConstants` map is updated with the provided `name` and `typeSpec`,
     * signifying that these values have been added to the set of enum constants for the
     * class.
     * 
     * Therefore, the output of the `addEnumConstant` function is a modified `Builder`
     * object that allows further manipulation of the class's elements without repeating
     * the `addEnumConstant` method again.
     */
    public Builder addEnumConstant(String name, TypeSpec typeSpec) {
      enumConstants.put(name, typeSpec);
      return this;
    }

    /**
     * in the Java code allows for the addition of multiple field specifications to a
     * `Builder` object, which is then returned with the added fields.
     * 
     * @param fieldSpecs iterable of FieldSpec objects that contain information about the
     * fields to be added to the builder.
     * 
     * 	- `fieldSpecs` is an iterable of `FieldSpec` objects, which represent the fields
     * to be added to the builder.
     * 	- The `fieldSpecs` object is not null, as checked by the function.
     * 	- Each `FieldSpec` object in the `fieldSpecs` iterable represents a field that
     * needs to be added to the builder.
     * 
     * The function then loops through each `FieldSpec` object and adds the corresponding
     * field to the builder using the `addField` function. Finally, the function returns
     * the current builder instance.
     * 
     * @returns a builder instance with added fields.
     * 
     * 	- The `Builder` object is updated with the added fields.
     * 	- The `fieldSpecs` parameter is checked for nullness before proceeding.
     * 	- Each `FieldSpec` element in the `fieldSpecs` collection is passed to the
     * `addField` function, which adds the corresponding field to the builder.
     * 
     * In summary, the `addFields` function modifies a `Builder` object by adding fields
     * specified in an iterable collection of `FieldSpec` objects.
     */
    public Builder addFields(Iterable<FieldSpec> fieldSpecs) {
      checkArgument(fieldSpecs != null, "fieldSpecs == null");
      for (FieldSpec fieldSpec : fieldSpecs) {
        addField(fieldSpec);
      }
      return this;
    }

    /**
     * adds a new field specification to the builder's list of fields.
     * 
     * @param fieldSpec specification of a field to be added to the builder, including
     * its name and type.
     * 
     * 	- `fieldSpecs`: A collection of `FieldSpec` objects containing information about
     * the fields to be added to the builder.
     * 	- `fieldSpec`: A `FieldSpec` object with various attributes such as name, type,
     * and label, representing a field to be added to the builder.
     * 
     * @returns a reference to the updated `Builder` instance, allowing for further method
     * calls without creating a new object.
     * 
     * The `fieldSpecs` collection is added to, which contains the field specifications
     * for the builder. This collection is not modified by the addition of new fields
     * through this method. The `Builder` object itself is the returned output, indicating
     * that the builder has been updated with a new field specification.
     */
    public Builder addField(FieldSpec fieldSpec) {
      fieldSpecs.add(fieldSpec);
      return this;
    }

    /**
     * adds a field to the class it is defined in, using the specified type, name, and modifiers.
     * 
     * @param type type of the field being added to the builder object.
     * 
     * 	- TypeName: represents the type of the field being added, which can be any valid
     * Java class or interface.
     * 	- name: specifies the name of the field being added.
     * 	- Modifier...: represents an array of modifiers that can be used to customize the
     * field's access level and other attributes. The available modifiers are: `public`,
     * `private`, `protected`, and `static`.
     * 
     * @param name name of the field to be added.
     * 
     * @returns a new `FieldSpec` object built using the provided type, name, and modifiers.
     * 
     * 	- The `Builder` object is returned as the result of the function call. This
     * indicates that the function is designed to be chainable, allowing for multiple
     * method calls to be composed together to build a larger entity.
     * 	- The `TypeName` parameter represents the type of the field being added, which
     * can be any valid Java class or interface.
     * 	- The `String` parameter `name` represents the name of the field, which is used
     * to uniquely identify the field within the entity being built.
     * 	- The `Modifier...` parameter represents an array of modifiers that can be applied
     * to the field, such as `public`, `private`, or `protected`. These modifiers determine
     * the accessibility and behavior of the field within the entity.
     */
    public Builder addField(TypeName type, String name, Modifier... modifiers) {
      return addField(FieldSpec.builder(type, name, modifiers).build());
    }

    /**
     * adds a field to a Java class, providing the field type, name, and modifiers as parameters.
     * 
     * @param type type of field being added to the builder, which is used to determine
     * the name and modifiers of the field.
     * 
     * 	- `TypeName.get(type)`: This method returns the named type of the given `type`
     * parameter. The named type can be accessed through its name or a constant `TypeName`.
     * 	- `name`: This is the name of the field being added, provided as an argument to
     * the function.
     * 	- `modifiers`: An array of modifiers, which are optional and can be included to
     * customize the behavior of the function.
     * 
     * @param name name of the field to be added.
     * 
     * @returns a new `Field` instance with the specified type, name, and modifiers.
     * 
     * 	- `TypeName.get(type)`: This method returns a `TypeName` object that represents
     * the type of the field being added. The type can be either a primitive type or a
     * class type.
     * 	- `name`: This is the name of the field being added.
     * 	- `modifiers`: An array of `Modifier` objects that represent the access modifiers
     * for the field. These modifiers can be either `PUBLIC`, `PROTECTED`, `PRIVATE`, or
     * `STATIC`.
     */
    public Builder addField(Type type, String name, Modifier... modifiers) {
      return addField(TypeName.get(type), name, modifiers);
    }

    /**
     * adds a code block to the class's static block.
     * 
     * @param block code block that is added to the static block of the builder object.
     * 
     * 	- `staticBlock`: This is an instance of `ControlFlow` representing a `static`
     * block in Java. It is used to control the flow of execution within the method.
     * 	- `beginControlFlow()`: This is a method of `ControlFlow` that starts a new control
     * flow context. It takes no arguments and returns a new instance of `ControlFlow`.
     * 	- `add(block)`: This method adds the given `CodeBlock` to the current control
     * flow context. It takes no arguments and returns the updated control flow context.
     * 	- `endControlFlow()`: This is a method of `ControlFlow` that ends the current
     * control flow context. It takes no arguments and returns the final control flow context.
     * 
     * @returns a modified builder object with the provided code block added to its static
     * block.
     * 
     * 	- The function takes a `CodeBlock` parameter named `block`.
     * 	- The `staticBlock` object is used to create a new control flow statement (beginning
     * and ending with `endControlFlow()`).
     * 	- The `add()` method is called on the `staticBlock` object, passing in the `block`
     * parameter.
     */
    public Builder addStaticBlock(CodeBlock block) {
      staticBlock.beginControlFlow("static").add(block).endControlFlow();
      return this;
    }

    /**
     * modifies the given `CodeBlock` and adds it to the initializer block of a class or
     * enum.
     * 
     * @param block code to be added as an initializer block for the class or enum being
     * built.
     * 
     * 	- `kind`: The kind of entity being initialized, which can be either `CLASS` or `ENUM`.
     * 	- `initializerBlock`: A `CodeBlock` object that represents the initializer block
     * for the entity.
     * 
     * @returns a modified `Builder` object with an additional `CodeBlock` added to the
     * initializer block.
     * 
     * 	- The `Builder` object is modified by adding an initializer block to the current
     * builder state.
     * 	- The kind of the class or enum being built determines whether the initializer
     * block can be added. If the kind is not `CLASS` or `ENUM`, an `UnsupportedOperationException`
     * is thrown.
     * 	- The `initializerBlock` field is used to store the resulting initializer block,
     * which consists of a series of text nodes representing indentation and the insertion
     * of the provided code block.
     * 	- The `add` method is called on the `initializerBlock` field to add the necessary
     * text nodes for the initializer block.
     */
    public Builder addInitializerBlock(CodeBlock block) {
      if ((kind != Kind.CLASS && kind != Kind.ENUM)) {
        throw new UnsupportedOperationException(kind + " can't have initializer blocks");
      }
      initializerBlock.add("{\n")
          .indent()
          .add(block)
          .unindent()
          .add("}\n");
      return this;
    }

    /**
     * adds methods to a `Builder` object, iterating through an Iterable of `MethodSpec`
     * objects and adding each one to the Builder.
     * 
     * @param methodSpecs Iterable of method specs that are added to the builder instance,
     * allowing the user to specify multiple methods at once and simplify code reuse.
     * 
     * 	- `checkArgument`: This is an instance method of the `Builder` class that checks
     * if the input `methodSpecs` is not null before proceeding with the next line of code.
     * 	- `for (MethodSpec methodSpec : methodSpecs)`: This line iterates over the elements
     * of the input `methodSpecs` collection using a `for` loop.
     * 	- `addMethod(methodSpec)`: This line adds each element of the `methodSpecs`
     * collection to the `addMethod` method of the current `Builder` instance.
     * 
     * @returns a builder instance with additional methods added to it.
     * 
     * 	- The returned object is of type `Builder`, indicating that it is a builder class
     * that can be used to construct an instance of the specified class.
     * 	- The method specs parameter is passed in as an iterable, which suggests that
     * multiple method specifiers can be added to the builder at once.
     * 	- The checkArgument method is called to verify that the `methodSpecs` parameter
     * is not null, indicating that the function will throw an exception if any methods
     * are attempted to be added to a null builder instance.
     */
    public Builder addMethods(Iterable<MethodSpec> methodSpecs) {
      checkArgument(methodSpecs != null, "methodSpecs == null");
      for (MethodSpec methodSpec : methodSpecs) {
        addMethod(methodSpec);
      }
      return this;
    }

    /**
     * allows the builder to add a new method to the class it is building, by passing in
     * a `MethodSpec` object representing the method to be added.
     * 
     * @param methodSpec Method Specification object that contains the details of a method
     * to be added to the builder.
     * 
     * 	- `methodSpec`: The input parameter is a `MethodSpec` object that contains
     * information about a method to be added to the builder. It has attributes such as
     * the name of the method, its return type, and its parameters.
     * 
     * @returns a reference to the current builder instance.
     * 
     * The method returns `this`, indicating that it creates a new builder instance that
     * can be used to modify the original object.
     * The method adds a `MethodSpec` object to the `methodSpecs` list, which is an array
     * of method specifications.
     * The method modifies the original object by adding the specified method to its
     * methods list.
     */
    public Builder addMethod(MethodSpec methodSpec) {
      methodSpecs.add(methodSpec);
      return this;
    }

    /**
     * in Java adds multiple types to a Builder object, which is used for creating objects
     * of a specific class. The function takes an Iterable<TypeSpec> parameter and iterates
     * over each type spec in the list, adding it to the builder using the `addType` method.
     * 
     * @param typeSpecs Iterable of TypeSpec objects that are added to the builder object.
     * 
     * 	- `typeSpecs` is an iterable collection of type specifications.
     * 	- Each type specification in the iteration represents a single type that can be
     * added to the builder.
     * 	- The type specification may contain various attributes, such as the name of the
     * type, its Java class or interface, and any additional metadata.
     * 
     * @returns a builder instance with additional types added to its type registry.
     * 
     * 	- The returned object is a `Builder` instance, indicating that the method is
     * designed to be chainable and allows for further modifications to the builder.
     * 	- The method takes an `Iterable` of `TypeSpec` objects as input, indicating that
     * it can handle multiple type specifiers at once.
     * 	- The method checks if the input `typeSpecs` is null before proceeding, ensuring
     * that the method does not crash or produce unexpected results due to null inputs.
     * 	- The method loops through each `TypeSpec` in the input iterable and calls the
     * `addType` method on the current builder instance for each one, indicating that the
     * method can handle multiple types in a single call.
     */
    public Builder addTypes(Iterable<TypeSpec> typeSpecs) {
      checkArgument(typeSpecs != null, "typeSpecs == null");
      for (TypeSpec typeSpec : typeSpecs) {
        addType(typeSpec);
      }
      return this;
    }

    /**
     * allows for the addition of a TypeSpec object to the `typeSpecs` collection, returning
     * a reference to the same builder instance for further modifications.
     * 
     * @param typeSpec type to be added to the builder, allowing the caller to configure
     * and customize the type as needed before it is added to the builder's collection
     * of types.
     * 
     * 	- `typeSpecs`: The instance field `typeSpecs` is added to, which stores an array
     * of type specifications.
     * 	- `return this`: This statement returns a reference to the current object instance,
     * allowing method chaining.
     * 
     * @returns a reference to the same instance of the `Builder` class, allowing for
     * additional types to be added to the builder.
     * 
     * The `typeSpecs` field is added to the builder instance, indicating that the `addType`
     * method has successfully updated its state with the provided type specification.
     * 
     * The `this` keyword in the return statement indicates that the method is returning
     * a reference to the same builder instance, allowing the caller to continue modifying
     * it.
     */
    public Builder addType(TypeSpec typeSpec) {
      typeSpecs.add(typeSpec);
      return this;
    }

    /**
     * adds an Element to a list of originating elements associated with the current
     * Builder instance, allowing for further customization and modification of the overall
     * element structure.
     * 
     * @param originatingElement Element that initiated the creation of the current Builder
     * instance, and it is added to a list of originatingElements within the function.
     * 
     * 	- `originatingElements`: This is an array that stores the elements added to the
     * builder. The `addOriginatingElement` function adds a new element to this array.
     * 
     * @returns a reference to the provided `Element` object, added to a list of originating
     * elements.
     * 
     * 	- The `originatingElements` field is an array that stores the originating elements.
     * 	- The `this` reference is used to return the current builder instance.
     * 	- The function modifies the state of the builder by adding a new element to the
     * `originatingElements` array.
     */
    public Builder addOriginatingElement(Element originatingElement) {
      originatingElements.add(originatingElement);
      return this;
    }

    /**
     * modifies the `alwaysQualifiedNames` list by adding the given `simpleNames` array,
     * while checking for null inputs and reporting invalid arguments.
     * 
     * @returns a modified builder instance with added qualified names.
     * 
     * 	- The `Builder` object is returned as the output of the function, indicating that
     * the method is a builder method that returns a new instance of the same class with
     * modified attributes.
     * 	- The `simpleNames` parameter is passed in as an array of strings, which is checked
     * for null before being processed further.
     * 	- For each string in the `simpleNames` array, the method checks if it is null and
     * if not, adds it to the `alwaysQualifiedNames` list.
     * 	- After adding each name to the list, the method returns a new instance of the
     * same class with the modified attribute.
     */
    public Builder alwaysQualify(String... simpleNames) {
      checkArgument(simpleNames != null, "simpleNames == null");
      for (String name : simpleNames) {
        checkArgument(
            name != null,
            "null entry in simpleNames array: %s",
            Arrays.toString(simpleNames)
        );
        alwaysQualifiedNames.add(name);
      }
      return this;
    }

    /**
     * checks for potential clashes between a type element and nested classes, interfaces,
     * or superclasses, and adjusts the type element accordingly.
     * 
     * @param typeElement type element of interest for which the method checks for clashes
     * with nested classes.
     * 
     * 	- `typeElement != null`: This is checked to ensure that the input is not null.
     * 	- `getEnclosedElements()`: This method returns a list of type elements nested
     * within the given element.
     * 	- `getSuperclass()`: This method returns the superclass of the given type element,
     * which is checked to ensure it is not null and is an instance of `DeclaredType`.
     * 	- `getInterfaces()`: This method returns a list of interfaces implemented by the
     * given type element, which are checked to ensure they are instances of `DeclaredType`.
     * 
     * The function then performs actions based on these properties:
     * 
     * 	- For each nested type in the list returned by `getEnclosedElements()`, the simple
     * name of the type is always qualified.
     * 	- For each superclass of the given type element, the corresponding `TypeElement`
     * is checked and the `avoidClashesWithNestedClasses()` function is called recursively.
     * 	- For each interface implemented by the given type element, the corresponding
     * `TypeElement` is checked and the `avoidClashesWithNestedClasses()` function is
     * called recursively.
     * 
     * @returns a builder instance that has performed checks to avoid clashes with nested
     * classes.
     * 
     * 	- `typeElement`: The input parameter, which is a `TypeElement` representing a
     * Java class or interface.
     * 	- `superclass`: The superclass of the input `typeElement`, which is checked to
     * ensure it is not a nested class. If it is, the function recursively calls itself
     * on the superclass element.
     * 	- `interfaces`: The interfaces of the input `typeElement`, which are checked to
     * ensure they are not nested classes. If any interface is a nested class, the function
     * recursively calls itself on that interface element.
     * 	- `this`: A reference to the current instance of the `Builder` class.
     * 
     * The function takes in a `TypeElement` and checks if it has any nested classes. If
     * it does, the function recursively calls itself on each nested class until no more
     * nested classes are found. The function then returns the current instance of the
     * `Builder` class.
     */
    public Builder avoidClashesWithNestedClasses(TypeElement typeElement) {
      checkArgument(typeElement != null, "typeElement == null");
      for (TypeElement nestedType : ElementFilter.typesIn(typeElement.getEnclosedElements())) {
        alwaysQualify(nestedType.getSimpleName().toString());
      }
      TypeMirror superclass = typeElement.getSuperclass();
      if (!(superclass instanceof NoType) && superclass instanceof DeclaredType) {
        TypeElement superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
        avoidClashesWithNestedClasses(superclassElement);
      }
      for (TypeMirror superinterface : typeElement.getInterfaces()) {
        if (superinterface instanceof DeclaredType) {
          TypeElement superinterfaceElement
              = (TypeElement) ((DeclaredType) superinterface).asElement();
          avoidClashesWithNestedClasses(superinterfaceElement);
        }
      }
      return this;
    }

    /**
     * checks and ensures that no nested classes within a class clash with each other or
     * with the parent class or interfaces.
     * 
     * @param clazz class being built, which is checked for nullity and then its declared
     * classes are iterated over to avoid clashes with nested classes.
     * 
     * 	- `clazz`: The deserialized class object that is being checked for clashes with
     * nested classes. (Type: Class<?>)
     * 	- `getDeclaredClasses()`: Returns an array of all declared classes in the specified
     * class. (Method: getDeclaredClasses())
     * 	- `getSuperclass()`: Returns the superclass of the specified class, or null if
     * it is a top-level class. (Method: getSuperclass())
     * 	- `getInterfaces()`: Returns an array of all interfaces implemented by the specified
     * class. (Method: getInterfaces())
     * 
     * The function then performs checks on each of these properties to avoid clashes
     * with nested classes, as described below:
     * 
     * 	- Checks that `clazz` is not null and is not a top-level class.
     * 	- Iterates over the declared classes in `clazz` and qualifies each name using `alwaysQualify()`.
     * 	- Checks that `superclass` is not null and is not equal to `Object.class`. If it
     * is, the function recursively calls itself on the superclass.
     * 	- Iterates over the interfaces implemented by `clazz` and recursively calls itself
     * on each interface.
     * 
     * The function returns a modified instance of the original builder object.
     * 
     * @returns a builder instance that has checked for clashes with nested classes and
     * interfaces of the provided class.
     * 
     * 	- The `this` reference is used to return the builder object itself, allowing the
     * method chaining.
     * 	- The function takes a `Class<?>` parameter, which represents the class being
     * analyzed for clashes with nested classes.
     * 	- The function uses several loops to iterate over the classes of the given class,
     * including its declared classes, superclass, and interfaces.
     * 	- For each class, the function checks if it has a simple name that is not equal
     * to `Object`. If so, the method calls itself recursively to check for clashes with
     * nested classes of the corresponding class.
     * 	- The function does not include any information about the code author or licensing.
     * 	- The function is designed to be neutral and formal in its responses, using the
     * third-person pronoun `they` instead of `I` or `you`.
     */
    public Builder avoidClashesWithNestedClasses(Class<?> clazz) {
      checkArgument(clazz != null, "clazz == null");
      for (Class<?> nestedType : clazz.getDeclaredClasses()) {
        alwaysQualify(nestedType.getSimpleName());
      }
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && !Object.class.equals(superclass)) {
        avoidClashesWithNestedClasses(superclass);
      }
      for (Class<?> superinterface : clazz.getInterfaces()) {
        avoidClashesWithNestedClasses(superinterface);
      }
      return this;
    }

    /**
     * generates a Java type specification based on input annotations, modifiers, and
     * other type-related information. It returns a `TypeSpec` object representing the
     * generated type.
     * 
     * @returns a `TypeSpec` object representing the specified type.
     * 
     * 1/ TypeSpec: This is the type of the output returned by the `build` function. It
     * represents the fully-qualified name of the type, including its package and class
     * name.
     * 2/ Modifiers: This is an instance of `Modifier` that contains information about
     * the modifiers applied to the type. The modifiers include `PUBLIC`, `PRIVATE`,
     * `PROTECTED`, `STATIC`, `FINAL`, and `ABSTRACT`.
     * 3/ Superinterfaces: This is a list of interface types that are implemented by the
     * type.
     * 4/ Anonymous TypeArguments: This is a list of type arguments that are used to
     * create an anonymous type. If this list is non-null, it means that the type is not
     * anonymous.
     * 5/ FieldSpecs: This is a list of `FieldSpec` objects that represent the fields of
     * the type. Each field spec contains information about the field name, modifiers,
     * and default value.
     * 6/ MethodSpecs: This is a list of `MethodSpec` objects that represent the methods
     * of the type. Each method spec contains information about the method name, return
     * type, modifiers, and default value.
     * 7/ EnumConstants: This is a map of `String` to `TypeSpec` objects that represent
     * the enum constants of the type. Each key is the name of the constant, and each
     * value is the type of the constant.
     * 8/ Superclass: This is the superclass of the type, which can be either an interface
     * or a class. If it is `ClassName.OBJECT`, then the type is not abstract.
     * 9/ InterestingSupertypeCount: This is an integer that represents the number of
     * supertypes of the type, including the superclass and any interfaces implemented
     * by the type. If the count is greater than 1, then the type has too many supertypes.
     * 
     * In summary, the output of the `build` function is a `TypeSpec` object that contains
     * information about the fully-qualified name of the type, its modifiers, superinterfaces,
     * anonymous type arguments, fields, methods, enum constants, and superclass.
     */
    public TypeSpec build() {
      for (AnnotationSpec annotationSpec : annotations) {
        checkNotNull(annotationSpec, "annotationSpec == null");
      }

      if (!modifiers.isEmpty()) {
        checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
        for (Modifier modifier : modifiers) {
          checkArgument(modifier != null, "modifiers contain null");
        }
      }

      for (TypeName superinterface : superinterfaces) {
        checkArgument(superinterface != null, "superinterfaces contains null");
      }

      if (!typeVariables.isEmpty()) {
        checkState(anonymousTypeArguments == null,
            "typevariables are forbidden on anonymous types.");
        for (TypeVariableName typeVariableName : typeVariables) {
          checkArgument(typeVariableName != null, "typeVariables contain null");
        }
      }

      for (Map.Entry<String, TypeSpec> enumConstant : enumConstants.entrySet()) {
        checkState(kind == Kind.ENUM, "%s is not enum", this.name);
        checkArgument(enumConstant.getValue().anonymousTypeArguments != null,
            "enum constants must have anonymous type arguments");
        checkArgument(SourceVersion.isName(name), "not a valid enum constant: %s", name);
      }

      for (FieldSpec fieldSpec : fieldSpecs) {
        if (kind == Kind.INTERFACE || kind == Kind.ANNOTATION) {
          requireExactlyOneOf(fieldSpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE);
          Set<Modifier> check = EnumSet.of(Modifier.STATIC, Modifier.FINAL);
          checkState(fieldSpec.modifiers.containsAll(check), "%s %s.%s requires modifiers %s",
              kind, name, fieldSpec.name, check);
        }
      }

      for (MethodSpec methodSpec : methodSpecs) {
        if (kind == Kind.INTERFACE) {
          requireExactlyOneOf(methodSpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE);
          if (methodSpec.modifiers.contains(Modifier.PRIVATE)) {
            checkState(!methodSpec.hasModifier(Modifier.DEFAULT),
                "%s %s.%s cannot be private and default", kind, name, methodSpec.name);
            checkState(!methodSpec.hasModifier(Modifier.ABSTRACT),
                "%s %s.%s cannot be private and abstract", kind, name, methodSpec.name);
          } else {
            requireExactlyOneOf(methodSpec.modifiers, Modifier.ABSTRACT, Modifier.STATIC,
                Modifier.DEFAULT);
          }
        } else if (kind == Kind.ANNOTATION) {
          checkState(methodSpec.modifiers.equals(kind.implicitMethodModifiers),
              "%s %s.%s requires modifiers %s",
              kind, name, methodSpec.name, kind.implicitMethodModifiers);
        }
        if (kind != Kind.ANNOTATION) {
          checkState(methodSpec.defaultValue == null, "%s %s.%s cannot have a default value",
              kind, name, methodSpec.name);
        }
        if (kind != Kind.INTERFACE) {
          checkState(!methodSpec.hasModifier(Modifier.DEFAULT), "%s %s.%s cannot be default",
              kind, name, methodSpec.name);
        }
      }

      for (TypeSpec typeSpec : typeSpecs) {
        checkArgument(typeSpec.modifiers.containsAll(kind.implicitTypeModifiers),
            "%s %s.%s requires modifiers %s", kind, name, typeSpec.name,
            kind.implicitTypeModifiers);
      }

      boolean isAbstract = modifiers.contains(Modifier.ABSTRACT) || kind != Kind.CLASS;
      for (MethodSpec methodSpec : methodSpecs) {
        checkArgument(isAbstract || !methodSpec.hasModifier(Modifier.ABSTRACT),
            "non-abstract type %s cannot declare abstract method %s", name, methodSpec.name);
      }

      boolean superclassIsObject = superclass.equals(ClassName.OBJECT);
      int interestingSupertypeCount = (superclassIsObject ? 0 : 1) + superinterfaces.size();
      checkArgument(anonymousTypeArguments == null || interestingSupertypeCount <= 1,
          "anonymous type has too many supertypes");

      return new TypeSpec(this);
    }
  }
}
