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

import static com.google.common.base.Charsets.*;
import static com.google.common.base.Preconditions.*;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.Statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * is a class that provides a high-level description of a Java program's types and
 * elements during execution. It contains a field compilationRule which is a JUnit4
 * Rule that executes tests such that instances of Elements and Types are available
 * during execution. The CompilationRule class is a rule that compiles the program
 * and returns the instances of Elements and Types.
 */
@RunWith(JUnit4.class)
public final class TypesEclipseTest extends AbstractTypesTest {
  /**
   * is a test rule for Java compilers that provides a way to run tests on the compilation
   * of code. It has two main methods: `apply` and `getElements`/`getTypes`. The `apply`
   * method takes a statement as input and evaluates it after running the compiler,
   * while the `getElements`/`getTypes` methods provide access to the Elements and Types
   * instances associated with the current execution of the rule. Compile is a static
   * private method that compiles the code using the Eclipse Java compiler.
   */
  public static final class CompilationRule implements TestRule {
    private Elements elements;
    private Types types;

    /**
     * creates a new `Statement` instance that delegates to the `base` statement, but
     * with an additional `AbstractProcessor` that runs the test on the last round after
     * compilation is over and throws any exceptions found.
     * 
     * @param base statement that will be compiled and evaluated before the new statement
     * created by the `apply` method is executed.
     * 
     * 	- `base`: The original statement to be processed, which is returned as a new
     * `Statement` object.
     * 	- `description`: A description of the input statement, which can be used for error
     * handling or logging purposes.
     * 
     * The `apply` function first compiles the input `base` using the `compile` method
     * and stores the result in an atomic reference. It then runs the `process` method
     * on the last round after compilation is over, passing in the `annotations` set
     * containing only the `base` element, and the `roundEnvironment` object. If any
     * errors are encountered during processing, they are stored in the `thrown` reference
     * and thrown as an exception at the end of the method.
     * 
     * @param description description of the method being processed, which is used to
     * determine the type of processing that should be performed on the method's code.
     * 
     * 	- `Description`: This is the class that represents a description object in the
     * Java programming language. It has several properties and methods that can be used
     * to manipulate and analyze the description object.
     * 	- `getSupportedSourceVersion()`: This method returns the latest version of the
     * Java source code supported by the `description` object.
     * 	- `getSupportedAnnotationTypes()`: This method returns a set of strings containing
     * the types of annotations supported by the `description` object. In this case, the
     * set contains only one element, "*", indicating that any type of annotation is supported.
     * 	- `init(ProcessingEnvironment processingEnv)`: This method is called when the
     * `description` object is created and is used to initialize the object's elements
     * and types fields. The `processingEnv` parameter is an instance of `ProcessingEnvironment`,
     * which provides access to the element and type utilities for the current Java compilation.
     * 	- `process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)`:
     * This method is called when the `description` object is processed by the compiler.
     * It takes a set of type elements and a `RoundEnvironment` parameter, which represents
     * the current round of compilation. The method checks if the current round is the
     * last one after compilation is over, and then calls the `evaluate()` method on the
     * input `base` statement to evaluate it. If an exception is thrown during evaluation,
     * the method catches it and stores it in an atomic reference. Finally, the method
     * returns `false`, indicating that the `description` object has been processed.
     * 
     * @returns a statement that runs the `base` statement and checks its state for errors,
     * throwing an exception if any are found.
     * 
     * 	- The output is an instance of the `Statement` interface, which means it can be
     * evaluated to produce a result.
     * 	- The `evaluate()` method is overrideed in the generated statement class, which
     * allows for customization of the behavior of the statement.
     * 	- The `thrown` field is an atomic reference variable that stores any throwable
     * exceptions encountered during the evaluation of the statement.
     * 	- The `checkState()` method is used to check if the statement was successfully
     * evaluated, and throws an exception if it was not.
     * 
     * Overall, the generated output is a customized statement class that can be used to
     * perform complex operations on code, including running tests after compilation is
     * over.
     */
    @Override
    public Statement apply(final Statement base, Description description) {
      return new Statement() {
        /**
         * compiles a given code and checks if it is successful, throwing an exception if
         * there are any issues.
         */
        @Override public void evaluate() throws Throwable {
          final AtomicReference<Throwable> thrown = new AtomicReference<>();
          boolean successful = compile(ImmutableList.of(new AbstractProcessor() {
            /**
             * returns the latest version of the source code for a particular module or library.
             * 
             * @returns the latest available source version.
             * 
             * 	- The `SourceVersion` object returned is of type `latest()`, indicating that it
             * represents the most recent version of Java available for use.
             * 	- The `latest()` method returns a `SourceVersion` object that represents the
             * current state of the Java language standard, which may change over time as new
             * versions are released.
             * 	- The `SourceVersion` class has several attributes that provide information about
             * the version, including the major and minor version numbers, the revision number,
             * and the build date.
             */
            @Override
            public SourceVersion getSupportedSourceVersion() {
              return SourceVersion.latest();
            }

            /**
             * returns a set of annotation types that are supported by the code snippet. In this
             * case, the set contains only one element, which is "*", indicating that all possible
             * annotation types are supported.
             * 
             * @returns a set of strings containing only the wildcard character "*".
             * 
             * 	- The return value is an `ImmutableSet` containing only one element, which is the
             * string literal `"*".`
             * 	- This indicates that the method supports all annotation types.
             * 	- The use of an `ImmutableSet` ensures that the set cannot be modified or changed
             * once it has been created.
             * 	- The single element in the set is the string `"*",` which represents all possible
             * annotation types.
             */
            @Override
            public Set<String> getSupportedAnnotationTypes() {
              return ImmutableSet.of("*");
            }

            /**
             * initializes various object references used throughout the program, including
             * `ProcessingEnvironment.getElementUtils()` and `ProcessingEnvironment.getTypeUtils()`.
             * 
             * @param processingEnv Java processing environment and provides access to the element
             * utilities and type utilities for use in the init method.
             * 
             * 	- `elements`: This is an instance of the `ElementUtils` class provided by the
             * processing environment, which offers methods for dealing with Java elements.
             * 	- `types`: This is an instance of the `TypeUtils` class offered by the processing
             * environment, which gives approaches to deal with Java types.
             */
            @Override
            public synchronized void init(ProcessingEnvironment processingEnv) {
              super.init(processingEnv);
              elements = processingEnv.getElementUtils();
              types = processingEnv.getTypeUtils();
            }

            /**
             * determines whether to evaluate a base class's `evaluate()` method based on the
             * round environment's `processingOver()` status and handles any thrown exceptions.
             * 
             * @param annotations set of type element annotations that are passed to the `evaluate()`
             * method for processing.
             * 
             * 	- `annotations`: A set of type elements representing annotations applied to the
             * Java element being compiled.
             * 	- `roundEnv`: The round environment containing the compilation unit being processed.
             * 
             * The function checks if the round environment's processing is over, and then evaluates
             * the `base` using a try-catch block. If an exception occurs during evaluation, it
             * sets the `thrown` variable to the caught exception.
             * 
             * @param roundEnv environment of the current round of compilation, which contains
             * information about the classes and methods being compiled.
             * 
             * 	- `processingOver()`: This method returns `true` if the round is over, and `false`
             * otherwise. It indicates whether the current round has been processed or not.
             * 	- `roundEnvironment`: This is a reference to the `RoundEnvironment` object, which
             * provides information about the current round of compilation.
             * 
             * @returns a boolean value indicating whether the test should be executed based on
             * the round's compilation status.
             */
            @Override
            public boolean process(Set<? extends TypeElement> annotations,
                RoundEnvironment roundEnv) {
              // just run the test on the last round after compilation is over
              if (roundEnv.processingOver()) {
                try {
                  base.evaluate();
                } catch (Throwable e) {
                  thrown.set(e);
                }
              }
              return false;
            }
          }));
          checkState(successful);
          Throwable t = thrown.get();
          if (t != null) {
            throw t;
          }
        }
      };
    }

