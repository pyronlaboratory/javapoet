/*
 * Copyright (C) 2014 Google, Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * in Java represents a fully qualified class name as a string of characters. It
 * provides methods for getting the package and simple name of a class, as well as a
 * nestedClass method for creating a new class name based on a given package and
 * simple name. The compareTo method is used for comparing the Canonical names of two
 * ClassNames, and the emit method is used for emitting the class name to a CodeWriter
 * object for generation of bytecode. Additionally, the enclosingClasses method returns
 * all enclosing classes in this outermost class, reversed.
 */
public final class ClassName extends TypeName implements Comparable<ClassName> {
  public static final ClassName OBJECT = ClassName.get(Object.class);

  /** The name representing the default Java package. */
  private static final String NO_PACKAGE = "";

  /** The package name of this class, or "" if this is in the default package. */
  final String packageName;

  /** The enclosing class, or null if this is not enclosed in another class. */
  final ClassName enclosingClassName;

  /** This class name, like "Entry" for java.util.Map.Entry. */
  final String simpleName;

  private List<String> simpleNames;

  /** The full class name like "java.util.Map.Entry". */
  final String canonicalName;

  private ClassName(String packageName, ClassName enclosingClassName, String simpleName) {
    this(packageName, enclosingClassName, simpleName, Collections.emptyList());
  }

  private ClassName(String packageName, ClassName enclosingClassName, String simpleName,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.packageName = Objects.requireNonNull(packageName, "packageName == null");
    this.enclosingClassName = enclosingClassName;
    this.simpleName = simpleName;
    this.canonicalName = enclosingClassName != null
        ? (enclosingClassName.canonicalName + '.' + simpleName)
        : (packageName.isEmpty() ? simpleName : packageName + '.' + simpleName);
  }

  /**
   * takes a list of annotations and returns a new instance of the specified class with
   * the annotations applied using concatenation.
   * 
   * @param annotations list of annotations to be concatenated and added to the simple
   * name of the class, resulting in the final class representation.
   * 
   * 	- `packageName`: The package name of the annotated class.
   * 	- `enclosingClassName`: The enclosing class name of the annotated class.
   * 	- `simpleName`: The simple name of the annotated class.
   * 	- `concatAnnotations()`: A function that concatenates the various annotations
   * specified in the input `annotations` list.
   * 
   * @returns a new instance of the specified class with the added annotations.
   * 
   * 	- `packageName`: The package name of the annotated class.
   * 	- `enclosingClassName`: The enclosing class name of the annotated class.
   * 	- `simpleName`: The simple name of the annotated class.
   * 	- `concatAnnotations(annotations)`: A list of annotations concatenated together.
   */
  @Override public ClassName annotated(List<AnnotationSpec> annotations) {
    return new ClassName(packageName, enclosingClassName, simpleName,
        concatAnnotations(annotations));
  }

  /**
   * returns a new instance of the `ClassName` class, taking into account the enclosing
   * class and inner class name, and recursively calling itself to handle the enclosing
   * class.
   * 
   * @returns a new instance of `ClassName` with the same package name and simple name
   * as the original, along with the enclosing class name if it is provided.
   * 
   * 	- `packageName`: The package name of the class being processed.
   * 	- `enclosingClassName`: The enclosing class name of the class being processed,
   * or null if there is no enclosing class.
   * 	- `simpleName`: The simple name of the class being processed.
   */
  @Override public ClassName withoutAnnotations() {
    if (!isAnnotated()) return this;
    ClassName resultEnclosingClassName = enclosingClassName != null
        ? enclosingClassName.withoutAnnotations()
        : null;
    return new ClassName(packageName, resultEnclosingClassName, simpleName);
  }

