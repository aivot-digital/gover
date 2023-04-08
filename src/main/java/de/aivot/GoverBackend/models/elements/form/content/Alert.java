package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class Alert extends FormElement {
    private String title;
    private String text;
    private AlertType alertType;

    public Alert(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        title = MapUtils.getString(values, "title", "");
        text = MapUtils.getString(values, "text", "");
        alertType = MapUtils.getEnum(values, "alertType", String.class, AlertType.values(), AlertType.Success);
    }

    //region Getters & Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    //endregion
}
