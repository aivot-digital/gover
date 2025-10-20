package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;

public class ElementDerivationRequest {
    private BaseElement element;
    private ElementData elementData;
    private ElementDerivationOptions options;

    public BaseElement getElement() {
        return element;
    }

    public ElementDerivationRequest setElement(BaseElement element) {
        this.element = element;
        return this;
    }

    public ElementData getElementData() {
        return elementData;
    }

    public ElementDerivationRequest setElementData(ElementData elementData) {
        this.elementData = elementData;
        return this;
    }

    public ElementDerivationOptions getOptions() {
        return options;
    }

    public ElementDerivationRequest setOptions(ElementDerivationOptions options) {
        this.options = options;
        return this;
    }
}
