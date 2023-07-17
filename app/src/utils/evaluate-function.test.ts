import {evaluateFunction} from "./evaluate-function";
import {ElementType} from "../data/element-type/element-type";
import {ConditionSetOperator} from "../data/condition-set-operator";
import {ConditionOperator} from "../data/condition-operator";
import {AnyElement} from "../models/elements/any-element";
import {Function} from "../models/functions/function";

// TODO: Fix tests

const element: AnyElement = {
    id: '',
    type: ElementType.Checkbox,
    appVersion: 'test',
};

const customerData = {
    'undefined': undefined,

    'boolean-id1': true,
    'boolean-id2': false,
    'boolean-text-id1': 'Ja',
    'boolean-text-id2': 'Nein',

    'text-id1': 'test',
    'text-id2': 'test',
    'text-id3': 'test-id3',

    'num-id1': 12,
    'num-id2': 12,
    'num-id3': 12.0,
    'num-id4': 0,

    'num-text-id1': '12',
    'num-text-id3': '12.0',
    'num-text-id4': '0',
};

function newNoCodeFunc(id1: keyof typeof customerData, id2: keyof typeof customerData): Function {
    return {
        requirements: '',
        conditionSet: {
            operator: ConditionSetOperator.All,
            conditions: [{
                operator: ConditionOperator.Equals,
                reference: id1,
                target: id2,
            }],
        },
    };
}

const evalAbbr = (allElements: AnyElement[], id1: keyof typeof customerData, id2: keyof typeof customerData) => evaluateFunction(
    undefined,
    allElements,
    newNoCodeFunc(id1, id2),
    customerData,
    element,
    '',
    true,
);

test('test evaluate code function', () => {
    const resTrue = evaluateFunction(
        undefined, [], {
            requirements: '',
            code: `
        function main(data, element, id) {
            return data[id];
        }
        `,
        }, customerData, element, 'boolean-id1', true);
    expect(resTrue).toBe(true);

    const resFalse = evaluateFunction(
        undefined, [], {
            requirements: '',
            code: `
        function main(data, element, id) {
            return data[id] == data['boolean-id2'];
        }
        `,
        }, customerData, element, 'boolean-id1', true);
    expect(resFalse).toBe(false);
});

test('test condition evaluate no-code equals boolean', () => {
    expect(
        evalAbbr([], 'boolean-id1', 'boolean-id1'),
    ).toBe(true);

    expect(
        evalAbbr([], 'boolean-id1', 'boolean-id2'),
    ).toBe(false);

    expect(
        evalAbbr([], 'boolean-id1', 'undefined'),
    ).toBe(false);
});

test('test condition evaluate no-code equals boolean -- type --> text', () => {
    expect(
        evalAbbr([], 'boolean-id1', 'boolean-text-id1'),
    ).toBe(true);

    expect(
        evalAbbr([], 'boolean-id2', 'boolean-text-id2'),
    ).toBe(true);

    expect(
        evalAbbr([], 'boolean-id1', 'boolean-text-id2'),
    ).toBe(false);
});

test('test condition evaluate no-code equals text', () => {
    expect(
        evalAbbr([], 'text-id1', 'text-id1'),
    ).toBe(true);

    expect(
        evalAbbr([], 'text-id1', 'text-id3'),
    ).toBe(false);

    expect(
        evalAbbr([], 'text-id1', 'undefined'),
    ).toBe(false);
});

test('test condition evaluate no-code equals number', () => {
    expect(
        evalAbbr([], 'num-id1', 'num-id2'),
    ).toBe(true);

    expect(
        evalAbbr([], 'num-id1', 'num-id3'),
    ).toBe(true);

    expect(
        evalAbbr([], 'num-id1', 'num-id4'),
    ).toBe(false);

    expect(
        evalAbbr([], 'num-id1', 'num-id4'),
    ).toBe(false);

    expect(
        evalAbbr([], 'num-id1', 'undefined'),
    ).toBe(false);
});

test('test condition evaluate no-code equals number -- type --> text', () => {
    expect(
        evalAbbr([], 'num-id1', 'num-text-id1'),
    ).toBe(true);

    expect(
        evalAbbr([], 'num-id2', 'num-text-id3'),
    ).toBe(true);

    expect(
        evalAbbr([], 'num-id1', 'num-text-id4'),
    ).toBe(false);
});

