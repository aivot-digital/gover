package de.aivot.prosuna.backend.xrepository.models;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListColumnSetKeyRef {
    @JacksonXmlProperty(localName = "Ref", isAttribute = true)
    private String ref;

    public String getRef() {
        return ref;
    }

    public XRepositoryCodeListColumnSetKeyRef setRef(String ref) {
        this.ref = ref;
        return this;
    }
}
