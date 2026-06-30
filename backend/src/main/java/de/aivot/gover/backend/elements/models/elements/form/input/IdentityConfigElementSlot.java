package de.aivot.gover.backend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class IdentityConfigElementSlot implements Serializable {
    @Nullable
    private String id;

    @Nullable
    private String title;

    @Nullable
    private String description;

    @Nullable
    private Boolean allowsMail;

    @Nullable
    private Boolean isOptional;

    @Nullable
    private List<IdentityConfigElementOption> options;

    // region Constructors

    // Empty constructor
    public IdentityConfigElementSlot() {

    }

    // Full constructor

    public IdentityConfigElementSlot(@Nullable String id,
                                     @Nullable String title,
                                     @Nullable String description,
                                     @Nullable Boolean allowsMail,
                                     @Nullable Boolean isOptional,
                                     @Nullable List<IdentityConfigElementOption> options) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.allowsMail = allowsMail;
        this.isOptional = isOptional;
        this.options = options;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityConfigElementSlot that = (IdentityConfigElementSlot) o;
        return Objects.equals(id, that.id) && Objects.equals(title, that.title) && Objects.equals(description, that.description) &&
                Objects.equals(allowsMail, that.allowsMail) && Objects.equals(isOptional, that.isOptional) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, allowsMail, isOptional, options);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getId() {
        return id;
    }

    public IdentityConfigElementSlot setId(@Nullable String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public IdentityConfigElementSlot setTitle(@Nullable String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public IdentityConfigElementSlot setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nullable
    public Boolean getAllowsMail() {
        return allowsMail;
    }

    public IdentityConfigElementSlot setAllowsMail(@Nullable Boolean allowsMail) {
        this.allowsMail = allowsMail;
        return this;
    }

    @Nullable
    public Boolean getIsOptional() {
        return isOptional;
    }

    public IdentityConfigElementSlot setIsOptional(@Nullable Boolean optional) {
        isOptional = optional;
        return this;
    }

    @Nullable
    public List<IdentityConfigElementOption> getOptions() {
        return options;
    }

    public IdentityConfigElementSlot setOptions(@Nullable List<IdentityConfigElementOption> options) {
        this.options = options;
        return this;
    }

    // endregion
}
