import {EDITOR_DRAFT_RETURN_PARAM} from './auth-service';
import {
    broadcastCommittedEditorDraftLogout, getObservedEditorDraftLogoutId, readEditorDraftLogoutId,
    signalEditorDraftLogout, subscribeEditorDraftLogout,
} from './editor-draft-logout';
import {EditorDraftStorage} from './editor-draft-storage';

export {EDITOR_DRAFT_RETURN_PARAM};
const RETURN_STORAGE_KEY = 'prosuna-editor-draft-login-return';
const LOGIN_PREPARATION_TIMEOUT = 2_000;

type LoginHandler = (returnId: string) => Promise<boolean>;
let activeEditor: {prepare: LoginHandler; stop?: (logoutId?: string) => Promise<void>} | undefined;
let confirmedReturn: {id: string; logoutId: string | null | undefined} | undefined;
let observingLogout = false;
let preparing: {logoutId: string | null | undefined} | undefined;

function observeLogout() {
    if (observingLogout) return;
    observingLogout = true;
    subscribeEditorDraftLogout((id) => { void stopEditorDraftForLogout(id).catch(console.error); },
        (id) => EditorDraftStorage.isCurrentGeneration(id));
}

/** The staff router mounts a single form or process-node editor at a time. */
export function registerEditorDraftLoginHandler(prepare: LoginHandler, stop?: (logoutId?: string) => Promise<void>): () => void {
    observeLogout();
    const editor = {prepare, stop};
    activeEditor = editor;
    return () => {
        if (activeEditor === editor) {
            activeEditor = undefined;
        }
    };
}

export async function stopEditorDraftForLogout(logoutId?: string): Promise<void> {
    if (logoutId == null || preparing?.logoutId !== logoutId) preparing = undefined;
    if (logoutId == null || confirmedReturn?.logoutId !== logoutId) confirmedReturn = undefined;
    try {
        // An event queued in a background tab may arrive after its next explicit save and login reservation.
        if (logoutId == null || readReturnMarker()?.observedLogoutId !== logoutId) {
            sessionStorage.removeItem(RETURN_STORAGE_KEY);
        }
    } catch {
        // A missing or malformed marker cannot match a later callback.
    }
    await activeEditor?.stop?.(logoutId);
}

/** Only the document handling the deliberate logout clears the database. Receivers stop their old visit locally. */
export async function beginExplicitEditorDraftLogout(): Promise<void> {
    const id = typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
    const markerPublished = signalEditorDraftLogout(id);
    void stopEditorDraftForLogout(id).catch(console.error);
    await EditorDraftStorage.clear(id, markerPublished);
    if (!markerPublished) broadcastCommittedEditorDraftLogout(id);
}

export function clearEditorDraftLoginReturn(): void {
    preparing = undefined;
    confirmedReturn = undefined;
    try {
        sessionStorage.removeItem(RETURN_STORAGE_KEY);
    } catch {
        // Browser storage can be disabled.
    }
}

/** Run before the staff router captures the initial location. Duplicated tabs can inherit sessionStorage. */
export function initializeEditorDraftLoginReturn(): void {
    observeLogout();
    const url = new URL(window.location.href);
    const id = url.searchParams.get(EDITOR_DRAFT_RETURN_PARAM);
    const logoutId = getObservedEditorDraftLogoutId();
    let matches = false;
    if (id != null) {
        url.searchParams.delete(EDITOR_DRAFT_RETURN_PARAM);
        window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
        try {
            const marker = readReturnMarker();
            const durable = readEditorDraftLogoutId() ?? null;
            matches = id !== '' && marker?.id === id && marker.durableLogoutId === durable &&
                (logoutId === durable || logoutId == null || marker.observedLogoutId === logoutId);
        } catch {
            // Without the tab-local half of the return marker, only ordinary recovery is available.
        }
    }
    clearEditorDraftLoginReturn();
    if (matches && id != null) {
        confirmedReturn = {id, logoutId};
    }
}

export function getEditorDraftReturnId(): string | undefined {
    return confirmedReturn?.logoutId === getObservedEditorDraftLogoutId() ? confirmedReturn?.id : undefined;
}

function readReturnMarker(): {id: string; durableLogoutId: string | null; observedLogoutId?: string} | undefined {
    const raw = sessionStorage.getItem(RETURN_STORAGE_KEY);
    if (!raw) return undefined;
    // A pre-upgrade login stores only its UUID, before any explicit logout marker existed.
    return raw.startsWith('{') ? JSON.parse(raw) : {id: raw, durableLogoutId: null};
}

/** Flush the active editor only once the user starts reauthentication in this tab. */
export async function prepareEditorDraftLogin(): Promise<string | undefined> {
    const editor = activeEditor;
    if (!editor) {
        return undefined;
    }

    const logoutId = getObservedEditorDraftLogoutId();
    const attempt = {logoutId};
    preparing = attempt;
    let id: string | undefined;
    try {
        const newId = crypto.randomUUID();
        sessionStorage.setItem(RETURN_STORAGE_KEY, JSON.stringify({
            id: newId, durableLogoutId: readEditorDraftLogoutId() ?? null, observedLogoutId: logoutId,
        }));
        id = newId;
    } catch {
        // Still flush the editor if this browser cannot correlate a redirect.
    }

    let timer: number | undefined;
    try {
        // A blocked IndexedDB open must not strand the user on the expired-session dialog.
        // Carry the ID on timeout in case a pending transaction commits just before navigation.
        const timeout = new Promise<boolean>((resolve) => {
            timer = window.setTimeout(() => resolve(true), LOGIN_PREPARATION_TIMEOUT);
        });
        if (await Promise.race([editor.prepare(id ?? ''), timeout]) &&
            preparing === attempt && logoutId === getObservedEditorDraftLogoutId()) {
            return id;
        }
    } catch (err) {
        console.error(err);
    } finally {
        window.clearTimeout(timer);
    }
    if (preparing === attempt) clearEditorDraftLoginReturn();
    return undefined;
}
