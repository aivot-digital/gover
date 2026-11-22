import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeNodeDataReference} from '../../../models/functions/no-code-expression';
import {InputVariableSource} from '../../../models/input-mode';
import {
    ProcessNodeEditorProvider,
    useProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {NoCodeOperandEditorProcessDataReference} from './no-code-operand-editor-process-data-reference';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

function Editor({
                    initial,
                    onChange,
                }: { initial: NoCodeNodeDataReference; onChange: (value: unknown) => void }) {
    const [value, setValue] = useState(initial);
    return <NoCodeOperandEditorProcessDataReference label="Ergebnis"
                                                    value={value}
                                                    onChange={(next) => {
                                                        onChange(next);
                                                        if (next?.type === 'NoCodeNodeDataReference') setValue(next);
                                                    }}
                                                    onAddEnclosingExpression={vi.fn()}/>;
}

const initial: NoCodeNodeDataReference = {
    type: 'NoCodeNodeDataReference',
    nodeDataKey: 'pruefung',
    path: 'person.Name',
};

describe('No-Code element data reference', () => {
    it.each(['', null, undefined].flatMap(nodeDataKey => ['', null, undefined].map(path => ({nodeDataKey, path}))))
    ('renders an empty input for node key $nodeDataKey and path $path without changing the reference', ({nodeDataKey, path}) => {
        const onChange = vi.fn();
        render(<Editor initial={{...initial, nodeDataKey, path}}
                       onChange={onChange}/>);

        const input = screen.getByRole('textbox', {name: /^Ergebnis/});
        expect(input).toHaveValue('');
        expect(input.parentElement).toHaveTextContent('_.');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('edits a combined path without storing the display prefix and preserves incomplete input', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Editor initial={initial}
                       onChange={onChange}/>);
        const input = screen.getByRole('textbox', {name: /^Ergebnis/});
        expect(input).toHaveValue('pruefung.person.Name');
        expect(input).toBeRequired();
        expect(input.parentElement).toHaveTextContent('_.');
        expect(screen.getAllByRole('textbox')).toHaveLength(1);
        fireEvent.change(input, {target: {value: 'formular.'}});
        expect(input).toHaveValue('formular.');
        fireEvent.blur(input);
        expect(input).toHaveAttribute('aria-invalid', 'true');
        await user.type(input, 'items[[0].Name');
        expect(onChange).toHaveBeenLastCalledWith({
            ...initial,
            nodeDataKey: 'formular',
            path: 'items[0].Name',
        });
        expect(input).toHaveValue('formular.items[0].Name');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('retains numeric legacy keys and array roots without rewriting an existing reference', () => {
        const onChange = vi.fn();
        render(<Editor initial={{
            ...initial,
            nodeDataKey: '123',
            path: '[0].Name',
        }}
                       onChange={onChange}/>);
        const input = screen.getByRole('textbox', {name: /^Ergebnis/});
        expect(input).toHaveValue('123[0].Name');
        fireEvent.blur(input);
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.change(input, {target: {value: '123[1].Name'}});
        expect(onChange).toHaveBeenLastCalledWith({
            ...initial,
            nodeDataKey: '123',
            path: '[1].Name',
        });
    });

    it('filters the existing picker to element data and applies the selected technical reference', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const context = {
            incomingMetadata: {
                inputVariables: [
                    {
                        source: InputVariableSource.ElementData,
                        nodeDataKey: 'formular',
                        path: 'email',
                        label: 'E-Mail-Adresse',
                    },
                    {
                        source: InputVariableSource.ProcessData,
                        path: 'other',
                        label: 'Andere Ebene',
                    },
                ],
            },
        } as ReturnType<typeof useProcessNodeEditorContext>;
        render(<ProcessNodeEditorProvider value={context}><Editor initial={initial}
                                                                  onChange={onChange}/></ProcessNodeEditorProvider>);
        await user.click(screen.getByRole('button', {name: 'Elementdaten auswählen'}));
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).queryByRole('tablist')).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('tab', {name: 'Vorgangsdaten'})).not.toBeInTheDocument();
        expect(within(dialog).queryByText('Andere Ebene')).not.toBeInTheDocument();
        await user.click(within(dialog).getByRole('radio', {name: /E-Mail-Adresse/}));
        expect(onChange).not.toHaveBeenCalled();
        await user.click(within(dialog).getByRole('button', {name: 'Referenz verwenden'}));
        expect(onChange).toHaveBeenLastCalledWith({
            ...initial,
            nodeDataKey: 'formular',
            path: 'email',
        });
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('allows manual references without metadata and cancels picker drafts without changing the value', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Editor initial={initial}
                       onChange={onChange}/>);
        await user.click(screen.getByRole('button', {name: 'Elementdaten auswählen'}));
        expect(screen.getByText('Keine passenden Vorschläge gefunden')).toBeInTheDocument();
        fireEvent.change(screen.getByRole('textbox', {name: /^Variablenpfad/}), {target: {value: 'formular[0].email'}});
        await user.click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(onChange).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Elementdaten auswählen'}));
        expect(screen.getByRole('textbox', {name: /^Variablenpfad/})).toHaveValue('pruefung.person.Name');
        fireEvent.change(screen.getByRole('textbox', {name: /^Variablenpfad/}), {target: {value: 'formular[0].email'}});
        await user.click(screen.getByRole('button', {name: 'Referenz verwenden'}));
        expect(onChange).toHaveBeenLastCalledWith({
            ...initial,
            nodeDataKey: 'formular',
            path: '[0].email',
        });
    });

    it.each(['pruefung.items[*].Name', 'pruefung.items[2147483648].Name', 'pruefung', 'pruefung.$invalid-key'])
    ('reports invalid path %s without losing the draft', (path) => {
        render(<Editor initial={initial}
                       onChange={vi.fn()}/>);
        const input = screen.getByRole('textbox', {name: /^Ergebnis/});
        fireEvent.change(input, {target: {value: path}});
        fireEvent.blur(input);
        expect(input).toHaveValue(path);
        expect(input).toHaveAttribute('aria-invalid', 'true');
    });
});
