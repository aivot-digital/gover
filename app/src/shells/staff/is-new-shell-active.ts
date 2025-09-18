export function isNewShellActive() {
    return localStorage.getItem('new-shell') != null;
}