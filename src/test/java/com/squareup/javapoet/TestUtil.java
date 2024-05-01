package com.squareup.javapoet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Collection;

/**
 * is a static utility class that provides a method for finding an element in a
 * collection based on its name. The method takes a collection of elements and a
 * specific name as input, and it iterates over the elements in the collection to
 * find the matching element. If the element is found, it is returned, otherwise, an
 * IllegalArgumentException is thrown.
 */
final class TestUtil {
  /**
   * searches a collection of elements for the first one with a matching simple name,
   * and returns it if found. If not found, an `IllegalArgumentException` is thrown
   * with the name and collection.
   * 
   * @param elements collection of elements from which the first element with the
   * matching name should be found.
   * 
   * 	- Type: The type of elements in the collection is specified by the generic parameter
   * `E`, which can be any subclass of the `Element` class.
   * 	- Elements: The collection `elements` contains a list of elements that are instances
   * of the `Element` class, possibly with different subclasses.
   * 	- Name: The name attribute of each element in the collection is used to identify
   * it in the search operation.
   * 
   * @param name identifier of the element to be found in the collection of elements
   * passed as argument to the function.
   * 
   * @returns the first element in the collection that matches the given name, or an
   * `IllegalArgumentException` if none is found.
   * 
   * 	- The output is an element of type `E`, which is the input parameter of the function.
   * 	- The output is retrieved from the input collection `elements`.
   * 	- The output has a `SimpleName` attribute, which is used for comparison with the
   * input name in the `if` statement.
   * 	- If the input name is not found in the input collection, an `IllegalArgumentException`
   * is thrown.
   */
  static <E extends Element> E findFirst(Collection<E> elements, String name) {
    for (E element : elements) {
      if (element.getSimpleName().toString().equals(name)) {
        return element;
      }
    }
    throw new IllegalArgumentException(name + " not found in " + elements);
  }
}
