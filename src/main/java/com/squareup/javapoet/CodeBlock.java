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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.Util.checkArgument;

/**
 * in Java is a utility class for building and manipulating code blocks, which are
 * sequences of code that can be used to generate code snippets or to perform formatting
 * operations on existing code. The class provides various methods for adding and
 * manipulating code blocks, as well as joining them together into a single code
 * block. Additionally, it includes a builder pattern for creating instances of the
 * class with customized options.
 */
public final class CodeBlock {
  private static final Pattern NAMED_ARGUMENT =
      Pattern.compile("\\$(?<argumentName>[\\w_]+):(?<typeChar>[\\w]).*");
  private static final Pattern LOWERCASE = Pattern.compile("[a-z]+[\\w_]*");

  /** A heterogeneous list containing string literals and value placeholders. */
  final List<String> formatParts;
  final List<Object> args;

  private CodeBlock(Builder builder) {
    this.formatParts = Util.immutableList(builder.formatParts);
    this.args = Util.immutableList(builder.args);
  }

  /**
   * returns a boolean value indicating whether the `formatParts` collection is empty.
   * 
   * @returns a boolean value indicating whether the `formatParts` collection is empty.
   */
  public boolean isEmpty() {
    return formatParts.isEmpty();
  }

  /**
   * compares the object with an arbitrary reference and returns a boolean indicating
   * whether they are equal based on a string comparison.
   * 
   * @param o object being compared to the current object for equality testing, and is
   * used to determine if the objects are equal based on their toString() method output.
   * 
   * 	- If `this` and `o` are the same reference, the method returns `true`.
   * 	- If `o` is `null`, the method returns `false`.
   * 	- If the classes of `this` and `o` are different, the method returns `false`.
   * 	- Otherwise, the method compares the strings of `this` and `o` using the `equals`
   * method.
   * 
   * @returns a boolean value indicating whether the object being compared is equal to
   * the current object.
   * 
   * 	- The first `if` statement checks if the object being compared to this object is
   * the same as this object itself. If so, the method returns `true`.
   * 	- The second `if` statement checks if the object being compared to this object
   * is `null`. If it is, the method returns `false`.
   * 	- The third `if` statement checks if the classes of the two objects being compared
   * are different. If they are, the method returns `false`.
   * 	- The final `if` statement compares the strings of the two objects using the
   * `toString()` method. If they are equal, the method returns `true`, otherwise it
   * returns `false`.
   */
  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  /**
   * overrides the default implementation by calling `toString()` and returning its
   * `hashCode`. This allows the object to provide its own unique hash value instead
   * of relying on the default implementation.
   * 
   * @returns an integer representing the hash code of the object's `toString()` method.
   */
  @Override public int hashCode() {
    return toString().hashCode();
  }

