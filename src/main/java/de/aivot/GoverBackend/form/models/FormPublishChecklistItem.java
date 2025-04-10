package de.aivot.GoverBackend.form.models;

public class FormPublishChecklistItem {
    private String label;
    private Boolean done;

    public static FormPublishChecklistItem create() {
        return new FormPublishChecklistItem();
    }

    public String getLabel() {
        return label;
    }

    public FormPublishChecklistItem setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean getDone() {
        return done;
    }

    public FormPublishChecklistItem setDone(Boolean done) {
        this.done = done;
        return this;
    }
}
