package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.FunctionResult;
import de.aivot.GoverBackend.models.functions.conditions.Condition;
import de.aivot.GoverBackend.models.lib.TestProtocolSet;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class BaseElement {
    private ElementType type;
    private String id;
    private String appVersion;
    private String name;

    private TestProtocolSet testProtocolSet;

    private Function isVisible;
    private FunctionCode patchElement;

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
            return isVisibleResult.getObjectValue() == null;
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

    // endregion
}
