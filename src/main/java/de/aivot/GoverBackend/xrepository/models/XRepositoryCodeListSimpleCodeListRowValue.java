package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListSimpleCodeListRowValue {
    @JacksonXmlProperty(localName = "ColumnRef", isAttribute = true)
    private String columnRef;
    @JacksonXmlProperty(localName = "SimpleValue")
    private String simpleValue;

    public String getColumnRef() {
        return columnRef;
    }

    public XRepositoryCodeListSimpleCodeListRowValue setColumnRef(String columnRef) {
        this.columnRef = columnRef;
        return this;
    }

    public String getSimpleValue() {
        return simpleValue;
    }

    public XRepositoryCodeListSimpleCodeListRowValue setSimpleValue(String simpleValue) {
        this.simpleValue = simpleValue;
        return this;
    }
}
