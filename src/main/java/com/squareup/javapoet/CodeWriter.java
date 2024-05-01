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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;
import static com.squareup.javapoet.Util.stringLiteralWithDoubleQuotes;
import static java.lang.String.join;

/**
 * in Java is a utility class that provides methods for emitting source code to an
 * output stream, managing indentation, and resolving names. It also maintains a set
 * of suggested imports for the code being written. The class uses a multiset data
 * structure to keep track of imported types and referenced names, and it provides
 * methods for adding, removing, and checking if a type is in the multiset.
 */
final class CodeWriter {
  /** Sentinel value that indicates that no user-provided package has been set. */
  private static final String NO_PACKAGE = new String();
  private static final Pattern LINE_BREAKING_PATTERN = Pattern.compile("\\R");

  private final String indent;
  private final LineWrapper out;
  private int indentLevel;

  private boolean javadoc = false;
  private boolean comment = false;
  private String packageName = NO_PACKAGE;
  private final List<TypeSpec> typeSpecStack = new ArrayList<>();
  private final Set<String> staticImportClassNames;
  private final Set<String> staticImports;
  private final Set<String> alwaysQualify;
  private final Map<String, ClassName> importedTypes;
  private final Map<String, ClassName> importableTypes = new LinkedHashMap<>();
  private final Set<String> referencedNames = new LinkedHashSet<>();
  private final Multiset<String> currentTypeVariables = new Multiset<>();
  private boolean trailingNewline;

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  int statementLine = -1;

  CodeWriter(Appendable out) {
    this(out, "  ", Collections.emptySet(), Collections.emptySet());
  }

  CodeWriter(Appendable out, String indent, Set<String> staticImports, Set<String> alwaysQualify) {
    this(out, indent, Collections.emptyMap(), staticImports, alwaysQualify);
  }

  CodeWriter(Appendable out,
      String indent,
      Map<String, ClassName> importedTypes,
      Set<String> staticImports,
      Set<String> alwaysQualify) {
    this.out = new LineWrapper(out, indent, 100);
    this.indent = checkNotNull(indent, "indent == null");
    this.importedTypes = checkNotNull(importedTypes, "importedTypes == null");
    this.staticImports = checkNotNull(staticImports, "staticImports == null");
    this.alwaysQualify = checkNotNull(alwaysQualify, "alwaysQualify == null");
    this.staticImportClassNames = new LinkedHashSet<>();
    for (String signature : staticImports) {
      staticImportClassNames.add(signature.substring(0, signature.lastIndexOf('.')));
    }
  }

  /**
   * maps the imported class names to their respective types.
   * 
   * @returns a map of string to class names.
   * 
   * 	- The output is a map containing key-value pairs where the keys are strings and
   * the values are Class objects.
   * 	- Each key in the map represents a type that has been imported by the program.
   * 	- The values of the map are instances of the Class class, which represents a class
   * or interface in Java.
   * 	- The map can have multiple entries for the same type if different versions of
   * the class or interface have been imported.
   */
  public Map<String, ClassName> importedTypes() {
    return importedTypes;
  }

  /**
   * returns the result of calling the `indent` method with an argument of `1`.
   * 
   * @returns a `CodeWriter` object with one level of indentation added to the input code.
   */
  public CodeWriter indent() {
    return indent(1);
  }

  /**
   * increments the `indentLevel` and returns a reference to itself, allowing chaining
   * of calls for nested indentation.
   * 
   * @param levels number of indentation levels to be applied to the code.
   * 
   * @returns an instance of the `CodeWriter` class with increased indentation level.
   * 
   * The `indentLevel` variable represents the number of indentation levels added to
   * the current level. It is incremented by the input `levels`.
   * 
   * The `return this;` statement indicates that the function returns a reference to
   * itself, allowing for chaining multiple calls to the function.
   */
  public CodeWriter indent(int levels) {
    indentLevel += levels;
    return this;
  }

  /**
   * returns an instance of `CodeWriter`, which can be used to indent or dedent code.
   * The function takes an integer parameter `n` representing the number of spaces to
   * unIndent.
   * 
   * @returns a `CodeWriter` object containing the indented code.
   */
  public CodeWriter unindent() {
    return unindent(1);
  }

  /**
   * reduces the number of indentation levels of a given code by the specified amount
   * `levels`.
   * 
   * @param levels amount of indentation to be unindented by the `unindent()` method.
   * 
   * @returns a reference to the same `CodeWriter` instance with a reduced indentation
   * level.
   * 
   * The `un indent` function returns an instance of the `CodeWriter` class, indicating
   * that it is a method that can be called repeatedly to perform various code-related
   * tasks. The returned object is the same as the original one, but with the specified
   * number of indentation levels removed from its `indentLevel` attribute.
   * The `checkArgument` method is used inside the function to ensure that the input
   * provided by the user is within a valid range. If the input is not valid, an error
   * message is displayed.
   */
  public CodeWriter unindent(int levels) {
    checkArgument(indentLevel - levels >= 0, "cannot unindent %s from %s", levels, indentLevel);
    indentLevel -= levels;
    return this;
  }

