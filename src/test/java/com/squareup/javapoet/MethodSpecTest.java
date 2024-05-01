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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationRule;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.squareup.javapoet.TestUtil.findFirst;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.junit.Assert.fail;

/**
 * is a test class for testing the MethodSpec class. It provides various methods to
 * test the different features of the MethodSpec class, such as parameter annotations,
 * method annotations, and control flow statements. The tests cover various scenarios,
 * including empty or missing parameters, multiple parameter annotations, and control
 * flow statements with named code blocks.
 */
public final class MethodSpecTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private Elements elements;
  private Types types;

  /**
   * prepares the compilation's elements and types for use by assigning them to instance
   * variables `elements` and `types`.
   */
  @Before public void setUp() {
    elements = compilation.getElements();
    types = compilation.getTypes();
  }

  /**
   * retrieves a `TypeElement` object representing a class or interface, given its
   * canonical name.
   * 
   * @param clazz Class object to retrieve the TypeElement for.
   * 
   * 	- `clazz`: A `Class<?>` object representing the Java class to retrieve an element
   * for.
   * 	- `elements`: A Map containing information about types and their corresponding
   * Elements. The key of the map is the fully qualified name of the type, while the
   * value is an `Element` object representing the type in the current module.
   * 
   * @returns a `TypeElement` object representing the class of the given type.
   * 
   * 	- The output is a `TypeElement` object representing a class or interface type in
   * the Java programming language.
   * 	- The object is obtained by invoking the `getTypeElement` method of the `elements`
   * map, passing the canonical name of the class or interface as a parameter.
   * 	- The `TypeElement` object provides access to various attributes and methods
   * related to the type, such as its name, fully qualified name, and a reference to
   * its enclosing element (e.g., a package).
   */
  private TypeElement getElement(Class<?> clazz) {
    return elements.getTypeElement(clazz.getCanonicalName());
  }

  /**
   * tests whether attempting to add annotations to a method using
   * `MethodSpec.methodBuilder().addAnnotations(null)` throws an `IllegalArgumentException`.
   */
  @Test public void nullAnnotationsAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addAnnotations(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("annotationSpecs == null");
    }
  }

  /**
   * tests whether adding a null type variable to a `MethodSpec` throws an `IllegalArgumentException`.
   */
  @Test public void nullTypeVariablesAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addTypeVariables(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("typeVariables == null");
    }
  }

  /**
   * tests whether an attempt to add null parameters to a MethodSpec results in an `IllegalArgumentException`.
   */
  @Test public void nullParametersAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addParameters(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("parameterSpecs == null");
    }
  }

  /**
   * tests whether an exception is thrown when adding null exceptions to a method builder.
   */
  @Test public void nullExceptionsAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addExceptions(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("exceptions == null");
    }
  }

  @Target(ElementType.PARAMETER)
  @interface Nullable {
  }

  /**
   * is an abstract class that provides a central method for performing various actions,
   * including running and closing things, using a generic type parameter. The method
   * is marked as deprecated and takes two parameters: a nullable string and a list of
   * types extending a specific interface (Runnable & Closeable).
   */
  abstract static class Everything {
    @Deprecated protected abstract <T extends Runnable & Closeable> Runnable everything(
        @Nullable String thing, List<? extends T> things) throws IOException, SecurityException;
  }

  /**
   * is an abstract class that provides a method `run` that takes a parameter of type
   * `R` and returns a value of type `R`. The method throws a runtime exception of type
   * `V` if any errors occur during execution.
   */
  abstract static class Generics {
    /**
     * returns a `null` value when executed.
     * 
     * @param param 1st class type `R` and is used in the function's return statement
     * without any further processing or manipulation.
     * 
     * 	- Type: `R`, indicating that the input is of type `R`.
     * 	- Extends `Throwable`, indicating that `R` may potentially extend a class that
     * implements `Throwable`.
     * 	- Returns null, suggesting that the function does not return any value.
     * 
     * @returns `null`.
     * 
     * 	- The returned value is null.
     * 	- This indicates that no exception was thrown during the execution of the function.
     * 	- The function does not handle any exceptions, therefore any exception thrown
     * will be propagated to the calling code.
     */
    <T, R, V extends Throwable> T run(R param) throws V {
      return null;
    }
  }

  /**
   * is an abstract class that provides an implementation of the toString() method,
   * which is a standard method in Java for converting an object to a string representation.
   * The class serves as a base class for other classes to inherit from and provide
   * their own implementation of the toString() method.
   */
  abstract static class HasAnnotation {
    @Override public abstract String toString();
  }

  /**
   * defines a method fail() that throws a subclass of RuntimeException (R).
   */
  interface Throws<R extends RuntimeException> {
    void fail() throws R;
  }

  /**
   * extends other interfaces, including Callable<Integer>, Comparable<ExtendsOthers>,
   * and Throws<IllegalStateException>.
   */
  interface ExtendsOthers extends Callable<Integer>, Comparable<ExtendsOthers>,
      Throws<IllegalStateException> {
  }

  /**
   * extends the Iterable interface and provides some additional default methods for
   * performing common operations on an iterable object, such as iterating over its elements.
   */
  interface ExtendsIterableWithDefaultMethods extends Iterable<Object> {
  }

  /**
   * is a simple class with a single method, `method()`. It does not have any fields
   * or other methods. The class is marked as `final`, which means that it cannot be
   * subclassed or modified in any way.
   */
  final class FinalClass {
    /**
     * does not have any defined functionality as it only contains a single line of empty
     * code: `{ }`.
     */
    void method() {
    }
  }

  /**
   * is an abstract class with several methods that can be overridden. These methods
   * include "finalMethod()", "privateMethod()", and "staticMethod()". The class provides
   * a way to test the behavior of these methods when they are overridden in subclasses.
   */
  abstract static class InvalidOverrideMethods {
    /**
     * has no functionality as it is empty and does not contain any statements.
     */
    final void finalMethod() {
    }

    /**
     * has no discernible purpose or effect due to its complete lack of content or action.
     */
    private void privateMethod() {
    }

    /**
     * does not perform any explicit operation or have any visible effect. It is merely
     * a placeholder method with no functionality.
     */
    static void staticMethod() {
    }
  }

  /**
   * overrides a method with the same name and signature as the parent class, but
   * modifies its implementation to throw additional exceptions.
   */
  @Test public void overrideEverything() {
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    MethodSpec method = MethodSpec.overriding(methodElement).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "protected <T extends java.lang.Runnable & java.io.Closeable> java.lang.Runnable "
        + "everything(\n"
        + "    java.lang.String arg0, java.util.List<? extends T> arg1) throws java.io.IOException,\n"
        + "    java.lang.SecurityException {\n"
        + "}\n");
  }

  /**
   * overrides a method with the same name and signature as the original method, but
   * with a new implementation that returns `null`.
   */
  @Test public void overrideGenerics() {
    TypeElement classElement = getElement(Generics.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    MethodSpec method = MethodSpec.overriding(methodElement)
        .addStatement("return null")
        .build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "<T, R, V extends java.lang.Throwable> T run(R param) throws V {\n"
        + "  return null;\n"
        + "}\n");
  }

  /**
   * verifies that overridding a method with the `@Java Lang Override` annotation does
   * not copy the annotation.
   */
  @Test public void overrideDoesNotCopyOverrideAnnotation() {
    TypeElement classElement = getElement(HasAnnotation.class);
    ExecutableElement exec = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    MethodSpec method = MethodSpec.overriding(exec).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {\n"
        + "}\n");
  }

  /**
   * checks if an override method's modifiers are not copied from its parent class.
   */
  @Test public void overrideDoesNotCopyDefaultModifier() {
    TypeElement classElement = getElement(ExtendsIterableWithDefaultMethods.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    ExecutableElement exec = findFirst(methods, "spliterator");
    MethodSpec method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.util.Spliterator<java.lang.Object> spliterator() {\n"
        + "}\n");
  }

  /**
   * tests the override and implementation of methods from superclass `ExtendsOthers`
   * with actual type parameters.
   */
  @Test public void overrideExtendsOthersWorksWithActualTypeParameters() {
    TypeElement classElement = getElement(ExtendsOthers.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    ExecutableElement exec = findFirst(methods, "call");
    MethodSpec method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.Integer call() throws java.lang.Exception {\n"
        + "}\n");
    exec = findFirst(methods, "compareTo");
    method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public int compareTo(" + ExtendsOthers.class.getCanonicalName() + " arg0) {\n"
        + "}\n");
    exec = findFirst(methods, "fail");
    method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public void fail() throws java.lang.IllegalStateException {\n"
        + "}\n");
  }

  /**
   * attempts to override a method on a final class using `MethodSpec.overriding()`.
   * It fails with an exception indicating that methods cannot be overridden on final
   * classes.
   */
  @Test public void overrideFinalClassMethod() {
    TypeElement classElement = getElement(FinalClass.class);
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    try {
      MethodSpec.overriding(findFirst(methods, "method"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo(
          "Cannot override method on final class com.squareup.javapoet.MethodSpecTest.FinalClass");
    }
  }

  /**
   * tests whether attempting to override methods with final, private, or static modifiers
   * results in an IllegalArgumentException.
   */
  @Test public void overrideInvalidModifiers() {
    TypeElement classElement = getElement(InvalidOverrideMethods.class);
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    try {
      MethodSpec.overriding(findFirst(methods, "finalMethod"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("cannot override method with modifiers: [final]");
    }
    try {
      MethodSpec.overriding(findFirst(methods, "privateMethod"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("cannot override method with modifiers: [private]");
    }
    try {
      MethodSpec.overriding(findFirst(methods, "staticMethod"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("cannot override method with modifiers: [static]");
    }
  }

  /**
   * is an abstract class that provides an interface for annotating methods with a
   * private annotation. The class has a single abstract method called foo, which takes
   * a string parameter and is annotated with the PrivateAnnotation interface.
   */
  abstract static class AbstractClassWithPrivateAnnotation {

    private @interface PrivateAnnotation{ }

    abstract void foo(@PrivateAnnotation final String bar);
  }

  /**
   * tests whether the `@Override` annotation copies the parameter annotations of the
   * overridden method to the new implementation method. It does not.
   */
  @Test public void overrideDoesNotCopyParameterAnnotations() {
    TypeElement abstractTypeElement = getElement(AbstractClassWithPrivateAnnotation.class);
    ExecutableElement fooElement = ElementFilter.methodsIn(abstractTypeElement.getEnclosedElements()).get(0);
    ClassName implClassName = ClassName.get("com.squareup.javapoet", "Impl");
    TypeSpec type = TypeSpec.classBuilder(implClassName)
            .superclass(abstractTypeElement.asType())
            .addMethod(MethodSpec.overriding(fooElement).build())
            .build();
    JavaFileObject jfo = JavaFile.builder(implClassName.packageName, type).build().toJavaFileObject();
    Compilation compilation = javac().compile(jfo);
    assertThat(compilation).succeeded();
  }

  /**
   * tests whether two MethodSpecs are equal and have the same hash code by creating
   * different instances and comparing them.
   */
  @Test public void equalsAndHashCode() {
    MethodSpec a = MethodSpec.constructorBuilder().build();
    MethodSpec b = MethodSpec.constructorBuilder().build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = MethodSpec.methodBuilder("taco").build();
    b = MethodSpec.methodBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    a = MethodSpec.overriding(methodElement).build();
    b = MethodSpec.overriding(methodElement).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  /**
   * gets the best Taco.
   */
  @Test public void withoutParameterJavaDoc() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("getTaco")
        .addModifiers(Modifier.PRIVATE)
        .addParameter(TypeName.DOUBLE, "money")
        .addJavadoc("Gets the best Taco\n")
        .build();
    assertThat(methodSpec.toString()).isEqualTo(""
        + "/**\n"
        + " * Gets the best Taco\n"
        + " */\n"
        + "private void getTaco(double money) {\n"
        + "}\n");
  }

  /**
   * takes two parameters - `money` and `count`, and returns a method signature with
   * JavaDocs that describe its functionality.
   */
  @Test public void withParameterJavaDoc() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("getTaco")
        .addParameter(ParameterSpec.builder(TypeName.DOUBLE, "money")
            .addJavadoc("the amount required to buy the taco.\n")
            .build())
        .addParameter(ParameterSpec.builder(TypeName.INT, "count")
            .addJavadoc("the number of Tacos to buy.\n")
            .build())
        .addJavadoc("Gets the best Taco money can buy.\n")
        .build();
    assertThat(methodSpec.toString()).isEqualTo(""
        + "/**\n"
        + " * Gets the best Taco money can buy.\n"
        + " *\n"
        + " * @param money the amount required to buy the taco.\n"
        + " * @param count the number of Tacos to buy.\n"
        + " */\n"
        + "void getTaco(double money, int count) {\n"
        + "}\n");
  }

  /**
   * takes two parameters `money` and `count` and does nothing with them. It also
   * generates JavaDocs for the method.
   */
  @Test public void withParameterJavaDocAndWithoutMethodJavadoc() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("getTaco")
        .addParameter(ParameterSpec.builder(TypeName.DOUBLE, "money")
            .addJavadoc("the amount required to buy the taco.\n")
            .build())
        .addParameter(ParameterSpec.builder(TypeName.INT, "count")
            .addJavadoc("the number of Tacos to buy.\n")
            .build())
        .build();
    assertThat(methodSpec.toString()).isEqualTo(""
        + "/**\n"
        + " * @param money the amount required to buy the taco.\n"
        + " * @param count the number of Tacos to buy.\n"
        + " */\n"
        + "void getTaco(double money, int count) {\n"
        + "}\n");
  }

  /**
   * tests whether the method `MethodSpec.toBuilder().addException(ioException).build().exceptions`
   * returns the same list as `MethodSpec.methodBuilder("duplicateExceptions")
   *       .addException(ioException)
   *       .addException(timeoutException)
   *       .addException(timeoutException)
   *       .addException(ioException)
   *       .build()`
   */
  @Test public void duplicateExceptionsIgnored() {
    ClassName ioException = ClassName.get(IOException.class);
    ClassName timeoutException = ClassName.get(TimeoutException.class);
    MethodSpec methodSpec = MethodSpec.methodBuilder("duplicateExceptions")
      .addException(ioException)
      .addException(timeoutException)
      .addException(timeoutException)
      .addException(ioException)
      .build();
    assertThat(methodSpec.exceptions).isEqualTo(Arrays.asList(ioException, timeoutException));
    assertThat(methodSpec.toBuilder().addException(ioException).build().exceptions)
      .isEqualTo(Arrays.asList(ioException, timeoutException));
  }

  /**
   * checks that attempting to build a method with a null name results in a `NullPointerException`.
   */
  @Test public void nullIsNotAValidMethodName() {
    try {
      MethodSpec.methodBuilder(null);
      fail("NullPointerException expected");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("name == null");
    }
  }

  /**
   * tests whether adding null to a `MethodSpec.Builder` causes a `NullPointerException`.
   */
  @Test public void addModifiersVarargsShouldNotBeNull() {
    try {
      MethodSpec.methodBuilder("taco")
              .addModifiers((Modifier[]) null);
      fail("NullPointerException expected");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("modifiers == null");
    }
  }

  /**
   * modifies the name of a method by reusing an existing method builder object and
   * building it with the new name.
   */
  @Test public void modifyMethodName() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("initialMethod")
        .build()
        .toBuilder()
        .setName("revisedMethod")
        .build();

    assertThat(methodSpec.toString()).isEqualTo("" + "void revisedMethod() {\n" + "}\n");
  }

  /**
   * modifies an annotation on a method by removing one of its annotations, and then
   * checks that the remaining annotation is still present.
   */
  @Test public void modifyAnnotations() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("foo")
            .addAnnotation(Override.class)
            .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  /**
   * modifies the modifiers of a method by removing one of them.
   */
  @Test public void modifyModifiers() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("foo")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }

  /**
   * modifies a method parameter list by removing the first element, leaving an empty
   * list.
   */
  @Test public void modifyParameters() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("foo")
            .addParameter(int.class, "source");

    builder.parameters.remove(0);
    assertThat(builder.build().parameters).isEmpty();
  }

  /**
   * modifies a MethodSpec.Builder's type variables, removing one of them and ensuring
   * that only the remaining variable is present.
   */
  @Test public void modifyTypeVariables() {
    TypeVariableName t = TypeVariableName.get("T");
    MethodSpec.Builder builder = MethodSpec.methodBuilder("foo")
            .addTypeVariable(t)
            .addTypeVariable(TypeVariableName.get("V"));

    builder.typeVariables.remove(1);
    assertThat(builder.build().typeVariables).containsExactly(t);
  }

  /**
   * verifies that a method's code ends with a newline character.
   */
  @Test public void ensureTrailingNewline() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("method")
        .addCode("codeWithNoNewline();")
        .build();

    assertThat(methodSpec.toString()).isEqualTo(""
        + "void method() {\n"
        + "  codeWithNoNewline();\n"
        + "}\n");
  }

  /**
   * verifies that a method's code has no trailing newline and already contains one or
   * more newlines.
   */
  @Test public void ensureTrailingNewlineWithExistingNewline() {
    MethodSpec methodSpec = MethodSpec.methodBuilder("method")
        .addCode("codeWithNoNewline();\n") // Have a newline already, so ensure we're not adding one
        .build();

    assertThat(methodSpec.toString()).isEqualTo(""
        + "void method() {\n"
        + "  codeWithNoNewline();\n"
        + "}\n");
  }

  /**
   * creates a methodSpec that defines control flow statements using named code blocks,
   * allowing for more readable and maintainable code.
   */
  @Test public void controlFlowWithNamedCodeBlocks() {
    Map<String, Object> m = new HashMap<>();
    m.put("field", "valueField");
    m.put("threshold", "5");

    MethodSpec methodSpec = MethodSpec.methodBuilder("method")
        .beginControlFlow(named("if ($field:N > $threshold:L)", m))
        .nextControlFlow(named("else if ($field:N == $threshold:L)", m))
        .endControlFlow()
        .build();

    assertThat(methodSpec.toString()).isEqualTo(""
        + "void method() {\n"
        + "  if (valueField > 5) {\n"
        + "  } else if (valueField == 5) {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * defines a method that executes a block of code while a field value is greater than
   * a specified threshold. The method uses the `do-while` control flow to iterate
   * through the block until the condition is met.
   */
  @Test public void doWhileWithNamedCodeBlocks() {
    Map<String, Object> m = new HashMap<>();
    m.put("field", "valueField");
    m.put("threshold", "5");

    MethodSpec methodSpec = MethodSpec.methodBuilder("method")
        .beginControlFlow("do")
        .addStatement(named("$field:N--", m))
        .endControlFlow(named("while ($field:N > $threshold:L)", m))
        .build();

    assertThat(methodSpec.toString()).isEqualTo(""
        + "void method() {\n" +
        "  do {\n" +
        "    valueField--;\n" +
        "  } while (valueField > 5);\n" +
        "}\n");
  }

  /**
   * builds a `CodeBlock` instance with named code snippets based on the input `format`
   * and `args`.
   * 
   * @param format name of the code block to be generated.
   * 
   * @param args map of named arguments to be passed to the `CodeBlock.addNamed()`
   * method when creating the code block.
   * 
   * 	- `Map<String, ?>`: Represents an untyped map container with string keys and any
   * type value.
   * 	- `String format`: The name of the code block to be generated.
   * 	- `Map<String, ?> args`: The deserialized input map containing various properties
   * or attributes, which may require additional processing before generating the code
   * block.
   * 
   * @returns a `CodeBlock` object with named code fragments based on the input format
   * and arguments.
   * 
   * 	- The return type of the function is `CodeBlock`, which represents a block of
   * code that can be executed in a Java program.
   * 	- The function takes two parameters: `format` and `args`. `format` is a string
   * representing the name of the code block, while `args` is a map of strings to values
   * containing the arguments for the code block.
   * 	- The function returns a `CodeBlock.builder()` instance with the provided format
   * and arguments added to it.
   * 
   * The `named` function allows developers to create named code blocks in their programs,
   * which can help with organization and readability.
   */
  private static CodeBlock named(String format, Map<String, ?> args){
    return CodeBlock.builder().addNamed(format, args).build();
  }

}
