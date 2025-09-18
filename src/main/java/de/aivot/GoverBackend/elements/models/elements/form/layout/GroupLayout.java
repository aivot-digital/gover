package de.aivot.GoverBackend.elements.models.elements.form.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class GroupLayout extends BaseFormElement implements ElementWithChildren<BaseFormElement> {
    @Nonnull
    private List<BaseFormElement> children = new LinkedList<>();
    @Nullable
    private GroupLayoutStoreLink storeLink;

    public GroupLayout() {
        super(ElementType.Group);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupLayout that = (GroupLayout) o;
        return Objects.equals(children, that.children) && Objects.equals(storeLink, that.storeLink);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(children);
        result = 31 * result + Objects.hashCode(storeLink);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    @Override
    public List<BaseFormElement> getChildren() {
        if (children == null) {
            children = new LinkedList<>();
        }
        return children;
    }

    @Nonnull
    @Override
    public GroupLayout setChildren(@Nullable List<BaseFormElement> children) {
        if (children == null) {
            children = new LinkedList<>();
        }
        this.children = children;
        return this;
    }

    @Nullable
    public GroupLayoutStoreLink getStoreLink() {
        return storeLink;
    }

    public GroupLayout setStoreLink(@Nullable GroupLayoutStoreLink storeLink) {
        this.storeLink = storeLink;
        return this;
    }

    // endregion

    // region Subclasses

    public static class GroupLayoutStoreLink implements Serializable {
        @Nullable
        private String storeId;
        @Nullable
        private String storeVersion;

        // region Hash & Equals

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            GroupLayoutStoreLink that = (GroupLayoutStoreLink) o;
            return Objects.equals(storeId, that.storeId) && Objects.equals(storeVersion, that.storeVersion);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(storeId);
            result = 31 * result + Objects.hashCode(storeVersion);
            return result;
        }

        // endregion

        // region Getters & Setters

        @Nullable
        public String getStoreId() {
            return storeId;
        }

        public GroupLayoutStoreLink setStoreId(@Nullable String storeId) {
            this.storeId = storeId;
            return this;
        }

        @Nullable
        public String getStoreVersion() {
            return storeVersion;
        }

        public GroupLayoutStoreLink setStoreVersion(@Nullable String storeVersion) {
            this.storeVersion = storeVersion;
            return this;
        }

        // endregion
    }

    // endregion
}