  /**
   * generates a string representation of its input, using the `CodeWriter` interface
   * to emit the output.
   * 
   * @returns a string representation of the current object.
   */
  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      new CodeWriter(out).emit(this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /**
   * creates a `CodeBlock` instance by combining a format string and input arguments
   * using a builder pattern.
   * 
   * @param format string to be formatted and is used by the `Builder` class to construct
   * a `CodeBlock` object.
   * 
   * @returns a `CodeBlock` instance representing a code snippet with the specified
   * format and arguments.
   * 
   * 	- The output is a `CodeBlock` object, which represents a piece of code in a
   * programming language.
   * 	- The `CodeBlock` object can be constructed using the `add()` method, which allows
   * for adding multiple code elements to it.
   * 	- In this specific implementation, the `add()` method is used to add a single
   * line of code in the format specified by the `format` parameter.
   * 	- The `args` parameter is an array of objects that can be used as arguments for
   * the code element being constructed.
   */
  public static CodeBlock of(String format, Object... args) {
    return new Builder().add(format, args).build();
  }

  /**
   * aggregates an iterable of `CodeBlock` objects into a single code block using a
   * specified separator.
   * 
   * @param codeBlocks Iterable of CodeBlock objects that will be joined together using
   * the specified separator.
   * 
   * 	- The type of `codeBlocks` is `Iterable<CodeBlock>`, indicating that it is an
   * iterable container of `CodeBlock` objects.
   * 	- The `spliterator` method is called on `codeBlocks`, which returns a `Spliterator`
   * object that can be used to iterate over the elements of `codeBlocks`.
   * 	- The `collect` method is called on the resulting stream, with the `joining`
   * method as its argument. This method takes two arguments: the first is the separator
   * string, and the second is a function that takes each element from the stream and
   * returns a string representation of it. By calling `collect` in this way, the method
   * can concatenate all the strings generated by the stream into a single string
   * representing the joined output.
   * 
   * @param separator string used to concatenate the CodeBlocks when they are collected
   * into a single CodeBlock using the `joining()` method.
   * 
   * @returns a single CodeBlock containing the concatenated code blocks from the input
   * iterable, separated by the specified separator.
   */
  public static CodeBlock join(Iterable<CodeBlock> codeBlocks, String separator) {
    return StreamSupport.stream(codeBlocks.spliterator(), false).collect(joining(separator));
  }

  /**
   * creates a collector that combines multiple `CodeBlock` objects using a specified
   * separator string. It provides three methods to merge, join and add elements to the
   * collection.
   * 
   * @param separator string used to separate  code blocks when joining them.
   * 
   * @returns a collector that joins multiple code blocks using a specified separator.
   * 
   * 	- The function returns a Collector instance that can be used to collect code blocks.
   * 	- The first argument of the Collector is a Supplier that produces a new CodeBlockJoiner
   * object every time it is called.
   * 	- The second argument is an accumulator method that takes a CodeBlockJoiner object
   * as its argument and adds a new code block to it using the `add` method.
   * 	- The third argument is a merger method that takes two CodeBlockJoiner objects
   * as its arguments and merges them into a single CodeBlockJoinner object using the
   * `merge` method.
   * 	- The fourth argument is a joiner method that takes a CodeBlockJoiner object as
   * its argument and joins all the code blocks collected so far using the `join` method.
   */
  public static Collector<CodeBlock, ?, CodeBlock> joining(String separator) {
    return Collector.of(
        () -> new CodeBlockJoiner(separator, builder()),
        CodeBlockJoiner::add,
        CodeBlockJoiner::merge,
        CodeBlockJoiner::join);
  }

  /**
   * generates a collector that can join multiple `CodeBlock` objects using a specified
   * separator and prefix/suffix strings. It creates a builder to build the resulting
   * code block and provides methods to add, merge, and finalize the joining process.
   * 
   * @param separator text to be used as a separator between code blocks when joining
   * them using the `joining()` method.
   * 
   * @param prefix starting sequence of characters that are concatenated with the
   * separated blocks when joining them.
   * 
   * @param suffix 3rd argument to be added to the joined code blocks, following the
   * separator and prefix.
   * 
   * @returns a collector that combines multiple code blocks using a specified separator.
   * 
   * 	- The output is of type `Collector<CodeBlock, ?, CodeBlock>`, indicating that it
   * is a collector that takes a sequence of `CodeBlock` objects as input and returns
   * a new sequence of `CodeBlock` objects.
   * 	- The function returns a `Builder` instance, which is used to construct the final
   * output sequence.
   * 	- The `add` method of the `Builder` instance takes a single argument of type
   * `CodeBlock`, which is added to the current sequence of `CodeBlock` objects.
   * 	- The `merge` method of the `Builder` instance takes no arguments and simply
   * merges the current sequence of `CodeBlock` objects with any previously merged sequences.
   * 	- The function also defines a Supplier that takes no arguments and returns a new
   * `CodeBlockJoiner` instance. This supplier is used to construct the final output sequence.
   * 	- The `add` and `merge` methods of the `CodeBlockJoiner` instance take a single
   * argument of type `CodeBlock`, which is added to the current sequence of `CodeBlock`
   * objects or merged with any previously merged sequences, respectively.
   * 	- The `join` method of the `CodeBlockJoiner` instance takes no arguments and
   * simply returns the final output sequence constructed by merging all the input
   * sequences using the `add` and `merge` methods.
   */
  public static Collector<CodeBlock, ?, CodeBlock> joining(
      String separator, String prefix, String suffix) {
    Builder builder = builder().add("$N", prefix);
    return Collector.of(
        () -> new CodeBlockJoiner(separator, builder),
        CodeBlockJoiner::add,
        CodeBlockJoiner::merge,
        joiner -> {
            builder.add(CodeBlock.of("$N", suffix));
            return joiner.join();
        });
  }

  /**
   * creates a new instance of the `Builder` class, providing a default implementation
   * for building objects.
   * 
   * @returns a new instance of the `Builder` class.
   * 
   * 	- A new Builder object is returned.
   * 	- The Builder object has all the fields of a new Java object with default values.
   * 	- The Builder object allows for additional configuration options to be added or
   * modified before building the final Java object.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * creates a new `Builder` instance with the same format parts and arguments as the
   * current instance, allowing for easy copying and modification of the object.
   * 
   * @returns a new `Builder` instance with the added `formatParts` and `args`.
   * 
   * 	- The returned object is of type `Builder`, which represents a builder for creating
   * a new instance of the same class as the original object.
   * 	- The `formatParts` list and `args` list are copied into the new builder, allowing
   * modification of these lists to customize the construction of the new object.
   * 	- The returned builder has the same state as the original object, including any
   * nested objects or references.
   */
  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.formatParts.addAll(formatParts);
    builder.args.addAll(args);
    return builder;
  }

