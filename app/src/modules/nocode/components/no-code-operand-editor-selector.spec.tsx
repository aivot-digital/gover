import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
        expect(within(list).getAllByRole('button').slice(0, 3).map(button => button.getAttribute('aria-label'))).toEqual([
            'Wert selbst festlegen', 'Mit Funktion ermitteln', 'Aus Formularelement übernehmen',
        ]);
        expect(within(list).getByRole('button', {name: 'Wert selbst festlegen'})).toHaveAccessibleDescription(
            'Einen Wert direkt eingeben oder aus verfügbaren Optionen auswählen. Der festgelegte Wert wird bei jeder Ausführung unverändert verwendet.',
        );
        expect(within(list).getByRole('button', {name: 'Mit Funktion ermitteln'})).toHaveAccessibleDescription(
            'Das Ergebnis einer Funktion verwenden. Die Funktion ermittelt den Wert bei der Ausführung.',
        );
        const element = within(list).getByRole('button', {name: 'Aus Formularelement übernehmen'});
        expect(element).toHaveAccessibleDescription('Den aktuellen eingegebenen oder berechneten Wert eines Formularelements verwenden.');
        fireEvent.click(element);
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeReference',
            elementId: '',
        });
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('offers declared options directly without a default and keeps their technical values', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<NoCodeOperandEditorSelector allOperators={[]}
                                            label="Vergleich"
                                            parameterOptions={[
                                                {label: 'ist davor', value: 'vor'},
                                                {label: 'ist davor oder gleich', value: 'vor-oder-gleich'},
                                            ]}
                                            onChange={onChange}/>);
        const list = screen.getByRole('list', {name: 'Vergleich auswählen'});
        expect(within(list).getAllByRole('button').map(button => button.textContent)).toEqual([
            'ist davor', 'ist davor oder gleich', 'Andere Wertquelle verwenden …',
        ]);
        expect(onChange).not.toHaveBeenCalled();
        await user.click(within(list).getByRole('button', {name: 'ist davor oder gleich'}));
        expect(onChange).toHaveBeenCalledExactlyOnceWith({
            type: 'NoCodeStaticValue',
            value: 'vor-oder-gleich',
        }, false);
    });

    it.each(['FORM', 'PROCESS', 'BOTH'] as const)('keeps other sources contextual and navigates by keyboard in %s', async (contextType) => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<NoCodeOperandEditorSelector allOperators={[]}
                                            label="Einheit"
                                            parameterOptions={[{label: 'Jahre', value: 'years'}]}
                                            contextType={contextType}
                                            onChange={onChange}/>);
        screen.getByRole('button', {name: 'Andere Wertquelle verwenden …'}).focus();
        await user.keyboard('{Enter}');
        const back = screen.getByRole('button', {name: 'Zurück zur Auswahl'});
        expect(back).toHaveFocus();
        expect(screen.queryByRole('button', {name: 'Wert selbst festlegen'})).not.toBeInTheDocument();
        expect(screen.queryAllByRole('button', {name: 'Aus Formularelement übernehmen'})).toHaveLength(contextType === 'PROCESS' ? 0 : 1);
        for (const name of ['Vorgangsdaten', 'Geschützte Vorgangsdaten', 'Elementdaten']) {
            expect(screen.queryAllByRole('button', {name})).toHaveLength(contextType === 'FORM' ? 0 : 1);
        }
        await user.keyboard('{Enter}');
        expect(screen.getByRole('button', {name: 'Jahre'})).toHaveFocus();
        expect(screen.queryByRole('button', {name: 'Zurück zur Auswahl'})).not.toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('returns to the alternate sources after canceling the function dialog without changing the value', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<NoCodeOperandEditorSelector allOperators={[operator(NoCodeDataType.String)]}
                                            label="Vergleich"
                                            desiredType={NoCodeDataType.String}
                                            parameterOptions={[{label: 'ist gleich', value: 'gleich'}]}
                                            contextType="FORM"
                                            onChange={onChange}/>);
        await user.click(screen.getByRole('button', {name: 'Andere Wertquelle verwenden …'}));
        await user.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
        const dialog = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        await user.click(within(dialog).getByRole('button', {name: 'Schließen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getByRole('button', {name: 'Mit Funktion ermitteln'})).toHaveFocus();
        expect(screen.getByRole('button', {name: 'Zurück zur Auswahl'})).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Aus Formularelement übernehmen'}));
        expect(onChange).toHaveBeenCalledExactlyOnceWith({type: 'NoCodeReference', elementId: ''});
    });

    it.each(['FORM', 'PROCESS', 'BOTH'] as const)('shows only applicable choices in %s', (contextType) => {
        render(<NoCodeOperandEditorSelector allOperators={[]}
                                            label="Wert"
                                            contextType={contextType}
                                            onChange={vi.fn()}/>);
        expect(screen.queryAllByRole('button', {name: 'Aus Formularelement übernehmen'})).toHaveLength(contextType === 'PROCESS' ? 0 : 1);
        for (const name of ['Vorgangsdaten', 'Geschützte Vorgangsdaten', 'Elementdaten']) {
            expect(screen.queryAllByRole('button', {name})).toHaveLength(contextType === 'FORM' ? 0 : 1);
        }
        expect(screen.getByRole('button', {name: 'Mit Funktion ermitteln'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Wert selbst festlegen'})).toBeInTheDocument();
    });

    it('respects the slot type, leaves cancellation unchanged and initializes operator parameters', async () => {
        const onChange = vi.fn();
        render(
            <NoCodeOperandEditorSelector allOperators={[operator(NoCodeDataType.String), operator(NoCodeDataType.Boolean)]}
                                         desiredType={NoCodeDataType.Boolean}
                                         label="Wert"
                                         onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
        let picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        expect(within(picker).queryByRole('button', {name: `${NoCodeDataType.String}: Hinzufügen`})).not.toBeInTheDocument();
        fireEvent.click(within(picker).getByRole('button', {name: 'Schließen'}));
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(await screen.findByRole('button', {name: 'Mit Funktion ermitteln'}));
        picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        fireEvent.click(within(picker).getByRole('button', {name: `${NoCodeDataType.Boolean}: Hinzufügen`}));
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: NoCodeDataType.Boolean,
            operands: [null],
        });
    });
});
