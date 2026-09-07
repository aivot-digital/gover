import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {InputModeComparison} from './input-mode-comparison';

vi.mock('../../hooks/use-api', () => {
    const api = {get: vi.fn().mockResolvedValue([{
        identifier: 'add',
        humanReadableTemplate: 'Addiere #0 und #1',
        signatures: [{parameters: [{label: 'Wert 1'}, {label: 'Wert 2'}]}],
    }])};
    return {useApi: () => api};
});

const variables = [{
    label: 'Anzahl der Positionen',
    path: 'warenkorb.positionen.anzahl',
    source: 'ProcessData' as const,
    origin: 'Warenkorb laden',
}];

describe('InputModeComparison', () => {
    it('shows all four modes together with editable filled and empty examples', async () => {
        const user = userEvent.setup();
        render(<InputModeComparison variables={variables}/>);

        const literal = within(screen.getByRole('group', {name: 'Wert'}));
        expect(literal.getByRole('textbox', {name: /^Inkrement/})).toHaveValue('13');
        expect(within(screen.getByRole('group', {name: 'Variable'})).getByText('Anzahl der Positionen')).toBeInTheDocument();
        expect(within(screen.getByRole('group', {name: 'Ausdruck (No-Code)'})).getByText('Ausdruck definiert')).toBeInTheDocument();
        expect(await screen.findByText('Addiere Vorgangsdaten → warenkorb.positionen.anzahl und „1“')).toBeInTheDocument();
        expect(within(screen.getByRole('group', {name: 'Skript (Low-Code)'}))
            .getByText('const anzahl = $.warenkorb?.positionen?.anzahl ?? 0;')).toBeInTheDocument();

        await user.clear(literal.getByRole('textbox'));
        await user.type(literal.getByRole('textbox'), '7');
        await user.click(screen.getByRole('button', {name: 'Leer'}));

        expect(literal.getByRole('textbox')).toHaveValue('');
        expect(screen.getByText('Keine Variable referenziert')).toBeInTheDocument();
        expect(screen.getByText('Kein Ausdruck definiert')).toBeInTheDocument();
        expect(screen.getByText('Kein Skript definiert')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Befüllt'}));
        expect(literal.getByRole('textbox')).toHaveValue('7');
    });

    it('locks editing in the read-only comparison without disabling the summary controls', async () => {
        const user = userEvent.setup();
        render(<InputModeComparison variables={variables}/>);

        await user.click(screen.getByRole('button', {name: 'Schreibgeschützt'}));

        expect(screen.getByRole('textbox', {name: /^Inkrement/})).toHaveAttribute('readonly');
        for (const selector of screen.getAllByRole('button', {name: /Eingabemodus für Inkrement ändern/})) {
            expect(selector).toBeDisabled();
        }
        for (const summary of screen.getAllByRole('button', {name: /Inkrement: .*ansehen/})) {
            expect(summary).toBeEnabled();
        }
    });
});