  /**
   * checks if a class or method is annotated by evaluating the annotations of its
   * superclass and the enclosing class, if any.
   * 
   * @returns a boolean value indicating whether the class or any of its enclosed classes
   * are annotated with a specific annotation.
   * 
   * 	- `super.isAnnotated()`: This property checks whether the immediate superclass
   * is annotated. If it is, then the current class is also annotated.
   * 	- `enclosingClassName != null`: This property indicates whether the current class
   * has an enclosing class that is annotated. If the enclosing class is annotated,
   * then the current class is also annotated.
   * 
   * Therefore, the output of the `isAnnotated` function depends on both the superclass
   * and the enclosing class annotations.
   */
  @Override public boolean isAnnotated() {
    return super.isAnnotated() || (enclosingClassName != null && enclosingClassName.isAnnotated());
  }

  /**
   * returns the package name of a class or package.
   * 
   * @returns a string representing the package name of the Java class being analyzed.
   */
  public String packageName() {
    return packageName;
  }

  /**
   * returns the enclosing class name of the current class.
   * 
   * @returns the value of the `enclosingClassName` field.
   * 
   * The `enclosingClassName` function returns the enclosing class name of the current
   * class. This means that if the current class is nested within another class, the
   * returned value will be the name of the outer class.
   * 
   * The output is a string representing the name of the enclosing class. It may contain
   * the package name and class name separated by a dot (.), such as `com.example.package.ClassName`.
   * 
   * The length of the output string can vary depending on the structure of the class
   * hierarchy.
   */
  public ClassName enclosingClassName() {
    return enclosingClassName;
  }

  /**
   * returns the top-level class of the enclosing class, or itself if no enclosing class
   * is present.
   * 
   * @returns the top-level class name of the enclosing class or the current class
   * itself if there is no enclosing class.
   * 
   * The return value is an instance of the `ClassName` class, indicating that it is a
   * top-level class in the Java hierarchy.
   * If `enclosingClassName` is not null, the returned object is a nested class within
   * another top-level class, otherwise it is a standalone top-level class.
   * The `this` keyword is used to return an instance of the same class, indicating
   * that the function is calling itself recursively until it reaches the root class
   * of the hierarchy.
   */
  public ClassName topLevelClassName() {
    return enclosingClassName != null ? enclosingClassName.topLevelClassName() : this;
  }

  /**
   * calculates a fully qualified name for a Java class by combining the simple name,
   * enclosing class name (if present), and package name (if applicable). The resulting
   * name is a string representing the complete identity of the class.
   * 
   * @returns a string representing the class name with its package and simple name
   * separated by a dollar sign or a period, depending on the context.
   */
  public String reflectionName() {
    return enclosingClassName != null
        ? (enclosingClassName.reflectionName() + '$' + simpleName)
        : (packageName.isEmpty() ? simpleName : packageName + '.' + simpleName);
  }

  /**
   * retrieves a list of simple names for a given class, either using an existing list
   * or creating a new one by combining the simple name of the enclosing class and the
   * simple name of the current class.
   * 
   * @returns a list of strings containing the names of classes and methods enclosed
   * by the function.
   * 
   * 1/ The list `simpleNames` is a mutable list that stores strings representing simple
   * names for classes, interfaces, and fields in the current scope.
   * 2/ If `simpleNames` is not null, it returns the list directly.
   * 3/ If `enclosingClassName` is null, the list is initialized with only the single
   * string `simpleName`.
   * 4/ Otherwise, the list is initialized by concatenating the simple names of all
   * classes, interfaces, and fields in the current scope from the enclosing class,
   * followed by the `simpleName`.
   * 5/ The list returned is an unmodifiable list, which means it cannot be modified
   * once created.
   */
  public List<String> simpleNames() {
    if (simpleNames != null) {
      return simpleNames;
    }

    if (enclosingClassName == null) {
      simpleNames = Collections.singletonList(simpleName);
    } else {
      List<String> mutableNames = new ArrayList<>();
      mutableNames.addAll(enclosingClassName().simpleNames());
      mutableNames.add(simpleName);
      simpleNames = Collections.unmodifiableList(mutableNames);
    }
    return simpleNames;
  }

