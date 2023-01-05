package de.aivot.GoverBackend.enums;

public enum SystemConfigKey {
    ProviderName("ProviderName", "Aivot UG (haftungsbeschränkt)"),
    ProviderNameShort("ProviderNameShort", "Aivot"),
    ProviderNameLong("ProviderNameLong", "Aivot UG\nDigitale Lösungen für\nden Öffentlichen Dienst"),

    SystemHost("SystemHost", "https://gover.aivot.de"),
    SystemTheme("SystemTheme", "0"),
    ;

    private final String key;
    private final String def;

    SystemConfigKey(String key, String def) {
        this.key = key;
        this.def = def;
    }

    public String getKey() {
        return key;
    }

    public String getDef() {
        return def;
    }
}
