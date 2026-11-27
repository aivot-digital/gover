import {describe, expect, it} from 'vitest';
import {
    getDynamicTextInsertionReference,
    getDynamicTextTokenTitle,
    getDynamicTextVariableReferences,
} from './dynamic-text-metadata';

describe('dynamic text metadata', () => {
    it('extracts distinct references from expressions', () => {
        expect(getDynamicTextVariableReferences('{% if $.count > $.limit %}')).toEqual([
            '$.count',
            '$.limit',
        ]);
    });

    it('inserts Markdown values as text unless they keep their formatting', () => {
        expect(getDynamicTextInsertionReference('$.nachricht', true, 'text')).toBe('_text_v1.fromMarkdown($.nachricht)');
        expect(getDynamicTextInsertionReference('$.nachricht', true, 'markdown')).toBe('$.nachricht');
        expect(getDynamicTextInsertionReference('$.vorname', undefined, 'text')).toBe('$.vorname');
    });

    it('finds the variable inside the Markdown conversion for the token title', () => {
        expect(getDynamicTextVariableReferences('{{ _text_v1.fromMarkdown($.nachricht) }}')).toEqual(['$.nachricht']);
    });

    it('formats available metadata and keeps unknown references visible', () => {
        expect(getDynamicTextTokenTitle('{% if $.count > $.limit %}', [{
            reference: '$.count',
            label: 'Anzahl',
            category: 'Vorgangsdaten',
            origin: 'Warenkorb laden',
            description: 'Anzahl der Positionen',
        }])).toBe(
            'Anzahl\nVorgangsdaten · Erzeugt von Warenkorb laden\nAnzahl der Positionen\n$.count\n\n$.limit',
        );
    });
});
