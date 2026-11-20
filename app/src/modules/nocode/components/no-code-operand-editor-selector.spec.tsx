import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';

const operator = (type: NoCodeDataType): NoCodeOperatorDetailsDTO => ({
    identifier: type,
    packageName: 'core',
    label: type,
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: type,
        parameters: [{
            type,
            label: 'Wert',
            description: null,
            options: [],
        }],
    }],
});

describe('NoCodeOperandEditorSelector', () => {
    it('explains choices directly and exposes each row as one named action', () => {
        const onChange = vi.fn();
        render(<NoCodeOperandEditorSelector allOperators={[]}
                                            label="Wert"
                                            onChange={onChange}/>);
        const list = screen.getByRole('list', {name: 'Wert'});
        expect(within(list).getAllByRole('button')).toHaveLength(6);
        const element = within(list).getByRole('button', {name: 'Wert eines Elements'});
        expect(element).toHaveAccessibleDescription('Einen eingegebenen oder berechneten Formularwert verwenden.');
        fireEvent.click(element);
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeReference',
            elementId: '',
        });
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it.each(['FORM', 'PROCESS', 'BOTH'] as const)('shows only applicable choices in %s', (contextType) => {
        render(<NoCodeOperandEditorSelector allOperators={[]}
                                            label="Wert"
                                            contextType={contextType}
                                            onChange={vi.fn()}/>);
        expect(screen.queryAllByRole('button', {name: 'Wert eines Elements'})).toHaveLength(contextType === 'PROCESS' ? 0 : 1);
        for (const name of ['Vorgangsdaten', 'Geschützte Vorgangsdaten', 'Elementdaten']) {
            expect(screen.queryAllByRole('button', {name})).toHaveLength(contextType === 'FORM' ? 0 : 1);
        }
        expect(screen.getByRole('button', {name: 'Operator'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Fester Wert'})).toBeInTheDocument();
    });

    it('respects the slot type, leaves cancellation unchanged and initializes operator parameters', async () => {
        const onChange = vi.fn();
        render(
            <NoCodeOperandEditorSelector allOperators={[operator(NoCodeDataType.String), operator(NoCodeDataType.Boolean)]}
                                         desiredType={NoCodeDataType.Boolean}
                                         label="Wert"
                                         onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Operator'}));
        let picker = await screen.findByRole('dialog', {name: 'Operator auswählen'});
        expect(within(picker).queryByRole('button', {name: `${NoCodeDataType.String}: Hinzufügen`})).not.toBeInTheDocument();
        fireEvent.click(within(picker).getByRole('button', {name: 'Schließen'}));
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(await screen.findByRole('button', {name: 'Operator'}));
        picker = await screen.findByRole('dialog', {name: 'Operator auswählen'});
        fireEvent.click(within(picker).getByRole('button', {name: `${NoCodeDataType.Boolean}: Hinzufügen`}));
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: NoCodeDataType.Boolean,
            operands: [null],
        });
    });
});
