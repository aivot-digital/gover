import {useState} from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ConfirmProvider} from '../../providers/confirm-provider';
import {DataKeyDefinitionField} from './data-key-definition-field';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../utils/copy-to-clipboard', () => ({copyToClipboardText: vi.fn().mockResolvedValue(true)}));

function Harness({initial = 'alter_schluessel', persisted = initial, node = false}: {
    initial?: string; persisted?: string; node?: boolean;
}) {
    const [value, setValue] = useState<string | null>(initial);
    const [unlocked, setUnlocked] = useState(false);
    return <ConfirmProvider><DataKeyDefinitionField
        value={value} persistedValue={persisted} onChange={setValue} source="Größe des Haushalts"
        kind={node ? 'node-key' : 'process-data-path'} unlocked={unlocked} onUnlock={() => setUnlocked(true)} onLock={() => setUnlocked(false)}
    /></ConfirmProvider>;
}

describe('DataKeyDefinitionField', () => {
    it('shows the lock state before typing and allows manually relocking an edited key', async () => {
        const user = userEvent.setup();
        render(<Harness initial="automatisch" persisted=""/>);
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        expect(screen.getByText('Gesperrt')).toBeVisible();
        expect(input).toHaveAccessibleDescription(/Gesperrt/);
        await user.type(input, 'zusatz');
        expect(input).toHaveValue('automatisch');
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        expect(screen.queryByText('Gesperrt')).not.toBeInTheDocument();
        expect(input).not.toHaveAccessibleDescription(/Gesperrt/);
        await user.clear(input);
        await user.type(input, 'bearbeitet');
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel sperren'}));
        expect(input).toHaveAttribute('readonly');
        expect(input).toHaveAccessibleDescription(/Gesperrt/);
        expect(screen.getByRole('button', {name: 'Datenschlüssel entsperren'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'Vorgangsdatenpfad leeren'})).not.toBeInTheDocument();
        await user.type(input, 'zusatz');
        expect(input).toHaveValue('bearbeitet');
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('displays and copies the element-data prefix without including it in the editable node key', async () => {
        const user = userEvent.setup();
        render(<Harness initial="random_key_2" node/>);
        expect(screen.getByText('_.')).toBeVisible();
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('random_key_2');
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel kopieren'}));
        expect(copyToClipboardText).toHaveBeenCalledWith('_.random_key_2');
    });

    it('requires confirmation before regenerating an established key and preserves it on cancel', async () => {
        const user = userEvent.setup();
        render(<Harness/>);
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        expect(input).toHaveAttribute('readonly');
        expect(screen.queryByRole('button', {name: 'Vorgangsdatenpfad leeren'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Aus Titel erzeugen'}));
        let dialog = screen.getByRole('dialog');
        expect(dialog).toHaveTextContent('groesse_des_haushalts');
        await user.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(input).toHaveValue('alter_schluessel');
        expect(input).toHaveAttribute('readonly');
        await user.click(screen.getByRole('button', {name: 'Aus Titel erzeugen'}));
        dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', {name: 'Bearbeitung freigeben'}));
        await waitFor(() => expect(input).toHaveValue('groesse_des_haushalts'));
        expect(input).not.toHaveAttribute('readonly');
        expect(screen.getByRole('button', {name: 'Aus Titel erzeugen'})).toBeDisabled();
        await user.click(screen.getByRole('button', {name: 'Vorgangsdatenpfad leeren'}));
        expect(input).toHaveValue('');
    });

    it('unlocks a new key without confirmation and normalizes typed and pasted definitions', async () => {
        render(<Harness initial="" persisted=""/>);
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        fireEvent.change(input, {target: {value: 'person.Äußere Größe'}});
        expect(input).toHaveValue('person.Aeussere_Groesse');
        fireEvent.change(input, {target: {value: 'person.'}});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.blur(input);
        expect(input).toHaveAttribute('aria-invalid', 'true');
        fireEvent.change(input, {target: {value: 'person.Name'}});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('normalizes node keys and validates identifiers on blur and while correcting', async () => {
        const user = userEvent.setup();
        render(<Harness initial="" node/>);
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        await user.type(input, 'Größe des-Haushalts');
        expect(input).toHaveValue('Groesse_des_Haushalts');
        await user.clear(input);
        await user.paste('Straße-Änderung');
        expect(input).toHaveValue('Strasse_Aenderung');
        fireEvent.change(input, {target: {value: 'person.name'}});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.blur(input);
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription(/keine Punkte oder Klammern/);
        fireEvent.change(input, {target: {value: '1name'}});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        fireEvent.change(input, {target: {value: 'Name_2'}});
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
    });

    it.each([
        {initial: 'name_end', offset: 4, text: 'äX', expected: 'nameaeX_end', caret: 7},
        {initial: 'person.Name', offset: 6, text: ' X', expected: 'personX.Name', caret: 7},
    ])('keeps the caret when normalizing inside $initial', async ({initial, offset, text, expected, caret}) => {
        const user = userEvent.setup();
        render(<Harness initial={initial} persisted=""/>);
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/}) as HTMLInputElement;
        await user.click(input);
        input.setSelectionRange(offset, offset);
        await user.keyboard(text);
        expect(input).toHaveValue(expected);
        expect(input.selectionStart).toBe(caret);
    });

    it('retains a random node key until the user explicitly generates a name-based replacement', async () => {
        const user = userEvent.setup();
        render(<Harness initial="random_key_2" node/>);
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        expect(input).toHaveValue('random_key_2');
        await user.click(screen.getByRole('button', {name: 'Aus Name erzeugen'}));
        await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: 'Bearbeitung freigeben'}));
        await waitFor(() => expect(input).toHaveValue('groesse_des_haushalts'));
    });
});
