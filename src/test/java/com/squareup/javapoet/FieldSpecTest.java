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

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import javax.lang.model.element.Modifier;

/**
 * is a Java file that contains several tests for the FieldSpec class. The tests cover
 * various aspects of the FieldSpec class, including equality and hash code, null
 * annotations addition, annotation removals, and modifier removals. The tests are
 * written in a formal and neutral tone, without any first-person statements or
 * personal pronouns.
 */
public class FieldSpecTest {
  /**
   * tests whether two FieldSpecs are equal and hashable based on their class, name,
   * modifiers, and other attributes.
   */
  @Test public void equalsAndHashCode() {
    FieldSpec a = FieldSpec.builder(int.class, "foo").build();
    FieldSpec b = FieldSpec.builder(int.class, "foo").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
    a = FieldSpec.builder(int.class, "FOO", Modifier.PUBLIC, Modifier.STATIC).build();
    b = FieldSpec.builder(int.class, "FOO", Modifier.PUBLIC, Modifier.STATIC).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  /**
   * tests whether an exception is thrown when attempting to add annotations to a field
   * spec with null annotations.
   */
  @Test public void nullAnnotationsAddition() {
    try {
      FieldSpec.builder(int.class, "foo").addAnnotations(null);
      fail();
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  /**
   * modifies an instance of `FieldSpec.Builder`, removing an annotation from a list
   * of annotations added to the field.
   */
  @Test public void modifyAnnotations() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
          .addAnnotation(Override.class)
          .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  /**
   * modifies the modifiers of a `FieldSpec`. Specifically, it removes the second
   * modifier from the list of modifiers for the `FieldSpec`.
   */
  @Test public void modifyModifiers() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }
}
