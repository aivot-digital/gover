import {createContext, useContext} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../services/auth-service';
import {useChangeBlocker} from './use-change-blocker-2';

const Visit = createContext('first');

function renderEditor() {
    const pending = Promise.withResolvers<void>();
    const discard = vi.fn((visit: string) => visit === 'first' ? pending.promise : Promise.resolve());
    vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);

    function Editor() {
        const visit = useContext(Visit);
        return useChangeBlocker({
            original: false, edited: true, visitKey: visit, onConfirmNavigation: () => discard(visit),
        }).dialog;
    }
    const router = createMemoryRouter([
        {path: '/', element: <Editor/>},
        {path: '/next', element: <div>Next page</div>},
    ]);
    const view = (visit: string) => <Visit.Provider value={visit}><RouterProvider router={router}/></Visit.Provider>;
    const {rerender, unmount} = render(view('first'), {reactStrictMode: true});
    const requestNavigation = async () => {
        await act(async () => { await router.navigate('/next'); });
        return screen.getByRole('button', {name: 'Änderungen verwerfen'});
    };
    return {router, discard, pending, requestNavigation, changeVisit: () => rerender(view('second')), unmount};
}

afterEach(() => vi.restoreAllMocks());

describe('useChangeBlocker editor visits', () => {
    it.each([false, true])('resets the previous navigation when the visit changes (discard already started: %s)', async (started) => {
        const editor = renderEditor();
        const confirm = await editor.requestNavigation();
        if (started) fireEvent.click(confirm);
        editor.changeVisit();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        // The old discard may complete while another confirmation is already open.
        const nextConfirm = await editor.requestNavigation();
        await act(async () => editor.pending.resolve());
        expect(editor.router.state.location.pathname).toBe('/');
        expect(nextConfirm).toBeEnabled();
        expect(editor.discard.mock.calls).toEqual(started ? [['first']] : []);
        fireEvent.click(nextConfirm);
        await screen.findByText('Next page');
        expect(editor.discard).toHaveBeenLastCalledWith('second');
    });

    it('ignores the completion of a discard after unmount', async () => {
        const editor = renderEditor();
        fireEvent.click(await editor.requestNavigation());
        editor.unmount();
        await act(async () => editor.pending.resolve());
        expect(editor.router.state.location.pathname).toBe('/');
    });
});
