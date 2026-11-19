import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {useEditorDraft} from './use-editor-draft';
import {EditorDraftStorage} from '../services/editor-draft-storage';
import {prepareEditorDraftLogin, stopEditorDraftForLogout} from '../services/editor-draft-login';

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

    it('removes a deferred conflict once the working copy is reverted to the confirmed state', () => {
        const saveCheckpoint = {phase: 'confirmed' as const, submitted: props.edited};
        renderHook(() => useEditorDraft({...props, edited: props.base, hasChanged: false, saveCheckpoint}));
        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', props.draftKey);
        vi.advanceTimersByTime(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('flushes the submitted snapshot and the active working copy when a value is reverted during the PUT', async () => {
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        const saveCheckpoint = {phase: 'pending' as const, submitted: props.edited};
        renderHook(() => useEditorDraft({...props, edited: props.base, hasChanged: false, saveCheckpoint}));
        await prepareEditorDraftLogin();
        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({
            base: props.base, mine: props.base, saveCheckpoint,
        }));
        expect(reserve).toHaveBeenCalledOnce();
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

    it('flushes the latest edit and waits for the write to commit before reserving it for login', async () => {
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        vi.advanceTimersByTime(1000);
        rerender({...props, edited: {label: 'Letzte Änderung'}});

        const redirect = prepareEditorDraftLogin();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
        expect(reserve).not.toHaveBeenCalled();
        commit.resolve();
        const id = await redirect;
        expect(id).toBeTruthy();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({mine: {label: 'Letzte Änderung'}}));
        expect(reserve).toHaveBeenCalledWith('user-1', props.draftKey, id);
    });

    it('reserves an existing draft while the editor is waiting for a restore decision', async () => {
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        renderHook(() => useEditorDraft({...props, enabled: false, hasChanged: false}));

        const id = await prepareEditorDraftLogin();
        expect(reserve).toHaveBeenCalledWith('user-1', props.draftKey, id);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
    });

    it('flushes edits that arrive while a login reservation is pending, even with an unchanged form', async () => {
        const reservation = Promise.withResolvers<boolean>();
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockReturnValueOnce(reservation.promise).mockResolvedValue(true);
        const form = {...props, edited: props.base};
        const {rerender} = renderHook((label: string) => useEditorDraft({...form, context: {label}}), {
            initialProps: 'Erste Eingabe',
        });
        const redirect = prepareEditorDraftLogin();
        await vi.advanceTimersByTimeAsync(0);
        expect(reserve).toHaveBeenCalledOnce();
        rerender('Letzte Eingabe');
        reservation.resolve(true);

        expect(await redirect).toBeTruthy();
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({
            base: form.base, mine: form.base, context: {label: 'Letzte Eingabe'},
        }));
        expect(reserve).toHaveBeenCalledTimes(2);
    });

    it('immediately replaces a discarded nested draft with the remaining form edits after an outstanding write', async () => {
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((context: {label: string} | undefined) => useEditorDraft({...props, context}), {
            initialProps: {label: 'Elementänderung'} as {label: string} | undefined,
        });
        vi.advanceTimersByTime(1000);
        rerender(undefined);
        // An older write is in flight. Finishing it must also write the remaining form without waiting for a timer.
        commit.resolve();
        await vi.advanceTimersByTimeAsync(0);

        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({
            mine: props.edited, context: undefined,
        }));
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
    });

    it('drops the debounce on explicit logout and waits for an outstanding write before storage is cleared', async () => {
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        vi.advanceTimersByTime(1000);
        rerender({...props, edited: {label: 'Letzte Änderung'}});

        const cleared = vi.fn();
        const logout = stopEditorDraftForLogout().then(cleared);
        await vi.advanceTimersByTimeAsync(1000);
        expect(cleared).not.toHaveBeenCalled();
        commit.resolve();
        await logout;
        expect(cleared).toHaveBeenCalledOnce();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
    });

    it('allows discarding in-memory edits when draft storage is unavailable', async () => {
        vi.mocked(EditorDraftStorage.remove).mockRejectedValue(new Error('Storage unavailable'));
        const {result} = renderHook(() => useEditorDraft(props));
        await expect(result.current.discardForNavigation()).resolves.toBeUndefined();

        vi.advanceTimersByTime(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('stores the changes of an object that was never saved, whose stored state is empty', () => {
        renderHook(() => useEditorDraft<{label: string} | null>({...props, base: null, baseLockVersion: 0}));
        vi.advanceTimersByTime(1000);

        expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({base: null, mine: {label: 'Neu'}}));
    });
});
