import {fireEvent, render, screen} from '@testing-library/react';
import {type ReactElement} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {type NoCodeExpression, type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {BaseApiService} from '../../../services/base-api-service';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {CodeListValueMode} from '../../code-lists/enums/code-list-value-mode';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../../process/entities/process-node-definition-metadata';
import {ProcessNodeEditorProvider} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {NoCodeOperandEditorExpression} from './no-code-operand-editor-expression';

// The data path's copy action uses the app dispatch; these editor tests render without a Redux store.
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    packageName: 'test',
    label: 'Vergleichen',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [
            {
                type: NoCodeDataType.String,
                label: 'Auswahl',
                description: null,
                options: [],
            },
            {
                type: NoCodeDataType.String,
                label: 'Vergleichswert',
                description: null,
                options: [],
            },
        ],
    }],
};

describe('NoCodeOperandEditorExpression', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('offers codelist values to a static operand and stores the technical value', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
            {label: 'Hamburg', value: 'HH'},
        ]);
        const element = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'state',
            label: 'Bundesland',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
            options: [{label: 'Veraltet', value: 'old'}],
        };
        const expression: NoCodeExpression = {
            type: 'NoCodeExpression',
            operatorIdentifier: operator.identifier,
            operands: [
                {type: 'NoCodeReference', elementId: element.id},
                {type: 'NoCodeStaticValue', value: 'BE'},
            ],
        };
        const onChange = vi.fn();

        render(
            <NoCodeOperandEditorExpression
                allElements={[{element, parents: []}]}
                allOperators={[operator]}
                label="Sichtbarkeit"
                value={expression}
                onChange={onChange}
                onAddEnclosingExpression={vi.fn()}
            />,
        );

        const input = await screen.findByRole('combobox', {name: /^Vergleichswert/});
        expect(input).toHaveValue('BE');
        expect(input).toHaveAccessibleDescription('Der Wert entspricht der Auswahloption „Berlin“.');
        fireEvent.mouseDown(input);
        fireEvent.click(await screen.findByRole('option', {name: /Hamburg/}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [
                expression.operands![0],
                {type: 'NoCodeStaticValue', value: 'HH'},
            ],
        });
        expect(screen.queryByRole('option', {name: /Veraltet/})).not.toBeInTheDocument();
    });

    it('offers the value options of a referenced process data key', async () => {
        const expression = createProcessDataComparison('person.status', 'single');
        const onChange = vi.fn();

        renderInProcessNodeEditor(
            <NoCodeOperandEditorExpression
                allElements={[]}
                allOperators={[operator]}
                label="Bedingung"
                value={expression}
                onChange={onChange}
                onAddEnclosingExpression={vi.fn()}
                contextType="PROCESS"
            />,
            [forwardedKey('person.status', {
                valueOptions: [
                    {value: 'single', label: 'Ledig'},
                    {value: 'married', label: 'Verheiratet'},
                ],
            })],
        );

        const input = await screen.findByRole('combobox', {name: /^Vergleichswert/});
        expect(input).toHaveValue('single');
        expect(input).toHaveAccessibleDescription('Der Wert entspricht der Auswahloption „Ledig“.');
        fireEvent.mouseDown(input);
        fireEvent.click(await screen.findByRole('option', {name: /Verheiratet/}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [
                expression.operands![0],
                {type: 'NoCodeStaticValue', value: 'married'},
            ],
        });
    });

    it('offers codelist labels as values for a referenced chip input row', async () => {
        const get = vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue(['Deutsch', 'Englisch']);
        const expression = createProcessDataComparison('people[0].languages', 'Deutsch');
        const onChange = vi.fn();

        renderInProcessNodeEditor(
            <NoCodeOperandEditorExpression
                allElements={[]}
                allOperators={[operator]}
                label="Bedingung"
                value={expression}
                onChange={onChange}
                onAddEnclosingExpression={vi.fn()}
                contextType="PROCESS"
            />,
            [forwardedKey('people[*].languages', {
                codeListKey: 'languages',
                codeListValueMode: CodeListValueMode.LabelOnly,
            })],
        );

        const input = await screen.findByRole('combobox', {name: /^Vergleichswert/});
        fireEvent.mouseDown(input);
        fireEvent.click(await screen.findByRole('option', {name: /Englisch/}));

        expect(get).toHaveBeenCalledWith('/api/public/code-lists/languages/chip-input/', {skipAuthCheck: true});
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [
                expression.operands![0],
                {type: 'NoCodeStaticValue', value: 'Englisch'},
            ],
        });
    });

    it('offers no process data options outside a process node editor', () => {
        render(
            <NoCodeOperandEditorExpression
                allElements={[]}
                allOperators={[operator]}
                label="Bedingung"
                value={createProcessDataComparison('person.status', 'single')}
                onChange={vi.fn()}
                onAddEnclosingExpression={vi.fn()}
                contextType="PROCESS"
            />,
        );

        expect(screen.queryByRole('combobox', {name: /^Vergleichswert/})).not.toBeInTheDocument();
    });

    it('keeps the options of the operator when a referenced process data key has options', async () => {
        renderInProcessNodeEditor(
            <NoCodeOperandEditorExpression
                allElements={[]}
                allOperators={[addToDateOperator]}
                label="Frist"
                value={createAddToDate({type: 'NoCodeProcessDataReference', path: 'deadlineDays'})}
                onChange={vi.fn()}
                onAddEnclosingExpression={vi.fn()}
                contextType="PROCESS"
            />,
            [forwardedKey('deadlineDays', {
                valueOptions: [
                    {value: '7', label: '7 Tage'},
                    {value: '14', label: '14 Tage'},
                ],
            })],
        );

        await expectOnlyUnitOptions();
    });

    it('keeps the options of the operator when a referenced form field has options', async () => {
        const element = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'deadline',
            label: 'Frist',
            options: [
                {value: '7', label: '7 Tage'},
                {value: '14', label: '14 Tage'},
            ],
        };

        render(
            <NoCodeOperandEditorExpression
                allElements={[{element, parents: []}]}
                allOperators={[addToDateOperator]}
                label="Frist"
                value={createAddToDate({type: 'NoCodeReference', elementId: element.id})}
                onChange={vi.fn()}
                onAddEnclosingExpression={vi.fn()}
            />,
        );

        await expectOnlyUnitOptions();
    });

    it('offers referenced options to a parameter that accepts any type', async () => {
        const listOperator: NoCodeOperatorDetailsDTO = {
            ...operator,
            identifier: 'listContains',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [
                    {type: NoCodeDataType.List, label: 'Liste', description: null, options: []},
                    {type: NoCodeDataType.Runtime, label: 'Gesuchter Wert', description: null, options: []},
                ],
            }],
        };
        const element = {
            ...generateElementWithDefaultValues(ElementType.MultiCheckbox),
            id: 'documents',
            label: 'Nachweise',
            options: [
                {value: 'id', label: 'Personalausweis'},
                {value: 'passport', label: 'Reisepass'},
            ],
        };

        render(
            <NoCodeOperandEditorExpression
                allElements={[{element, parents: []}]}
                allOperators={[listOperator]}
                label="Bedingung"
                value={{
                    type: 'NoCodeExpression',
                    operatorIdentifier: listOperator.identifier,
                    operands: [
                        {type: 'NoCodeReference', elementId: element.id},
                        {type: 'NoCodeStaticValue', value: 'id'},
                    ],
                }}
                onChange={vi.fn()}
                onAddEnclosingExpression={vi.fn()}
            />,
        );

        fireEvent.mouseDown(await screen.findByRole('combobox', {name: /^Gesuchter Wert/}));
        expect(await screen.findByRole('option', {name: /Reisepass/})).toBeInTheDocument();
    });
});

