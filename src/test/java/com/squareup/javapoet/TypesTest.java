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

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.testing.compile.CompilationRule;

/**
 * is a Java class that extends the AbstractTypesTest class and provides a framework
 * for testing the types in Java code using the JUnit4 framework. It defines a
 * CompilationRule field and overrides the getElements() and getTypes() methods to
 * access the compilation elements and types, respectively.
 */
@RunWith(JUnit4.class)
public final class TypesTest extends AbstractTypesTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  /**
   * in the provided code returns a collection of elements compiled by the `compilation`
   * object.
   * 
   * @returns a collection of `Element` objects representing the elements in the
   * compilation unit.
   * 
   * 	- The `Elements` object is a representation of the compilation's elements, which
   * includes methods for accessing and manipulating the elements.
   * 	- The `compilation` field refers to the compilation being worked on, which provides
   * access to various information about the code being compiled.
   * 	- The `getElements` function returns a reference to an `Elements` object, indicating
   * that it is possible to manipulate the elements within the compilation.
   */
  @Override
  protected Elements getElements() {
    return compilation.getElements();
  }

  /**
   * returns a `Types` object, which represents the type information of the current
   * compilation context. This allows for the proper resolution of references and the
   * enforcement of type constraints during code execution.
   * 
   * @returns a collection of type information derived from the compiling code.
   * 
   * The Types object returned is compilation.getTypes(), which contains information
   * about the types in the compilation unit being analyzed.
   * It has several attributes such as typeKind, typeName, and so on that describe its
   * characteristics.
   */
  @Override
  protected Types getTypes() {
    return compilation.getTypes();
  }
}
