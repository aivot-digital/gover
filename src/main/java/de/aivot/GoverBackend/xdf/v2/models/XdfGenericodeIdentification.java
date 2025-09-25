package de.aivot.GoverBackend.xdf.v2.models;

public class XdfGenericodeIdentification {
    public String canonicalIdentification;
    public String version;
    public String canonicalVersionUri;

    public String getCanonicalIdentification() {
        return canonicalIdentification;
    }

    public XdfGenericodeIdentification setCanonicalIdentification(String canonicalIdentification) {
        this.canonicalIdentification = canonicalIdentification;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public XdfGenericodeIdentification setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getCanonicalVersionUri() {
        return canonicalVersionUri;
    }

    public XdfGenericodeIdentification setCanonicalVersionUri(String canonicalVersionUri) {
        this.canonicalVersionUri = canonicalVersionUri;
        return this;
    }
}