  /**
   * updates the package name of an instance of `CodeWriter`. It checks if the package
   * name is already set, then sets the new package name and returns the modified instance.
   * 
   * @param packageName name of a package to which the `CodeWriter` instance belongs
   * or will belong after calling the `pushPackage()` method.
   * 
   * @returns a reference to the same `CodeWriter` instance.
   * 
   * 	- The function returns an instance of `CodeWriter`.
   * 	- The `packageName` field is updated with the provided package name.
   * 	- The function checks for the validity of the input parameters using static methods
   * `checkState()` and `checkNotNull()`.
   */
  public CodeWriter pushPackage(String packageName) {
    checkState(this.packageName == NO_PACKAGE, "package already set: %s", this.packageName);
    this.packageName = checkNotNull(packageName, "packageName == null");
    return this;
  }

  /**
   * modifies the internal state of a `CodeWriter` object by resetting its package name
   * to `NO_PACKAGE`.
   * 
   * @returns a reference to the same `CodeWriter` instance.
   * 
   * 	- The function returns a `public CodeWriter` object, indicating that the method
   * is public and returns a writer object.
   * 	- The function takes no arguments, meaning it does not accept any input parameters.
   * 	- The function modifies the state of the `CodeWriter` instance by setting the
   * `packageName` field to `NO_PACKAGE`, indicating that the package name has been cleared.
   */
  public CodeWriter popPackage() {
    checkState(this.packageName != NO_PACKAGE, "package not set");
    this.packageName = NO_PACKAGE;
    return this;
  }

  /**
   * adds a `TypeSpec` object to the calling object's internal type stack, returning
   * the modified object for chaining.
   * 
   * @param type type of data that will be written by the `CodeWriter` instance, and
   * it is added to the internal stack of type specifications.
   * 
   * 	- The method `pushType` adds the deserialized `TypeSpec` object to the internal
   * stack of `TypeSpec` objects.
   * 	- The `TypeSpec` object represents a type in the program's syntax tree. It contains
   * information about the type's identity, including its name, modifiers, and other attributes.
   * 	- The `type` parameter passed to the `pushType` function is of type `TypeSpec`,
   * which means it encapsulates information about the type's identity and can be used
   * to manipulate or analyze the type in various ways within the program.
   * 
   * @returns a reference to the `TypeSpec` object that was passed as an argument.
   * 
   * 	- The `typeSpecStack` attribute of the `pushType` function is updated by adding
   * the `type` parameter passed as an argument to its list of type specifications.
   * 	- The function returns a reference to itself, which allows for chaining multiple
   * method calls together.
   * 	- The return type of the function is `CodeWriter`, indicating that it can be used
   * in combination with other methods to create and modify Java code.
   */
  public CodeWriter pushType(TypeSpec type) {
    this.typeSpecStack.add(type);
    return this;
  }

  /**
   * removes the last element from the `typeSpecStack`.
   * 
   * @returns the current `CodeWriter` instance.
   * 
   * 	- The `typeSpecStack` is removed from the top of the stack, indicating that the
   * last type specification has been popped off.
   * 	- The function returns the same instance of the `CodeWriter` object, allowing for
   * chaining of method calls.
   */
  public CodeWriter popType() {
    this.typeSpecStack.remove(typeSpecStack.size() - 1);
    return this;
  }

  /**
   * enables the creation of comments using the `//` prefix and forces trailing newlines.
   * It also emits the code block and adds a newline before finishing the comment.
   * 
   * @param codeBlock code to be compiled and emitted as a comment in the output, which
   * is then produced by the `emit()` method.
   * 
   * 	- `trailingNewline`: A boolean variable indicating whether a newline character
   * should be appended to the comment.
   * 	- `comment`: A boolean variable representing whether the code block is a comment
   * or not.
   * 	- `emit(codeBlock)`: Calls the `emit` method on the `CodeWriter` instance, passing
   * in the deserialized `codeBlock` object as an argument. This method is used to write
   * the code block to a file or other output stream.
   * 	- `emit("\n")`: Appends a newline character to the output after writing the code
   * block.
   */
  public void emitComment(CodeBlock codeBlock) throws IOException {
    trailingNewline = true; // Force the '//' prefix for the comment.
    comment = true;
    try {
      emit(codeBlock);
      emit("\n");
    } finally {
      comment = false;
    }
  }

  /**
   * emits Javadoc comments for a given code block, based on whether it is empty or not.
   * 
   * @param javadocCodeBlock Java code that should be documented with Javadoc comments,
   * which are then emitted to the output file by the `emitJavadoc()` method.
   * 
   * 	- `isEmpty()`: This method checks whether the `javadocCodeBlock` is empty or not.
   * If it is empty, then nothing further is executed in the function.
   * 	- `javadoc`: A boolean variable that indicates whether the current line of code
   * should start a new Javadoc comment or not. It is set to true inside the function
   * and reset to false after the emission of each Javadoc line.
   * 	- `try...finally`: This construct encloses the emission of the `javadocCodeBlock`
   * within a try-catch block. The catch block resets the value of `javadoc` to false,
   * ensuring that the function correctly terminates after emitting all Javadoc lines.
   */
  public void emitJavadoc(CodeBlock javadocCodeBlock) throws IOException {
    if (javadocCodeBlock.isEmpty()) return;

    emit("/**\n");
    javadoc = true;
    try {
      emit(javadocCodeBlock, true);
    } finally {
      javadoc = false;
    }
    emit(" */\n");
  }

