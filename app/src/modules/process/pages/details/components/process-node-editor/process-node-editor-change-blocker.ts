export const PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY = 'skipProcessNodeEditorChangeBlocker';

export function shouldSkipProcessNodeEditorChangeBlocker(state: unknown): boolean {
    if (typeof state !== 'object' || state == null) {
        return false;
    }

    return (state as Record<string, unknown>)[PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY] === true;
}