const addToDateOperator: NoCodeOperatorDetailsDTO = {
    identifier: 'addToDate',
    packageName: 'test',
    label: 'Zu Datum hinzufügen',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Date,
        parameters: [
            {type: NoCodeDataType.Date, label: 'Datum', description: null, options: []},
            {type: NoCodeDataType.Number, label: 'Anzahl', description: null, options: []},
            {
                type: NoCodeDataType.String,
                label: 'Einheit',
                description: null,
                options: [
                    {value: 'DAYS', label: 'Tage'},
                    {value: 'WEEKS', label: 'Wochen'},
                    {value: 'MONTHS', label: 'Monate'},
                    {value: 'YEARS', label: 'Jahre'},
                ],
            },
        ],
    }],
};

function createAddToDate(amount: NoCodeOperand): NoCodeExpression {
    return {
        type: 'NoCodeExpression',
        operatorIdentifier: addToDateOperator.identifier,
        operands: [
            {type: 'NoCodeStaticValue', value: '2026-01-01'},
            amount,
            {type: 'NoCodeStaticValue', value: 'DAYS'},
        ],
    };
}

/**
 * The unit offers only the options of the operator, and the date, which has another type than the referenced amount,
 * gets no suggestions.
 */
async function expectOnlyUnitOptions() {
    fireEvent.mouseDown(await screen.findByRole('combobox', {name: /^Einheit/}));
    for (const unit of ['Tage', 'Wochen', 'Monate', 'Jahre']) {
        expect(await screen.findByRole('option', {name: new RegExp(`^${unit}`)})).toBeInTheDocument();
    }
    expect(screen.queryByRole('option', {name: /14 Tage/})).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', {name: /^Datum/})).not.toBeInTheDocument();
}

function createProcessDataComparison(path: string, value: string): NoCodeExpression {
    return {
        type: 'NoCodeExpression',
        operatorIdentifier: operator.identifier,
        operands: [
            {type: 'NoCodeProcessDataReference', path},
            {type: 'NoCodeStaticValue', value},
        ],
    };
}

function forwardedKey(
    processDataKey: string,
    valueSource: Partial<ProcessNodeDefinitionMetadataForwardedProcessDataKey>,
): ProcessNodeDefinitionMetadataForwardedProcessDataKey {
    return {
        processDataKey,
        label: processDataKey,
        subLabel: null,
        origin: {name: 'Formular'} as ProcessNodeDefinitionMetadataForwardedProcessDataKey['origin'],
        ...valueSource,
    };
}

function renderInProcessNodeEditor(
    ui: ReactElement,
    forwardedProcessDataKeys: ProcessNodeDefinitionMetadataForwardedProcessDataKey[],
) {
    return render(
        <ProcessNodeEditorProvider
            value={{
                provider: {} as any,
                layout: {} as any,
                testClaim: null,
                node: {} as any,
                setNode: vi.fn(),
                isEditable: true,
                problems: null,
                incomingMetadataLoadFailed: false,
                incomingMetadata: {
                    reusableUiDefinitions: [],
                    forwardedAttachmentSets: [],
                    forwardedProcessDataKeys,
                    forwardedIdentities: [],
                },
            }}
        >
            {ui}
        </ProcessNodeEditorProvider>,
    );
}