  /**
   * emits annotations in a source code, either inlined or at the end of a line, based
   * on a boolean parameter. It iterates through the list of AnnotationSpecs and calls
   * the `emit` method for each one, adding it to the output.
   * 
   * @param annotations list of AnnotationSpec objects that are emitted using the
   * `emit()` method.
   * 
   * 	- `List<AnnotationSpec>` represents a list of annotation specifications that can
   * contain different types of annotations.
   * 	- `AnnotationSpec` is an internal class representing an individual annotation
   * specification with various attributes such as name, description, and value.
   * 	- `inline` is a boolean parameter indicating whether the annotations should be
   * emitted inline or in a separate line.
   * 
   * @param inline whether the annotations should be emitted inline or as separate
   * lines, with `true` indicating inline emission and `false` indicating separate line
   * emission.
   */
  public void emitAnnotations(List<AnnotationSpec> annotations, boolean inline) throws IOException {
    for (AnnotationSpec annotationSpec : annotations) {
      annotationSpec.emit(this, inline);
      emit(inline ? " " : "\n");
    }
  }

  /**
   * emits the specified modifiers and indents them if they are not already present in
   * the implicit modifiers set.
   * 
   * @param modifiers set of modifiers that should be emitted in the code, and it is
   * used to control the flow of the function's execution.
   * 
   * 	- `modifiers`: A set containing various modifier types, such as `@Autowired`,
   * `@Service`, `@Component`, etc.
   * 	- `implicitModifiers`: A set containing modifiers that are automatically added
   * to classes without explicit mentioning.
   * 	- `EnumSet.copyOf()`: Used to create a new set containing all the modifiers in
   * the `modifiers` set.
   * 
   * @param implicitModifiers set of modifiers that are already applied to the method,
   * and is used to skip over them when emitting their names in the output.
   * 
   * 	- `Set<Modifier> implicitModifiers`: A set of modifiers that are included in the
   * serialized form of the code by default. These are not explicitly specified by the
   * user.
   * 	- `Modifier` class: Represents a single modifier in the Java language, such as
   * `public`, `private`, or `protected`.
   * 	- `EnumSet.copyOf()` method: Creates a copy of the `modifiers` set, which is used
   * to iterate through the modifiers and emit them in the output file.
   */
  public void emitModifiers(Set<Modifier> modifiers, Set<Modifier> implicitModifiers)
      throws IOException {
    if (modifiers.isEmpty()) return;
    for (Modifier modifier : EnumSet.copyOf(modifiers)) {
      if (implicitModifiers.contains(modifier)) continue;
      emitAndIndent(modifier.name().toLowerCase(Locale.US));
      emitAndIndent(" ");
    }
  }

  /**
   * emits a set of modifiers to a file or input stream.
   * 
   * @param modifiers set of modifiers to be emitted.
   * 
   * 	- The function takes a `Set` of `Modifier` objects as input.
   * 	- The function returns nothing (`void`) after processing the input.
   * 	- The function throws an `IOException` if any error occurs during the process.
   */
  public void emitModifiers(Set<Modifier> modifiers) throws IOException {
    emitModifiers(modifiers, Collections.emptySet());
  }

  /**
   * iterates over a list of type variables and emits their names, followed by any
   * annotations and bounds.
   * 
   * @param typeVariables list of type variables to be processed and emitted as part
   * of the Java bytecode.
   * 
   * 	- `isEmpty()`: Indicates whether the list is empty or not.
   * 	- `forEach()`: An iterator that iterates through each element in the list.
   * 	- `add()`: Adds an element to the list.
   * 	- `emit()`: Generates a specific XML output based on the input parameter.
   * 	- `<`: The less than symbol used as a delimiter between type variables.
   * 	- `currentTypeVariables`: A reference to a list of type variable names that are
   * currently being processed.
   * 	- `annotations`: An array of annotations associated with each type variable.
   * 	- `$L`: A placeholder for the type variable name.
   * 	- `$T`: A placeholder for the type name.
   * 	- `firstBound`: A boolean variable indicating whether the first bound is encountered
   * or not.
   * 	- `bounds`: An array of type names that are bounds for a particular type variable.
   */
  public void emitTypeVariables(List<TypeVariableName> typeVariables) throws IOException {
    if (typeVariables.isEmpty()) return;

    typeVariables.forEach(typeVariable -> currentTypeVariables.add(typeVariable.name));

    emit("<");
    boolean firstTypeVariable = true;
    for (TypeVariableName typeVariable : typeVariables) {
      if (!firstTypeVariable) emit(", ");
      emitAnnotations(typeVariable.annotations, true);
      emit("$L", typeVariable.name);
      boolean firstBound = true;
      for (TypeName bound : typeVariable.bounds) {
        emit(firstBound ? " extends $T" : " & $T", bound);
        firstBound = false;
      }
      firstTypeVariable = false;
    }
    emit(">");
  }

  /**
   * removes specified type variables from a list and their corresponding types from a
   * separate list, using a forEach loop.
   * 
   * @param typeVariables list of type variables that need to be removed from the current
   * set of type variables.
   * 
   * 	- The `List<TypeVariableName>` type indicates that `typeVariables` is a list of
   * strings representing the names of type variables.
   * 	- The `forEach` method is used to iterate through each element in the list,
   * applying the action on each element. In this case, the action is removing the
   * current type variable from the `currentTypeVariables` collection.
   * 	- The `currentTypeVariables` collection is referenced but not defined within the
   * provided code snippet, suggesting it may be a field or parameter of the class
   * containing the `popTypeVariables` method.
   */
  public void popTypeVariables(List<TypeVariableName> typeVariables) throws IOException {
    typeVariables.forEach(typeVariable -> currentTypeVariables.remove(typeVariable.name));
  }

