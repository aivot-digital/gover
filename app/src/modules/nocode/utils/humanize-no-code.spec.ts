import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementType} from '../../../data/element-type/element-type';
import {InputVariableSource} from '../../../models/input-mode';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {humanizeNoCode, summarizeNoCodeParameters} from './humanize-no-code';

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'compare',
    packageName: 'core',
    label: 'Vergleich',
    abstractDescription: '',
    description: '',
    tags: [],
    humanReadableTemplate: '„#0“ ist gleich „#1“',
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [0, 1].map((i) => ({
            type: NoCodeDataType.Runtime,
            label: `Wert ${i + 1}`,
            description: null,
            options: [],
        })),
    }],
};
const literal = (value: unknown) => ({
    type: 'NoCodeStaticValue',
    value,
}) as NoCodeOperand;
const expr = (...operands: Array<NoCodeOperand | null>): NoCodeOperand => ({
    type: 'NoCodeExpression',
    operatorIdentifier: 'compare',
    operands,
});
const humanize = (operand: NoCodeOperand | null, op = operator) => humanizeNoCode(operand, [], [op]);

describe('humanizeNoCode', () => {
    it('replaces placeholders atomically and removes only the template quotes around them', () => {
        expect(humanize(expr(literal('Text #1 mit "Zitat"'), literal('#0'))))
            .toBe('„Text #1 mit "Zitat"“ ist gleich „#0“');
    });
    it('handles multi-digit placeholders without matching their prefix', () => {
        const params = Array.from({length: 11}, (_, i) => ({
            ...operator.signatures[0].parameters[0],
            label: `Wert ${i}`,
        }));
        const op = {
            ...operator,
            humanReadableTemplate: '#10 / #1 / ' + params.map((_, i) => `#${i}`).join(' / '),
            signatures: [{
                returnType: NoCodeDataType.String,
                parameters: params,
            }],
        };
        expect(humanize(expr(...params.map((_, i) => literal(`v${i}`))), op)).toMatch(/^„v10“ \/ „v1“ \/ „v0“/);
    });
    it('distinguishes missing, empty, boolean, numeric and textual values', () => {
        expect(humanize(null)).toBe('Kein Ausdruck definiert');
        expect(humanize(literal(null))).toBe('Leerer Wert');
        expect(humanize(literal(''))).toBe('Leerer Text');
        expect(humanize(literal(false))).toBe('Falsch');
        expect(humanize(literal(0))).toBe('0');
        expect(humanize(literal('0'))).toBe('„0“');
        expect(humanize(expr(null, literal(false)))).toBe('Fehlender Parameter „Wert 1“ ist gleich Falsch');
    });
    it('groups nested expressions and falls back to sentence metadata without a template', () => {
        expect(humanize(expr(expr(literal('a'), literal('b')), literal(true)))).toBe('(„a“ ist gleich „b“) ist gleich Wahr');
        const op = {
            ...operator,
            humanReadableTemplate: null,
            sentenceLead: 'ergibt wahr',
        };
        expect(humanize(expr(literal('a'), literal('b')), op)).toBe('Vergleich ergibt wahr: Wert 1 „a“; Wert 2 „b“');
        expect(humanize({
            ...expr(literal('a')),
            operatorIdentifier: 'unknown',
        } as NoCodeOperand)).toContain('Unbekannter Operator (unknown): „a“');
    });
    it('names unknown references and never hides omitted or additional parameters', () => {
        expect(humanize({
            type: 'NoCodeReference',
            elementId: 'deleted',
        })).toBe('Unbekanntes Element (ID: deleted)');
        expect(humanize(expr(literal('a'), literal('b')), {
            ...operator,
            humanReadableTemplate: '#0',
        })).toContain('Weitere Parameter: „b“');
        expect(humanize(expr(literal('a')))).toContain('Fehlender Parameter „Wert 2“');
    });
    it('uses selection labels without losing the technical value', () => {
        const field = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'state',
            label: 'Bundesland',
            options: [{
                value: 'BE',
                label: 'Berlin',
            }],
        };
        expect(humanizeNoCode(expr({
            type: 'NoCodeReference',
            elementId: 'state',
        }, literal('BE')), [{
            element: field,
            parents: [],
        }], [operator]))
            .toBe('Wert von „Bundesland“ ist gleich „Berlin“ (BE)');
    });
    it('uses available variable labels and retains the exact reference path', () => {
        const ref: NoCodeOperand = {
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'form',
            path: 'name',
        };
        expect(humanizeNoCode(ref, [], [], {
            variables: [{
                source: InputVariableSource.ElementData,
                nodeDataKey: 'form',
                path: 'name',
                label: 'Name',
            }],
        }))
            .toBe('„Name“ (_.form.name)');
    });
});


describe('collapsed operator sentence', () => {
    const sentenceOperator = {
        ...operator,
        humanReadableTemplate: 'THIS MUST NOT BE REPEATED',
        sentenceLead: 'ergibt wahr',
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [
                {
                    ...operator.signatures[0].parameters[0],
                    sentencePhrase: 'wenn',
                },
                {
                    ...operator.signatures[0].parameters[1],
                    sentencePhrase: 'gleich',
                    sentenceSuffix: 'ist',
                },
            ],
        }],
    };
    it('continues with parameters and suffixes without repeating the current operator', () => {
        expect(summarizeNoCodeParameters(expr(literal('a'), literal('b')), [], [sentenceOperator]))
            .toBe('wenn „a“ gleich „b“ ist');
    });
    it('preserves grouping, nested sentence leads and missing values', () => {
        expect(summarizeNoCodeParameters(expr(expr(literal('a'), null), literal('b')), [], [sentenceOperator]))
            .toBe('wenn (Vergleich ergibt wahr wenn „a“ gleich Fehlender Parameter „Wert 2“ ist) gleich „b“ ist');
    });
    it('keeps unknown operators and parameter-label fallbacks understandable', () => {
        expect(summarizeNoCodeParameters(expr(literal('a'), literal('b')), [], [operator]))
            .toBe('Wert 1: „a“ Wert 2: „b“');
        expect(summarizeNoCodeParameters({
            type: 'NoCodeExpression',
            operands: [literal('a')],
            operatorIdentifier: 'unknown',
        }, [], []))
            .toBe('Parameter 1: „a“');
    });
});
