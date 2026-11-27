import {describe, expect, it} from 'vitest';
import {generateDataKey, normalizeDataKeyInput, validateDataKeyPath, validateNodeDataKey} from './data-key-input';

describe('data key input', () => {
    it.each([
        ['Größe des Haushalts', 'groesse_des_haushalts'],
        ['2026 Einkommen', 'feld_2026_einkommen'],
        ['Straße / Hausnummer', 'strasse_hausnummer'],
        ['GROẞE A\u0308NDERUNG', 'grosse_aenderung'],
        ['  😀  ', null],
        ['', null],
    ])('generates a safe identifier from %s', (title, expected) => {
        expect(generateDataKey(title)).toBe(expected);
    });

    it('limits generated node keys without truncating manual paths', () => {
        expect(generateDataKey('a'.repeat(40), 32)).toHaveLength(32);
        expect(validateNodeDataKey('a'.repeat(33))).toBeDefined();
        expect(validateNodeDataKey('a'.repeat(32))).toBeUndefined();
    });

    it.each(['name', 'Name_2', '_name', '$name', 'name$2'])('accepts the node identifier %s', (value) => {
        expect(validateNodeDataKey(value)).toBeUndefined();
    });

    it.each(['name-2', '12name', 'person.name', 'items[0]', 'name space', 'Änderung', 'name@', 'name\n', '', null])('rejects the node identifier %s', (value) => {
        expect(validateNodeDataKey(value)).toBeDefined();
    });

    it.each([
        ['person.Vor Name', 'person.Vor_Name'],
        ['person.Äußere-Größe', 'person.Aeussere_Groesse'],
        ['GROẞE A\u0308NDERUNG', 'GROSSE_AeNDERUNG'],
        [' people [ 0 ] . Name ', '_people[0].Name_'],
        ['items[0].Name', 'items[0].Name'],
        ['person.', 'person.'],
        ['items[', 'items['],
        ['name@', 'name@'],
    ])('normalizes definition input %s without destroying path structure', (value, expected) => {
        expect(normalizeDataKeyInput(value)).toBe(expected);
    });

    it.each(['person.Name', '_name', '$name', 'people[0].name', 'people[2147483647].name'])('accepts %s', (value) => {
        expect(validateDataKeyPath(value)).toBeUndefined();
    });
    it.each(['person.', '12name', 'person.12name', 'people[01]', 'people[-1]', 'people[2147483648]', 'people[*]', 'na@me'])('rejects %s', (value) => {
        expect(validateDataKeyPath(value)).toBeDefined();
    });
    it('respects optional fields and wildcard policies', () => {
        expect(validateDataKeyPath(null)).toBeUndefined();
        expect(validateDataKeyPath(null, true)).toBeDefined();
        expect(validateDataKeyPath('people[*].name', false, true)).toBeUndefined();
    });
});
