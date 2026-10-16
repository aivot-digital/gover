import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SnackbarProvider} from './snackbar-provider';

const state = vi.hoisted(() => ({
    dispatch: vi.fn(),
    shell: {
        minimizeDrawer: false,
        snackbars: [{key: 'notice', message: 'Einmalige Meldung', severity: 'info', type: 'dismissable'}],
    },
}));

vi.mock('../hooks/use-app-selector', () => ({useAppSelector: (selector: (value: typeof state) => unknown) => selector(state)}));
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => state.dispatch}));
vi.mock('../app-info', () => ({AppInfo: {mode: 'customer'}}));

describe('SnackbarProvider', () => {
    beforeEach(() => state.dispatch.mockClear());

    it('displays a notification once across nested page providers and closes it immediately', async () => {
        render(<SnackbarProvider><SnackbarProvider><span>Formular</span></SnackbarProvider></SnackbarProvider>);

        expect(await screen.findAllByText('Einmalige Meldung')).toHaveLength(1);
        expect(document.querySelectorAll('.notistack-SnackbarContainer')).toHaveLength(1);
        fireEvent.click(screen.getByRole('button', {name: /close|schließen/i}));

        await waitFor(() => expect(screen.queryByText('Einmalige Meldung')).not.toBeInTheDocument());
        expect(state.dispatch).toHaveBeenCalledWith(expect.objectContaining({payload: 'notice'}));
    });

    it('dismisses a notification with Escape', async () => {
        render(<SnackbarProvider><span>Formular</span></SnackbarProvider>);
        await screen.findByText('Einmalige Meldung');
        fireEvent.keyDown(document, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByText('Einmalige Meldung')).not.toBeInTheDocument());
    });
});
