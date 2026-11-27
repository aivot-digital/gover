import {describe, expect, it} from 'vitest';
import {findLegacyNoCode} from './find-legacy-no-code';

describe('legacy rule discovery in imported and copied configurations', () => {
    it('finds implicit and explicit rules in nested UI definitions and tagged function values without modifying them', () => {
        const configuration = {
            form: {
                children: [{
                    id: 'age',
                    type: 2,
                    label: 'Alter',
                    visibility: {conditionSet: {conditions: []}},
                    validation: {
                        type: 'ConditionSet',
                        conditionSet: {conditions: []},
                    },
                }],
            },
            expression: {
                type: 'ConditionSet',
                conditionSet: {conditions: []},
            },
        };
        const snapshot = JSON.stringify(configuration);
        expect(findLegacyNoCode(configuration)).toEqual([
            'Alter · Sichtbarkeit · Konfiguration.form.children[0].visibility',
            'Alter · Validierung · Konfiguration.form.children[0].validation',
            'Konfiguration.expression',
        ]);
        expect(JSON.stringify(configuration)).toBe(snapshot);
    });
    it('ignores null, new expressions and arbitrary customer properties and strings', () => {
        expect(findLegacyNoCode({
            conditionSet: {},
            content: '{"type":"ConditionSet","conditionSet":{}}',
            newRule: {
                type: 'NoCode',
                noCode: {
                    type: 'NoCodeStaticValue',
                    value: true,
                },
            },
            element: {
                id: 'field',
                type: 2,
                visibility: {conditionSet: null},
            },
        })).toEqual([]);
        expect(findLegacyNoCode(null)).toEqual([]);
    });
});
