import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {HelpDialog} from './help.dialog';

describe('HelpDialog', () => {
    it('explains the optional field label in the form instructions', async () => {
        const user = userEvent.setup();
        render(<HelpDialog open onHide={vi.fn()}/>);

        await user.click(screen.getByRole('button', {name: 'Wie funktioniert das Online-Formular?'}));

        expect(screen.getByText('Füllen Sie die Pflichtfelder aus. Felder mit dem Zusatz „optional“ können Sie freilassen.'))
            .toBeVisible();
    });
});
