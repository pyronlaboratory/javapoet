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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;

import static java.lang.Character.isISOControl;

/**
 * is a collection of static utility methods for working with various data structures
 * in Java. The class provides methods for creating immutable maps and multimaps,
 * checking arguments and states, and working with character literals in string
 * literals. Additionally, the class includes methods for requiring exactly one
 * modifier among a set of mutually exclusive modifiers, and for wrapping double
 * quotes in a string literal.
 */
final class Util {
  private Util() {
  }

  /**
   * creates an immutable multimap by transforming a mutable map into an immutable one,
   * while preserving the original data structure.
   * 
   * @param multimap map that will be transformed into an immutable multimap by the function.
   * 
   * 	- `Map<K, List<V>> multimap`: A map that contains key-value pairs where each key
   * is associated with a list of values.
   * 	- `LinkedHashMap<K, List<V>> result`: The resulting immutable map created by the
   * function, which contains the same key-value pairs as `multimap`.
   * 	- `entrySet()`: An method that returns a set of all the key-value pairs in `multimap`.
   * 	- `isEmpty()`: A method that checks if the list of values associated with a key
   * is empty. If it is, the corresponding key is skipped in the resulting map.
   * 
   * @returns an immutable map containing the original multimap's key-value pairs, where
   * each value is transformed into an immutable list.
   * 
   * 1/ Type: The return type is `Map`, specifically `LinkedHashMap`. This indicates
   * that the map is a linked one, which means that each key-value pair is stored in a
   * separate node, and the map's performance is optimized for random access to keys
   * and values.
   * 2/ Immutability: The function returns an immutable map, meaning that once it is
   * created, its contents cannot be modified. This ensures that the map is thread-safe
   * and provides predictable behavior.
   * 3/ Key Set: The set of keys in the returned map is the same as the input multimap's
   * key set.
   * 4/ Value Lists: Each key in the returned map has a list of values associated with
   * it, which is also immutable.
   * 5/ Unmodifiable: The map is unmodifiable, meaning that any attempts to modify its
   * contents will result in an `IllegalArgumentException`. This ensures that the map
   * is used correctly and provides consistent behavior.
   */
  static <K, V> Map<K, List<V>> immutableMultimap(Map<K, List<V>> multimap) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>();
    for (Map.Entry<K, List<V>> entry : multimap.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      result.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * creates an immutable map by wrapping an existing map with a view of an unmodifiable
   * map, ensuring that the map cannot be modified.
   * 
   * @param map Map that will be immutableized.
   * 
   * 	- The input map is of type `Map<K, V>`.
   * 	- It is an instance of the `LinkedHashMap` class, which provides constant-time
   * map operations.
   * 	- The map's keys and values are of type `K` and `V`, respectively.
   * 
   * @returns an unmodifiable Map instance of the given map.
   * 
   * 	- The output is a `Map` object, specifically an `ImmutableMap`, which means it
   * cannot be modified once created.
   * 	- It is a copy of the original `Map` object, created using `Collections.unmodifiableMap()`.
   * 	- The `Map` object returned by `immutableMap()` has the same key-value pairs as
   * the original map.
   * 	- The `Map` object is immutable, meaning its state cannot be changed once it is
   * created.
   */
  static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  /**
   * verifies a condition and throws an `IllegalArgumentException` if it is not met.
   * It takes a format string and arguments as parameters.
   * 
   * @param condition condition that must be true for the function to execute without
   * throwing an exception.
   * 
   * @param format format string for the error message to be thrown when the condition
   * is not met.
   */
  static void checkArgument(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalArgumentException(String.format(format, args));
  }

  /**
   * checks if a given reference is null and throws a `NullPointerException` if it is.
   * It returns the original reference if it is not null.
   * 
   * @param reference object that needs to be checked for nullity before returning it.
   * 
   * 	- It is a type parameter `T`, indicating that it can hold any object type.
   * 	- It is passed as an argument to the function, ensuring that it is not null when
   * the function is called.
   * 	- If `reference` is null, a `NullPointerException` is thrown with a message
   * formatted using the `String.format()` method and the provided `args`.
   * 
   * @param format formatting pattern for the error message that will be thrown if the
   * `reference` is null.
   * 
   * @returns the referenced object, or an exception if the reference is null.
   * 
   * The input parameter `reference` is of type `T`, which can be any object type.
   * 
   * The function checks if the reference is null and throws a `NullPointerException`
   * if it is.
   * 
   * The function returns the original reference unmodified, so its value will be the
   * same as the original reference.
   * 
   * The function does not perform any additional validation or manipulation on the
   * input parameter, so the output is simply the original reference.
   */
  static <T> T checkNotNull(T reference, String format, Object... args) {
    if (reference == null) throw new NullPointerException(String.format(format, args));
    return reference;
  }

  /**
   * checks whether a given condition is true or false and throws an `IllegalStateException`
   * if it's not.
   * 
   * @param condition basis for determining whether an exception should be thrown when
   * the function is called.
   * 
   * @param format message to be displayed as an exception when the condition is not met.
   */
  static void checkState(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalStateException(String.format(format, args));
  }

  /**
   * converts a given collection to an immutable list.
   * 
   * @param collection Collection of objects to be converted into an immutable List.
   * 
   * 	- It is a `Collection<T>` object, where `T` is any type that satisfies the
   * `java.lang.Object` interface.
   * 	- It contains zero or more elements, all of which are also instances of `T`.
   * 	- The elements are stored in a list, specifically an instance of `ArrayList`.
   * 	- The list is immutable, meaning its elements cannot be modified once they are
   * added to the list.
   * 
   * @returns an immutable list based on the input collection.
   * 
   * 	- The function returns an unmodifiable list, which means that any attempts to
   * modify the list will result in an UnsupportedOperationException.
   * 	- The list is constructed from a Collection, meaning that the order and contents
   * of the original collection are preserved.
   * 	- The list is immutable, ensuring that changes cannot be made to its state once
   * it has been created.
   */
  static <T> List<T> immutableList(Collection<T> collection) {
    return Collections.unmodifiableList(new ArrayList<>(collection));
  }

  /**
   * creates a new immutable set from a given collection, using unmodifiable set creation.
   * 
   * @param set collection of objects that will be converted into an immutable set.
   * 
   * 1/ Type: The type parameter `T` represents any class that is assignable to `LinkedHashSet`.
   * 2/ Collection: The input `set` is a collection of elements of type `T`.
   * 3/ Modifiability: The returned set is unmodifiable, meaning it cannot be modified
   * once created.
   * 4/ Size: The size of the input `set` is equal to its cardinality.
   * 
   * @returns an immutable Set instance containing the elements of the given Collection.
   * 
   * 	- The Set is an unmodifiable set, meaning it cannot be modified once created.
   * 	- It is returned as a new LinkedHashSet instance containing the elements of the
   * provided Collection.
   * 	- The LinkedHashSet maintains the order of the elements in the original Collection.
   * 	- The LinkedHashSet is immutable, meaning its state cannot be changed after creation.
   */
  static <T> Set<T> immutableSet(Collection<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  /**
   * takes two sets as input, combines them into a new set, and returns it. The resulting
   * set contains all elements from either input set.
   * 
   * @param a 1st set that will be unioned with the 2nd input parameter `b`.
   * 
   * 	- It is of type `Set<T>`, indicating that it is an unordered set of elements of
   * type `T`.
   * 	- The elements in `a` can be added to the resulting set.
   * 	- `a` has a size of at least 1, since it is not empty.
   * 
   * @param b 2nd set to be merged with the 1st set `a`, resulting in the updated union
   * set.
   * 
   * 	- `a`: The first operand is a `Set` object that contains elements to be added to
   * the result set.
   * 	- `b`: The second operand is also a `Set` object that contains elements to be
   * added to the result set after removing any duplicates with `a`.
   * 	- Return value: A new `Set` object that contains all the elements from both `a`
   * and `b`, without duplicates.
   * 
   * @returns a new set containing all elements from both input sets.
   * 
   * 	- The `Set<T>` returned is always non-empty, as it contains elements from both
   * `a` and `b`.
   * 	- The `Set<T>` has a unique set of elements, as duplicates are removed during the
   * `addAll` operation.
   * 	- The order of elements in the resulting set is determined by the order of their
   * insertion into the sets `a` and `b`, followed by the `addAll` operation.
   */
  static <T> Set<T> union(Set<T> a, Set<T> b) {
    Set<T> result = new LinkedHashSet<>();
    result.addAll(a);
    result.addAll(b);
    return result;
  }

  /**
   * checks that a set of modifiers contains exactly one element from a list of mutually
   * exclusive modifiers.
   * 
   * @param modifiers set of modifiers to be checked for containment.
   * 
   * 	- The type of `modifiers` is `Set`, indicating that it is an unordered set of elements.
   * 	- The elements of `modifiers` can be any instance of `Modifier`, which is a generic
   * type parameter.
   * 	- The `mutuallyExclusive` argument is provided as an array of `Modifier` objects,
   * indicating that these are the modifiers that must be present in `modifiers`.
   * 	- The `count` variable is initialized to 0 and incremented for each `Modifier`
   * present in `modifiers` that is not also present in `mutuallyExclusive`.
   * 
   * The function then checks if exactly one of the modifiers in `mutuallyExclusive`
   * is present in `modifiers`, using a custom message based on the properties of
   * `modifiers` and `mutuallyExclusive`.
   */
  static void requireExactlyOneOf(Set<Modifier> modifiers, Modifier... mutuallyExclusive) {
    int count = 0;
    for (Modifier modifier : mutuallyExclusive) {
      if (modifiers.contains(modifier)) count++;
    }
    checkArgument(count == 1, "modifiers %s must contain one of %s",
        modifiers, Arrays.toString(mutuallyExclusive));
  }

  /**
   * maps a single character to its literal representation in Java, taking into account
   * special characters and ISO controls.
   * 
   * @param c 8-bit binary code of a character, and the function returns the corresponding
   * Unicode escape sequence or the character itself depending on its value.
   * 
   * @returns a string representing the corresponding character or control sequence in
   * Java syntax.
   */
  static String characterLiteralWithoutSingleQuotes(char c) {
    // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
    switch (c) {
      case '\b': return "\\b"; /* \u0008: backspace (BS) */
      case '\t': return "\\t"; /* \u0009: horizontal tab (HT) */
      case '\n': return "\\n"; /* \u000a: linefeed (LF) */
      case '\f': return "\\f"; /* \u000c: form feed (FF) */
      case '\r': return "\\r"; /* \u000d: carriage return (CR) */
      case '\"': return "\"";  /* \u0022: double quote (") */
      case '\'': return "\\'"; /* \u0027: single quote (') */
      case '\\': return "\\\\";  /* \u005c: backslash (\) */
      default:
        return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
    }
  }

  /**
   * takes a string value and an indent as input, and returns a new string with double
   * quotes escaped or unescaped based on the character.
   * 
   * @param value String value to be wrapped in double quotes and escaped for special
   * characters.
   * 
   * @param indent 0-based index of an indentation level, which is added to the resulting
   * string literal after each newline character.
   * 
   * @returns a string literal with double quotes, containing escaped and unescaped
   * characters as needed.
   */
  static String stringLiteralWithDoubleQuotes(String value, String indent) {
    StringBuilder result = new StringBuilder(value.length() + 2);
    result.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      // trivial case: single quote must not be escaped
      if (c == '\'') {
        result.append("'");
        continue;
      }
      // trivial case: double quotes must be escaped
      if (c == '\"') {
        result.append("\\\"");
        continue;
      }
      // default case: just let character literal do its work
      result.append(characterLiteralWithoutSingleQuotes(c));
      // need to append indent after linefeed?
      if (c == '\n' && i + 1 < value.length()) {
        result.append("\"\n").append(indent).append(indent).append("+ \"");
      }
    }
    result.append('"');
    return result.toString();
  }
}
