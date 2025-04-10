package de.aivot.GoverBackend.elements.models.form.layout;

import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class GroupLayout extends BaseFormElement {
    private Collection<BaseFormElement> children;
    private GroupLayoutStoreLink storeLink;

    public GroupLayout(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        children = MapUtils.getCollection(values, "children", ElementResolver::resolve);

        var storeLink = MapUtils.get(values, "storeLink", Map.class, null);
        if (storeLink != null) {
            this.storeLink = new GroupLayoutStoreLink(storeLink);
        } else {
            this.storeLink = null;
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String idPrefix, FormState formState) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (var child : children) {
                var isVisible = formState.visibilities().getOrDefault(child.getResolvedId(idPrefix), true);
                if (!isVisible) {
                    continue;
                }
                var resolvedChild = formState.overrides().getOrDefault(child.getResolvedId(idPrefix), child);
                rows.addAll(resolvedChild.toPdfRows(root, idPrefix, formState));
            }
        }

        return rows;
    }

    @Override
    public Optional<BaseFormElement> findChild(@Nonnull String id) {
        Optional<BaseFormElement> matchingChild = children
                .stream()
                .filter(s -> s.matches(id))
                .findFirst();

        if (matchingChild.isPresent()) {
            return matchingChild;
        }

        return children
                .stream()
                .map(c -> {
                    Optional<BaseFormElement> res = Optional.empty();
                    if (c instanceof GroupLayout groupLayout) {
                        res = groupLayout.findChild(id);
                    } else if (c instanceof ReplicatingContainerLayout replicatingContainerLayout) {
                        res = replicatingContainerLayout.findChild(id);
                    }
                    return res;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupLayout that = (GroupLayout) o;

        if (!Objects.equals(children, that.children)) return false;
        return Objects.equals(storeLink, that.storeLink);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (children != null ? children.hashCode() : 0);
        result = 31 * result + (storeLink != null ? storeLink.hashCode() : 0);
        return result;
    }

    public Collection<BaseFormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<BaseFormElement> children) {
        this.children = children;
    }

    public GroupLayoutStoreLink getStoreLink() {
        return storeLink;
    }

    public void setStoreLink(GroupLayoutStoreLink storeLink) {
        this.storeLink = storeLink;
    }

    public static class GroupLayoutStoreLink {
        private final String storeId;
        private final String storeVersion;

        public GroupLayoutStoreLink(Map<String, Object> data) {
            this.storeId = MapUtils.getString(data, "storeId");
            this.storeVersion = MapUtils.getString(data, "storeVersion");
        }

        public String getStoreId() {
            return storeId;
        }

        public String getStoreVersion() {
            return storeVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GroupLayoutStoreLink that = (GroupLayoutStoreLink) o;

            if (!Objects.equals(storeId, that.storeId)) return false;
            return Objects.equals(storeVersion, that.storeVersion);
        }

        @Override
        public int hashCode() {
            int result = storeId != null ? storeId.hashCode() : 0;
            result = 31 * result + (storeVersion != null ? storeVersion.hashCode() : 0);
            return result;
        }
    }
}
