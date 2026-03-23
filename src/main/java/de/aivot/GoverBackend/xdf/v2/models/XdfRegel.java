package de.aivot.GoverBackend.xdf.v2.models;

public class XdfRegel {
    public XdfIdentifikation identifikation;
    public String name;
    public String bezeichnungEingabe;
    public String beschreibung;
    public String definition;
    public String bezug;
    public XdfCodeListeWrapper status;
    public String fachlicherErsteller;
    public String script;

    public XdfIdentifikation getIdentifikation() {
        return identifikation;
    }

    public XdfRegel setIdentifikation(XdfIdentifikation identifikation) {
        this.identifikation = identifikation;
        return this;
    }

    public String getName() {
        return name;
    }

    public XdfRegel setName(String name) {
        this.name = name;
        return this;
    }

    public String getBezeichnungEingabe() {
        return bezeichnungEingabe;
    }

    public XdfRegel setBezeichnungEingabe(String bezeichnungEingabe) {
        this.bezeichnungEingabe = bezeichnungEingabe;
        return this;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public XdfRegel setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
        return this;
    }

    public String getDefinition() {
        return definition;
    }

    public XdfRegel setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public String getBezug() {
        return bezug;
    }

    public XdfRegel setBezug(String bezug) {
        this.bezug = bezug;
        return this;
    }

    public XdfCodeListeWrapper getStatus() {
        return status;
    }

    public XdfRegel setStatus(XdfCodeListeWrapper status) {
        this.status = status;
        return this;
    }

    public String getFachlicherErsteller() {
        return fachlicherErsteller;
    }

    public XdfRegel setFachlicherErsteller(String fachlicherErsteller) {
        this.fachlicherErsteller = fachlicherErsteller;
        return this;
    }

    public String getScript() {
        return script;
    }

    public XdfRegel setScript(String script) {
        this.script = script;
        return this;
    }
}
