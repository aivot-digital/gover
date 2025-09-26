package de.aivot.GoverBackend.xdf.v2.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class XdfDatenfeldgruppe {
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
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "regel")
    public List<XdfRegel> regel;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "struktur")
    public List<XdfStruktur> struktur;

    public XdfIdentifikation getIdentifikation() {
        return identifikation;
    }

    public XdfDatenfeldgruppe setIdentifikation(XdfIdentifikation identifikation) {
        this.identifikation = identifikation;
        return this;
    }

    public String getName() {
        return name;
    }

    public XdfDatenfeldgruppe setName(String name) {
        this.name = name;
        return this;
    }

    public String getBezeichnungEingabe() {
        return bezeichnungEingabe;
    }

    public XdfDatenfeldgruppe setBezeichnungEingabe(String bezeichnungEingabe) {
        this.bezeichnungEingabe = bezeichnungEingabe;
        return this;
    }

    public String getBezeichnungAusgabe() {
        return bezeichnungAusgabe;
    }

    public XdfDatenfeldgruppe setBezeichnungAusgabe(String bezeichnungAusgabe) {
        this.bezeichnungAusgabe = bezeichnungAusgabe;
        return this;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public XdfDatenfeldgruppe setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
        return this;
    }

    public String getDefinition() {
        return definition;
    }

    public XdfDatenfeldgruppe setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public String getBezug() {
        return bezug;
    }

    public XdfDatenfeldgruppe setBezug(String bezug) {
        this.bezug = bezug;
        return this;
    }

    public XdfCodeListeWrapper getStatus() {
        return status;
    }

    public XdfDatenfeldgruppe setStatus(XdfCodeListeWrapper status) {
        this.status = status;
        return this;
    }

    public String getFachlicherErsteller() {
        return fachlicherErsteller;
    }

    public XdfDatenfeldgruppe setFachlicherErsteller(String fachlicherErsteller) {
        this.fachlicherErsteller = fachlicherErsteller;
        return this;
    }

    public XdfCodeListeWrapper getSchemaelementart() {
        return schemaelementart;
    }

    public XdfDatenfeldgruppe setSchemaelementart(XdfCodeListeWrapper schemaelementart) {
        this.schemaelementart = schemaelementart;
        return this;
    }

    public String getHilfetextEingabe() {
        return hilfetextEingabe;
    }

    public XdfDatenfeldgruppe setHilfetextEingabe(String hilfetextEingabe) {
        this.hilfetextEingabe = hilfetextEingabe;
        return this;
    }

    public String getHilfetextAusgabe() {
        return hilfetextAusgabe;
    }

    public XdfDatenfeldgruppe setHilfetextAusgabe(String hilfetextAusgabe) {
        this.hilfetextAusgabe = hilfetextAusgabe;
        return this;
    }

    public List<XdfRegel> getRegel() {
        return regel;
    }

    public XdfDatenfeldgruppe setRegel(List<XdfRegel> regel) {
        this.regel = regel;
        return this;
    }

    public List<XdfStruktur> getStruktur() {
        return struktur;
    }

    public XdfDatenfeldgruppe setStruktur(List<XdfStruktur> struktur) {
        this.struktur = struktur;
        return this;
    }
}
