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

export function createStaffPath(path: string) {
    if (path.startsWith('/')) {
        path = path.slice(1);
    }
    return `${window.location.protocol}//${window.location.host}/staff/${path}`;
}

export function createCustomerPath(path: string) {
    if (path.startsWith('/')) {
        path = path.slice(1);
    }
    return `${window.location.protocol}//${window.location.host}/${path}`;
}
