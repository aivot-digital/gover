import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {literalAuthoredValue} from '../../../models/element-data';
import {InputMode, InputVariableSource} from '../../../models/input-mode';
import {describeConfigurationProperty, describeElementProperty} from './element-conflict-labels';

describe('element conflict labels', () => {
    it('uses the field editor label for the affected element type', () => {
        const text = {id: 'text', type: ElementType.Text} as AnyElement;
        const button = {id: 'button', type: ElementType.LinkButton} as AnyElement;
        const date = {id: 'date', type: ElementType.Date} as AnyElement;

        expect(describeElementProperty(['label'], [text], text)).toBe('Titel');
        expect(describeElementProperty(['label'], [button], button)).toBe('Beschriftung');
        expect(describeElementProperty(['mode'], [date], date)).toBe('Datums-Format');
        expect(describeElementProperty(['placeholder'], [text], text)).toBe('Platzhalter');
    });

    it('names nested rule properties without repeating storage wrappers', () => {
        expect(describeElementProperty(['validation', 'javascriptCode', 'code'], []))
            .toBe('Validierung › Low-Code-Skript');
        expect(describeElementProperty(['visibility', 'requirements'], []))
            .toBe('Sichtbarkeit › Fachliche Anforderungen');
    });

    it('keeps unknown extensions and authored mapping keys unchanged', () => {
        expect(describeElementProperty(['extension', 'placeholder', 'label'], []))
            .toBe('extension › placeholder › label');
        expect(describeElementProperty(['metadata', 'identityMappings', 'placeholder'], []))
            .toBe('Metadaten › Attribute zuordnen › placeholder');
        expect(describeElementProperty(['metadata', 'identityMappings', 'Attribute zuordnen'], []))
            .toBe('Metadaten › Attribute zuordnen › Attribute zuordnen');
        expect(describeElementProperty(['override', 'fieldNoCodeMap', 'label', 'value'], []))
            .toBe('Dynamische Struktur › Eigenschaften › label › Wert');
    });

    it('names column properties and list items without losing their identifiers', () => {
        const table = {id: 't', type: ElementType.Table, fields: [{id: 'c', label: 'Kosten'}]} as unknown as AnyElement;

        expect(describeElementProperty(['fields', '[c]', 'placeholder'], [table], table))
            .toBe('Spalten › Eintrag „Kosten“ › Platzhalter');
        expect(describeElementProperty(['fields', '[missing]', 'label'], [table], table))
            .toBe('Spalten › Eintrag „missing“ › Titel');
        expect(describeElementProperty(['fields', '#order'], [table], table))
            .toBe('Reihenfolge der Spalten');
    });

    it('hides the authored literal wrapper while preserving ordinary configuration keys', () => {
        const values = [literalAuthoredValue({placeholder: {type: InputMode.Literal, value: 'User JSON'}})];

        expect(describeConfigurationProperty(['value'], values)).toBe('Wert');
        expect(describeConfigurationProperty(['value', 'placeholder', 'value'], values)).toBe('placeholder › value');
        expect(describeConfigurationProperty(['type'], values)).toBe('Eingabeart');
    });

    it('names variable and code inputs by their visible concepts', () => {
        const variable = {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'vorname'}};

        expect(describeConfigurationProperty(['reference', 'path'], [variable])).toBe('Variable › Datenschlüssel');
        expect(describeConfigurationProperty(['code'], [{type: InputMode.LowCode, code: 'return 1;'}])).toBe('Low-Code-Skript');
    });
});
