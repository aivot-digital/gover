import {act, renderHook, waitFor} from '@testing-library/react';
import {createElement, type ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useNoCodePreview} from './use-no-code-preview';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {InputVariableSource, type InputVariableSuggestion} from '../../../models/input-mode';
import {createDerivedRuntimeElementData} from '../../../models/element-data';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../../../components/view-dispatcher/view-dispatcher.context';
import {summarizeNoCode} from '../utils/no-code-summary';

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
    signatures: [{
        returnType: NoCodeDataType.Number,
        parameters: [1, 2].map((index) => ({
            label: `Wert ${index}`,
            type: NoCodeDataType.Number,
            description: null,
            options: [],
        })),
    }],
}];

const expression: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'add',
    operands: [
        {
            type: 'NoCodeProcessDataReference',
            path: 'warenkorb.anzahl',
        },
        {
            type: 'NoCodeStaticValue',
            value: '1',
        },
    ],
};

function withVariables(variables: InputVariableSuggestion[]) {
    const rootElement = generateElementWithDefaultValues(ElementType.GroupLayout);
    return ({children}: {children: ReactNode}) => createElement(ViewDispatcherContextProvider, {
        value: {
            mode: ViewDispatcherMode.Viewer,
            rootElement,
            allElements: [rootElement],
            rootAuthoredElementValues: {},
            rootDerivedData: createDerivedRuntimeElementData(),
            inputModeVariables: variables,
        },
    }, children);
}

describe('useNoCodePreview', () => {
    beforeEach(() => {
        api.get.mockReset().mockResolvedValue(operators);
    });

    it('humanizes operations using metadata without evaluating their result', async () => {
        const {
            result,
            rerender,
        } = renderHook(({operand}: { operand: NoCodeOperand }) => useNoCodePreview(operand), {
            initialProps: {operand: expression as NoCodeOperand},
        });

        expect(result.current).toBe('Vorschau wird geladen …');
        await waitFor(() => expect(result.current).toBe('Addiere Vorgangsdaten → warenkorb.anzahl und 1'));
        rerender({
            operand: {
                ...expression,
                operands: [{
                    type: 'NoCodeStaticValue',
                    value: '2',
                }, expression],
            },
        });
        expect(result.current).toBe('Addiere 2 und (Addiere Vorgangsdaten → warenkorb.anzahl und 1)');
        expect(api.get).toHaveBeenCalledTimes(1);
        expect(api.get).toHaveBeenCalledWith('no-code/operators', {queryParams: undefined});
    });

    it('uses available process variable labels without adding their reference paths', async () => {
        const {result} = renderHook(() => useNoCodePreview(expression), {
            wrapper: withVariables([{
                source: InputVariableSource.ProcessData,
                path: 'warenkorb.anzahl',
                label: 'Anzahl der Positionen',
            }]),
        });

        await waitFor(() => expect(result.current).toBe('Addiere „Anzahl der Positionen“ und 1'));
    });

    it('matches the collapsed reading text for nested expressions and selection labels', async () => {
        const comparison: NoCodeOperatorDetailsDTO = {
            ...operators[0],
            identifier: 'equals',
            label: 'Ist gleich',
            sentenceLead: 'ergibt wahr',
            humanReadableTemplate: '„#0“ ist gleich „#1“',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: operators[0].signatures[0].parameters.map(parameter => ({
                    ...parameter,
                    type: NoCodeDataType.String,
                    options: [{label: 'Ja', value: 'ja'}],
                })),
            }],
        };
        const not: NoCodeOperatorDetailsDTO = {
            ...comparison,
            identifier: 'not',
            label: 'Nicht',
            humanReadableTemplate: 'nicht „#0“',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [{...comparison.signatures[0].parameters[0], type: NoCodeDataType.Boolean, options: []}],
            }],
        };
        const nested: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [{
                type: 'NoCodeExpression',
                operatorIdentifier: 'equals',
                operands: [
                    {type: 'NoCodeProcessDataReference', path: 'befreiung.beantragt'},
                    {type: 'NoCodeStaticValue', value: 'ja'},
                ],
            }],
        };
        const variables: InputVariableSuggestion[] = [{
            source: InputVariableSource.ProcessData,
            path: 'befreiung.beantragt',
            label: 'Befreiung beantragt',
        }];
        api.get.mockResolvedValue([comparison, not]);
        const {result} = renderHook(() => useNoCodePreview(nested), {wrapper: withVariables(variables)});

        await waitFor(() => expect(result.current).toBe('nicht („Befreiung beantragt“ ist gleich „Ja“)'));
        expect(result.current).toBe(summarizeNoCode(nested, [], [comparison, not], {variables}).text);
    });

    it('keeps the collapsed editor disambiguation when multiple variables share a label', async () => {
        const {result} = renderHook(() => useNoCodePreview(expression), {
            wrapper: withVariables(['warenkorb.anzahl', 'bestellung.anzahl'].map(path => ({
                source: InputVariableSource.ProcessData,
                path,
                label: 'Anzahl',
            }))),
        });

        await waitFor(() => expect(result.current).toBe('Addiere „Anzahl“ ($.warenkorb.anzahl) und 1'));
    });

    it('uses the complete collapsed fallback instead of a partial template for extra operands', async () => {
        const operand: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'add',
            operands: ['1', '2', '3'].map(value => ({type: 'NoCodeStaticValue', value})),
        };
        const {result} = renderHook(() => useNoCodePreview(operand));

        await waitFor(() => expect(result.current).toBe('Addiere: Wert 1: 1; Wert 2: 2; Wert 3: 3'));
    });

    it('renders literals and references without fetching operator metadata', () => {
        const {
            result,
            rerender,
        } = renderHook(({operand}: { operand: NoCodeOperand | null }) => useNoCodePreview(operand), {
            initialProps: {operand: null as NoCodeOperand | null},
        });
        expect(result.current).toBeUndefined();
        rerender({
            operand: {
                type: 'NoCodeStaticValue',
                value: '13',
            },
        });
        expect(result.current).toBe('„13“');
        rerender({
            operand: {
                type: 'NoCodeNodeDataReference',
                nodeDataKey: 'warenkorbLaden',
                path: 'anzahl',
            },
        });
        expect(result.current).toBe('Elementdaten (warenkorbLaden) → anzahl');
        expect(api.get).not.toHaveBeenCalled();
    });

    it('uses the current element label for a field reference', () => {
        const field = {
            ...generateElementWithDefaultValues(ElementType.Text),
            label: 'Aktenzeichen',
        };
        const root = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            children: [field],
        };
        const {
            result,
            rerender,
        } = renderHook(({rootElement}) => useNoCodePreview({
            type: 'NoCodeReference',
            elementId: field.id,
        }, rootElement), {
            initialProps: {rootElement: root},
        });

        expect(result.current).toBe('Wert von „Aktenzeichen“');
        rerender({
            rootElement: {
                ...root,
                children: [{
                    ...field,
                    label: 'Vorgangsnummer',
                }],
            },
        });
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
        api.get.mockReturnValue(new Promise<NoCodeOperatorDetailsDTO[]>((done) => {
            resolve = done;
        }));
        const {
            result,
            rerender,
        } = renderHook(({operand}: { operand: NoCodeOperand }) => useNoCodePreview(operand), {
            initialProps: {operand: expression as NoCodeOperand},
        });
        rerender({
            operand: {
                type: 'NoCodeProcessDataReference',
                path: 'neuerWert',
            },
        });
        await act(async () => resolve(operators));

        expect(result.current).toBe('Vorgangsdaten → neuerWert');
    });
});
