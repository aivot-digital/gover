package de.aivot.GoverBackend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "CodeList", namespace = "http://docs.oasis-open.org/codelist/ns/genericode/1.0/")
public class XRepositoryCodeList {
    @JacksonXmlProperty(localName = "Annotation")
    private XRepositoryCodeListAnnotation annotation;
    @JacksonXmlProperty(localName = "Identification")
    private XRepositoryCodeListIdentification identification;
    @JacksonXmlProperty(localName = "ColumnSet")
    private XRepositoryCodeListColumnSet columnSet;
    @JacksonXmlProperty(localName = "SimpleCodeList")
    private XRepositoryCodeListSimpleCodeList codeList;

    public XRepositoryCodeListAnnotation getAnnotation() {
        return annotation;
    }

    public XRepositoryCodeList setAnnotation(XRepositoryCodeListAnnotation annotation) {
        this.annotation = annotation;
        return this;
    }

    public XRepositoryCodeListIdentification getIdentification() {
        return identification;
    }

    public XRepositoryCodeList setIdentification(XRepositoryCodeListIdentification identification) {
        this.identification = identification;
        return this;
    }

    public XRepositoryCodeListColumnSet getColumnSet() {
        return columnSet;
    }

    public XRepositoryCodeList setColumnSet(XRepositoryCodeListColumnSet columnSet) {
        this.columnSet = columnSet;
        return this;
    }

    public XRepositoryCodeListSimpleCodeList getCodeList() {
        return codeList;
    }

    public XRepositoryCodeList setCodeList(XRepositoryCodeListSimpleCodeList codeList) {
        this.codeList = codeList;
        return this;
    }
}
