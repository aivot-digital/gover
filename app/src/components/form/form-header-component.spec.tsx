import React from 'react';
import {createTheme, ThemeProvider} from '@mui/material';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {FormHeaderComponent} from './form-header-component';
import type {FormLayoutElement} from '../../models/elements/form-layout-element';
import type {ProcessEntity} from '../../modules/process/entities/process-entity';
import type {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import type {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(),
}));

vi.mock('../color-mode-picker/color-mode-picker', () => ({
    ColorModePicker: () => null,
}));

vi.mock('react-wrap-balancer', () => ({
    default: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

describe('FormHeaderComponent', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('shows the divider when an already loaded logo URL arrives after mounting', async () => {
        vi.spyOn(HTMLImageElement.prototype, 'complete', 'get').mockReturnValue(true);
        vi.spyOn(HTMLImageElement.prototype, 'naturalWidth', 'get').mockReturnValue(200);
        const theme = createTheme();
        const {rerender} = renderHeader(theme, null);

        expect(getComputedStyle(getTitleContainer()).borderLeftStyle).toBe('none');

        rerender(createHeader(theme, '/theme-logo.svg'));

        await waitFor(() => {
            expect(getComputedStyle(getTitleContainer()).borderLeftStyle).toBe('solid');
        });
    });

    it('does not retain the divider when a replacement logo fails to load', async () => {
        vi.spyOn(HTMLImageElement.prototype, 'complete', 'get').mockReturnValue(false);
        const theme = createTheme();
        const {rerender} = renderHeader(theme, '/first-logo.svg');

        fireEvent.load(screen.getByRole('img'));
        await waitFor(() => {
            expect(getComputedStyle(getTitleContainer()).borderLeftStyle).toBe('solid');
        });

        rerender(createHeader(theme, '/broken-logo.svg'));
        fireEvent.error(screen.getByRole('img'));

        await waitFor(() => {
            expect(getComputedStyle(getTitleContainer()).borderLeftStyle).toBe('none');
        });
    });
});

function renderHeader(theme: ReturnType<typeof createTheme>, logoUrl: string | null) {
    return render(createHeader(theme, logoUrl));
}

function createHeader(theme: ReturnType<typeof createTheme>, logoUrl: string | null) {
    return (
        <ThemeProvider theme={theme}>
            <FormHeaderComponent
                form={{publicTitle: 'Testformular'} as FormLayoutElement}
                node={{} as ProcessNodeEntity}
                process={{} as ProcessEntity}
                version={{
                    publicTitle: 'Testformular',
                    updated: '2026-09-14T10:00:00Z',
                } as ProcessVersionEntity}
                logoUrl={logoUrl}
                logoUrlDark={null}
                onDeleteFormData={vi.fn()}
            />
        </ThemeProvider>
    );
}

function getTitleContainer(): HTMLElement {
    return screen.getByRole('heading', {name: 'Testformular'}).parentElement!;
}
