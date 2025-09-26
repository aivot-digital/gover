package de.aivot.GoverBackend.xdf.v2.models;

public class XdfIdentifikation {
    public String id;
    public String version;

    public String getId() {
        return id;
    }

    public XdfIdentifikation setId(String id) {
        this.id = id;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public XdfIdentifikation setVersion(String version) {
        this.version = version;
        return this;
    }
}
