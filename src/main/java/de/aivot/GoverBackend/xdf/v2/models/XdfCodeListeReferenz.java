package de.aivot.GoverBackend.xdf.v2.models;

public class XdfCodeListeReferenz {
    public XdfIdentifikation identifikation;
    public XdfGenericodeIdentification genericodeIdentification;

    public XdfIdentifikation getIdentifikation() {
        return identifikation;
    }

    public XdfCodeListeReferenz setIdentifikation(XdfIdentifikation identifikation) {
        this.identifikation = identifikation;
        return this;
    }

    public XdfGenericodeIdentification getGenericodeIdentification() {
        return genericodeIdentification;
    }

    public XdfCodeListeReferenz setGenericodeIdentification(XdfGenericodeIdentification genericodeIdentification) {
        this.genericodeIdentification = genericodeIdentification;
        return this;
    }
}
