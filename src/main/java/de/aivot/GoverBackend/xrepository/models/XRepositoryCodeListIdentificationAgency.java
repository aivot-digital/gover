package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListIdentificationAgency {
    @JacksonXmlProperty(localName = "LongName")
    private String longName;

    public String getLongName() {
        return longName;
    }

    public XRepositoryCodeListIdentificationAgency setLongName(String longName) {
        this.longName = longName;
        return this;
    }
}