  /**
   * is an abstract class that provides a simple and intuitive way to generate code
   * blocks in Gradle. It allows users to build a code block by adding various parts
   * such as format strings, arguments, control flow statements, and statements. The
   * class provides methods for adding each of these parts and also provides a clear()
   * method to reset the state of the builder. The build() method returns a CodeBlock
   * object that represents the generated code block.
   */
  public static final class Builder {
    final List<String> formatParts = new ArrayList<>();
    final List<Object> args = new ArrayList<>();

    private Builder() {
    }

    /**
     * checks if the `formatParts` collection is empty.
     * 
     * @returns a boolean value indicating whether the `formatParts` collection is empty.
     */
    public boolean isEmpty() {
      return formatParts.isEmpty();
    }

    /**
     * modifies a `Builder` object by adding named arguments to a format string, based
     * on a map of argument values.
     * 
     * @param format format string that the `addNamed()` method applies named arguments
     * to.
     * 
     * @param arguments map of named arguments that are to be applied to the format string
     * using the `$` placeholder.
     * 
     * 	- `keySet()`: Returns a set of all keys in the map, including the null key.
     * 	- `containsKey()`: Checks if a specific key is present in the map.
     * 	- `get()`: Returns the value associated with a specific key.
     * 	- `size()`: Returns the number of key-value pairs in the map.
     * 	- `isEmpty()`: Returns true if the map has no elements, false otherwise.
     * 	- `clear()`: Removes all elements from the map.
     * 	- `contains()`: Checks if a specific value is present in the map.
     * 
     * The `addArgument` method takes three parameters:
     * 
     * 	- `format`: The format string to be processed.
     * 	- `typeChar`: The character that indicates the type of argument, such as `$` for
     * a named argument or `%` for a positional argument.
     * 	- `value`: The value associated with the argument.
     * 
     * @returns a modified `Builder` object with additional named arguments added to the
     * format string.
     * 
     * 1/ The `formatParts` list contains the parts of the format string that have been
     * processed so far, including any named arguments and placeholders.
     * 2/ The `addArgument` method is used to add an argument to the format string, with
     * the type character (either a dollar sign or a colon) and the value for the named
     * argument.
     * 3/ The `checkArgument` method is used to verify that the provided argument is
     * present in the `arguments` map, and to raise an error if it is not.
     * 4/ The `isNoArgPlaceholder` method is used to determine whether a particular
     * character in the format string is a no-argument placeholder.
     * 5/ The `p` variable tracks the current position in the format string being processed.
     * 6/ The `format` parameter is the original format string being processed.
     */
    public Builder addNamed(String format, Map<String, ?> arguments) {
      int p = 0;

      for (String argument : arguments.keySet()) {
        checkArgument(LOWERCASE.matcher(argument).matches(),
            "argument '%s' must start with a lowercase character", argument);
      }

      while (p < format.length()) {
        int nextP = format.indexOf("$", p);
        if (nextP == -1) {
          formatParts.add(format.substring(p));
          break;
        }

        if (p != nextP) {
          formatParts.add(format.substring(p, nextP));
          p = nextP;
        }

        Matcher matcher = null;
        int colon = format.indexOf(':', p);
        if (colon != -1) {
          int endIndex = Math.min(colon + 2, format.length());
          matcher = NAMED_ARGUMENT.matcher(format.substring(p, endIndex));
        }
        if (matcher != null && matcher.lookingAt()) {
          String argumentName = matcher.group("argumentName");
          checkArgument(arguments.containsKey(argumentName), "Missing named argument for $%s",
              argumentName);
          char formatChar = matcher.group("typeChar").charAt(0);
          addArgument(format, formatChar, arguments.get(argumentName));
          formatParts.add("$" + formatChar);
          p += matcher.regionEnd();
        } else {
          checkArgument(p < format.length() - 1, "dangling $ at end");
          checkArgument(isNoArgPlaceholder(format.charAt(p + 1)),
              "unknown format $%s at %s in '%s'", format.charAt(p + 1), p + 1, format);
          formatParts.add(format.substring(p, p + 2));
          p += 2;
        }
      }

      return this;
    }

