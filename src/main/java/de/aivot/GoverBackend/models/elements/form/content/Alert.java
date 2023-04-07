package de.aivot.GoverBackend.models.elements.form.content;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;

import java.util.Map;

public class Alert extends FormElement {
    private String title;
    private String text;
    private AlertType alertType;

    public Alert(BaseElement parent, Map<String, Object> data) {
        super(data);
        title = (String) data.get("title");
        text = (String) data.get("text");
        alertType = AlertType.findElement(data.get("alertType")).orElse(null);
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Nullable
    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }
}
