package de.aivot.GoverBackend.models.elements;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.functions.FunctionSet;
import de.aivot.GoverBackend.models.TestProtocolSet;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import net.minidev.json.annotate.JsonIgnore;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class BaseElement {
    private ElementType type;
    private String id;
    private String name;

    private TestProtocolSet testProtocolSet;
    private FunctionSet functionSet;

    protected BaseElement(Map<String, Object> data) {

        type = ElementType.findElement(data.get("type")).orElse(null);
        id = (String) data.get("id");
        name = (String) data.get("name");

        if (data.containsKey("testProtocolSet")) {
            testProtocolSet = new TestProtocolSet((Map<String, Object>) data.get("testProtocolSet"));
        }

        if (data.containsKey("functionSet")) {
            functionSet = new FunctionSet((Map<String, Object>) data.get("functionSet"));
        }
    }

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

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public TestProtocolSet getTestProtocolSet() {
        return testProtocolSet;
    }

    public void setTestProtocolSet(TestProtocolSet testProtocolSet) {
        this.testProtocolSet = testProtocolSet;
    }

    @Nullable
    public FunctionSet getFunctionSet() {
        return functionSet;
    }

    public void setFunctionSet(FunctionSet functionSet) {
        this.functionSet = functionSet;
    }

    public boolean isVisible(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return true;
    }

    public boolean isValid(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return true;
    }

    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return new LinkedList<>();
    }
}
