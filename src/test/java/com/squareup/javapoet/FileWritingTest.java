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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

/**
 * tests various aspects of the JavaFile class, including:
 * 
 * 	- Writing a file to disk and verifying its existence
 * 	- Including nested classes in the file
 * 	- Passing originating elements to the file
 * 	- Handling classes with tab indentation
 * 	- Verifying that the file is encoded in UTF-8.
 */
@RunWith(JUnit4.class)
public final class FileWritingTest {
  // Used for testing java.io File behavior.
  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  // Used for testing java.nio.file Path behavior.
  private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
  private final Path fsRoot = fs.getRootDirectories().iterator().next();

  // Used for testing annotation processor Filer behavior.
  private final TestFiler filer = new TestFiler(fs, fsRoot);

  /**
   * tests if an existing file path can be written to as a JavaFile object, failing the
   * test with an IllegalArgumentException if the path is not a directory.
   */
  @Test public void pathNotDirectory() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile javaFile = JavaFile.builder("example", type).build();
    Path path = fs.getPath("/foo/bar");
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    try {
      javaFile.writeTo(path);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("path /foo/bar exists but is not a directory.");
    }
  }

  /**
   * tests whether an existing file can be written to using the `JavaFile.writeTo()`
   * method, fails if the file exists but is not a directory.
   */
  @Test public void fileNotDirectory() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile javaFile = JavaFile.builder("example", type).build();
    File file = new File(tmp.newFolder("foo"), "bar");
    file.createNewFile();
    try {
      javaFile.writeTo(file);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(
          "path " + file.getPath() + " exists but is not a directory.");
    }
  }

  /**
   * verifies that a Java file with the specified name and package exists in the default
   * package directory.
   */
  @Test public void pathDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(fsRoot);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  /**
   * writes a Java file to a temporary directory, creating a new class with the specified
   * name and package.
   */
  @Test public void fileDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(tmp.getRoot());

    File testFile = new File(tmp.getRoot(), "Test.java");
    assertThat(testFile.exists()).isTrue();
  }

  /**
   * writes a Java class file to a file located at a specific path using the
   * `JavaFile.builder()` method, and then verifies that the file exists using the
   * `Files.exists()` method.
   */
  @Test public void filerDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(filer);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  /**
   * validates the existence of three Java classes: "foo", "foo.bar", and "foo.bar.baz"
   * within a root directory specified by `fsRoot`.
   */
  @Test public void pathNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(fsRoot);
    JavaFile.builder("foo.bar", type).build().writeTo(fsRoot);
    JavaFile.builder("foo.bar.baz", type).build().writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  /**
   * creates three nested Java classes: `Test`, `bar.Test`, and `bar.baz.Test`. It then
   * checks if the corresponding files exist in a temporary directory.
   */
  @Test public void fileNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(tmp.getRoot());
    JavaFile.builder("foo.bar", type).build().writeTo(tmp.getRoot());
    JavaFile.builder("foo.bar.baz", type).build().writeTo(tmp.getRoot());

    File fooDir = new File(tmp.getRoot(), "foo");
    File fooFile = new File(fooDir, "Test.java");
    File barDir = new File(fooDir, "bar");
    File barFile = new File(barDir, "Test.java");
    File bazDir = new File(barDir, "baz");
    File bazFile = new File(bazDir, "Test.java");
    assertThat(fooFile.exists()).isTrue();
    assertThat(barFile.exists()).isTrue();
    assertThat(bazFile.exists()).isTrue();
  }

  /**
   * writes three Java files to a file system, and then checks that each file exists.
   */
  @Test public void filerNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(filer);
    JavaFile.builder("foo.bar", type).build().writeTo(filer);
    JavaFile.builder("foo.bar.baz", type).build().writeTo(filer);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  /**
   * tests whether a filer preserves the originating elements of the classes it serializes.
   * It creates two test classes with different originating elements and writes their
   * bytecode to a filer, then checks if the filer correctly preserved the originating
   * elements when reading them back.
   */
  @Test public void filerPassesOriginatingElements() throws IOException {
    Element element1_1 = Mockito.mock(Element.class);
    TypeSpec test1 = TypeSpec.classBuilder("Test1")
        .addOriginatingElement(element1_1)
        .build();

    Element element2_1 = Mockito.mock(Element.class);
    Element element2_2 = Mockito.mock(Element.class);
    TypeSpec test2 = TypeSpec.classBuilder("Test2")
        .addOriginatingElement(element2_1)
        .addOriginatingElement(element2_2)
        .build();

    JavaFile.builder("example", test1).build().writeTo(filer);
    JavaFile.builder("example", test2).build().writeTo(filer);

    Path testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.java"));
    assertThat(filer.getOriginatingElements(testPath1)).containsExactly(element1_1);
    Path testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.java"));
    assertThat(filer.getOriginatingElements(testPath2)).containsExactly(element2_1, element2_2);
  }

  /**
   * generates a Java file with a single class named `Test`. The class has a field and
   * a main method that prints "Hello World!" to the console.
   */
  @Test public void filerClassesWithTabIndent() throws IOException {
    TypeSpec test = TypeSpec.classBuilder("Test")
        .addField(Date.class, "madeFreshDate")
        .addMethod(MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String[].class, "args")
            .addCode("$T.out.println($S);\n", System.class, "Hello World!")
            .build())
        .build();
    JavaFile.builder("foo", test).indent("\t").build().writeTo(filer);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    String source = new String(Files.readAllBytes(fooPath));

    assertThat(source).isEqualTo(""
        + "package foo;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.lang.System;\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Test {\n"
        + "\tDate madeFreshDate;\n"
        + "\n"
        + "\tpublic static void main(String[] args) {\n"
        + "\t\tSystem.out.println(\"Hello World!\");\n"
        + "\t}\n"
        + "}\n");
  }

  /**
   * verifies that a Java file contains the correct character encoding ("UTF-8") and
   * contents ("// Pi\u00f1ata\u00a1").
   */
  @Test public void fileIsUtf8() throws IOException {
    JavaFile javaFile = JavaFile.builder("foo", TypeSpec.classBuilder("Taco").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build();
    javaFile.writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Taco.java"));
    assertThat(new String(Files.readAllBytes(fooPath), UTF_8)).isEqualTo(""
        + "// Pi\u00f1ata\u00a1\n"
        + "package foo;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * converts a Java file into a Path object, representing the file's location on disk.
   */
  @Test public void writeToPathReturnsPath() throws IOException {
    JavaFile javaFile = JavaFile.builder("foo", TypeSpec.classBuilder("Taco").build()).build();
    Path filePath = javaFile.writeToPath(fsRoot);
    // Cast to avoid ambiguity between assertThat(Path) and assertThat(Iterable<?>)
    assertThat((Iterable<?>) filePath).isEqualTo(fsRoot.resolve(fs.getPath("foo", "Taco.java")));
  }
}
