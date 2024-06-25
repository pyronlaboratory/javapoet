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
     * Returns a reference to the same object, allowing chaining of method calls and
     * convenience for building complex content.
     * 
     * @param charSequence sequence of characters to be appended to the current state of
     * the `Appendable` object.
     * 
     * @returns the same object instance.
     */
    @Override public Appendable append(CharSequence charSequence) {
      return this;
    }
    /**
     * Returns a reference to itself after appending the provided `CharSequence` to the
     * current instance, starting from the specified `start` index and ending at the `end`
     * index.
     * 
     * @param charSequence sequence of characters to be appended to the current buffer.
     * 
     * @param start 0-based index of the portion of the `charSequence` to be appended.
     * 
     * @param end 2-based index of the last character in the sequence that should be
     * appended to the current sequence.
     * 
     * @returns the same instance of `Appendable`.
     */
    @Override public Appendable append(CharSequence charSequence, int start, int end) {
      return this;
    }
    /**
     * Returns a reference to the same instance, allowing for chaining of method calls.
     * 
     * @param c character to be appended to the current contents of the `Appendable`
     * object, and the function returns the resulting updated object.
     * 
     * @returns the same object reference, denoted by the keyword `this`.
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
   * Adds all the 'always qualified names' from a type spec to a set and recursively
   * calls itself for each nested type spec to add its always qualified names to the set.
   * 
   * @param spec TypeSpec object that contains the information about the type being
   * qualified, including its always qualified names.
   * 
   * @param alwaysQualifiedNames set of names to be filled with qualified names from
   * the `TypeSpec` objects passed as arguments to the `fillAlwaysQualifiedNames` function.
   */
  private void fillAlwaysQualifiedNames(TypeSpec spec, Set<String> alwaysQualifiedNames) {
    alwaysQualifiedNames.addAll(spec.alwaysQualifiedNames);
    for (TypeSpec nested : spec.typeSpecs) {
      fillAlwaysQualifiedNames(nested, alwaysQualifiedNames);
    }
  }

  /**
   * Emits code to an `Appendable` object for output, using imports collected during a
   * previous pass to simplify the writing process.
   * 
   * @param out `Appendable` object to which the code will be written.
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
   * Writes data to a specified directory path.
   * 
   * @param directory location where the code will write its output, and it is of type
   * `Path`.
   */
  public void writeTo(Path directory) throws IOException {
    writeToPath(directory);
  }

  /**
   * Writes the contents of a given `CharSequence` to a specified directory using the
   * specified `Charset`.
   * 
   * @param directory location where the generated documentation will be saved.
   * 
   * @param charset character encoding used for writing the output to the specified directory.
   */
  public void writeTo(Path directory, Charset charset) throws IOException {
    writeToPath(directory, charset);
  }

  /**
   * Writes data to a file path using the specified encoding.
   * 
   * @param directory directory where the output should be written.
   * 
   * @returns a path object representing the written data.
   */
  public Path writeToPath(Path directory) throws IOException {
    return writeToPath(directory, UTF_8);
  }

  /**
   * Writes the given `typeSpec` to a file with the specified path, checking that the
   * path exists and is a directory before creating it if necessary.
   * 
   * @param directory path where the generated Java class will be saved.
   * 
   * @param charset character set to use when writing the Java file.
   * 
   * @returns a Path object representing the location where the Java source file was written.
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
   * Writes code to a specified directory or its corresponding path.
   * 
   * @param directory destination path where the code wants to write its output.
   */
  public void writeTo(File directory) throws IOException {
    writeTo(directory.toPath());
  }

  /**
   * Writes data to a file located at the specified directory path using the `Path`
   * class in Java.
   * 
   * @param directory directory where the file will be written.
   * 
   * @returns a File object representing the path where the contents were written to disk.
   */
  public File writeToFile(File directory) throws IOException {
    final Path outputPath = writeToPath(directory.toPath());
    return outputPath.toFile();
  }

  /**
   * Writes the code to a file using the `Filer` object, based on the type specification
   * and package name provided.
   * 
   * @param filer JavaFileObject that will be used to write the source code for the
   * given type specification.
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
   * Generates high-quality documentation for given code by:
   * 
   * 1/ Pushing the package name to a writer.
   * 2/ Emitting comments and package statements based on the `fileComment` and `packageName`.
   * 3/ Emitting static imports based on the `staticImports`.
   * 4/ Skipping Java Lang imports if necessary.
   * 5/ Emitting type import statements based on the `importedTypes`.
   * 
   * @param codeWriter code generator writer that emits the generated code to a file.
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
   * Compares an object to another object or itself, and returns a boolean value
   * indicating whether they are equal.
   * 
   * @param o object to be compared with the current object for equality checking.
   * 
   * @returns a boolean value indicating whether the object passed as argument is equal
   * to the current object.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * In the provided codebase returns the hash code of the class's `toString()` method
   * output, which is the string representation of the object.
   * 
   * @returns an integer representing the hash code of the object's string representation.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Generates high-quality documentation for given code by using a `StringBuilder` to
   * write out the code and then returning the resulting string.
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
   * Generates a `SimpleJavaFileObject` instance that represents a Java source file.
   * The created object has methods for returning the file's contents, last modified
   * date, and an input stream for reading the file's bytes.
   * 
   * @returns a `SimpleJavaFileObject` instance that represents the given code as a
   * Java file.
   */
  public JavaFileObject toJavaFileObject() {
    URI uri = URI.create((packageName.isEmpty()
        ? typeSpec.name
        : packageName.replace('.', '/') + '/' + typeSpec.name)
        + Kind.SOURCE.extension);
    return new SimpleJavaFileObject(uri, Kind.SOURCE) {
      private final long lastModified = System.currentTimeMillis();
      /**
       * Returns a string representation of the class `JavaFile`. When the `ignoreEncodingErrors`
       * parameter is set to `true`, any encoding errors are ignored during the conversion
       * to a string.
       * 
       * @param ignoreEncodingErrors boolean value that enables or disables encoding errors
       * ignoring during the creation of the code documentation.
       * 
       * @returns a string representation of the `JavaFile` object in question.
       */
      @Override public String getCharContent(boolean ignoreEncodingErrors) {
        return JavaFile.this.toString();
      }
      /**
       * Creates an input stream from the character content of a class, encoding it as UTF-8
       * bytes and returning the resulting input stream.
       * 
       * @returns a binary input stream containing the encoded byte array of the given
       * code's character content.
       */
      @Override public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(getCharContent(true).getBytes(UTF_8));
      }
      /**
       * Retrieves the last modified date of a code unit.
       * 
       * @returns a long representing the last modification time of the code.
       */
      @Override public long getLastModified() {
        return lastModified;
      }
    };
  }

  /**
   * Creates a new `Builder` instance with the specified package name and TypeSpec object.
   * 
   * @param packageName name of the package to which the `TypeSpec` instance will belong.
   * 
   * @param typeSpec TypeSpec object that contains information about the Kotlin class
   * or object that is to be generated by the builder.
   * 
   * @returns a new `Builder` instance representing the specified package name and type
   * specification.
   */
  public static Builder builder(String packageName, TypeSpec typeSpec) {
    checkNotNull(packageName, "packageName == null");
    checkNotNull(typeSpec, "typeSpec == null");
    return new Builder(packageName, typeSpec);
  }

  /**
   * Creates a new `Builder` instance with the same package name, type specification,
   * file comment, skip Java Lang imports and indentation level as the original instance.
   * 
   * @returns a new `Builder` instance with updated values for `fileComment`,
   * `skipJavaLangImports`, and `indent`.
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
     * Adds a file comment to the builder instance by formatting the comment with the
     * specified format and passing the arguments.
     * 
     * @param format template for the file comment to be generated.
     * 
     * @param args 0 or more arguments to be formatted and added as a comment to the file.
     * 
     * @returns a new builder instance with a added file comment in the specified format
     * and arguments.
     */
    public Builder addFileComment(String format, Object... args) {
      this.fileComment.add(format, args);
      return this;
    }

    /**
     * Adds a static import for an Enum constant, providing the class name and constant
     * name as arguments.
     * 
     * @param constant Enum value that should be statically imported.
     * 
     * @returns a `Builder` object with the added static import.
     */
    public Builder addStaticImport(Enum<?> constant) {
      return addStaticImport(ClassName.get(constant.getDeclaringClass()), constant.name());
    }

    /**
     * Adds static imports to a code, using the given class and import names.
     * 
     * @param clazz Class object to be statically imported.
     * 
     * @param names 1-based indices of the static import statements to be added to the
     * class, as a list of strings.
     * 
     * @returns a `Builder` instance with the specified class and import names added to
     * its static imports.
     */
    public Builder addStaticImport(Class<?> clazz, String... names) {
      return addStaticImport(ClassName.get(clazz), names);
    }

    /**
     * Adds a list of static imports to the builder, ensuring each entry is not null and
     * exists in the names array.
     * 
     * @param className class name for which static imports are to be added.
     * 
     * @param names 0 or more non-null string values that are used to add static imports
     * to the Builder object.
     * 
     * @returns a Builder object representing the original code, with additional static
     * imports added to the class.
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
     * Sets the `skipJavaLangImports` field of the current builder instance to a given
     * `boolean` value, allowing for customization of the import handling behavior.
     * 
     * @param skipJavaLangImports boolean value that determines whether or not to skip
     * Java language imports when generating high-quality documentation for the given code.
     * 
     * @returns a Builder instance with the skipJavaLangImports property set to the input
     * argument.
     */
    public Builder skipJavaLangImports(boolean skipJavaLangImports) {
      this.skipJavaLangImports = skipJavaLangImports;
      return this;
    }

    /**
     * Modifies the `Builder` instance by setting its `indent` field to the provided
     * string value, allowing further method calls on the modified Builder instance.
     * 
     * @param indent 0 or more spaces to add to the beginning of each line of text generated
     * by the builder.
     * 
     * @returns a reference to the current builder instance with the provided indentation
     * level applied.
     */
    public Builder indent(String indent) {
      this.indent = indent;
      return this;
    }

    /**
     * Creates a new `JavaFile` object representing the current class file.
     * 
     * @returns a new `JavaFile` object representing the current class file.
     */
    public JavaFile build() {
      return new JavaFile(this);
    }
  }
}
