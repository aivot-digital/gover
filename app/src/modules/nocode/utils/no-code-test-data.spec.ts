import {describe, expect, it} from 'vitest';
import {NoCodeDataType as T} from '../../../data/no-code-data-type';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {InputVariableSource} from '../../../models/input-mode';
import {toNoCodeEditorNode} from './no-code-editor-store';
import {
    buildTestSource,
    collectNoCodeTestFields,
    NoCodeTestValueDraft,
    parseTestDraft,
    testDraftFromValue,
    testSourceFieldDraft,
} from './no-code-test-data';

const ref = (path: string): NoCodeOperand => ({
    type: 'NoCodeProcessDataReference',
    path,
});
const expression = (...operands: NoCodeOperand[]): NoCodeOperand => ({
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands,
});
const plan = (...operands: NoCodeOperand[]) => collectNoCodeTestFields(toNoCodeEditorNode(expression(...operands)), []).sources.processData!;
const draft = (text: string, type = T.String): NoCodeTestValueDraft => ({
    text,
    type,
    mode: 'value',
});

describe('No-Code test data', () => {
    it('deduplicates normalized paths within their source and reuses labels and options', () => {
        const fields = collectNoCodeTestFields(toNoCodeEditorNode(expression(ref('person.name'), ref(' person . name '),
            {
                type: 'NoCodeInstanceDataReference',
                path: 'person.name',
            },
            {
                type: 'NoCodeNodeDataReference',
                nodeDataKey: '123',
                path: 'person.name',
            })), [], {
            variables: [{
                source: InputVariableSource.ProcessData,
                path: 'person.name',
                label: 'Name',
            }],
            processOptions: new Map([['person.name', [{
                label: 'Anna',
                value: 'anna',
            }]]]),
        });
        expect(fields.sources.processData?.fields).toHaveLength(1);
        expect(fields.sources.processData?.fields[0]).toMatchObject({
            label: 'Name',
            options: [{
                label: 'Anna',
                value: 'anna',
            }],
        });
        expect(fields.sources.instanceData?.fields).toHaveLength(1);
        expect(fields.sources.nodeData?.fields[0].segments).toEqual(['123', 'person', 'name']);
    });

    it.each(['items[0].name', 'items[*].name', '__proto__.polluted', 'constructor.prototype.x', ''])('keeps %s in JSON rather than guessing a structure', path => {
        expect(plan(ref(path)).reason).toBeTruthy();
        expect(({} as Record<string, unknown>).polluted).toBeUndefined();
    });

    it('requires JSON for overlapping paths but not for merely similar names', () => {
        expect(plan(ref('person'), ref('person.name')).reason).toBeTruthy();
        expect(plan(ref('person'), ref('personal.name')).reason).toBeUndefined();
    });

    it('builds nested values, preserves unrelated JSON and keeps missing distinct from empty/null/zero/false', () => {
        const schema = plan(ref('person.name'), ref('person.age'), ref('active'), ref('empty'), ref('nil'), ref('unused.deep'));
        const values = Object.fromEntries(schema.fields.map((field, i) => [field.key, [draft('Anna'), draft('0', T.Number), draft('false', T.Boolean),
            {
                ...draft(''),
                mode: 'empty',
            }, {
                ...draft(''),
                mode: 'null',
            }, {
                ...draft(''),
                mode: 'missing',
            }][i] as NoCodeTestValueDraft]));
        expect(buildTestSource(schema, {
            json: '{"keep":{"x":3}}',
            values,
        })).toEqual({
            value: {
                keep: {x: 3},
                person: {
                    name: 'Anna',
                    age: 0,
                },
                active: false,
                empty: '',
                nil: null,
            },
        });
    });

    it('does not delete an unrelated root property when removing an absent nested value', () => {
        const schema = plan(ref('missing.name'));
        expect(buildTestSource(schema, {
            json: '{"name":"keep"}',
            values: {
                [schema.fields[0].key]: {
                    ...draft(''),
                    mode: 'missing',
                },
            },
        })).toEqual({value: {name: 'keep'}});
    });

    it('rejects invalid inputs and parent conflicts without replacing existing data', () => {
        const schema = plan(ref('person.name'));
        expect(buildTestSource(schema, {json: '{"person":42}'})).toHaveProperty('error');
        expect(buildTestSource(schema, {json: '{'})).toHaveProperty('error');
        expect(buildTestSource(schema, {values: {[schema.fields[0].key]: draft('unfinished', T.Number)}})).toHaveProperty('error');
    });

    it.each([false, 0, '', null, ['a'], {a: 1}, 'plain'])('reads JSON values into field drafts without changing %j', value => {
        const schema = plan(ref('value'));
        const input = {
            json: JSON.stringify({
                value,
                extra: 'keep',
            }),
        };
        const fieldDraft = testSourceFieldDraft(schema.fields[0], input);
        expect(parseTestDraft(fieldDraft)).toEqual({value});
        expect(buildTestSource(schema, {
            ...input,
            values: {[schema.fields[0].key]: fieldDraft},
        })).toEqual({
            value: {
                value,
                extra: 'keep',
            },
        });
    });

    it('preserves invalid JSON in expert mode and never runs field coercions there', () => {
        const schema = plan(ref('value'));
        expect(buildTestSource(schema, {
            mode: 'json',
            json: '{',
        })).toHaveProperty('error');
        expect(buildTestSource(schema, {
            mode: 'json',
            json: '{"value":false}',
            values: {[schema.fields[0].key]: draft('true')},
        })).toEqual({value: {value: false}});
    });

    it('starts unknown types explicitly as text and retains null and empty text without JSON knowledge', () => {
        expect(testDraftFromValue(undefined)).toEqual(draft(''));
        expect(parseTestDraft({
            ...draft(''),
            mode: 'empty',
        })).toEqual({value: ''});
        expect(parseTestDraft({
            ...draft(''),
            mode: 'null',
        })).toEqual({value: null});
        expect(parseTestDraft({
            ...draft(''),
            mode: 'missing',
        })).toEqual({});
    });
});
