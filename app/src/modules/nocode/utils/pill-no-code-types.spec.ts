import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {toPillNoCodeNode} from './pill-no-code-store';
import {getPillNoCodeTypeConflict} from './pill-no-code-types';

const isEmpty: NoCodeOperatorDetailsDTO = {
    identifier: 'is-empty',
    packageName: 'core',
    label: 'Ist Leer',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{returnType: NoCodeDataType.Boolean, parameters: [{type: NoCodeDataType.Runtime, label: 'Wert', description: null, options: []}]}],
};

const dateElement = {...generateElementWithDefaultValues(ElementType.Date), id: 'birthday', label: 'Geburtstag'};

function conflict(operand: NoCodeOperand, expected: NoCodeDataType) {
    return getPillNoCodeTypeConflict(toPillNoCodeNode(operand)!, expected, [{element: dateElement, parents: []}], [isEmpty]);
}

describe('getPillNoCodeTypeConflict', () => {
    it('flags values that the backend would replace by a default', () => {
        expect(conflict({type: 'NoCodeExpression', operatorIdentifier: 'is-empty', operands: [null]}, NoCodeDataType.Number))
            .toBe('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Wahrheitswert“.');
        expect(conflict({type: 'NoCodeReference', elementId: 'birthday'}, NoCodeDataType.DateTime))
            .toBe('Typkonflikt: Erwartet wird „Datums- und Zeitwert“, geliefert wird „Datumswert“.');
    });

    it('accepts conversions the backend performs', () => {
        expect(conflict({type: 'NoCodeReference', elementId: 'birthday'}, NoCodeDataType.Number)).toBeNull();
        expect(conflict({type: 'NoCodeExpression', operatorIdentifier: 'is-empty', operands: [null]}, NoCodeDataType.String)).toBeNull();
        expect(conflict({type: 'NoCodeProcessDataReference', path: 'a'}, NoCodeDataType.Date)).toBeNull();
        expect(conflict({type: 'NoCodeExpression', operatorIdentifier: 'unknown', operands: []}, NoCodeDataType.Date)).toBeNull();
    });

    it('checks static text against the formats the backend parses', () => {
        expect(conflict({type: 'NoCodeStaticValue', value: '1.5e3'}, NoCodeDataType.Number)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '2024-02-29'}, NoCodeDataType.Number)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: 'abc'}, NoCodeDataType.Number))
            .toBe('Typkonflikt: Erwartet wird „Zahlenwert“, „abc“ lässt sich nicht umwandeln.');
        expect(conflict({type: 'NoCodeStaticValue', value: '2024-05'}, NoCodeDataType.Date)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '2024-05-01'}, NoCodeDataType.DateTime)).not.toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '2024-05-01T10:00:00Z'}, NoCodeDataType.DateTime)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '10:30'}, NoCodeDataType.Time)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '{"a": 1}'}, NoCodeDataType.List)).not.toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: '[1, 2]'}, NoCodeDataType.List)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: ''}, NoCodeDataType.Number)).toBeNull();
        expect(conflict({type: 'NoCodeStaticValue', value: 'irgendwas'}, NoCodeDataType.Boolean)).toBeNull();
    });
});
