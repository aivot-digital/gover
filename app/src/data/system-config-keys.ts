export const SystemConfigKeys = {
    provider: {
        name: 'ProviderName',
        listingPage: {
            imprintDepartmentId: 'ProviderListingPageImprintDepartmentId',
            privacyDepartmentId: 'ProviderListingPagePrivacyDepartmentId',
            accessibilityDepartmentId: 'ProviderListingPageAccessibilityDepartmentId',
            customListingPageLink: 'ProviderListingPageCustomLink',
            disableListingPageLink: 'ProviderListingPageDisableLink',
            disableGoverListingPage: 'ProviderListingPageDisablePublicListingPage',
        }
    },
    system: {
        theme: 'SystemTheme',
        logo: 'SystemLogo',
        favicon: 'SystemFavicon',
    },
    experimentalFeatures: {
        complexity: 'ExperimentalFeaturesComplexity',
        newCodeEditors: 'ExperimentalFeaturesNewCodeEditors',
    },
    gover: {
        storeKey: 'GoverStoreKey',
    },
    nutzerkonten: {
        bundid: 'BundIDActive',
        bayernId: 'BayernIDActive',
        schleswigHolsteinId: 'SHActive',
        muk: 'MukActive',
    },
};

/*
 * @deprecated Is now handled by the backend
 */
export const SystemConfigPublic = {
    [SystemConfigKeys.provider.name]: true,

    [SystemConfigKeys.system.theme]: true,
    [SystemConfigKeys.system.logo]: true,
    [SystemConfigKeys.system.favicon]: true,

    [SystemConfigKeys.experimentalFeatures.complexity]: false,

    [SystemConfigKeys.gover.storeKey]: false,

    [SystemConfigKeys.nutzerkonten.bundid]: true,
    [SystemConfigKeys.nutzerkonten.bayernId]: true,
    [SystemConfigKeys.nutzerkonten.schleswigHolsteinId]: true,
    [SystemConfigKeys.nutzerkonten.muk]: true,
};
