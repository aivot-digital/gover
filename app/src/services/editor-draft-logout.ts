const LOGOUT_KEY = 'prosuna-editor-drafts/explicit-logout';

// A denied Local Storage write can still be announced to the other open documents.
let broadcastMarker: {id: string; previous: string | null | undefined} | undefined;

export function readEditorDraftLogoutId(): string | null | undefined {
    try {
        return window.localStorage.getItem(LOGOUT_KEY);
    } catch {
        return undefined;
    }
}

export function getObservedEditorDraftLogoutId(): string | null | undefined {
    const stored = readEditorDraftLogoutId();
    return broadcastMarker && broadcastMarker.previous === stored ? broadcastMarker.id : stored;
}

/** The durable marker also lets the next working database connection finish an interrupted cleanup. */
export function signalEditorDraftLogout(id: string): boolean {
    try {
        window.localStorage.setItem(LOGOUT_KEY, id);
        broadcastMarker = undefined;
        return true;
    } catch {
        broadcastMarker = {id, previous: readEditorDraftLogoutId()};
        return false;
    }
}

/** A non-durable notification is sent only once IndexedDB can confirm or reject it for a delayed receiver. */
export function broadcastCommittedEditorDraftLogout(id: string): void {
    try {
        if (window.BroadcastChannel) {
            const channel = new window.BroadcastChannel(LOGOUT_KEY);
            channel.postMessage(id);
            channel.close();
        }
    } catch {
        // The database generation also protects tabs without a working BroadcastChannel.
    }
}

export function subscribeEditorDraftLogout(handler: (id: string) => void, isCommitted?: (id: string) => Promise<boolean>): () => void {
    const onStorage = (event: StorageEvent) => {
        if (event.key === LOGOUT_KEY && event.newValue != null && event.newValue === getObservedEditorDraftLogoutId()) {
            handler(event.newValue);
        }
    };
    window.addEventListener('storage', onStorage);
    let channel: BroadcastChannel | undefined;
    try {
        if (isCommitted && window.BroadcastChannel) {
            channel = new window.BroadcastChannel(LOGOUT_KEY);
            channel.onmessage = async ({data}) => {
                const previous = readEditorDraftLogoutId();
                if (typeof data === 'string' && await isCommitted(data).catch(() => false) &&
                    previous === readEditorDraftLogoutId()) {
                    broadcastMarker = {id: data, previous};
                    handler(data);
                }
            };
        }
    } catch {
        // The storage listener and transaction checks work independently of BroadcastChannel.
    }
    return () => {
        window.removeEventListener('storage', onStorage);
        channel?.close();
    };
}
