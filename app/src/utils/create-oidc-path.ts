export function createOidcPath(path: string) {
    let hostname = AppConfig.oidc.hostname;
    if (hostname.endsWith('/')) {
        hostname = hostname.slice(0, -1);
    }
    if (path.startsWith('/')) {
        path = path.slice(1);
    }
    return `${hostname}/${path}`;
}