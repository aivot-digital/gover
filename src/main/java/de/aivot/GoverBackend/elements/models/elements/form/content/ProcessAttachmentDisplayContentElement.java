package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class ProcessAttachmentDisplayContentElement extends BaseFormElement {
    @Nullable
    private String fileName;
    @Nullable
    private String hint;

    public ProcessAttachmentDisplayContentElement() {
        super(ElementType.ProcessAttachmentDisplay);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProcessAttachmentDisplayContentElement that = (ProcessAttachmentDisplayContentElement) o;
        return Objects.equals(fileName, that.fileName) && Objects.equals(hint, that.hint);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(fileName);
        result = 31 * result + Objects.hashCode(hint);
        return result;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public ProcessAttachmentDisplayContentElement setFileName(@Nullable String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public ProcessAttachmentDisplayContentElement setHint(@Nullable String hint) {
        this.hint = hint;
        return this;
    }
}
