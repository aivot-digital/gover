declare var AppConfig: {
    knownFileExtensions: {
        name: string;
        mime: string;
        extensions: string[];
    }[];
    providerName: string;
    systemTheme: {
        id: number;
        name: string;
        main: string;
        mainDark: string;
        accent: string;
        error: string;
        warning: string;
        info: string;
        success: string;
        faviconKey: string | null;
        logoKey: string | null;
    };
    systemConfigs: {
        ProviderName: string;
        SystemTheme: string;
    };
    faviconUrl: string;
    logoUrl: string;
    apiHostname: string;
    registryHostname: string;
    applicationTimeZone: string;
    departmentLevelLabels: string[];
    sentryDsn: string;
    oidc: {
        hostname: string;
        realm: string;
        clientId: string;
    },
};
