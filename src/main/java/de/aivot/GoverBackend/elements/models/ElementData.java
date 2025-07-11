package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class ElementData extends HashMap<String, ElementDataObject> {
    public ElementData() {
        super();
    }

    public ElementData(ElementData elementData) {
        super(elementData);
    }

    public static ElementData copyOf(ElementData elementData) {
        return new ElementData(elementData);
    }

    public void put(BaseElement element, ElementDataObject dataObject) {
        put(element.getId(), dataObject);
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
