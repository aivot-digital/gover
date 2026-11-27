package de.aivot.prosuna.backend.xrepository.models;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class XRepositoryCodeListSimpleCodeListRow {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Value")
    private List<XRepositoryCodeListSimpleCodeListRowValue> value;

    public List<XRepositoryCodeListSimpleCodeListRowValue> getValue() {
        return value;
    }

    public XRepositoryCodeListSimpleCodeListRow setValue(List<XRepositoryCodeListSimpleCodeListRowValue> value) {
        this.value = value;
        return this;
    }
}
