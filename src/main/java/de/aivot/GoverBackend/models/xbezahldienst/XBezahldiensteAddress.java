package de.aivot.GoverBackend.models.xbezahldienst;

import com.google.gson.annotations.SerializedName;
import de.aivot.GoverBackend.utils.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class XBezahldiensteAddress implements Serializable {
    // Strasse
    @SerializedName("street")
    private String street = null;

    // Hausnummer
    @SerializedName("houseNumber")
    private String houseNumber = null;

    // Zum Abbilden von Internationalen Adressen werden generische "Adresszeilen" verwendet und nicht "Straße / Hausnummer".
    @SerializedName("addressLine")
    private List<String> addressLine = null;

    // Postleitzahl
    @SerializedName("postalCode")
    private String postalCode = null;

    // Stadt
    @SerializedName("city")
    private String city = null;

    // ISO 3166-1, Alpha-2 code, zwei Großbuchstaben
    @SerializedName("country")
    private String country = null;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        if (street != null) {
            this.street = StringUtils
                    .cleanAndTruncate(
                            street,
                            "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                            250
                    );
        } else {
            this.street = null;
        }
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        if (houseNumber != null) {
            this.houseNumber = StringUtils.cleanAndTruncate(
                    houseNumber,
                    "[^\\w\\d-]",
                    20
            );
        } else {
            this.houseNumber = null;
        }
    }

    public List<String> getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        if (addressLine != null) {
            this.addressLine = Arrays
                    .stream(addressLine.split("[^\r\n]+"))
                    .map(ln -> StringUtils.cleanAndTruncate(
                                    ln,
                                    "[^\\w\\d\\s-,:\\.\\u00C0-\\u017F]",
                                    250
                            )
                    )
                    .toList();
            if (this.addressLine.size() > 9) {
                this.addressLine = this.addressLine.subList(0, 9);
            }
        } else {
            this.addressLine = null;
        }
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        if (postalCode != null) {
            this.postalCode = StringUtils.cleanAndTruncate(
                    postalCode,
                    "\\D",
                    5
            );
        } else {
            this.postalCode = null;
        }
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        if (city != null) {
            this.city = StringUtils.cleanAndTruncate(
                    city,
                    "[^\\w\\d\\s-,\\u00C0-\\u017F]",
                    250
            );
        } else {
            this.city = null;
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        if (country != null) {
            this.country = StringUtils.cleanAndTruncate(
                    country,
                    "[^\\w]",
                    2
            );
        } else {
            this.country = null;
        }
    }
}
