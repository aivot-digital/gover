import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {threeWayMerge} from '../../../utils/three-way-merge';
import {describeElementConflict, formLayoutMergeSpec} from './form-layout-merge-spec';

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
            .toBe('Text „Rufname“, Eigenschaft „Titel“');
    });

    it('decides for the whole form when one side moved an element that the other side changed', () => {
        const base = layout([group('g1', 'Eins', [text('a', 'A')]), group('g2', 'Zwei', [])]);
        const mine = layout([group('g1', 'Eins', []), group('g2', 'Zwei', [text('a', 'A')])]);
        const theirs = layout([group('g1', 'Eins', [text('a', 'A2')]), group('g2', 'Zwei', [])]);

        const result = threeWayMerge(base, mine, theirs, formLayoutMergeSpec);

        expect(result.conflicts.map((conflict) => conflict.path)).toEqual([[]]);
        expect(formLayoutMergeSpec.label!([], {base, mine, theirs})).toBe('Gesamtes Formular');
    });

    it('groups different properties of a nested element by its stable ID', () => {
        const base = layout([group('g', 'Person', [text('a', 'Vorname')])]);
        const mine = layout([group('g', 'Person', [{...text('a', 'Rufname'), placeholder: 'Mein Beispiel'}])]);
        const theirs = layout([group('g', 'Person', [{...text('a', 'Vorname(n)'), placeholder: 'Anderes Beispiel'}])]);
        const states = {base, mine, theirs};
        const title = formLayoutMergeSpec.describeConflict!(['children', '[g]', 'children', '[a]', 'label'], states);
        const placeholder = formLayoutMergeSpec.describeConflict!(['children', '[g]', 'children', '[a]', 'placeholder'], states);

        expect(title).toEqual({group: {id: '["form","root","a"]', label: 'Text „Rufname“'}, propertyLabel: 'Titel'});
        expect(placeholder).toEqual({...title, propertyLabel: 'Platzhalter'});
        expect(formLayoutMergeSpec.describeConflict!(['children', '[g]', 'children', '[a]', 'label'], {...states, mine: base}).group.id)
            .toBe(title.group.id);
    });

    it('keeps equal element titles in separate groups and names removed elements from another state', () => {
        const base = layout([text('a', 'Name'), text('b', 'Name')]);
        const mine = layout([]);
        const states = {base, mine, theirs: base};
        const first = formLayoutMergeSpec.describeConflict!(['children', '[a]'], states);
        const second = formLayoutMergeSpec.describeConflict!(['children', '[b]'], states);

        expect(first.group.label).toBe('Text „Name“');
        expect(second.group.label).toBe(first.group.label);
        expect(second.group.id).not.toBe(first.group.id);
        expect(first.propertyLabel).toBe('Gesamtes Element');
    });

    it('groups root properties and the whole form together', () => {
        const value = layout([]);
        const states = {base: value, mine: value, theirs: value};
        const root = formLayoutMergeSpec.describeConflict!([], states);

        expect(root).toEqual({group: {id: '["form","root"]', label: 'Formular'}, propertyLabel: 'Gesamtes Formular'});
        expect(formLayoutMergeSpec.describeConflict!(['tabTitle'], states)).toEqual({...root, propertyLabel: 'Titel des Formulars im Browser-Tab'});
        expect(formLayoutMergeSpec.describeConflict!(['children', '#order'], states)).toEqual({...root, propertyLabel: 'Reihenfolge der Elemente'});
    });

    it('keeps list entries separate from element identity, even when an entry has an element ID', () => {
        const value = layout([{...text('a', 'Vorname'), options: [{id: 'b', label: 'Eine Option', value: 'x'}]}, text('b', 'Nachname')]);
        const description = describeElementConflict(['children', '[a]', 'options', '[b]', 'label'], [value], 'Formular');

        expect(description.group).toEqual({id: '["form","root","a"]', label: 'Text „Vorname“'});
        expect(description.propertyLabel).toBe('Optionen › Eintrag „Eine Option“ › Beschriftung');
    });

    it('falls back to an internal name or the element type when no title exists', () => {
        const value = layout([{id: 'a', type: ElementType.Text, label: ' ', name: 'intern'}, {id: 'b', type: ElementType.Text}]);

        expect(describeElementConflict(['children', '[a]', 'placeholder'], [value], 'Formular').group.label).toBe('Text „intern“');
        expect(describeElementConflict(['children', '[b]', 'placeholder'], [value], 'Formular').group.label).toBe('Text');
    });
});
