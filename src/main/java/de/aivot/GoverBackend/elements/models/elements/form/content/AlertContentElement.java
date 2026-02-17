package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class AlertContentElement extends BaseFormElement {
    @Nullable
    private String title;

    @Nullable
    private String text;

    @Nullable
    private AlertType alertType;

    public AlertContentElement() {
        super(ElementType.Alert);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AlertContentElement alert = (AlertContentElement) o;

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

    // endregion

    // region Getters & Setters

    @Nullable
    public String getTitle() {
        return title;
    }

    public AlertContentElement setTitle(@Nullable String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public AlertContentElement setText(@Nullable String text) {
        this.text = text;
        return this;
    }

    @Nullable
    public AlertType getAlertType() {
        return alertType;
    }

    public AlertContentElement setAlertType(@Nullable AlertType alertType) {
        this.alertType = alertType;
        return this;
    }

    // endregion
}