  /**
   * takes a `String` argument and returns another string emitted from it, along with
   * any necessary indentation.
   * 
   * @param s string to be processed and passed through the `emitAndIndent()` method
   * for further processing.
   * 
   * @returns a string representing the emitted code.
   * 
   * The output is an instance of `IOException`. This means that it can throw an exception
   * of type `IOException` during execution.
   * 
   * The `emitAndIndent` method returns the output as a result of the `emit` function.
   * This indicates that the output is the result of a sequence of instructions executed
   * by the `emit` function.
   * 
   * Therefore, the output returned by the `emit` function has the following attributes:
   * it can throw an exception of type `IOException`, and it is the result of executing
   * the `emit` function followed by the `emitAndIndent` method.
   */
  public CodeWriter emit(String s) throws IOException {
    return emitAndIndent(s);
  }

  /**
   * writes a code block represented by a string format and arguments to an output
   * stream as an IOException.
   * 
   * @param format Java code to be emitted by the `emit()` method.
   * 
   * @returns a `CodeWriter` object capable of emitting code in the specified format
   * with the provided arguments.
   * 
   * 	- The return type is `String`, indicating that the method emits a string value
   * as its output.
   * 	- The input parameters `format` and `args` represent the format and arguments to
   * be used for the emission.
   * 	- The method returns an instance of `CodeBlock`, which is a container class for
   * representing a block of code, including its formatting and syntax information.
   */
  public CodeWriter emit(String format, Object... args) throws IOException {
    return emit(CodeBlock.of(format, args));
  }

  /**
   * emits a code block as an output in the form of a string. It takes an optional
   * boolean parameter indicating whether to include line numbers in the output.
   * 
   * @param codeBlock Java code to be emitted by the `CodeWriter` object.
   * 
   * 	- `codeBlock` is an instance of `CodeBlock`, which represents a block of Java
   * code to be emitted into bytecode.
   * 	- The method takes a single parameter `false`, which indicates that the emitted
   * bytecode should not be signed or verified by the Java Virtual Machine (JVM).
   * 
   * @returns a `CodeWriter` object that can be used to write the provided code block
   * to a file or other output stream.
   * 
   * 1/ The output is an instance of the `CodeWriter` class, which represents a sink
   * for writing source code.
   * 2/ The output has a ` throws IOException` exception, indicating that it may raise
   * an error of type IOException during its execution.
   * 3/ The output's `emit` method takes a single argument, `codeBlock`, which is an
   * instance of the `CodeBlock` class representing a block of source code.
   */
  public CodeWriter emit(CodeBlock codeBlock) throws IOException {
    return emit(codeBlock, false);
  }

