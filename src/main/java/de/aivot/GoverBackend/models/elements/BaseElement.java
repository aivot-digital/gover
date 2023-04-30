package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.FunctionResult;
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
    private String name;

    private TestProtocolSet testProtocolSet;

    private Function isVisible;
    private FunctionCode patchElement;

    public BaseElement(Map<String, Object> values) {
        type = MapUtils.getEnum(values, "type", Integer.class, ElementType.values(), ElementType.Group);
        id = MapUtils.getString(values, "id", "missing_id");
        name = MapUtils.getString(values, "name", "");

        testProtocolSet = MapUtils.getApply(values, "testProtocolSet", Map.class, TestProtocolSet::new);

        isVisible = MapUtils.getApply(values, "isVisible", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "code") != null;
            return mainFunctionExists ? new FunctionCode(d) : new FunctionNoCode(d);
        });
        patchElement = MapUtils.getApply(values, "patchElement", Map.class, FunctionCode::new);

        applyValues(values);
    }

    public abstract void applyValues(Map<String, Object> values);

    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
    }

    public boolean isVisible(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        if (isVisible == null) {
            return true;
        }

        FunctionResult isVisibleResult = isVisible.evaluate(this, customerInput, getResolvedId(idPrefix), scriptEngine);
        return isVisibleResult.getBooleanValue();
    }

    public void patch(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        if (patchElement == null) {
            return;
        }
        FunctionResult patchElementResult = patchElement.evaluate(this, customerInput, getResolvedId(idPrefix), scriptEngine);
        applyValues(patchElementResult.getJsonValue());
    }

    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        return new LinkedList<>();
    }

    protected String getResolvedId(String idPrefix) {
        return idPrefix != null ? idPrefix + "_" + id : id;
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

    // endregion
}
