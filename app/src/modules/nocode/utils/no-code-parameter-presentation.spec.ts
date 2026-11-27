import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeParameter} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {resolveNoCodeExpressionPresentation} from './no-code-parameter-presentation';

const literal = (value: string | null): NoCodeOperand => ({type: 'NoCodeStaticValue', value});

function parameters(): NoCodeParameter[] {
    return [{
        type: NoCodeDataType.Date,
        label: 'Datum',
        description: null,
        sentencePhrase: 'wenn',
        options: [],
    }, {
        type: NoCodeDataType.String,
        label: 'Vergleichsart',
        description: null,
        sentencePhrase: '',
        optionsAsSentencePart: true,
        options: [{label: 'früher als', value: 'vor'}],
    }, {
        type: NoCodeDataType.Date,
        label: 'Vergleichsdatum',
        description: null,
        sentencePhrase: '',
        sentenceSuffix: 'ist',
        options: [],
    }];
}

describe('resolveNoCodeExpressionPresentation', () => {
    it('uses sentence wording when the declared relationship is a recognized fixed choice', () => {
        const declared = parameters();
        const original = structuredClone(declared);
        const result = resolveNoCodeExpressionPresentation(declared, [literal('2026-11-21'), literal('vor'), literal('2026-11-22')]);

        expect(result.useSentence).toBe(true);
        expect(result.parameters[0].sentencePhrase).toBe('wenn');
        expect(result.parameters[2].sentenceSuffix).toBe('ist');
        expect(declared).toEqual(original);
    });

    it.each([
        null,
        undefined,
        literal(null),
        literal(''),
        literal('unbekannt'),
        {type: 'NoCodeReference', elementId: 'comparison'},
        {type: 'NoCodeProcessDataReference', path: 'comparison'},
        {type: 'NoCodeExpression', operatorIdentifier: 'value', operands: [literal('vor')]},
    ] satisfies (NoCodeOperand | null | undefined)[])('uses field labels for an unresolved relationship %j without evaluating sources', relationship => {
        const declared = parameters();
        const original = structuredClone(declared);
        const result = resolveNoCodeExpressionPresentation(declared, [literal('2026-11-21'), relationship, literal('2026-11-22')]);

        expect(result.useSentence).toBe(false);
        expect(result.parameters.map(parameter => parameter.label)).toEqual(['Datum', 'Vergleichsart', 'Vergleichsdatum']);
        expect(result.parameters.every(parameter => parameter.sentencePhrase == null && parameter.sentenceSuffix == null)).toBe(true);
        expect(result.parameters[1].options).toEqual(declared[1].options);
        expect(declared).toEqual(original);
    });

    it('does not treat an arbitrary literal as a declared relationship without fixed options', () => {
        const declared = parameters();
        declared[1].options = [];
        expect(resolveNoCodeExpressionPresentation(declared, [literal('a'), literal('vor'), literal('b')]).useSentence).toBe(false);
    });

    it.each([undefined, false])('preserves legacy sentence behavior when the relationship flag is %j', flag => {
        const declared = parameters();
        declared[1].optionsAsSentencePart = flag;
        const result = resolveNoCodeExpressionPresentation(declared, [literal('a'), {type: 'NoCodeProcessDataReference', path: 'comparison'}, literal('b')]);

        expect(result.useSentence).toBe(true);
        expect(result.parameters[0].sentencePhrase).toBe('wenn');
        expect(result.parameters[2].sentenceSuffix).toBe('ist');
    });

    it('keeps quantity grammar and numeric constraints when another relationship needs field labels', () => {
        const declared: NoCodeParameter[] = [parameters()[1], {
            type: NoCodeDataType.Number,
            label: 'Anzahl',
            description: null,
            sentencePhrase: 'plus',
            numberConstraints: {integerOnly: true, minimum: -2147483648, maximum: 2147483647},
            options: [],
        }, {
            type: NoCodeDataType.String,
            label: 'Einheit',
            description: null,
            sentencePhrase: '',
            quantityParameterIndex: 1,
            options: [{label: 'Tage', value: 'tage', singularLabel: 'Tag'}],
        }];
        const original = structuredClone(declared);
        const result = resolveNoCodeExpressionPresentation(declared, [null, literal('-1'), literal('tage')]);

        expect(result.useSentence).toBe(false);
        expect(result.parameters[2].options[0].label).toBe('Tag');
        expect(result.parameters[1].numberConstraints).toEqual(declared[1].numberConstraints);
        expect(result.parameters[0].optionsAsSentencePart).toBe(true);
        expect(declared).toEqual(original);
    });

    it('requires every marked relationship to be known before using sentence wording', () => {
        const declared = [parameters()[1], {...parameters()[1], label: 'Weitere Vergleichsart'}];
        expect(resolveNoCodeExpressionPresentation(declared, [literal('vor'), literal('vor')]).useSentence).toBe(true);
        expect(resolveNoCodeExpressionPresentation(declared, [literal('vor'), null]).useSentence).toBe(false);
    });
});
