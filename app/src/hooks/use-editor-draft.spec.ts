import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {useEditorDraft} from './use-editor-draft';
import {EditorDraftStorage} from '../services/editor-draft-storage';

vi.mock('./use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({user: {user: {id: 'user-1'}}}),
}));

describe('useEditorDraft', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.spyOn(EditorDraftStorage, 'save').mockResolvedValue(undefined);
        vi.spyOn(EditorDraftStorage, 'remove').mockResolvedValue(undefined);
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    const props = {
        draftKey: 'process-node/1/form-layout',
        base: {label: 'Alt'},
        baseLockVersion: 3,
        edited: {label: 'Neu'},
        hasChanged: true,
        enabled: true,
    };

    it('stores unsaved changes after a short delay, once per pause', () => {
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        rerender({...props, edited: {label: 'Neuer'}});

        vi.advanceTimersByTime(999);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();

        vi.advanceTimersByTime(1);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
        expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({
            userId: 'user-1',
            key: 'process-node/1/form-layout',
            base: {label: 'Alt'},
            baseLockVersion: 3,
            mine: {label: 'Neuer'},
        }));
    });

    it('removes the draft once there are no unsaved changes, e.g. after saving', () => {
        renderHook(() => useEditorDraft({...props, hasChanged: false}));

        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', 'process-node/1/form-layout');
        vi.advanceTimersByTime(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('leaves an existing draft alone until the editor has decided about it', () => {
        renderHook(() => useEditorDraft({...props, hasChanged: false, enabled: false}));
        vi.advanceTimersByTime(1000);

        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('cancels a pending write when the draft is discarded', async () => {
        const {result} = renderHook(() => useEditorDraft(props));

        await result.current.clearDraft();
        vi.advanceTimersByTime(1000);

        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', 'process-node/1/form-layout');
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });
});