    /**
     * adds format placeholders and arguments to a `Builder`. It consumes format string
     * characters ($) and optional digits (0-9), and adds corresponding argument(s). It
     * handles mixed indexed and relative positional parameters, and reports unused arguments.
     * 
     * @param format method being called, and it is used to construct the string of format
     * parts that will be passed to the `add()` method.
     * 
     * @returns a builder object that can be used to create a new Java object.
     * 
     * 1/ `hasRelative`: A boolean variable that indicates whether any relative parameters
     * were present in the format string. If it is true, then there are unused arguments
     * that need to be handled.
     * 2/ `hasIndexed`: A boolean variable that indicates whether any indexed parameters
     * were present in the format string. If it is true, then the `indexedParameterCount`
     * array is non-zero.
     * 3/ `relativeParameterCount`: An integer variable that keeps track of the number
     * of relative parameters consumed by the `add` function so far. It is updated whenever
     * a new relative parameter is found in the format string.
     * 4/ `indexedParameterCount`: An array of integers that keeps track of the number
     * of arguments for each indexed parameter present in the format string. The length
     * of this array is equal to the number of indexed parameters present in the format
     * string.
     * 5/ `addArgument`: A method that adds an argument to the format string based on the
     * current state (hasIndexed or hasRelative). It takes three arguments: the format
     * string, a character representing the type of argument (either a digit for an indexed
     * parameter or a dollar sign for a relative parameter), and the actual argument value.
     * 
     * The `add` function returns a new instance of the `Builder` class, indicating that
     * it has modified the original input in some way.
     */
    public Builder add(String format, Object... args) {
      boolean hasRelative = false;
      boolean hasIndexed = false;

      int relativeParameterCount = 0;
      int[] indexedParameterCount = new int[args.length];

      for (int p = 0; p < format.length(); ) {
        if (format.charAt(p) != '$') {
          int nextP = format.indexOf('$', p + 1);
          if (nextP == -1) nextP = format.length();
          formatParts.add(format.substring(p, nextP));
          p = nextP;
          continue;
        }

        p++; // '$'.

        // Consume zero or more digits, leaving 'c' as the first non-digit char after the '$'.
        int indexStart = p;
        char c;
        do {
          checkArgument(p < format.length(), "dangling format characters in '%s'", format);
          c = format.charAt(p++);
        } while (c >= '0' && c <= '9');
        int indexEnd = p - 1;

        // If 'c' doesn't take an argument, we're done.
        if (isNoArgPlaceholder(c)) {
          checkArgument(
              indexStart == indexEnd, "$$, $>, $<, $[, $], $W, and $Z may not have an index");
          formatParts.add("$" + c);
          continue;
        }

        // Find either the indexed argument, or the relative argument. (0-based).
        int index;
        if (indexStart < indexEnd) {
          index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1;
          hasIndexed = true;
          if (args.length > 0) {
            indexedParameterCount[index % args.length]++; // modulo is needed, checked below anyway
          }
        } else {
          index = relativeParameterCount;
          hasRelative = true;
          relativeParameterCount++;
        }

        checkArgument(index >= 0 && index < args.length,
            "index %d for '%s' not in range (received %s arguments)",
            index + 1, format.substring(indexStart - 1, indexEnd + 1), args.length);
        checkArgument(!hasIndexed || !hasRelative, "cannot mix indexed and positional parameters");

        addArgument(format, c, args[index]);

        formatParts.add("$" + c);
      }

      if (hasRelative) {
        checkArgument(relativeParameterCount >= args.length,
            "unused arguments: expected %s, received %s", relativeParameterCount, args.length);
      }
      if (hasIndexed) {
        List<String> unused = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
          if (indexedParameterCount[i] == 0) {
            unused.add("$" + (i + 1));
          }
        }
        String s = unused.size() == 1 ? "" : "s";
        checkArgument(unused.isEmpty(), "unused argument%s: %s", s, String.join(", ", unused));
      }
      return this;
    }

    /**
     * determines if a given character is a no-argument placeholder character, which
     * includes some special characters such as `$`, `>`, `<`, `[`, `]`, `W`, and `Z`.
     * 
     * @param c 16th bit of an integer value, checking whether it is a no-argument
     * placeholder character.
     * 
     * @returns a boolean value indicating whether the given character is a no-argument
     * placeholder.
     */
    private boolean isNoArgPlaceholder(char c) {
      return c == '$' || c == '>' || c == '<' || c == '[' || c == ']' || c == 'W' || c == 'Z';
    }

