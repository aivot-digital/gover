import {fireEvent, render, screen} from '@testing-library/react';
import {Provider as StoreProvider} from 'react-redux';
import {useTheme} from '@mui/material';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AppProvider} from './app-provider';
import {useColorMode} from './color-mode-context';
import {store} from '../store.customer';

function ColorModeProbe() {
    const theme = useTheme();
    const {preference, setPreference} = useColorMode();

    return (
        <>
            <span>{preference}:{theme.palette.mode}</span>
            <button onClick={() => setPreference('dark')}>Dunkel</button>
            <button onClick={() => setPreference('system')}>System-Standard</button>
        </>
    );
}

describe('AppProvider color mode', () => {
    beforeEach(() => {
        Object.defineProperty(window, 'matchMedia', {
            configurable: true,
            writable: true,
            value: vi.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addEventListener: vi.fn(),
                removeEventListener: vi.fn(),
                addListener: vi.fn(),
                removeListener: vi.fn(),
                dispatchEvent: vi.fn(),
            })),
        });
    });

    it('uses the system preference by default and persists explicit modes only', () => {
        render(
            <StoreProvider store={store}>
                <AppProvider>
                    <ColorModeProbe/>
                </AppProvider>
            </StoreProvider>,
        );

        expect(screen.getByText('system:light')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Dunkel'}));

        expect(screen.getByText('dark:dark')).toBeInTheDocument();
        expect(localStorage.getItem('prosuna-color_mode')).toBe('dark');

        fireEvent.click(screen.getByRole('button', {name: 'System-Standard'}));

        expect(screen.getByText('system:light')).toBeInTheDocument();
        expect(localStorage.getItem('prosuna-color_mode')).toBeNull();
    });

    it('resolves the default preference from the operating system', () => {
        vi.mocked(window.matchMedia).mockImplementation((query: string) => ({
            matches: query === '(prefers-color-scheme: dark)',
            media: query,
            onchange: null,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            addListener: vi.fn(),
            removeListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <StoreProvider store={store}>
                <AppProvider>
                    <ColorModeProbe/>
                </AppProvider>
            </StoreProvider>,
        );

        expect(screen.getByText('system:dark')).toBeInTheDocument();
    });

    it('restores an explicit mode from local storage', () => {
        localStorage.setItem('prosuna-color_mode', 'dark');

        render(
            <StoreProvider store={store}>
                <AppProvider>
                    <ColorModeProbe/>
                </AppProvider>
            </StoreProvider>,
        );

        expect(screen.getByText('dark:dark')).toBeInTheDocument();
    });
});
