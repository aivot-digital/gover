package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;
import java.util.HashMap;

public class ElementData extends HashMap<String, ElementDataObject> implements Serializable {
    public ElementData() {
        super();
    }

    public ElementData(ElementData elementData) {
        super(elementData);
    }

    public ElementDataObject put(String key, ElementDataObject dataObject) {
        super.put(key, dataObject);
        return dataObject;
    }

    public ElementDataObject put(BaseElement element, ElementDataObject dataObject) {
        put(element.getId(), dataObject);
        return dataObject;
    }

    public ElementDataObject putInputValue(String key, Object value) {
        ElementDataObject elementDataObject = new ElementDataObject();
        elementDataObject.setInputValue(value);
        return put(key, elementDataObject);
    }

    public static ElementData of(Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be in pairs of key and value.");
        }
        ElementData elementData = new ElementData();
        for (int i = 0; i < args.length; i += 2) {
            String key = (String) args[i];
            Object value = args[i + 1];
            ElementDataObject elementDataObject = new ElementDataObject();
            elementDataObject.setInputValue(value);
            elementData.put(key, elementDataObject);
        }
        return elementData;
    }

    @JsonIgnore
    public boolean hasAnyError() {
        return values()
                .stream()
                .anyMatch(d -> (
                                       d.getComputedErrors() != null &&
                                       !d.getComputedErrors().isEmpty()
                               ) || (
                                       d.getValue() instanceof ElementData ed &&
                                       ed.hasAnyError()
                               ));
    }
}
