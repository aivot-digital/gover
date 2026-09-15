import {describe, expect, it} from 'vitest';
import {
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