    /**
     * returns a reference to an instance of `Elements`. The function checks that the
     * input `elements` is not null before returning it, ensuring that the function only
     * runs within the intended scope.
     * 
     * @returns a list of `Elements`.
     * 
     * The `elements` variable is checked to ensure it is not null before being returned
     * in the function.
     */
    public Elements getElements() {
      checkState(elements != null, "Not running within the rule");
      return elements;
    }

    /**
     * retrieves an array of type objects representing the element types of a given state.
     * 
     * @returns a `Types` object containing the types of elements in the input array.
     * 
     * The output is of type `Types`, which represents an immutable set of types in the
     * current state of the program.
     * The set contains elements that are references to the types defined within the program.
     * The types are stored in a TreeSet, which ensures that the order of the types is
     * consistent and follows a specific order based on their definition in the program.
     */
    public Types getTypes() {
      checkState(elements != null, "Not running within the rule");
      return types;
    }

    /**
     * takes an iterable of `Processor` objects and compiles them using the `JavaCompiler`.
     * The result is a successful compilation if the function returns `true`, otherwise
     * it returns `false`.
     * 
     * @param processors Iterable of Java processors that will be compiled by the Eclipse
     * Java compiler when called.
     * 
     * 	- `EclipseCompiler`: The Java compiler being used to compile the code.
     * 	- `DiagnosticCollector`: A container for storing diagnostics generated during the
     * compilation process.
     * 	- `JavaFileManager`: An object responsible for managing the files involved in the
     * compilation process.
     * 	- `JavaCompiler.CompilationTask`: An abstract class representing a task that
     * performs compilation.
     * 	- `ImmutableSet`: A set of immutable objects, used to store the types and classes
     * being compiled.
     * 	- `TypesEclipseTest`: The fully qualified name of the class being compiled.
     * 	- `processors`: An iterable collection of processors that are being passed as an
     * argument to the `compile` function.
     * 
     * @returns a boolean value indicating whether the compilation was successful.
     * 
     * 1/ The output is a boolean value indicating whether the compilation was successful
     * or not.
     * 2/ The value is determined by calling the `call()` method on a `JavaCompiler.CompilationTask`
     * object, passing in the processors as an argument.
     * 3/ The `call()` method executes the compiler and returns a boolean value representing
     * the result of the compilation.
     */
    static private boolean compile(Iterable<? extends Processor> processors) {
      JavaCompiler compiler = new EclipseCompiler();
      DiagnosticCollector<JavaFileObject> diagnosticCollector =
          new DiagnosticCollector<>();
      JavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, Locale.getDefault(), UTF_8);
      JavaCompiler.CompilationTask task = compiler.getTask(
          null,
          fileManager,
          diagnosticCollector,
          ImmutableSet.of(),
          ImmutableSet.of(TypesEclipseTest.class.getCanonicalName()),
          ImmutableSet.of());
      task.setProcessors(processors);
      return task.call();
    }
  }

  @Rule public final CompilationRule compilation = new CompilationRule();

  /**
   * returns a collection of elements generated by the compilation process.
   * 
   * @returns a collection of `Element` objects representing the elements in the compiled
   * code.
   * 
   * 	- The output is an array of Elements, indicating a collection of elements that
   * have been compiled from source code.
   * 	- The Elements array contains information about each element, including its name,
   * type, and other attributes.
   * 	- The order of the elements in the array reflects the order in which they were
   * defined in the source code.
   * 	- The Elements objects themselves contain additional properties and methods for
   * working with the elements, such as their text content, tag names, and parent elements.
   */
  @Override
  protected Elements getElements() {
    return compilation.getElements();
  }

  /**
   * returns a `Types` object representing the types available for use within the
   * compiling code.
   * 
   * @returns a collection of type references obtained from the compilation context.
   * 
   * The return type is `Types`, which represents a collection of type declarations.
   * 
   * The `compilation` field is a reference to the compilation unit that generated the
   * types.
   * 
   * The `getTypes` method returns the types of the compilation unit, indicating the
   * types defined in the code.
   */
  @Override
  protected Types getTypes() {
    return compilation.getTypes();
  }
}
