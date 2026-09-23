import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {type NoCodeExpression} from '../../../models/functions/no-code-expression';
import {BaseApiService} from '../../../services/base-api-service';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
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
});
