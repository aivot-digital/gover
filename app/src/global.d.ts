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
    sentry: {
        dsn: string;
    };
};
