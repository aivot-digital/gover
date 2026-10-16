import {fireEvent, render, screen} from '@testing-library/react';
import {type ReactElement} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {type NoCodeExpression} from '../../../models/functions/no-code-expression';
import {BaseApiService} from '../../../services/base-api-service';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {CodeListValueMode} from '../../code-lists/enums/code-list-value-mode';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../../process/entities/process-node-definition-metadata';
import {ProcessNodeEditorProvider} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {NoCodeOperandEditorExpression} from './no-code-operand-editor-expression';

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
            ...generateElementWithDefaultValues(ElementType.Radio),
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
});

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
