import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {EditorDraftStorage} from '../services/editor-draft-storage';
import {useEditorDraft} from './use-editor-draft';
import {useEditorDraftVisit} from './use-editor-draft-visit';

const account = vi.hoisted(() => ({id: 'first'}));
vi.mock('./use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({user: {user: account}}),
}));

afterEach(() => vi.restoreAllMocks());

describe('useEditorDraftVisit', () => {
    it('invalidates old callbacks across repeated visits, a missing key and unmount in Strict Mode', () => {
        const {result, rerender, unmount} = renderHook((key: string | null) => useEditorDraftVisit(key), {
            initialProps: 'node/1' as string | null, reactStrictMode: true,
        });
        expect(result.current.isCurrent()).toBe(true);
        expect(result.current.canEdit()).toBe(false);
        act(() => result.current.finish());
        const first = result.current;
        expect(first.canEdit()).toBe(true);

        rerender('node/2');
        const second = result.current;
        expect(first.canEdit()).toBe(false);
        rerender('node/1');
        act(() => { first.finish(); second.finish(); });
        expect(result.current.checked).toBe(false);
        expect(first.isCurrent()).toBe(false);
        act(() => result.current.finish());
        const revisited = result.current;
        expect(revisited.canEdit()).toBe(true);

        rerender(null);
        expect(revisited.canEdit()).toBe(false);
        act(() => result.current.finish());
        expect(result.current.checked).toBe(false);
        rerender('node/1');
        expect(result.current.checked).toBe(false);
        act(() => result.current.finish());
        const last = result.current;
        unmount();
        expect(last.canEdit()).toBe(false);
    });

    it('keeps the new account’s existing draft intact until that account finishes its own check', async () => {
        vi.spyOn(EditorDraftStorage, 'openGeneration').mockResolvedValue('first-generation');
        const oldRead = Promise.withResolvers<null>();
        vi.spyOn(EditorDraftStorage, 'load').mockReturnValueOnce(oldRead.promise).mockResolvedValue(null);
        const remove = vi.spyOn(EditorDraftStorage, 'remove').mockResolvedValue(undefined);
        const {result, rerender} = renderHook((userId: string) => {
            account.id = userId;
            const visit = useEditorDraftVisit(userId + '/node/1');
            const storage = useEditorDraft({
                draftKey: 'node/1', base: 'stored', edited: 'stored', baseLockVersion: 1,
                hasChanged: false, enabled: visit.checked,
            });
            return {visit, storage};
        }, {initialProps: 'first', reactStrictMode: true});
        const first = result.current;
        const oldCheck = first.storage.loadDraft().then(first.visit.finish);
        rerender('second');
        await act(async () => {
            oldRead.resolve(null);
            await oldCheck;
        });
        expect(result.current.visit.checked).toBe(false);
        expect(remove).not.toHaveBeenCalled();

        const second = result.current;
        await act(async () => {
            await second.storage.loadDraft();
            second.visit.finish();
        });
        expect(result.current.visit.canEdit()).toBe(true);
        await act(async () => {});
        expect(remove.mock.calls).toEqual([['second', 'node/1', 'first-generation']]);
    });
});
