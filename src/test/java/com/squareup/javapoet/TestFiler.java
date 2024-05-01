/*
 * Copyright (C) 2014 Square, Inc.
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
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * is a Java class that implements Filer interface and provides functionality for
 * creating source files, class files, and resources. It has several fields and
 * methods, including a map to store originating elements, createSourceFile method
 * to create source files, createClassFile method to create class files, and
 * createResource method to create resources.
 */
final class TestFiler implements Filer {
  /**
   * is a Java class that extends the SimpleJavaFileObject class and provides an output
   * stream for writing to a file. The class has a Path field representing the location
   * of the file, and a single method, openOutputStream(), which creates a new output
   * stream for writing to the file if it does not already exist in the file system.
   */
  class Source extends SimpleJavaFileObject {
    private final Path path;
    protected Source(Path path) {
      super(path.toUri(), Kind.SOURCE);
      this.path = path;
    }
    /**
     * creates an output stream for a given path, ensuring the parent directory exists
     * and is created if necessary before returning the output stream.
     * 
     * @returns an `OutputStream` object that can be used to write data to a specific
     * file location.
     * 
     * 	- The output is an instance of `OutputStream`, which represents a stream for
     * writing data to a file.
     * 	- The output is returned as the result of creating a new output stream using the
     * `fileSystemProvider.newOutputStream(path)` method.
     * 	- The path provided in the function is used to determine the location where the
     * output will be written.
     * 	- If the parent directory of the path does not exist, it is created by calling `fileSystemProvider.createDirectory(parent)`.
     */
    @Override public OutputStream openOutputStream() throws IOException {
      Path parent = path.getParent();
      if (!Files.exists(parent)) fileSystemProvider.createDirectory(parent);
      return fileSystemProvider.newOutputStream(path);
    }
  }

  private final String separator;
  private final Path fileSystemRoot;
  private final FileSystemProvider fileSystemProvider;
  private final Map<Path, Set<Element>> originatingElementsMap;

  public TestFiler(FileSystem fileSystem, Path fsRoot) {
    separator = fileSystem.getSeparator();
    fileSystemRoot = fsRoot;
    fileSystemProvider = fileSystem.provider();
    originatingElementsMap = new LinkedHashMap<>();
  }

  /**
   * returns a set of elements that originate from a given path.
   * 
   * @param path Java package, class and method that originated the element.
   * 
   * Path is a `com.example.domain.Path` object representing a file or directory path.
   * It has attributes such as `segments`, which is an array of `com.example.domain.Segment`
   * objects representing the constituent parts of the path, and `segmentCount`, which
   * represents the total number of segments in the path.
   * 
   * @returns a set of elements associated with the specified path.
   * 
   * 	- `Set<Element>` represents a set of elements that originate from the path passed
   * as an argument to the function.
   * 	- `originatingElementsMap` is the map that stores the elements associated with
   * each path in the program.
   * 	- The `get()` method is used to retrieve the set of elements corresponding to a
   * given path from the `originatingElementsMap`.
   */
  public Set<Element> getOriginatingElements(Path path) {
    return originatingElementsMap.get(path);
  }

  /**
   * creates a new Java source file based on a given name and originating elements, and
   * stores the relationship between the file path and the originating elements in a
   * map for later use.
   * 
   * @param name name of the source file to be created, which is used to construct the
   * absolute path of the file.
   * 
   * 	- `name`: A `CharSequence` object representing the name of the source file to be
   * created.
   * 	- `separator`: A String used to separate the name of the source file from its path.
   * 
   * @returns a `Source` object representing the generated Java source file.
   * 
   * 	- The `JavaFileObject` returned is an instance of the `Source` class, which
   * represents a source file in Java.
   * 	- The `name` parameter is the name of the source file, which is used to generate
   * the file path.
   * 	- The `originatingElements` parameter is a list of elements that originated from
   * the `createSourceFile` function, which are stored in a map for future reference.
   * 	- The `fileSystemRoot` parameter is a Path object representing the root directory
   * of the file system where the source file will be saved.
   * 	- The `separator` parameter is used to separate the name of the source file from
   * its path, assuming a well-formed path structure.
   */
  @Override public JavaFileObject createSourceFile(
      CharSequence name, Element... originatingElements) throws IOException {
    String relative = name.toString().replace(".", separator) + ".java"; // Assumes well-formed.
    Path path = fileSystemRoot.resolve(relative);
    originatingElementsMap.put(path, Util.immutableSet(Arrays.asList(originatingElements)));
    return new Source(path);
  }

