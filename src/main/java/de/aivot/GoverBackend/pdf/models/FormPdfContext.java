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
        return config.createUrlWithTrailingSlash("/api/public/assets", assetKey);
    }

    public String createUrl(String suffix) {
        return config.createUrl(suffix);
    }

    public String createUrlWithQueryParameter(String suffix, String key, String value) {
        var url = createUrl(suffix);
        var separator = url.contains("?") ? "&" : "?";
        return url + separator + encodeQueryParameter(key) + "=" + encodeQueryParameter(value);
    }

    private String encodeQueryParameter(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
