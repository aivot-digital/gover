import {describe, expect, it} from 'vitest';
import {
    findDynamicTextTokenMatch,
    getDynamicTextSyntaxSegments,
    splitDynamicTextSegments,
} from './dynamic-text-syntax';

describe('dynamic text parsing', () => {
    it('keeps an incomplete expression as plain text', () => {
        expect(findDynamicTextTokenMatch('Text {{ $.name')).toBeNull();
        expect(splitDynamicTextSegments('Text {{ $.name')).toEqual([
            {type: 'plain', value: 'Text {{ $.name'},
        ]);
    });

    it('separates variables and control flow without hiding their expressions', () => {
        const value = '{% if $.enabled %}Hallo {{ $.name }}{% else %}Nicht verfügbar{% endif %}';

        expect(splitDynamicTextSegments(value)).toEqual([
            {type: 'token', kind: 'directive', value: '{% if $.enabled %}'},
            {type: 'plain', value: 'Hallo '},
            {type: 'token', kind: 'output', value: '{{ $.name }}'},
            {type: 'token', kind: 'directive', value: '{% else %}'},
            {type: 'plain', value: 'Nicht verfügbar'},
            {type: 'token', kind: 'directive', value: '{% endif %}'},
        ]);
    });

    it('recognizes raw output and comments', () => {
        expect(splitDynamicTextSegments('{! $.html !}{# Hinweis #}')).toEqual([
            {type: 'token', kind: 'raw', value: '{! $.html !}'},
            {type: 'token', kind: 'comment', value: '{# Hinweis #}'},
        ]);
    });

    it('assigns lightweight syntax roles without changing the expression', () => {
        const value = '{% if $.count > 0 and active == true %}';
        const segments = getDynamicTextSyntaxSegments(value);

        expect(segments.map((segment) => [segment.role, segment.value])).toEqual([
            ['punctuation', '{%'],
            ['plain', ' '],
            ['keyword', 'if'],
            ['plain', ' '],
            ['property', '$.count'],
            ['plain', ' '],
            ['punctuation', '>'],
            ['plain', ' '],
            ['number', '0'],
            ['plain', ' '],
            ['keyword', 'and'],
            ['plain', ' active '],
            ['punctuation', '=='],
            ['plain', ' '],
            ['constant', 'true'],
            ['plain', ' '],
            ['punctuation', '%}'],
        ]);
        expect(segments.map((segment) => segment.value).join('')).toBe(value);
    });

    it('uses comment and string syntax roles', () => {
        expect(getDynamicTextSyntaxSegments('{# Hinweis #}')).toEqual([
            {role: 'punctuation', value: '{#'},
            {role: 'comment', value: ' Hinweis '},
            {role: 'punctuation', value: '#}'},
        ]);
        expect(getDynamicTextSyntaxSegments('{{ fallback("leer") }}'))
            .toEqual(expect.arrayContaining([
                {role: 'function', value: 'fallback'},
                {role: 'string', value: '"leer"'},
            ]));
    });
});
