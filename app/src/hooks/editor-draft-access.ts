import {UsersApiService} from '../modules/users/users-api-service';
import {AuthService} from '../services/auth-service';
import {getObservedEditorDraftLogoutId} from '../services/editor-draft-logout';
import {EditorDraftStorage} from '../services/editor-draft-storage';

// A newly opened editor can still belong to a document whose Redux user predates a logout in another tab.
const documentLogoutId = getObservedEditorDraftLogoutId();
let documentGeneration: Promise<string | null> | undefined;
const MANUAL_SAVE_STORAGE_TIMEOUT = 2_000;

export function initializeEditorDraftDocumentGeneration(): void {
    // Capture this before authentication so even a missed announcement is visible to a later editor in this document.
    documentGeneration ??= EditorDraftStorage.openGeneration().catch(() => null);
}

export interface EditorDraftManualSave {
    signal: AbortSignal;
    generation: string | null;
    resumesDrafts: boolean;
}

/** A frozen writer can block even a read. Local recovery must not strand an authenticated server save. */
async function checkStorageForSave<T>(check: Promise<T>): Promise<T | null> {
    let timer: number | undefined;
    try {
        const timeout = new Promise<null>((resolve) => {
            timer = window.setTimeout(() => resolve(null), MANUAL_SAVE_STORAGE_TIMEOUT);
        });
        return await Promise.race([check, timeout]);
    } catch {
        return null;
    } finally {
        window.clearTimeout(timer);
    }
}

/** An editor visit keeps its storage generation across timers, queued work and retries. */
export function createEditorDraftAccess(userId: string, onLogout: () => void) {
    const recovery = new AbortController();
    let operations = new AbortController();
    let observedLogout = getObservedEditorDraftLogoutId();
    let suspended = false;
    let generation: Promise<string | null> | undefined;
    let boundGeneration: string | null = null;

    function revoke(id?: string) {
        if (id !== undefined && observedLogout === id) return false;
        if (id !== undefined) observedLogout = id;
        // A late fallback broadcast can announce the same generation a new visit or manual save already verified.
        if (id != null && boundGeneration === id && !suspended) return false;
        suspended = true;
        recovery.abort();
        operations.abort();
        operations = new AbortController();
        onLogout();
        return true;
    }

    function signal() {
        const marker = getObservedEditorDraftLogoutId();
        if (marker != null && marker !== observedLogout) revoke(marker);
        return operations.signal;
    }

    function active(expected: AbortSignal) {
        return signal() === expected && !suspended;
    }

    async function open() {
        try {
            return await EditorDraftStorage.openGeneration();
        } catch (error) {
            // An editor without IndexedDB can still save to the server.
            console.error(error);
            return null;
        }
    }

    async function sameSignedInAccount() {
        return AuthService.isAuthenticated() && (await new UsersApiService().retrieveSelf()).id === userId;
    }

    async function bind(expected: AbortSignal) {
        const candidate = await open();
        const atStartup = await documentGeneration;
        if (!active(expected)) return null;
        if (observedLogout !== documentLogoutId || (atStartup !== undefined && candidate !== atStartup)) {
            // A fresh visit in an old document must not save under a stale Redux identity.
            try {
                if (!await sameSignedInAccount()) {
                    if (active(expected)) revoke();
                    return null;
                }
            } catch {
                if (active(expected)) revoke();
                return null;
            }
        }
        if (!active(expected)) return null;
        boundGeneration = candidate;
        return candidate;
    }

    async function current(expected: AbortSignal) {
        if (!active(expected)) return null;
        generation ??= bind(expected);
        const value = await generation;
        return active(expected) ? value : null;
    }

    async function beginManualSave(preparation: AbortSignal): Promise<EditorDraftManualSave | null> {
        const click = signal();
        const validClick = () => !preparation.aborted && signal() === click;
        const resumesDrafts = suspended;
        let candidate = await checkStorageForSave(resumesDrafts ? open() : current(click));
        if (!validClick()) return null;
        if (resumesDrafts && (!await sameSignedInAccount() || !validClick())) return null;
        // When a tab cannot publish its localStorage marker, the database still records the logout.
        if (candidate != null) {
            const currentGeneration = await checkStorageForSave(EditorDraftStorage.isCurrentGeneration(candidate));
            if (!validClick()) return null;
            if (currentGeneration === false) {
                // This click did not start under a verified generation. A new click may resume the visit.
                if (!resumesDrafts) revoke();
                return null;
            }
            if (currentGeneration == null) candidate = null;
        }
        // With no database, verify the account even if this tab never received a logout signal.
        if (!resumesDrafts && candidate == null && !await sameSignedInAccount()) return null;
        return validClick() ? {signal: click, generation: candidate, resumesDrafts} : null;
    }

    async function confirmManualSave(token: EditorDraftManualSave): Promise<boolean> {
        if (!token.resumesDrafts || token.generation == null || signal() !== token.signal || !suspended) return false;
        if (!await checkStorageForSave(EditorDraftStorage.isCurrentGeneration(token.generation)) || signal() !== token.signal) return false;
        generation = Promise.resolve(token.generation);
        boundGeneration = token.generation;
        suspended = false;
        return true;
    }

    return {recoverySignal: recovery.signal, signal, active, revoke, current, beginManualSave, confirmManualSave};
}
