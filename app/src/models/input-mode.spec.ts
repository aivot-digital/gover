import {describe, expect, it} from 'vitest';
import {
    type AuthoredInputValue,
    getInputVariableReference,
    InputMode,
    InputVariableSource,
    isAuthoredInputValue,
} from './input-mode';

describe('Input-mode JSON contract', () => {
    it('preserves the backend mode and source identifiers', () => {
        expect(Object.values(InputMode)).toEqual(['Literal', 'Variable', 'NoCode', 'LowCode']);
        expect(Object.values(InputVariableSource)).toEqual([
            'ProcessData', 'ElementData', 'ElementMetadata', 'ProtectedProcessData',
        ]);
    });

    it.each<[AuthoredInputValue<unknown>, string]>([
        [{type: InputMode.Literal, value: 0}, '{"type":"Literal","value":0}'],
        [
            {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'name'}},
            '{"type":"Variable","reference":{"source":"ProcessData","path":"name"}}',
        ],
        [
            {type: InputMode.NoCode, operand: {type: 'NoCodeStaticValue', value: null}},
            '{"type":"NoCode","operand":{"type":"NoCodeStaticValue","value":null}}',
        ],
        [{type: InputMode.LowCode, code: 'return 1;'}, '{"type":"LowCode","code":"return 1;"}'],
    ])('serializes and recognizes wrapper %j', (value, json) => {
        expect(JSON.stringify(value)).toBe(json);
        expect(isAuthoredInputValue(JSON.parse(json))).toBe(true);
    });

    it.each([
        [InputVariableSource.ProcessData, '$.name'],
        [InputVariableSource.ElementData, '_.task.name'],
        [InputVariableSource.ElementMetadata, '$$.taskMetadata.task.name'],
        [InputVariableSource.ProtectedProcessData, '$$.name'],
    ])('formats references from %s', (source, expected) => {
        expect(getInputVariableReference({source, path: 'name', nodeDataKey: 'task'})).toBe(expected);
    });
});
