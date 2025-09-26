package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class XRepositoryCodeListColumnSet {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Column")
    private List<XRepositoryCodeListColumnSetColumn> column;
    @JacksonXmlProperty(localName = "Key")
    private XRepositoryCodeListColumnSetKey key;

    public List<XRepositoryCodeListColumnSetColumn> getColumn() {
        return column;
    }

    public XRepositoryCodeListColumnSet setColumn(List<XRepositoryCodeListColumnSetColumn> column) {
        this.column = column;
        return this;
    }

    public XRepositoryCodeListColumnSetKey getKey() {
        return key;
    }

    public XRepositoryCodeListColumnSet setKey(XRepositoryCodeListColumnSetKey key) {
        this.key = key;
        return this;
    }
}
