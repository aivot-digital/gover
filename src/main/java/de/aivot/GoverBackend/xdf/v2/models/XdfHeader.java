package de.aivot.GoverBackend.xdf.v2.models;

public class XdfHeader {
    public String nachrichtID;
    public String erstellungszeitpunkt;

    public String getNachrichtID() {
        return nachrichtID;
    }

    public XdfHeader setNachrichtID(String nachrichtID) {
        this.nachrichtID = nachrichtID;
        return this;
    }

    public String getErstellungszeitpunkt() {
        return erstellungszeitpunkt;
    }

    public XdfHeader setErstellungszeitpunkt(String erstellungszeitpunkt) {
        this.erstellungszeitpunkt = erstellungszeitpunkt;
        return this;
    }
}
