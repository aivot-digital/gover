import {useCallback, useEffect, useRef} from 'react';
import {useAppSelector} from './use-app-selector';
import {selectUser} from '../slices/user-slice';
import {type EditorDraft, EditorDraftStorage} from '../services/editor-draft-storage';

// Short enough that hardly any work is lost, long enough not to write on every keystroke.
const DRAFT_SAVE_DELAY = 1000;

interface EditorDraftProps<T> {
    /** Identifies the edited object, e.g. `process-node/17/form-layout`. */
    draftKey: string | null;
    /** The stored state the editor started from. */
    base: T | null;
    baseLockVersion: number | null | undefined;
    edited: T | null;
    hasChanged: boolean;
    /**
     * Must stay false until the editor has decided about an existing draft. Otherwise the freshly loaded, unchanged
     * state would remove the draft before it could be offered.
     */
    enabled: boolean;
}

/**
 * Keeps the unsaved changes of an editor in the browser while they exist and removes them once they are saved or
 * reverted, so they can be offered again after an expired session or a reload.
 */
export function useEditorDraft<T>(props: EditorDraftProps<T>) {
    const {draftKey, base, baseLockVersion, edited, hasChanged, enabled} = props;
    const userId = useAppSelector(selectUser)?.id;
    const timeoutRef = useRef<number | undefined>(undefined);

    useEffect(() => {
        if (!enabled || draftKey == null || userId == null || base == null || edited == null) {
            return;
        }

        if (!hasChanged) {
            EditorDraftStorage.remove(userId, draftKey).catch(console.error);
            return;
        }

        timeoutRef.current = window.setTimeout(() => {
            EditorDraftStorage
                .save<T>({userId, key: draftKey, base, baseLockVersion: baseLockVersion ?? null, mine: edited, savedAt: Date.now()})
                .catch(console.error);
        }, DRAFT_SAVE_DELAY);

        return () => {
            window.clearTimeout(timeoutRef.current);
        };
    }, [enabled, draftKey, userId, base, baseLockVersion, edited, hasChanged]);

    const loadDraft = useCallback(async (): Promise<EditorDraft<T> | null> => {
        if (draftKey == null || userId == null) {
            return null;
        }
        try {
            return await EditorDraftStorage.load<T>(userId, draftKey);
        } catch (error) {
            // Without the storage, e.g. in a private window, the editor works as before, just without drafts.
            console.error(error);
            return null;
        }
    }, [draftKey, userId]);

    /** Removes the draft when the person discards the changes on purpose. */
    const clearDraft = useCallback(async (): Promise<void> => {
        window.clearTimeout(timeoutRef.current);
        if (draftKey == null || userId == null) {
            return;
        }
        await EditorDraftStorage.remove(userId, draftKey).catch(console.error);
    }, [draftKey, userId]);

    return {
        loadDraft,
        clearDraft,
        isReady: userId != null,
    };
}