    /**
     * adds an argument to a list based on the character code passed as parameter, where
     * 'N', 'L', 'S', and 'T' are used for adding named, literal, string, and type arguments
     * respectively.
     * 
     * @param format string to be parsed and processed by the function.
     * 
     * @param c 3rd argument of the `addArgument()` method and specifies the type of the
     * argument to be added, with possible values 'N', 'L', 'S', or 'T'.
     * 
     * @param arg 3rd argument of the `addArgument()` method, which is used to determine
     * the type of the argument based on its value.
     * 
     * 	- `argToName`: This method is used to convert an object into a name.
     * 	- `argToLiteral`: This method is used to convert an object into a literal value.
     * 	- `argToString`: This method is used to convert an object into a string value.
     * 	- `argToType`: This method is used to convert an object into a type value.
     */
    private void addArgument(String format, char c, Object arg) {
      switch (c) {
        case 'N':
          this.args.add(argToName(arg));
          break;
        case 'L':
          this.args.add(argToLiteral(arg));
          break;
        case 'S':
          this.args.add(argToString(arg));
          break;
        case 'T':
          this.args.add(argToType(arg));
          break;
        default:
          throw new IllegalArgumentException(
              String.format("invalid format string: '%s'", format));
      }
    }

    /**
     * converts an input object into a string representation of its name, handling various
     * types of objects through a series of if-else statements.
     * 
     * @param o object to convert to a string, and it is checked against various types
     * of objects using if-else statements to determine the appropriate conversion to perform.
     * 
     * 	- If `o` is an instance of `CharSequence`, the function returns a string
     * representation of `o`.
     * 	- If `o` is an instance of `ParameterSpec`, the function returns the name of the
     * parameter.
     * 	- If `o` is an instance of `FieldSpec`, the function returns the name of the field.
     * 	- If `o` is an instance of `MethodSpec`, the function returns the name of the method.
     * 	- If `o` is an instance of `TypeSpec`, the function returns the name of the type.
     * 	- If any other type of object is passed to the function, an `IllegalArgumentException`
     * is thrown with a message indicating that the expected input was not provided.
     * 
     * @returns a string representing the name of the input argument.
     * 
     * 	- If the input `o` is an instance of `CharSequence`, the function returns a string
     * representation of the object.
     * 	- If the input `o` is an instance of `ParameterSpec`, the function returns the
     * name of the parameter.
     * 	- If the input `o` is an instance of `FieldSpec`, the function returns the name
     * of the field.
     * 	- If the input `o` is an instance of `MethodSpec`, the function returns the name
     * of the method.
     * 	- If the input `o` is an instance of `TypeSpec`, the function returns the name
     * of the type.
     * 	- If any other type of object is passed as input, the function throws an `IllegalArgumentException`.
     */
    private String argToName(Object o) {
      if (o instanceof CharSequence) return o.toString();
      if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
      if (o instanceof FieldSpec) return ((FieldSpec) o).name;
      if (o instanceof MethodSpec) return ((MethodSpec) o).name;
      if (o instanceof TypeSpec) return ((TypeSpec) o).name;
      throw new IllegalArgumentException("expected name but was " + o);
    }

    /**
     * converts an object argument to its literal value.
     * 
     * @param o argument to be converted into a literal value, which is then returned by
     * the function.
     * 
     * 	- `Object o`: The function takes an arbitrary object as input, which may have any
     * combination of properties and attributes.
     * 
     * @returns the input object itself, unmodified.
     * 
     * The `argToLiteral` function returns an object of type `Object`. This means that
     * the function can potentially return any type of object, including primitive types,
     * classes, or even a reference to another object.
     * 
     * The function does not perform any specific operation on the input parameter `o`,
     * other than returning it unmodified. Therefore, the output of the function is simply
     * a copy of the original input.
     * 
     * The returned object is an instance of class `Object`, which means that it has
     * certain properties and methods that are common to all objects in Java. These
     * properties and methods include things like `equals()`, `hashCode()`, and `toString()`.
     * However, the specific implementation of these properties and methods may vary
     * depending on the type of object being returned.
     */
    private Object argToLiteral(Object o) {
      return o;
    }

    /**
     * converts an object to a string, returning the original value if not null, or null
     * otherwise.
     * 
     * @param o Object value that is being converted to a string.
     * 
     * 	- `o` is an object, which could be any type of Java object.
     * 	- If `o` is not null, its value is converted to a string using `String.valueOf(o)`.
     * 	- If `o` is null, the function returns `null`.
     * 
     * @returns a string representation of the input object, or `null` if the input is `null`.
     */
    private String argToString(Object o) {
      return o != null ? String.valueOf(o) : null;
    }

