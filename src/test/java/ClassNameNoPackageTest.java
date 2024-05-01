/*
 * Copyright (C) 2019 Square, Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.squareup.javapoet.ClassName;
import org.junit.Test;

/**
 * is a test class that verifies the functionality of the ClassName class in Java.
 * The class has a single method called shouldSupportClassInDefaultPackage() which
 * tests the package name, simple name, and toString representation of the class.
 */
public final class ClassNameNoPackageTest {
  /**
   * verifies that a class in the default package has the expected properties, including
   * an empty package name and a simple name matching the class name.
   */
  @Test public void shouldSupportClassInDefaultPackage() {
    ClassName className = ClassName.get(ClassNameNoPackageTest.class);
    assertThat(className.packageName()).isEqualTo("");
    assertThat(className.simpleName()).isEqualTo("ClassNameNoPackageTest");
    assertThat(className.toString()).isEqualTo("ClassNameNoPackageTest");
  }
}