  /**
   * creates a new instance of a class by combining its package name, enclosing class
   * name, and given name.
   * 
   * @param name name of the class to be generated.
   * 
   * @returns a new instance of the `ClassName` class with the provided `name`.
   * 
   * 	- `String name`: The name of the class being created.
   * 	- `packageName`: The package in which the class is defined.
   * 	- `enclosingClassName`: The name of the enclosing class that contains the `peerClass`
   * function.
   * 	- `return type`: The type of the returned object, which is a new instance of the
   * class defined in the `packageName`.
   */
  public ClassName peerClass(String name) {
    return new ClassName(packageName, enclosingClassName, name);
  }

  /**
   * creates a new instance of the `ClassName` class with a given `name`, using the
   * current class as the `enclosingClass`, and the package name of the current class
   * as the `packageName`.
   * 
   * @param name name of the object being created and is used to determine the final
   * class name of the new object.
   * 
   * @returns a new instance of the `ClassName` class with the specified `name`.
   * 
   * 	- `String name`: The given name passed as an argument to the function.
   * 	- `ClassName`: The class that is being nested, which is created by combining the
   * `packageName`, `this`, and `name` arguments using the `new` operator.
   */
  public ClassName nestedClass(String name) {
    return new ClassName(packageName, this, name);
  }

  /**
   * returns a string value representing the simple name of the class or interface,
   * without any package information.
   * 
   * @returns a string representing the simple name of an object.
   */
  public String simpleName() {
    return simpleName;
  }

  /**
   * returns a string representing the canonical name of the object.
   * 
   * @returns a string representing the canonical name of the object.
   */
  public String canonicalName() {
    return canonicalName;
  }

  /**
   * takes a `Class<?>` parameter and returns a `ClassName` object representing the
   * class. It checks for null, primitive types, arrays, and anonymous classes before
   * constructing the name using the class's simple name and any nested annotations.
   * Finally, it checks for unreliable package information and returns a nested class
   * if necessary.
   * 
   * @param clazz Class object to be transformed into a ClassName object, and various
   * checks are performed on it before returning the resulting ClassName object.
   * 
   * 	- `clazz` is not null, meaning it has been provided as a non-null argument.
   * 	- `clazz` is not a primitive type, to ensure that only classes can be represented
   * as a `ClassName`.
   * 	- `clazz` is not the `void` type, to allow for proper classification of classes.
   * 	- `clazz` is not an array type, to avoid confusion when dealing with nested classes.
   * 	- `clazz` has an anonymous suffix, which is calculated recursively until the
   * enclosing class is found or the input `clazz` is an anonymous class. The anonymous
   * suffix is used to construct the final `ClassName`.
   * 	- If `clazz` has no enclosing class, a package name is calculated based on the
   * last dot in its name, or `NO_PACKAGE` if there are no dots.
   * 
   * These explanations provide insight into the properties of `clazz` and guide the
   * creation of the resulting `ClassName`.
   * 
   * @returns a `ClassName` object representing the given class.
   * 
   * 	- The `ClassName` object is created using the `Package.getName()` method to get
   * the package name of the class, and the `null` argument represents the absence of
   * a package name.
   * 	- In case the enclosing class is not null, the `getEnclosingClass()` method is
   * used to retrieve it, and the resulting `ClassName` object is nested with the given
   * name.
   * 	- The `checkNotNull()` method is used to ensure that the input `clazz` parameter
   * is not null before proceeding with the rest of the logic.
   * 	- The `checkArgument()` methods are used to check for specific conditions on the
   * input `clazz` parameter, such as it not being a primitive type or an array type.
   * If any of these conditions are true, a corresponding error message is thrown.
   * 
   * The output of the `get` function depends on the input provided. If the input `clazz`
   * is non-null and not an anonymous class, the output will be a `ClassName` object
   * representing the enclosing class with the given name. Otherwise, the output will
   * be a `ClassName` object representing the anonymous class with the given name.
   */
  public static ClassName get(Class<?> clazz) {
    checkNotNull(clazz, "clazz == null");
    checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
    checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
    checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");

    String anonymousSuffix = "";
    while (clazz.isAnonymousClass()) {
      int lastDollar = clazz.getName().lastIndexOf('$');
      anonymousSuffix = clazz.getName().substring(lastDollar) + anonymousSuffix;
      clazz = clazz.getEnclosingClass();
    }
    String name = clazz.getSimpleName() + anonymousSuffix;

    if (clazz.getEnclosingClass() == null) {
      // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
      int lastDot = clazz.getName().lastIndexOf('.');
      String packageName = (lastDot != -1) ? clazz.getName().substring(0, lastDot) : NO_PACKAGE;
      return new ClassName(packageName, null, name);
    }

    return ClassName.get(clazz.getEnclosingClass()).nestedClass(name);
  }

