package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.models.config.GoverConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record PdfContext(String providerName, String logoAssetKey, String logoAssetName, GoverConfig config, Boolean isSummary) {
    public Boolean isForm() {
        return !isSummary;
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
