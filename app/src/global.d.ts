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
        primaryColor: string;
        secondaryColor: string;
        primaryColorDark: string | null;
        secondaryColorDark: string | null;
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
    moduleFlags: Array<'FORM' | 'PROCESS' | 'PORTAL'>,
    processNodeLimits: Record<string, number>;
};