  /**
   * emits Java code based on a provided `CodeBlock` and handles various syntax elements
   * such as literals, identifiers, imports, and statements.
   * 
   * @param codeBlock code to be formatted and emitted, which is passed through a series
   * of method calls to perform formatting and emission.
   * 
   * 	- `codeBlock.formatParts`: an iterator over the parts of the code block, each
   * part being a single line of the code
   * 	- `codeBlock.args`: a list of arguments passed to the `emit` function for each
   * part of the code block
   * 	- `codeBlock.statementLine`: the line number of the current statement in the code
   * block (used to handle multi-line statements)
   * 	- `deferredTypeName`: a variable that stores the name of a type that is being
   * imported statically, used by the "import static" logic
   * 
   * The function explains each part of the code block using the following lines:
   * 
   * 	- `int a = 0;`: declares an integer variable 'a' with value 0
   * 	- `ClassName deferredTypeName = null; // used by "import static"`: stores the
   * name of a type that is being imported statically, used by the "import static" logic
   * 	- `ListIterator<String> partIterator = codeBlock.formatParts.listIterator();`:
   * explains that 'partIterator' is an iterator over the parts of the code block
   * 	- `while (partIterator.hasNext()) {`: explains that the function iterates over
   * the parts of the code block using a while loop
   * 	- `String part = partIterator.next();`: explains that each iteration of the loop
   * retrieves the next part of the code block as a 'part' variable
   * 
   * The function then describes the logic for each part of the code block:
   * 
   * 	- `switch (part) { ... }`: explains that the function uses a switch statement to
   * handle different types of parts in the code block
   * 	- `emitLiteral(codeBlock.args.get(a++));`: explains that the function emits a
   * literal value passed as an argument to the current part
   * 	- `emitAndIndent((String) codeBlock.args.get(a++));`: explains that the function
   * emits a single line of code and increments 'a' by 1
   * 	- `TypeName typeName = (TypeName) codeBlock.args.get(a++);`: explains that the
   * function retrieves the next type name passed as an argument to the current part
   * 	- `if (typeName instanceof ClassName && partIterator.hasNext()) { ... }`: explains
   * that the function checks if the retrieved type name is an instance of 'ClassName'
   * and has a next part in the code block
   * 	- `deferredTypeName = candidate;`: explains that the function stores the name of
   * the type that was imported statically
   * 	- `typeName.emit(this);`: explains that the function calls the 'emit' method of
   * the retrieved type name to emit its code
   * 
   * Finally, the function handles the remaining parts of the code block:
   * 
   * 	- `if (part.startsWith(".")) { ... }`: explains that the function handles leading
   * dots in the part of the code block
   * 	- `out.wrappingSpace(indentLevel + 2);`: explains that the function inserts a
   * wrapping space at the current indentation level
   * 	- `out.zeroWidthSpace(indentLevel + 2);`: explains that the function inserts a
   * zero-width space at the current indentation level
   * 
   * The function does not provide a summary at the end, as it is intended to be a
   * detailed explanation of each part of the code block.
   * 
   * @param ensureTrailingNewline emitter's duty to emit an explicit newline character
   * if the output buffer ends with anything other than a newline.
   * 
   * @returns a Java code block with appropriate indentation and literal values.
   * 
   * 	- `out`: This is an instance of `IndentingWriter`, which provides the ability to
   * emit text with indentation based on the current indentation level.
   * 	- `ensureTrailingNewline`: This is a boolean parameter that indicates whether a
   * trailing newline should be emitted if the last character in the output is not a newline.
   * 
   * The `emit` function takes a `CodeBlock` object as input and emits its code using
   * the `out` instance. The `CodeBlock` object contains information about the code,
   * such as the arguments passed to the `emit` function, which are used to determine
   * how to emit each part of the code.
   * 
   * The various parts of the code are handled differently depending on their type, as
   * specified in the `switch` statement. For example, literals and static imports are
   * emitted directly, while other types require additional processing, such as deferring
   * the emission of a type name if it is a class name that may be used in a static import.
   * 
   * The `emit` function also handles special parts of the code, such as `$L`, `$N`,
   * `$S`, `$T`, `$`, `$>`, `$<`, `$[`, and `$]`, each with their own specific behavior.
   * For example, the `$L` part is emitted literally, while the `$N` part is emited as
   * a name.
   * 
   * Overall, the `emit` function is designed to handle the complexities of Java code
   * formatting and indentation in a flexible and efficient manner.
   */
  public CodeWriter emit(CodeBlock codeBlock, boolean ensureTrailingNewline) throws IOException {
    int a = 0;
    ClassName deferredTypeName = null; // used by "import static" logic
    ListIterator<String> partIterator = codeBlock.formatParts.listIterator();
    while (partIterator.hasNext()) {
      String part = partIterator.next();
      switch (part) {
        case "$L":
          emitLiteral(codeBlock.args.get(a++));
          break;

        case "$N":
          emitAndIndent((String) codeBlock.args.get(a++));
          break;

        case "$S":
          String string = (String) codeBlock.args.get(a++);
          // Emit null as a literal null: no quotes.
          emitAndIndent(string != null
              ? stringLiteralWithDoubleQuotes(string, indent)
              : "null");
          break;

        case "$T":
          TypeName typeName = (TypeName) codeBlock.args.get(a++);
          // defer "typeName.emit(this)" if next format part will be handled by the default case
          if (typeName instanceof ClassName && partIterator.hasNext()) {
            if (!codeBlock.formatParts.get(partIterator.nextIndex()).startsWith("$")) {
              ClassName candidate = (ClassName) typeName;
              if (staticImportClassNames.contains(candidate.canonicalName)) {
                checkState(deferredTypeName == null, "pending type for static import?!");
                deferredTypeName = candidate;
                break;
              }
            }
          }
          typeName.emit(this);
          break;

        case "$$":
          emitAndIndent("$");
          break;

        case "$>":
          indent();
          break;

        case "$<":
          unindent();
          break;

        case "$[":
          checkState(statementLine == -1, "statement enter $[ followed by statement enter $[");
          statementLine = 0;
          break;

        case "$]":
          checkState(statementLine != -1, "statement exit $] has no matching statement enter $[");
          if (statementLine > 0) {
            unindent(2); // End a multi-line statement. Decrease the indentation level.
          }
          statementLine = -1;
          break;

        case "$W":
          out.wrappingSpace(indentLevel + 2);
          break;

        case "$Z":
          out.zeroWidthSpace(indentLevel + 2);
          break;

        default:
          // handle deferred type
          if (deferredTypeName != null) {
            if (part.startsWith(".")) {
              if (emitStaticImportMember(deferredTypeName.canonicalName, part)) {
                // okay, static import hit and all was emitted, so clean-up and jump to next part
                deferredTypeName = null;
                break;
              }
            }
            deferredTypeName.emit(this);
            deferredTypeName = null;
          }
          emitAndIndent(part);
          break;
      }
    }
    if (ensureTrailingNewline && out.lastChar() != '\n') {
      emit("\n");
    }
    return this;
  }

  /**
   * writes a wrapping space at the specified indentation level to the output stream `out`.
   * 
   * @returns a new instance of the `CodeWriter` class with an increased indentation level.
   * 
   * 1/ The `out` object is used to write wrapping spaces with an indentation level of
   * `indentLevel + 2`.
   * 2/ The `wrappingSpace` method modifies the output by adding wrapping spaces.
   * 3/ The returned object is a `CodeWriter` instance, which allows for further
   * manipulation or outputting of code.
   */
  public CodeWriter emitWrappingSpace() throws IOException {
    out.wrappingSpace(indentLevel + 2);
    return this;
  }

  /**
   * extracts a member name from a given string, checking that it starts with an
   * identifier character and consists only of identifiers thereafter.
   * 
   * @param part string to be checked for validity as an identifier in Java.
   * 
   * @returns a string representing the member name of the given input.
   */
  private static String extractMemberName(String part) {
    checkArgument(Character.isJavaIdentifierStart(part.charAt(0)), "not an identifier: %s", part);
    for (int i = 1; i <= part.length(); i++) {
      if (!SourceVersion.isIdentifier(part.substring(0, i))) {
        return part.substring(0, i - 1);
      }
    }
    return part;
  }

