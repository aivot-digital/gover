package de.aivot.prosuna.backend.xrepository.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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
