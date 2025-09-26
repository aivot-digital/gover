package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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