    /**
     * takes an Object parameter and converts it to a TypeName object, handling various
     * types of inputs including direct instances, mirrors, elements, and raw type references.
     * 
     * @param o Object being passed to the `argToType()` method, which then determines
     * the corresponding `TypeName`.
     * 
     * 	- If `o` is an instance of `TypeName`, it is returned unmodified.
     * 	- If `o` is an instance of `TypeMirror`, it is converted to a `TypeName` object
     * using the `TypeName.get()` method.
     * 	- If `o` is an instance of `Element`, it is converted to a `TypeName` object using
     * the `(Element) o`.asType()` method.
     * 	- If `o` is an instance of `Type`, it is converted to a `TypeName` object using
     * the `(Type) o`.getTypeName() method.
     * 	- If any other type of input is provided, an `IllegalArgumentException` is thrown
     * with the message "expected type but was ...".
     * 
     * @returns a `TypeName` object representing the type of the provided `Object`.
     * 
     * 1/ If `o` is an instance of `TypeName`, the function returns the same object directly.
     * 2/ If `o` is an instance of `TypeMirror`, the function converts it to a `TypeName`
     * object and returns it.
     * 3/ If `o` is an instance of `Element`, the function converts it to a `Type` object
     * using the `asType()` method, and then returns the converted `TypeName`.
     * 4/ If `o` is an instance of `Type`, the function converts it to a `TypeName` object
     * directly and returns it.
     * 5/ If any other type of object is passed as `o`, the function throws an `IllegalArgumentException`.
     * 
     * In summary, the function takes any object as input and returns a `TypeName` object
     * if it is a valid type, or throws an exception otherwise.
     */
    private TypeName argToType(Object o) {
      if (o instanceof TypeName) return (TypeName) o;
      if (o instanceof TypeMirror) return TypeName.get((TypeMirror) o);
      if (o instanceof Element) return TypeName.get(((Element) o).asType());
      if (o instanceof Type) return TypeName.get((Type) o);
      throw new IllegalArgumentException("expected type but was " + o);
    }

    /**
     * allows the creation of control flow statements using strings. It appends the
     * specified string to the builder and indents the next line before returning the
     * builder for further modifications.
     * 
     * @param controlFlow control flow statement to be added to the builder, which can
     * be one of several options such as "if", "else", "while", or "break".
     * 
     * @returns a string representing the beginning of a control flow statement, including
     * the control flow keyword and any arguments provided.
     * 
     * The first argument passed to the function is `controlFlow`, which is a string
     * indicating the type of control flow to be executed next.
     * The second argument `Object... args` represents an arbitrary number of objects
     * that can be used as arguments for the control flow.
     * The function returns a `Builder` object, which allows additional statements to be
     * added to the control flow sequence.
     */
    public Builder beginControlFlow(String controlFlow, Object... args) {
      add(controlFlow + " {\n", args);
      indent();
      return this;
    }

