export const SystemConfigKeys = {
    provider: {
        name: 'ProviderName',
    },
    system: {
        theme: 'SystemTheme',
        logo: 'SystemLogo',
        favicon: 'SystemFavicon',
    },
    gover: {
        storeKey: 'GoverStoreKey',
    },
};

export const SystemConfigPublic = {
    [SystemConfigKeys.provider.name]: true,
    [SystemConfigKeys.system.theme]: true,
    [SystemConfigKeys.system.logo]: true,
    [SystemConfigKeys.system.favicon]: true,
    [SystemConfigKeys.gover.storeKey]: false,
};