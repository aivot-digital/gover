import {useState} from 'react';
import {render, screen, within} from '@testing-library/react';
import {ThemeProvider} from '@mui/material';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {
    InputModeField,
    type InputMode,
    type InputModeValue,
    type InputModeVariable,
} from './input-mode-field';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type InputVariableSource} from '../../models/input-mode';
import {BaseTheme} from '../../theming/base-theme';
import {createDefaultAppTheme} from '../../theming/themes';
import {FormFieldTokens} from '../../theming/form-field-tokens';

vi.mock('../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});

vi.mock('../code-editor/code-editor', () => ({
    CodeEditor: ({ariaLabel, value, readOnly, onChange}: {
        ariaLabel: string;
        value: string;
        readOnly: boolean;
        onChange: (value: string) => void;
    }) => <textarea aria-label={ariaLabel} value={value} readOnly={readOnly} onChange={(event) => onChange(event.target.value)}/>,
}));

const variables: InputModeVariable[] = [
    {
        id: 'count',
        label: 'Anzahl der Positionen',
        path: 'warenkorb.anzahl',
        origin: 'Warenkorb laden',
        source: 'ProcessData',
    },
    {
        id: 'total',
        label: 'Gesamtbetrag',
        path: 'warenkorb.gesamtbetrag',
        origin: 'Warenkorb laden',
        source: 'ProcessData',
    },
    {
        id: 'case-number',
        label: 'Aktenzeichen des Vorgangs',
        path: 'caseNumber',
        origin: 'Vorgang',
        source: 'ProtectedProcessData',
    },
];

function createInitialValue(): InputModeValue<string> {
    return {type: 'Literal', value: '42'};
}

function Harness(props: {
    initialValue?: InputModeValue<string>;
    onInsertVariable?: (variable: InputModeVariable) => void;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    allowedModes?: InputMode[];
    allowedVariableSources?: InputVariableSource[];
    dynamicTextVariableSources?: InputVariableSource[];
}) {
    const [value, setValue] = useState(props.initialValue ?? createInitialValue);

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
            busy={props.busy}
            allowedModes={props.allowedModes}
            allowedVariableSources={props.allowedVariableSources}
            dynamicTextVariableSources={props.dynamicTextVariableSources}
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
        Wert: 'Literal',
        Variable: 'Variable',
        'Ausdruck (No-Code)': 'NoCode',
        'Skript (Low-Code)': 'LowCode',
    };
    await user.click(screen.getByTestId(`input-mode-option-${modeByLabel[mode]}`));
}

