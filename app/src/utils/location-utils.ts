export function getUrlWithoutQuery(): string {
    const urlPieces = [location.protocol, '//', location.host, location.pathname]
    return urlPieces.join('');
}
