package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.FunctionResult;
import de.aivot.GoverBackend.models.lib.TestProtocolSet;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public abstract class BaseElement {
    private ElementType type;
    private String id;
    private String appVersion;
    private String name;

    private TestProtocolSet testProtocolSet;

    private Function isVisible;
    private FunctionCode patchElement;

    private ElementMetadata metadata;

    public BaseElement(Map<String, Object> values) {
        type = MapUtils.getEnum(values, "type", Integer.class, ElementType.values(), ElementType.Group);
        id = MapUtils.getString(values, "id", "missing_id");
        appVersion = MapUtils.getString(values, "appVersion", "0.0.0");
        name = MapUtils.getString(values, "name", "");

        testProtocolSet = MapUtils.getApply(values, "testProtocolSet", Map.class, TestProtocolSet::new);

        isVisible = MapUtils.getApply(values, "isVisible", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "code") != null;
            return mainFunctionExists ? new FunctionCode(d) : new FunctionNoCode(d);
        });
        patchElement = MapUtils.getApply(values, "patchElement", Map.class, FunctionCode::new);

        metadata = MapUtils.getApply(values, "metadata", Map.class, ElementMetadata::new);

        applyValues(values);
    }

    public BaseElement(ElementType type, String id, String appVersion, String name, TestProtocolSet testProtocolSet, Function isVisible, FunctionCode patchElement) {
        this.type = type;
        this.id = id;
        this.appVersion = appVersion;
        this.name = name;
        this.testProtocolSet = testProtocolSet;
        this.isVisible = isVisible;
        this.patchElement = patchElement;
    }

    public abstract void applyValues(Map<String, Object> values);

    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) throws ValidationException {
        // Children should overwrite this if validation is necessary.
    }

    public boolean isVisible(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) {
        if (isVisible == null) {
            return true;
        }

        FunctionResult isVisibleResult = isVisible.evaluate(idPrefix, root,this, customerInput, scriptEngine);

        if (isVisibleResult != null) {
            if (isVisibleResult.isNoCodeResult()) {
                return isVisibleResult.getObjectValue() == null;
            } else {
                return isVisibleResult.getBooleanValue();
            }
        }
        return false;
    }

    public void patch(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) {
        if (patchElement == null) {
            return;
        }
        FunctionResult patchElementResult = patchElement.evaluate(idPrefix, root,this, customerInput, scriptEngine);

        if (patchElementResult != null) {
            applyValues(patchElementResult.getJsonValue());
        }
    }

    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        return new LinkedList<>();
    }

    public String getResolvedId(String idPrefix) {
        return idPrefix != null ? idPrefix + "_" + id : id;
    }

    public boolean matches(String id) {
        return this.id.equals(id);
    }

    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseElement that = (BaseElement) o;

        if (type != that.type) return false;
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(appVersion, that.appVersion)) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(testProtocolSet, that.testProtocolSet))
            return false;
        if (!Objects.equals(isVisible, that.isVisible)) return false;
        if (!Objects.equals(patchElement, that.patchElement)) return false;
        return Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (appVersion != null ? appVersion.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (testProtocolSet != null ? testProtocolSet.hashCode() : 0);
        result = 31 * result + (isVisible != null ? isVisible.hashCode() : 0);
        result = 31 * result + (patchElement != null ? patchElement.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }


// region Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ElementType getType() {
        return type;
    }

    public void setType(ElementType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestProtocolSet getTestProtocolSet() {
        return testProtocolSet;
    }

    public void setTestProtocolSet(TestProtocolSet testProtocolSet) {
        this.testProtocolSet = testProtocolSet;
    }

    public Function getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Function isVisible) {
        this.isVisible = isVisible;
    }

    public FunctionCode getPatchElement() {
        return patchElement;
    }

    public void setPatchElement(FunctionCode patchElement) {
        this.patchElement = patchElement;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public ElementMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ElementMetadata metadata) {
        this.metadata = metadata;
    }

    // endregion
}
