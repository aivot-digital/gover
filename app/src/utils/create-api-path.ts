export function createApiPath(path: string) {
    let hostname = AppConfig.api.hostname;
    if (hostname.endsWith('/')) {
        hostname = hostname.slice(0, -1);
    }
    if (path.startsWith('/')) {
        path = path.slice(1);
    }
    return `${hostname}/${path}`;
}