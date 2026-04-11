package de.aivot.GoverBackend.elements.models;

import java.util.HashMap;

/**
 * The inputs for an element structure. The keys are the ids of the elements. The values are the values of the elements. The values can be of any type, depending on the element
 * type. For example, a text element can have a string value, while a number element can have a double value. The values can also be null if the element has no value. The values
 * can also be another AuthoredValues object if the element is a container or an array of AuthoredValues if it's a replicating container.
 */
public class AuthoredElementValues extends HashMap<String, Object> {
}
