package de.aivot.GoverBackend.enums;

public enum SystemAssetKey {
    Favicon("provider_favicon.ico"),
    Logo("provider_logo.png"),
    ;

    private final String key;

    SystemAssetKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
