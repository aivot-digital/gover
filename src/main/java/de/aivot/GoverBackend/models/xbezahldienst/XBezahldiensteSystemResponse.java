package de.aivot.GoverBackend.models.xbezahldienst;

import com.google.gson.annotations.SerializedName;
import de.aivot.GoverBackend.enums.XBezahldienstFunctionalCode;

import java.io.Serializable;

public class XBezahldiensteSystemResponse implements Serializable {
    // Link auf eine Seite mit der Fehlerbeschreibung
    @SerializedName("type")
    private String type = null;

    // HTTP Response Code
    @SerializedName("status")
    private Integer status = null;

    // HTTP Response Text
    @SerializedName("title")
    private String title = null;

    // Funktionale Beschreibung des Fehlers.
    // Hier sollen aber nur 'fachliche Beschreibungen' erfolgen.
    // Aus Sicherheitsgründen sollten die Bezahldienste hier keine Systeminterna herausgeben.
    // Eine Referenz-Nummer für den Austausch zwischen den Betriebsorganisationen scheint sinnvoll.
    @SerializedName("detail")
    private String detail = null;

    // Die genauen Ausprägungen der Funktionalen Response müssen noch diskutiert werden.
    @SerializedName("functionalCode")
    private XBezahldienstFunctionalCode functionalCode = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public XBezahldienstFunctionalCode getFunctionalCode() {
        return functionalCode;
    }

    public void setFunctionalCode(XBezahldienstFunctionalCode functionalCode) {
        this.functionalCode = functionalCode;
    }
}
