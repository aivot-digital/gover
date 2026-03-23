package de.aivot.GoverBackend.xdf.v2.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "xdatenfelder.stammdatenschema.0102", namespace = "urn:xoev-de:fim:standard:xdatenfelder_2")
public class XdfStammdatenschema0102 {
    public XdfHeader header;
    public XdfStammdatenschema stammdatenschema;

    public XdfHeader getHeader() {
        return header;
    }

    public XdfStammdatenschema0102 setHeader(XdfHeader header) {
        this.header = header;
        return this;
    }

    public XdfStammdatenschema getStammdatenschema() {
        return stammdatenschema;
    }

    public XdfStammdatenschema0102 setStammdatenschema(XdfStammdatenschema stammdatenschema) {
        this.stammdatenschema = stammdatenschema;
        return this;
    }
}
