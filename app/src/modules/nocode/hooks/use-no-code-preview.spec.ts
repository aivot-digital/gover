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

function temporalComparison(type: NoCodeDataType): NoCodeOperatorDetailsDTO {
    const [identifier, label, firstLabel, secondLabel] = type === NoCodeDataType.Date
        ? ['compare-date', 'Datumswerte vergleichen', 'Datum', 'Vergleichsdatum']
        : type === NoCodeDataType.Time
            ? ['compare-time', 'Uhrzeiten vergleichen', 'Uhrzeit', 'Vergleichsuhrzeit']
            : ['compare-datetime', 'Zeitpunkte vergleichen', 'Zeitpunkt', 'Vergleichszeitpunkt'];
    return {
        ...operators[0], identifier, label, humanReadableTemplate: '#0 ist #1 #2', sentenceLead: 'ergibt wahr',
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [{
                type, label: firstLabel, description: null, sentencePhrase: 'wenn', options: [],
            }, {
                type: NoCodeDataType.String,
                label: 'Vergleichsart',
                description: null,
                sentencePhrase: '',
                optionsAsSentencePart: true,
                options: [
                    {label: 'früher als', value: 'vor'},
                    {label: 'früher als oder gleich', value: 'vor-oder-gleich'},
                ],
            }, {
                type, label: secondLabel, description: null, sentencePhrase: '', sentenceSuffix: 'ist', options: [],
            }],
        }],
    };
}

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

    it.each([
        [NoCodeDataType.Date, '2026-11-22', '2026-11-23', '22.11.2026', '23.11.2026'],
        [NoCodeDataType.Time, '09:00', '17:00:00', '09:00:00 Uhr', '17:00:00 Uhr'],
        [NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '2026-11-22T13:30:00Z', '22.11.2026 13:30:00 Uhr', '22.11.2026 14:30:00 Uhr'],
    ] as const)('previews a known %s comparison and names the parameters when its mode becomes dynamic', async (type, left, right, displayedLeft, displayedRight) => {
        const comparison = temporalComparison(type);
        const variables: InputVariableSuggestion[] = [{source: InputVariableSource.ProcessData, path: 'vergleich', label: 'Ausgewählte Vergleichsart'}];
        const initial: NoCodeOperand = {
            type: 'NoCodeExpression', operatorIdentifier: comparison.identifier,
            operands: [{type: 'NoCodeStaticValue', value: left}, {type: 'NoCodeStaticValue', value: 'vor'}, {type: 'NoCodeStaticValue', value: right}],
        };
        const original = structuredClone(initial);
        api.get.mockResolvedValue([comparison]);
        const {result, rerender} = renderHook(({operand}) => useNoCodePreview(operand), {
            initialProps: {operand: initial}, wrapper: withVariables(variables),
        });

        await waitFor(() => expect(result.current).toBe(`${displayedLeft} ist früher als ${displayedRight}`));
        expect(result.current).toBe(summarizeNoCode(initial, [], [comparison], {variables}).text);

        const dynamic: NoCodeOperand = {...initial, operands: [initial.operands![0], {type: 'NoCodeProcessDataReference', path: 'vergleich'}, initial.operands![2]]};
        rerender({operand: dynamic});
        const [first, , second] = comparison.signatures[0].parameters;
        expect(result.current).toBe(`${comparison.label}: ${first.label}: ${displayedLeft}; Vergleichsart: „Ausgewählte Vergleichsart“; ${second.label}: ${displayedRight}`);
        expect(result.current).toBe(summarizeNoCode(dynamic, [], [comparison], {variables}).text);
        expect(initial).toEqual(original);
        expect(api.get).toHaveBeenCalledExactlyOnceWith('no-code/operators', {queryParams: undefined});
    });

    it.each([
        ['create-today', 'Heutiges Datum', NoCodeDataType.Date, 'heutiges Datum'],
        ['create-now', 'Aktueller Zeitpunkt', NoCodeDataType.DateTime, 'aktueller Zeitpunkt'],
    ] as const)('previews %s as a symbolic value both alone and inside another function', async (identifier, label, returnType, template) => {
        const isToday = identifier === 'create-today';
        const currentTime: NoCodeOperatorDetailsDTO = {
            ...operators[0],
            identifier,
            label,
            humanReadableTemplate: template,
            signatures: [{returnType, parameters: []}],
        };
        const parent: NoCodeOperatorDetailsDTO = isToday ? {
            ...operators[0],
            identifier: 'subtract-from-date',
            label: 'Vom Datum abziehen',
            humanReadableTemplate: '#0 minus #1 #2',
            signatures: [{
                returnType: NoCodeDataType.Date,
                parameters: [{
                    label: 'Ausgangsdatum',
                    type: NoCodeDataType.Date,
                    description: null,
                    options: [],
                }, {
                    label: 'Anzahl',
                    type: NoCodeDataType.Number,
                    description: null,
                    options: [],
                }, {
                    label: 'Einheit',
                    type: NoCodeDataType.String,
                    description: null,
                    quantityParameterIndex: 1,
                    options: [{label: 'Jahre', value: 'jahre'}],
                }],
            }],
        } : temporalComparison(NoCodeDataType.DateTime);
        const operand: NoCodeOperand = {type: 'NoCodeExpression', operatorIdentifier: identifier, operands: []};
        api.get.mockResolvedValue([currentTime, parent]);
        const {result, rerender} = renderHook(({value}) => useNoCodePreview(value), {
            initialProps: {value: operand},
            wrapper: withVariables([{source: InputVariableSource.ProcessData, path: 'termin', label: 'Termin'}]),
        });

        await waitFor(() => expect(result.current).toBe(template));
        const nested: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: parent.identifier,
            operands: isToday ? [operand, {type: 'NoCodeStaticValue', value: '18'}, {type: 'NoCodeStaticValue', value: 'jahre'}]
                : [{type: 'NoCodeProcessDataReference', path: 'termin'}, {type: 'NoCodeStaticValue', value: 'vor-oder-gleich'}, operand],
        };
        const original = structuredClone(nested);
        rerender({value: nested});
        expect(result.current).toBe(isToday ? '(heutiges Datum) minus 18 Jahre'
            : '„Termin“ ist früher als oder gleich (aktueller Zeitpunkt)');
        expect(nested).toEqual(original);
        expect(api.get).toHaveBeenCalledExactlyOnceWith('no-code/operators', {queryParams: undefined});
    });

    it.each([['true', 'Wahr'], ['false', 'Falsch']])('previews a fixed Boolean %s without quotes', async (value, label) => {
        api.get.mockResolvedValue([{
            ...operators[0],
            identifier: 'not',
            label: 'Nicht',
            humanReadableTemplate: 'nicht „#0“',
            signatures: [{
                returnType: NoCodeDataType.Boolean,
                parameters: [{
                    type: NoCodeDataType.Boolean,
                    label: 'Bedingung',
                    description: null,
                    options: [{label: 'Wahr', value: 'true'}, {label: 'Falsch', value: 'false'}],
                }],
            }],
        }]);
        const {result} = renderHook(() => useNoCodePreview({
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [{type: 'NoCodeStaticValue', value}],
        }));

        await waitFor(() => expect(result.current).toBe(`nicht ${label}`));
    });

    it.each([
        ['subtract-from-date', 'Vom Datum abziehen', 'minus', NoCodeDataType.Date, '2026-08-15', '15.08.2026'],
        ['add-to-date', 'Zum Datum hinzufügen', 'plus', NoCodeDataType.Date, '2026-08-15', '15.08.2026'],
        ['subtract-from-datetime', 'Vom Zeitpunkt abziehen', 'minus', NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '22.11.2026 13:30:00 Uhr'],
        ['add-to-datetime', 'Zum Zeitpunkt hinzufügen', 'plus', NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '22.11.2026 13:30:00 Uhr'],
    ] as const)('previews %s with the same temporal value and quantity grammar as the collapsed editor', async (identifier, label, direction, type, value, displayedValue) => {
        const arithmetic: NoCodeOperatorDetailsDTO = {
            ...operators[0],
            identifier,
            label,
            humanReadableTemplate: `#0 ${direction} #1 #2`,
            signatures: [{
                returnType: type,
                parameters: [{
                    label: type === NoCodeDataType.DateTime ? 'Ausgangszeitpunkt' : 'Ausgangsdatum',
                    type,
                    description: null,
                    options: [],
                }, {
                    label: 'Anzahl',
                    type: NoCodeDataType.Number,
                    description: null,
                    options: [],
                }, {
                    label: 'Einheit',
                    type: NoCodeDataType.String,
                    description: null,
                    quantityParameterIndex: 1,
                    options: [{label: 'Tage', value: 'tage', singularLabel: 'Tag'}],
                }],
            }],
        };
        const operand: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: identifier,
            operands: [
                {type: 'NoCodeStaticValue', value},
                {type: 'NoCodeStaticValue', value: '1'},
                {type: 'NoCodeStaticValue', value: 'tage'},
            ],
        };
        const now: NoCodeOperatorDetailsDTO = {
            ...operators[0],
            identifier: 'create-now',
            label: 'Aktueller Zeitpunkt',
            humanReadableTemplate: 'aktueller Zeitpunkt',
            signatures: [{returnType: NoCodeDataType.DateTime, parameters: []}],
        };
        api.get.mockResolvedValue(type === NoCodeDataType.DateTime ? [arithmetic, now] : [arithmetic]);
        const {result, rerender} = renderHook(({value}) => useNoCodePreview(value), {
            initialProps: {value: operand},
        });

        await waitFor(() => expect(result.current).toBe(`${displayedValue} ${direction} 1 Tag`));
        expect(result.current).toBe(summarizeNoCode(operand, [], [arithmetic]).text);

        const plural = {...operand, operands: [operand.operands![0], {type: 'NoCodeStaticValue' as const, value: '2'}, operand.operands![2]]};
        rerender({value: plural});
        expect(result.current).toBe(`${displayedValue} ${direction} 2 Tage`);
        expect(result.current).toBe(summarizeNoCode(plural, [], [arithmetic]).text);
        expect(operand.operands?.[0]).toEqual({type: 'NoCodeStaticValue', value});
        if (type === NoCodeDataType.DateTime) {
            const dynamic = {
                ...operand,
                operands: [{type: 'NoCodeExpression' as const, operatorIdentifier: 'create-now', operands: []}, operand.operands![1], operand.operands![2]],
            };
            rerender({value: dynamic});
            expect(result.current).toBe(`(aktueller Zeitpunkt) ${direction} 1 Tag`);
        }
        expect(api.get).toHaveBeenCalledExactlyOnceWith('no-code/operators', {queryParams: undefined});
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
