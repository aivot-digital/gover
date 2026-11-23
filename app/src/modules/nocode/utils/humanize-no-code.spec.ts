import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementType} from '../../../data/element-type/element-type';
import {InputVariableSource} from '../../../models/input-mode';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {FormLayoutElement} from '../../../models/elements/form-layout-element';
import {humanizeNoCode} from './humanize-no-code';

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

describe('visibility checks', () => {
    it('show the name of the checked element instead of its ID', () => {
        const section = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'section',
            name: 'Angaben zur Person',
        };
        const field = {
            ...generateElementWithDefaultValues(ElementType.Text),
            id: 'field',
        };
        const form = {
            ...(generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement),
            id: 'form',
            children: [{...section, children: [field]}],
        };
        const isVisible: NoCodeOperatorDetailsDTO = {
            identifier: 'is-visible',
            packageName: 'core',
            label: 'Ist sichtbar',
            description: '',
            abstractDescription: '',
            humanReadableTemplate: 'das Formularelement #0 ist sichtbar',
            tags: [],
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [{type: NoCodeDataType.String, label: 'Formularelement', description: '', options: []}],
            }],
        };

        expect(humanizeNoCode({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-visible',
            operands: [{type: 'NoCodeStaticValue', value: 'section'}],
        }, [{element: field, parents: [form, section]}], [isVisible])).toBe('das Formularelement „Angaben zur Person“ (section) ist sichtbar');
    });
});

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
        } as NoCodeOperand)).toContain('Unbekannte Funktion (unknown): „a“');
        expect(humanize({
            ...expr(),
            operatorIdentifier: null,
        } as NoCodeOperand)).toBe('Keine Funktion ausgewählt');
    });
    it('names unknown references and never hides omitted or additional parameters', () => {
        expect(humanize({
            type: 'NoCodeReference',
            elementId: 'deleted',
        })).toBe('Unbekanntes Formularelement (ID: deleted)');
        expect(humanize({
            type: 'NoCodeReference',
            elementId: null,
        })).toBe('Kein Formularelement ausgewählt');
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
