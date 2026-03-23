package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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
