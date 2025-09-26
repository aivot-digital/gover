package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListColumnSetColumn {
    @JacksonXmlProperty(localName = "Id", isAttribute = true)
    private String id;
    @JacksonXmlProperty(localName = "Use", isAttribute = true)
    private String use;
    @JacksonXmlProperty(localName = "ShortName")
    private String shortName;
    @JacksonXmlProperty(localName = "LongName")
    private String longName;
    @JacksonXmlProperty(localName = "Data")
    private XRepositoryCodeListColumnSetColumnData data;

    public String getId() {
        return id;
    }

    public XRepositoryCodeListColumnSetColumn setId(String id) {
        this.id = id;
        return this;
    }

    public String getUse() {
        return use;
    }

    public XRepositoryCodeListColumnSetColumn setUse(String use) {
        this.use = use;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public XRepositoryCodeListColumnSetColumn setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getLongName() {
        return longName;
    }

    public XRepositoryCodeListColumnSetColumn setLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public XRepositoryCodeListColumnSetColumnData getData() {
        return data;
    }

    public XRepositoryCodeListColumnSetColumn setData(XRepositoryCodeListColumnSetColumnData data) {
        this.data = data;
        return this;
    }
}
