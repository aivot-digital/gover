import {TextFieldElement} from '../../../models/elements/form/input/text-field-element';
import {useState} from 'react';
import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {useLegacyNoCodeMigration} from './use-legacy-no-code-migration';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {ConditionSetOperator} from '../../../data/condition-set-operator';

const original = {
    type: 'ConditionSet' as const,
    conditionSet: {
        operator: ConditionSetOperator.All,
        conditions: [],
    },
    noCode: null,
    noCodeList: null,
    requirements: 'Bisherige Anforderungen',
    javascriptCode: null,
    referencedIds: [],
};

function harness() {
    const [base, setBase] = useState<TextFieldElement>({
        ...generateElementWithDefaultValues(ElementType.Text),
        visibility: original,
        validation: original,
    });
    return {base, ...useLegacyNoCodeMigration(base, setBase)};
}

describe('legacy conversion drafts', () => {
    it('keeps old rules intact until apply and preserves independent changes when cancelling', () => {
        const {result} = renderHook(harness);
        act(() => result.current.start('visibility'));
        expect(result.current.base.visibility).toEqual(original);
        expect(result.current.current.visibility).toMatchObject({
            type: 'NoCode',
            conditionSet: null,
            noCode: null,
        });
        act(() => result.current.update({
            ...result.current.current,
            label: 'Neuer Titel',
            visibility: {
                ...result.current.current.visibility!,
                requirements: 'Neue Anforderungen',
                noCode: {
                    type: 'NoCodeStaticValue',
                    value: 'true',
                },
            },
        }));
        expect(result.current.migrations.visibility?.original).toEqual(original);
        expect(result.current.base.visibility?.conditionSet).toEqual(original.conditionSet);
        act(() => result.current.cancel('visibility'));
        expect(result.current.current.label).toBe('Neuer Titel');
        expect(result.current.current.visibility).toEqual({
            ...original,
            requirements: 'Neue Anforderungen',
        });
    });
    it('keeps visibility and validation conversions independent and clears both for a new editing session', () => {
        const {result} = renderHook(harness);
        act(() => result.current.start('visibility'));
        act(() => result.current.start('validation'));
        expect(result.current.current.validation).toMatchObject({
            type: 'NoCode',
            noCodeList: [{
                noCode: null,
                message: '',
            }],
        });
        act(() => result.current.cancel('visibility'));
        expect(result.current.current.visibility).toEqual(original);
        expect(result.current.current.validation?.type).toBe('NoCode');
        act(() => result.current.reset());
        expect(result.current.current.validation).toEqual(original);
    });
});
