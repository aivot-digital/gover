package de.aivot.GoverBackend.elements.models.steps;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class SubmitStepElement extends BaseElement {
    public static final String CAPTCHA_FILED_ID = "__human__";

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
        documentsToReceive = MapUtils.getStringCollection(values, "documentsToReceive");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SubmitStepElement that = (SubmitStepElement) o;

        if (!Objects.equals(textPreSubmit, that.textPreSubmit))
            return false;
        if (!Objects.equals(textPostSubmit, that.textPostSubmit))
            return false;
        if (!Objects.equals(textProcessingTime, that.textProcessingTime))
            return false;
        return Objects.equals(documentsToReceive, that.documentsToReceive);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (textPreSubmit != null ? textPreSubmit.hashCode() : 0);
        result = 31 * result + (textPostSubmit != null ? textPostSubmit.hashCode() : 0);
        result = 31 * result + (textProcessingTime != null ? textProcessingTime.hashCode() : 0);
        result = 31 * result + (documentsToReceive != null ? documentsToReceive.hashCode() : 0);
        return result;
    }

    // region Getters & Setters

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

    // endregion
}