  /**
   * checks if a member name in a part string is valid and if it already exists as an
   * import statement in the code, then adds it to the import statements if not found.
   * 
   * @param canonical fully qualified name of the component or class being processed,
   * which is used to construct the import statement.
   * 
   * @param part portion of the code to be checked for static imports, and its value
   * is passed to the `extractMemberName()` method to determine whether it contains any
   * valid Java identifier characters.
   * 
   * @returns a boolean value indicating whether the given part should be imported
   * statically or not.
   */
  private boolean emitStaticImportMember(String canonical, String part) throws IOException {
    String partWithoutLeadingDot = part.substring(1);
    if (partWithoutLeadingDot.isEmpty()) return false;
    char first = partWithoutLeadingDot.charAt(0);
    if (!Character.isJavaIdentifierStart(first)) return false;
    String explicit = canonical + "." + extractMemberName(partWithoutLeadingDot);
    String wildcard = canonical + ".*";
    if (staticImports.contains(explicit) || staticImports.contains(wildcard)) {
      emitAndIndent(partWithoutLeadingDot);
      return true;
    }
    return false;
  }

  /**
   * emits Java code based on the object passed as an argument. It handles different
   * types of objects, including `TypeSpec`, `AnnotationSpec`, `CodeBlock`, and strings,
   * and emits them accordingly.
   * 
   * @param o object to be emitted, which can be either an instance of `TypeSpec`,
   * `AnnotationSpec`, `CodeBlock`, or a string representing a literal code block.
   * 
   * 	- If `o` is an instance of `TypeSpec`, it represents a type definition and has
   * no additional properties.
   * 	- If `o` is an instance of `AnnotationSpec`, it represents an annotation and has
   * an `emit()` method that emits the annotation to the output stream.
   * 	- If `o` is an instance of `CodeBlock`, it represents a code block and has no
   * additional properties.
   * 	- Otherwise, `o` represents a string value and can be directly emitted to the
   * output stream using the `emitAndIndent()` method.
   */
  private void emitLiteral(Object o) throws IOException {
    if (o instanceof TypeSpec) {
      TypeSpec typeSpec = (TypeSpec) o;
      typeSpec.emit(this, null, Collections.emptySet());
    } else if (o instanceof AnnotationSpec) {
      AnnotationSpec annotationSpec = (AnnotationSpec) o;
      annotationSpec.emit(this, true);
    } else if (o instanceof CodeBlock) {
      CodeBlock codeBlock = (CodeBlock) o;
      emit(codeBlock);
    } else {
      emitAndIndent(String.valueOf(o));
    }
  }

  /**
   * determines the shortest suffix of a given class name that resolves to the original
   * class name, taking into account local type names and imports. If the name is
   * resolved, it returns the fully qualified name, otherwise, it returns the original
   * class name.
   * 
   * @param className name of a class to be looked up, and the function returns the
   * canonical name of the class based on its simple name and import information.
   * 
   * 	- `topLevelClassName()`: This method returns the top-level simple name of the
   * class, which is the simple name of the class without any package information.
   * 	- `simpleName()`: This method returns the simple name of the class, which is the
   * shortest name that can refer to the class.
   * 	- `enclosingClassName()`: This method returns the enclosing class of the current
   * class, or null if the class has no enclosing class.
   * 	- `resolved`: This variable keeps track of whether a suffix was resolved for the
   * current class. If it is set to true, then a suffix was resolved and the method
   * returned an abbreviated name. Otherwise, the method did not find a suffix that
   * resolved to the original class name.
   * 	- `packageName()`: This method returns the package name of the class.
   * 	- `referencedNames`: This variable keeps track of the top-level simple names of
   * the classes that have been processed so far.
   * 	- `javadoc`: This variable indicates whether the class is documented or not. If
   * it is set to true, then the class has documentation. Otherwise, it does not have
   * documentation.
   * 
   * @returns a fully-qualified name of a class, or an importable type name if the class
   * is not in the same package.
   */
  String lookupName(ClassName className) {
    // If the top level simple name is masked by a current type variable, use the canonical name.
    String topLevelSimpleName = className.topLevelClassName().simpleName();
    if (currentTypeVariables.contains(topLevelSimpleName)) {
      return className.canonicalName;
    }

    // Find the shortest suffix of className that resolves to className. This uses both local type
    // names (so `Entry` in `Map` refers to `Map.Entry`). Also uses imports.
    boolean nameResolved = false;
    for (ClassName c = className; c != null; c = c.enclosingClassName()) {
      ClassName resolved = resolve(c.simpleName());
      nameResolved = resolved != null;

      if (resolved != null && Objects.equals(resolved.canonicalName, c.canonicalName)) {
        int suffixOffset = c.simpleNames().size() - 1;
        return join(".", className.simpleNames().subList(
            suffixOffset, className.simpleNames().size()));
      }
    }

    // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
    if (nameResolved) {
      return className.canonicalName;
    }

    // If the class is in the same package, we're done.
    if (Objects.equals(packageName, className.packageName())) {
      referencedNames.add(topLevelSimpleName);
      return join(".", className.simpleNames());
    }

    // We'll have to use the fully-qualified name. Mark the type as importable for a future pass.
    if (!javadoc) {
      importableType(className);
    }

    return className.canonicalName;
  }

