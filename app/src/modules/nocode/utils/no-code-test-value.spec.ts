import {describe, expect, it} from 'vitest';
import {NoCodeDataType as T} from '../../../data/no-code-data-type';
import {parseNoCodeTestValue} from './no-code-test-value';

describe('parseNoCodeTestValue', () => {
    it.each([
        ['3,5', T.Number, 3.5], ['0', T.Number, 0], ['false', T.Boolean, false], ['true', T.String, 'true'],
        ['[1,2]', T.List, [1, 2]], ['{"ok":true}', T.Object, {ok: true}], ['2024-02-29', T.Date, '2024-02-29'],
        ['09:30', T.Time, '09:30'], ['2024-12-31T09:30:00+01:00', T.DateTime, '2024-12-31T09:30:00+01:00'],
    ])('parses %s as %s', (text, type, value) => expect(parseNoCodeTestValue(text as string, type as T)).toEqual({value}));
    it.each([
        ['no', T.Number], ['1e999', T.Number], ['maybe', T.Boolean], ['{}', T.List], ['[]', T.Object], ['{', T.Runtime, true],
        ['2023-02-29', T.Date], ['25:90', T.Time], ['2024-12-31', T.DateTime], ['2026-11-22T13:30:00+00:60', T.DateTime], ['false', T.String, true],
    ])('rejects invalid %s for %s instead of passing through text', (text, type, json = false) => {
        expect(parseNoCodeTestValue(text as string, type as T, json as boolean)).toHaveProperty('error');
    });
    it('separates missing input from explicit JSON null and an empty string', () => {
        expect(parseNoCodeTestValue('', T.String)).toEqual({});
        expect(parseNoCodeTestValue('null', T.String, true)).toEqual({value: null});
        expect(parseNoCodeTestValue('""', T.String, true)).toEqual({value: ''});
    });
});
