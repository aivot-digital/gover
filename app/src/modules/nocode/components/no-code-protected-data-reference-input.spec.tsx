import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {InputVariableSource} from '../../../models/input-mode';
import {
    ProcessNodeEditorProvider,
    useProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {NoCodeProtectedDataReferenceInput} from './no-code-protected-data-reference-input';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

it.each(['PROCESS', 'BOTH', 'FORM'] as const)('offers protected references only in compatible contexts: %s', (contextType) => {
    const onChange = vi.fn();
    render(<NoCodeOperandEditorSelector label="Wert"
                                        allOperators={[]}
                                        contextType={contextType}
                                        onChange={onChange}/>);
    const button = screen.queryByRole('button', {name: 'Geschützte Vorgangsdaten'});
    if (contextType === 'FORM') expect(button).not.toBeInTheDocument();
    else {
        fireEvent.click(button!);
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeInstanceDataReference',
            path: '',
        });
    }
});

describe('protected process data picker', () => {
    it('filters suggestions to the protected source and applies a path without its display prefix', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const context = {
            incomingMetadata: {
                inputVariables: [
                    {
                        source: InputVariableSource.ProtectedProcessData,
                        path: 'caseNumber',
                        label: 'Vorgangskennung',
                    },
                    {
                        source: InputVariableSource.ProcessData,
                        path: 'other',
                        label: 'Andere Ebene',
                    },
                ],
            },
        } as ReturnType<typeof useProcessNodeEditorContext>;
        render(<ProcessNodeEditorProvider value={context}><NoCodeProtectedDataReferenceInput label="Wert"
                                                                                             value={{
                                                                                                 type: 'NoCodeInstanceDataReference',
                                                                                                 path: '',
                                                                                             }}
                                                                                             onChange={onChange}
                                                                                             endAction={[]}/></ProcessNodeEditorProvider>);
        await user.click(screen.getByRole('button', {name: 'Geschützte Vorgangsdaten auswählen'}));
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).queryByRole('tab', {name: 'Vorgangsdaten'})).not.toBeInTheDocument();
        expect(within(dialog).queryByText('Andere Ebene')).not.toBeInTheDocument();
        await user.click(within(dialog).getByRole('radio', {name: /Vorgangskennung/}));
        expect(onChange).not.toHaveBeenCalled();
        await user.click(within(dialog).getByRole('button', {name: 'Referenz verwenden'}));
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeInstanceDataReference',
            path: 'caseNumber',
        });
    });

    it('allows manual paths and leaves the original unchanged when cancelling the picker', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<NoCodeProtectedDataReferenceInput label="Wert"
                                                  value={{
                                                      type: 'NoCodeInstanceDataReference',
                                                      path: 'caseNumber',
                                                  }}
                                                  onChange={onChange}
                                                  endAction={[]}/>);
        await user.click(screen.getByRole('button', {name: 'Geschützte Vorgangsdaten auswählen'}));
        await user.click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.change(screen.getByRole('textbox', {name: /^Wert/}), {target: {value: 'identities.person.email'}});
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeInstanceDataReference',
            path: 'identities.person.email',
        });
    });
});
