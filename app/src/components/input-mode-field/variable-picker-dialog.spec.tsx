import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {VariablePickerDialog} from './input-mode-field';
import {InputVariableSource, type InputVariableReference} from '../../models/input-mode';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../utils/copy-to-clipboard', () => ({copyToClipboardText: vi.fn().mockResolvedValue(true)}));

function renderPicker(reference: InputVariableReference | null = null, readOnly = false, offerFormattedInsertion = false) {
    const onSelect = vi.fn();
    render(<VariablePickerDialog
        open title="Variable referenzieren" variables={[]}
        allowedSources={Object.values(InputVariableSource)}
        selectedReference={reference} readOnly={readOnly} allowClear
        offerFormattedInsertion={offerFormattedInsertion}
        onSelect={onSelect} onClear={vi.fn()} onClose={vi.fn()}
    />);
    return {onSelect};
}

describe('custom variable references', () => {
    it.each([
        {source: InputVariableSource.ProcessData, path: 'person.Name', prefix: '$.', input: 'person.Name'},
        {source: InputVariableSource.ProtectedProcessData, path: 'caseNumber', prefix: '$$.', input: 'caseNumber'},
        {source: InputVariableSource.ElementData, path: '[0].Name', nodeDataKey: 'pruefen', prefix: '_.', input: 'pruefen[0].Name'},
        {source: InputVariableSource.ElementMetadata, path: 'finished', nodeDataKey: 'pruefen', prefix: '$$.taskMetadata.', input: 'pruefen.finished'},
    ])('displays and copies the $source prefix without storing it in the reference', async ({source, path, nodeDataKey, prefix, input}) => {
        const user = userEvent.setup();
        const reference = {source, path, ...(nodeDataKey ? {nodeDataKey} : {})};
        const {onSelect} = renderPicker(reference);
        const field = screen.getByRole('textbox', {name: /^Variablenpfad/});
        expect(field).toHaveValue(input);
        expect(field.parentElement).toHaveTextContent(prefix);
        await user.click(screen.getByRole('button', {name: 'Variablenpfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenCalledWith(prefix + input);
        await user.click(screen.getByRole('button', {name: 'Referenz verwenden'}));
        expect(onSelect).toHaveBeenCalledWith(reference, undefined, 'text');
    });

    it('validates on blur and while correcting without silently renaming the referenced key', async () => {
        const user = userEvent.setup();
        const {onSelect} = renderPicker();
        const field = screen.getByRole('textbox', {name: /^Variablenpfad/});
        const apply = screen.getByRole('button', {name: 'Referenz verwenden'});
        await user.click(field);
        await user.paste('person.Äußerer-Name');
        expect(field).toHaveValue('person.Äußerer-Name');
        expect(field).not.toHaveAttribute('aria-invalid', 'true');
        expect(apply).toBeDisabled();
        fireEvent.blur(field);
        expect(field).toHaveAttribute('aria-invalid', 'true');
        expect(field).toHaveAccessibleDescription(/Verwenden Sie Buchstaben/);
        fireEvent.change(field, {target: {value: 'personen[*].Name'}});
        expect(field).toHaveAccessibleDescription(/keine Platzhalter/);
        expect(apply).toBeDisabled();
        fireEvent.change(field, {target: {value: 'personen[0].Name'}});
        expect(field).not.toHaveAttribute('aria-invalid', 'true');
        await user.click(apply);
        expect(onSelect).toHaveBeenCalledWith({source: InputVariableSource.ProcessData, path: 'personen[0].Name'}, undefined, 'text');
    });

    it.each([InputVariableSource.ElementData, InputVariableSource.ElementMetadata])
    ('explains the required child path for %s and reevaluates it when changing sources', async (source) => {
        const user = userEvent.setup();
        renderPicker({source, nodeDataKey: 'pruefen', path: ''});
        const field = screen.getByRole('textbox', {name: /^Variablenpfad/});
        expect(field).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.blur(field);
        expect(field).toHaveAccessibleDescription(/nach dem Element-Datenschlüssel den Pfad/);
        expect(screen.getByRole('button', {name: 'Referenz verwenden'})).toBeDisabled();
        await user.click(screen.getByRole('combobox', {name: /^Variablenquelle/}));
        await user.click(screen.getByRole('option', {name: 'Vorgangsdaten'}));
        expect(field).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('button', {name: 'Referenz verwenden'})).toBeEnabled();
    });

    it('keeps copying available for a read-only reference', async () => {
        const user = userEvent.setup();
        renderPicker({source: InputVariableSource.ProcessData, path: 'person.Name'}, true);
        expect(screen.getByRole('textbox', {name: /^Variablenpfad/})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Referenz verwenden'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Variablenpfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenCalledWith('$.person.Name');
    });
});

describe('insertion into Markdown fields', () => {
    const reference = {source: InputVariableSource.ProcessData, path: 'nachricht'};

    it('inserts values as text unless the author chooses formatting', async () => {
        const user = userEvent.setup();
        const {onSelect} = renderPicker(reference, false, true);
        const insertion = screen.getByRole('group', {name: /^Einfügen/});

        expect(screen.getByRole('radio', {name: 'Als Text'})).toBeChecked();
        expect(insertion).toHaveAccessibleDescription(/zeigt den Wert genau so, wie er eingegeben wurde/);
        await user.click(screen.getByRole('radio', {name: 'Mit Formatierung'}));
        await user.click(screen.getByRole('button', {name: 'Referenz verwenden'}));

        expect(onSelect).toHaveBeenCalledWith(reference, undefined, 'markdown');
    });

    it('does not offer formatting for other fields', async () => {
        const user = userEvent.setup();
        const {onSelect} = renderPicker(reference);

        expect(screen.queryByRole('radio', {name: 'Mit Formatierung'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Referenz verwenden'}));

        expect(onSelect).toHaveBeenCalledWith(reference, undefined, 'text');
    });
});
