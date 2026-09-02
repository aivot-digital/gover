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
        logoKeyDark: string | null;
    };
    systemConfigs: {
        ProviderName: string;
        SystemTheme: string;
        'dashboard.activity.enabled'?: boolean;
        'dashboard.activity.period'?: string;
    };
    faviconUrl: string;
    logoUrl: string;
    apiHostname: string;
    registryHostname: string;
    supportUrl: string | null;
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
