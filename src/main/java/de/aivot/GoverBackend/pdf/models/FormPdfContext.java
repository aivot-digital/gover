package de.aivot.GoverBackend.pdf.models;

import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record FormPdfContext(String providerName, String logoAssetKey, String logoAssetName, GoverConfig config, FormPdfScope scope) {
    public Boolean isStaffPrint() {
        return scope == FormPdfScope.Staff;
    }

    public Boolean isNotStaffPrint() {
        return !isStaffPrint();
    }

    /**
     * @deprecated Use isStaffPrint() instead.
     * @return True if the form is printed for a destination submit.
     */
    public Boolean isDestinationPrint() {
        return isStaffPrint();
    }

    public Boolean isCitizenPrint() {
        return scope == FormPdfScope.Citizen;
    }

    public Boolean isBlankPrint() {
        return scope == FormPdfScope.Blank;
    }

    public Boolean isNotBlankPrint() {
        return !isBlankPrint();
    }

    public String createAssetUrl(String assetKey, String assetName) {
        var urlEncodedAssetName = URLEncoder
                .encode(assetName, StandardCharsets.UTF_8);
        return config.createUrl("api/public/assets/" + assetKey + "/" + urlEncodedAssetName);
    }

    public String createUrl(String suffix) {
        return config.createUrl(suffix);
    }
}
