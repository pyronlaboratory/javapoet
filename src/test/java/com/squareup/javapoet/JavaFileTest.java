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

import java.io.File;
import com.google.testing.compile.CompilationRule;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * is used to test the generation of Java files by the Poet library. It contains a
 * number of methods that generate code for various scenarios, including interfaces,
 * classes, and inheritance. The tests cover different cases such as avoiding clashes
 * between parent and child classes, superclasses and interfaces, and map entries.
 * The generated code is checked to ensure that it does not contain any errors or inconsistencies.
 */
@RunWith(JUnit4.class)
public final class JavaFileTest {

  @Rule public final CompilationRule compilation = new CompilationRule();

  /**
   * retrieves a `TypeElement` object representing a class or interface from the
   * compilation's elements set based on the given class name.
   * 
   * @param clazz Class<?> object that the function is called with, and it is used to
   * locate the corresponding TypeElement in the compilation's Elements collection.
   * 
   * 	- `compilation`: This is a reference to an object representing the compilation
   * of types in the Java programming language.
   * 	- `Elements`: This refers to a collection of type elements, which are the fundamental
   * units of typing in the Java programming language.
   * 	- `getTypeElement`: This method returns a specific type element within the
   * `Elements` collection based on the canonical name of the class represented by `clazz`.
   * 
   * @returns a `TypeElement` object representing the type of the given class.
   * 
   * The TypeElement object represents a type in the Java programming language, which
   * is returned by the function call. The type may be an interface, class, or other
   * type-related construct. The TypeElement object contains information about the
   * type's name, kind, and other attributes, such as its enclosing scope and any
   * annotations it may have.
   * 
   * The function returns a TypeElement object after obtaining it from the compilation's
   * Elements collection, which contains all types declared in the code being compiled.
   * The method uses the canonical name of the class to retrieve the appropriate
   * TypeElement object from the Elements collection.
   */
  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  /**
   * creates a new list of Hoverboards and adds three elements to it using the
   * `createNimbus` method from the `Hoverboard` class, followed by sorting the list
   * using the `sort` method from the `Collections` class. It then returns the list of
   * Hoverboards.
   */
  @Test public void importStaticReadmeExample() {
    ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
    ClassName namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards");
    ClassName list = ClassName.get("java.util", "List");
    ClassName arrayList = ClassName.get("java.util", "ArrayList");
    TypeName listOfHoverboards = ParameterizedTypeName.get(list, hoverboard);
    MethodSpec beyond = MethodSpec.methodBuilder("beyond")
        .returns(listOfHoverboards)
        .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
        .addStatement("result.add($T.createNimbus(2000))", hoverboard)
        .addStatement("result.add($T.createNimbus(\"2001\"))", hoverboard)
        .addStatement("result.add($T.createNimbus($T.THUNDERBOLT))", hoverboard, namedBoards)
        .addStatement("$T.sort(result)", Collections.class)
        .addStatement("return result.isEmpty() ? $T.emptyList() : result", Collections.class)
        .build();
    TypeSpec hello = TypeSpec.classBuilder("HelloWorld")
        .addMethod(beyond)
        .build();
    JavaFile example = JavaFile.builder("com.example.helloworld", hello)
        .addStaticImport(hoverboard, "createNimbus")
        .addStaticImport(namedBoards, "*")
        .addStaticImport(Collections.class, "*")
        .build();
    assertThat(example.toString()).isEqualTo(""
        + "package com.example.helloworld;\n"
        + "\n"
        + "import static com.mattel.Hoverboard.Boards.*;\n"
        + "import static com.mattel.Hoverboard.createNimbus;\n"
        + "import static java.util.Collections.*;\n"
        + "\n"
        + "import com.mattel.Hoverboard;\n"
        + "import java.util.ArrayList;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class HelloWorld {\n"
        + "  List<Hoverboard> beyond() {\n"
        + "    List<Hoverboard> result = new ArrayList<>();\n"
        + "    result.add(createNimbus(2000));\n"
        + "    result.add(createNimbus(\"2001\"));\n"
        + "    result.add(createNimbus(THUNDERBOLT));\n"
        + "    sort(result);\n"
        + "    return result.isEmpty() ? emptyList() : result;\n"
        + "  }\n"
        + "}\n");
  }
  /**
   * imports static blocks for various crazy formats, including nested classes, inner
   * classes, and anonymous classes.
   */
  @Test public void importStaticForCrazyFormatsWorks() {
    MethodSpec method = MethodSpec.methodBuilder("method").build();
    JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("$T", Runtime.class)
                .addStatement("$T.a()", Runtime.class)
                .addStatement("$T.X", Runtime.class)
                .addStatement("$T$T", Runtime.class, Runtime.class)
                .addStatement("$T.$T", Runtime.class, Runtime.class)
                .addStatement("$1T$1T", Runtime.class)
                .addStatement("$1T$2L$1T", Runtime.class, "?")
                .addStatement("$1T$2L$2S$1T", Runtime.class, "?")
                .addStatement("$1T$2L$2S$1T$3N$1T", Runtime.class, "?", method)
                .addStatement("$T$L", Runtime.class, "?")
                .addStatement("$T$S", Runtime.class, "?")
                .addStatement("$T$N", Runtime.class, method)
                .build())
            .build())
        .addStaticImport(Runtime.class, "*")
        .build()
        .toString(); // don't look at the generated code...
  }

  /**
   * imports static blocks and methods from the `java.lang` package, including `System`,
   * `Thread`, and `ValueOf`. It also defines a constructor and static methods for a
   * class called `Taco`.
   */
  @Test public void importStaticMixed() {
    JavaFile source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("assert $1T.valueOf(\"BLOCKED\") == $1T.BLOCKED", Thread.State.class)
                .addStatement("$T.gc()", System.class)
                .addStatement("$1T.out.println($1T.nanoTime())", System.class)
                .build())
            .addMethod(MethodSpec.constructorBuilder()
                .addParameter(Thread.State[].class, "states")
                .varargs(true)
                .build())
            .build())
        .addStaticImport(Thread.State.BLOCKED)
        .addStaticImport(System.class, "*")
        .addStaticImport(Thread.State.class, "valueOf")
        .build();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import static java.lang.System.*;\n"
        + "import static java.lang.Thread.State.BLOCKED;\n"
        + "import static java.lang.Thread.State.valueOf;\n"
        + "\n"
        + "import java.lang.Thread;\n"
        + "\n"
        + "class Taco {\n"
        + "  static {\n"
        + "    assert valueOf(\"BLOCKED\") == BLOCKED;\n"
        + "    gc();\n"
        + "    out.println(nanoTime());\n"
        + "  }\n"
        + "\n"
        + "  Taco(Thread.State... states) {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a Java source code that imports a static member from another class using
   * the `static` import statement, while also including the member's name in the import
   * statement.
   */
  @Ignore("addStaticImport doesn't support members with $L")
  @Test public void importStaticDynamic() {
    JavaFile source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addMethod(MethodSpec.methodBuilder("main")
                .addStatement("$T.$L.println($S)", System.class, "out", "hello")
                .build())
            .build())
        .addStaticImport(System.class, "out")
        .build();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import static java.lang.System.out;\n"
        + "\n"
        + "class Taco {\n"
        + "  void main() {\n"
        + "    out.println(\"hello\");\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * verifies that a Java file containing an `import static` statement with no arguments
   * results in an empty string for its toString() method output.
   */
  @Test public void importStaticNone() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .build().toString()).isEqualTo(""
        + "package readme;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "import java.util.concurrent.TimeUnit;\n"
        + "\n"
        + "class Util {\n"
        + "  public static long minutesToSeconds(long minutes) {\n"
        + "    System.gc();\n"
        + "    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES);\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether importing static typespecs once results in the expected output.
   */
  @Test public void importStaticOnce() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .build().toString()).isEqualTo(""
        + "package readme;\n"
        + "\n"
        + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "import java.util.concurrent.TimeUnit;\n"
        + "\n"
        + "class Util {\n"
        + "  public static long minutesToSeconds(long minutes) {\n"
        + "    System.gc();\n"
        + "    return SECONDS.convert(minutes, TimeUnit.MINUTES);\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether the import statement is redundant when used twice for different types.
   */
  @Test public void importStaticTwice() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .addStaticImport(TimeUnit.MINUTES)
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import static java.util.concurrent.TimeUnit.MINUTES;\n"
            + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
            + "\n"
            + "import java.lang.System;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    System.gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  /**
   * imports static classes and fields from the `java.lang`, `java.util.concurrent`,
   * and `java.util` packages using wildcards.
   */
  @Test public void importStaticUsingWildcards() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.class, "*")
        .addStaticImport(System.class, "*")
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import static java.lang.System.*;\n"
            + "import static java.util.concurrent.TimeUnit.*;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  /**
   * creates a new instance of the `TypeSpec` class, and returns it after building a
   * `MethodSpec` object that defines a static method called `minutesToSeconds`.
   * 
   * @param name name of the generated type spec.
   * 
   * @returns a `TypeSpec` object representing a static method named `minutesToSeconds`.
   */
  private TypeSpec importStaticTypeSpec(String name) {
    MethodSpec method = MethodSpec.methodBuilder("minutesToSeconds")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(long.class)
        .addParameter(long.class, "minutes")
        .addStatement("$T.gc()", System.class)
        .addStatement("return $1T.SECONDS.convert(minutes, $1T.MINUTES)", TimeUnit.class)
        .build();
    return TypeSpec.classBuilder(name).addMethod(method).build();

  }
  /**
   * tests whether a Java file contains any imports by comparing its source code to an
   * empty string.
   */
  @Test public void noImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * verifies that a single import statement is present in a Java file, with the expected
   * import being for the `java.util.Date` class.
   */
  @Test public void singleImport() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "}\n");
  }

  /**
   * tests whether importing the same class name multiple times in a Java file leads
   * to an error.
   */
  @Test public void conflictingImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .addField(ClassName.get("java.sql", "Date"), "madeFreshDatabaseDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "\n"
        + "  java.sql.Date madeFreshDatabaseDate;\n"
        + "}\n");
  }

  /**
   * generates a Java file with a class `Taco` that has a field `chorizo` of type `List`,
   * where each element is an instance of `Chorizo` annotated with `@Spicy`.
   */
  @Test public void annotatedTypeParam() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ParameterizedTypeName.get(ClassName.get(List.class),
                ClassName.get("com.squareup.meat", "Chorizo")
                    .annotated(AnnotationSpec.builder(ClassName.get("com.squareup.tacos", "Spicy"))
                        .build())), "chorizo")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.meat.Chorizo;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Taco {\n"
        + "  List<@Spicy Chorizo> chorizo;\n"
        + "}\n");
  }

  /**
   * generates a Java source code with the specified class and fields, while skipping
   * the import statements for the `java.lang` package if there is a conflicting field
   * with a fully qualified name in the same package.
   */
  @Test public void skipJavaLangImportsWithConflictingClassLast() throws Exception {
    // Whatever is used first wins! In this case the Float in java.lang is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float litres;\n"
        + "\n"
        + "  com.squareup.soda.Float beverage;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  /**
   * imports a class from a package with the same name as a conflicting class from the
   * standard library, importing the first conflicting class fully qualified.
   */
  @Test public void skipJavaLangImportsWithConflictingClassFirst() throws Exception {
    // Whatever is used first wins! In this case the Float in com.squareup.soda is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.soda.Float;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float beverage;\n"
        + "\n"
        + "  java.lang.Float litres;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  /**
   * tests whether a class with conflicting parent names can be compiled successfully.
   */
  @Test public void conflictingParentName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class Twin {\n"
        + "    }\n"
        + "\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a class can have two child classes with the same name, but different
   * types.
   */
  @Test public void conflictingChildName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Twin").build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "\n"
        + "      class Twin {\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a class with conflicting names can be properly compiled, ensuring
   * that the output is as expected.
   */
  @Test public void conflictingNameOutOfScope() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Nested")
                        .addType(TypeSpec.classBuilder("Twin").build())
                        .build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      Twin.D d;\n"
        + "\n"
        + "      class Nested {\n"
        + "        class Twin {\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a nested class and its superclass share the same name.
   */
  @Test public void nestedClassAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName.get("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.wire.Message;\n"
        + "\n"
        + "class Taco extends Message {\n"
        + "  class Builder extends Message.Builder {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a class and its superclass share the same name.
   */
  @Test public void classAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco extends com.taco.bell.Taco {\n"
        + "}\n");
  }

  /**
   * tests whether a class with conflicting annotations can be generated by Java compiler.
   */
  @Test public void conflictingAnnotation() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addAnnotation(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@com.taco.bell.Taco\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether an annotation reference to a class conflicts with the referenced
   * class's package name.
   */
  @Test public void conflictingAnnotationReferencedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addAnnotation(AnnotationSpec.builder(ClassName.get("com.squareup.tacos", "MyAnno"))
                .addMember("value", "$T.class", ClassName.get("com.taco.bell", "Taco"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@MyAnno(com.taco.bell.Taco.class)\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether a type variable bound can conflict with a nested type variable bound
   * in the same class.
   */
  @Test public void conflictingTypeVariableBound() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addTypeVariable(
                TypeVariableName.get("T", ClassName.get("com.taco.bell", "Taco")))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco<T extends com.taco.bell.Taco> {\n"
        + "}\n");
  }

  /**
   * tests whether a subclass references its direct superclass in its generic type declaration.
   */
  @Test public void superclassReferencesSelf() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ParameterizedTypeName.get(
                ClassName.get(Comparable.class), ClassName.get("com.squareup.tacos", "Taco")))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Comparable;\n"
        + "\n"
        + "class Taco extends Comparable<Taco> {\n"
        + "}\n");
  }

  /**
   * verifies that an annotation is contained within a nested class, by comparing the
   * expected source code to the actual source code generated by Dagger.
   */
  @Test public void annotationIsNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName.get("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName.get("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import dagger.Component;\n"
        + "\n"
        + "@Component\n"
        + "class TestComponent {\n"
        + "  @Component.Builder\n"
        + "  class Builder {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a Java file contains the source code for a `HelloWorld` class with
   * a single `main` method that prints "Hello World!" to the console.
   */
  @Test public void defaultPackage() throws Exception {
    String source = JavaFile.builder("",
        TypeSpec.classBuilder("HelloWorld")
            .addMethod(MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .addCode("$T.out.println($S);\n", System.class, "Hello World!")
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "import java.lang.String;\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class HelloWorld {\n"
        + "  public static void main(String[] args) {\n"
        + "    System.out.println(\"Hello World!\");\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests that default packages are not imported when generating Java code.
   */
  @Test public void defaultPackageTypesAreNotImported() throws Exception {
    String source = JavaFile.builder("hello",
          TypeSpec.classBuilder("World").addSuperinterface(ClassName.get("", "Test")).build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package hello;\n"
        + "\n"
        + "class World implements Test {\n"
        + "}\n");
  }

  /**
   * generates a file comment at the top of a Java class file based on a given date and
   * company name, using the `JavaFile` builder class to create the file contents.
   */
  @Test public void topOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated $L by JavaPoet. DO NOT EDIT!", "2015-01-13")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "// Generated 2015-01-13 by JavaPoet. DO NOT EDIT!\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * verifies that a generated Java file contains only empty lines at the top, with the
   * first line being a comment indicating that the file is generated and should not
   * be edited.
   */
  @Test public void emptyLinesInTopOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "//\n"
        + "// GENERATED FILE:\n"
        + "//\n"
        + "// DO NOT EDIT!\n"
        + "//\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether a nested class conflicts with an outer class with the same package
   * name.
   */
  @Test public void packageClassConflictsWithNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  com.squareup.tacos.A a;\n"
        + "\n"
        + "  class A {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a class with the same name as its superclass but different package
   * name conflicts with its superclass.
   */
  @Test public void packageClassConflictsWithSuperlass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "A"))
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco extends com.taco.bell.A {\n"
        + "  A a;\n"
        + "}\n");
  }

  /**
   * updates a Java file to add a static import for the `separatorChar` field of the
   * `File` class, while clearing and adding other static imports.
   */
  @Test public void modifyStaticImports() throws Exception {
    JavaFile.Builder builder = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .build())
            .addStaticImport(File.class, "separator");

    builder.staticImports.clear();
    builder.staticImports.add(File.class.getCanonicalName() + ".separatorChar");

    String source = builder.build().toString();

    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import static java.io.File.separatorChar;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether the `alwaysQualify` flag is properly applied to a `TypeSpec`.
   */
  @Test public void alwaysQualifySimple() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Thread.class, "thread")
            .alwaysQualify("Thread")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  java.lang.Thread thread;\n"
        + "}\n");
  }

  /**
   * modifies a Java file to include fields with fully qualified class names, even when
   * using imports from the `java.lang` package.
   */
  @Test public void alwaysQualifySupersedesJavaLangImports() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Thread.class, "thread")
            .alwaysQualify("Thread")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  java.lang.Thread thread;\n"
        + "}\n");
  }

  /**
   * generates a Java file that contains a class with fields that are qualified with
   * their respective nested classes, while avoiding conflicts with already defined classes.
   */
  @Test public void avoidClashesWithNestedClasses_viaClass() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .avoidClashesWithNestedClasses(Foo.class)
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import other.Foo;\n"
        + "import other.NestedTypeC;\n"
        + "\n"
        + "class Taco {\n"
        + "  other.NestedTypeA nestedA;\n"
        + "\n"
        + "  other.NestedTypeB nestedB;\n"
        + "\n"
        + "  NestedTypeC nestedC;\n"
        + "\n"
        + "  Foo foo;\n"
        + "}\n");
  }

  /**
   * generates a Java class with fields that avoid clashing with nested classes, using
   * the `TypeElement` to specify the nested types.
   */
  @Test public void avoidClashesWithNestedClasses_viaTypeElement() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .avoidClashesWithNestedClasses(getElement(Foo.class))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import other.Foo;\n"
        + "import other.NestedTypeC;\n"
        + "\n"
        + "class Taco {\n"
        + "  other.NestedTypeA nestedA;\n"
        + "\n"
        + "  other.NestedTypeB nestedB;\n"
        + "\n"
        + "  NestedTypeC nestedC;\n"
        + "\n"
        + "  Foo foo;\n"
        + "}\n");
  }

  /**
   * tests whether a class can use a superinterface type to avoid conflicts with nested
   * classes.
   */
  @Test public void avoidClashesWithNestedClasses_viaSuperinterfaceType() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .addType(TypeSpec.classBuilder("NestedTypeA").build())
            .addType(TypeSpec.classBuilder("NestedTypeB").build())
            .addSuperinterface(FooInterface.class)
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.javapoet.JavaFileTest;\n"
        + "import other.Foo;\n"
        + "import other.NestedTypeC;\n"
        + "\n"
        + "class Taco implements JavaFileTest.FooInterface {\n"
        + "  other.NestedTypeA nestedA;\n"
        + "\n"
        + "  other.NestedTypeB nestedB;\n"
        + "\n"
        + "  NestedTypeC nestedC;\n"
        + "\n"
        + "  Foo foo;\n"
        + "\n"
        + "  class NestedTypeA {\n"
        + "  }\n"
        + "\n"
        + "  class NestedTypeB {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * has a static nested class hierarchy with two subclasses: NestedTypeA and NestedTypeB.
   */
  static class Foo {
    /**
     * is a nested inner class within the Parent Class.
     */
    static class NestedTypeA {

    }
    /**
     * is a nested inner class within a larger outer class, with no fields or methods of
     * its own.
     */
    static class NestedTypeB {

    }
  }

  /**
   * has two nested classes: NestedTypeA and NestedTypeB.
   */
  interface FooInterface {
    /**
     * represents a nested type in a parent class.
     */
    class NestedTypeA {

    }
    /**
     * represents a nested type B within a larger JavaPoet file.
     */
    class NestedTypeB {

    }
  }

  /**
   * generates a `TypeSpec.Builder` instance that defines two methods: `optionalString()`
   * and `pattern()`. The `optionalString()` method returns an `Optional<String>` object,
   * while the `pattern()` method returns a null reference.
   * 
   * @returns a `TypeSpec` object representing a class with two methods: `optionalString()`
   * and `pattern()`.
   * 
   * 1/ The `TypeSpec.Builder` object is created with the class name "Child".
   * 2/ Two methods are defined: "optionalString" and "pattern".
   * 3/ The "optionalString" method returns a type named "Optional<String>", which is
   * a subtype of the "String" type.
   * 4/ The "pattern" method returns a null value.
   * 
   * The properties of these methods and their return types are as follows:
   * 
   * 	- "optionalString": This method returns an optional string, which means it can
   * be either a string or null.
   * 	- "pattern": This method returns a null value, indicating that the class does not
   * have any pattern information.
   */
  private TypeSpec.Builder childTypeBuilder() {
    return TypeSpec.classBuilder("Child")
        .addMethod(MethodSpec.methodBuilder("optionalString")
            .returns(ParameterizedTypeName.get(Optional.class, String.class))
            .addStatement("return $T.empty()", Optional.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("pattern")
            .returns(Pattern.class)
            .addStatement("return null")
            .build());
  }

  /**
   * generates Java code that defines a child class with a superclass of `Parent`, and
   * ensures that no conflicts occur between the classes by providing empty implementations
   * for `optionalString()` and `pattern()`.
   */
  @Test
  public void avoidClashes_parentChild_superclass_type() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().superclass(Parent.class).build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.javapoet;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Child extends JavaFileTest.Parent {\n"
        + "  java.util.Optional<String> optionalString() {\n"
        + "    return java.util.Optional.empty();\n"
        + "  }\n"
        + "\n"
        + "  java.util.regex.Pattern pattern() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a Java file that contains a child class with a superclass and type mirror,
   * and checks that the generated code does not have any clashes or conflicts.
   */
  @Test
  public void avoidClashes_parentChild_superclass_typeMirror() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().superclass(getElement(Parent.class).asType()).build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.javapoet;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Child extends JavaFileTest.Parent {\n"
        + "  java.util.Optional<String> optionalString() {\n"
        + "    return java.util.Optional.empty();\n"
        + "  }\n"
        + "\n"
        + "  java.util.regex.Pattern pattern() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a Java class that implements a super interface and has a method that
   * returns an empty optional and another method that returns null, without any clashes
   * with the super interface.
   */
  @Test
  public void avoidClashes_parentChild_superinterface_type() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().addSuperinterface(ParentInterface.class).build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.javapoet;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.util.regex.Pattern;\n"
        + "\n"
        + "class Child implements JavaFileTest.ParentInterface {\n"
        + "  java.util.Optional<String> optionalString() {\n"
        + "    return java.util.Optional.empty();\n"
        + "  }\n"
        + "\n"
        + "  Pattern pattern() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates Java code that implements a parent interface and a child class with the
   * same name, without any clashes or conflicts between them.
   */
  @Test
  public void avoidClashes_parentChild_superinterface_typeMirror() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().addSuperinterface(getElement(ParentInterface.class).asType()).build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.javapoet;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.util.regex.Pattern;\n"
        + "\n"
        + "class Child implements JavaFileTest.ParentInterface {\n"
        + "  java.util.Optional<String> optionalString() {\n"
        + "    return java.util.Optional.empty();\n"
        + "  }\n"
        + "\n"
        + "  Pattern pattern() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  // Regression test for https://github.com/square/javapoet/issues/77
  // This covers class and inheritance
  /**
   * implements an interface called ParentInterface and has a static inner class called
   * Pattern.
   */
  static class Parent implements ParentInterface {
    /**
     * is a static inner class in the Parent class with no fields or methods declared.
     */
    static class Pattern {

    }
  }

  /**
   * defines an interface for a class to implement methods related to parents and parenting.
   */
  interface ParentInterface {
    /**
     * is a class in Java that provides an optional value, which can be used to represent
     * the absence of a value or the presence of a value that may be null.
     */
    class Optional {

    }
  }

  // Regression test for case raised here: https://github.com/square/javapoet/issues/77#issuecomment-519972404
  /**
   * generates a Java class that implements the `Map` interface and has a method
   * `optionalString()` that returns a `com.foo.Entry` object, but always returns `null`.
   */
  @Test
  public void avoidClashes_mapEntry() {
    String source = JavaFile.builder("com.squareup.javapoet",
        TypeSpec.classBuilder("MapType")
            .addMethod(MethodSpec.methodBuilder("optionalString")
                .returns(ClassName.get("com.foo", "Entry"))
                .addStatement("return null")
                .build())
            .addSuperinterface(Map.class)
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo("package com.squareup.javapoet;\n"
        + "\n"
        + "import java.util.Map;\n"
        + "\n"
        + "class MapType implements Map {\n"
        + "  com.foo.Entry optionalString() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }
}
