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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.lang.model.SourceVersion;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * is designed to allocate unique names for various identifiers in a program. It
 * provides a way to generate new names that are not Java identifiers or clash with
 * other names, and it also allows retrieving a name created with a specific tag. The
 * class has a `newName()` method that generates a new name based on a given suggestion,
 * and it also has a `get()` method that retrieves a name created with a specific
 * tag. Additionally, the class provides a `clone()` method for creating a deep copy
 * of the NameAllocator.
 */
public final class NameAllocator implements Cloneable {
  private final Set<String> allocatedNames;
  private final Map<Object, String> tagToName;

  public NameAllocator() {
    this(new LinkedHashSet<>(), new LinkedHashMap<>());
  }

  private NameAllocator(LinkedHashSet<String> allocatedNames,
                        LinkedHashMap<Object, String> tagToName) {
    this.allocatedNames = allocatedNames;
    this.tagToName = tagToName;
  }

  /**
   * generates a unique string name based on a given suggestion and a randomly generated
   * UUID.
   * 
   * @param suggestion name to be created, which is then combined with a unique random
   * UUID string to generate a new name.
   * 
   * @returns a unique string consisting of a suggested name and a randomly generated
   * UUID.
   */
  public String newName(String suggestion) {
    return newName(suggestion, UUID.randomUUID().toString());
  }

  /**
   * takes a suggested name and an object tag, checks if the input is null, converts
   * the suggested name to a Java identifier, adds it to a set of allocated names, and
   * replaces the tag with a unique name if it is already in use.
   * 
   * @param suggestion name that will be generated for the object tag.
   * 
   * @param tag object that suggests the new name for the variable, and it is used to
   * determine whether the suggested name is already in use or not.
   * 
   * 	- `tag`: The original object that was passed as an argument to `newName`. This
   * can be any type of object, and its properties or attributes will depend on the
   * specific class it belongs to.
   * 	- `Object tag`: The `tag` parameter is of type `Object`, indicating that it can
   * hold any type of object.
   * 
   * @returns a unique identifier for the given tag.
   */
  public String newName(String suggestion, Object tag) {
    checkNotNull(suggestion, "suggestion");
    checkNotNull(tag, "tag");

    suggestion = toJavaIdentifier(suggestion);

    while (SourceVersion.isKeyword(suggestion) || !allocatedNames.add(suggestion)) {
      suggestion = suggestion + "_";
    }

    String replaced = tagToName.put(tag, suggestion);
    if (replaced != null) {
      tagToName.put(tag, replaced); // Put things back as they were!
      throw new IllegalArgumentException("tag " + tag + " cannot be used for both '" + replaced
          + "' and '" + suggestion + "'");
    }

    return suggestion;
  }

  /**
   * converts a given string into a Java identifier by replacing non-Java identifier
   * starting and ending characters with an underscore, and replacing non-Java identifier
   * parts with a single underscore. The resulting identifier is then returned as a String.
   * 
   * @param suggestion string to be converted into a Java identifier.
   * 
   * @returns a Java-compliant identifier string.
   */
  public static String toJavaIdentifier(String suggestion) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < suggestion.length(); ) {
      int codePoint = suggestion.codePointAt(i);
      if (i == 0
          && !Character.isJavaIdentifierStart(codePoint)
          && Character.isJavaIdentifierPart(codePoint)) {
        result.append("_");
      }

      int validCodePoint = Character.isJavaIdentifierPart(codePoint) ? codePoint : '_';
      result.appendCodePoint(validCodePoint);
      i += Character.charCount(codePoint);
    }
    return result.toString();
  }

  /**
   * maps an object to a string value in a named map called `tagToName`. It returns the
   * corresponding string if present in the map, otherwise it throws an `IllegalArgumentException`.
   * 
   * @param tag object that contains the name of the tag to be retrieved.
   * 
   * 	- `result`: This is a String variable that stores the name of the tag.
   * 	- `tagToName`: This is an instance of a `Map` class that maps tags to their
   * corresponding names. The map contains entries for each valid tag, where each entry
   * consists of a tag as the key and its corresponding name as the value.
   * 
   * @returns a string representing the name of the tag passed as an argument.
   */
  public String get(Object tag) {
    String result = tagToName.get(tag);
    if (result == null) {
      throw new IllegalArgumentException("unknown tag: " + tag);
    }
    return result;
  }

  /**
   * creates a copy of the `NameAllocator` object, including its internal maps of
   * allocated names and tag-to-name mappings.
   * 
   * @returns a new instance of the `NameAllocator` class with identical allocated names
   * and tag-to-name mappings as the original instance.
   * 
   * 	- The returned instance is an identical copy of the original `NameAllocator`,
   * with the same allocated names and tag-to-name mapping as the original.
   * 	- The `LinkedHashSet` used to store the allocated names is a shallow clone, meaning
   * that it contains references to the same objects as the original set.
   * 	- The `LinkedHashMap` used to store the tag-to-name mapping is also a shallow
   * clone, with the same mappings as the original map.
   * 
   * Overall, the `clone` function returns a new instance of `NameAllocator` that is
   * an exact copy of the original, allowing for easy cloning and sharing of instances
   * without modifying their internal state.
   */
  @Override
  public NameAllocator clone() {
    return new NameAllocator(
        new LinkedHashSet<>(this.allocatedNames),
        new LinkedHashMap<>(this.tagToName));
  }

}
