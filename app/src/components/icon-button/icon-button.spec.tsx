import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {IconButton} from './icon-button';

describe('IconButton', () => {
    it('uses a string tooltip as the button label, including when wrapped in a badge', () => {
        render(
            <>
                <IconButton
                    buttonProps={{onClick: vi.fn()}}
                    tooltipProps={{title: 'Aktualisieren'}}
                >
                    <span aria-hidden="true">r</span>
                </IconButton>
                <IconButton
                    buttonProps={{onClick: vi.fn()}}
                    tooltipProps={{title: 'Benachrichtigungen'}}
                    badgeProps={{badgeContent: 2}}
                >
                    <span aria-hidden="true">n</span>
                </IconButton>
            </>,
        );

        expect(screen.getByRole('button', {name: 'Aktualisieren'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Benachrichtigungen'})).toBeInTheDocument();
    });

    it('preserves an explicit accessible label', () => {
        render(
            <IconButton
                buttonProps={{onClick: vi.fn(), 'aria-label': 'Expliziter Name'}}
                tooltipProps={{title: 'Kurzer Tooltip'}}
            >
                <span aria-hidden="true">x</span>
            </IconButton>,
        );

        expect(screen.getByRole('button', {name: 'Expliziter Name'})).toBeInTheDocument();
    });
});