describe('InputModeField', () => {
    it.each([
        ['Variable', 'Keine Variable referenziert'],
        ['Ausdruck (No-Code)', 'Kein Ausdruck definiert'],
        ['Skript (Low-Code)', 'Kein Skript definiert'],
    ])('shows a compact, unambiguous empty state for %s', async (mode, emptyText) => {
        const user = userEvent.setup();
        render(<Harness required error="Bitte ergänzen Sie die Eingabe."/>);

        await selectMode(user, mode);

        const summary = screen.getByRole('button', {name: new RegExp(`Inkrement: ${emptyText}`)});
        expect(within(summary).getAllByText(emptyText)).toHaveLength(1);
        expect(summary).toHaveAttribute('aria-haspopup', 'dialog');
        expect(summary).toHaveAttribute('aria-invalid', 'true');
        expect(summary).toHaveAccessibleDescription('Bitte ergänzen Sie die Eingabe.');
        expect(summary).toHaveAccessibleName(/Pflichtfeld/);
        expect(summary).toHaveStyle({minHeight: `${FormFieldTokens.controlMinHeight}px`});
        expect(summary.querySelectorAll('svg')).toHaveLength(2);
        summary.querySelectorAll('svg').forEach((icon) => expect(icon).toHaveAttribute('aria-hidden', 'true'));
    });

    it.each(['light', 'dark'] as const)('distinguishes the script preview from its label in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        render(<ThemeProvider theme={theme}>
            <Harness initialValue={{type: 'LowCode', code: '\n  4 + 9\n'}}/>
        </ThemeProvider>);

        const summary = screen.getByRole('button', {name: /Inkrement: Skript definiert/});
        const primary = within(summary).getByText('Skript definiert');
        const secondary = within(summary).getByText('4 + 9');
        expect(primary).toHaveStyle({color: theme.palette.text.primary});
        expect(secondary).toHaveStyle({color: theme.palette.text.secondary});
        expect(summary).toHaveAccessibleDescription('4 + 9');
        expect(summary).toHaveStyle({minHeight: `${FormFieldTokens.controlWithSecondaryTextMinHeight}px`});
    });

    it.each(['light', 'dark'] as const)('shows a humanized no-code preview on a secondary line in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        render(<ThemeProvider theme={theme}>
            <Harness initialValue={{type: 'NoCode', operand: {type: 'NoCodeProcessDataReference', path: 'warenkorb.anzahl'}}}/>
        </ThemeProvider>);

        const summary = screen.getByRole('button', {name: /Inkrement: Ausdruck definiert/});
        const preview = within(summary).getByText('Vorgangsdaten → warenkorb.anzahl');
        expect(preview).toHaveStyle({color: theme.palette.text.secondary});
        expect(preview).toHaveAttribute('title', 'Vorgangsdaten → warenkorb.anzahl');
        expect(summary).toHaveAccessibleDescription('Vorgangsdaten → warenkorb.anzahl');
        expect(summary).toHaveStyle({minHeight: `${FormFieldTokens.controlWithSecondaryTextMinHeight}px`});
    });

    it('does not repeat a custom reference when no suggestion supplies a label', () => {
        render(<Harness initialValue={{type: 'Variable', reference: {source: 'ProcessData', path: 'neuerWert'}}}/>);

        const summary = screen.getByRole('button', {name: /Inkrement: \$\.neuerWert/});
        expect(within(summary).getAllByText('$.neuerWert')).toHaveLength(1);
        expect(summary).toHaveAccessibleDescription('Vorgangsdaten');
    });

    it.each(['disabled', 'readOnly'] as const)('keeps %s scripts available for inspection', async (state) => {
        const user = userEvent.setup();
        render(<Harness {...{[state]: true}} initialValue={{type: 'LowCode', code: '4 + 9'}}/>);

        const summary = screen.getByRole('button', {name: /Inkrement: Skript definiert. Skript ansehen/});
        expect(summary).toBeEnabled();
        await user.click(summary);

        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByRole('textbox', {name: 'JavaScript für Inkrement'})).toHaveAttribute('readonly');
        expect(within(dialog).queryByRole('button', {name: 'Übernehmen'})).not.toBeInTheDocument();
        const closeButtons = within(dialog).getAllByRole('button', {name: 'Schließen'});
        expect(closeButtons).toHaveLength(2);
        closeButtons.forEach((button) => expect(button).toBeEnabled());
    });

    it('prevents opening a script while the field is busy', () => {
        render(<Harness busy initialValue={{type: 'LowCode', code: '4 + 9'}}/>);

        expect(screen.getByRole('button', {name: /Inkrement: Skript definiert/})).toBeDisabled();
    });

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
        await user.click(screen.getByRole('button', {name: /Inkrement: Keine Variable referenziert/}));
        expect(screen.getByText(/Die Vorschläge zeigen Variablen/)).toBeInTheDocument();
        expect(screen.getByLabelText('Variablenvorschläge')).toBeInTheDocument();
        const variableOption = await screen.findByLabelText(/Gesamtbetrag/);
        await user.click(variableOption);
        expect(variableOption).toBeChecked();
        await user.click(screen.getByTestId('use-variable-reference'));

        const summary = screen.getByLabelText(/Inkrement: Gesamtbetrag.*Variable referenzieren/);
        expect(summary).toHaveTextContent('Gesamtbetrag');
        expect(summary).toHaveTextContent('$.warenkorb.gesamtbetrag');
    });

    it('groups the primary and cancel actions before the clear action', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Keine Variable referenziert/}));

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

    it('applies dynamic-text source restrictions independently from variable mappings', async () => {
        const user = userEvent.setup();
        render(<Harness
            onInsertVariable={vi.fn()}
            allowedVariableSources={['ProcessData']}
            dynamicTextVariableSources={['ProtectedProcessData']}
        />);

        await user.click(screen.getByRole('button', {name: 'Platzhalter einfügen'}));

        expect(screen.getByText('Aktenzeichen des Vorgangs')).toBeInTheDocument();
        expect(screen.queryByText('Gesamtbetrag')).not.toBeInTheDocument();
        expect(screen.queryByTestId('variable-category-ProcessData')).not.toBeInTheDocument();
    });

    it('filters variables by their fachliche category', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Keine Variable referenziert/}));
        await user.click(screen.getByTestId('variable-category-ProtectedProcessData'));

        expect(screen.getByText('Aktenzeichen des Vorgangs')).toBeInTheDocument();
        expect(screen.queryByText('Gesamtbetrag')).not.toBeInTheDocument();
        expect(screen.getByText(/\$\$\.caseNumber/)).toBeInTheDocument();
    });

    it('creates an element-data reference from one source-relative path', async () => {
        const user = userEvent.setup();
        render(<Harness/>);

        await selectMode(user, 'Variable');
        await user.click(screen.getByRole('button', {name: /Inkrement: Keine Variable referenziert/}));
        await user.click(screen.getByRole('combobox', {name: /^Variablenquelle/}));
        await user.click(screen.getByRole('option', {name: 'Elementdaten'}));

        const pathInput = screen.getByRole('textbox', {name: /^Variablenpfad/});
        expect(pathInput.parentElement).toHaveTextContent('_.');
        await user.type(pathInput, 'previousNode.result.value');
        await user.click(screen.getByTestId('use-variable-reference'));

        expect(screen.getByLabelText(/Inkrement: _\.previousNode\.result\.value/)).toBeInTheDocument();
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

    it('omits the selector when the policy exposes only one mode', () => {
        render(<Harness allowedModes={['Literal']}/>);

        expect(screen.queryByRole('button', {name: /Eingabemodus für Inkrement ändern/})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: /^Inkrement/})).toBeInTheDocument();
    });

    it('preserves triggering element ids from the literal control', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<InputModeField
            label="Inkrement"
            variables={[]}
            value={{type: 'Literal', value: '1'}}
            onChange={onChange}
            renderLiteral={({onChange: changeLiteral}) => (
                <button onClick={() => changeLiteral('2', ['dependency'])}>Ändern</button>
            )}
        />);

        await user.click(screen.getByRole('button', {name: 'Ändern'}));

        expect(onChange).toHaveBeenCalledWith({type: 'Literal', value: '2'}, ['dependency']);
    });

    it('uses the first policy-approved source when entering variable mode', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<InputModeField
            label="Inkrement"
            variables={variables}
            allowedModes={['Literal', 'Variable']}
            allowedVariableSources={['ProtectedProcessData']}
            value={{type: 'Literal', value: '1'}}
            onChange={onChange}
            renderLiteral={({value, fieldProps}) => <TextFieldComponent
                {...fieldProps}
                value={value}
                onChange={() => undefined}
            />}
        />);

        await selectMode(user, 'Variable');

        expect(onChange).toHaveBeenCalledWith({
            type: 'Variable',
            reference: {source: 'ProtectedProcessData', path: ''},
        });
    });
});
