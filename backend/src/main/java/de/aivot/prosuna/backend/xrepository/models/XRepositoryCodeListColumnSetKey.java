package de.aivot.prosuna.backend.xrepository.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XRepositoryCodeListColumnSetKey {
    @JacksonXmlProperty(localName = "Id", isAttribute = true)
    private String id;
    @JacksonXmlProperty(localName = "ShortName")
    private String shortName;
    @JacksonXmlProperty(localName = "ColumnRef")
    private XRepositoryCodeListColumnSetKeyRef columnRef;

    public String getId() {
        return id;
    }

    public XRepositoryCodeListColumnSetKey setId(String id) {
        this.id = id;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public XRepositoryCodeListColumnSetKey setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public XRepositoryCodeListColumnSetKeyRef getColumnRef() {
        return columnRef;
    }

    public XRepositoryCodeListColumnSetKey setColumnRef(XRepositoryCodeListColumnSetKeyRef columnRef) {
        this.columnRef = columnRef;
        return this;
    }
}
