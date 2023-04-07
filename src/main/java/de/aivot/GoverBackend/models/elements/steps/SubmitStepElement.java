package de.aivot.GoverBackend.models.elements.steps;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;

import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Map;

public class SubmitStepElement extends BaseElement {
    private String textPreSubmit;
    private String textPostSubmit;
    private String textProcessingTime;
    private Collection<String> documentsToReceive;

    public SubmitStepElement(Map<String, Object> data) {
        super(data);

        textPreSubmit = (String) data.get("textPreSubmit");
        textPostSubmit = (String) data.get("textPostSubmit");
        textProcessingTime = (String) data.get("textProcessingTime");
        documentsToReceive = (Collection<String>) data.get("documentsToReceive");
    }

    @Nullable
    public String getTextPreSubmit() {
        return textPreSubmit;
    }

    public void setTextPreSubmit(String textPreSubmit) {
        this.textPreSubmit = textPreSubmit;
    }

    @Nullable
    public String getTextPostSubmit() {
        return textPostSubmit;
    }

    public void setTextPostSubmit(String textPostSubmit) {
        this.textPostSubmit = textPostSubmit;
    }

    @Nullable
    public String getTextProcessingTime() {
        return textProcessingTime;
    }

    public void setTextProcessingTime(String textProcessingTime) {
        this.textProcessingTime = textProcessingTime;
    }

    @Nullable
    public Collection<String> getDocumentsToReceive() {
        return documentsToReceive;
    }

    public void setDocumentsToReceive(Collection<String> documentsToReceive) {
        this.documentsToReceive = documentsToReceive;
    }
}
