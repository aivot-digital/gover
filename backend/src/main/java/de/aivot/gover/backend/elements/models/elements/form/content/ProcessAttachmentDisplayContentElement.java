package de.aivot.gover.backend.elements.models.elements.form.content;

import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class ProcessAttachmentDisplayContentElement extends BaseFormElement {
    @Nullable
    private String attachmentSetKey;
    @Nullable
    private String label;
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
        return Objects.equals(attachmentSetKey, that.attachmentSetKey) &&
               Objects.equals(label, that.label) &&
               Objects.equals(hint, that.hint);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(attachmentSetKey);
        result = 31 * result + Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(hint);
        return result;
    }

    /**
     * The display is tied to an attachment set key instead of a filename because generated and uploaded
     * files can be renamed, duplicated, or replaced while the set key remains the stable process contract.
     */
    @Nullable
    public String getAttachmentSetKey() {
        return attachmentSetKey;
    }

    public ProcessAttachmentDisplayContentElement setAttachmentSetKey(@Nullable String attachmentSetKey) {
        this.attachmentSetKey = attachmentSetKey;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public ProcessAttachmentDisplayContentElement setLabel(@Nullable String label) {
        this.label = label;
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
