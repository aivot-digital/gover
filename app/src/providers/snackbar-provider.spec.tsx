import {createTheme, ThemeProvider} from '@mui/material';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SnackbarProvider, SnackbarTheme} from './snackbar-provider';

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

    it('displays a notification once with a customer page theme and closes it immediately', async () => {
        const theme = createTheme({palette: {background: {paper: '#123456'}}, typography: {fontFamily: 'serif'}});
        render(<SnackbarProvider><ThemeProvider theme={theme}><SnackbarTheme theme={theme}/><span>Formular</span></ThemeProvider></SnackbarProvider>);

        expect(await screen.findAllByText('Einmalige Meldung')).toHaveLength(1);
        expect(document.querySelectorAll('.notistack-SnackbarContainer')).toHaveLength(1);
        expect(screen.getByRole('alert')).toHaveStyle({backgroundColor: '#123456', fontFamily: 'serif'});
        fireEvent.click(screen.getByRole('button', {name: /close|schließen/i}));

        await waitFor(() => expect(screen.queryByText('Einmalige Meldung')).not.toBeInTheDocument());
        expect(state.dispatch).toHaveBeenCalledWith(expect.objectContaining({payload: 'notice'}));
    });

    it('updates a visible notification with the page theme and restores the application theme on leaving', async () => {
        const base = createTheme({palette: {background: {paper: '#eeeeee'}}});
        const page = createTheme({palette: {background: {paper: '#123456'}}});
        const view = render(<ThemeProvider theme={base}><SnackbarProvider><SnackbarTheme theme={page}/></SnackbarProvider></ThemeProvider>);
        expect(await screen.findByRole('alert')).toHaveStyle({backgroundColor: '#123456'});
        view.rerender(<ThemeProvider theme={base}><SnackbarProvider><span>Andere Seite</span></SnackbarProvider></ThemeProvider>);
        expect(screen.getByRole('alert')).toHaveStyle({backgroundColor: '#eeeeee'});
        expect(screen.getAllByText('Einmalige Meldung')).toHaveLength(1);
    });

    it('dismisses a notification with Escape', async () => {
        render(<SnackbarProvider><span>Formular</span></SnackbarProvider>);
        await screen.findByText('Einmalige Meldung');
        fireEvent.keyDown(document, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByText('Einmalige Meldung')).not.toBeInTheDocument());
    });
});
