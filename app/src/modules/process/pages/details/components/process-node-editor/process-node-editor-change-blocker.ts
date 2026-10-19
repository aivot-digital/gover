export const PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY = 'skipProcessNodeEditorChangeBlocker';

export function isProcessNodeEditorTabNavigation(currentPathname: string, nextPathname: string): boolean {
    const editorPath = /^(\/processes\/[^/]+\/versions\/[^/]+\/nodes\/[^/]+)(?:\/tabs\/[^/]+)?\/?$/;
    const current = editorPath.exec(currentPathname);
    const next = editorPath.exec(nextPathname);
    return current != null && next != null && current[1] === next[1];
}

export function shouldSkipProcessNodeEditorChangeBlocker(state: unknown): boolean {
    if (typeof state !== 'object' || state == null) {
        return false;
    }

    return (state as Record<string, unknown>)[PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY] === true;
}
