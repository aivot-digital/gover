import {afterEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from './auth-service';
import {EditorDraftStorage} from './editor-draft-storage';
import {getObservedEditorDraftLogoutId, readEditorDraftLogoutId, signalEditorDraftLogout, subscribeEditorDraftLogout} from './editor-draft-logout';
import {
    beginExplicitEditorDraftLogout,
    clearEditorDraftLoginReturn,
    EDITOR_DRAFT_RETURN_PARAM,
    getEditorDraftReturnId,
    initializeEditorDraftLoginReturn,
    prepareEditorDraftLogin,
    registerEditorDraftLoginHandler,
    stopEditorDraftForLogout,
} from './editor-draft-login';

describe('editor draft login return', () => {
    let unregister: (() => void) | undefined;

    afterEach(() => {
        unregister?.();
        unregister = undefined;
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
        vi.useRealTimers();
        clearEditorDraftLoginReturn();
        sessionStorage.clear();
        localStorage.clear();
        window.history.replaceState(null, '', '/');
    });

    it('carries only the return ID to the backend callback and removes it before routing', async () => {
        window.history.replaceState(null, '', '/staff/editor/42?tab=details&logout=true#field-3');
        const prepare = vi.fn(async () => true);
        unregister = registerEditorDraftLoginHandler(prepare);
        const id = await prepareEditorDraftLogin();
        expect(id).toBeTruthy();
        expect(prepare).toHaveBeenCalledWith(id);

        const location = {pathname: '/editor/42', search: '?tab=details', hash: '', state: null, key: 'key'};
        const login = new URL(AuthService.getLoginUrl(location, id), window.location.origin);
        const target = new URL(login.searchParams.get('app_uri')!, window.location.origin);
        expect(target.pathname).toBe('/staff/editor/42');
        expect(Object.fromEntries(target.searchParams)).toEqual({tab: 'details', [EDITOR_DRAFT_RETURN_PARAM]: id});
        expect(target.hash).toBe('#field-3');

        window.history.replaceState({idx: 2}, '', target);
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBe(id);
        expect(window.location.pathname + window.location.search + window.location.hash).toBe('/staff/editor/42?tab=details#field-3');
        expect(window.history.state).toEqual({idx: 2});
        // Replaying the callback URL no longer has the matching tab-local marker.
        window.history.replaceState(null, '', target);
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBeUndefined();
    });

    it.each(['', '?editor_draft_return=different'])('ignores an inherited marker without its matching callback (%s)', async (search) => {
        unregister = registerEditorDraftLoginHandler(async () => true);
        const id = await prepareEditorDraftLogin();
        window.history.replaceState(null, '', '/staff/editor' + search);
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBeUndefined();
        expect(window.location.search).toBe('');

        window.history.replaceState(null, '', '/staff/editor?' + new URLSearchParams({[EDITOR_DRAFT_RETURN_PARAM]: id!}));
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBeUndefined();
    });

    it('starts an ordinary login if there is no eligible draft', async () => {
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        unregister = registerEditorDraftLoginHandler(async () => false);
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        expect(sessionStorage.length).toBe(0);
    });

    it('still flushes and builds a login URL with browser storage disabled', async () => {
        vi.stubGlobal('sessionStorage', {
            setItem: () => { throw new DOMException('Denied', 'SecurityError'); },
            removeItem: vi.fn(),
        });
        const prepare = vi.fn(async () => false);
        unregister = registerEditorDraftLoginHandler(prepare);
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        expect(prepare).toHaveBeenCalledWith('');
        const url = AuthService.getLoginUrl({pathname: '/editor/42', search: '', hash: '', state: null, key: 'key'});
        expect(new URL(url, window.location.origin).searchParams.get('app_uri')).toBe('/staff/editor/42');
    });

    it('allows login when IndexedDB is stalled and correlates a possible late commit', async () => {
        vi.useFakeTimers();
        const prepare = vi.fn((_id: string) => new Promise<boolean>(() => {}));
        unregister = registerEditorDraftLoginHandler(prepare);
        const pending = prepareEditorDraftLogin();
        await vi.runAllTimersAsync();
        expect(await pending).toBe(prepare.mock.calls[0][0]);
    });

    it('invalidates a pending return on explicit logout and stops the current editor', async () => {
        const stop = vi.fn(async () => {});
        unregister = registerEditorDraftLoginHandler(async () => true, stop);
        const id = await prepareEditorDraftLogin();
        await stopEditorDraftForLogout();
        expect(stop).toHaveBeenCalledOnce();
        window.history.replaceState(null, '', '/staff/editor?' + new URLSearchParams({[EDITOR_DRAFT_RETURN_PARAM]: id!}));
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBeUndefined();
    });

    it('starts global cleanup even while a local write is stalled and invalidates the old login preparation', async () => {
        const reservation = Promise.withResolvers<boolean>();
        const write = Promise.withResolvers<void>();
        const stop = vi.fn(() => write.promise);
        unregister = registerEditorDraftLoginHandler(() => reservation.promise, stop);
        const login = prepareEditorDraftLogin();
        const clear = vi.spyOn(EditorDraftStorage, 'clear').mockResolvedValue();
        await beginExplicitEditorDraftLogout();
        const logoutId = readEditorDraftLogoutId();
        expect(logoutId).toBeTruthy();
        expect(stop).toHaveBeenCalledWith(logoutId);
        expect(clear).toHaveBeenCalledExactlyOnceWith(logoutId, true);
        reservation.resolve(true);
        expect(await login).toBeUndefined();
        write.resolve();
    });

    it('rejects a pre-logout callback even if this tab missed the event, and keeps a new reservation on delayed delivery', async () => {
        unregister = registerEditorDraftLoginHandler(async () => true);
        const previous = await prepareEditorDraftLogin();
        signalEditorDraftLogout('explicit');
        window.history.replaceState(null, '', '/staff/editor?editor_draft_return=' + previous);
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBeUndefined();

        const fresh = await prepareEditorDraftLogin();
        await stopEditorDraftForLogout('explicit');
        window.history.replaceState(null, '', '/staff/editor?editor_draft_return=' + fresh);
        initializeEditorDraftLoginReturn();
        expect(getEditorDraftReturnId()).toBe(fresh);
    });

    it('ignores a delayed storage event superseded by a logout that used the database fallback', () => {
        const setItem = vi.spyOn(window.localStorage, 'setItem');
        signalEditorDraftLogout('persisted');
        const [key] = setItem.mock.calls[0];
        const observed = vi.fn();
        const unsubscribe = subscribeEditorDraftLogout(observed);
        try {
            setItem.mockImplementation(() => { throw new DOMException('Storage full', 'QuotaExceededError'); });
            expect(signalEditorDraftLogout('later')).toBe(false);
            window.dispatchEvent(new StorageEvent('storage', {key, newValue: 'persisted'}));
            expect(observed).not.toHaveBeenCalled();
            expect(getObservedEditorDraftLogoutId()).toBe('later');
        } finally {
            unsubscribe();
        }
    });
});