    /**
     * adds a new statement to the current flow of execution, using the specified control
     * flow and arguments.
     * 
     * @param controlFlow sequence of statements to be executed after the current
     * block of code has been indented.
     * 
     * @returns a Java statement adding the specified `controlFlow` and arguments to the
     * current builder instance.
     * 
     * 	- The output is a `Builder` object that represents an unindented block of code.
     * 	- The output has a single method call `add` that adds an unformatted string to
     * the current control flow.
     * 	- The `add` method takes two arguments: the first is the string to be added, and
     * the second is an array of objects representing the optional arguments for the
     * method call.
     * 	- The `nextControlFlow` function returns a reference to the modified `Builder`
     * object, allowing it to be used as a building block for further method calls.
     */
    public Builder nextControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} " + controlFlow + " {\n", args);
      indent();
      return this;
    }

    /**
     * indents and adds a closing brace `}`. It then returns a modified builder instance.
     * 
     * @returns a JavaBuilder object containing the updated code with the control flow
     * statement at the end.
     * 
     * 	- `unindent()` is called to remove any indentation added by the previous `add()`
     * method call.
     * 	- The `}\n` string is added to the builder's internal buffer, indicating the end
     * of a control flow statement.
     */
    public Builder endControlFlow() {
      unindent();
      add("}\n");
      return this;
    }

    /**
     * adds a statement at the end of a block of code, with the specified control flow
     * and arguments.
     * 
     * @param controlFlow additional code to be executed after the method's execution,
     * which is added to the current method implementation.
     * 
     * @returns a Java statement adding the specified control flow statement to the current
     * builder object.
     * 
     * The `String` parameter `controlFlow` represents the control flow statement to be
     * added to the code, such as `if`, `else`, `while`, or `break`.
     * 
     * The `Object... args` parameter is a variable-length argument list that contains
     * the arguments for the control flow statement.
     * 
     * The function adds the specified control flow statement followed by a semicolon and
     * the provided arguments to the code being built.
     * 
     * The returned output is a modified instance of the `Builder` class, which allows
     * for further modification and manipulation of the code being built.
     */
    public Builder endControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} " + controlFlow + ";\n", args);
      return this;
    }

    /**
     * adds a statement to the builder's statement list. The statement is created by
     * concatenating three strings: `$[`, the format string, and `]$`.
     * 
     * @param format format string for the addition of the `args` parameters to the builder
     * object.
     * 
     * @returns a Java statement consisting of a format string and arguments.
     */
    public Builder addStatement(String format, Object... args) {
      add("$[");
      add(format, args);
      add(";\n$]");
      return this;
    }

    /**
     * adds a `CodeBlock` to the builder object, returning the modified builder instance.
     * 
     * @param codeBlock code to be added to the builder object, which can then be used
     * to create a new instance of the `CodeBlock` class.
     * 
     * 	- `$L`: The input code block is a string representing Java code.
     * 	- `codeBlock`: A deserialized representation of the input code, which may contain
     * various attributes or properties depending on its type and structure.
     * 
     * @returns a builder instance with the given code block added to it.
     * 
     * 	- `$L`: This represents the line number where the code block will be inserted in
     * the builder's AST.
     * 	- `codeBlock`: This is the code block that will be added to the builder's AST at
     * the specified line number.
     */
    public Builder addStatement(CodeBlock codeBlock) {
      return addStatement("$L", codeBlock);
    }

    /**
     * adds the formatting parts and arguments of a given `CodeBlock` to those of the
     * current `Builder`.
     * 
     * @param codeBlock code block that contains formatting instructions and argument
     * values to be added to the builder's parts and arguments arrays, respectively.
     * 
     * 	- `formatParts`: This is an array of format parts, which contains additional
     * information about how to render the code block. It may contain elements such as
     * line numbers, syntax highlighting, or other formatting instructions.
     * 	- `args`: This is an array of arguments that can be used to customize the rendering
     * of the code block. Each element in the array represents a single argument, and can
     * be a string, integer, or other type of data depending on the specific implementation.
     * 
     * @returns a modified instance of the `Builder` class, with additional format parts
     * and arguments added from the provided code block.
     * 
     * 	- `formatParts`: A Collection of CodeBlock's `formatParts` attribute, which
     * contains all the formatting parts defined in the function.
     * 	- `args`: A Collection of CodeBlock's `args` attribute, which contains all the
     * arguments passed to the function.
     */
    public Builder add(CodeBlock codeBlock) {
      formatParts.addAll(codeBlock.formatParts);
      args.addAll(codeBlock.args);
      return this;
    }

    /**
     * adds a new format part to the builder's parts list, specifically "$>".
     * 
     * @returns a new builder instance with an added format part of `$>`.
     * 
     * The `Builder` object `this` is updated with a new `formatParts` list containing
     * the added value `$>`.
     */
    public Builder indent() {
      this.formatParts.add("$>");
      return this;
    }

    /**
     * adds a special character `$<` to the list of format parts, indicating that the
     * next part should be indented.
     * 
     * @returns a new `Builder` object with an additional " "$<" format part added to the
     * list of format parts.
     * 
     * 	- The `this` keyword in the return statement indicates that the current object
     * is being returned.
     * 	- The `formatParts` field is added to the list of strings by using the `$<`
     * operator. This adds a new string to the list of format parts.
     * 	- The `unindent` function returns an instance of the `Builder` class, which
     * represents the current state of the builder.
     */
    public Builder unindent() {
      this.formatParts.add("$<");
      return this;
    }

    /**
     * in the `Builder` class clears the contents of two collections, `formatParts` and
     * `args`, and returns the `Builder` object itself.
     * 
     * @returns a reference to the current builder instance with its parts and arguments
     * cleared.
     * 
     * 	- `formatParts.clear()`: Removes all elements from the `formatParts` list.
     * 	- `args.clear()`: Removes all elements from the `args` list.
     * 	- `return this`: Returns a reference to the same instance of the `Builder` class,
     * allowing for chaining of methods.
     */
    public Builder clear() {
      formatParts.clear();
      args.clear();
      return this;
    }

    /**
     * creates a new instance of `CodeBlock` and returns it.
     * 
     * @returns a new instance of the `CodeBlock` class.
     * 
     * A CodeBlock instance is generated using the `new` operator.
     * The CodeBlock instance is a composite object that represents the code block created
     * by the builder method.
     */
    public CodeBlock build() {
      return new CodeBlock(this);
    }
  }

  /**
   * is an inner class that facilitates the process of combining multiple code blocks
   * into a single code block. It provides a way to add, merge, and join code blocks
   * using a delimiter string. The class has a builder instance variable, which is used
   * to build the final code block.
   */
  private static final class CodeBlockJoiner {
    private final String delimiter;
    private final Builder builder;
    private boolean first = true;

    CodeBlockJoiner(String delimiter, Builder builder) {
      this.delimiter = delimiter;
      this.builder = builder;
    }

    /**
     * adds a code block to an internal builder, handling the case where multiple blocks
     * are added together by adding a delimiter between them.
     * 
     * @param codeBlock code block that is to be added to the current sequence of code
     * blocks being built by the `CodeBlockJoiner`.
     * 
     * 	- `first`: A boolean variable indicating whether this is the first `CodeBlock`
     * added to the builder.
     * 	- `builder`: A reference to the current state of the `CodeBlockJoiner`, which is
     * used to accumulate the `CodeBlocks`.
     * 
     * @returns a new `CodeBlockJoiner` instance with the additional code block appended
     * to it.
     * 
     * The `add` function adds a new `CodeBlock` to the builder object. The `builder`
     * parameter is an instance of `CodeBlockJoiner`, which means it holds a sequence of
     * `CodeBlock`s that will be joined together when the `add` function is called.
     * 
     * The first condition in the function, `if (!first)`, checks if the current call to
     * `add` is the first one in the sequence. If it is not the first call, then the
     * previous delimiter (`delimiter`) is added to the builder before adding the new
     * `CodeBlock`. This ensures that each `CodeBlock` is separated by a delimiter.
     * 
     * The second condition, `first = false`, sets the value of the `first` variable to
     * `false` after the `if` statement, indicating that the `add` function has been
     * called at least once in the sequence.
     * 
     * Finally, the new `CodeBlock` is added to the builder using the `builder.add()` method.
     * 
     * In summary, the `add` function adds a new `CodeBlock` to a sequence of blocks being
     * built by a `CodeBlockJoiner`, separating each block with a delimiter when appropriate.
     */
    CodeBlockJoiner add(CodeBlock codeBlock) {
      if (!first) {
        builder.add(delimiter);
      }
      first = false;

      builder.add(codeBlock);
      return this;
    }

    /**
     * combines an instance of `CodeBlockJoiner` with another instance of `CodeBlockJoiner`,
     * appending any non-empty blocks to the current one.
     * 
     * @param other 2nd code block that gets merged with the current block.
     * 
     * 	- `CodeBlock otherBlock = other.builder.build();`: This line of code retrieves
     * the built CodeBlock from the `other` object's builder and assigns it to a variable
     * named `otherBlock`.
     * 	- `if (!otherBlock.isEmpty()) {`: This line checks if the `otherBlock` is not
     * empty. If it is, then...
     * 	- `add(otherBlock);`: This line adds the `otherBlock` to this object's CodeBlock
     * list.
     * 
     * @returns a modified instance of the original `CodeBlockJoiner` object, with any
     * additional code blocks added from the provided input.
     * 
     * 	- The `CodeBlockJoiner` object is modified by adding any non-empty `CodeBlock`
     * objects provided in the parameter `other`.
     * 	- The `otherBlock` variable refers to the `CodeBlock` object that was built using
     * the builder method of the `other` parameter.
     * 	- The `isEmpty()` method is used to check if the `otherBlock` is empty before
     * adding it to the current `CodeBlockJoiner`. If it is empty, nothing is added.
     */
    CodeBlockJoiner merge(CodeBlockJoiner other) {
      CodeBlock otherBlock = other.builder.build();
      if (!otherBlock.isEmpty()) {
        add(otherBlock);
      }
      return this;
    }

    /**
     * builds a `Builder` object into a complete `Java` object, returning the resulting
     * object.
     * 
     * @returns a built object.
     * 
     * The `builder` field is used to construct an immutable `java.lang.Object`. The
     * build() method returns the constructed object as the output of the function.
     */
    CodeBlock join() {
      return builder.build();
    }
  }
}