  /**
   * takes a String argument representing a class name and returns an instance of the
   * best-guessed class based on its package and simple name components.
   * 
   * @param classNameString complete or partial name of a Java class, which is then
   * used to generate a best guess for the class's package and simple name.
   * 
   * @returns a `ClassName` object representing the best guess for the given class name.
   * 
   * 	- `ClassName`: This is the class name that was guessed based on the input string.
   * 	- `packageName`: This is the package name associated with the guessed class name.
   * 	- `className`: This is the complete class name, including the package name and
   * simple name.
   * 	- `simpleName`: This is the simple name of the class, without the package name.
   */
  public static ClassName bestGuess(String classNameString) {
    // Add the package name, like "java.util.concurrent", or "" for no package.
    int p = 0;
    while (p < classNameString.length() && Character.isLowerCase(classNameString.codePointAt(p))) {
      p = classNameString.indexOf('.', p) + 1;
      checkArgument(p != 0, "couldn't make a guess for %s", classNameString);
    }
    String packageName = p == 0 ? NO_PACKAGE : classNameString.substring(0, p - 1);

    // Add class names like "Map" and "Entry".
    ClassName className = null;
    for (String simpleName : classNameString.substring(p).split("\\.", -1)) {
      checkArgument(!simpleName.isEmpty() && Character.isUpperCase(simpleName.codePointAt(0)),
          "couldn't make a guess for %s", classNameString);
      className = new ClassName(packageName, className, simpleName);
    }

    return className;
  }

  /**
   * creates a new instance of a `ClassName` object and recursively adds nested classes
   * to it based on a list of simple names provided as arguments. The resulting class
   * name is returned.
   * 
   * @param packageName package name of the class being created.
   * 
   * @param simpleName name of a nested class within which the `ClassName` is being constructed.
   * 
   * @returns a `ClassName` object representing the nested class hierarchy.
   * 
   * 	- The `ClassName` object represents a class in the Java programming language.
   * 	- The `packageName` parameter specifies the package name of the class.
   * 	- The `simpleName` parameter specifies the simple name of the class, which is the
   * name of the class without any qualifiers or suffixes.
   * 	- The `simpleNames` parameters are the names of the nested classes, which are
   * added to the class using the `nestedClass()` method.
   * 
   * The output returned by the `get` function is a `ClassName` object that represents
   * the class with the specified package name and simple name, along with any nested
   * classes defined in the `simpleNames` parameters.
   */
  public static ClassName get(String packageName, String simpleName, String... simpleNames) {
    ClassName className = new ClassName(packageName, null, simpleName);
    for (String name : simpleNames) {
      className = className.nestedClass(name);
    }
    return className;
  }

