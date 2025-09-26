package de.aivot.GoverBackend.xdf.v2.models;

public class XdfEnthaelt {
    public XdfDatenfeldgruppe datenfeldgruppe;
    public XdfDatenfeld datenfeld;

    public XdfDatenfeldgruppe getDatenfeldgruppe() {
        return datenfeldgruppe;
    }

    public XdfEnthaelt setDatenfeldgruppe(XdfDatenfeldgruppe datenfeldgruppe) {
        this.datenfeldgruppe = datenfeldgruppe;
        return this;
    }

    public XdfDatenfeld getDatenfeld() {
        return datenfeld;
    }

    public XdfEnthaelt setDatenfeld(XdfDatenfeld datenfeld) {
        this.datenfeld = datenfeld;
        return this;
    }
}
