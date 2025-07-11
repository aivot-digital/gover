package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.enums.ElementApprovalStatus;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.elements.utils.ElementFlattenUtils;

import java.util.HashMap;
import java.util.Map;

public class ElementApprovalService {
    public static Map<String, ElementApprovalStatus> determineApprovals(RootElement rootElement) {
        var allElements = ElementFlattenUtils
                .flattenElements(rootElement);

        var status = new HashMap<String, ElementApprovalStatus>();
        for (var element : allElements) {
            status.put(element.getId(), element.getApproval());
        }

        return status;
    }

    public static boolean isApproved(RootElement rootElement) {
        var allElements = ElementFlattenUtils
                .flattenElements(rootElement);

        for (var element : allElements) {
            if (element.getApproval() != ElementApprovalStatus.Approved) {
                return false;
            }
        }

        return true;
    }
}
