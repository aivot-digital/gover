package de.aivot.GoverBackend.xdf.v2.models;

public class XdfStruktur {
    public String anzahl;
    public String bezug;
    public XdfEnthaelt enthaelt;

    public String getAnzahl() {
        return anzahl;
    }

    public XdfStruktur setAnzahl(String anzahl) {
        this.anzahl = anzahl;
        return this;
    }

    public String getBezug() {
        return bezug;
    }

    public XdfStruktur setBezug(String bezug) {
        this.bezug = bezug;
        return this;
    }

    public XdfEnthaelt getEnthaelt() {
        return enthaelt;
    }

    public XdfStruktur setEnthaelt(XdfEnthaelt enthaelt) {
        this.enthaelt = enthaelt;
        return this;
    }
}

