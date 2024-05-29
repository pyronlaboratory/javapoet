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

import com.google.testing.compile.CompilationRule;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class AnnotationSpecTest {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationA {
  }

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationB {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationC {
    String value();
  }

  public enum Breakfast {
    WAFFLES, PANCAKES;
    public String toString() { return name() + " with cherries!"; };
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface HasDefaultsAnnotation {

    byte a() default 5;

    short b() default 6;

    int c() default 7;

    long d() default 12345678910L;

    float e() default 9.0f;

    double f() default 10.0;

    char[] g() default {0, 0xCAFE, 'z', '€', 'ℕ', '"', '\'', '\t', '\n'};

    boolean h() default true;

    Breakfast i() default Breakfast.WAFFLES;

    AnnotationA j() default @AnnotationA();

    String k() default "maple";

    Class<? extends Annotation> l() default AnnotationB.class;

    int[] m() default {1, 2, 3};

    Breakfast[] n() default {Breakfast.WAFFLES, Breakfast.PANCAKES};

    Breakfast o();

    int p();

    AnnotationC q() default @AnnotationC("foo");

    Class<? extends Number>[] r() default {Byte.class, Short.class, Integer.class, Long.class};

  }

  @HasDefaultsAnnotation(
      o = Breakfast.PANCAKES,
      p = 1701,
      f = 11.1,
      m = {9, 8, 1},
      l = Override.class,
      j = @AnnotationA,
      q = @AnnotationC("bar"),
      r = {Float.class, Double.class})
  public class IsAnnotated {
    // empty
  }

  @Rule public final CompilationRule compilation = new CompilationRule();

  /**
   * Tests whether two AnnotationSpec objects are equal and have the same hash code,
   * using the ` equals()` method and `hashCode()` method.
   */
  @Test public void equalsAndHashCode() {
    AnnotationSpec a = AnnotationSpec.builder(AnnotationC.class).build();
    AnnotationSpec b = AnnotationSpec.builder(AnnotationC.class).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = AnnotationSpec.builder(AnnotationC.class).addMember("value", "$S", "123").build();
    b = AnnotationSpec.builder(AnnotationC.class).addMember("value", "$S", "123").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  /**
   * Tests the `@AnnotationSpecTest.HasDefaultsAnnotation` annotation by creating a new
   * type `Taco` with various annotations and fields, and then asserts that its toString()
   * method returns a expected string representation of the type.
   */
  @Test public void defaultAnnotation() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));

    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(annotation)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.javapoet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation(\n"
        + "    o = AnnotationSpecTest.Breakfast.PANCAKES,\n"
        + "    p = 1701,\n"
        + "    f = 11.1,\n"
        + "    m = {\n"
        + "        9,\n"
        + "        8,\n"
        + "        1\n"
        + "    },\n"
        + "    l = Override.class,\n"
        + "    j = @AnnotationSpecTest.AnnotationA,\n"
        + "    q = @AnnotationSpecTest.AnnotationC(\"bar\"),\n"
        + "    r = {\n"
        + "        Float.class,\n"
        + "        Double.class\n"
        + "    }\n"
        + ")\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * Generates a Java file with an annotated class, using the `@HasDefaultsAnnotation`
   * annotation to specify default values for fields and methods.
   */
  @Test public void defaultAnnotationWithImport() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(IsAnnotated.class.getSimpleName());
    typeBuilder.addAnnotation(annotation);
    JavaFile file = JavaFile.builder("com.squareup.javapoet", typeBuilder.build()).build();
    assertThat(file.toString()).isEqualTo(
        "package com.squareup.javapoet;\n"
            + "\n"
            + "import java.lang.Double;\n"
            + "import java.lang.Float;\n"
            + "import java.lang.Override;\n"
            + "\n"
            + "@AnnotationSpecTest.HasDefaultsAnnotation(\n"
            + "    o = AnnotationSpecTest.Breakfast.PANCAKES,\n"
            + "    p = 1701,\n"
            + "    f = 11.1,\n"
            + "    m = {\n"
            + "        9,\n"
            + "        8,\n"
            + "        1\n"
            + "    },\n"
            + "    l = Override.class,\n"
            + "    j = @AnnotationSpecTest.AnnotationA,\n"
            + "    q = @AnnotationSpecTest.AnnotationC(\"bar\"),\n"
            + "    r = {\n"
            + "        Float.class,\n"
            + "        Double.class\n"
            + "    }\n"
            + ")\n"
            + "class IsAnnotated {\n"
            + "}\n"
    );
  }

  /**
   * Tests the `@Builder` annotation by creating an `AnnotationSpec` instance with empty
   * arrays for `members`.
   */
  @Test public void emptyArray() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$L", "{}");
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation(" + "n = {}" + ")");
    builder.addMember("m", "$L", "{}");
    assertThat(builder.build().toString())
        .isEqualTo(
            "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
                + "n = {}, m = {}"
                + ")");
  }

  /**
   * Generates high-quality documentation for code given to it. It creates a builder
   * instance and adds members to it using the `addMember` method, which takes the name
   * of an enum constant and its corresponding class. The function then builds the
   * builder instance into an `AnnotationSpec` object and returns it.
   */
  @Test public void dynamicArrayOfEnumConstants() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ")");

    // builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES.name());
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder = builder.build().toBuilder(); // idempotent
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + "})");
  }

  /**
   * Generates a new `AnnotationSpec.Builder` instance using the given type element and
   * adds default members to it. The built `AnnotationSpec` is then returned, which can
   * be used to generate a JavaPoet annotation document.
   */
  @Test public void defaultAnnotationToBuilder() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec.Builder builder = AnnotationSpec.get(element.getAnnotationMirrors().get(0))
        .toBuilder();
    builder.addMember("m", "$L", 123);
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "o = com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", p = 1701"
            + ", f = 11.1"
            + ", m = {9, 8, 1, 123}"
            + ", l = java.lang.Override.class"
            + ", j = @com.squareup.javapoet.AnnotationSpecTest.AnnotationA"
            + ", q = @com.squareup.javapoet.AnnotationSpecTest.AnnotationC(\"bar\")"
            + ", r = {java.lang.Float.class, java.lang.Double.class}"
            + ")");
  }

  /**
   * Generates a Java class that represents the given annotation and its properties.
   */
  @Test public void reflectAnnotation() {
    HasDefaultsAnnotation annotation = IsAnnotated.class.getAnnotation(HasDefaultsAnnotation.class);
    AnnotationSpec spec = AnnotationSpec.get(annotation);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.javapoet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation(\n"
        + "    f = 11.1,\n"
        + "    l = Override.class,\n"
        + "    m = {\n"
        + "        9,\n"
        + "        8,\n"
        + "        1\n"
        + "    },\n"
        + "    o = AnnotationSpecTest.Breakfast.PANCAKES,\n"
        + "    p = 1701,\n"
        + "    q = @AnnotationSpecTest.AnnotationC(\"bar\"),\n"
        + "    r = {\n"
        + "        Float.class,\n"
        + "        Double.class\n"
        + "    }\n"
        + ")\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * Tests whether a annotated class can be generated with default values for annotations.
   */
  @Test public void reflectAnnotationWithDefaults() {
    HasDefaultsAnnotation annotation = IsAnnotated.class.getAnnotation(HasDefaultsAnnotation.class);
    AnnotationSpec spec = AnnotationSpec.get(annotation, true);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.javapoet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation(\n"
        + "    a = 5,\n"
        + "    b = 6,\n"
        + "    c = 7,\n"
        + "    d = 12345678910L,\n"
        + "    e = 9.0f,\n"
        + "    f = 11.1,\n"
        + "    g = {\n"
        + "        '\\u0000',\n"
        + "        '쫾',\n"
        + "        'z',\n"
        + "        '€',\n"
        + "        'ℕ',\n"
        + "        '\"',\n"
        + "        '\\'',\n"
        + "        '\\t',\n"
        + "        '\\n'\n"
        + "    },\n"
        + "    h = true,\n"
        + "    i = AnnotationSpecTest.Breakfast.WAFFLES,\n"
        + "    j = @AnnotationSpecTest.AnnotationA,\n"
        + "    k = \"maple\",\n"
        + "    l = Override.class,\n"
        + "    m = {\n"
        + "        9,\n"
        + "        8,\n"
        + "        1\n"
        + "    },\n"
        + "    n = {\n"
        + "        AnnotationSpecTest.Breakfast.WAFFLES,\n"
        + "        AnnotationSpecTest.Breakfast.PANCAKES\n"
        + "    },\n"
        + "    o = AnnotationSpecTest.Breakfast.PANCAKES,\n"
        + "    p = 1701,\n"
        + "    q = @AnnotationSpecTest.AnnotationC(\"bar\"),\n"
        + "    r = {\n"
        + "        Float.class,\n"
        + "        Double.class\n"
        + "    }\n"
        + ")\n"
        + "class Taco {\n"
        + "}\n");
  }

  /**
   * Tests whether a `HasDefaultsAnnotation.Builder` throws a `NullPointerException`
   * when adding a member with a null name.
   */
  @Test public void disallowsNullMemberName() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    try {
      AnnotationSpec.Builder $L = builder.addMember(null, "$L", "");
      fail($L.build().toString());
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat().isEqualTo("name == null");
    }
  }

  /**
   * Tests if an annotation builder throws an illegal argument exception when adding a
   * member with an invalid name.
   */
  @Test public void requiresValidMemberName() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    try {
      AnnotationSpec.Builder $L = builder.addMember("@", "$L", "");
      fail($L.build().toString());
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("not a valid name: @");
    }
  }

  /**
   * Modifies the builder for a `SuppressWarnings` annotation, clearing and then
   * re-addding members to the builder. The resulting annotation is then obtained and
   * its string representation checked to be equal to `@java.lang.SuppressWarnings("Bar")`.
   */
  @Test public void modifyMembers() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "$S", "Foo");
    
    builder.members.clear();
    builder.members.put("value", Arrays.asList(CodeBlock.of("$S", "Bar")));

    assertThat(builder.build().toString()).isEqualTo("@java.lang.SuppressWarnings(\"Bar\")");
  }

  /**
   * Generates a string representation of the `TypeSpec` parameter by creating a
   * `JavaFile` instance and calling its `build()` method to generate the Java source
   * code for the type, and then returns the resulting string.
   * 
   * @param typeSpec Java class or interface that is being generated by the `JavaFile.builder`
   * method, and its `toString()` method is being called to return the generated source
   * code as a string.
   * 
   * @returns a string representation of the given `TypeSpec`.
   */
  private String toString(TypeSpec typeSpec) {
    return JavaFile.builder("com.squareup.tacos", typeSpec).build().toString();
  }
}
