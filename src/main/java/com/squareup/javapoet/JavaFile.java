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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * represents a file containing source code in the Java programming language. It has
 * various fields and methods for specifying details about the file, such as its
 * package name, type spec, file comment, static imports, and indentation. The `Builder`
 * class is used to create instances of `JavaFile`, which can be customized before
 * being used to generate source code.
 */
public final class JavaFile {
  private static final Appendable NULL_APPENDABLE = new Appendable() {
    /**
     * returns a reference to the same object, indicating that the method does not change
     * the original object.
     * 
     * @param charSequence sequence of characters to be appended to the current Appendable
     * object.
     * 
     * 	- Return type: `Appendable`, indicating that this method returns an object capable
     * of being appended to.
     * 	- Input parameter: `CharSequence charSequence`, which is a generic parameter
     * representing any sequence of characters, such as a string or a character array.
     * 
     * @returns the same object instance.
     * 
     * The function returns an instance of the `Appendable` interface, which indicates
     * that it can accept additional content to be appended.
     * 
     * The function's return type is a reference to the same class (i.e., `this`),
     * indicating that the method chaining capability is enabled.
     * 
     * No arguments are passed to the function, so no parameters are used.
     */
    @Override public Appendable append(CharSequence charSequence) {
      return this;
    }
    /**
     * returns a reference to the same instance, allowing for chaining of method calls.
     * 
     * @param charSequence sequence of characters to be appended to the current state of
     * the `Appendable` object.
     * 
     * 	- The method takes `CharSequence`, `start`, and `end` parameters as input.
     * 	- `CharSequence` is an interface in Java that represents a sequence of characters.
     * It has no explicit attributes.
     * 	- `start` and `end` represent the indices of the subsequence within the original
     * sequence that should be appended.
     * 
     * @param start 0-based index of the portion of the `charSequence` to be appended.
     * 
     * @param end 2nd to last character of the sequence to be appended.
     * 
     * @returns the same object instance.
     * 
     * The `append` function returns a reference to the same `Appendable` object instance,
     * indicating that it maintains the same state and can be used again in subsequent
     * method calls.
     * 
     * The function takes three parameters: `CharSequence charSequence`, `int start`, and
     * `int end`. These parameters specify the portion of the input sequence to which the
     * method should append the specified character or string.
     */
    @Override public Appendable append(CharSequence charSequence, int start, int end) {
      return this;
    }
    /**
     * returns a reference to the same object, allowing the method chaining and increasing
     * the efficiency of the code.
     * 
     * @param c character to be appended to the current value of the `Appendable` object.
     * 
     * @returns the same object, `this`.
     * 
     * 	- The function returns a reference to the same `Appendable` object, indicating
     * that the method is idempotent.
     * 	- The function has no side effects and does not modify any external state.
     * 	- The function always returns the same object reference, regardless of the input
     * value passed.
     * 	- The function has a neutral return type, which means it does not provide any
     * additional information about the output beyond the fact that it is an instance of
     * `Appendable`.
     */
    @Override public Appendable append(char c) {
      return this;
    }
  };

  public final CodeBlock fileComment;
  public final String packageName;
  public final TypeSpec typeSpec;
  public final boolean skipJavaLangImports;
  private final Set<String> staticImports;
  private final Set<String> alwaysQualify;
  private final String indent;

  private JavaFile(Builder builder) {
    this.fileComment = builder.fileComment.build();
    this.packageName = builder.packageName;
    this.typeSpec = builder.typeSpec;
    this.skipJavaLangImports = builder.skipJavaLangImports;
    this.staticImports = Util.immutableSet(builder.staticImports);
    this.indent = builder.indent;

    Set<String> alwaysQualifiedNames = new LinkedHashSet<>();
    fillAlwaysQualifiedNames(builder.typeSpec, alwaysQualifiedNames);
    this.alwaysQualify = Util.immutableSet(alwaysQualifiedNames);
  }

