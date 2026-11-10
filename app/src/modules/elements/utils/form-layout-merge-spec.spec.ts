import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {threeWayMerge} from '../../../utils/three-way-merge';
import {formLayoutMergeSpec} from './form-layout-merge-spec';

function layout(children: unknown[]): FormLayoutElement {
    return {id: 'root', type: ElementType.FormLayout, name: 'Formular', children} as unknown as FormLayoutElement;
}

function text(id: string, label: string) {
    return {id, type: ElementType.Text, name: null, label};
}

function group(id: string, label: string, children: unknown[]) {
    return {id, type: ElementType.GroupLayout, name: null, label, children};
}

describe('formLayoutMergeSpec', () => {
    it('combines changes to different elements of the same form', () => {
        const base = layout([text('a', 'Vorname'), text('b', 'Nachname')]);
        const mine = layout([text('a', 'Vorname*'), text('b', 'Nachname')]);
        const theirs = layout([text('a', 'Vorname'), text('b', 'Familienname')]);

        const result = threeWayMerge(base, mine, theirs, formLayoutMergeSpec);

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual(layout([text('a', 'Vorname*'), text('b', 'Familienname')]));
    });

    it('names conflicting element properties by the element', () => {
        const base = layout([group('g', 'Person', [text('a', 'Vorname')])]);
        const mine = layout([group('g', 'Person', [text('a', 'Rufname')])]);
        const theirs = layout([group('g', 'Person', [text('a', 'Vorname(n)')])]);

        const result = threeWayMerge(base, mine, theirs, formLayoutMergeSpec);

        expect(result.conflicts).toHaveLength(1);
        expect(formLayoutMergeSpec.label!(result.conflicts[0].path, {base, mine, theirs}))
            .toBe('Text „Rufname“, Eigenschaft „label“');
    });

    it('decides for the whole form when one side moved an element that the other side changed', () => {
        const base = layout([group('g1', 'Eins', [text('a', 'A')]), group('g2', 'Zwei', [])]);
        const mine = layout([group('g1', 'Eins', []), group('g2', 'Zwei', [text('a', 'A')])]);
        const theirs = layout([group('g1', 'Eins', [text('a', 'A2')]), group('g2', 'Zwei', [])]);

        const result = threeWayMerge(base, mine, theirs, formLayoutMergeSpec);

        expect(result.conflicts.map((conflict) => conflict.path)).toEqual([[]]);
        expect(formLayoutMergeSpec.label!([], {base, mine, theirs})).toBe('Gesamtes Formular');
    });
});
