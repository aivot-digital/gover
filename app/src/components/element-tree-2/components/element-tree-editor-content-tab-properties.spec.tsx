import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {type ElementWithParents} from '../../../utils/flatten-elements';
import {
    collectHttpMappingProblems,
    destinationKeysOverlap,
    resolveEffectiveDestinationKey,
} from './element-tree-editor-content-tab-properties-utils';

const root = {
    id: 'root',
    type: ElementType.FormLayout,
    name: null,
    children: [],
} as unknown as AnyElement;

describe('resolveEffectiveDestinationKey', () => {
    it('should include replicating container parent context', () => {
        const outer = createReplicatingContainer('outerList', 'outer');
        const inner = createReplicatingContainer('innerList', 'inner');
        const field = createTextField('field', 'value');

        expect(resolveEffectiveDestinationKey(withParents(field, [root, outer, inner]))).toBe('outer.*.inner.*.value');
    });

    it('should ignore descendants when a replicating parent has no destination key', () => {
        const list = createReplicatingContainer('list', null);
        const field = createTextField('field', 'value');

        expect(resolveEffectiveDestinationKey(withParents(field, [root, list]))).toBeNull();
    });
});

describe('destinationKeysOverlap', () => {
    it('should treat wildcard and concrete array segments as overlapping', () => {
        expect(destinationKeysOverlap('list.*.field', 'list.0.field')).toBe(true);
        expect(destinationKeysOverlap('list.*.field', 'list.0.other')).toBe(false);
    });
});

describe('collectHttpMappingProblems', () => {
    it('should not warn for equal relative keys in different structural contexts', () => {
        const globalField = withParents(createTextField('globalField', 'einzelnachweisOhneDateiname'), [root]);
        const list = createReplicatingContainer('replicatingList', 'replizierendeListe');
        const listField = withParents(createTextField('listField', 'einzelnachweisOhneDateiname'), [root, list]);

        expect(collectHttpMappingProblems(globalField, [listField])).toHaveLength(0);
        expect(collectHttpMappingProblems(listField, [globalField])).toHaveLength(0);
    });

    it('should still warn for equal relative keys in the same replicating container', () => {
        const list = createReplicatingContainer('replicatingList', 'replizierendeListe');
        const firstField = withParents(createTextField('firstField', 'einzelnachweisOhneDateiname'), [root, list]);
        const secondField = withParents(createTextField('secondField', 'einzelnachweisOhneDateiname'), [root, list]);

        expect(collectHttpMappingProblems(firstField, [secondField])).toHaveLength(1);
    });

    it('should not warn for equal relative keys in different replicating containers', () => {
        const firstList = createReplicatingContainer('firstList', 'ersteListe');
        const secondList = createReplicatingContainer('secondList', 'zweiteListe');
        const firstField = withParents(createTextField('firstField', 'einzelnachweisOhneDateiname'), [root, firstList]);
        const secondField = withParents(createTextField('secondField', 'einzelnachweisOhneDateiname'), [root, secondList]);

        expect(collectHttpMappingProblems(firstField, [secondField])).toHaveLength(0);
    });

    it('should warn for indexed keys overlapping with list child keys', () => {
        const indexedField = withParents(createTextField('indexedField', 'replizierendeListe.0.einzelnachweisOhneDateiname'), [root]);
        const list = createReplicatingContainer('replicatingList', 'replizierendeListe');
        const listField = withParents(createTextField('listField', 'einzelnachweisOhneDateiname'), [root, list]);

        expect(collectHttpMappingProblems(indexedField, [listField])).toHaveLength(1);
    });

    it('should not warn between a replicating container and its own descendants', () => {
        const list = createReplicatingContainer('replicatingList', 'replizierendeListe');
        const listElement = withParents(list, [root]);
        const listField = withParents(createTextField('listField', 'einzelnachweisOhneDateiname'), [root, list]);

        expect(collectHttpMappingProblems(listElement, [listField])).toHaveLength(0);
        expect(collectHttpMappingProblems(listField, [listElement])).toHaveLength(0);
    });

    it('should keep warnings for normal duplicate and nested destination keys', () => {
        const firstField = withParents(createTextField('firstField', 'person'), [root]);
        const duplicateField = withParents(createTextField('duplicateField', 'person'), [root]);
        const nestedField = withParents(createTextField('nestedField', 'person.name'), [root]);

        expect(collectHttpMappingProblems(firstField, [duplicateField])).toHaveLength(1);
        expect(collectHttpMappingProblems(firstField, [nestedField])).toHaveLength(1);
    });
});

function createTextField(id: string, destinationKey: string | null): AnyElement {
    return {
        id,
        type: ElementType.Text,
        label: id,
        name: null,
        destinationKey,
    } as unknown as AnyElement;
}

function createReplicatingContainer(id: string, destinationKey: string | null): AnyElement {
    return {
        id,
        type: ElementType.ReplicatingContainer,
        label: id,
        name: null,
        destinationKey,
        children: [],
    } as unknown as AnyElement;
}

function withParents(element: AnyElement, parents: AnyElement[]): ElementWithParents {
    return {
        element,
        parents,
    };
}
