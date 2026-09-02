import {useState} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {
    InputModeField,
    type InputModeValue,
    type InputModeVariable,
} from './input-mode-field';
import {TextFieldComponent} from '../text-field/text-field-component';

const variables: InputModeVariable[] = [
    {
        id: 'count',
        label: 'Anzahl der Positionen',
        path: 'warenkorb.anzahl',
        origin: 'Warenkorb laden',
        category: 'processData',
    },
    {
        id: 'total',
        label: 'Gesamtbetrag',
        path: 'warenkorb.gesamtbetrag',
        origin: 'Warenkorb laden',
        category: 'processData',
    },
    {
        id: 'case-number',
        label: 'Aktenzeichen des Vorgangs',
        path: 'caseNumber',
        origin: 'Vorgang',
        category: 'protectedProcessData',
    },
];

function createInitialValue(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: '42',
        variableId: null,
        noCode: {
            sourceVariableId: 'count',
            operator: 'multiply',
            operand: '2',
        },
        lowCode: 'return $.warenkorb.anzahl;',
    };
}

function Harness(props: {
    onInsertVariable?: (variable: InputModeVariable) => void;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
}) {
    const [value, setValue] = useState(createInitialValue);

    return (
        <InputModeField
            label="Inkrement"
            variables={variables}
            value={value}
            onChange={setValue}
            onInsertVariable={props.onInsertVariable}
            error={props.error}
            required={props.required}
            disabled={props.disabled}
            readOnly={props.readOnly}
            renderLiteral={({value: literal, onChange, variableInsertAction, fieldProps}) => (
                <>
                    <TextFieldComponent
                        {...fieldProps}
                        readonly={fieldProps.readOnly}
                        value={literal}
                        onChange={onChange}
                    />
                    {variableInsertAction != null && (
                        <button onClick={variableInsertAction.onClick}>
                            Platzhalter einfügen
                        </button>
                    )}
                </>
            )}
        />
    );
}

async function selectMode(user: ReturnType<typeof userEvent.setup>, mode: string) {
    await user.click(screen.getByRole('button', {name: /Eingabemodus für Inkrement ändern/}));
    const modeByLabel: Record<string, string> = {
        Wert: 'literal',
        Variable: 'variable',
        'Ausdruck (No-Code)': 'noCode',
        'Skript (Low-Code)': 'lowCode',
    };
    await user.click(screen.getByTestId(`input-mode-option-${modeByLabel[mode]}`));
}

describe('InputModeField', () => {
    it('associates the external label with the literal control', () => {
        render(<Harness/>);

        const input = screen.getByRole('textbox', {name: /^Inkrement/});
        const label = screen.getByText('Inkrement', {selector: 'label'});
        const modeSelector = screen.getByRole('button', {name: 'Wert: Eingabemodus für Inkrement ändern'});

        expect(label).toHaveAttribute('for', input.id);
        expect(label.parentElement).toContainElement(modeSelector);
    });

    it('switches to a variable and keeps the selected mapping', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Variable referenzieren/}));
        expect(screen.getByText(/Die Vorschläge zeigen Variablen/)).toBeInTheDocument();
        expect(screen.getByLabelText('Variablenvorschläge')).toBeInTheDocument();
        const variableOption = await screen.findByLabelText(/Gesamtbetrag/);
        await user.click(variableOption);
        expect(variableOption).toBeChecked();
        await user.click(screen.getByTestId('use-variable-reference'));

        const summary = screen.getByLabelText(/Gesamtbetrag.*Inkrement: Variable referenzieren/);
        expect(summary).toHaveTextContent('Gesamtbetrag');
        expect(summary).toHaveTextContent('$.warenkorb.gesamtbetrag');
    });

    it('groups the primary and cancel actions before the clear action', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Variable referenzieren/}));

        const primaryActions = screen.getByTestId('variable-dialog-primary-actions');
        const primaryActionLabels = Array.from(primaryActions.querySelectorAll('button'))
            .map((button) => button.textContent);
        const dialogActions = primaryActions.parentElement;

        expect(primaryActionLabels).toEqual([
            'Referenz verwenden',
            'Abbrechen',
        ]);
        expect(dialogActions?.children).toHaveLength(2);
        expect(dialogActions?.children[0]).toBe(primaryActions);
        expect(dialogActions?.children[1]).toHaveTextContent('Referenz entfernen');
    });

    it('preserves the literal draft while other modes are active', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        const input = screen.getByRole('textbox', {name: /^Inkrement/});
        await user.clear(input);
        await user.type(input, '7');

        await selectMode(user, 'Ausdruck (No-Code)');
        expect(screen.queryByRole('textbox', {name: /^Inkrement/})).not.toBeInTheDocument();

        await selectMode(user, 'Wert');
        expect(screen.getByRole('textbox', {name: /^Inkrement/})).toHaveValue('7');
    });

    it('offers the complete variable picker for dynamic text', async () => {
        const user = userEvent.setup();
        const handleInsertVariable = vi.fn();
        render(<Harness onInsertVariable={handleInsertVariable}/>);

        expect(screen.getByLabelText('Dieser dynamische Text unterstützt Variablen und Bedingungen.'))
            .toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Platzhalter einfügen'}));
        await user.click(await screen.findByText('Aktenzeichen des Vorgangs'));
        await user.click(screen.getByTestId('use-variable-reference'));

        expect(handleInsertVariable).toHaveBeenCalledWith(variables[2]);
    });

    it('filters variables by their fachliche category', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Variable referenzieren/}));
        await user.click(screen.getByTestId('variable-category-protectedProcessData'));

        expect(screen.getByText('Aktenzeichen des Vorgangs')).toBeInTheDocument();
        expect(screen.queryByText('Gesamtbetrag')).not.toBeInTheDocument();
        expect(screen.getByText(/\$\$\.caseNumber/)).toBeInTheDocument();
    });

    it('propagates required, invalid and read-only state to the literal control', () => {
        render(<Harness required error="Ungültiger Wert" readOnly/>);

        const input = screen.getByRole('textbox', {name: /^Inkrement/});
        expect(input).toHaveAttribute('aria-required', 'true');
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAttribute('aria-readonly', 'true');
        expect(input).toHaveAttribute('readonly');
        expect(screen.getByRole('button', {name: /Eingabemodus für Inkrement ändern/})).toBeDisabled();
    });
});
