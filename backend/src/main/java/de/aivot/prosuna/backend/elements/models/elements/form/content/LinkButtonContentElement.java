package de.aivot.prosuna.backend.elements.models.elements.form.content;

import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class LinkButtonContentElement extends BaseFormElement {
    public static final String DEFAULT_LABEL = "Link öffnen";
    public static final String DEFAULT_VARIANT = "contained";
    public static final String DEFAULT_COLOR = "primary";

    @Nullable
    private String label;
    @Nullable
    private String href;
    @Nullable
    private Boolean openInNewTab;
    @Nullable
    private String staffTaskEvent;
    @Nullable
    private String customerTaskEvent;
    @Nullable
    private String variant;
    @Nullable
    private String color;

    public LinkButtonContentElement() {
        super(ElementType.LinkButton);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LinkButtonContentElement that = (LinkButtonContentElement) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(href, that.href) &&
                Objects.equals(openInNewTab, that.openInNewTab) &&
                Objects.equals(staffTaskEvent, that.staffTaskEvent) &&
                Objects.equals(customerTaskEvent, that.customerTaskEvent) &&
                Objects.equals(variant, that.variant) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, href, openInNewTab, staffTaskEvent, customerTaskEvent, variant, color);
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nonnull
    public String getResolvedLabel() {
        if (label != null && !label.isBlank()) {
            return label;
        }
        return DEFAULT_LABEL;
    }

    public LinkButtonContentElement setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    @Nullable
    public String getHref() {
        return href;
    }

    public LinkButtonContentElement setHref(@Nullable String href) {
        this.href = href;
        return this;
    }

    @Nullable
    public Boolean getOpenInNewTab() {
        return openInNewTab;
    }

    @Nonnull
    public Boolean getResolvedOpenInNewTab() {
        return !Boolean.FALSE.equals(openInNewTab);
    }

    public LinkButtonContentElement setOpenInNewTab(@Nullable Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
        return this;
    }

    @Nullable
    public String getStaffTaskEvent() {
        return staffTaskEvent;
    }

    public LinkButtonContentElement setStaffTaskEvent(@Nullable String staffTaskEvent) {
        this.staffTaskEvent = staffTaskEvent;
        return this;
    }

    @Nullable
    public String getCustomerTaskEvent() {
        return customerTaskEvent;
    }

    public LinkButtonContentElement setCustomerTaskEvent(@Nullable String customerTaskEvent) {
        this.customerTaskEvent = customerTaskEvent;
        return this;
    }

    @Nullable
    public String getVariant() {
        return variant;
    }

    @Nonnull
    public String getResolvedVariant() {
        if (Objects.equals(variant, "text") || Objects.equals(variant, "outlined")) {
            return variant;
        }
        return DEFAULT_VARIANT;
    }

    public LinkButtonContentElement setVariant(@Nullable String variant) {
        this.variant = variant;
        return this;
    }

    @Nullable
    public String getColor() {
        return color;
    }

    @Nonnull
    public String getResolvedColor() {
        if (Objects.equals(color, "secondary")) {
            return color;
        }
        return DEFAULT_COLOR;
    }

    public LinkButtonContentElement setColor(@Nullable String color) {
        this.color = color;
        return this;
    }
}
