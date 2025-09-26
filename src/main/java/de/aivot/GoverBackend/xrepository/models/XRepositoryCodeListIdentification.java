package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XRepositoryCodeListIdentification {
    @JacksonXmlProperty(localName = "ShortName")
    private String shortName;
    @JacksonXmlProperty(localName = "LongName")
    private String longName;
    @JacksonXmlProperty(localName = "Version")
    private String version;
    @JacksonXmlProperty(localName = "CanonicalUri")
    private String canonicalUri;
    @JacksonXmlProperty(localName = "CanonicalVersionUri")
    private String canonicalVersionUri;
    @JacksonXmlProperty(localName = "Agency")
    private XRepositoryCodeListIdentificationAgency agency;

    public String getShortName() {
        return shortName;
    }

    public XRepositoryCodeListIdentification setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getLongName() {
        return longName;
    }

    public XRepositoryCodeListIdentification setLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public XRepositoryCodeListIdentification setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getCanonicalUri() {
        return canonicalUri;
    }

    public XRepositoryCodeListIdentification setCanonicalUri(String canonicalUri) {
        this.canonicalUri = canonicalUri;
        return this;
    }

    public String getCanonicalVersionUri() {
        return canonicalVersionUri;
    }

    public XRepositoryCodeListIdentification setCanonicalVersionUri(String canonicalVersionUri) {
        this.canonicalVersionUri = canonicalVersionUri;
        return this;
    }

    public XRepositoryCodeListIdentificationAgency getAgency() {
        return agency;
    }

    public XRepositoryCodeListIdentification setAgency(XRepositoryCodeListIdentificationAgency agency) {
        this.agency = agency;
        return this;
    }
}
