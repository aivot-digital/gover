package de.aivot.prosuna.backend.elements.models.elements.layout;

import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class GroupLayoutElement extends BaseFormElement implements LayoutElement<BaseFormElement> {
    @Nonnull
    private List<BaseFormElement> children = new LinkedList<>();

    @Nullable
    private GroupLayoutMarketplaceLink marketplaceLink;

    public GroupLayoutElement() {
        super(ElementType.GroupLayout);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GroupLayoutElement that = (GroupLayoutElement) o;
        return Objects.equals(children, that.children) && Objects.equals(marketplaceLink, that.marketplaceLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children, marketplaceLink);
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
    public GroupLayoutElement setChildren(@Nullable List<BaseFormElement> children) {
        if (children == null) {
            children = new LinkedList<>();
        }
        this.children = children;
        return this;
    }

    @Nullable
    public GroupLayoutMarketplaceLink getMarketplaceLink() {
        return marketplaceLink;
    }

    public GroupLayoutElement setMarketplaceLink(@Nullable GroupLayoutMarketplaceLink marketplaceLink) {
        this.marketplaceLink = marketplaceLink;
        return this;
    }

    // endregion

    // region Subclasses

    public static class GroupLayoutMarketplaceLink implements Serializable {
        @Nullable
        private String marketplaceId;
        @Nullable
        private String marketplaceVersion;

        // region Hash & Equals

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            GroupLayoutMarketplaceLink that = (GroupLayoutMarketplaceLink) o;
            return Objects.equals(marketplaceId, that.marketplaceId) && Objects.equals(marketplaceVersion, that.marketplaceVersion);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(marketplaceId);
            result = 31 * result + Objects.hashCode(marketplaceVersion);
            return result;
        }

        // endregion

        // region Getters & Setters

        @Nullable
        public String getMarketplaceId() {
            return marketplaceId;
        }

        public GroupLayoutMarketplaceLink setMarketplaceId(@Nullable String marketplaceId) {
            this.marketplaceId = marketplaceId;
            return this;
        }

        @Nullable
        public String getMarketplaceVersion() {
            return marketplaceVersion;
        }

        public GroupLayoutMarketplaceLink setMarketplaceVersion(@Nullable String marketplaceVersion) {
            this.marketplaceVersion = marketplaceVersion;
            return this;
        }

        // endregion
    }

    // endregion
}
