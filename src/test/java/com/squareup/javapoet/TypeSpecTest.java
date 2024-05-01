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

import com.google.common.collect.ImmutableMap;
import com.google.testing.compile.CompilationRule;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * tests various aspects of the TypeSpec class in Kotlin, including:
 * 
 * 	- Creating and manipulating classes, interfaces, enums, annotations, and type variables.
 * 	- Modifying modifiers, fields, methods, type variables, superinterfaces, and types.
 * 	- Adding and removing Javadoc comments with trailing lines.
 * 
 * The tests cover various scenarios such as adding a new field or method to a class,
 * removing an annotation from an interface, and modifying the modifiers of a class.
 * The tests also verify that the resulting code is correct by checking the generated
 * code against a expected output.
 */
@RunWith(JUnit4.class)
public final class TypeSpecTest {
  private final String tacosPackage = "com.squareup.tacos";
  private static final String donutsPackage = "com.squareup.donuts";

  @Rule public final CompilationRule compilation = new CompilationRule();

  /**
   * retrieves a `TypeElement` object representing a class type from the compilation's
   * element set, given the class's canonical name.
   * 
   * @param clazz class for which the type element is being retrieved, and it is used
   * to identify the corresponding type element in the compilation's elements map.
   * 
   * 	- The type of the element returned is determined by the `compilation.getElements()`
   * method, which retrieves the elements from the compilation unit associated with the
   * class loader that loaded the class represented by `clazz`.
   * 	- The `getTypeElement()` method of this collection returns a `TypeElement` object
   * representing the corresponding type in the compilation unit's type hierarchy.
   * 	- The `canonicalName` property of `clazz` provides the fully qualified name of
   * the class, which is used to identify the type element in the type hierarchy.
   * 
   * @returns a `TypeElement` object representing the type of the given class.
   * 
   * 	- The `TypeElement` object represents a single type declaration in the compilation
   * unit.
   * 	- The `Class<?>` parameter `clazz` specifies the fully qualified name of the type
   * to be resolved.
   * 	- The function returns the `TypeElement` object associated with the given type
   * name, as retrieved from the `compilation.getElements()` collection.
   * 	- The `TypeElement` object has various attributes, such as `qualifiedName`,
   * `simpleName`, `type`, and `enclosingElement`, which provide additional information
   * about the type declaration.
   */
  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  /**
   * tests the toString() method of a TypeSpec object. It compares the output of the
   * method to the expected string value and verifies that the hash code of the object
   * is consistent across runs.
   */
  @Test public void basic() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(String.class)
            .addCode("return $S;\n", "taco")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @Override\n"
        + "  public final String toString() {\n"
        + "    return \"taco\";\n"
        + "  }\n"
        + "}\n");
    assertEquals(472949424, taco.hashCode()); // update expected number if source changes
  }

  /**
   * tests the ability to generate a TypeSpec with various types, including generic
   * types, wildcard types, and super types, and verifies that the generated code is correct.
   */
  @Test public void interestingTypes() throws Exception {
    TypeName listOfAny = ParameterizedTypeName.get(
        ClassName.get(List.class), WildcardTypeName.subtypeOf(Object.class));
    TypeName listOfExtends = ParameterizedTypeName.get(
        ClassName.get(List.class), WildcardTypeName.subtypeOf(Serializable.class));
    TypeName listOfSuper = ParameterizedTypeName.get(ClassName.get(List.class),
        WildcardTypeName.supertypeOf(String.class));
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(listOfAny, "extendsObject")
        .addField(listOfExtends, "extendsSerializable")
        .addField(listOfSuper, "superString")
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Taco {\n"
        + "  List<?> extendsObject;\n"
        + "\n"
        + "  List<? extends Serializable> extendsSerializable;\n"
        + "\n"
        + "  List<? super String> superString;\n"
        + "}\n");
  }

  /**
   * tests whether the toString method of a class returns the expected string representation
   * of the class.
   */
  @Test public void anonymousInnerClass() throws Exception {
    ClassName foo = ClassName.get(tacosPackage, "Foo");
    ClassName bar = ClassName.get(tacosPackage, "Bar");
    ClassName thingThang = ClassName.get(tacosPackage, "Thing", "Thang");
    TypeName thingThangOfFooBar = ParameterizedTypeName.get(thingThang, foo, bar);
    ClassName thung = ClassName.get(tacosPackage, "Thung");
    ClassName simpleThung = ClassName.get(tacosPackage, "SimpleThung");
    TypeName thungOfSuperBar = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(bar));
    TypeName thungOfSuperFoo = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(foo));
    TypeName simpleThungOfBar = ParameterizedTypeName.get(simpleThung, bar);

    ParameterSpec thungParameter = ParameterSpec.builder(thungOfSuperFoo, "thung")
        .addModifiers(Modifier.FINAL)
        .build();
    TypeSpec aSimpleThung = TypeSpec.anonymousClassBuilder(CodeBlock.of("$N", thungParameter))
        .superclass(simpleThungOfBar)
        .addMethod(MethodSpec.methodBuilder("doSomething")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(bar, "bar")
            .addCode("/* code snippets */\n")
            .build())
        .build();
    TypeSpec aThingThang = TypeSpec.anonymousClassBuilder("")
        .superclass(thingThangOfFooBar)
        .addMethod(MethodSpec.methodBuilder("call")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(thungOfSuperBar)
            .addParameter(thungParameter)
            .addCode("return $L;\n", aSimpleThung)
            .build())
        .build();
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(thingThangOfFooBar, "NAME")
            .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.FINAL)
            .initializer("$L", aThingThang)
            .build())
        .build();

    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "class Taco {\n"
        + "  static final Thing.Thang<Foo, Bar> NAME = new Thing.Thang<Foo, Bar>() {\n"
        + "    @Override\n"
        + "    public Thung<? super Bar> call(final Thung<? super Foo> thung) {\n"
        + "      return new SimpleThung<Bar>(thung) {\n"
        + "        @Override\n"
        + "        public void doSomething(Bar bar) {\n"
        + "          /* code snippets */\n"
        + "        }\n"
        + "      };\n"
        + "    }\n"
        + "  };\n"
        + "}\n");
  }

  /**
   * tests whether the annotations on method parameters are correctly generated based
   * on the class name and parameter names.
   */
  @Test public void annotatedParameters() throws Exception {
    TypeSpec service = TypeSpec.classBuilder("Foo")
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(long.class, "id")
            .addParameter(ParameterSpec.builder(String.class, "one")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "two")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "three")
                .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "Pong"))
                    .addMember("value", "$S", "pong")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder(String.class, "four")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addCode("/* code snippets */\n")
            .build())
        .build();

    assertThat(toString(service)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Foo {\n"
        + "  public Foo(long id, @Ping String one, @Ping String two, @Pong(\"pong\") String three,\n"
        + "      @Ping String four) {\n"
        + "    /* code snippets */\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a class with a field annotated with `@FreeRange` is generated correctly.
   */
  @Test public void annotationsAndJavaLangTypes() throws Exception {
    ClassName freeRange = ClassName.get("javax.annotation", "FreeRange");
    TypeSpec taco = TypeSpec.classBuilder("EthicalTaco")
        .addField(ClassName.get(String.class)
            .annotated(AnnotationSpec.builder(freeRange).build()), "meat")
        .build();

    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import javax.annotation.FreeRange;\n"
        + "\n"
        + "class EthicalTaco {\n"
        + "  @FreeRange String meat;\n"
        + "}\n");
  }

  /**
   * generates a TypeSpec interface defining a Retrofit-style service with a single
   * method, `fooBar`, that takes an `Observable<FooBar>` as input and returns an
   * `Observable<FooBar>` with a map of headers, query parameters, and an authorization
   * header.
   */
  @Test public void retrofitStyleInterface() throws Exception {
    ClassName observable = ClassName.get(tacosPackage, "Observable");
    ClassName fooBar = ClassName.get(tacosPackage, "FooBar");
    ClassName thing = ClassName.get(tacosPackage, "Thing");
    ClassName things = ClassName.get(tacosPackage, "Things");
    ClassName map = ClassName.get("java.util", "Map");
    ClassName string = ClassName.get("java.lang", "String");
    ClassName headers = ClassName.get(tacosPackage, "Headers");
    ClassName post = ClassName.get(tacosPackage, "POST");
    ClassName body = ClassName.get(tacosPackage, "Body");
    ClassName queryMap = ClassName.get(tacosPackage, "QueryMap");
    ClassName header = ClassName.get(tacosPackage, "Header");
    TypeSpec service = TypeSpec.interfaceBuilder("Service")
        .addMethod(MethodSpec.methodBuilder("fooBar")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(headers)
                .addMember("value", "$S", "Accept: application/json")
                .addMember("value", "$S", "User-Agent: foobar")
                .build())
            .addAnnotation(AnnotationSpec.builder(post)
                .addMember("value", "$S", "/foo/bar")
                .build())
            .returns(ParameterizedTypeName.get(observable, fooBar))
            .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(things, thing), "things")
                .addAnnotation(body)
                .build())
            .addParameter(ParameterSpec.builder(
                ParameterizedTypeName.get(map, string, string), "query")
                .addAnnotation(AnnotationSpec.builder(queryMap)
                    .addMember("encodeValues", "false")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder(string, "authorization")
                .addAnnotation(AnnotationSpec.builder(header)
                    .addMember("value", "$S", "Authorization")
                    .build())
                .build())
            .build())
        .build();

    assertThat(toString(service)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "interface Service {\n"
        + "  @Headers({\n"
        + "      \"Accept: application/json\",\n"
        + "      \"User-Agent: foobar\"\n"
        + "  })\n"
        + "  @POST(\"/foo/bar\")\n"
        + "  Observable<FooBar> fooBar(@Body Things<Thing> things,\n"
        + "      @QueryMap(encodeValues = false) Map<String, String> query,\n"
        + "      @Header(\"Authorization\") String authorization);\n"
        + "}\n");
  }

  /**
   * tests a class with a field annotated with `@JsonAdapter`, verifying that the
   * annotation is present and that the field name matches the expected format.
   */
  @Test public void annotatedField() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "thing", Modifier.PRIVATE, Modifier.FINAL)
            .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "JsonAdapter"))
                .addMember("value", "$T.class", ClassName.get(tacosPackage, "Foo"))
                .build())
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @JsonAdapter(Foo.class)\n"
        + "  private final String thing;\n"
        + "}\n");
  }

  /**
   * tests whether a TypeSpec builder can generate a Java class with annotations that
   * conform to a given annotation class.
   */
  @Test public void annotatedClass() throws Exception {
    ClassName someType = ClassName.get(tacosPackage, "SomeType");
    TypeSpec taco = TypeSpec.classBuilder("Foo")
        .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "Something"))
            .addMember("hi", "$T.$N", someType, "FIELD")
            .addMember("hey", "$L", 12)
            .addMember("hello", "$S", "goodbye")
            .build())
        .addModifiers(Modifier.PUBLIC)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@Something(\n"
        + "    hi = SomeType.FIELD,\n"
        + "    hey = 12,\n"
        + "    hello = \"goodbye\"\n"
        + ")\n"
        + "public class Foo {\n"
        + "}\n");
  }

  /**
   * tests whether adding an annotation to a TypeSpec instance throws a NullPointerException
   * when the annotation, type name, or class loader is null.
   */
  @Test public void addAnnotationDisallowsNull() {
    try {
      TypeSpec.classBuilder("Foo").addAnnotation((AnnotationSpec) null);
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("annotationSpec == null");
    }
    try {
      TypeSpec.classBuilder("Foo").addAnnotation((ClassName) null);
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("type == null");
    }
    try {
      TypeSpec.classBuilder("Foo").addAnnotation((Class<?>) null);
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("clazz == null");
    }
  }

  /**
   * defines an enum class `Roshambo` with three constant values, each with a custom
   * toString() method. It also defines a field and two constructors for the enum.
   */
  @Test public void enumWithSubclassing() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addJavadoc("Avalanche!\n")
            .build())
        .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder("$S", "flat")
            .addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addCode("return $S;\n", "paper airplane!")
                .build())
            .build())
        .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder("$S", "peace sign")
            .build())
        .addField(String.class, "handPosition", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(String.class, "handPosition")
            .addCode("this.handPosition = handPosition;\n")
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addCode("this($S);\n", "fist")
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "public enum Roshambo {\n"
        + "  /**\n"
        + "   * Avalanche!\n"
        + "   */\n"
        + "  ROCK,\n"
        + "\n"
        + "  PAPER(\"flat\") {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"paper airplane!\";\n"
        + "    }\n"
        + "  },\n"
        + "\n"
        + "  SCISSORS(\"peace sign\");\n"
        + "\n"
        + "  private final String handPosition;\n"
        + "\n"
        + "  Roshambo(String handPosition) {\n"
        + "    this.handPosition = handPosition;\n"
        + "  }\n"
        + "\n"
        + "  Roshambo() {\n"
        + "    this(\"fist\");\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether an enum can define an abstract method. It creates an enum with an
   * abstract method and checks if the resulting code conforms to the expected format.
   */
  @Test public void enumsMayDefineAbstractMethods() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Tortilla")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("CORN", TypeSpec.anonymousClassBuilder("")
            .addMethod(MethodSpec.methodBuilder("fold")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .build())
            .build())
        .addMethod(MethodSpec.methodBuilder("fold")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "public enum Tortilla {\n"
        + "  CORN {\n"
        + "    @Override\n"
        + "    public void fold() {\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  public abstract void fold();\n"
        + "}\n");
  }

  /**
   * tests whether an enum class has a constant field with no name or value.
   */
  @Test public void noEnumConstants() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
            .addField(String.class, "NO_ENUM", Modifier.STATIC)
            .build();
    assertThat(toString(roshambo)).isEqualTo(""
            + "package com.squareup.tacos;\n"
            + "\n"
            + "import java.lang.String;\n"
            + "\n"
            + "enum Roshambo {\n"
            + "  ;\n"
            + "  static String NO_ENUM;\n"
            + "}\n");
  }

  /**
   * checks whether an class builder can create an enum constant only if it is an enum
   * type.
   */
  @Test public void onlyEnumsMayHaveEnumConstants() throws Exception {
    try {
      TypeSpec.classBuilder("Roshambo")
          .addEnumConstant("ROCK")
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * tests an enum class with members but no constructor call. It uses the `TypeSpec`
   * class to generate a type description and then asserts that the resulting toString()
   * method implementation is correct.
   */
  @Test public void enumWithMembersButNoConstructorCall() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addEnumConstant("SPOCK", TypeSpec.anonymousClassBuilder("")
            .addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addCode("return $S;\n", "west side")
                .build())
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "enum Roshambo {\n"
        + "  SPOCK {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"west side\";\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether the toString() method of an enumeration returns the expected format
   * for an enumeration with annotated values.
   */
  @Test public void enumWithAnnotatedValues() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addAnnotation(Deprecated.class)
            .build())
        .addEnumConstant("PAPER")
        .addEnumConstant("SCISSORS")
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "\n"
        + "public enum Roshambo {\n"
        + "  @Deprecated\n"
        + "  ROCK,\n"
        + "\n"
        + "  PAPER,\n"
        + "\n"
        + "  SCISSORS\n"
        + "}\n");
  }

  /**
   * tests various ways in which a method can throw exceptions, including throwing
   * different classes and using modifiers to specify the type of exception that can
   * be thrown.
   */
  @Test public void methodThrows() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addModifiers(Modifier.ABSTRACT)
        .addMethod(MethodSpec.methodBuilder("throwOne")
            .addException(IOException.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("throwTwo")
            .addException(IOException.class)
            .addException(ClassName.get(tacosPackage, "SourCreamException"))
            .build())
        .addMethod(MethodSpec.methodBuilder("abstractThrow")
            .addModifiers(Modifier.ABSTRACT)
            .addException(IOException.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("nativeThrow")
            .addModifiers(Modifier.NATIVE)
            .addException(IOException.class)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.IOException;\n"
        + "\n"
        + "abstract class Taco {\n"
        + "  void throwOne() throws IOException {\n"
        + "  }\n"
        + "\n"
        + "  void throwTwo() throws IOException, SourCreamException {\n"
        + "  }\n"
        + "\n"
        + "  abstract void abstractThrow() throws IOException;\n"
        + "\n"
        + "  native void nativeThrow() throws IOException;\n"
        + "}\n");
  }

  /**
   * tests the `Location` class, which has a single abstract method `compareTo()` and
   * an static factory method `of()`. The test verifies that the generated code implements
   * the expected behavior for these methods.
   */
  @Test public void typeVariables() throws Exception {
    TypeVariableName t = TypeVariableName.get("T");
    TypeVariableName p = TypeVariableName.get("P", Number.class);
    ClassName location = ClassName.get(tacosPackage, "Location");
    TypeSpec typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), p))
        .addField(t, "label")
        .addField(p, "x")
        .addField(p, "y")
        .addMethod(MethodSpec.methodBuilder("compareTo")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(p, "p")
            .addCode("return 0;\n")
            .build())
        .addMethod(MethodSpec.methodBuilder("of")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(t)
            .addTypeVariable(p)
            .returns(ParameterizedTypeName.get(location, t, p))
            .addParameter(t, "label")
            .addParameter(p, "x")
            .addParameter(p, "y")
            .addCode("throw new $T($S);\n", UnsupportedOperationException.class, "TODO")
            .build())
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Comparable;\n"
        + "import java.lang.Number;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.UnsupportedOperationException;\n"
        + "\n"
        + "class Location<T, P extends Number> implements Comparable<P> {\n"
        + "  T label;\n"
        + "\n"
        + "  P x;\n"
        + "\n"
        + "  P y;\n"
        + "\n"
        + "  @Override\n"
        + "  public int compareTo(P p) {\n"
        + "    return 0;\n"
        + "  }\n"
        + "\n"
        + "  public static <T, P extends Number> Location<T, P> of(T label, P x, P y) {\n"
        + "    throw new UnsupportedOperationException(\"TODO\");\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a TypeSpec can be generated with type variables that have bounds and
   * are annotated with an AnnotationSpec.
   */
  @Test public void typeVariableWithBounds() {
    AnnotationSpec a = AnnotationSpec.builder(ClassName.get("com.squareup.tacos", "A")).build();
    TypeVariableName p = TypeVariableName.get("P", Number.class);
    TypeVariableName q = (TypeVariableName) TypeVariableName.get("Q", Number.class).annotated(a);
    TypeSpec typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(p.withBounds(Comparable.class))
        .addTypeVariable(q.withBounds(Comparable.class))
        .addField(p, "x")
        .addField(q, "y")
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Comparable;\n"
        + "import java.lang.Number;\n"
        + "\n"
        + "class Location<P extends Number & Comparable, @A Q extends Number & Comparable> {\n"
        + "  P x;\n"
        + "\n"
        + "  @A Q y;\n"
        + "}\n");
  }

  /**
   * tests the generated TypeSpec class for a Taco class that implements AbstractSet
   * and Serializable interfaces, and is also comparable to other Tacos.
   */
  @Test public void classImplementsExtends() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Taco");
    ClassName food = ClassName.get("com.squareup.tacos", "Food");
    TypeSpec typeSpec = TypeSpec.classBuilder("Taco")
        .addModifiers(Modifier.ABSTRACT)
        .superclass(ParameterizedTypeName.get(ClassName.get(AbstractSet.class), food))
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), taco))
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Comparable;\n"
        + "import java.util.AbstractSet;\n"
        + "\n"
        + "abstract class Taco extends AbstractSet<Food> "
        + "implements Serializable, Comparable<Taco> {\n"
        + "}\n");
  }

  /**
   * tests the implementation of a nested class within an outer class, using Java's
   * TypeInference feature to automatically generate the necessary type declarations.
   */
  @Test public void classImplementsNestedClass() throws Exception {
    ClassName outer = ClassName.get(tacosPackage, "Outer");
    ClassName inner = outer.nestedClass("Inner");
    ClassName callable = ClassName.get(Callable.class);
    TypeSpec typeSpec = TypeSpec.classBuilder("Outer")
        .superclass(ParameterizedTypeName.get(callable,
            inner))
        .addType(TypeSpec.classBuilder("Inner")
            .addModifiers(Modifier.STATIC)
            .build())
        .build();

    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.concurrent.Callable;\n"
        + "\n"
        + "class Outer extends Callable<Outer.Inner> {\n"
        + "  static class Inner {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests an enumeration type implementation of Serializable and Cloneable interfaces,
   * adding constants and building the type specification to verify the output equals
   * the expected code structure.
   */
  @Test public void enumImplements() throws Exception {
    TypeSpec typeSpec = TypeSpec.enumBuilder("Food")
        .addSuperinterface(Serializable.class)
        .addSuperinterface(Cloneable.class)
        .addEnumConstant("LEAN_GROUND_BEEF")
        .addEnumConstant("SHREDDED_CHEESE")
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Cloneable;\n"
        + "\n"
        + "enum Food implements Serializable, Cloneable {\n"
        + "  LEAN_GROUND_BEEF,\n"
        + "\n"
        + "  SHREDDED_CHEESE\n"
        + "}\n");
  }

  /**
   * tests the syntax for an interface that extends two interfaces: `Serializable` and
   * `Comparable`.
   */
  @Test public void interfaceExtends() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Taco");
    TypeSpec typeSpec = TypeSpec.interfaceBuilder("Taco")
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), taco))
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Comparable;\n"
        + "\n"
        + "interface Taco extends Serializable, Comparable<Taco> {\n"
        + "}\n");
  }

  /**
   * generates a TypeSpec that defines a class hierarchy with a nested class structure,
   * consisting of a parent class with three fields and two inner classes: one for
   * toppings and another for sauce.
   */
  @Test public void nestedClasses() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Combo", "Taco");
    ClassName topping = ClassName.get(tacosPackage, "Combo", "Taco", "Topping");
    ClassName chips = ClassName.get(tacosPackage, "Combo", "Chips");
    ClassName sauce = ClassName.get(tacosPackage, "Combo", "Sauce");
    TypeSpec typeSpec = TypeSpec.classBuilder("Combo")
        .addField(taco, "taco")
        .addField(chips, "chips")
        .addType(TypeSpec.classBuilder(taco.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(ParameterizedTypeName.get(ClassName.get(List.class), topping), "toppings")
            .addField(sauce, "sauce")
            .addType(TypeSpec.enumBuilder(topping.simpleName())
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(TypeSpec.classBuilder(chips.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(topping, "topping")
            .addField(sauce, "dippingSauce")
            .build())
        .addType(TypeSpec.enumBuilder(sauce.simpleName())
            .addEnumConstant("SOUR_CREAM")
            .addEnumConstant("SALSA")
            .addEnumConstant("QUESO")
            .addEnumConstant("MILD")
            .addEnumConstant("FIRE")
            .build())
        .build();

    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Combo {\n"
        + "  Taco taco;\n"
        + "\n"
        + "  Chips chips;\n"
        + "\n"
        + "  static class Taco {\n"
        + "    List<Topping> toppings;\n"
        + "\n"
        + "    Sauce sauce;\n"
        + "\n"
        + "    enum Topping {\n"
        + "      SHREDDED_CHEESE,\n"
        + "\n"
        + "      LEAN_GROUND_BEEF\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  static class Chips {\n"
        + "    Taco.Topping topping;\n"
        + "\n"
        + "    Sauce dippingSauce;\n"
        + "  }\n"
        + "\n"
        + "  enum Sauce {\n"
        + "    SOUR_CREAM,\n"
        + "\n"
        + "    SALSA,\n"
        + "\n"
        + "    QUESO,\n"
        + "\n"
        + "    MILD,\n"
        + "\n"
        + "    FIRE\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a TypeSpec object representing an annotation type with a single method
   * called `test`. The method has no return type and default value of 0.
   */
  @Test public void annotation() throws Exception {
    TypeSpec annotation = TypeSpec.annotationBuilder("MyAnnotation")
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .defaultValue("$L", 0)
            .returns(int.class)
            .build())
        .build();

    assertThat(toString(annotation)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "public @interface MyAnnotation {\n"
        + "  int test() default 0;\n"
        + "}\n"
    );
  }

  /**
   * tests whether an inner annotation can be declared within an annotation declaration.
   */
  @Test public void innerAnnotationInAnnotationDeclaration() throws Exception {
    TypeSpec bar = TypeSpec.annotationBuilder("Bar")
        .addMethod(MethodSpec.methodBuilder("value")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .defaultValue("@$T", Deprecated.class)
            .returns(Deprecated.class)
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "\n"
        + "@interface Bar {\n"
        + "  Deprecated value() default @Deprecated;\n"
        + "}\n"
    );
  }

  /**
   * tests whether an annotation with fields is generated correctly by checking its
   * toString representation against a expected output.
   */
  @Test public void annotationWithFields() {
    FieldSpec field = FieldSpec.builder(int.class, "FOO")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$L", 101)
        .build();

    TypeSpec anno = TypeSpec.annotationBuilder("Anno")
        .addField(field)
        .build();

    assertThat(toString(anno)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@interface Anno {\n"
        + "  int FOO = 101;\n"
        + "}\n"
    );
  }

  /**
   * tests whether a class can have a default value for a method.
   */
  @Test
  public void classCannotHaveDefaultValueForMethod() throws Exception {
    try {
      TypeSpec.classBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PUBLIC)
              .defaultValue("0")
              .returns(int.class)
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * attempts to create a class with a default method, which is prohibited in Java. If
   * successful, it throws an `IllegalStateException`.
   */
  @Test
  public void classCannotHaveDefaultMethods() throws Exception {
    try {
      TypeSpec.classBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
              .returns(int.class)
              .addCode(CodeBlock.builder().addStatement("return 0").build())
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * tests an interface with a static method named `test`.
   */
  @Test
  public void interfaceStaticMethods() throws Exception {
    TypeSpec bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "interface Tacos {\n"
        + "  static int test() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n"
    );
  }

  /**
   * tests whether the default methods are generated correctly for an interface. It
   * compares the expected output with the actual code generated by the `TypeSpec` builder.
   */
  @Test
  public void interfaceDefaultMethods() throws Exception {
    TypeSpec bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "interface Tacos {\n"
        + "  default int test() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n"
    );
  }

  /**
   * tests whether attempting to define private methods on an interface throws the
   * appropriate exceptions.
   */
  @Test
  public void invalidInterfacePrivateMethods() {
    try {
      TypeSpec.interfaceBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PRIVATE, Modifier.DEFAULT)
              .returns(int.class)
              .addCode(CodeBlock.builder().addStatement("return 0").build())
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }

    try {
      TypeSpec.interfaceBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PRIVATE, Modifier.ABSTRACT)
              .returns(int.class)
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }

    try {
      TypeSpec.interfaceBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PRIVATE, Modifier.PUBLIC)
              .returns(int.class)
              .addCode(CodeBlock.builder().addStatement("return 0").build())
              .build())
          .build();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * tests the creation and usage of private methods in Java interfaces. It generates
   * a sample interface with a private method and then verifies that the generated code
   * reflects the expected behavior of private methods in an interface.
   */
  @Test
  public void interfacePrivateMethods() {
    TypeSpec bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PRIVATE)
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "interface Tacos {\n"
        + "  private int test() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n"
    );

    bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "interface Tacos {\n"
        + "  private static int test() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n"
    );
  }

  /**
   * tests the conflict between referenced and declared simple names in Java. It builds
   * a type hierarchy with conflicting names and checks the resulting code snippet for
   * correctness.
   */
  @Test public void referencedAndDeclaredSimpleNamesConflict() throws Exception {
    FieldSpec internalTop = FieldSpec.builder(
        ClassName.get(tacosPackage, "Top"), "internalTop").build();
    FieldSpec internalBottom = FieldSpec.builder(
        ClassName.get(tacosPackage, "Top", "Middle", "Bottom"), "internalBottom").build();
    FieldSpec externalTop = FieldSpec.builder(
        ClassName.get(donutsPackage, "Top"), "externalTop").build();
    FieldSpec externalBottom = FieldSpec.builder(
        ClassName.get(donutsPackage, "Bottom"), "externalBottom").build();
    TypeSpec top = TypeSpec.classBuilder("Top")
        .addField(internalTop)
        .addField(internalBottom)
        .addField(externalTop)
        .addField(externalBottom)
        .addType(TypeSpec.classBuilder("Middle")
            .addField(internalTop)
            .addField(internalBottom)
            .addField(externalTop)
            .addField(externalBottom)
            .addType(TypeSpec.classBuilder("Bottom")
                .addField(internalTop)
                .addField(internalBottom)
                .addField(externalTop)
                .addField(externalBottom)
                .build())
            .build())
        .build();
    assertThat(toString(top)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.donuts.Bottom;\n"
        + "\n"
        + "class Top {\n"
        + "  Top internalTop;\n"
        + "\n"
        + "  Middle.Bottom internalBottom;\n"
        + "\n"
        + "  com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "  Bottom externalBottom;\n"
        + "\n"
        + "  class Middle {\n"
        + "    Top internalTop;\n"
        + "\n"
        + "    Bottom internalBottom;\n"
        + "\n"
        + "    com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "    com.squareup.donuts.Bottom externalBottom;\n"
        + "\n"
        + "    class Bottom {\n"
        + "      Top internalTop;\n"
        + "\n"
        + "      Bottom internalBottom;\n"
        + "\n"
        + "      com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "      com.squareup.donuts.Bottom externalBottom;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether field names conflict when they are defined in different packages
   * using the same simple name.
   */
  @Test public void simpleNamesConflictInThisAndOtherPackage() throws Exception {
    FieldSpec internalOther = FieldSpec.builder(
        ClassName.get(tacosPackage, "Other"), "internalOther").build();
    FieldSpec externalOther = FieldSpec.builder(
        ClassName.get(donutsPackage, "Other"), "externalOther").build();
    TypeSpec gen = TypeSpec.classBuilder("Gen")
        .addField(internalOther)
        .addField(externalOther)
        .build();
    assertThat(toString(gen)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Gen {\n"
        + "  Other internalOther;\n"
        + "\n"
        + "  com.squareup.donuts.Other externalOther;\n"
        + "}\n");
  }

  /**
   * tests the generation of a Java class with simple names conflicting with type
   * variables, and checks that the generated code includes the expected method signatures
   * and behavior.
   */
  @Test public void simpleNameConflictsWithTypeVariable() {
    ClassName inPackage = ClassName.get("com.squareup.tacos", "InPackage");
    ClassName otherType = ClassName.get("com.other", "OtherType");
    ClassName methodInPackage = ClassName.get("com.squareup.tacos", "MethodInPackage");
    ClassName methodOtherType = ClassName.get("com.other", "MethodOtherType");
    TypeSpec gen = TypeSpec.classBuilder("Gen")
        .addTypeVariable(TypeVariableName.get("InPackage"))
        .addTypeVariable(TypeVariableName.get("OtherType"))
        .addField(FieldSpec.builder(inPackage, "inPackage").build())
        .addField(FieldSpec.builder(otherType, "otherType").build())
        .addMethod(MethodSpec.methodBuilder("withTypeVariables")
            .addTypeVariable(TypeVariableName.get("MethodInPackage"))
            .addTypeVariable(TypeVariableName.get("MethodOtherType"))
            .addStatement("$T inPackage = null", methodInPackage)
            .addStatement("$T otherType = null", methodOtherType)
            .build())
        .addMethod(MethodSpec.methodBuilder("withoutTypeVariables")
            .addStatement("$T inPackage = null", methodInPackage)
            .addStatement("$T otherType = null", methodOtherType)
            .build())
        .addMethod(MethodSpec.methodBuilder("againWithTypeVariables")
            .addTypeVariable(TypeVariableName.get("MethodInPackage"))
            .addTypeVariable(TypeVariableName.get("MethodOtherType"))
            .addStatement("$T inPackage = null", methodInPackage)
            .addStatement("$T otherType = null", methodOtherType)
            .build())
        // https://github.com/square/javapoet/pull/657#discussion_r205514292
        .addMethod(MethodSpec.methodBuilder("masksEnclosingTypeVariable")
            .addTypeVariable(TypeVariableName.get("InPackage"))
            .build())
        .addMethod(MethodSpec.methodBuilder("hasSimpleNameThatWasPreviouslyMasked")
            .addStatement("$T inPackage = null", inPackage)
            .build())
        .build();
    assertThat(toString(gen)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.other.MethodOtherType;\n"
        + "\n"
        + "class Gen<InPackage, OtherType> {\n"
        + "  com.squareup.tacos.InPackage inPackage;\n"
        + "\n"
        + "  com.other.OtherType otherType;\n"
        + "\n"
        + "  <MethodInPackage, MethodOtherType> void withTypeVariables() {\n"
        + "    com.squareup.tacos.MethodInPackage inPackage = null;\n"
        + "    com.other.MethodOtherType otherType = null;\n"
        + "  }\n"
        + "\n"
        + "  void withoutTypeVariables() {\n"
        + "    MethodInPackage inPackage = null;\n"
        + "    MethodOtherType otherType = null;\n"
        + "  }\n"
        + "\n"
        + "  <MethodInPackage, MethodOtherType> void againWithTypeVariables() {\n"
        + "    com.squareup.tacos.MethodInPackage inPackage = null;\n"
        + "    com.other.MethodOtherType otherType = null;\n"
        + "  }\n"
        + "\n"
        + "  <InPackage> void masksEnclosingTypeVariable() {\n"
        + "  }\n"
        + "\n"
        + "  void hasSimpleNameThatWasPreviouslyMasked() {\n"
        + "    com.squareup.tacos.InPackage inPackage = null;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * checks if an outer type element includes those of its nested types.
   */
  @Test public void originatingElementsIncludesThoseOfNestedTypes() {
    Element outerElement = Mockito.mock(Element.class);
    Element innerElement = Mockito.mock(Element.class);
    TypeSpec outer = TypeSpec.classBuilder("Outer")
        .addOriginatingElement(outerElement)
        .addType(TypeSpec.classBuilder("Inner")
            .addOriginatingElement(innerElement)
            .build())
        .build();
    assertThat(outer.originatingElements).containsExactly(outerElement, innerElement);
  }

  /**
   * tests the `getComparator()` method of a `Taco` class, which returns `null`.
   */
  @Test public void intersectionType() {
    TypeVariableName typeVariable = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("getComparator")
            .addTypeVariable(typeVariable)
            .returns(typeVariable)
            .addCode("return null;\n")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.util.Comparator;\n"
        + "\n"
        + "class Taco {\n"
        + "  <T extends Comparator & Serializable> T getComparator() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a `TypeSpec` instance represents an array type with the specified
   * component type.
   */
  @Test public void arrayType() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(int[].class, "ints")
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  int[] ints;\n"
        + "}\n");
  }

  /**
   * generates Javadoc for a Java class, including fields and methods. It mentions types
   * in Javadoc without adding imports, but uses the short name if already imported.
   */
  @Test public void javadoc() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addJavadoc("A hard or soft tortilla, loosely folded and filled with whatever {@link \n")
        .addJavadoc("{@link $T random} tex-mex stuff we could find in the pantry\n", Random.class)
        .addJavadoc(CodeBlock.of("and some {@link $T} cheese.\n", String.class))
        .addField(FieldSpec.builder(boolean.class, "soft")
            .addJavadoc("True for a soft flour tortilla; false for a crunchy corn tortilla.\n")
            .build())
        .addMethod(MethodSpec.methodBuilder("refold")
            .addJavadoc("Folds the back of this taco to reduce sauce leakage.\n"
                + "\n"
                + "<p>For {@link $T#KOREAN}, the front may also be folded.\n", Locale.class)
            .addParameter(Locale.class, "locale")
            .build())
        .build();
    // Mentioning a type in Javadoc will not cause an import to be added (java.util.Random here),
    // but the short name will be used if it's already imported (java.util.Locale here).
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Locale;\n"
        + "\n"
        + "/**\n"
        + " * A hard or soft tortilla, loosely folded and filled with whatever {@link \n"
        + " * {@link java.util.Random random} tex-mex stuff we could find in the pantry\n"
        + " * and some {@link java.lang.String} cheese.\n"
        + " */\n"
        + "class Taco {\n"
        + "  /**\n"
        + "   * True for a soft flour tortilla; false for a crunchy corn tortilla.\n"
        + "   */\n"
        + "  boolean soft;\n"
        + "\n"
        + "  /**\n"
        + "   * Folds the back of this taco to reduce sauce leakage.\n"
        + "   *\n"
        + "   * <p>For {@link Locale#KOREAN}, the front may also be folded.\n"
        + "   */\n"
        + "  void refold(Locale locale) {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether an annotation can reference another annotation within its own
   * annotation list.
   */
  @Test public void annotationsInAnnotations() throws Exception {
    ClassName beef = ClassName.get(tacosPackage, "Beef");
    ClassName chicken = ClassName.get(tacosPackage, "Chicken");
    ClassName option = ClassName.get(tacosPackage, "Option");
    ClassName mealDeal = ClassName.get(tacosPackage, "MealDeal");
    TypeSpec menu = TypeSpec.classBuilder("Menu")
        .addAnnotation(AnnotationSpec.builder(mealDeal)
            .addMember("price", "$L", 500)
            .addMember("options", "$L", AnnotationSpec.builder(option)
                .addMember("name", "$S", "taco")
                .addMember("meat", "$T.class", beef)
                .build())
            .addMember("options", "$L", AnnotationSpec.builder(option)
                .addMember("name", "$S", "quesadilla")
                .addMember("meat", "$T.class", chicken)
                .build())
            .build())
        .build();
    assertThat(toString(menu)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@MealDeal(\n"
        + "    price = 500,\n"
        + "    options = {\n"
        + "        @Option(name = \"taco\", meat = Beef.class),\n"
        + "        @Option(name = \"quesadilla\", meat = Chicken.class)\n"
        + "    }\n"
        + ")\n"
        + "class Menu {\n"
        + "}\n");
  }

  /**
   * prepares a method to take an arbitrary number of `Runnable` objects as parameters.
   */
  @Test public void varargs() throws Exception {
    TypeSpec taqueria = TypeSpec.classBuilder("Taqueria")
        .addMethod(MethodSpec.methodBuilder("prepare")
            .addParameter(int.class, "workers")
            .addParameter(Runnable[].class, "jobs")
            .varargs()
            .build())
        .build();
    assertThat(toString(taqueria)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Runnable;\n"
        + "\n"
        + "class Taqueria {\n"
        + "  void prepare(int workers, Runnable... jobs) {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a map of common prefixes between two lists of strings, using a for loop
   * to iterate through the lists and compare each element. It then returns the length
   * of the common prefix.
   */
  @Test public void codeBlocks() throws Exception {
    CodeBlock ifBlock = CodeBlock.builder()
        .beginControlFlow("if (!a.equals(b))")
        .addStatement("return i")
        .endControlFlow()
        .build();
    CodeBlock methodBody = CodeBlock.builder()
        .addStatement("$T size = $T.min(listA.size(), listB.size())", int.class, Math.class)
        .beginControlFlow("for ($T i = 0; i < size; i++)", int.class)
        .addStatement("$T $N = $N.get(i)", String.class, "a", "listA")
        .addStatement("$T $N = $N.get(i)", String.class, "b", "listB")
        .add("$L", ifBlock)
        .endControlFlow()
        .addStatement("return size")
        .build();
    CodeBlock fieldBlock = CodeBlock.builder()
        .add("$>$>")
        .add("\n$T.<$T, $T>builder()$>$>", ImmutableMap.class, String.class, String.class)
        .add("\n.add($S, $S)", '\'', "&#39;")
        .add("\n.add($S, $S)", '&', "&amp;")
        .add("\n.add($S, $S)", '<', "&lt;")
        .add("\n.add($S, $S)", '>', "&gt;")
        .add("\n.build()$<$<")
        .add("$<$<")
        .build();
    FieldSpec escapeHtml = FieldSpec.builder(ParameterizedTypeName.get(
        Map.class, String.class, String.class), "ESCAPE_HTML")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer(fieldBlock)
        .build();
    TypeSpec util = TypeSpec.classBuilder("Util")
        .addField(escapeHtml)
        .addMethod(MethodSpec.methodBuilder("commonPrefixLength")
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(List.class, String.class), "listA")
            .addParameter(ParameterizedTypeName.get(List.class, String.class), "listB")
            .addCode(methodBody)
            .build())
        .build();
    assertThat(toString(util)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.google.common.collect.ImmutableMap;\n"
        + "import java.lang.Math;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "class Util {\n"
        + "  private static final Map<String, String> ESCAPE_HTML = \n"
        + "      ImmutableMap.<String, String>builder()\n"
        + "          .add(\"\'\", \"&#39;\")\n"
        + "          .add(\"&\", \"&amp;\")\n"
        + "          .add(\"<\", \"&lt;\")\n"
        + "          .add(\">\", \"&gt;\")\n"
        + "          .build();\n"
        + "\n"
        + "  int commonPrefixLength(List<String> listA, List<String> listB) {\n"
        + "    int size = Math.min(listA.size(), listB.size());\n"
        + "    for (int i = 0; i < size; i++) {\n"
        + "      String a = listA.get(i);\n"
        + "      String b = listB.get(i);\n"
        + "      if (!a.equals(b)) {\n"
        + "        return i;\n"
        + "      }\n"
        + "    }\n"
        + "    return size;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * in Java is used to execute different blocks of code based on the values of multiple
   * variables.
   */
  @Test public void indexedElseIf() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("choices")
            .beginControlFlow("if ($1L != null || $1L == $2L)", "taco", "otherTaco")
            .addStatement("$T.out.println($S)", System.class, "only one taco? NOO!")
            .nextControlFlow("else if ($1L.$3L && $2L.$3L)", "taco", "otherTaco", "isSupreme()")
            .addStatement("$T.out.println($S)", System.class, "taco heaven")
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void choices() {\n"
        + "    if (taco != null || taco == otherTaco) {\n"
        + "      System.out.println(\"only one taco? NOO!\");\n"
        + "    } else if (taco.isSupreme() && otherTaco.isSupreme()) {\n"
        + "      System.out.println(\"taco heaven\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a piece of code that prints "hello" if the condition 5 is less than 6,
   * and prints "wat" otherwise.
   */
  @Test public void elseIf() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("choices")
            .beginControlFlow("if (5 < 4) ")
            .addStatement("$T.out.println($S)", System.class, "wat")
            .nextControlFlow("else if (5 < 6)")
            .addStatement("$T.out.println($S)", System.class, "hello")
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void choices() {\n"
        + "    if (5 < 4)  {\n"
        + "      System.out.println(\"wat\");\n"
        + "    } else if (5 < 6) {\n"
        + "      System.out.println(\"hello\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * iterates indefinitely, printing "hello" to the console on each iteration until a
   * condition is met.
   */
  @Test public void doWhile() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("loopForever")
            .beginControlFlow("do")
            .addStatement("$T.out.println($S)", System.class, "hello")
            .endControlFlow("while (5 < 6)")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void loopForever() {\n"
        + "    do {\n"
        + "      System.out.println(\"hello\");\n"
        + "    } while (5 < 6);\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * checks if 3 is less than 4 and prints "hello" to System.out if it is.
   */
  @Test public void inlineIndent() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("inlineIndent")
            .addCode("if (3 < 4) {\n$>$T.out.println($S);\n$<}\n", System.class, "hello")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void inlineIndent() {\n"
        + "    if (3 < 4) {\n"
        + "      System.out.println(\"hello\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a Java interface with fields, methods, and classes. The generated code
   * includes the default modifiers for each member (public, static, final) and initializes
   * a field with a non-null value.
   */
  @Test public void defaultModifiersForInterfaceMembers() throws Exception {
    TypeSpec taco = TypeSpec.interfaceBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "SHELL")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "crunchy corn")
            .build())
        .addMethod(MethodSpec.methodBuilder("fold")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .addType(TypeSpec.classBuilder("Topping")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "interface Taco {\n"
        + "  String SHELL = \"crunchy corn\";\n"
        + "\n"
        + "  void fold();\n"
        + "\n"
        + "  class Topping {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a Java code snippet that defines a class and interfaces with default
   * modifiers for static members, and an enum with default modifiers for static constants.
   */
  @Test public void defaultModifiersForMemberInterfacesAndEnums() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addType(TypeSpec.classBuilder("Meat")
            .addModifiers(Modifier.STATIC)
            .build())
        .addType(TypeSpec.interfaceBuilder("Tortilla")
            .addModifiers(Modifier.STATIC)
            .build())
        .addType(TypeSpec.enumBuilder("Topping")
            .addModifiers(Modifier.STATIC)
            .addEnumConstant("SALSA")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  static class Meat {\n"
        + "  }\n"
        + "\n"
        + "  interface Tortilla {\n"
        + "  }\n"
        + "\n"
        + "  enum Topping {\n"
        + "    SALSA\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests the ordering of members in a Java class, including static fields, instance
   * fields, constructors, methods, and classes.
   */
  @Test public void membersOrdering() throws Exception {
    // Hand out names in reverse-alphabetical order to defend against unexpected sorting.
    TypeSpec taco = TypeSpec.classBuilder("Members")
        .addType(TypeSpec.classBuilder("Z").build())
        .addType(TypeSpec.classBuilder("Y").build())
        .addField(String.class, "X", Modifier.STATIC)
        .addField(String.class, "W")
        .addField(String.class, "V", Modifier.STATIC)
        .addField(String.class, "U")
        .addMethod(MethodSpec.methodBuilder("T").addModifiers(Modifier.STATIC).build())
        .addMethod(MethodSpec.methodBuilder("S").build())
        .addMethod(MethodSpec.methodBuilder("R").addModifiers(Modifier.STATIC).build())
        .addMethod(MethodSpec.methodBuilder("Q").build())
        .addMethod(MethodSpec.constructorBuilder().addParameter(int.class, "p").build())
        .addMethod(MethodSpec.constructorBuilder().addParameter(long.class, "o").build())
        .build();
    // Static fields, instance fields, constructors, methods, classes.
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Members {\n"
        + "  static String X;\n"
        + "\n"
        + "  static String V;\n"
        + "\n"
        + "  String W;\n"
        + "\n"
        + "  String U;\n"
        + "\n"
        + "  Members(int p) {\n"
        + "  }\n"
        + "\n"
        + "  Members(long o) {\n"
        + "  }\n"
        + "\n"
        + "  static void T() {\n"
        + "  }\n"
        + "\n"
        + "  void S() {\n"
        + "  }\n"
        + "\n"
        + "  static void R() {\n"
        + "  }\n"
        + "\n"
        + "  void Q() {\n"
        + "  }\n"
        + "\n"
        + "  class Z {\n"
        + "  }\n"
        + "\n"
        + "  class Y {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests the native methods and GWT JSNI in a Java class.
   */
  @Test public void nativeMethods() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("nativeInt")
            .addModifiers(Modifier.NATIVE)
            .returns(int.class)
            .build())
        // GWT JSNI
        .addMethod(MethodSpec.methodBuilder("alert")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.NATIVE)
            .addParameter(String.class, "msg")
            .addCode(CodeBlock.builder()
                .add(" /*-{\n")
                .indent()
                .addStatement("$$wnd.alert(msg)")
                .unindent()
                .add("}-*/")
                .build())
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  native int nativeInt();\n"
        + "\n"
        + "  public static native void alert(String msg) /*-{\n"
        + "    $wnd.alert(msg);\n"
        + "  }-*/;\n"
        + "}\n");
  }

  /**
   * tests the creation and usage of a null string literal in Java code using the
   * TypeSpec class builder.
   */
  @Test public void nullStringLiteral() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "NULL")
            .initializer("$S", (Object) null)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  String NULL = null;\n"
        + "}\n");
  }

  /**
   * tests whether the toString() method of an annotation object returns the expected
   * string representation, which is `@java.lang.SuppressWarnings("unused")`.
   */
  @Test public void annotationToString() throws Exception {
    AnnotationSpec annotation = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "unused")
        .build();
    assertThat(annotation.toString()).isEqualTo("@java.lang.SuppressWarnings(\"unused\")");
  }

  /**
   * converts a `CodeBlock` object into a string representation of its code.
   */
  @Test public void codeBlockToString() throws Exception {
    CodeBlock codeBlock = CodeBlock.builder()
        .addStatement("$T $N = $S.substring(0, 3)", String.class, "s", "taco")
        .build();
    assertThat(codeBlock.toString()).isEqualTo("java.lang.String s = \"taco\".substring(0, 3);\n");
  }

  /**
   * takes a `CodeBlock` object as input and returns its corresponding Java statement
   * as a string.
   */
  @Test public void codeBlockAddStatementOfCodeBlockToString() throws Exception {
    CodeBlock contents = CodeBlock.of("$T $N = $S.substring(0, 3)", String.class, "s", "taco");
    CodeBlock statement = CodeBlock.builder().addStatement(contents).build();
    assertThat(statement.toString()).isEqualTo("java.lang.String s = \"taco\".substring(0, 3);\n");
  }

  /**
   * tests a `FieldSpec` object that represents a field with the name "s" and the type
   * "String". It asserts that the toString() method of the field returns the expected
   * string value.
   */
  @Test public void fieldToString() throws Exception {
    FieldSpec field = FieldSpec.builder(String.class, "s", Modifier.FINAL)
        .initializer("$S.substring(0, 3)", "taco")
        .build();
    assertThat(field.toString())
        .isEqualTo("final java.lang.String s = \"taco\".substring(0, 3);\n");
  }

  /**
   * generates a method signature string that includes information about the method,
   * such as its name, return type, and annotations.
   */
  @Test public void methodToString() throws Exception {
    MethodSpec method = MethodSpec.methodBuilder("toString")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return $S", "taco")
        .build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {\n"
        + "  return \"taco\";\n"
        + "}\n");
  }

  /**
   * tests whether the constructor string is correctly generated for a given methodSpec
   * object.
   */
  @Test public void constructorToString() throws Exception {
    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(tacosPackage, "Taco"), "taco")
        .addStatement("this.$N = $N", "taco", "taco")
        .build();
    assertThat(constructor.toString()).isEqualTo(""
        + "public Constructor(com.squareup.tacos.Taco taco) {\n"
        + "  this.taco = taco;\n"
        + "}\n");
  }

  /**
   * tests whether the toString() method of a ParameterSpec object returns the expected
   * string value, which is `@javax.annotation.Nullable final com.squareup.tacos.Taco
   * taco`.
   */
  @Test public void parameterToString() throws Exception {
    ParameterSpec parameter = ParameterSpec.builder(ClassName.get(tacosPackage, "Taco"), "taco")
        .addModifiers(Modifier.FINAL)
        .addAnnotation(ClassName.get("javax.annotation", "Nullable"))
        .build();
    assertThat(parameter.toString())
        .isEqualTo("@javax.annotation.Nullable final com.squareup.tacos.Taco taco");
  }

  /**
   * tests whether the toString() method of a TypeSpec object returns the correct string
   * representation of the class.
   */
  @Test public void classToString() throws Exception {
    TypeSpec type = TypeSpec.classBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether the toString method of a TypeSpec object generates the expected
   * output for an anonymous class that implements the Runnable interface and has a
   * single method override annotation.
   */
  @Test public void anonymousClassToString() throws Exception {
    TypeSpec type = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(Runnable.class)
        .addMethod(MethodSpec.methodBuilder("run")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .build())
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "new java.lang.Runnable() {\n"
        + "  @java.lang.Override\n"
        + "  public void run() {\n"
        + "  }\n"
        + "}");
  }

  /**
   * tests whether an interface type's toString() method returns a string representing
   * its definition.
   */
  @Test public void interfaceClassToString() throws Exception {
    TypeSpec type = TypeSpec.interfaceBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "interface Taco {\n"
        + "}\n");
  }

  /**
   * converts a `TypeSpec` object representing an annotation to its string representation,
   * which includes the annotation name and a blank line.
   */
  @Test public void annotationDeclarationToString() throws Exception {
    TypeSpec type = TypeSpec.annotationBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "@interface Taco {\n"
        + "}\n");
  }

  /**
   * generates a string representation of a `TypeSpec` object by building a Java file
   * containing the `TypeSpec` and then returning its resulting string representation.
   * 
   * @param typeSpec TypeSpec object that contains information about the Java class or
   * interface to be generated by the toString() method.
   * 
   * 	- The method returns a string representation of a Java file built from the
   * `typeSpec` parameter using the `JavaFile.builder` method.
   * 	- The `tacosPackage` parameter represents the package of the generated Java class.
   * 	- The `typeSpec` parameter is an instance of `TypeSpec`, which contains information
   * about the type to be generated, including its name, fields, and methods.
   * 
   * @returns a string representation of the generated Java code.
   */
  private String toString(TypeSpec typeSpec) {
    return JavaFile.builder(tacosPackage, typeSpec).build().toString();
  }

  /**
   * generates a string that represents the toString() method of a class named Taco,
   * with the method body consisting of multiple lines.
   */
  @Test public void multilineStatement() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S\n+ $S\n+ $S\n+ $S\n+ $S",
                "Taco(", "beef,", "lettuce,", "cheese", ")")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return \"Taco(\"\n"
        + "        + \"beef,\"\n"
        + "        + \"lettuce,\"\n"
        + "        + \"cheese\"\n"
        + "        + \")\";\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests the functionality of a custom comparator class that sorts strings based on
   * a prefix comparison. It creates an anonymous inner class with a single method that
   * overrides the `compareTo()` method and compares two strings based on their prefix.
   * The function also implements another method to sort a list of strings using the
   * custom comparator.
   */
  @Test public void multilineStatementWithAnonymousClass() throws Exception {
    TypeName stringComparator = ParameterizedTypeName.get(Comparator.class, String.class);
    TypeName listOfString = ParameterizedTypeName.get(List.class, String.class);
    TypeSpec prefixComparator = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(stringComparator)
        .addMethod(MethodSpec.methodBuilder("compare")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(String.class, "a")
            .addParameter(String.class, "b")
            .addStatement("return a.substring(0, length)\n"
                + ".compareTo(b.substring(0, length))")
            .build())
        .build();
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("comparePrefix")
            .returns(stringComparator)
            .addParameter(int.class, "length", Modifier.FINAL)
            .addStatement("return $L", prefixComparator)
            .build())
        .addMethod(MethodSpec.methodBuilder("sortPrefix")
            .addParameter(listOfString, "list")
            .addParameter(int.class, "length", Modifier.FINAL)
            .addStatement("$T.sort(\nlist,\n$L)", Collections.class, prefixComparator)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "import java.util.Collections;\n"
        + "import java.util.Comparator;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Taco {\n"
        + "  Comparator<String> comparePrefix(final int length) {\n"
        + "    return new Comparator<String>() {\n"
        + "      @Override\n"
        + "      public int compare(String a, String b) {\n"
        + "        return a.substring(0, length)\n"
        + "            .compareTo(b.substring(0, length));\n"
        + "      }\n"
        + "    };\n"
        + "  }\n"
        + "\n"
        + "  void sortPrefix(List<String> list, final int length) {\n"
        + "    Collections.sort(\n"
        + "        list,\n"
        + "        new Comparator<String>() {\n"
        + "          @Override\n"
        + "          public int compare(String a, String b) {\n"
        + "            return a.substring(0, length)\n"
        + "                .compareTo(b.substring(0, length));\n"
        + "          }\n"
        + "        });\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a `TypeSpec` object can contain multiline strings by creating a
   * `Taco` class with a field containing a multiline string and checking that the
   * resulting code matches the expected output.
   */
  @Test public void multilineStrings() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "toppings")
            .initializer("$S", "shell\nbeef\nlettuce\ncheese\n")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  String toppings = \"shell\\n\"\n"
        + "      + \"beef\\n\"\n"
        + "      + \"lettuce\\n\"\n"
        + "      + \"cheese\\n\";\n"
        + "}\n");
  }

  /**
   * tests whether attempting to initialize a field with multiple initializers results
   * in an IllegalStateException.
   */
  @Test public void doubleFieldInitialization() {
    try {
      FieldSpec.builder(String.class, "listA")
          .initializer("foo")
          .initializer("bar")
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }

    try {
      FieldSpec.builder(String.class, "listA")
          .initializer(CodeBlock.builder().add("foo").build())
          .initializer(CodeBlock.builder().add("bar").build())
          .build();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * tests whether attempting to add annotations to a TypeSpec with null annotations
   * will throw an `IllegalArgumentException`.
   */
  @Test public void nullAnnotationsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addAnnotations(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  /**
   * tests whether a class builder can add multiple annotations to a class using the
   * `addAnnotations()` method.
   */
  @Test public void multipleAnnotationAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotations(Arrays.asList(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .build(),
            AnnotationSpec.builder(Deprecated.class).build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "import java.lang.SuppressWarnings;\n"
        + "\n"
        + "@SuppressWarnings(\"unchecked\")\n"
        + "@Deprecated\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * tests whether an attempt to build a TypeSpec with null field specifications will
   * throw an IllegalArgumentException.
   */
  @Test public void nullFieldsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addFields(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("fieldSpecs == null");
    }
  }

  /**
   * tests a class with multiple fields using JUnit.
   */
  @Test public void multipleFieldAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addFields(Arrays.asList(
            FieldSpec.builder(int.class, "ANSWER", Modifier.STATIC, Modifier.FINAL).build(),
            FieldSpec.builder(BigDecimal.class, "price", Modifier.PRIVATE).build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.math.BigDecimal;\n"
        + "\n"
        + "class Taco {\n"
        + "  static final int ANSWER;\n"
        + "\n"
        + "  private BigDecimal price;\n"
        + "}\n");
  }

  /**
   * tests whether an attempt to add methods to a `TypeSpec` object with null method
   * specifications will result in an `IllegalArgumentException`.
   */
  @Test public void nullMethodsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addMethods(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("methodSpecs == null");
    }
  }

  /**
   * adds multiple methods to a class using the `TypeSpec` builder. The added methods
   * include `getAnswer()` and `getRandomQuantity()`, which return integers values
   * respectively, 42 and 4.
   */
  @Test public void multipleMethodAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethods(Arrays.asList(
            MethodSpec.methodBuilder("getAnswer")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addStatement("return $L", 42)
                .build(),
            MethodSpec.methodBuilder("getRandomQuantity")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addJavadoc("chosen by fair dice roll ;)")
                .addStatement("return $L", 4)
                .build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  public static int getAnswer() {\n"
        + "    return 42;\n"
        + "  }\n"
        + "\n"
        + "  /**\n"
        + "   * chosen by fair dice roll ;)\n"
        + "   */\n"
        + "  public int getRandomQuantity() {\n"
        + "    return 4;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether adding a null value to an existing list of super interfaces in a
   * class builder throws an `IllegalArgumentException`.
   */
  @Test public void nullSuperinterfacesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addSuperinterfaces(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("superinterfaces == null");
    }
  }

  /**
   * tests whether adding a null superinterface to a class builder results in an `IllegalArgumentException`.
   */
  @Test public void nullSingleSuperinterfaceAddition() {
    try {
      TypeSpec.classBuilder("Taco").addSuperinterface((TypeName) null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("superinterface == null");
    }
  }

  /**
   * tests whether attempting to add a null object to an iterable list of superinterfaces
   * in a class builder throws an expected IllegalArgumentException.
   */
  @Test public void nullInSuperinterfaceIterableAddition() {
    List<TypeName> superinterfaces = new ArrayList<>();
    superinterfaces.add(TypeName.get(List.class));
    superinterfaces.add(null);

    try {
      TypeSpec.classBuilder("Taco").addSuperinterfaces(superinterfaces);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("superinterface == null");
    }
  }

  /**
   * tests the addition of multiple superinterfaces to a class using the `TypeSpec`
   * class builder.
   */
  @Test public void multipleSuperinterfaceAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addSuperinterfaces(Arrays.asList(
            TypeName.get(Serializable.class),
            TypeName.get(EventListener.class)))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.util.EventListener;\n"
        + "\n"
        + "class Taco implements Serializable, EventListener {\n"
        + "}\n");
  }

  /**
   * tests whether adding a null value to a `TypeSpec.Builder` throws an `IllegalArgumentException`.
   */
  @Test public void nullModifiersAddition() {
    try {
      TypeSpec.classBuilder("Taco").addModifiers((Modifier) null).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("modifiers contain null");
    }
  }

  /**
   * tests whether an attempt to add type variables to a TypeSpec object with null type
   * variables throws an IllegalArgumentException.
   */
  @Test public void nullTypeVariablesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addTypeVariables(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("typeVariables == null");
    }
  }

  /**
   * tests whether a `TypeSpec` builder can add multiple type variables to a class,
   * including one that is a generic type variable with a parameter of a specific type
   * (in this case, `Number`).
   */
  @Test public void multipleTypeVariableAddition() {
    TypeSpec location = TypeSpec.classBuilder("Location")
        .addTypeVariables(Arrays.asList(
            TypeVariableName.get("T"),
            TypeVariableName.get("P", Number.class)))
        .build();
    assertThat(toString(location)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Number;\n"
        + "\n"
        + "class Location<T, P extends Number> {\n"
        + "}\n");
  }

  /**
   * tests whether an `IllegalArgumentException` is thrown when a `TypeSpec.Builder`
   * is called with a null `typeSpecs` parameter.
   */
  @Test public void nullTypesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addTypes(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("typeSpecs == null");
    }
  }

  /**
   * tests whether a type builder can add multiple types to a base type successfully,
   * by creating a type specification with nested types and verifying its toString representation.
   */
  @Test public void multipleTypeAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addTypes(Arrays.asList(
            TypeSpec.classBuilder("Topping").build(),
            TypeSpec.classBuilder("Sauce").build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  class Topping {\n"
        + "  }\n"
        + "\n"
        + "  class Sauce {\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * generates a TypeSpec representation of a Java class that has a method for adding
   * toppings to an instance of a `Taco` class. The method includes a `try-catch` block
   * that handles an `IllegalToppingException`.
   */
  @Test public void tryCatch() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("addTopping")
            .addParameter(ClassName.get("com.squareup.tacos", "Topping"), "topping")
            .beginControlFlow("try")
            .addCode("/* do something tricky with the topping */\n")
            .nextControlFlow("catch ($T e)",
                ClassName.get("com.squareup.tacos", "IllegalToppingException"))
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  void addTopping(Topping topping) {\n"
        + "    try {\n"
        + "      /* do something tricky with the topping */\n"
        + "    } catch (IllegalToppingException e) {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * builds a method that takes an integer parameter 'count' and returns a boolean value
   * indicating whether the count is greater than 0.
   */
  @Test public void ifElse() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(
            MethodSpec.methodBuilder("isDelicious")
                .addParameter(TypeName.INT, "count")
                .returns(TypeName.BOOLEAN)
                .beginControlFlow("if (count > 0)")
                .addStatement("return true")
                .nextControlFlow("else")
                .addStatement("return false")
                .endControlFlow()
                .build()
        )
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  boolean isDelicious(int count) {\n"
        + "    if (count > 0) {\n"
        + "      return true;\n"
        + "    } else {\n"
        + "      return false;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether a `CodeBlock` object can be generated from any object using the
   * `toString()` method.
   */
  @Test public void literalFromAnything() {
    Object value = new Object() {
      /**
       * returns the string "foo".
       * 
       * @returns "foo".
       */
      @Override public String toString() {
        return "foo";
      }
    };
    assertThat(CodeBlock.of("$L", value).toString()).isEqualTo("foo");
  }

  /**
   * takes a `CharSequence` object and returns its corresponding string value.
   */
  @Test public void nameFromCharSequence() {
    assertThat(CodeBlock.of("$N", "text").toString()).isEqualTo("text");
  }

  /**
   * takes a `FieldSpec` object as input and returns the field's name in a string format.
   */
  @Test public void nameFromField() {
    FieldSpec field = FieldSpec.builder(String.class, "field").build();
    assertThat(CodeBlock.of("$N", field).toString()).isEqualTo("field");
  }

  /**
   * takes a `ParameterSpec` object as input and returns the name of the parameter
   * passed in the builder.
   */
  @Test public void nameFromParameter() {
    ParameterSpec parameter = ParameterSpec.builder(String.class, "parameter").build();
    assertThat(CodeBlock.of("$N", parameter).toString()).isEqualTo("parameter");
  }

  /**
   * generates a string representation of a `MethodSpec`. The resulting string is the
   * name of the method as defined in the code block.
   */
  @Test public void nameFromMethod() {
    MethodSpec method = MethodSpec.methodBuilder("method")
        .addModifiers(Modifier.ABSTRACT)
        .returns(String.class)
        .build();
    assertThat(CodeBlock.of("$N", method).toString()).isEqualTo("method");
  }

  /**
   * takes a `TypeSpec` object as input and returns its string representation, which
   * is simply the class name of the type builder.
   */
  @Test public void nameFromType() {
    TypeSpec type = TypeSpec.classBuilder("Type").build();
    assertThat(CodeBlock.of("$N", type).toString()).isEqualTo("Type");
  }

  /**
   * attempts to generate a name for an object of an unsupported type, throwing an `IllegalArgumentException`.
   */
  @Test public void nameFromUnsupportedType() {
    try {
      CodeBlock.builder().add("$N", String.class);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("expected name but was " + String.class);
    }
  }

  /**
   * verifies that a `CodeBlock` instance created from an arbitrary object can be
   * converted to a string with the expected value.
   */
  @Test public void stringFromAnything() {
    Object value = new Object() {
      /**
       * returns the literal string "foo".
       * 
       * @returns "foo".
       */
      @Override public String toString() {
        return "foo";
      }
    };
    assertThat(CodeBlock.of("$S", value).toString()).isEqualTo("\"foo\"");
  }

  /**
   * asserts that a `CodeBlock` containing a `String` literal with null value returns
   * "null" when converted to a string.
   */
  @Test public void stringFromNull() {
    assertThat(CodeBlock.of("$S", new Object[] {null}).toString()).isEqualTo("null");
  }

  /**
   * takes a `TypeName` object as input and returns the corresponding fully qualified
   * class name of the type represented by the `TypeName`.
   */
  @Test public void typeFromTypeName() {
    TypeName typeName = TypeName.get(String.class);
    assertThat(CodeBlock.of("$T", typeName).toString()).isEqualTo("java.lang.String");
  }

  /**
   * takes a `TypeMirror` object and returns its underlying type as a string, which is
   * verified to be equal to "java.lang.String".
   */
  @Test public void typeFromTypeMirror() {
    TypeMirror mirror = getElement(String.class).asType();
    assertThat(CodeBlock.of("$T", mirror).toString()).isEqualTo("java.lang.String");
  }

  /**
   * verifies that the toString() method of a `TypeElement` object returns the fully
   * qualified name of the corresponding class, in this case `java.lang.String`.
   */
  @Test public void typeFromTypeElement() {
    TypeElement element = getElement(String.class);
    assertThat(CodeBlock.of("$T", element).toString()).isEqualTo("java.lang.String");
  }

  /**
   * evaluates a string representing a Class object and returns the corresponding Class
   * type.
   */
  @Test public void typeFromReflectType() {
    assertThat(CodeBlock.of("$T", String.class).toString()).isEqualTo("java.lang.String");
  }

  /**
   * tests whether an attempt to use an unsupported type in a `CodeBlock.builder()`
   * method will result in an `IllegalArgumentException` with a specific message.
   */
  @Test public void typeFromUnsupportedType() {
    try {
      CodeBlock.builder().add("$T", "java.lang.String");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("expected type but was java.lang.String");
    }
  }

  /**
   * tests whether an IllegalArgumentException is thrown when too few arguments are
   * passed to a method that expects at least one argument.
   */
  @Test public void tooFewArguments() {
    try {
      CodeBlock.builder().add("$S");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 1 for '$S' not in range (received 0 arguments)");
    }
  }

  /**
   * tests whether an IllegalArgumentException is thrown when unused arguments are
   * passed to a method.
   */
  @Test public void unusedArgumentsRelative() {
    try {
      CodeBlock.builder().add("$L $L", "a", "b", "c");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("unused arguments: expected 2, received 3");
    }
  }

  /**
   * tests whether the `CodeBlock.builder()` method throws an `IllegalArgumentException`
   * when unused arguments are provided in a call to its `add()` method.
   */
  @Test public void unusedArgumentsIndexed() {
    try {
      CodeBlock.builder().add("$1L $2L", "a", "b", "c");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("unused argument: $3");
    }
    try {
      CodeBlock.builder().add("$1L $1L $1L", "a", "b", "c");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("unused arguments: $2, $3");
    }
    try {
      CodeBlock.builder().add("$3L $1L $3L $1L $3L", "a", "b", "c", "d");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("unused arguments: $2, $4");
    }
  }

  /**
   * tests whether a superclass can only be applied to classes, not to enums or interfaces.
   */
  @Test public void superClassOnlyValidForClasses() {
    try {
      TypeSpec.annotationBuilder("A").superclass(ClassName.get(Object.class));
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      TypeSpec.enumBuilder("E").superclass(ClassName.get(Object.class));
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      TypeSpec.interfaceBuilder("I").superclass(ClassName.get(Object.class));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * tests if attempting to set a superclass that is not a subclass of another class
   * will fail, and if setting an invalid superclass will throw an exception.
   */
  @Test public void invalidSuperClass() {
    try {
      TypeSpec.classBuilder("foo")
          .superclass(ClassName.get(List.class))
          .superclass(ClassName.get(Map.class));
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      TypeSpec.classBuilder("foo")
          .superclass(TypeName.INT);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * generates code for a class `Taco`, including a static block that sets the value
   * of a private field `FOO`, and a method `toString` that returns the value of `FOO`.
   */
  @Test public void staticCodeBlock() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(String.class, "foo", Modifier.PRIVATE)
        .addField(String.class, "FOO", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addStaticBlock(CodeBlock.builder()
            .addStatement("FOO = $S", "FOO")
            .build())
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addCode("return FOO;\n")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  private static final String FOO;\n"
        + "\n"
        + "  static {\n"
        + "    FOO = \"FOO\";\n"
        + "  }\n"
        + "\n"
        + "  private String foo;\n"
        + "\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return FOO;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether an initializer block is placed correctly in a class. It creates a
   * `TypeSpec` representing a `Taco` class with an initializer block and a static
   * block, and then verifies that the output of the toString() method is equal to the
   * value of the FOO static field.
   */
  @Test public void initializerBlockInRightPlace() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(String.class, "foo", Modifier.PRIVATE)
        .addField(String.class, "FOO", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addStaticBlock(CodeBlock.builder()
            .addStatement("FOO = $S", "FOO")
            .build())
        .addMethod(MethodSpec.constructorBuilder().build())
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addCode("return FOO;\n")
            .build())
        .addInitializerBlock(CodeBlock.builder()
            .addStatement("foo = $S", "FOO")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  private static final String FOO;\n"
        + "\n"
        + "  static {\n"
        + "    FOO = \"FOO\";\n"
        + "  }\n"
        + "\n"
        + "  private String foo;\n"
        + "\n"
        + "  {\n"
        + "    foo = \"FOO\";\n"
        + "  }\n"
        + "\n"
        + "  Taco() {\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return FOO;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests if toBuilder() method contains correct static and instance initializers for
   * a given TypeSpec.
   */
  @Test public void initializersToBuilder() {
    // Tests if toBuilder() contains correct static and instance initializers
    Element originatingElement = getElement(TypeSpecTest.class);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(String.class, "foo", Modifier.PRIVATE)
        .addField(String.class, "FOO", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addStaticBlock(CodeBlock.builder()
            .addStatement("FOO = $S", "FOO")
            .build())
        .addMethod(MethodSpec.constructorBuilder().build())
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addCode("return FOO;\n")
            .build())
        .addInitializerBlock(CodeBlock.builder()
            .addStatement("foo = $S", "FOO")
            .build())
        .addOriginatingElement(originatingElement)
        .alwaysQualify("com.example.AlwaysQualified")
        .build();

    TypeSpec recreatedTaco = taco.toBuilder().build();
    assertThat(toString(taco)).isEqualTo(toString(recreatedTaco));
    assertThat(taco.originatingElements)
        .containsExactlyElementsIn(recreatedTaco.originatingElements);
    assertThat(taco.alwaysQualifiedNames)
        .containsExactlyElementsIn(recreatedTaco.alwaysQualifiedNames);

    TypeSpec initializersAdded = taco.toBuilder()
        .addInitializerBlock(CodeBlock.builder()
            .addStatement("foo = $S", "instanceFoo")
            .build())
        .addStaticBlock(CodeBlock.builder()
            .addStatement("FOO = $S", "staticFoo")
            .build())
        .build();

    assertThat(toString(initializersAdded)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  private static final String FOO;\n"
        + "\n"
        + "  static {\n"
        + "    FOO = \"FOO\";\n"
        + "  }\n"
        + "  static {\n"
        + "    FOO = \"staticFoo\";\n"
        + "  }\n"
        + "\n"
        + "  private String foo;\n"
        + "\n"
        + "  {\n"
        + "    foo = \"FOO\";\n"
        + "  }\n"
        + "  {\n"
        + "    foo = \"instanceFoo\";\n"
        + "  }\n"
        + "\n"
        + "  Taco() {\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return FOO;\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests whether an exception is thrown when attempting to add an initializer block
   * to an interface using `TypeSpec.Builder`.
   */
  @Test public void initializerBlockUnsupportedExceptionOnInterface() {
    TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder("Taco");
    try {
      interfaceBuilder.addInitializerBlock(CodeBlock.builder().build());
      fail("Exception expected");
    } catch (UnsupportedOperationException e) {
    }
  }

  /**
   * tests whether an exception is thrown when an initializer block is added to an
   * annotation using the `TypeSpec.builder().addInitializerBlock()` method.
   */
  @Test public void initializerBlockUnsupportedExceptionOnAnnotation() {
    TypeSpec.Builder annotationBuilder = TypeSpec.annotationBuilder("Taco");
    try {
      annotationBuilder.addInitializerBlock(CodeBlock.builder().build());
      fail("Exception expected");
    } catch (UnsupportedOperationException e) {
    }
  }

  /**
   * takes multiple string parameters and calls a method with them, wrapping each
   * parameter on a new line for readability.
   */
  @Test public void lineWrapping() {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("call");
    methodBuilder.addCode("$[call(");
    for (int i = 0; i < 32; i++) {
      methodBuilder.addParameter(String.class, "s" + i);
      methodBuilder.addCode(i > 0 ? ",$W$S" : "$S", i);
    }
    methodBuilder.addCode(");$]\n");

    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(methodBuilder.build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  void call(String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7,\n"
        + "      String s8, String s9, String s10, String s11, String s12, String s13, String s14, String s15,\n"
        + "      String s16, String s17, String s18, String s19, String s20, String s21, String s22,\n"
        + "      String s23, String s24, String s25, String s26, String s27, String s28, String s29,\n"
        + "      String s30, String s31) {\n"
        + "    call(\"0\", \"1\", \"2\", \"3\", \"4\", \"5\", \"6\", \"7\", \"8\", \"9\", \"10\", \"11\", \"12\", \"13\", \"14\", \"15\", \"16\",\n"
        + "        \"17\", \"18\", \"19\", \"20\", \"21\", \"22\", \"23\", \"24\", \"25\", \"26\", \"27\", \"28\", \"29\", \"30\", \"31\");\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * wraps a line of text within a method call using a zero-width space character.
   */
  @Test public void lineWrappingWithZeroWidthSpace() {
    MethodSpec method = MethodSpec.methodBuilder("call")
        .addCode("$[iAmSickOfWaitingInLine($Z")
        .addCode("it, has, been, far, too, long, of, a, wait, and, i, would, like, to, eat, ")
        .addCode("this, is, a, run, on, sentence")
        .addCode(");$]\n")
        .build();

    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(method)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  void call() {\n"
        + "    iAmSickOfWaitingInLine(\n"
        + "        it, has, been, far, too, long, of, a, wait, and, i, would, like, to, eat, this, is, a, run, on, sentence);\n"
        + "  }\n"
        + "}\n");
  }

  /**
   * tests the equality and hash code generation of different types, including interfaces,
   * classes, enums, and annotations, using assertions to verify the results.
   */
  @Test public void equalsAndHashCode() {
    TypeSpec a = TypeSpec.interfaceBuilder("taco").build();
    TypeSpec b = TypeSpec.interfaceBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.classBuilder("taco").build();
    b = TypeSpec.classBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build();
    b = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.annotationBuilder("taco").build();
    b = TypeSpec.annotationBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  /**
   * tests the ability to create classes, interfaces, enums and annotations with the
   * `ClassName` class.
   */
  @Test public void classNameFactories() {
    ClassName className = ClassName.get("com.example", "Example");
    assertThat(TypeSpec.classBuilder(className).build().name).isEqualTo("Example");
    assertThat(TypeSpec.interfaceBuilder(className).build().name).isEqualTo("Example");
    assertThat(TypeSpec.enumBuilder(className).addEnumConstant("A").build().name).isEqualTo("Example");
    assertThat(TypeSpec.annotationBuilder(className).build().name).isEqualTo("Example");
  }

  /**
   * modifies an instance of `TypeSpec.Builder`, adding and removing annotations. It
   * then builds the resulting annotated type and checks its size to verify the
   * modifications were successful.
   */
  @Test
  public void modifyAnnotations() {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder("Taco")
            .addAnnotation(Override.class)
            .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  /**
   * alters the modifiers of a `TypeSpec` object, specifically removing one modifier
   * and ensuring that the resulting modifiers list contains only the `PUBLIC` modifier.
   */
  @Test
  public void modifyModifiers() {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder("Taco").addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }

  /**
   * modifies a TypeSpec instance representing a class named "Taco". It removes one of
   * the fields from the TypeSpec and verifies that the resulting field specs are empty.
   */
  @Test
  public void modifyFields() {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Taco")
        .addField(int.class, "source");

    builder.fieldSpecs.remove(0);
    assertThat(builder.build().fieldSpecs).isEmpty();
  }

  /**
   * modifies a TypeSpec object's type variables, removing and adding new variables as
   * needed.
   */
  @Test
  public void modifyTypeVariables() {
    TypeVariableName t = TypeVariableName.get("T");
    TypeSpec.Builder builder =
        TypeSpec.classBuilder("Taco")
            .addTypeVariable(t)
            .addTypeVariable(TypeVariableName.get("V"));

    builder.typeVariables.remove(1);
    assertThat(builder.build().typeVariables).containsExactly(t);
  }

  /**
   * modifies a `TypeSpec` instance to remove all superinterfaces and verifies that the
   * resulting `TypeSpec` instance has an empty set of superinterfaces.
   */
  @Test
  public void modifySuperinterfaces() {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Taco")
        .addSuperinterface(File.class);

    builder.superinterfaces.clear();
    assertThat(builder.build().superinterfaces).isEmpty();
  }

  /**
   * clears and empties the method specs of a `TypeSpec`.
   */
  @Test
  public void modifyMethods() {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("bell").build());

    builder.methodSpecs.clear();
    assertThat(builder.build().methodSpecs).isEmpty();
  }

  /**
   * modifies a TypeSpec builder to remove all type specs and then builds the TypeSpec
   * instance. It returns an empty list of type specs after building the instance.
   */
  @Test
  public void modifyTypes() {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Taco")
        .addType(TypeSpec.classBuilder("Bell").build());

    builder.typeSpecs.clear();
    assertThat(builder.build().typeSpecs).isEmpty();
  }

  /**
   * modifies an existing enum class with new constants, removes a existing constant,
   * and verifies that the updated enum constants are correctly reflected in the resulting
   * enum class.
   */
  @Test
  public void modifyEnumConstants() {
    TypeSpec constantType = TypeSpec.anonymousClassBuilder("").build();
    TypeSpec.Builder builder = TypeSpec.enumBuilder("Taco")
        .addEnumConstant("BELL", constantType)
        .addEnumConstant("WUT", TypeSpec.anonymousClassBuilder("").build());

    builder.enumConstants.remove("WUT");
    assertThat(builder.build().enumConstants).containsExactly("BELL", constantType);
  }

  /**
   * modifies the originating elements of a `TypeSpec` builder by clearing its internal
   * list and then verifying that the resulting `TypeSpec` instance contains no originating
   * elements.
   */
  @Test
  public void modifyOriginatingElements() {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Taco")
        .addOriginatingElement(Mockito.mock(Element.class));

    builder.originatingElements.clear();
    assertThat(builder.build().originatingElements).isEmpty();
  }
    
  /**
   * tests whether adding a newline to Javadoc documentation does not result in an
   * additional line being added to the generated code.
   */
  @Test public void javadocWithTrailingLineDoesNotAddAnother() {
    TypeSpec spec = TypeSpec.classBuilder("Taco")
        .addJavadoc("Some doc with a newline\n")
        .build();

    assertThat(toString(spec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "/**\n"
        + " * Some doc with a newline\n"
        + " */\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * verifies that a generated Javadoc comment has a trailing line break.
   */
  @Test public void javadocEnsuresTrailingLine() {
    TypeSpec spec = TypeSpec.classBuilder("Taco")
        .addJavadoc("Some doc with a newline")
        .build();

    assertThat(toString(spec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "/**\n"
        + " * Some doc with a newline\n"
        + " */\n"
        + "class Taco {\n"
        + "}\n");
  }
}