  /**
   * takes a `TypeElement` parameter and returns a `ClassName` object representing the
   * fully qualified name of the enclosing class, nested inside another class with the
   * same simple name.
   * 
   * @param element TypeElement to be analyzed and its nullness is checked before
   * proceeding with the analysis.
   * 
   * 	- `checkNotNull`: It checks that the provided `element` is not null before
   * proceeding with the code.
   * 	- `simpleName`: It represents the simple name of the `TypeElement`.
   * 	- `enclosingElement`: It represents the enclosing element of the `element`, which
   * could be a package or another type.
   * 	- `accept`: It is an method that takes a visitor object and applies its operations
   * to the deserialized input `element`. In this case, it visits each element in the
   * tree structure of the `TypeElement` using the `SimpleElementVisitor8` class.
   * 
   * @returns a ClassName object representing the nested class of the given type element.
   * 
   * 1/ The first argument is a `TypeElement` object, which represents the type element
   * that the method is called on.
   * 2/ The second argument is a `String` object, which contains the simple name of the
   * type element.
   * 3/ The `visit*` methods are used to recursively visit the type element's ancestors
   * and generate the nested class name.
   * 4/ The `defaultAction` method is used to handle unexpected types that cannot be
   * processed by the other methods.
   * 
   * In summary, the `get` function takes a `TypeElement` object as input and generates
   * a nested class name based on the type element's ancestry and simple name.
   */
  public static ClassName get(TypeElement element) {
    checkNotNull(element, "element == null");
    String simpleName = element.getSimpleName().toString();

    return element.getEnclosingElement().accept(new SimpleElementVisitor8<ClassName, Void>() {
      /**
       * generates a new class name by combining the qualified name of the package element
       * and its simple name.
       * 
       * @param packageElement package element being visited and is used to construct the
       * fully qualified name of the class being returned by the function.
       * 
       * 	- `packageElement`: This represents an instance of the `PackageElement` class,
       * which contains information about a package in Java. The qualified name of the
       * package is stored in the `qualifiedName` attribute, and the simple name of the
       * package is stored in the `simpleName` attribute.
       * 	- `p`: This is a Void parameter passed to the function as an argument. It has no
       * relevance to the function's operation.
       * 
       * @param p Void value that is passed to the visitor method as an argument when
       * visiting a PackageElement object.
       * 
       * 	- `p` is a Void object that represents the package element being visited.
       * 	- `packageElement` is an object representing the package element being analyzed,
       * which contains information about the package's qualified name and simple name.
       * 	- `simpleName` is a string representing the simple name of the package, which is
       * the name of the package without any prefixes or suffixes.
       * 
       * @returns a new class name generated from the qualified package name of the element
       * being visited.
       * 
       * 	- `ClassName`: This is the class name that is generated by combining the qualified
       * name of the package element with the simple name of the class.
       * 	- `qualifiedName`: This is the qualified name of the package element, which
       * includes the package name and the simple name of the class.
       * 	- `simpleName`: This is the simple name of the class, which is used to generate
       * the class name.
       */
      @Override public ClassName visitPackage(PackageElement packageElement, Void p) {
        return new ClassName(packageElement.getQualifiedName().toString(), null, simpleName);
      }

      /**
       * takes a `TypeElement` and a `Void` parameter, and returns a `ClassName` object
       * representing a nested class within the enclosing class.
       * 
       * @param enclosingClass enclosing class of the nested class being visited, which is
       * used to determine the nested class name.
       * 
       * 	- `enclosingClass`: The enclosing class for which the type element is being visited.
       * 	- `simpleName`: The simple name of the nested class.
       * 
       * @param p Void value passed to the visitor pattern method.
       * 
       * 	- `enclosingClass`: The TypeElement representing the class that contains the
       * nested class being visited.
       * 	- `simpleName`: The simple name of the nested class being visited.
       * 
       * These two properties provide information about the context in which the function
       * is being called and the specific class being visited.
       * 
       * @returns a nested class name based on the enclosed class and simple name.
       * 
       * 	- `ClassName`: This represents the class name of the nested class, which is
       * obtained by concatenating the enclosing class's simple name with the nested class
       * name.
       * 	- `enclosingClass`: This represents the enclosing class for which the nested class
       * is being generated, which is provided as a parameter in the function.
       * 	- `simpleName`: This represents the simple name of the enclosing class, which is
       * used to construct the nested class name.
       */
      @Override public ClassName visitType(TypeElement enclosingClass, Void p) {
        return ClassName.get(enclosingClass).nestedClass(simpleName);
      }

      /**
       * generates a string representation of an unknown element by returning the string "".
       * 
       * @param unknown element being visited and returns its simple name.
       * 
       * 	- The `Element unknown` is the root element of the JSON object being deserialized.
       * 	- The `Void p` parameter is an optional void value that can be used to pass
       * additional data or configuration to the visit method.
       * 	- The function returns a string value, specifically the simple name of the class
       * representing the deserialized input.
       * 
       * @param p Void value passed as an argument to the visitor method.
       * 
       * 	- `p`: This is a Void parameter passed to the method as an argument.
       * 	- `simpleName`: This is a String property of `p`, representing the simple name
       * of the element being deserialized.
       * 
       * Therefore, the return value of the function is determined by the value of `simpleName`.
       * 
       * @returns a reference to the `ClassName` object of the type `""` (an empty string).
       * 
       * 	- `ClassName`: This represents the return type of the function, which is the name
       * of a class in this case.
       * 	- `simpleName`: This represents the simple name of the class, which is the
       * unqualified name of the class without any packages or interfaces.
       */
      @Override public ClassName visitUnknown(Element unknown, Void p) {
        return get("", simpleName);
      }

      /**
       * throws an `IllegalArgumentException` when the type of a nested element is unexpected.
       * 
       * @param enclosingElement enclosing element of the `Element` object passed to the
       * function, providing context for the error message generated by the function.
       * 
       * 	- Type: `ClassName` representing the class of the element enclosure
       * 	- Element: The actual element object that was deserialized and passed as an
       * argument to the function.
       * 	- P: A Void parameter indicating the presence of a Void value in the function call.
       * 
       * @param p Void value that is passed to the default action of the `ClassName`.
       * 
       * The `Void` parameter `p` represents an uninitialized value that is passed as a
       * reference to the function.
       * 
       * The function throws an `IllegalArgumentException` with the message "Unexpected
       * type nesting: " + `element`, indicating that the expected input type was not provided.
       * 
       * @returns an `IllegalArgumentException` with a message indicating unexpected type
       * nesting.
       * 
       * The function throws an `IllegalArgumentException` with the message "Unexpected
       * type nesting: <element>" whenever it encounters an unexpected type nesting in the
       * enclosing element.
       * The `p` parameter is a Void reference passed as an argument to the function, which
       * has no significance or use within the function's implementation.
       */
      @Override public ClassName defaultAction(Element enclosingElement, Void p) {
        throw new IllegalArgumentException("Unexpected type nesting: " + element);
      }
    }, null);
  }

