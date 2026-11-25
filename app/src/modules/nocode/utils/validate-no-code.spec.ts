import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementType} from '../../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {FormLayoutElement} from '../../../models/elements/form-layout-element';
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

    it('accepts extra operands only for variadic operators and still validates every child', () => {
        const variadic = {...operator, identifier: 'concat', variableArgumentCount: true};
        const value = {type: 'NoCodeStaticValue', value: 'A'} as const;
        const check = (id: string, operands: (NoCodeOperand | null)[]) => validateNoCode(
            {type: 'NoCodeExpression', operatorIdentifier: id, operands}, [operator, variadic], [],
        );
        expect(check('concat', [value, value, value]).isValid).toBe(true);
        expect(check('equals', [value, value, value]).isValid).toBe(false);
        expect(check('concat', [value]).isValid).toBe(false);
        expect(check('concat', [value, value, null]).subErrors?.[2].isValid).toBe(false);
    });

    it('validates declared number constraints on static operands without evaluating dynamic sources', () => {
        const countedOperator: NoCodeOperatorDetailsDTO = {
            ...operator,
            identifier: 'counted',
            signatures: [{
                returnType: NoCodeDataType.Date,
                parameters: [{
                    type: NoCodeDataType.Number,
                    label: 'Anzahl',
                    description: '',
                    options: [],
                    numberConstraints: {integerOnly: true, minimum: -2147483648, maximum: 2147483647},
                }],
            }],
        };
        const withCount = (count: NoCodeOperand): NoCodeOperand => ({
            type: 'NoCodeExpression',
            operatorIdentifier: 'counted',
            operands: [count],
        });
        for (const value of ['1.5', '2147483648', null]) {
            const result = validateNoCode(withCount({type: 'NoCodeStaticValue', value}), [countedOperator], []);
            expect(result.isValid).toBe(false);
            expect(result.subErrors?.[0].error).toContain('„Anzahl“');
        }
        for (const count of [
            {type: 'NoCodeStaticValue', value: '5.0'},
            {type: 'NoCodeStaticValue', value: '0'},
            {type: 'NoCodeProcessDataReference', path: 'duration'},
            {type: 'NoCodeExpression', operatorIdentifier: 'equals', operands: [
                {type: 'NoCodeStaticValue', value: 'a'},
                {type: 'NoCodeStaticValue', value: 'b'},
            ]},
        ] satisfies NoCodeOperand[]) {
            expect(validateNoCode(withCount(count), [countedOperator, operator], []).isValid).toBe(true);
        }
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

    it('requires that visibility checks name an element of the form, also one without input', () => {
        const section = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'section',
            children: [{
                ...generateElementWithDefaultValues(ElementType.Text),
                id: 'field',
            }],
        };
        const form = {
            ...(generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement),
            id: 'form',
            children: [section],
        };
        const isVisible: NoCodeOperatorDetailsDTO = {
            ...operator,
            identifier: 'is-visible',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [{type: NoCodeDataType.String, label: 'Formularelement', description: '', options: []}],
            }],
        };
        const elements = [{element: section.children[0], parents: [form, section]}];
        const check = (id: string) => validateNoCode({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-visible',
            operands: [{type: 'NoCodeStaticValue', value: id}],
        }, [isVisible], elements);

        expect(check('field').isValid).toBe(true);
        expect(check('section').isValid).toBe(true);
        expect(check('typo').subErrors?.[0].error).toBe('Das ausgewählte Formularelement ist nicht verfügbar.');
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
