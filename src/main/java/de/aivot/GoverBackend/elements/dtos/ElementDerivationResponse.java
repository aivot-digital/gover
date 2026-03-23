package de.aivot.GoverBackend.elements.dtos;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationLogItem;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;

import java.util.List;

public class ElementDerivationResponse {
    private ElementData elementData;
    private List<ElementDerivationLogItem> logItems;

    public static ElementDerivationResponse from(ElementData elementData, ElementDerivationLogger logger, boolean isAuthenticated) {
        if (!isAuthenticated) {
            return new ElementDerivationResponse()
                    .setElementData(elementData)
                    .setLogItems(List.of());
        }

        return new ElementDerivationResponse()
                .setElementData(elementData)
                .setLogItems(logger.getLogItems());
    }

    public ElementData getElementData() {
        return elementData;
    }

    public ElementDerivationResponse setElementData(ElementData elementData) {
        this.elementData = elementData;
        return this;
    }

    public List<ElementDerivationLogItem> getLogItems() {
        return logItems;
    }

    public ElementDerivationResponse setLogItems(List<ElementDerivationLogItem> logItems) {
        this.logItems = logItems;
        return this;
    }
}
