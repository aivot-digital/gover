export const SystemConfigKeys = {
    provider: {
        name: 'ProviderName',
    },
    system: {
        theme: 'SystemTheme',
        logo: 'SystemLogo',
        favicon: 'SystemFavicon',
    },
    experimentalFeatures: {
        complexity: 'ExperimentalFeaturesComplexity',
        additionalDialogs: 'ExperimentalFeaturesAdditionalDialogs',
        pdfTemplates: 'ExperimentalFeaturesPdfTemplates',
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
