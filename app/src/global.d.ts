declare var AppConfig: {
    oidc: {
        realm: string;
        client: string;
        hostname: string;
        idp_hint: string;
    };
    api: {
        hostname: string;
    };
    registry: {
        url: string;
    },
    sentry: {
        dsn: string;
    };
};

declare var AppConfigV2: {
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
    sentryDsn: string;
};