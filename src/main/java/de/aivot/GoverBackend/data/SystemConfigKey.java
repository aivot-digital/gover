package de.aivot.GoverBackend.data;

import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Optional;

public enum SystemConfigKey implements Identifiable<String> {
    PROVIDER__NAME("ProviderName", true),

    PROVIDER__LISTINGPAGE__IMPRINTDEPARTMENTID("ProviderListingPageImprintDepartmentId", true),
    PROVIDER__LISTINGPAGE__PRIVACYDEPARTMENTID("ProviderListingPagePrivacyDepartmentId", true),
    PROVIDER__LISTINGPAGE__ACCESSIBILITYDEPARTMENTID("ProviderListingPageAccessibilityDepartmentId", true),
    PROVIDER__LISTINGPAGE__CUSTOMLINK("ProviderListingPageCustomLink", true),
    PROVIDER__LISTINGPAGE__DISABLEDLINK("ProviderListingPageDisableLink", true),
    PROVIDER__LISTINGPAGE__DISABLE("ProviderListingPageDisablePublicListingPage", true),

    SYSTEM__THEME("SystemTheme", true),
    SYSTEM__LOGO("SystemLogo", true),
    SYSTEM__FAVICON("SystemFavicon", true),

    EXPERIMENTAL_FEATURES__COMPLEXITY("ExperimentalFeaturesComplexity", false),
    EXPERIMENTAL_FEATURES__ADDITIONAL_DIALOGS("ExperimentalFeaturesAdditionalDialogs", false),

    GOVER__STORE_KEY("GoverStoreKey", false),

    NUTZERKONTEN__BUNDID("BundIDActive", true),
    NUTZERKONTEN__BAYERN_ID("BayernIDActive", true),
    NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID("SHActive", true),
    NUTZERKONTEN__MUK("MukActive", true),
    ;

    public static final String SYSTEM_CONFIG_TRUE = "true";

    private final String key;
    private final Boolean isPublic;

    SystemConfigKey(String key, Boolean isPublic) {
        this.key = key;
        this.isPublic = isPublic;
    }


    @Override
    public String getKey() {
        return key;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    @Override
    public boolean matches(Object other) {
        return other instanceof String && key.equals(other);
    }

    public static Optional<SystemConfigKey> fromString(String key) {
        for (SystemConfigKey k : SystemConfigKey.values()) {
            if (k.getKey().equals(key)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}