  /**
   * adds all the `alwaysQualifiedNames` of a given `TypeSpec` to a set and recursively
   * calls itself on each nested `TypeSpec`.
   * 
   * @param spec TypeSpec object that contains information about the types of a class,
   * interface, or method, and provides an opportunity to add always-qualified names
   * to the set of always-qualified names.
   * 
   * 	- `typeSpecs`: This property is an array of type specs, which contain information
   * about the types in the serialized data.
   * 	- `alwaysQualifiedNames`: This property is a set that contains the always-qualified
   * names for each type.
   * 
   * @param alwaysQualifiedNames set of qualified names to be added to the list of
   * always-qualified names during the function execution.
   * 
   * 	- It is a set that contains all the always qualified names extracted from the
   * given `TypeSpec` object `spec`.
   * 	- It also contains the always qualified names extracted from the nested `TypeSpecs`
   * objects through the recursive function call `fillAlwaysQualifiedNames(nested, alwaysQualifiedNames)`.
   */
  private void fillAlwaysQualifiedNames(TypeSpec spec, Set<String> alwaysQualifiedNames) {
    alwaysQualifiedNames.addAll(spec.alwaysQualifiedNames);
    for (TypeSpec nested : spec.typeSpecs) {
      fillAlwaysQualifiedNames(nested, alwaysQualifiedNames);
    }
  }

  /**
   * emits Java code to an `Appendable` output stream, importing relevant classes and
   * utilizing suggested imports for improved efficiency.
   * 
   * @param out Appendable object to which the code will be written.
   * 
   * 	- `out` is a `Appendable`, which means it has a `append()` method that can be
   * used to write text or binary data to a stream.
   * 	- `out` may be `NULL_APPENDABLE`, indicating that no output stream is provided
   * and the method must return an error.
   * 	- `indent` is a boolean value indicating whether the code should be indented or
   * not.
   * 	- `staticImports` is a map of import statements, which can be used to import
   * classes and packages statically.
   * 	- `alwaysQualify` is a boolean value indicating whether class names should always
   * be qualified with their enclosing package or not.
   */
  public void writeTo(Appendable out) throws IOException {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    CodeWriter importsCollector = new CodeWriter(
        NULL_APPENDABLE,
        indent,
        staticImports,
        alwaysQualify
    );
    emit(importsCollector);
    Map<String, ClassName> suggestedImports = importsCollector.suggestedImports();

    // Second pass: write the code, taking advantage of the imports.
    CodeWriter codeWriter
        = new CodeWriter(out, indent, suggestedImports, staticImports, alwaysQualify);
    emit(codeWriter);
  }

  /**
   * writes data to a specified directory using the `IOException` method `writeToPath`.
   * 
   * @param directory location where the output will be written.
   * 
   * 	- It is a `Path` object representing a file path or directory path.
   * 	- It has a type of `java.io.File` or `java.nio.file.Path`, depending on the type
   * of input provided.
   * 	- It may or may not be absolute, depending on the configuration of the application.
   */
  public void writeTo(Path directory) throws IOException {
    writeToPath(directory);
  }

  /**
   * writes data to a specified directory using a given character set.
   * 
   * @param directory location where the written content will be stored.
   * 
   * 	- `Path directory`: This represents a path to a directory where the output will
   * be written. The path can contain various attributes such as file separators,
   * devices, and symbolic links.
   * 	- `Charset charset`: This is the character encoding used for the output write
   * operation. It determines how the output data is represented in terms of bytes and
   * can affect the overall performance of the function.
   * 
   * @param charset character encoding to be used when writing the output file, allowing
   * the method to handle different types of files and data.
   * 
   * 	- The `Path` object represents a directory path.
   * 	- The `Charset` object encodes and decodes data using a particular character set.
   * It can have various attributes such as the encoding scheme, the character repertoire,
   * and the byte order.
   */
  public void writeTo(Path directory, Charset charset) throws IOException {
    writeToPath(directory, charset);
  }

  /**
   * writes a file to a specified directory using the given encoding.
   * 
   * @param directory location where the output file will be written.
   * 
   * 	- The `directory` parameter is a `Path` object representing a directory location
   * on the file system.
   * 	- The `UTF_8` argument indicates that the written data should be encoded in UTF-8
   * format.
   * 
   * @returns a `Path` object representing the written file.
   * 
   * The Path object `return writeToPath(directory, UTF_8)` represents a file path in
   * the local file system.
   * It has the attributes of being a directory or a file, which can be determined by
   * checking if the path ends with a forward slash `/`.
   * The Path also has the attribute of being encoded in UTF-8 format, indicating that
   * it is a text-based representation of the file path.
   */
  public Path writeToPath(Path directory) throws IOException {
    return writeToPath(directory, UTF_8);
  }

