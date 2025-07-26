declare var AppConfig: {
    oidc: {
        realm: string;
        client: string;
        hostname: string;
    };
    api: {
        hostname: string;
    };
    sentry: {
        dsn: string;
    };
};

declare var AppInfo: typeof AppInfo;