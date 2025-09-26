package de.aivot.GoverBackend.xdf.v2.models;

public class XdfDatenfeld {
    public XdfIdentifikation identifikation;
    public String name;
    public String bezeichnungEingabe;
    public String bezeichnungAusgabe;
    public String beschreibung;
    public String definition;
    public String bezug;
    public XdfCodeListeWrapper status;
    public String fachlicherErsteller;
    public XdfCodeListeWrapper schemaelementart;
    public String hilfetextEingabe;
    public String hilfetextAusgabe;
    public XdfCodeListeWrapper feldart;
    public XdfCodeListeWrapper datentyp;
    public String praezisierung;
    public String inhalt;
    public XdfCodeListeReferenz codelisteReferenz;

    public XdfIdentifikation getIdentifikation() {
        return identifikation;
    }

    public XdfDatenfeld setIdentifikation(XdfIdentifikation identifikation) {
        this.identifikation = identifikation;
        return this;
    }

    public String getName() {
        return name;
    }

    public XdfDatenfeld setName(String name) {
        this.name = name;
        return this;
    }

    public String getBezeichnungEingabe() {
        return bezeichnungEingabe;
    }

    public XdfDatenfeld setBezeichnungEingabe(String bezeichnungEingabe) {
        this.bezeichnungEingabe = bezeichnungEingabe;
        return this;
    }

    public String getBezeichnungAusgabe() {
        return bezeichnungAusgabe;
    }

    public XdfDatenfeld setBezeichnungAusgabe(String bezeichnungAusgabe) {
        this.bezeichnungAusgabe = bezeichnungAusgabe;
        return this;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public XdfDatenfeld setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
        return this;
    }

    public String getDefinition() {
        return definition;
    }

    public XdfDatenfeld setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public String getBezug() {
        return bezug;
    }

    public XdfDatenfeld setBezug(String bezug) {
        this.bezug = bezug;
        return this;
    }

    public XdfCodeListeWrapper getStatus() {
        return status;
    }

    public XdfDatenfeld setStatus(XdfCodeListeWrapper status) {
        this.status = status;
        return this;
    }

    public String getFachlicherErsteller() {
        return fachlicherErsteller;
    }

    public XdfDatenfeld setFachlicherErsteller(String fachlicherErsteller) {
        this.fachlicherErsteller = fachlicherErsteller;
        return this;
    }

    public XdfCodeListeWrapper getSchemaelementart() {
        return schemaelementart;
    }

    public XdfDatenfeld setSchemaelementart(XdfCodeListeWrapper schemaelementart) {
        this.schemaelementart = schemaelementart;
        return this;
    }

    public String getHilfetextEingabe() {
        return hilfetextEingabe;
    }

    public XdfDatenfeld setHilfetextEingabe(String hilfetextEingabe) {
        this.hilfetextEingabe = hilfetextEingabe;
        return this;
    }

    public String getHilfetextAusgabe() {
        return hilfetextAusgabe;
    }

    public XdfDatenfeld setHilfetextAusgabe(String hilfetextAusgabe) {
        this.hilfetextAusgabe = hilfetextAusgabe;
        return this;
    }

    public XdfCodeListeWrapper getFeldart() {
        return feldart;
    }

    public XdfDatenfeld setFeldart(XdfCodeListeWrapper feldart) {
        this.feldart = feldart;
        return this;
    }

    public XdfCodeListeWrapper getDatentyp() {
        return datentyp;
    }

    public XdfDatenfeld setDatentyp(XdfCodeListeWrapper datentyp) {
        this.datentyp = datentyp;
        return this;
    }

    public String getPraezisierung() {
        return praezisierung;
    }

    public XdfDatenfeld setPraezisierung(String praezisierung) {
        this.praezisierung = praezisierung;
        return this;
    }

    public String getInhalt() {
        return inhalt;
    }

    public XdfDatenfeld setInhalt(String inhalt) {
        this.inhalt = inhalt;
        return this;
    }

    public XdfCodeListeReferenz getCodelisteReferenz() {
        return codelisteReferenz;
    }

    public XdfDatenfeld setCodelisteReferenz(XdfCodeListeReferenz codelisteReferenz) {
        this.codelisteReferenz = codelisteReferenz;
        return this;
    }
}
