package de.aivot.GoverBackend.models.elements.steps;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;

public class SubmitStepElement extends BaseElement {
    private String textPreSubmit;
    private String textPostSubmit;
    private String textProcessingTime;
    private Collection<String> documentsToReceive;

    public SubmitStepElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        textPreSubmit = MapUtils.getString(values, "textPreSubmit");
        textPostSubmit = MapUtils.getString(values, "textPostSubmit");
        textProcessingTime = MapUtils.getString(values, "textProcessingTime");
        documentsToReceive = MapUtils.get(values, "documentsToReceive", Collection.class);
    }

    //region Getters & Setters

    public String getTextPreSubmit() {
        return textPreSubmit;
    }

    public void setTextPreSubmit(String textPreSubmit) {
        this.textPreSubmit = textPreSubmit;
    }

    public String getTextPostSubmit() {
        return textPostSubmit;
    }

    public void setTextPostSubmit(String textPostSubmit) {
        this.textPostSubmit = textPostSubmit;
    }

    public String getTextProcessingTime() {
        return textProcessingTime;
    }

    public void setTextProcessingTime(String textProcessingTime) {
        this.textProcessingTime = textProcessingTime;
    }

    public Collection<String> getDocumentsToReceive() {
        return documentsToReceive;
    }

    public void setDocumentsToReceive(Collection<String> documentsToReceive) {
        this.documentsToReceive = documentsToReceive;
    }

    //endregion
}
