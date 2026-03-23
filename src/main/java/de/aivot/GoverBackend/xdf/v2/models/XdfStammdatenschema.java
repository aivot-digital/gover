package de.aivot.GoverBackend.xdf.v2.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class XdfStammdatenschema {
    public XdfIdentifikation identifikation;
    public String name;
    public String bezeichnungEingabe;
    public String bezeichnungAusgabe;
    public String beschreibung;
    public String definition;
    public String bezug;
    public XdfCodeListeWrapper status;
    public String fachlicherErsteller;
    public String freigabedatum;
    public String veroeffentlichungsdatum;
    public XdfCodeListeWrapper ableitungsmodifikationenStruktur;
    public XdfCodeListeWrapper ableitungsmodifikationenRepraesentation;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "struktur")
    public List<XdfStruktur> struktur;

    public XdfIdentifikation getIdentifikation() {
        return identifikation;
    }

    public XdfStammdatenschema setIdentifikation(XdfIdentifikation identifikation) {
        this.identifikation = identifikation;
        return this;
    }

    public String getName() {
        return name;
    }

    public XdfStammdatenschema setName(String name) {
        this.name = name;
        return this;
    }

    public String getBezeichnungEingabe() {
        return bezeichnungEingabe;
    }

    public XdfStammdatenschema setBezeichnungEingabe(String bezeichnungEingabe) {
        this.bezeichnungEingabe = bezeichnungEingabe;
        return this;
    }

    public String getBezeichnungAusgabe() {
        return bezeichnungAusgabe;
    }

    public XdfStammdatenschema setBezeichnungAusgabe(String bezeichnungAusgabe) {
        this.bezeichnungAusgabe = bezeichnungAusgabe;
        return this;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public XdfStammdatenschema setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
        return this;
    }

    public String getDefinition() {
        return definition;
    }

    public XdfStammdatenschema setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public String getBezug() {
        return bezug;
    }

    public XdfStammdatenschema setBezug(String bezug) {
        this.bezug = bezug;
        return this;
    }

    public XdfCodeListeWrapper getStatus() {
        return status;
    }

    public XdfStammdatenschema setStatus(XdfCodeListeWrapper status) {
        this.status = status;
        return this;
    }

    public String getFachlicherErsteller() {
        return fachlicherErsteller;
    }

    public XdfStammdatenschema setFachlicherErsteller(String fachlicherErsteller) {
        this.fachlicherErsteller = fachlicherErsteller;
        return this;
    }

    public String getFreigabedatum() {
        return freigabedatum;
    }

    public XdfStammdatenschema setFreigabedatum(String freigabedatum) {
        this.freigabedatum = freigabedatum;
        return this;
    }

    public String getVeroeffentlichungsdatum() {
        return veroeffentlichungsdatum;
    }

    public XdfStammdatenschema setVeroeffentlichungsdatum(String veroeffentlichungsdatum) {
        this.veroeffentlichungsdatum = veroeffentlichungsdatum;
        return this;
    }

    public XdfCodeListeWrapper getAbleitungsmodifikationenStruktur() {
        return ableitungsmodifikationenStruktur;
    }

    public XdfStammdatenschema setAbleitungsmodifikationenStruktur(XdfCodeListeWrapper ableitungsmodifikationenStruktur) {
        this.ableitungsmodifikationenStruktur = ableitungsmodifikationenStruktur;
        return this;
    }

    public XdfCodeListeWrapper getAbleitungsmodifikationenRepraesentation() {
        return ableitungsmodifikationenRepraesentation;
    }

    public XdfStammdatenschema setAbleitungsmodifikationenRepraesentation(XdfCodeListeWrapper ableitungsmodifikationenRepraesentation) {
        this.ableitungsmodifikationenRepraesentation = ableitungsmodifikationenRepraesentation;
        return this;
    }

    public List<XdfStruktur> getStruktur() {
        return struktur;
    }

    public XdfStammdatenschema setStruktur(List<XdfStruktur> struktur) {
        this.struktur = struktur;
        return this;
    }
}
