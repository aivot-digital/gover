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
const booleanNot: NoCodeOperatorDetailsDTO = {
    ...not,
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [{
            type: NoCodeDataType.Boolean,
            label: 'Bedingung',
            description: null,
            sentencePhrase: 'wenn',
            sentenceSuffix: 'falsch ist',
            options: [{label: 'Wahr', value: 'true'}, {label: 'Falsch', value: 'false'}],
        }],
    }],
};
const operators = [equals, and, or, not];
const summary = (operand: NoCodeOperand | null, ops = operators) => summarizeNoCode(operand, [], ops);

function dateArithmetic(identifier = 'subtract-from-date', direction = 'minus'): NoCodeOperatorDetailsDTO {
    return {
        ...operator(identifier, `#0 ${direction} #1 #2`, 3),
        signatures: [{
            returnType: NoCodeDataType.Date,
            parameters: [{
                type: NoCodeDataType.Date,
                label: 'Ausgangsdatum',
                description: null,
                options: [],
            }, {
                type: NoCodeDataType.Number,
                label: 'Anzahl',
                description: null,
                options: [],
            }, {
                type: NoCodeDataType.String,
                label: 'Einheit',
                description: null,
                quantityParameterIndex: 1,
                options: [
                    {label: 'Tage', value: 'tage', singularLabel: 'Tag'},
                    {label: 'Wochen', value: 'wochen', singularLabel: 'Woche'},
                    {label: 'Monate', value: 'monate', singularLabel: 'Monat'},
                    {label: 'Jahre', value: 'jahre', singularLabel: 'Jahr'},
                ],
            }],
        }],
    };
}

function dateTimeArithmetic(identifier = 'add-to-datetime', direction = 'plus'): NoCodeOperatorDetailsDTO {
    const arithmetic = dateArithmetic(identifier, direction);
    arithmetic.label = direction === 'plus' ? 'Zum Zeitpunkt hinzufügen' : 'Vom Zeitpunkt abziehen';
    arithmetic.signatures[0].returnType = NoCodeDataType.DateTime;
    arithmetic.signatures[0].parameters[0] = {
        ...arithmetic.signatures[0].parameters[0],
        type: NoCodeDataType.DateTime,
        label: 'Ausgangszeitpunkt',
    };
    return arithmetic;
}

const temporalComparisonModes = [
    ['gleich', 'gleich'],
    ['ungleich', 'nicht gleich'],
    ['vor', 'früher als'],
    ['vor-oder-gleich', 'früher als oder gleich'],
    ['nach', 'später als'],
    ['nach-oder-gleich', 'später als oder gleich'],
] as const;

function temporalComparison(type: NoCodeDataType): NoCodeOperatorDetailsDTO {
    const [identifier, label, firstLabel, secondLabel] = type === NoCodeDataType.Date
        ? ['compare-date', 'Datumswerte vergleichen', 'Datum', 'Vergleichsdatum']
        : type === NoCodeDataType.Time
            ? ['compare-time', 'Uhrzeiten vergleichen', 'Uhrzeit', 'Vergleichsuhrzeit']
            : ['compare-datetime', 'Zeitpunkte vergleichen', 'Zeitpunkt', 'Vergleichszeitpunkt'];
    return {
        ...operator(identifier, '#0 ist #1 #2', 3),
        label,
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [{
                type, label: firstLabel, description: null, sentencePhrase: 'wenn', options: [],
            }, {
                type: NoCodeDataType.String,
                label: 'Vergleichsart',
                description: null,
                sentencePhrase: '',
                optionsAsSentencePart: true,
                options: temporalComparisonModes.map(([value, label]) => ({label, value})),
            }, {
                type, label: secondLabel, description: null, sentencePhrase: '', sentenceSuffix: 'ist', options: [],
            }],
        }],
    };
}

function field(id: string, label: string, options: NoCodeParameterOption[] = []): ElementWithParents {
    return {
        element: {...generateElementWithDefaultValues(ElementType.Select), id, label, options},
        parents: [],
    };
}

