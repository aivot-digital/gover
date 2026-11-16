import {describe, expect, it} from 'vitest';
import {
    buildProcessDataStructureTree,
    filterProcessDataStructureTree,
    hasMixedStructure,
    getDataStructureTabEntries,
    matchesDataStructureTab,
} from './process-data-structure';
import {ProcessDataStructureCategory, type ProcessDataStructureEntry} from '../entities/process-data-structure';

function entry(path: string, nodeId = 1): ProcessDataStructureEntry {
    return {
        category: ProcessDataStructureCategory.ProcessData,
        unexpandedStructure: false,
        path,
        label: 'Vorname',
        description: null,
        origin: {
            nodeId,
            dataKey: `node${nodeId}`,
            name: 'Formular',
        },
    };
}

describe('process data structure', () => {
    it('preserves parent definitions, multiple origins, nested arrays and concrete indices', () => {
        const tree = buildProcessDataStructureTree([
            entry('people'), entry('people[*].name'), entry('people[*].name', 2), entry('people[*].name'),
            entry('people[0].age'), entry('people[*].children[*].name'),
        ]);
        const people = tree.children.get('people')!;
        expect(people.entries).toHaveLength(1);
        expect([...people.children.keys()]).toEqual(['*', '0']);
        expect(people.children.get('*')!.children.get('name')!.entries).toHaveLength(2);
        expect(people.children.get('*')!.children.get('children')!.children.get('*')!.children.get('name')!.path)
            .toBe('people[*].children[*].name');
        expect(hasMixedStructure(people)).toBe(false);
    });

    it('detects mixed containers without treating parent definitions as conflicts', () => {
        const tree = buildProcessDataStructureTree([entry('people'), entry('people.name'), entry('people[*].name')]);
        expect(hasMixedStructure(tree.children.get('people')!)).toBe(true);
        expect(hasMixedStructure(tree)).toBe(false);
    });

    it('filters by path, label and origin while keeping ancestors', () => {
        const tree = buildProcessDataStructureTree([entry('person.name'), {
            ...entry('other.value'),
            label: 'Alter',
            origin: {
                nodeId: 2,
                name: 'Prüfung',
                dataKey: 'review',
            },
        }]);
        for (const search of ['$.PERSON.name', 'Vorname', 'Formular', 'node1']) {
            expect([...filterProcessDataStructureTree(tree, search)!.children.keys()]).toEqual(['person']);
        }
        expect(filterProcessDataStructureTree(tree, 'missing')).toBeNull();
        expect(filterProcessDataStructureTree(tree, '')).toBe(tree);
    });

    it('supports system entries without an origin and searches the selected prefix', () => {
        const tree = buildProcessDataStructureTree([{
            ...entry('initialPayload'),
            category: ProcessDataStructureCategory.ProtectedProcessData,
            origin: null,
            unexpandedStructure: true,
        }]);
        expect(filterProcessDataStructureTree(tree, '$$.initialPayload', '$$')?.children.size).toBe(1);
        expect(filterProcessDataStructureTree(tree, '_.initialPayload', '$$')).toBeNull();
        expect(tree.children.get('initialPayload')?.entries[0].unexpandedStructure).toBe(true);
    });

    it('separates element metadata from protected data without losing complete references', () => {
        const entries = [
            {
                ...entry('taskMetadata'),
                category: ProcessDataStructureCategory.ProtectedProcessData,
                origin: null,
            },
            {
                ...entry('taskMetadata.review.finished'),
                category: ProcessDataStructureCategory.ProtectedProcessData,
            },
            {
                ...entry('taskMetadataExtra'),
                category: ProcessDataStructureCategory.ProtectedProcessData,
            },
            {
                ...entry('caseNumber'),
                category: ProcessDataStructureCategory.ProtectedProcessData,
                origin: null,
            },
            entry('taskMetadata'),
        ];
        expect(getDataStructureTabEntries(entries, 'ElementMetadata').map(value => value.path)).toEqual(['review.finished']);
        expect(getDataStructureTabEntries(entries, ProcessDataStructureCategory.ProtectedProcessData).map(value => value.path))
            .toEqual(['taskMetadataExtra', 'caseNumber']);
        expect(getDataStructureTabEntries(entries, ProcessDataStructureCategory.ProcessData).map(value => value.path)).toEqual(['taskMetadata']);
        expect(entries[1].path).toBe('taskMetadata.review.finished');
        expect(matchesDataStructureTab(ProcessDataStructureCategory.ProtectedProcessData, 'taskMetadata.invalid-path', 'ElementMetadata')).toBe(true);
        expect(matchesDataStructureTab(ProcessDataStructureCategory.ProtectedProcessData, 'taskMetadata.invalid-path', ProcessDataStructureCategory.ProtectedProcessData)).toBe(false);
        expect(matchesDataStructureTab(ProcessDataStructureCategory.ProtectedProcessData, null, 'ElementMetadata')).toBe(true);
    });

    it('keeps special property names as data and rejects malformed paths', () => {
        const tree = buildProcessDataStructureTree([entry('__proto__.constructor'), entry('invalid-key'), entry('')]);
        expect([...tree.children.keys()]).toEqual(['__proto__']);
    });
});
