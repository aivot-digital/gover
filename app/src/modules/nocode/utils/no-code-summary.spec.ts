import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO, NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {InputVariableSource} from '../../../models/input-mode';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {summarizeNoCode} from './no-code-summary';

function operator(identifier: string, template: string | null, count = 2): NoCodeOperatorDetailsDTO {
    return {
        identifier,
        packageName: 'core',
        label: identifier,
        abstractDescription: '',
        description: '',
        tags: [],
        humanReadableTemplate: template,
        sentenceLead: 'ergibt wahr',
        signatures: [{
            returnType: NoCodeDataType.Runtime,
            parameters: Array.from({length: count}, (_, i) => ({
                type: NoCodeDataType.Runtime,
                label: `Wert ${i + 1}`,
                description: null,
                options: [],
            })),
        }],
    };
}

const literal = (value: unknown) => ({type: 'NoCodeStaticValue', value}) as NoCodeOperand;
const expression = (operatorIdentifier: string | null, ...operands: (NoCodeOperand | null)[]): NoCodeExpression => ({
    type: 'NoCodeExpression', operatorIdentifier, operands,
});
const reference = (elementId: string | null): NoCodeOperand => ({type: 'NoCodeReference', elementId});
const processReference = (path: string | null): NoCodeOperand => ({type: 'NoCodeProcessDataReference', path});
const equals = operator('Vergleich', '„#0“ ist gleich „#1“');
const and = operator('Und', '„#0“ und „#1“');
const or = operator('Oder', '„#0“ oder „#1“');
const not = operator('Nicht', 'nicht „#0“', 1);
const operators = [equals, and, or, not];
const summary = (operand: NoCodeOperand | null, ops = operators) => summarizeNoCode(operand, [], ops);

function field(id: string, label: string, options: NoCodeParameterOption[] = []): ElementWithParents {
    return {
        element: {...generateElementWithDefaultValues(ElementType.Select), id, label, options},
        parents: [],
    };
}

