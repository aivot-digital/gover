import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {NoCodeInputFieldComponent} from './no-code-input-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {type AnyElement} from '../../models/elements/any-element';
import {type NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../models/functions/no-code-expression';
import {InputVariableSource} from '../../models/input-mode';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../view-dispatcher/view-dispatcher.context';

const noCodeApi = vi.hoisted(() => ({getNoCodeOperators: vi.fn()}));

vi.mock('../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../services/no-code-api-service', () => ({
    NoCodeApiService: class {
        getNoCodeOperators = noCodeApi.getNoCodeOperators;
    },
}));
vi.mock('../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper', () => ({
    NoCodeEditorWrapper: (props: { editable: boolean; label?: string }) => (
        <div data-testid="no-code-editor"
             data-editable={props.editable}>
            {props.label}
        </div>
    ),
}));

const rootElement = {
    type: ElementType.FormLayout,
    id: 'root',
    children: [],
} as unknown as AnyElement;

describe('NoCodeInputFieldComponent', () => {
    beforeEach(() => {
        noCodeApi.getNoCodeOperators.mockReset().mockResolvedValue([]);
    });

    it('exposes its summary and edit action as separate accessible concepts', async () => {
        const user = userEvent.setup();

        render(
            <NoCodeInputFieldComponent
                rootElement={rootElement}
                label="Freigabebedingung"
                value={null}
                onChange={vi.fn()}
                desiredReturnType={NoCodeDataType.Boolean}
                hint="Legt fest, ob eine Freigabe erforderlich ist."
            />,
        );

        const summary = screen.getByRole('group', {name: 'Freigabebedingung – optional'});
        expect(summary).toHaveTextContent('Kein Ausdruck definiert');
        expect(summary).toHaveAccessibleDescription('Legt fest, ob eine Freigabe erforderlich ist.');
        expect(getComputedStyle(summary).height).toBe('44px');
        expect(getComputedStyle(summary).borderStyle).toBe('dashed');
        expect(getComputedStyle(summary).backgroundColor).toBe('rgba(0, 0, 0, 0)');
        expect(summary.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');

        const editButton = screen.getByRole('button', {name: 'Bearbeiten'});
        expect(editButton.closest('[data-form-field-label-action]')).not.toBeNull();
        expect(editButton).not.toHaveClass('MuiButton-outlined');
        await user.click(editButton);

        const dialog = document.getElementById(editButton.getAttribute('aria-controls')!);
        expect(dialog).toHaveAttribute('role', 'dialog');
        expect(dialog).toHaveAccessibleName('Freigabebedingung');
        expect(screen.getByTestId('no-code-editor')).toHaveAttribute('data-editable', 'true');
    });

    it('uses a preview and dialog in compact process configurations', async () => {
        render(<NoCodeInputFieldComponent rootElement={{
            ...rootElement,
            type: ElementType.GroupLayout,
        } as unknown as AnyElement}
                                          label="Bedingung"
                                          value={null}
                                          onChange={vi.fn()}
                                          desiredReturnType={NoCodeDataType.Boolean}/>);
        expect(screen.queryByTestId('no-code-editor')).not.toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'Bedingung – optional'})).toHaveTextContent('Kein Ausdruck definiert');
        await userEvent.setup().click(screen.getByRole('button', {name: 'Bearbeiten'}));
        expect(await screen.findByRole('dialog', {name: 'Bedingung'})).toBeInTheDocument();
        expect(screen.getByTestId('no-code-editor')).toBeInTheDocument();
    });

    it('shows the readable nested sentence with context labels while keeping its compact field layout', async () => {
        const equals: NoCodeOperatorDetailsDTO = {
            identifier: 'equals',
            packageName: 'core',
            label: 'Ist gleich',
            abstractDescription: '',
            description: '',
            tags: [],
            sentenceLead: 'ergibt wahr',
            humanReadableTemplate: '„#0“ ist gleich „#1“',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [
                    {label: 'Wert 1', type: NoCodeDataType.Runtime, description: null, options: []},
                    {label: 'Wert 2', type: NoCodeDataType.Runtime, description: null, options: [{label: 'Ja', value: 'ja'}]},
                ],
            }],
        };
        noCodeApi.getNoCodeOperators.mockResolvedValue([equals, {
            ...equals,
            identifier: 'not',
            label: 'Nicht',
            humanReadableTemplate: 'nicht „#0“',
            signatures: [{returnType: NoCodeDataType.Boolean, parameters: equals.signatures[0].parameters.slice(0, 1)}],
        }]);
        const operand: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [{
                type: 'NoCodeExpression',
                operatorIdentifier: 'equals',
                operands: [
                    {type: 'NoCodeProcessDataReference', path: 'befreiung.beantragt'},
                    {type: 'NoCodeStaticValue', value: 'ja'},
                ],
            }],
        };
        const onChange = vi.fn();
        render(<ViewDispatcherContextProvider value={{
            mode: ViewDispatcherMode.Viewer,
            rootElement,
            allElements: [rootElement],
            rootAuthoredElementValues: {},
            rootDerivedData: createDerivedRuntimeElementData(),
            inputModeVariables: [{source: InputVariableSource.ProcessData, path: 'befreiung.beantragt', label: 'Befreiung beantragt'}],
        }}>
            <NoCodeInputFieldComponent rootElement={rootElement}
                                      label="Freigabebedingung"
                                      value={{noCode: operand}}
                                      onChange={onChange}
                                      desiredReturnType={NoCodeDataType.Boolean}
                                      hint="Legt fest, ob eine Freigabe erforderlich ist."/>
        </ViewDispatcherContextProvider>);

        const text = 'nicht („Befreiung beantragt“ ist gleich „Ja“)';
        const preview = await screen.findByText(text);
        const summary = screen.getByRole('group', {name: 'Freigabebedingung – optional'});
        expect(summary).toContainElement(preview);
        expect(preview).toHaveAttribute('title', text);
        expect(summary).toHaveAccessibleDescription('Legt fest, ob eine Freigabe erforderlich ist.');
        expect(getComputedStyle(summary).borderStyle).toBe('solid');
        expect(summary).not.toHaveTextContent('$.befreiung.beantragt');
        expect(summary).not.toHaveTextContent('„ja“');
        expect(summary).not.toHaveTextContent('ergibt wahr');
        expect(screen.queryByRole('button', {name: 'Technische Details'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('renders one shared error and a view action in read-only mode', () => {
        render(
            <NoCodeInputFieldComponent
                rootElement={rootElement}
                label="Freigabebedingung"
                value={null}
                onChange={vi.fn()}
                desiredReturnType={NoCodeDataType.Boolean}
                error="Definieren Sie einen gültigen Ausdruck."
                readOnly
            />,
        );

        expect(screen.getByRole('group', {name: 'Freigabebedingung – optional'}))
            .toHaveAttribute('aria-invalid', 'true');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
        expect(screen.getByRole('button', {name: 'Ansehen'})).toBeInTheDocument();
    });
});
