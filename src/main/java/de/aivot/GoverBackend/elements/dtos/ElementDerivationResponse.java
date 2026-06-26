package de.aivot.GoverBackend.elements.dtos;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationLogItem;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;

import java.util.List;

public class ElementDerivationResponse {
    private DerivedRuntimeElementData elementData;
    private List<ElementDerivationLogItem> logItems;

    public static ElementDerivationResponse from(DerivedRuntimeElementData elementData, ElementDerivationLogger logger, boolean isAuthenticated) {
        if (!isAuthenticated) {
            return new ElementDerivationResponse()
                    .setElementData(elementData)
                    .setLogItems(List.of());
        }

        return new ElementDerivationResponse()
                .setElementData(elementData)
                .setLogItems(logger.getLogItems());
    }

    public DerivedRuntimeElementData getElementData() {
        return elementData;
    }

    public ElementDerivationResponse setElementData(DerivedRuntimeElementData elementData) {
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
