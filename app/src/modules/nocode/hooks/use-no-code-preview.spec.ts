import {act, renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useNoCodePreview} from './use-no-code-preview';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));

const operators: NoCodeOperatorDetailsDTO[] = [{
    identifier: 'add',
    label: 'Addiere',
    packageName: 'math',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: 'Addiere #0 und #1',
    tags: [],
    signatures: [{returnType: NoCodeDataType.Number, parameters: [1, 2].map((index) => ({
        label: `Wert ${index}`,
        type: NoCodeDataType.Number,
        description: null,
        options: [],
    }))}],
}];

const expression: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'add',
    operands: [
        {type: 'NoCodeProcessDataReference', path: 'warenkorb.anzahl'},
        {type: 'NoCodeStaticValue', value: '1'},
    ],
};

describe('useNoCodePreview', () => {
    beforeEach(() => {
        api.get.mockReset().mockResolvedValue(operators);
    });

    it('humanizes operations using metadata without evaluating their result', async () => {
        const {result, rerender} = renderHook(({operand}: {operand: NoCodeOperand}) => useNoCodePreview(operand), {
            initialProps: {operand: expression as NoCodeOperand},
        });

        expect(result.current).toBe('Vorschau wird geladen …');
        await waitFor(() => expect(result.current).toBe('Addiere Vorgangsdaten → warenkorb.anzahl und „1“'));
        rerender({operand: {...expression, operands: [{type: 'NoCodeStaticValue', value: '2'}, expression]}});
        expect(result.current).toBe('Addiere „2“ und Addiere Vorgangsdaten → warenkorb.anzahl und „1“');
        expect(api.get).toHaveBeenCalledTimes(1);
        expect(api.get).toHaveBeenCalledWith('no-code/operators', {queryParams: undefined});
    });

    it('renders literals and references without fetching operator metadata', () => {
        const {result, rerender} = renderHook(({operand}: {operand: NoCodeOperand | null}) => useNoCodePreview(operand), {
            initialProps: {operand: null as NoCodeOperand | null},
        });
        expect(result.current).toBeUndefined();
        rerender({operand: {type: 'NoCodeStaticValue', value: '13'}});
        expect(result.current).toBe('„13“');
        rerender({operand: {type: 'NoCodeNodeDataReference', nodeDataKey: 'warenkorbLaden', path: 'anzahl'}});
        expect(result.current).toBe('Elementdaten (warenkorbLaden) → anzahl');
        expect(api.get).not.toHaveBeenCalled();
    });

    it('uses the current element label for a field reference', () => {
        const field = {...generateElementWithDefaultValues(ElementType.Text), label: 'Aktenzeichen'};
        const root = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [field]};
        const {result, rerender} = renderHook(({rootElement}) => useNoCodePreview({type: 'NoCodeReference', elementId: field.id}, rootElement), {
            initialProps: {rootElement: root},
        });

        expect(result.current).toBe('Wert von „Aktenzeichen“');
        rerender({rootElement: {...root, children: [{...field, label: 'Vorgangsnummer'}]}});
        expect(result.current).toBe('Wert von „Vorgangsnummer“');
        expect(api.get).not.toHaveBeenCalled();
    });

    it('provides a non-blocking fallback when metadata cannot be loaded', async () => {
        api.get.mockRejectedValue(new Error('Network unavailable'));
        const {result} = renderHook(() => useNoCodePreview(expression));

        await waitFor(() => expect(result.current).toBe('Vorschau nicht verfügbar'));
    });

    it('ignores metadata arriving after switching to a plain reference', async () => {
        let resolve!: (value: NoCodeOperatorDetailsDTO[]) => void;
        api.get.mockReturnValue(new Promise<NoCodeOperatorDetailsDTO[]>((done) => {resolve = done;}));
        const {result, rerender} = renderHook(({operand}: {operand: NoCodeOperand}) => useNoCodePreview(operand), {
            initialProps: {operand: expression as NoCodeOperand},
        });
        rerender({operand: {type: 'NoCodeProcessDataReference', path: 'neuerWert'}});
        await act(async () => resolve(operators));

        expect(result.current).toBe('Vorgangsdaten → neuerWert');
    });
});
