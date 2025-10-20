package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListColumnSetColumnData {
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private String type;

    public String getType() {
        return type;
    }

    public XRepositoryCodeListColumnSetColumnData setType(String type) {
        this.type = type;
        return this;
    }
}
