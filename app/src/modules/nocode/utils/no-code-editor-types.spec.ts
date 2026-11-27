import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeDateInput, type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {toNoCodeEditorNode} from './no-code-editor-store';
import {getNoCodeEditorTypeConflict} from './no-code-editor-types';

const isEmpty: NoCodeOperatorDetailsDTO = {
    identifier: 'is-empty',
    packageName: 'core',
    label: 'Ist Leer',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [{
            type: NoCodeDataType.Runtime,
            label: 'Wert',
            description: null,
            options: [],
        }],
    }],
};

const dateElement = {
    ...generateElementWithDefaultValues(ElementType.Date),
    id: 'birthday',
    label: 'Geburtstag',
};

function conflict(operand: NoCodeOperand, expected: NoCodeDataType, dateInput?: NoCodeDateInput) {
    return getNoCodeEditorTypeConflict(toNoCodeEditorNode(operand)!, {type: expected, dateInput}, [{
        element: dateElement,
        parents: [],
    }, {
        element: {...generateElementWithDefaultValues(ElementType.DateTime), id: 'appointment'},
        parents: [],
    }], [isEmpty]);
}

describe('getNoCodeEditorTypeConflict', () => {
    it('flags values that the backend would replace by a default', () => {
        expect(conflict({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-empty',
            operands: [null],
        }, NoCodeDataType.Number))
            .toBe('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Wahrheitswert“.');
        expect(conflict({
            type: 'NoCodeReference',
            elementId: 'birthday',
        }, NoCodeDataType.DateTime))
            .toBe('Typkonflikt: Erwartet wird „Datums- und Zeitwert“, geliefert wird „Datumswert“.');
    });

    it('accepts conversions the backend performs', () => {
        expect(conflict({
            type: 'NoCodeReference',
            elementId: 'birthday',
        }, NoCodeDataType.String)).toBeNull();
        expect(conflict({
            type: 'NoCodeReference',
            elementId: 'appointment',
        }, NoCodeDataType.Time)).toBeNull();
        expect(conflict({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-empty',
            operands: [null],
        }, NoCodeDataType.String)).toBeNull();
        expect(conflict({
            type: 'NoCodeProcessDataReference',
            path: 'a',
        }, NoCodeDataType.Date)).toBeNull();
        expect(conflict({
            type: 'NoCodeExpression',
            operatorIdentifier: 'unknown',
            operands: [],
        }, NoCodeDataType.Date)).toBeNull();
    });

    it('warns about timestamp literals and sources only for strict calendar parameters', () => {
        const inputs: NoCodeOperand[] = [
            {type: 'NoCodeStaticValue', value: '2026-11-22T12:30:00Z'},
            {type: 'NoCodeReference', elementId: 'appointment'},
        ];
        for (const input of inputs) {
            expect(conflict(input, NoCodeDataType.Date, NoCodeDateInput.Calendar)).not.toBeNull();
            expect(conflict(input, NoCodeDataType.Date, NoCodeDateInput.CalendarOrDateTime)).toBeNull();
        }
        for (const value of ['2026', '2026-11', '2026-11-22']) {
            expect(conflict({type: 'NoCodeStaticValue', value}, NoCodeDataType.Date, NoCodeDateInput.Calendar)).toBeNull();
        }
    });

    it('warns about dates as numbers and other values as conditions', () => {
        expect(conflict({
            type: 'NoCodeReference',
            elementId: 'birthday',
        }, NoCodeDataType.Number)).toBe('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Datumswert“.');
        expect(conflict({
            type: 'NoCodeReference',
            elementId: 'birthday',
        }, NoCodeDataType.Boolean)).not.toBeNull();
    });

    it('checks static text against the formats the backend parses', () => {
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '1.5e3',
        }, NoCodeDataType.Number)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: ' 1,5 ',
        }, NoCodeDataType.Number)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '2024-02-29',
        }, NoCodeDataType.Number)).not.toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '2026-11-22T12:30:00Z',
        }, NoCodeDataType.Time)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: 'abc',
        }, NoCodeDataType.Number))
            .toBe('Typkonflikt: Erwartet wird „Zahlenwert“, „abc“ lässt sich nicht umwandeln.');
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '2024-05',
        }, NoCodeDataType.Date)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '2024-05-01',
        }, NoCodeDataType.DateTime)).not.toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '2024-05-01T10:00:00Z',
        }, NoCodeDataType.DateTime)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '10:30',
        }, NoCodeDataType.Time)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '{"a": 1}',
        }, NoCodeDataType.List)).not.toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '[1, 2]',
        }, NoCodeDataType.List)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: '',
        }, NoCodeDataType.Number)).toBeNull();
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: 'irgendwas',
        }, NoCodeDataType.Boolean)).toBe('Typkonflikt: Erwartet wird „Wahrheitswert“, „irgendwas“ lässt sich nicht umwandeln.');
        expect(conflict({
            type: 'NoCodeStaticValue',
            value: ' Wahr ',
        }, NoCodeDataType.Boolean)).toBeNull();
    });
});


it('rejects invalid calendar and clock values instead of accepting their textual shape', () => {
    for (const [value, expected] of [
        ['2024-02-30', NoCodeDataType.Date],
        ['2024-13', NoCodeDataType.Date],
        ['25:01', NoCodeDataType.Time],
        ['2024-05', NoCodeDataType.Number],
        ['2024-02-30T10:00:00Z', NoCodeDataType.DateTime],
    ] as const) expect(conflict({
        type: 'NoCodeStaticValue',
        value,
    }, expected)).not.toBeNull();
    expect(conflict({
        type: 'NoCodeStaticValue',
        value: '2024-05-01T10:00:00.123',
    }, NoCodeDataType.DateTime)).toBeNull();
});
