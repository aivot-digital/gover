export const EDITOR_DRAFT_RETURN_PARAM = 'editor_draft_return';
const RETURN_STORAGE_KEY = 'prosuna-editor-draft-login-return';
const LOGIN_PREPARATION_TIMEOUT = 2_000;

type LoginHandler = (returnId: string) => Promise<boolean>;
let activeEditor: {prepare: LoginHandler; stop?: () => Promise<void>} | undefined;
let confirmedReturnId: string | undefined;

/** The staff router mounts a single form or process-node editor at a time. */
export function registerEditorDraftLoginHandler(prepare: LoginHandler, stop?: () => Promise<void>): () => void {
    const editor = {prepare, stop};
    activeEditor = editor;
    return () => {
        if (activeEditor === editor) {
            activeEditor = undefined;
        }
    };
}

export async function stopEditorDraftForLogout(): Promise<void> {
    clearEditorDraftLoginReturn();
    await activeEditor?.stop?.();
}

export function clearEditorDraftLoginReturn(): void {
    confirmedReturnId = undefined;
    try {
        sessionStorage.removeItem(RETURN_STORAGE_KEY);
    } catch {
        // Browser storage can be disabled.
    }
}

/** Run before the staff router captures the initial location. Duplicated tabs can inherit sessionStorage. */
export function initializeEditorDraftLoginReturn(): void {
    const url = new URL(window.location.href);
    const id = url.searchParams.get(EDITOR_DRAFT_RETURN_PARAM);
    let matches = false;
    if (id != null) {
        url.searchParams.delete(EDITOR_DRAFT_RETURN_PARAM);
        window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
        try {
            matches = id !== '' && id === sessionStorage.getItem(RETURN_STORAGE_KEY);
        } catch {
            // Without the tab-local half of the return marker, only ordinary recovery is available.
        }
    }
    clearEditorDraftLoginReturn();
    if (matches && id != null) {
        confirmedReturnId = id;
    }
}

export function getEditorDraftReturnId(): string | undefined {
    return confirmedReturnId;
}

/** Flush the active editor only once the user starts reauthentication in this tab. */
export async function prepareEditorDraftLogin(): Promise<string | undefined> {
    const editor = activeEditor;
    if (!editor) {
        return undefined;
    }

    let id: string | undefined;
    try {
        const newId = crypto.randomUUID();
        sessionStorage.setItem(RETURN_STORAGE_KEY, newId);
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
        if (await Promise.race([editor.prepare(id ?? ''), timeout])) {
            return id;
        }
    } catch (err) {
        console.error(err);
    } finally {
        window.clearTimeout(timer);
    }
    clearEditorDraftLoginReturn();
    return undefined;
}
