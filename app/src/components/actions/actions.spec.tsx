import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {Actions} from './actions';

describe('Actions', () => {
    it('uses the tooltip as the accessible name of an icon-only action', () => {
        render(
            <Actions
                actions={[{
                    icon: <span aria-hidden="true">x</span>,
                    tooltip: 'Eintrag löschen',
                    onClick: vi.fn(),
                }]}
            />,
        );

        expect(screen.getByRole('button', {name: 'Eintrag löschen'})).toBeInTheDocument();
    });
});