  /**
   * generates a Java class file with the given name and originating elements, but it
   * is not implemented and throws an `UnsupportedOperationException`.
   * 
   * @param name name of the class file to be created.
   * 
   * 	- `CharSequence`: This indicates that `name` is a sequence of characters, which
   * can be a string or any other type of character sequence.
   * 	- `Element... originatingElements`: This parameter list represents an array of
   * elements, which could be any type of Java element such as classes, interfaces,
   * fields, or methods.
   * 
   * @returns an unsupported operation exception.
   * 
   * 	- The `name` parameter is a CharSequence that represents the name of the class
   * file to be created.
   * 	- The `originatingElements` parameter is an array of Element objects representing
   * the elements from which the class file was generated.
   * 	- The function throws an IOException if any error occurs during the creation of
   * the class file.
   */
  @Override public JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
      throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }

  /**
   * is a placeholder that indicates the method is not implemented and cannot be used
   * to create resources.
   * 
   * @param location location where the resource is to be created, which is used to
   * determine the appropriate FileObject instance to return.
   * 
   * 	- `Location`: This parameter represents a location in the source code, which can
   * be a package name or a simple name.
   * 	- `pkg`: This is a string representation of the package name associated with the
   * location.
   * 	- `relativeName`: This is a string representing the relative name of the resource
   * within its containing package.
   * 	- `originatingElements`: This parameter represents an array of Element objects
   * that contain information about the resource's origin, such as the Java file or
   * directory where it was found.
   * 
   * @param pkg package name of the resource being created.
   * 
   * 	- `pkg`: A string representing the package name of the resource being created.
   * 	- `relativeName`: A string representing the relative path to the resource within
   * its package.
   * 
   * @param relativeName name of the resource to be created relative to the package of
   * the Java file being managed.
   * 
   * 	- `pkg`: The package name of the resource being created.
   * 	- `relativeName`: A string representing the relative path to the resource within
   * the package.
   * 	- `originatingElements`: An array of elements that provide context for the creation
   * of the resource, such as a method invocation or field access.
   * 
   * @returns an unsupported operation exception.
   * 
   * 	- `location`: The location where the resource was created, which is an instance
   * of `JavaFileManager.Location`.
   * 	- `pkg`: The package name of the resource, represented as a `CharSequence`.
   * 	- `relativeName`: The relative name of the resource within its package, also
   * represented as a `CharSequence`.
   * 	- `originatingElements`: An array of elements that originated the creation of the
   * resource, represented as an array of `Element` objects.
   */
  @Override public FileObject createResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName, Element... originatingElements) throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }

  /**
   * is intended to retrieve a resource from a specified location, but it throws an
   * `UnsupportedOperationException` instead, indicating that the operation is not implemented.
   * 
   * @param location location of the resource being requested, which is used to determine
   * the appropriate FileObject to return.
   * 
   * Location refers to the place in a file system where a resource is stored. The
   * package and relative name specify which resource is being requested.
   * 
   * @param pkg package name of the resource being requested.
   * 
   * 	- `pkg` is a CharSequence that represents the package name of the resource.
   * 	- It can be null or empty if no package is specified for the resource.
   * 	- If `pkg` is not null, it contains the fully qualified package name of the resource.
   * 	- The package name can include any valid Java identifier characters (letters,
   * digits, and underscores) separated by dots.
   * 
   * @param relativeName name of the resource to be retrieved, which is used to locate
   * the corresponding file within the package's resources.
   * 
   * 	- The `CharSequence` type indicates that the object can be any sequence of
   * characters, including strings, numbers, and special symbols.
   * 	- The method throws an instance of `IOException`, indicating that it may encounter
   * errors during resource retrieval.
   * 	- The exception message "Not implemented." suggests that the function does not
   * currently support the requested operation and should not be called.
   * 
   * @returns an UnsupportedOperationException with the message "Not implemented."
   * 
   * The output is an instance of `FileObject`, which represents a file or directory
   * within a Java module.
   * The `Location` parameter indicates the location of the resource, which can be
   * either a package or a relative name within that package.
   * The `pkg` parameter is a sequence of characters representing the package where the
   * resource is located, while `relativeName` is a sequence of characters representing
   * the name of the resource relative to the package.
   */
  @Override public FileObject getResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName) throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }
}
