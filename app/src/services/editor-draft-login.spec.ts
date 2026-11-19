import {afterEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from './auth-service';
import {
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
});