describe('summarizeNoCode', () => {
    it('reads nested boolean clauses without repeating function names and leads', () => {
        const result = summary(expression('Nicht', expression('Und',
            expression('Vergleich', literal('a'), literal('Ja')),
            expression('Vergleich', literal('b'), literal('Nein')),
        )));
        expect(result.text).toBe('nicht ((„a“ ist gleich „Ja“) und („b“ ist gleich „Nein“))');
        expect(result.text).not.toMatch(/ergibt wahr|Vergleich|Nicht|Und/);
        expect(result.details).toEqual([]);
    });

    it('retains mixed conjunction grouping and authored operand order', () => {
        expect(summary(expression('Und', literal(false), expression('Oder', literal(true), literal(false)))).text)
            .toBe('Falsch und (Wahr oder Falsch)');
    });

    it('preserves nested date arithmetic and parameter option labels', () => {
        const subtract = operator('Von Datum subtrahieren', 'subtrahiere „#1“ „#2“ von „#0“', 3);
        subtract.signatures[0].parameters[1].type = NoCodeDataType.Number;
        subtract.signatures[0].parameters[2].options = [{label: 'Jahre', value: 'years'}];
        const today = operator('Heutiges Datum erstellen', 'erstelle das heutige Datum', 0);
        const compare = operator('Datumswerte vergleichen', 'vergleiche „#0“: „#1“ „#2“', 3);
        compare.signatures[0].parameters[1].options = [{label: 'ist davor oder gleich', value: 'before-or-equal'}];
        const result = summarizeNoCode(expression('Datumswerte vergleichen', reference('birthday'), literal('before-or-equal'),
            expression('Von Datum subtrahieren', expression('Heutiges Datum erstellen'), literal('18'), literal('years')),
        ), [field('birthday', 'Geburtsdatum')], [subtract, today, compare]);
        expect(result.text).toBe('vergleiche Wert von „Geburtsdatum“: „ist davor oder gleich“ (subtrahiere 18 „Jahre“ von (erstelle das heutige Datum))');
        expect(result.details).toContainEqual({label: 'Jahre', value: 'years'});
    });

    it.each([null, '', '   ', '#0', '#0 und #2', '#0 und #1 und #2'])(
        'uses a complete named-parameter fallback for an unusable template %s', template => {
            expect(summary(expression('Vergleich', literal('a'), literal('b')), [{...equals, humanReadableTemplate: template}]).text)
                .toBe('Vergleich: Wert 1: „a“; Wert 2: „b“');
        },
    );

    it('does not omit a third operand from a two-placeholder template', () => {
        expect(summary(expression('Oder', literal(true), literal(false), literal(true))).text)
            .toBe('Oder: Wert 1: Wahr; Wert 2: Falsch; Wert 3: Wahr');
    });

    it('selects the matching signature before validating template coverage', () => {
        const overloaded = {
            ...equals,
            signatures: [
                {returnType: NoCodeDataType.Runtime, parameters: equals.signatures[0].parameters.slice(0, 1)},
                equals.signatures[0],
            ],
        };
        expect(summary(expression('Vergleich', literal('a'), literal('b')), [overloaded]).text).toBe('„a“ ist gleich „b“');
        expect(summary(expression('Vergleich', literal('a')), [overloaded]).text).toBe('Vergleich: Wert 1: „a“');
    });

    it('retains required missing slots and distinguishes authored empty and falsy values', () => {
        expect(summary(null).text).toBe('Kein Ausdruck definiert');
        expect(summary(literal(null)).text).toBe('Leerer Wert');
        expect(summary(literal('')).text).toBe('Leerer Text');
        expect(summary(literal(false)).text).toBe('Falsch');
        expect(summary(literal(0)).text).toBe('0');
        expect(summary(literal('0')).text).toBe('„0“');
        expect(summary(expression('Vergleich', literal('a'))).text).toBe('„a“ ist gleich Fehlender Parameter „Wert 2“');
        expect(summary(expression('Vergleich', null, literal(false))).text).toBe('Fehlender Parameter „Wert 1“ ist gleich Falsch');
    });

    it('names unavailable functions and references without hiding their operands', () => {
        expect(summary(expression('deleted', literal('a'), expression(null, null))).text)
            .toBe('Unbekannte Funktion (deleted): Parameter 1: „a“; Parameter 2: (Keine Funktion ausgewählt: Parameter 1: Fehlender Parameter „Parameter 1“)');
        expect(summary(reference('deleted')).text).toBe('Unbekanntes Formularelement (ID: deleted)');
        expect(summary(reference(null)).text).toBe('Kein Formularelement ausgewählt');
    });

    it.each(['„#0“', '“#0”', '"#0"', "'#0'", '#0'])(
        'replaces %s atomically and preserves quotes and placeholders within values', template => {
            expect(summary(expression('Text', literal('Text #0 mit "Zitat"')), [operator('Text', template, 1)]).text)
                .toBe('„Text #0 mit "Zitat"“');
        },
    );

    it('supports multi-digit and repeated placeholders', () => {
        const template = '#10 / #1 / ' + Array.from({length: 11}, (_, i) => `#${i}`).join(' / ');
        expect(summary(expression('Viele', ...Array.from({length: 11}, (_, i) => literal(`v${i}`))), [operator('Viele', template, 11)]).text)
            .toMatch(/^„v10“ \/ „v1“ \/ „v0“ \/ „v1“/);
    });

    it('shows field and selection labels while retaining their exact technical values in details', () => {
        const result = summarizeNoCode(expression('Vergleich', reference('state'), literal('BE')), [
            field('state', 'Bundesland', [{label: 'Berlin', value: 'BE'}]),
        ], operators);
        expect(result).toEqual({
            text: 'Wert von „Bundesland“ ist gleich „Berlin“',
            details: [{label: 'Formularelement „Bundesland“', value: 'state'}, {label: 'Berlin', value: 'BE'}],
        });
    });

    it('uses loaded code-list options and process-reference suggestions', () => {
        const codeListField = field('state', 'Bundesland');
        if (codeListField.element.type !== ElementType.Select) throw new Error('Expected select fixture');
        codeListField.element.optionsSource = OptionsSourceType.CodeList;
        const options = [{label: 'Berlin', value: 'BE'}];
        expect(summarizeNoCode(expression('Vergleich', reference('state'), literal('BE')), [codeListField], operators, {
            codeListOptions: new Map([['state', options]]),
        }).text).toBe('Wert von „Bundesland“ ist gleich „Berlin“');
        expect(summarizeNoCode(expression('Vergleich', processReference('state'), literal('BE')), [], operators, {
            variables: [{source: InputVariableSource.ProcessData, path: 'state', label: 'Bundesland'}],
            processOptions: new Map([['state', options]]),
        })).toEqual({
            text: '„Bundesland“ ist gleich „Berlin“',
            details: [{label: 'Bundesland', value: '$.state'}, {label: 'Berlin', value: 'BE'}],
        });
    });

    it('keeps options of incompatible parameter types out of number formatting', () => {
        const op = operator('Differenz', '#0 minus #1');
        op.signatures[0].parameters[0].type = NoCodeDataType.String;
        op.signatures[0].parameters[1].type = NoCodeDataType.Number;
        const result = summarizeNoCode(expression('Differenz', reference('text'), literal('18')), [
            field('text', 'Text', [{label: 'Falsche Auswahl', value: '18'}]),
        ], [op]);
        expect(result.text).toBe('Wert von „Text“ minus 18');
        expect(result.details).not.toContainEqual({label: 'Falsche Auswahl', value: '18'});
    });

    it('keeps variable paths in details and deduplicates repeated references', () => {
        const ref: NoCodeOperand = {type: 'NoCodeNodeDataReference', nodeDataKey: 'form', path: 'name'};
        expect(summarizeNoCode(expression('Vergleich', ref, ref), [], operators, {
            variables: [{source: InputVariableSource.ElementData, nodeDataKey: 'form', path: 'name', label: 'Name'}],
        })).toEqual({text: '„Name“ ist gleich „Name“', details: [{label: 'Name', value: '_.form.name'}]});
    });

    it('disambiguates field names and variable labels across reference sources', () => {
        const result = summarizeNoCode(expression('Vergleich', reference('name'), processReference('name')), [field('name', 'Name')], operators, {
            variables: [{source: InputVariableSource.ProcessData, path: 'name', label: 'Name'}],
        });
        expect(result.text).toBe('Wert von „Name“ (ID: name) ist gleich „Name“ ($.name)');
        expect(summarizeNoCode(reference('first'), [field('first', 'Name'), field('second', 'Name')], []).text)
            .toBe('Wert von „Name“ (ID: first)');
    });

    it('disambiguates duplicate selection labels even if only one value is selected', () => {
        expect(summarizeNoCode(expression('Vergleich', reference('state'), literal('BE')), [field('state', 'Bundesland', [
            {label: 'Berlin', value: 'BE'}, {label: 'Berlin', value: 'BER'},
        ])], operators).text).toBe('Wert von „Bundesland“ ist gleich „Berlin“ (BE)');
    });

    it('disambiguates identical selection labels used by separate nested expressions', () => {
        const first = operator('Erste Auswahl', '#0', 1);
        const second = operator('Zweite Auswahl', '#0', 1);
        first.signatures[0].parameters[0].options = [{label: 'Ja', value: 'yes'}];
        second.signatures[0].parameters[0].options = [{label: 'Ja', value: 'ja'}];
        expect(summary(expression('Vergleich', expression('Erste Auswahl', literal('yes')), expression('Zweite Auswahl', literal('ja'))), [equals, first, second]).text)
            .toBe('(„Ja“ (yes)) ist gleich („Ja“ (ja))');
    });

    it('keeps unresolved and incomplete reference paths explicit', () => {
        const result = summary(processReference('person.name'));
        expect(result.text).toBe('Vorgangsdaten → person.name');
        expect(result.details).toEqual([{label: 'Vorgangsdaten → person.name', value: '$.person.name'}]);
        expect(summary(processReference(null)).text).toBe('Vorgangsdaten → Kein Pfad angegeben');
        expect(summary({type: 'NoCodeInstanceDataReference', path: 'caseNumber'}).text).toBe('Geschützte Vorgangsdaten → caseNumber');
        expect(summary({type: 'NoCodeNodeDataReference', nodeDataKey: null, path: 'name'})).toEqual({
            text: 'Elementdaten (kein Datenschlüssel) → name', details: [],
        });
        expect(summarizeNoCode(processReference('name'), [], [], {
            variables: [{source: InputVariableSource.ProcessData, path: 'name', label: ' '}],
        }).text).toBe('Vorgangsdaten → name');
    });

    it('leaves the authored expression unchanged', () => {
        const operand = expression('Nicht', expression('Vergleich', literal('a'), literal('b')));
        const before = JSON.stringify(operand);
        summary(operand);
        expect(JSON.stringify(operand)).toBe(before);
    });
});
