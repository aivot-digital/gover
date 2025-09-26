package de.aivot.GoverBackend.xdf.v2.models;

public class XdfCodeListeWrapper {
    public String listURI;
    public String listVersionID;
    public String code;

    public String getListURI() {
        return listURI;
    }

    public XdfCodeListeWrapper setListURI(String listURI) {
        this.listURI = listURI;
        return this;
    }

    public String getListVersionID() {
        return listVersionID;
    }

    public XdfCodeListeWrapper setListVersionID(String listVersionID) {
        this.listVersionID = listVersionID;
        return this;
    }

    public String getCode() {
        return code;
    }

    public XdfCodeListeWrapper setCode(String code) {
        this.code = code;
        return this;
    }
}
