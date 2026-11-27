import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';
import {InputModeIconComparison} from './input-mode-icon-comparison';

describe('InputModeIconComparison', () => {
    it('shows named recommendations and alternatives without changing the active icon set', async () => {
        const user = userEvent.setup();
        render(<InputModeIconComparison/>);

        await user.click(screen.getByRole('button', {name: 'Icon-Vorschläge'}));
        const table = screen.getByRole('table', {name: 'Icon-Vorschläge für Eingabemodi und Formularstruktur'});
        expect(within(table).getAllByRole('row')).toHaveLength(7);

        const noCode = within(table).getByRole('row', {name: /Ausdruck \(No-Code\)/});
        expect(within(noCode).getByRole('cell', {name: 'DynamicForm'})).toBeInTheDocument();
        expect(within(noCode).getByRole('cell', {name: 'AccountTree Schema'})).toBeInTheDocument();
        expect(within(table).getByRole('row', {name: /Dynamischer Text Function/})).toBeInTheDocument();
        expect(within(table).getByRole('row', {name: /Formularstruktur AccountTree Toc FormatListBulleted ViewList/})).toBeInTheDocument();
        table.querySelectorAll('svg').forEach((icon) => expect(icon).toHaveAttribute('aria-hidden', 'true'));
    });
});
