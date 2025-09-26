package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListAnnotation {
    @JacksonXmlProperty(localName = "Description")
    private XRepositoryCodeListAnnotationDescription description;

    public XRepositoryCodeListAnnotationDescription getDescription() {
        return description;
    }

    public XRepositoryCodeListAnnotation setDescription(XRepositoryCodeListAnnotationDescription description) {
        this.description = description;
        return this;
    }
}