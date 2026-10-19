package de.aivot.prosuna.backend.xrepository.models;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class XRepositoryCodeListSimpleCodeList {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Row")
    private List<XRepositoryCodeListSimpleCodeListRow> row;

    public List<XRepositoryCodeListSimpleCodeListRow> getRow() {
        return row;
    }

    public XRepositoryCodeListSimpleCodeList setRow(List<XRepositoryCodeListSimpleCodeListRow> row) {
        this.row = row;
        return this;
    }
}
