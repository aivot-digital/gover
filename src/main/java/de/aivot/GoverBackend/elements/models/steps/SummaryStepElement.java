package de.aivot.GoverBackend.elements.models.steps;

import de.aivot.GoverBackend.elements.models.BaseElement;

import java.util.Map;

public class SummaryStepElement extends BaseElement {
    public static final String SUMMARY_CONFIRMATION = "__summary__";

    public SummaryStepElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {

    }
}