describe('summarizeNoCode', () => {
    it.each(temporalComparisonModes)('reads the temporal comparison %s as the chosen sentence part for every temporal type', (value, phrase) => {
        for (const [type, left, right, displayedLeft, displayedRight] of [
            [NoCodeDataType.Date, '2026-11-22', '2026-11-23', '22.11.2026', '23.11.2026'],
            [NoCodeDataType.Time, '09:00', '17:00:00', '09:00:00 Uhr', '17:00:00 Uhr'],
            [NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '2026-11-22T13:30:00Z', '22.11.2026 13:30:00 Uhr', '22.11.2026 14:30:00 Uhr'],
        ] as const) {
            const comparison = temporalComparison(type);
            const operand = expression(comparison.identifier, literal(left), literal(value), literal(right));
            const original = structuredClone(operand);
            const result = summary(operand, [comparison]);

            expect(result.text).toBe(`${displayedLeft} ist ${phrase} ${displayedRight}`);
            expect(result.details).toContainEqual({label: phrase, value});
            expect(operand).toEqual(original);
        }
    });

    it.each([
        [processReference('vergleich'), 'Vorgangsdaten → vergleich'],
        [expression('mode-source', literal('vor')), '(„vor“)'],
        [null, 'Fehlender Parameter „Vergleichsart“'],
        [literal('vorher'), '„vorher“'],
        [literal(null), 'Leerer Wert'],
    ] as const)('names every parameter when the comparison cannot be determined from a fixed selection (%j)', (mode, displayedMode) => {
        const comparison = temporalComparison(NoCodeDataType.Date);
        const operand = expression(comparison.identifier, literal('2026-11-22'), mode, literal('2026-11-23'));
        const original = structuredClone(operand);
        expect(summary(operand, [comparison, operator('mode-source', '#0', 1)]).text)
            .toBe(`Datumswerte vergleichen: Datum: 22.11.2026; Vergleichsart: ${displayedMode}; Vergleichsdatum: 23.11.2026`);
        expect(operand).toEqual(original);
    });

    it('keeps legacy and ordinary text options quoted unless sentence-part presentation is explicit', () => {
        const comparison = temporalComparison(NoCodeDataType.Date);
        delete comparison.signatures[0].parameters[1].optionsAsSentencePart;
        const operand = expression(comparison.identifier, literal('2026-11-22'), literal('vor'), literal('2026-11-23'));
        expect(summary(operand, [comparison]).text).toBe('22.11.2026 ist „früher als“ 23.11.2026');
        comparison.humanReadableTemplate = 'vergleiche „#0“: „#1“ „#2“';
        comparison.signatures[0].parameters[1].options = [{label: 'ist davor', value: 'vor'}];
        expect(summary(operand, [comparison]).text).toBe('vergleiche 22.11.2026: „ist davor“ 23.11.2026');
    });

    it.each([
        ['add-to-datetime', 'plus'],
        ['subtract-from-datetime', 'minus'],
    ])('reads %s in the application timezone without changing the stored timestamp', (identifier, direction) => {
        const operand = expression(identifier, literal('2026-11-22T12:30:00Z'), literal('1'), literal('tage'));
        const original = structuredClone(operand);
        expect(summary(operand, [dateTimeArithmetic(identifier, direction)])).toEqual({
            text: `22.11.2026 13:30:00 Uhr ${direction} 1 Tag`,
            details: [{label: 'Tag', value: 'tage'}],
        });
        expect(operand).toEqual(original);
    });

    it('preserves the distinct occurrences of a repeated clock time in a comparison', () => {
        const arithmetic = dateTimeArithmetic();
        const comparison = temporalComparison(NoCodeDataType.DateTime);
        const operand = expression('compare-datetime',
            expression(arithmetic.identifier, literal('2026-10-25T02:30:00.123456789+02:00'), literal('0'), literal('tage')),
            literal('gleich'),
            expression(arithmetic.identifier, literal('2026-10-25T02:30:00.123456789+01:00'), literal('0'), literal('tage')),
        );

        expect(summary(operand, [arithmetic, comparison]).text).toBe(
            '(25.10.2026 02:30:00,123456789 Uhr (erstes Vorkommen) plus 0 Tage) ist gleich '
            + '(25.10.2026 02:30:00,123456789 Uhr (zweites Vorkommen) plus 0 Tage)',
        );
    });

    it('keeps the current instant symbolic inside date-time arithmetic', () => {
        const now = operator('create-now', 'aktueller Zeitpunkt', 0);
        now.signatures[0].returnType = NoCodeDataType.DateTime;
        const operand = expression('add-to-datetime', expression('create-now'), literal('1'), literal('tage'));
        expect(summary(operand, [dateTimeArithmetic(), now]).text).toBe('(aktueller Zeitpunkt) plus 1 Tag');
    });

    it.each([
        ['subtract-from-date', 'minus'],
        ['add-to-date', 'plus'],
    ])('reads %s with a formatted date and unquoted unit while preserving the authored values', (identifier, direction) => {
        const operand = expression(identifier, literal('2026-08-15'), literal('5'), literal('tage'));
        const original = structuredClone(operand);

        expect(summary(operand, [dateArithmetic(identifier, direction)])).toEqual({
            text: `15.08.2026 ${direction} 5 Tage`,
            details: [{label: 'Tage', value: 'tage'}],
        });
        expect(operand).toEqual(original);
    });

    it.each([
        ['1', 'tage', 'Tag', '1'],
        ['-1', 'wochen', 'Woche', '-1'],
        ['1.0', 'monate', 'Monat', '1.0'],
        ['1e0', 'jahre', 'Jahr', '1e0'],
        ['0', 'tage', 'Tage', '0'],
        ['2', 'tage', 'Tage', '2'],
        [' 5 ', 'tage', 'Tage', '5'],
        ['1.', 'tage', 'Tag', '1.'],
        ['.0', 'tage', 'Tage', '.0'],
    ])('uses the unit form %s %s → %s from quantity metadata', (quantity, unit, expectedUnit, displayedQuantity) => {
        const operand = expression('subtract-from-date', literal('2026-08-15'), literal(quantity), literal(unit));
        const original = structuredClone(operand);
        expect(summary(operand, [dateArithmetic()]).text)
            .toBe(`15.08.2026 minus ${displayedQuantity} ${expectedUnit}`);
        expect(operand).toEqual(original);
    });

    it('keeps a dynamic quantity symbolic and leaves ordinary text selections quoted', () => {
        const arithmetic = dateArithmetic();
        const dynamic = expression('subtract-from-date', literal('2026-08-15'), processReference('anzahl'), literal('tage'));
        expect(summarizeNoCode(dynamic, [], [arithmetic], {
            variables: [{source: InputVariableSource.ProcessData, path: 'anzahl', label: 'Anzahl der Tage'}],
        }).text).toBe('15.08.2026 minus „Anzahl der Tage“ Tage');

        const ordinarySelection = dateArithmetic();
        ordinarySelection.signatures[0].parameters[2].quantityParameterIndex = null;
        expect(summary(expression('subtract-from-date', literal('2026-08-15'), literal('1'), literal('tage')), [ordinarySelection]).text)
            .toBe('15.08.2026 minus 1 „Tage“');
    });

    it.each([
        ['2026-08', '08.2026'],
        ['2026', '2026'],
    ])('preserves the precision of the typed date %s', (date, expectedDate) => {
        expect(summary(expression('subtract-from-date', literal(date), literal('1'), literal('jahre')), [dateArithmetic()]).text)
            .toBe(`${expectedDate} minus 1 Jahr`);
    });

    it.each([NoCodeDataType.String, NoCodeDataType.Runtime])('keeps a date-looking %s literal as authored text', type => {
        const arithmetic = dateArithmetic();
        arithmetic.signatures[0].parameters[0].type = type;
        expect(summary(expression('subtract-from-date', literal('2026-08-15'), literal('1'), literal('tage')), [arithmetic]).text)
            .toBe('„2026-08-15“ minus 1 Tag');
    });

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

    it.each([
        [true, 'Wahr', 'true'],
        [false, 'Falsch', 'false'],
        ['true', 'Wahr', 'true'],
        ['false', 'Falsch', 'false'],
    ] as const)('renders the selected Boolean %s without quotes and retains its technical value', (value, label, storedValue) => {
        expect(summary(expression('Nicht', literal(value)), [booleanNot])).toEqual({
            text: `nicht ${label}`,
            details: [{label, value: storedValue}],
        });
    });

    it.each([NoCodeDataType.String, NoCodeDataType.Runtime])('keeps %s selection labels quoted when their authored values are text', type => {
        const textSelection = {
            ...booleanNot,
            signatures: [{
                ...booleanNot.signatures[0],
                parameters: [{...booleanNot.signatures[0].parameters[0], type}],
            }],
        };
        expect(summary(expression('Nicht', literal('true')), [textSelection]).text).toBe('nicht „Wahr“');
        expect(summary(expression('Nicht', literal('false')), [textSelection]).text).toBe('nicht „Falsch“');
    });

    it('recognizes actual Booleans in runtime parameters while retaining explicit text selection formatting', () => {
        const runtimeSelection = {
            ...booleanNot,
            signatures: [{
                ...booleanNot.signatures[0],
                parameters: [{...booleanNot.signatures[0].parameters[0], type: NoCodeDataType.Runtime}],
            }],
        };
        expect(summary(expression('Nicht', literal(true)), [runtimeSelection]).text).toBe('nicht Wahr');
        const textSelection = {
            ...runtimeSelection,
            signatures: [{
                ...runtimeSelection.signatures[0],
                parameters: [{...runtimeSelection.signatures[0].parameters[0], type: NoCodeDataType.String}],
            }],
        };
        expect(summary(expression('Nicht', literal(true)), [textSelection]).text).toBe('nicht „Wahr“');
    });

    it('retains custom Boolean option labels and disambiguates identical labels', () => {
        const customLabels = {
            ...booleanNot,
            signatures: [{
                ...booleanNot.signatures[0],
                parameters: [{
                    ...booleanNot.signatures[0].parameters[0],
                    options: [{label: 'Ja', value: 'true'}, {label: 'Nein', value: 'false'}],
                }],
            }],
        };
        expect(summary(expression('Nicht', literal('true')), [customLabels])).toEqual({
            text: 'nicht Ja',
            details: [{label: 'Ja', value: 'true'}],
        });
        customLabels.signatures[0].parameters[0].options = [{label: 'Antwort', value: 'true'}, {label: 'Antwort', value: 'false'}];
        expect(summary(expression('Nicht', literal('false')), [customLabels])).toEqual({
            text: 'nicht Antwort (false)',
            details: [{label: 'Antwort', value: 'false'}],
        });
    });

    it('does not infer Boolean semantics from arbitrary option values or German labels', () => {
        const nonCanonicalOption = {
            ...booleanNot,
            signatures: [{
                ...booleanNot.signatures[0],
                parameters: [{...booleanNot.signatures[0].parameters[0], options: [{label: 'Falsch', value: 'Nein'}]}],
            }],
        };
        expect(summary(expression('Nicht', literal('Nein')), [nonCanonicalOption]).text).toBe('nicht „Falsch“');
    });

    it('preserves grouping and text quotes inside a nested Boolean condition', () => {
        expect(summary(expression('Nicht', expression('Vergleich', literal('a'), literal('b'))), [equals, booleanNot]).text)
            .toBe('nicht („a“ ist gleich „b“)');
        expect(summary(expression('Nicht', expression('Nicht', literal('false'))), [booleanNot]).text)
            .toBe('nicht (nicht Falsch)');
    });

    it('preserves nested date arithmetic and parameter option labels', () => {
        const subtract = dateArithmetic();
        const today = operator('create-today', 'heutiges Datum', 0);
        today.label = 'Heutiges Datum';
        today.signatures[0].returnType = NoCodeDataType.Date;
        const compare = temporalComparison(NoCodeDataType.Date);
        const operand = expression('compare-date', reference('birthday'), literal('vor-oder-gleich'),
            expression('subtract-from-date', expression('create-today'), literal('18'), literal('jahre')),
        );
        const original = structuredClone(operand);
        const result = summarizeNoCode(operand,
            [field('birthday', 'Geburtsdatum')], [subtract, today, compare]);
        expect(result.text).toBe('Wert von „Geburtsdatum“ ist früher als oder gleich ((heutiges Datum) minus 18 Jahre)');
        expect(result.details).toContainEqual({label: 'Jahre', value: 'jahre'});
        expect(operand).toEqual(original);
    });

    it('keeps the current instant symbolic when nested in an existing temporal comparison', () => {
        const now = operator('create-now', 'aktueller Zeitpunkt', 0);
        now.label = 'Aktueller Zeitpunkt';
        now.signatures[0].returnType = NoCodeDataType.DateTime;
        const compare = temporalComparison(NoCodeDataType.DateTime);
        const operand = expression('compare-datetime', reference('appointment'), literal('vor-oder-gleich'), expression('create-now'));
        const original = structuredClone(operand);

        expect(summarizeNoCode(operand, [field('appointment', 'Termin')], [compare, now]).text)
            .toBe('Wert von „Termin“ ist früher als oder gleich (aktueller Zeitpunkt)');
        expect(summary(expression('create-now'), [now])).toEqual({text: 'aktueller Zeitpunkt', details: []});
        expect(operand).toEqual(original);
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
