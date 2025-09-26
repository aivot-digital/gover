package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XRepositoryCodeListAnnotationDescription {
    @JacksonXmlElementWrapper(localName = "shortName", namespace = "http://xoev.de/schemata/genericode/4")
    private String shortName;

    @JacksonXmlElementWrapper(localName = "codelistDescription", namespace = "http://xoev.de/schemata/genericode/4")
    private String codelistDescription;

    @JacksonXmlElementWrapper(localName = "agencyShortName", namespace = "http://xoev.de/schemata/genericode/4")
    private String agencyShortName;

    @JacksonXmlElementWrapper(localName = "validFrom", namespace = "http://xoev.de/schemata/genericode/4")
    private String validFrom;

    @JacksonXmlElementWrapper(localName = "versionHandbook", namespace = "http://xoev.de/schemata/genericode/4")
    private String versionHandbook;

    public String getShortName() {
        return shortName;
    }

    public XRepositoryCodeListAnnotationDescription setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getCodelistDescription() {
        return codelistDescription;
    }

    public XRepositoryCodeListAnnotationDescription setCodelistDescription(String codelistDescription) {
        this.codelistDescription = codelistDescription;
        return this;
    }

    public String getAgencyShortName() {
        return agencyShortName;
    }

    public XRepositoryCodeListAnnotationDescription setAgencyShortName(String agencyShortName) {
        this.agencyShortName = agencyShortName;
        return this;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public XRepositoryCodeListAnnotationDescription setValidFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public String getVersionHandbook() {
        return versionHandbook;
    }

    public XRepositoryCodeListAnnotationDescription setVersionHandbook(String versionHandbook) {
        this.versionHandbook = versionHandbook;
        return this;
    }
}