package de.aivot.GoverBackend.models.elements.form.layout;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
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
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) throws ValidationException {
        if (children != null) {
            for (var child : children) {
                child.patch(idPrefix, root, customerInput, scriptEngine);
                if (child.isVisible(idPrefix, root, customerInput, scriptEngine)) {
                    child.validate(idPrefix, root, customerInput, scriptEngine);
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (var child : children) {
                child.patch(idPrefix, root, customerInput, scriptEngine);
                if (child.isVisible(idPrefix, root, customerInput, scriptEngine)) {
                    rows.addAll(child.toPdfRows(root, customerInput, idPrefix, scriptEngine));
                }
            }
        }

        return rows;
    }

    public Optional<BaseFormElement> findChild(String id) {
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