  /**
   * checks if a class name can be imported and updates an internal map accordingly.
   * If the class name is empty or already qualified, it returns without making any
   * changes. Otherwise, it checks if the class is a nested type like `java.util.Map.Entry`,
   * and if so, it does not update the map. If the class is unique and not already
   * imported, it updates the internal map with the preferred version of the class.
   * 
   * @param className Java class name to check for importability.
   * 
   * 	- `packageName()`: Returns the package name of the class.
   * 	- `isEmpty()`: Checks if the package name is empty.
   * 	- `alwaysQualify`: A set of class names that should always be qualified with their
   * enclosing packages.
   * 	- `simpleName()`: Returns the simple name of the class without the package name.
   * 	- `topLevelClassName()`: Returns the top-level class name without any nested
   * classes or interfaces.
   * 	- `put()`: Method for storing the class name in a map, replacing the existing
   * value if there is one.
   */
  private void importableType(ClassName className) {
    if (className.packageName().isEmpty()) {
      return;
    } else if (alwaysQualify.contains(className.simpleName)) {
      // TODO what about nested types like java.util.Map.Entry?
      return;
    }
    ClassName topLevelClassName = className.topLevelClassName();
    String simpleName = topLevelClassName.simpleName();
    ClassName replaced = importableTypes.put(simpleName, topLevelClassName);
    if (replaced != null) {
      importableTypes.put(simpleName, replaced); // On collision, prefer the first inserted.
    }
  }

  /**
   * Returns the class referenced by {@code simpleName}, using the current nesting context and
   * imports.
   */
  // TODO(jwilson): also honor superclass members when resolving names.
  /**
   * matches a class or interface based on its simple name and nested types, and returns
   * the resolved class or interface if found.
   * 
   * @param simpleName name of a type that is being searched for within the current
   * class or its nested classes, top-level class, imported types, or the package.
   * 
   * @returns a `ClassName` object representing the resolved class.
   * 
   * 	- If the `simpleName` matches a child class of the current class, the function
   * returns the `stackClassName(i, simpleName)` instance.
   * 	- If the `simpleName` matches the top-level class, the function returns the
   * `ClassName.get(packageName, simpleName)` instance.
   * 	- If the `simpleName` is an imported type, the function returns the `importedType`
   * instance.
   * 	- If no match is found, the function returns `null`.
   * 
   * The output of the `resolve` function is a `ClassName` instance, which represents
   * a class name in Java. The `packageName` field of the `ClassName` instance indicates
   * the package where the class is defined, while the `simpleName` field represents
   * the simple name of the class.
   */
  private ClassName resolve(String simpleName) {
    // Match a child of the current (potentially nested) class.
    for (int i = typeSpecStack.size() - 1; i >= 0; i--) {
      TypeSpec typeSpec = typeSpecStack.get(i);
      if (typeSpec.nestedTypesSimpleNames.contains(simpleName)) {
        return stackClassName(i, simpleName);
      }
    }

    // Match the top-level class.
    if (typeSpecStack.size() > 0 && Objects.equals(typeSpecStack.get(0).name, simpleName)) {
      return ClassName.get(packageName, simpleName);
    }

    // Match an imported type.
    ClassName importedType = importedTypes.get(simpleName);
    if (importedType != null) return importedType;

    // No match.
    return null;
  }

  /**
   * generates a nested class name based on a given stack depth and simple name. It
   * recursively calls itself to create the nested class name, using the type specifiers
   * from a provided array.
   * 
   * @param stackDepth 1-based index of the current class in the nested class hierarchy,
   * which is used to construct the final nested class name for the returned object.
   * 
   * @param simpleName 4-character class name of the nested class to be created within
   * the `stackClassName`.
   * 
   * @returns a nested class with a given name and depth.
   * 
   * 	- `className`: This is the fully qualified class name of the nested class, which
   * is generated by combining the package name, type spec stack elements, and the
   * simple name provided as input.
   * 	- `nestedClass`: This refers to the nested class within the outer class, which
   * is created by recursively calling the `nestedClass` method until the desired depth
   * is reached.
   * 	- `typeSpecStack`: This is an array of type specifications that are used to
   * generate the class name. Each element in the stack represents a different level
   * of nesting.
   */
  private ClassName stackClassName(int stackDepth, String simpleName) {
    ClassName className = ClassName.get(packageName, typeSpecStack.get(0).name);
    for (int i = 1; i <= stackDepth; i++) {
      className = className.nestedClass(typeSpecStack.get(i).name);
    }
    return className.nestedClass(simpleName);
  }

  /**
   * emits a newline character and indents the following lines, taking into account
   * Javadoc comments and blank lines. It returns the function instance for chaining.
   * 
   * @param s Java code to be formatted, which is split into lines and emitted with
   * appropriate indentation and comment prefixes according to the specified rules.
   * 
   * @returns a string representing the indented and formatted Java code.
   * 
   * 	- `this`: This is a reference to the current instance of the `CodeWriter` class,
   * which is used to modify the output stream.
   * 	- `out`: This is an instance of the `OutputStream` interface, which is used to
   * write the generated code to a file or other output stream.
   * 	- `javadoc`: A boolean indicating whether Javadoc comments are present in the
   * input string.
   * 	- `comment`: A boolean indicating whether comment lines are present in the input
   * string.
   * 	- `trailingNewline`: A boolean indicating whether a newline character should be
   * emitted at the end of the output.
   * 	- `statementLine`: An integer indicating the current line number in a multi-line
   * statement.
   * 
   * The function takes a single argument, `s`, which is a String containing the input
   * code to be processed. The function processes the input code by splitting it into
   * individual lines using the `LINE_BREAKING_PATTERN` splitter, and then emitting
   * each line with appropriate indentation and comment prefixes as needed. Finally,
   * the function returns a reference to the current instance of the `CodeWriter` class,
   * which can be used to modify the output stream as needed.
   */
  CodeWriter emitAndIndent(String s) throws IOException {
    boolean first = true;
    for (String line : LINE_BREAKING_PATTERN.split(s, -1)) {
      // Emit a newline character. Make sure blank lines in Javadoc & comments look good.
      if (!first) {
        if ((javadoc || comment) && trailingNewline) {
          emitIndentation();
          out.append(javadoc ? " *" : "//");
        }
        out.append("\n");
        trailingNewline = true;
        if (statementLine != -1) {
          if (statementLine == 0) {
            indent(2); // Begin multiple-line statement. Increase the indentation level.
          }
          statementLine++;
        }
      }

      first = false;
      if (line.isEmpty()) continue; // Don't indent empty lines.

      // Emit indentation and comment prefix if necessary.
      if (trailingNewline) {
        emitIndentation();
        if (javadoc) {
          out.append(" * ");
        } else if (comment) {
          out.append("// ");
        }
      }

      out.append(line);
      trailingNewline = false;
    }
    return this;
  }

