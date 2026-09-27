import {describe, expect, it} from 'vitest';
import {formatProcessDataPath, isValidProcessDataPath, parseProcessDataPath, processDataPathPattern} from './process-data-path';

describe('process data paths', () => {
    it.each([
        ['person.name', ['person', 'name']],
        ['people[0].name', ['people', '0', 'name']],
        ['matrix[1][2].value', ['matrix', '1', '2', 'value']],
        ['people[*].addresses[*].street', ['people', '*', 'addresses', '*', 'street']],
        [' people [ 0 ] . name ', ['people', '0', 'name']],
        ['$value._name', ['$value', '_name']],
    ])('parses and formats %s', (path, segments) => {
        expect(parseProcessDataPath(path as string)).toEqual(segments);
        expect(parseProcessDataPath(formatProcessDataPath(segments as string[]))).toEqual(segments);
        expect(new RegExp(processDataPathPattern(true), 'v').test(path as string)).toBe(true);
    });

    it.each(['people.0.name', 'people.*.name', 'people[01]', 'people[-1]', 'people[1.5]',
        'people[]', 'people[0', 'people[0]name', 'people..name', 'people.',
        'people[index]', 'people["name"]', 'people[0];alert(1)', '1name', 'person-name'])('rejects %s', (path) => {
        expect(parseProcessDataPath(path)).toBeNull();
        expect(new RegExp(processDataPathPattern(true), 'v').test(path)).toBe(false);
    });

    it('distinguishes concrete references, wildcard mappings and array roots', () => {
        expect(isValidProcessDataPath('people[0].name')).toBe(true);
        expect(isValidProcessDataPath('people[*].name')).toBe(false);
        expect(isValidProcessDataPath('people[*].name', true)).toBe(true);
        expect(isValidProcessDataPath('[0].name')).toBe(false);
        expect(isValidProcessDataPath('[0].name', false, true)).toBe(true);
        expect(isValidProcessDataPath('')).toBe(false);
        expect(isValidProcessDataPath('people[2147483648]')).toBe(false);
    });
});
