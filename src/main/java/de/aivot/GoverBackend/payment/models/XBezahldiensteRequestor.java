package de.aivot.GoverBackend.payment.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.GoverBackend.enums.XBezahldienstGender;
import de.aivot.GoverBackend.utils.StringUtils;

import java.io.Serializable;

public class XBezahldiensteRequestor implements Serializable {
    // Familienname des Bezahlers
    @JsonProperty("name")
    private String lastName = null;

    // Vorname(n) des Bezahlers
    @JsonProperty("firstName")
    private String firstName = null;

    // Geschlecht des Bezahlers: (M)ale=Männlich, (F)emale=Weiblich, (D)iverse=Divers
    @JsonProperty("gender")
    private XBezahldienstGender gender = null;

    // Wahr für Organisationen / juristische Personen, Falsch für 'natürliche' antragstellende Personen
    @JsonProperty("isOrganization")
    private Boolean isOrganization = null;

    // Name der Organisation / juristischen Person
    @JsonProperty("organizationName")
    private String organizationName = null;

    // Adresse des Bezahlers
    @JsonProperty("address")
    private XBezahldiensteAddress address = null;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName != null) {
            this.lastName = StringUtils.cleanAndTruncate(
                    lastName,
                    "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                    250
            );
        } else {
            this.lastName = null;
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName != null) {
            this.firstName = StringUtils.cleanAndTruncate(
                    firstName,
                    "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                    250
            );
        } else {
            this.firstName = null;
        }
    }

    public XBezahldienstGender getGender() {
        return gender;
    }

    public void setGender(XBezahldienstGender gender) {
        this.gender = gender;
    }

    public Boolean getOrganization() {
        return isOrganization;
    }

    public void setOrganization(Boolean organization) {
        isOrganization = organization;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        if (organizationName != null) {
            this.organizationName = StringUtils.cleanAndTruncate(
                    organizationName,
                    "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                    250
            );
        } else {
            this.organizationName = null;
        }
    }

    public XBezahldiensteAddress getAddress() {
        return address;
    }

    public void setAddress(XBezahldiensteAddress address) {
        this.address = address;
    }
}