  /**
   * compares two objects based on their canonical names, returning a negative value
   * if the first object has a shorter name, a positive value if the second object has
   * a shorter name, and zero if the names are the same length.
   * 
   * @param o object being compared to the current object, and is used for comparing
   * the canonical names of the two objects.
   * 
   * 	- `o` is an object of type `ClassName`, indicating that the function compares two
   * objects of different classes.
   * 	- `canonicalName` is a field or method of `o` that returns a string representing
   * the fully qualified name of the class to which `o` belongs.
   * 	- The `compareTo` function compares the strings returned by `canonicalName` of
   * `o` and the input object, and returns an integer value indicating the result of
   * the comparison.
   * 
   * @returns a positive, negative or zero value indicating the relative naming
   * compatibility between two class names.
   */
  @Override public int compareTo(ClassName o) {
    return canonicalName.compareTo(o.canonicalName);
  }

  /**
   * generates a string representation of a Java class hierarchy, emitting each enclosing
   * class and its simple name followed by an indentation level determined by its
   * distance from the root class. It also handles annotation emission for annotated classes.
   * 
   * @param out code writer that generates the Java source code for the enclosing classes
   * and their annotations, and emits them according to the current indentation level.
   * 
   * 	- `out` is an instance of `CodeWriter`, which represents the output stream where
   * the code will be written.
   * 	- `charsEmitted` is a boolean variable that keeps track of whether any characters
   * have been emitted so far. This variable is used to determine when to emit spaces
   * and annotations.
   * 	- `enclosingClasses()` returns an array of `ClassName` objects, representing the
   * enclosing classes of the current class.
   * 	- `simpleName` is a string variable that represents the simple name of the current
   * class or interface, without any package information.
   * 	- `qualifiedName` is a string variable that represents the fully qualified name
   * of the current class or interface, including the package information.
   * 	- `dot` is an integer variable that represents the index of the last character
   * in the `qualifiedName` that separates the package and class/interface names. This
   * variable is used to determine when to emit spaces and annotations.
   * 	- `out.emitAndIndent()` emits a space and indents the output stream by one level.
   * 	- `out.emit()` emits a character to the output stream.
   * 	- `out.emitAnnotations()` emits any annotations present on the current class or
   * interface.
   * 
   * @returns a written representation of the Java code, consisting of class names and
   * annotations.
   * 
   * 	- `charsEmitted`: A boolean variable that keeps track of whether any characters
   * have been emitted so far. It is set to `true` when the first enclosing class is
   * encountered and `false` otherwise.
   * 	- `simpleName`: The simple name of the current enclosing class, which is obtained
   * by stripping off the package name if any.
   * 	- `qualifiedName`: The fully qualified name of the current enclosing class, which
   * includes the package name followed by the class name.
   * 	- `dot`: The index of the last occurrence of `.` in the `qualifiedName`, which
   * represents the position of the enclosing class in the hierarchy.
   * 
   * The function returns an `CodeWriter` object, which is used to emit the source code
   * of the current class and its enclosing classes.
   */
  @Override CodeWriter emit(CodeWriter out) throws IOException {
    boolean charsEmitted = false;
    for (ClassName className : enclosingClasses()) {
      String simpleName;
      if (charsEmitted) {
        // We've already emitted an enclosing class. Emit as we go.
        out.emit(".");
        simpleName = className.simpleName;

      } else if (className.isAnnotated() || className == this) {
        // We encountered the first enclosing class that must be emitted.
        String qualifiedName = out.lookupName(className);
        int dot = qualifiedName.lastIndexOf('.');
        if (dot != -1) {
          out.emitAndIndent(qualifiedName.substring(0, dot + 1));
          simpleName = qualifiedName.substring(dot + 1);
          charsEmitted = true;
        } else {
          simpleName = qualifiedName;
        }

      } else {
        // Don't emit this enclosing type. Keep going so we can be more precise.
        continue;
      }

      if (className.isAnnotated()) {
        if (charsEmitted) out.emit(" ");
        className.emitAnnotations(out);
      }

      out.emit(simpleName);
      charsEmitted = true;
    }

    return out;
  }

  /**
   * retrieves a list of classes enclosed by a given class, recursively traversing the
   * class hierarchy until the base class is reached. The resulting list is reversed
   * and returned.
   * 
   * @returns a list of classes that are enclosed within the class being processed, in
   * reverse order.
   * 
   * 	- The result list contains all classes that are enclosed by the current class,
   * in a recursive manner.
   * 	- Each element in the list is a subclass of the current class.
   * 	- The list is reversed using the `Collections.reverse()` method before returning
   * it.
   * 	- The returned list can be used to traverse the hierarchy of classes and retrieve
   * information about their enclosing classes.
   */
  private List<ClassName> enclosingClasses() {
    List<ClassName> result = new ArrayList<>();
    for (ClassName c = this; c != null; c = c.enclosingClassName) {
      result.add(c);
    }
    Collections.reverse(result);
    return result;
  }
}