  /**
   * takes a directory and a charset as input, creates a new file with the given name
   * in the specified directory, and writes the provided code to it using the specified
   * charset.
   * 
   * @param directory directory where the Java file will be written.
   * 
   * 	- The `Files.notExists()` method is used to check if the specified directory
   * exists or not. If it does exist, an error message is displayed.
   * 	- The `Files.isDirectory()` method is used to verify if the directory is a directory
   * or not.
   * 	- The `packageName` variable holds the package name of the corresponding Java
   * class. It is split into individual components using the `split()` method and then
   * resolved to create directories in the output path.
   * 	- The `typeSpec.name` variable represents the name of the type specification in
   * the output path.
   * 
   * In summary, the `writeToPath` function checks if a directory exists or not, creates
   * directories if necessary, and writes the generated code to a file with the appropriate
   * name and format.
   * 
   * @param charset character encoding to be used when writing the generated Java code
   * to the output path.
   * 
   * 	- `Files.notExists()` checks whether the given path exists and is a directory.
   * If it exists but is not a directory, an exception is thrown.
   * 	- `packageName.isEmpty()` checks whether the package name is empty. If it is, an
   * exception is thrown.
   * 	- `split("\\.")` splits the package name into individual components separated by
   * dots (".").
   * 	- `createDirectories()` creates any necessary directories in the output directory.
   * 	- `OutputStreamWriter` writes the generated code to a new file in the output directory.
   * 
   * No summary is provided at the end of the explanation as it is not requested.
   * 
   * @returns a Path object representing the location of the generated Java file.
   * 
   * 	- The output is a `Path` object representing the location where the Java source
   * code was written.
   * 	- The path includes the directory where the code was written and the name of the
   * file (i.e., `typeSpec.name + ".java"`).
   * 	- The output is created using the `Files.newOutputStream()` method, which creates
   * a new output stream for writing to the specified path.
   * 	- The `Charset` parameter passed to the function is used to specify the character
   * encoding of the written code.
   */
  public Path writeToPath(Path directory, Charset charset) throws IOException {
    checkArgument(Files.notExists(directory) || Files.isDirectory(directory),
        "path %s exists but is not a directory.", directory);
    Path outputDirectory = directory;
    if (!packageName.isEmpty()) {
      for (String packageComponent : packageName.split("\\.")) {
        outputDirectory = outputDirectory.resolve(packageComponent);
      }
      Files.createDirectories(outputDirectory);
    }

    Path outputPath = outputDirectory.resolve(typeSpec.name + ".java");
    try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputPath), charset)) {
      writeTo(writer);
    }

    return outputPath;
  }

  /**
   * writes data to a specified directory or path, using the `IOException` class for
   * error handling.
   * 
   * @param directory file path where the output should be written.
   * 
   * 	- `directory`: This parameter is of type `File`, which represents a file or
   * directory on the local system. The `File` class has various methods and fields
   * that can be used to access its properties, such as the absolute path, name, size,
   * and last modified date.
   */
  public void writeTo(File directory) throws IOException {
    writeTo(directory.toPath());
  }

  /**
   * writes data to a file located at a specified directory path using the `IOException`
   * method.
   * 
   * @param directory directory where the file will be written.
   * 
   * 	- `toPath()` method is called to generate a `Path` object from the input `directory`.
   * This indicates that the `directory` variable is likely an instance of some class
   * that provides a `toPath()` method for converting it into a `Path` object.
   * 	- The `Path` object generated by `toPath()` represents a file or directory path,
   * which can be used to create a new file or write data to an existing one.
   * 
   * @returns a file located at the specified directory path.
   * 
   * The output is a `File` object representing the path of the written file.
   * The file path is represented as a `Path` object, which can be used to access various
   * attributes of the file system.
   * The `File` object provides direct access to the file's metadata and file system operations.
   */
  public File writeToFile(File directory) throws IOException {
    final Path outputPath = writeToPath(directory.toPath());
    return outputPath.toFile();
  }

  /**
   * writes Java source code to a file specified by the `filer`. The file name is
   * generated based on the package name and type specification, and the function checks
   * for errors before writing the code.
   * 
   * @param filer JavaFileObject that will be used to write the code.
   * 
   * 	- `filer`: This is an instance of the `Filer` class, which represents a file-like
   * object for writing Java source code to a file.
   * 	- `typeSpec`: This is an instance of the `TypeSpec` class, which contains information
   * about the type of the code being written, including its package name and name.
   * 	- `originatingElements`: This is a list of `Element` objects, representing the
   * elements that make up the code being written.
   * 	- `fileName`: This is a String object that represents the name of the file to be
   * created, which is either the package name plus the type name or just the type name
   * if the package name is empty.
   * 
   * The function then creates a `JavaFileObject` instance for the specified file name
   * using the `filer.createSourceFile()` method, and opens a writer on it using the
   * `openWriter()` method. The code is then written to the file using the `writeTo()`
   * method. If any exception occurs during writing, it is caught and handled by the
   * function, which deletes the created file if necessary.
   */
  public void writeTo(Filer filer) throws IOException {
    String fileName = packageName.isEmpty()
        ? typeSpec.name
        : packageName + "." + typeSpec.name;
    List<Element> originatingElements = typeSpec.originatingElements;
    JavaFileObject filerSourceFile = filer.createSourceFile(fileName,
        originatingElements.toArray(new Element[originatingElements.size()]));
    try (Writer writer = filerSourceFile.openWriter()) {
      writeTo(writer);
    } catch (Exception e) {
      try {
        filerSourceFile.delete();
      } catch (Exception ignored) {
      }
      throw e;
    }
  }

  /**
   * generates high-quality summaries of Java code by: (1) pushing a package, (2)
   * emitting comments, (3) emitting package statements, (4) emitting static import
   * statements, (5) skipping java.lang imports and unqualified types, and (6) emitting
   * type specifications.
   * 
   * @param codeWriter code generator API that will generate the Java code based on the
   * method's input parameters.
   * 
   * 	- `codeWriter`: A CodeWriter object that can be used to generate Java code. (Destructured)
   * 	- `packageName`: The name of the package associated with the current source code.
   * (String)
   * 	- `fileComment`: The file comment for the current source code. (String)
   * 	- `staticImports`: A set of static import statements for the current source code.
   * (Collection of String)
   * 	- `importedTypesCount`: The number of imported types for the current source code.
   * (Int)
   * 	- `typeSpec`: A TypeSpec object that represents the type information for the
   * current source code. (Object)
   */
  private void emit(CodeWriter codeWriter) throws IOException {
    codeWriter.pushPackage(packageName);

    if (!fileComment.isEmpty()) {
      codeWriter.emitComment(fileComment);
    }

    if (!packageName.isEmpty()) {
      codeWriter.emit("package $L;\n", packageName);
      codeWriter.emit("\n");
    }

    if (!staticImports.isEmpty()) {
      for (String signature : staticImports) {
        codeWriter.emit("import static $L;\n", signature);
      }
      codeWriter.emit("\n");
    }

    int importedTypesCount = 0;
    for (ClassName className : new TreeSet<>(codeWriter.importedTypes().values())) {
      // TODO what about nested types like java.util.Map.Entry?
      if (skipJavaLangImports
          && className.packageName().equals("java.lang")
          && !alwaysQualify.contains(className.simpleName)) {
        continue;
      }
      codeWriter.emit("import $L;\n", className.withoutAnnotations());
      importedTypesCount++;
    }

    if (importedTypesCount > 0) {
      codeWriter.emit("\n");
    }

    typeSpec.emit(codeWriter, null, Collections.emptySet());

    codeWriter.popPackage();
  }

  /**
   * compares an object to another object or null, checking for equivalence based on
   * class and string representation.
   * 
   * @param o object being compared to the current object, and is used to determine if
   * the two objects are equal.
   * 
   * 	- If `this` is equal to `o`, then `true` is returned.
   * 	- If `o` is null, then `false` is returned.
   * 	- If the classes of `this` and `o` are different, then `false` is returned.
   * 	- Otherwise, if the strings representing `this` and `o` are equal, then `true`
   * is returned.
   * 
   * @returns a boolean value indicating whether the object is equal to another object.
   * 
   * 	- The first `if` statement checks whether `this` and `o` refer to the same object.
   * If they do, then the method returns `true`.
   * 	- The second `if` statement checks whether `o` is `null`. If it is, then the
   * method returns `false`.
   * 	- The third `if` statement checks whether the classes of `this` and `o` are
   * different. If they are, then the method returns `false`.
   * 	- The final `if` statement compares the strings of `this` and `o` using the
   * `equals` method. If they match, then the method returns `true`. Otherwise, it
   * returns `false`.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * returns the hash code of its input, which is an instance of `toString`. This ensures
   * that two objects with the same toString value will have the same hash code.
   * 
   * @returns an integer value representing the hash code of the function's input.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of its internal state by calling the `writeTo`
   * method and returning the resulting string.
   * 
   * @returns a string representation of the object's state.
   */
  @Override public String toString() {
    try {
      StringBuilder result = new StringBuilder();
      writeTo(result);
      return result.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * creates a new `SimpleJavaFileObject` instance representing a source file with
   * specified name and package, and returns it.
   * 
   * @returns a Java file object that represents a source file.
   * 
   * 	- The `URI` created is of type `URI.create`, which constructs a new URI object
   * from a string representation of the form "scheme://path". In this case, the scheme
   * is assumed to be "file", and the path is constructed by combining the package name
   * with the type signature, separated by a forward slash, followed by the ".source"
   * extension.
   * 	- The `SimpleJavaFileObject` instance returned has various attributes:
   * 	+ `lastModified`: The last modified time of the file, which is set to the current
   * system time at the moment the function is called.
   * 	+ `getCharContent()`: Returns the contents of the file as a string, which is
   * simply the contents of the `JavaFile` instance itself.
   * 	+ `openInputStream()`: Opens an input stream for the file, which returns a byte
   * array representation of the file contents.
   * 	+ `getLastModified()`: Returns the last modified time of the file, which is the
   * same as the value stored in the `lastModified` attribute.
   */
  public JavaFileObject toJavaFileObject() {
    URI uri = URI.create((packageName.isEmpty()
        ? typeSpec.name
        : packageName.replace('.', '/') + '/' + typeSpec.name)
        + Kind.SOURCE.extension);
    return new SimpleJavaFileObject(uri, Kind.SOURCE) {
      private final long lastModified = System.currentTimeMillis();
      /**
       * returns a string representation of its invoking class, `JavaFile`. The method
       * ignores encoding errors when calling `toString()`.
       * 
       * @param ignoreEncodingErrors option to bypass encoding errors when returning the
       * String value.
       * 
       * @returns a string representation of the `JavaFile` object.
       */
      @Override public String getCharContent(boolean ignoreEncodingErrors) {
        return JavaFile.this.toString();
      }
      /**
       * creates an input stream from a byte array containing the contents of the Java class
       * file's character sequence.
       * 
       * @returns a byte array containing the contents of the file represented by the given
       * string.
       * 
       * 	- The input stream is an instance of `ByteArrayInputStream`.
       * 	- The underlying data is a byte array containing the contents of the character
       * sequence represented by the `getCharContent(true)` method.
       * 	- The byte array is encoded in UTF-8 format, indicating that it contains text
       * data in Unicode characters.
       */
      @Override public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(getCharContent(true).getBytes(UTF_8));
      }
      /**
       * retrieves and returns the last modified date of a file or resource.
       * 
       * @returns the last modified time of the entity, represented as a long value.
       */
      @Override public long getLastModified() {
        return lastModified;
      }
    };
  }

  /**
   * creates a new instance of the `Builder` class, passing in the package name and
   * type spec as parameters. It returns a new `Builder` instance with the provided
   * package name and type spec.
   * 
   * @param packageName name of the package that the `Builder` will be created for.
   * 
   * @param typeSpec TypeSpec object that contains the configuration for the generated
   * module.
   * 
   * 1/ The `packageName` parameter is validated by checking if it's null before passing
   * it to the constructor.
   * 2/ The `typeSpec` parameter is also validated by checking if it's null before
   * passing it to the constructor.
   * 3/ The `Builder` class is created with the passed `packageName` and `typeSpec`.
   * 
   * @returns a new `Builder` instance representing the given package name and TypeSpec.
   * 
   * 	- The Builder object returned has the package name and type spec as parameters.
   * 	- The checkNotNull methods are used to ensure that the input parameters are not
   * null before proceeding with the creation of the Builder object.
   * 	- The return statement creates a new Builder object with the given package name
   * and type spec.
   */
  public static Builder builder(String packageName, TypeSpec typeSpec) {
    checkNotNull(packageName, "packageName == null");
    checkNotNull(typeSpec, "typeSpec == null");
    return new Builder(packageName, typeSpec);
  }

  /**
   * generates a new instance of `Builder` with modified values for file comment, skip
   * Java Lang imports, and indentation level.
   * 
   * @returns a new `Builder` instance with updated values for `fileComment`,
   * `skipJavaLangImports`, and `indent`.
   * 
   * 	- The Builder object that is returned is an instance of the `Builder` class, which
   * is a generic class that can be used to build instances of various types, including
   * the one declared in the `packageName`.
   * 	- The `fileComment` attribute of the returned Builder object contains the file
   * comment associated with the current type being built.
   * 	- The `skipJavaLangImports` attribute indicates whether the builder should skip
   * importing the Java Lang packages or not. If set to `true`, the builder will ignore
   * the Java Lang imports, otherwise it will include them.
   * 	- The `indent` attribute represents the amount of indentation to be used for the
   * current type being built.
   */
  public Builder toBuilder() {
    Builder builder = new Builder(packageName, typeSpec);
    builder.fileComment.add(fileComment);
    builder.skipJavaLangImports = skipJavaLangImports;
    builder.indent = indent;
    return builder;
  }

  /**
   * in the provided file is a class that allows for customization of a JavaFile object.
   * It has several methods and fields that can be used to modify the behavior of the
   * generated code, such as adding file comments, skipping imports for classes in the
   * `java.lang` package, and setting the indentation level. The Builder Class provides
   * a flexible way to create a customized JavaFile object.
   */
  public static final class Builder {
    private final String packageName;
    private final TypeSpec typeSpec;
    private final CodeBlock.Builder fileComment = CodeBlock.builder();
    private boolean skipJavaLangImports;
    private String indent = "  ";

    public final Set<String> staticImports = new TreeSet<>();

    private Builder(String packageName, TypeSpec typeSpec) {
      this.packageName = packageName;
      this.typeSpec = typeSpec;
    }

    /**
     * adds a formatted comment to the current build configuration file. The format string
     * and optional arguments are used to generate the comment text. The updated build
     * configuration is returned.
     * 
     * @param format format of the file comment that will be added to the code by the
     * `addFileComment` method.
     * 
     * @returns a new Builder instance with the added file comment.
     * 
     * 	- `fileComment`: This is a list of file comments added to the current builder
     * instance. The list contains elements of type `String`, representing the formatted
     * comments for each file.
     * 	- `this`: Refers to the current builder instance, which is updated with the new
     * file comments added in the function.
     * 
     * The function takes two parameters: `format` and `args`. `format` is a string
     * parameter representing the format of the comment, while `args` is an array of
     * objects that contain additional information for the comment. The function adds
     * these elements to the `fileComment` list, allowing for dynamic comment formatting
     * based on user input.
     */
    public Builder addFileComment(String format, Object... args) {
      this.fileComment.add(format, args);
      return this;
    }

    /**
     * adds a static import to the builder, providing the fully qualified name of the
     * class and the constant's name.
     * 
     * @param constant Enum value that is being imported statically.
     * 
     * 	- `constant`: The input parameter is an instance of the `Enum` class, representing
     * a constant value.
     * 	- `DeclaringClass`: The declaring class of the constant value, which can be
     * retrieved using `ClassName.get(constant.getDeclaringClass())`.
     * 	- `Name`: The name of the constant value, which is equal to the string representation
     * of the constant value.
     * 
     * @returns a new builder instance with the added static import statement.
     * 
     * 	- The returned object is of type `Builder`.
     * 	- The method returns an instance of `Builder`, indicating that it can be used to
     * add more elements to the current build.
     * 	- The method takes two parameters: `constant`, which represents the `Enum` value
     * to be imported, and ` className`, which is the name of the class that contains the
     * `Enum`.
     */
    public Builder addStaticImport(Enum<?> constant) {
      return addStaticImport(ClassName.get(constant.getDeclaringClass()), constant.name());
    }

    /**
     * adds a static import to the class loader of the given class, providing the specified
     * names as the import statement.
     * 
     * @param clazz Class object that contains the static import to be added.
     * 
     * 	- The `Class<?>` type of the clazz parameter is mentioned in the method signature,
     * indicating that it represents an object of any class type.
     * 	- The `ClassName.get()` method is called on the `clazz` parameter to obtain its
     * fully qualified name as a string. This information is then passed to the
     * `addStaticImport()` method as part of the parameter list.
     * 
     * @returns a builder instance with the specified class and method names added to the
     * static imports.
     * 
     * The `Class<?>` parameter `clazz` represents the class to be statically imported.
     * The `String...` parameter `names` represents the names of the static imports to
     * be added to the class.
     * The return type of the function is a `Builder`, indicating that it is used to build
     * other objects in the Java programming language.
     */
    public Builder addStaticImport(Class<?> clazz, String... names) {
      return addStaticImport(ClassName.get(clazz), names);
    }

    /**
     * adds a static import to the builder instance, allowing for efficient and safe
     * access to classes and members within the Java project.
     * 
     * @param className fully qualified name of a class to be imported statically.
     * 
     * 	- `className`: The name of the class to be imported statically. It is passed as
     * a non-null reference to the function.
     * 	- `names`: An array of strings representing the names of the members to be imported
     * statically. It is passed as a non-null reference to the function.
     * 	- `staticImports`: A collection of string arrays representing the fully qualified
     * names of the static imports. It is modified in the function body to include the
     * new entries.
     * 
     * @returns a builder instance with added static imports.
     * 
     * 	- The returned output is an instance of the `Builder` class, indicating that the
     * method is meant to be called repeatedly to build a customized version of the target
     * object.
     * 	- The `this` keyword in the return statement indicates that the method returns a
     * reference to the same `Builder` object on which it was called, allowing for chaining
     * of methods.
     * 	- The `checkArgument` methods are used throughout the code to validate input
     * arguments against certain conditions, ensuring that the method is called with valid
     * inputs. These methods take no argument and simply throw an exception if the condition
     * is not met.
     * 	- The `staticImports` field is a collection of import statements for classes and
     * packages that are added to the builder object during construction. This field is
     * not modified by the `addStaticImport` method, but rather serves as a storage area
     * for the imported classes and packages.
     */
    public Builder addStaticImport(ClassName className, String... names) {
      checkArgument(className != null, "className == null");
      checkArgument(names != null, "names == null");
      checkArgument(names.length > 0, "names array is empty");
      for (String name : names) {
        checkArgument(name != null, "null entry in names array: %s", Arrays.toString(names));
        staticImports.add(className.canonicalName + "." + name);
      }
      return this;
    }

    /**
     * modifies a builder instance by setting the `skipJavaLangImports` field to the input
     * parameter.
     * 
     * @param skipJavaLangImports boolean value that controls whether or not to skip Java
     * language imports in the builder's construction process.
     * 
     * @returns a modified builder object with the `skipJavaLangImports` field set to the
     * provided value.
     * 
     * 	- `this.skipJavaLangImports`: This is a boolean value indicating whether to skip
     * Java lang imports or not.
     */
    public Builder skipJavaLangImports(boolean skipJavaLangImports) {
      this.skipJavaLangImports = skipJavaLangImports;
      return this;
    }

    /**
     * modifies the `Builder` instance by setting its `indent` field to the given string
     * value, allowing for easy manipulation of the builder's indentation level.
     * 
     * @param indent indentation level for the builder object, which is then applied to
     * the resulting Java code.
     * 
     * @returns a modified builder object with the specified indentation level applied
     * to its internal state.
     * 
     * The `this` keyword in the function signature indicates that the builder is being
     * updated with a new property value.
     * 
     * The `String indent` parameter represents the new value for the `indent` property
     * of the builder.
     */
    public Builder indent(String indent) {
      this.indent = indent;
      return this;
    }

    /**
     * creates a new instance of `JavaFile`.
     * 
     * @returns a new `JavaFile` instance representing the current class.
     * 
     * 	- The `build` function returns a `JavaFile` object that represents the compiled
     * Java file.
     * 	- The `JavaFile` object contains information about the source code and its
     * compilation, including the generated bytecode and any errors or warnings encountered
     * during the compilation process.
     * 	- The `JavaFile` object can be used to access various attributes of the compiled
     * Java file, such as the class name, package, and source file path.
     */
    public JavaFile build() {
      return new JavaFile(this);
    }
  }
}