  /**
   * embeds a specified number of spaces using the `out` output stream, based on the
   * value of the ` indentLevel` field.
   */
  private void emitIndentation() throws IOException {
    for (int j = 0; j < indentLevel; j++) {
      out.append(indent);
    }
  }

  /**
   * maps import suggestions based on the imported types and removes referenced names
   * to provide updated import suggestions.
   * 
   * @returns a map of importable types with their keys removed based on referenced names.
   * 
   * 	- The result map contains key-value pairs representing the suggested imports for
   * the given code.
   * 	- The keys in the map represent the import names, which are Strings.
   * 	- The values in the map represent the ClassNames of the imported classes, which
   * can be any class that is imported by the given code.
   * 	- The map is a LinkedHashMap, which means it maintains a linked list of key-value
   * pairs, allowing for efficient insertion and removal operations.
   * 	- The map uses the `importableTypes` parameter as its initial value, which
   * represents the set of all possible import names that can be suggested based on the
   * given code.
   * 	- The map removes any key-value pairs corresponding to referenced names that are
   * not imported in the given code, using the `referencedNames` parameter. This ensures
   * that only unimported classes are suggested for import.
   */
  Map<String, ClassName> suggestedImports() {
    Map<String, ClassName> result = new LinkedHashMap<>(importableTypes);
    result.keySet().removeAll(referencedNames);
    return result;
  }

  // A makeshift multi-set implementation
  /**
   * is an implementation of a multiset data structure that allows for adding and
   * removing elements, as well as checking if an element is present in the set. It
   * provides a simple way to manage a collection of unique elements, making it useful
   * for a variety of applications.
   */
  private static final class Multiset<T> {
    private final Map<T, Integer> map = new LinkedHashMap<>();

    /**
     * increments the value associated with a given element in a map by one.
     * 
     * @param t value that is being added to the map, and its default value of 0 is used
     * as the initial count if the key is not found in the map.
     * 
     * 	- `map`: This is an instance of `Map`, which is used to store key-value pairs
     * representing objects and their counts.
     * 	- `getOrDefault`: This method returns the value associated with a given key (in
     * this case, `t`) or defaults to a specified value if the key does not exist in the
     * map. In the function, `0` is used as the default value.
     * 	- `put`: This method adds a new key-value pair to the map, replacing any existing
     * value for the key.
     */
    void add(T t) {
      int count = map.getOrDefault(t, 0);
      map.put(t, count + 1);
    }

    /**
     * removes an element from a multiset. It retrieves the count of the element in the
     * multiset, decreases the count by 1, and stores the updated count back in the
     * multiset. If the element is not present in the multiset, an `IllegalStateException`
     * is thrown.
     * 
     * @param t element to be removed from the multiset.
     * 
     * 	- `int count`: The number of times `t` appears in the multiset, as provided by
     * the `getOrDefault` method.
     * 	- `T t`: The value being removed from the multiset, which is a type parameter
     * that can take on any type.
     */
    void remove(T t) {
      int count = map.getOrDefault(t, 0);
      if (count == 0) {
        throw new IllegalStateException(t + " is not in the multiset");
      }
      map.put(t, count - 1);
    }

    /**
     * checks if a given value is present in a `Map`. If the value is not found, it returns
     * `false`. Otherwise, it returns `true` if the value exists and has a non-zero value
     * associated with it.
     * 
     * @param t value to be checked if it is present in the `map` and its value is greater
     * than 0, returning `true` if it exists and has a positive value, and `false` otherwise.
     * 
     * 	- `map`: A map is used to store some data. The value associated with each key in
     * the map is compared to 0 to determine if the element is present in the map.
     * 	- `t`: An object of type `T`, which represents a specific type of data, is passed
     * as an argument to the function.
     * 
     * The function returns a boolean value indicating whether or not the element is
     * present in the map.
     * 
     * @returns a boolean value indicating whether the input element is present in the
     * map with a value greater than 0.
     * 
     * 	- The function returns a boolean value indicating whether a given element is
     * present in the map or not.
     * 	- The map used in the function is of type `Map<T, Integer>`, where `T` represents
     * the type of elements stored in the map, and `Integer` represents the type of the
     * value associated with each element.
     * 	- The function uses the `getOrDefault` method to retrieve the value associated
     * with a given element in the map. If the element is not present in the map, the
     * function returns 0 by default.
     */
    boolean contains(T t) {
      return map.getOrDefault(t, 0) > 0;
    }
  }
}
