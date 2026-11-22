import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {useEditorDraft} from './use-editor-draft';
import {UsersApiService} from '../modules/users/users-api-service';
import {EditorDraftStorage} from '../services/editor-draft-storage';
import {prepareEditorDraftLogin, stopEditorDraftForLogout} from '../services/editor-draft-login';
import {signalEditorDraftLogout} from '../services/editor-draft-logout';
import {initializeEditorDraftDocumentGeneration} from './editor-draft-access';
import {AuthService} from '../services/auth-service';

vi.mock('./use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({user: {user: {id: 'user-1'}}}),
}));

describe('useEditorDraft', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        vi.spyOn(EditorDraftStorage, 'openGeneration').mockResolvedValue('first');
        vi.spyOn(EditorDraftStorage, 'isCurrentGeneration').mockResolvedValue(true);
        vi.spyOn(EditorDraftStorage, 'save').mockResolvedValue(undefined);
        vi.spyOn(EditorDraftStorage, 'remove').mockResolvedValue(undefined);
    });

    afterEach(() => {
        localStorage.clear();
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

    const signedInUser = {...new UsersApiService().initialize(), id: 'user-1'};
    function logout(id: string) {
        signalEditorDraftLogout(id);
        vi.mocked(EditorDraftStorage.openGeneration).mockResolvedValue(id);
        vi.mocked(EditorDraftStorage.isCurrentGeneration).mockImplementation(async (candidate) => candidate === id);
        return stopEditorDraftForLogout(id);
    }

    it('stores unsaved changes after a short delay, once per pause', async () => {
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        rerender({...props, edited: {label: 'Neuer'}});

        await vi.advanceTimersByTimeAsync(999);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();

        await vi.advanceTimersByTimeAsync(1);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
        expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({
            userId: 'user-1',
            key: 'process-node/1/form-layout',
            base: {label: 'Alt'},
            baseLockVersion: 3,
            mine: {label: 'Neuer'},
        }), 'first');
    });

    it('removes the draft once there are no unsaved changes, e.g. after saving', async () => {
        renderHook(() => useEditorDraft({...props, hasChanged: false}));

        await vi.advanceTimersByTimeAsync(0);
        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', 'process-node/1/form-layout', 'first');
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('removes a deferred conflict once the working copy is reverted to the confirmed state', async () => {
        const saveCheckpoint = {phase: 'confirmed' as const, submitted: props.edited};
        renderHook(() => useEditorDraft({...props, edited: props.base, hasChanged: false, saveCheckpoint}));
        await vi.advanceTimersByTimeAsync(0);
        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', props.draftKey, 'first');
        await vi.advanceTimersByTimeAsync(1000);
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
        }), 'first');
        expect(reserve).toHaveBeenCalledOnce();
    });

    it('leaves an existing draft alone until the editor has decided about it', async () => {
        renderHook(() => useEditorDraft({...props, hasChanged: false, enabled: false}));
        await vi.advanceTimersByTimeAsync(1000);

        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('cancels a pending write when the draft is discarded', async () => {
        const {result} = renderHook(() => useEditorDraft(props));

        await result.current.clearDraft();
        await vi.advanceTimersByTimeAsync(1000);

        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', 'process-node/1/form-layout', 'first');
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('flushes the latest edit and waits for the write to commit before reserving it for login', async () => {
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        await vi.advanceTimersByTimeAsync(1000);
        rerender({...props, edited: {label: 'Letzte Änderung'}});

        const redirect = prepareEditorDraftLogin();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
        expect(reserve).not.toHaveBeenCalled();
        commit.resolve();
        const id = await redirect;
        expect(id).toBeTruthy();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({mine: {label: 'Letzte Änderung'}}), 'first');
        expect(reserve).toHaveBeenCalledWith('user-1', props.draftKey, id, 'first');
    });

    it('reserves an existing draft while the editor is waiting for a restore decision', async () => {
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        renderHook(() => useEditorDraft({...props, enabled: false, hasChanged: false}));

        const id = await prepareEditorDraftLogin();
        expect(reserve).toHaveBeenCalledWith('user-1', props.draftKey, id, 'first');
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
        }), 'first');
        expect(reserve).toHaveBeenCalledTimes(2);
    });

    it('immediately replaces a discarded nested draft with the remaining form edits after an outstanding write', async () => {
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((context: {label: string} | undefined) => useEditorDraft({...props, context}), {
            initialProps: {label: 'Elementänderung'} as {label: string} | undefined,
        });
        await vi.advanceTimersByTimeAsync(1000);
        rerender(undefined);
        // An older write is in flight. Finishing it must also write the remaining form without waiting for a timer.
        commit.resolve();
        await vi.advanceTimersByTimeAsync(0);

        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({
            mine: props.edited, context: undefined,
        }), 'first');
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
    });

    it('drops the debounce on explicit logout and waits for an outstanding write before storage is cleared', async () => {
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        await vi.advanceTimersByTimeAsync(1000);
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

        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('protects an unread draft from cleanup, writes and login reservations until its next successful lookup', async () => {
        const lookup = Promise.withResolvers<null>();
        vi.spyOn(EditorDraftStorage, 'load').mockReturnValueOnce(lookup.promise).mockResolvedValue(null);
        const reserve = vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {
            initialProps: {...props, enabled: false},
        });

        const reading = result.current.loadDraft();
        // This cleanup is already queued behind the still-pending read.
        rerender({...props, hasChanged: false});
        lookup.reject(new Error('Transaction aborted'));
        await expect(reading).resolves.toBeNull();
        rerender(props);
        await vi.advanceTimersByTimeAsync(1000);
        await expect(prepareEditorDraftLogin()).resolves.toBeUndefined();
        expect(EditorDraftStorage.remove).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        expect(reserve).not.toHaveBeenCalled();

        await result.current.loadDraft();
        rerender({...props, edited: {label: 'Nach erneutem Laden'}});
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(1);
        rerender({...props, hasChanged: false});
        await vi.advanceTimersByTimeAsync(0);
        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', props.draftKey, 'first');
    });

    it('keeps an explicit discard available after a failed draft lookup', async () => {
        vi.spyOn(EditorDraftStorage, 'load').mockRejectedValue(new Error('Transaction aborted'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const {result} = renderHook(() => useEditorDraft({...props, enabled: false}));
        await result.current.loadDraft();
        await result.current.discardForNavigation();
        expect(EditorDraftStorage.remove).toHaveBeenCalledWith('user-1', props.draftKey, 'first');
    });

    it('stores the changes of an object that was never saved, whose stored state is empty', async () => {
        renderHook(() => useEditorDraft<{label: string} | null>({...props, base: null, baseLockVersion: 0}));
        await vi.advanceTimersByTimeAsync(1000);

        expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({base: null, mine: {label: 'Neu'}}), 'first');
    });

    it('drops a late read and a queued write after a logout and rejects the old save confirmation', async () => {
        const lookup = Promise.withResolvers<Awaited<ReturnType<typeof EditorDraftStorage.load>>>();
        vi.spyOn(EditorDraftStorage, 'load').mockReturnValueOnce(lookup.promise);
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        const click = await result.current.beginManualSave();
        const restore = result.current.loadDraft();
        await vi.advanceTimersByTimeAsync(1000);
        const stopped = logout('second');
        expect(result.current.draftLogoutSignal?.aborted).toBe(true);
        expect(click?.signal.aborted).toBe(true);
        lookup.resolve({...props, userId: 'user-1', key: props.draftKey, tabId: 'other', mine: props.edited, savedAt: 1});
        expect(await restore).toBeNull();
        await stopped;
        await result.current.confirmManualSave(click!);
        rerender({...props, edited: {label: 'Nur im Speicher'}});
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        await expect(prepareEditorDraftLogin()).resolves.toBeUndefined();
    });

    it('starts a fresh generation only after the same account confirms a new manual save', async () => {
        const self = vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue({...signedInUser, id: 'other-user'});
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        await logout('second');
        expect(await result.current.beginManualSave()).toBeNull();
        self.mockResolvedValue(signedInUser);
        const aborted = await result.current.beginManualSave();
        await logout('third');
        await result.current.confirmManualSave(aborted!);
        const click = await result.current.beginManualSave();
        expect(click).toMatchObject({resumesDrafts: true, generation: 'third'});
        rerender({...props, edited: {label: 'Während des Speicherns'}});
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();

        rerender({...props, base: props.edited, baseLockVersion: 4, edited: {label: 'Während des Speicherns'}});
        await act(() => result.current.confirmManualSave(click!));
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
            base: props.edited, baseLockVersion: 4, mine: {label: 'Während des Speicherns'},
        }), 'third');
    });

    it('keeps an untouched old panel in memory after a save, including when its server base changes', async () => {
        vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue(signedInUser);
        const initial = {...props, context: {mine: 'Feld', base: 'alt'}, hasChangesWithoutContext: false, contextRevision: 1};
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: initial});
        await logout('second');
        const click = await result.current.beginManualSave();
        const rebased = {...initial, context: {...initial.context, base: 'neu'}};
        rerender(rebased);
        await act(() => result.current.confirmManualSave(click!));
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        rerender({...rebased, hasChangesWithoutContext: true});
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({context: undefined}), 'second');
        const changed = {...rebased.context, mine: 'Nach der Abmeldung bearbeitet'};
        rerender({...rebased, context: changed, contextRevision: 2});
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({context: changed}), 'second');
    });

    it('checks the current account before opening another editor in a document that predates logout', async () => {
        await logout('second');
        const self = vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue({...signedInUser, id: 'other-user'});
        const load = vi.spyOn(EditorDraftStorage, 'load').mockResolvedValue(null);
        const first = renderHook(() => useEditorDraft(props));
        expect(await first.result.current.loadDraft()).toBeNull();
        expect(load).not.toHaveBeenCalled();
        expect(await first.result.current.beginManualSave()).toBeNull();
        expect(self).toHaveBeenCalledTimes(2);
        first.unmount();

        self.mockResolvedValue(signedInUser);
        const second = renderHook(() => useEditorDraft(props));
        await second.result.current.loadDraft();
        expect(load).toHaveBeenCalledExactlyOnceWith('user-1', props.draftKey, undefined, 'second');
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledOnce();
    });

    it('identifies a logged-out document even if no logout notification reached it', async () => {
        initializeEditorDraftDocumentGeneration();
        vi.mocked(EditorDraftStorage.openGeneration).mockResolvedValue('changed without an announcement');
        vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue({...signedInUser, id: 'other-user'});
        const load = vi.spyOn(EditorDraftStorage, 'load').mockResolvedValue(null);
        const {result} = renderHook(() => useEditorDraft(props));
        await expect(result.current.loadDraft()).resolves.toBeNull();
        await expect(result.current.beginManualSave()).resolves.toBeNull();
        await vi.advanceTimersByTimeAsync(1000);
        expect(load).not.toHaveBeenCalled();
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('cancels a save still verifying its account when preparing a login redirect', async () => {
        const identity = Promise.withResolvers<typeof signedInUser>();
        vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockReturnValue(identity.promise);
        const {result} = renderHook(() => useEditorDraft(props));
        await logout('second');
        const starting = result.current.beginManualSave();
        await vi.advanceTimersByTimeAsync(0);
        expect(UsersApiService.prototype.retrieveSelf).toHaveBeenCalledOnce();
        const login = prepareEditorDraftLogin();
        expect((await starting)?.signal.aborted).toBe(true);
        identity.resolve(signedInUser);
        expect(await login).toBeUndefined();
    });

    it('validates an outstanding restore decision against the database even without a logout signal', async () => {
        const {result} = renderHook(() => useEditorDraft({...props, enabled: false}));
        expect(await result.current.isDraftRecoveryCurrent()).toBe(true);
        vi.mocked(EditorDraftStorage.isCurrentGeneration).mockResolvedValue(false);
        expect(await result.current.isDraftRecoveryCurrent()).toBe(false);
        expect(result.current.draftLogoutSignal?.aborted).toBe(true);
    });

    it('keeps manual saves available without IndexedDB but never refreshes a logged-out visit to resume it', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.mocked(EditorDraftStorage.openGeneration).mockRejectedValue(new Error('IndexedDB unavailable'));
        const self = vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue(signedInUser);
        const {result} = renderHook(() => useEditorDraft(props));
        expect(await result.current.beginManualSave()).toMatchObject({generation: null, resumesDrafts: false});
        await logout('again');
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(false);
        self.mockClear();
        expect(await result.current.beginManualSave()).toBeNull();
        expect(self).not.toHaveBeenCalled();
    });

    it('permits a verified server save with blocked IndexedDB and never resumes from a timed-out confirmation', async () => {
        const self = vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue(signedInUser);
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        const checking = Promise.withResolvers<boolean>();
        vi.mocked(EditorDraftStorage.isCurrentGeneration).mockReturnValueOnce(checking.promise);
        const starting = result.current.beginManualSave();
        await vi.advanceTimersByTimeAsync(2_001);
        expect(await starting).toMatchObject({generation: null, resumesDrafts: false});
        expect(self).toHaveBeenCalledOnce();
        checking.resolve(true);

        await logout('second');
        vi.mocked(EditorDraftStorage.save).mockClear();
        const confirmed = await result.current.beginManualSave();
        expect(confirmed).toMatchObject({generation: 'second', resumesDrafts: true});
        const late = Promise.withResolvers<boolean>();
        vi.mocked(EditorDraftStorage.isCurrentGeneration).mockReturnValueOnce(late.promise);
        const confirming = result.current.confirmManualSave(confirmed!);
        await vi.advanceTimersByTimeAsync(2_001);
        await confirming;
        late.resolve(true);
        rerender({...props, edited: {label: 'Kein neuer Entwurf'}});
        await vi.advanceTimersByTimeAsync(1_000);
        expect(EditorDraftStorage.save).not.toHaveBeenCalled();
    });

    it('retains a resumed visit when its logout announcement arrives after the database check', async () => {
        vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue(signedInUser);
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        await stopEditorDraftForLogout();
        vi.mocked(EditorDraftStorage.openGeneration).mockResolvedValue('second');
        const click = await result.current.beginManualSave();
        await act(() => result.current.confirmManualSave(click!));
        await logout('second');
        rerender({...props, edited: {label: 'Neue Änderung'}});
        await vi.advanceTimersByTimeAsync(1_000);
        expect(EditorDraftStorage.save).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({mine: {label: 'Neue Änderung'}}), 'second');
    });

    it('backs up the current edits immediately, including a snapshot scheduled while the previous one is written', async () => {
        const commit = Promise.withResolvers<void>();
        vi.mocked(EditorDraftStorage.save).mockReturnValueOnce(commit.promise);
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});

        const backup = result.current.backUpDraft();
        rerender({...props, edited: {label: 'Nach dem Fehlschlag'}});
        commit.resolve();

        await expect(backup).resolves.toBe(true);
        expect(EditorDraftStorage.save).toHaveBeenCalledTimes(2);
        expect(EditorDraftStorage.save).toHaveBeenLastCalledWith(expect.objectContaining({mine: {label: 'Nach dem Fehlschlag'}}), 'first');
    });

    it('reports no backup when the browser cannot store the edits or there are no unsaved changes', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.mocked(EditorDraftStorage.save).mockRejectedValueOnce(new Error('Quota exceeded'));
        const {result, rerender} = renderHook((current) => useEditorDraft(current), {initialProps: props});
        await expect(result.current.backUpDraft()).resolves.toBe(false);

        await expect(result.current.backUpDraft()).resolves.toBe(false);
        rerender({...props, edited: props.base, hasChanged: false});
        await expect(result.current.backUpDraft()).resolves.toBe(false);
        expect(EditorDraftStorage.remove).toHaveBeenCalled();
    });

    it('reports no backup once a logout stopped storing the edits of this visit', async () => {
        const {result} = renderHook(() => useEditorDraft(props));
        await vi.advanceTimersByTimeAsync(1000);
        expect(EditorDraftStorage.save).toHaveBeenCalledOnce();

        await logout('second');
        await expect(result.current.backUpDraft()).resolves.toBe(false);
    });
});
