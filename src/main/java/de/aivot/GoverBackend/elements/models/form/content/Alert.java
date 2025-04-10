package de.aivot.GoverBackend.elements.models.form.content;

import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class Alert extends BaseFormElement {
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
        alertType = MapUtils.getEnum(values, "alertType", String.class, AlertType.class, AlertType.values(), AlertType.Info);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Alert alert = (Alert) o;

        if (!Objects.equals(title, alert.title)) return false;
        if (!Objects.equals(text, alert.text)) return false;
        return alertType == alert.alertType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (alertType != null ? alertType.hashCode() : 0);
        return result;
    }

    // region Getters & Setters

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

    // endregion
}
