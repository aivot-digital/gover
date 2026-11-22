import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementType} from '../../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {elementNoCodeFunctions, validateNoCode} from './validate-no-code';

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    packageName: 'core',
    label: 'Vergleich',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: Array.from({length: 2}, () => ({
            type: NoCodeDataType.Runtime,
            label: 'Wert',
            description: '',
            options: [],
        })),
    }],
};
const validate = (operand: NoCodeOperand | null, required = true) => validateNoCode(operand, [operator], [], required);

describe('validateNoCode', () => {
    it('distinguishes a missing expression from an intentional empty, false or zero value', () => {
        expect(validate(null).isValid).toBe(false);
        expect(validate(null, false).isValid).toBe(true);
        for (const value of [null, '', 'false', '0', false, 0]) {
            expect(validate({
                type: 'NoCodeStaticValue',
                value,
            } as NoCodeOperand).isValid).toBe(true);
        }
    });

    it('rejects unknown operators, missing operands and unsupported counts', () => {
        const valid: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [{
                type: 'NoCodeStaticValue',
                value: '0',
            }, {
                type: 'NoCodeStaticValue',
                value: '0',
            }],
        };
        expect(validate(valid).isValid).toBe(true);
        expect(validate({
            ...valid,
            operatorIdentifier: 'missing',
        }).isValid).toBe(false);
        expect(validate({
            ...valid,
            operands: [null, valid],
        }).subErrors?.[0].error).toBeTruthy();
        expect(validate({
            ...valid,
            operands: [valid],
        }).isValid).toBe(false);
        expect(validate({
            ...valid,
            operands: [],
        }).isValid).toBe(false);
    });

    it('requires existing element references', () => {
        const element = {
            ...generateElementWithDefaultValues(ElementType.Text),
            id: 'field',
        };
        const reference: NoCodeOperand = {
            type: 'NoCodeReference',
            elementId: 'field',
        };
        expect(validate(reference).isValid).toBe(false);
        expect(validateNoCode(reference, [], [{
            element,
            parents: [],
        }]).isValid).toBe(true);
    });

    it.each([
        [{type: 'NoCodeExpression', operatorIdentifier: null, operands: []}, 'Wählen Sie eine Funktion aus.'],
        [{type: 'NoCodeExpression', operatorIdentifier: 'missing', operands: []}, 'Die ausgewählte Funktion ist nicht verfügbar.'],
        [{type: 'NoCodeExpression', operatorIdentifier: 'equals', operands: []}, 'Die Anzahl der Parameter passt nicht zur Funktion.'],
        [{type: 'NoCodeReference', elementId: null}, 'Wählen Sie ein Formularelement aus.'],
        [{type: 'NoCodeReference', elementId: 'missing'}, 'Das ausgewählte Formularelement ist nicht verfügbar.'],
    ] satisfies [NoCodeOperand, string][])('names the function or form element in the error for %j', (operand, error) => {
        expect(validate(operand).error).toBe(error);
    });

    it.each([
        [{
            type: 'NoCodeProcessDataReference',
            path: '',
        }, false],
        [{
            type: 'NoCodeProcessDataReference',
            path: 'people[*].name',
        }, true],
        [{
            type: 'NoCodeProcessDataReference',
            path: '[0].name',
        }, false],
        [{
            type: 'NoCodeInstanceDataReference',
            path: 'people[*].name',
        }, false],
        [{
            type: 'NoCodeInstanceDataReference',
            path: 'people[0].name',
        }, true],
        [{
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'step_1',
            path: '[0].name',
        }, true],
        [{
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'step_1',
            path: '',
        }, false],
        [{
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'bad.key',
            path: 'name',
        }, false],
        [{
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'step_1',
            path: 'people[*].name',
        }, false],
        [{
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'step_1',
            path: 'people[2147483648]',
        }, false],
    ])('validates reference paths against the backend grammar: %j', (operand, valid) => {
        expect(validate(operand as NoCodeOperand).isValid).toBe(valid);
    });

    it('collects all selected functions even when they are empty or in another tab', () => {
        const empty = {
            type: 'NoCode' as const,
            requirements: null,
            referencedIds: [],
            conditionSet: null,
            javascriptCode: null,
            noCode: null,
        };
        const element = {
            ...generateElementWithDefaultValues(ElementType.Text),
            visibility: empty,
            value: empty,
            validation: {
                ...empty,
                noCodeList: [],
            },
        };
        expect(elementNoCodeFunctions(element).map(({section}) => section))
            .toEqual(['visibility', 'value', 'validation']);
    });
});
