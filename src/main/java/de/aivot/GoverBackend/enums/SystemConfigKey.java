package de.aivot.GoverBackend.enums;

public enum SystemConfigKey {
    ProviderName("ProviderName", "Aivot UG (haftungsbeschränkt)", true),

    @Deprecated
    ProviderNameShort("ProviderNameShort", "Aivot", true),

    @Deprecated
    ProviderNameLong("ProviderNameLong", "Aivot UG\nDigitale Lösungen für\nden Öffentlichen Dienst", true),

    SystemHost("SystemHost", "https://gover.aivot.de", false),
    SystemTheme("SystemTheme", "0", true),
    ;

    private final String key;
    private final String def;
    private final boolean isPublic;

    SystemConfigKey(String key, String def, boolean isPublic) {
        this.key = key;
        this.def = def;
        this.isPublic = isPublic;
    }

    public String getKey() {
        return key;
    }

    public String getDef() {
        return def;
    }

    public boolean isPublic() {
        return isPublic;
    }
}
