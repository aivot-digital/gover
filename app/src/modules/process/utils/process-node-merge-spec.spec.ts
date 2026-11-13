import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {literalAuthoredValue} from '../../../models/element-data';
import {threeWayMerge} from '../../../utils/three-way-merge';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {createProcessNodeMergeSpec} from './process-node-merge-spec';

const layout = {
    id: 'layout',
    type: ElementType.GroupLayout,
    children: [
        {id: 'subject', type: ElementType.Text, label: 'Betreff'},
        {id: 'formLayout', type: ElementType.Text, label: 'Formular'},
    ],
} as unknown as GroupLayout;

function node(overrides: Partial<ProcessNodeEntity>): ProcessNodeEntity {
    return {
        id: 1,
        processId: 1,
        processVersion: 1,
        processNodeDefinitionKey: 'key',
        processNodeDefinitionVersion: 1,
        name: 'Antrag',
        description: null,
        dataKey: 'antrag',
        configuration: {subject: literalAuthoredValue('Hallo')},
        outputMappings: {},
        deadlines: {},
        requirements: null,
        notes: null,
        savedWithErrors: false,
        created: '2026-11-01T00:00:00Z',
        updated: '2026-11-01T00:00:00Z',
        lockVersion: 1,
        ports: [],
        portResolutionProblems: [],
        deadlineDefinitions: [],
        ...overrides,
    };
}

describe('createProcessNodeMergeSpec', () => {
    const spec = createProcessNodeMergeSpec(layout);

    it('keeps the own changes on top of a newer save and takes its lock version', () => {
        const base = node({});
        const mine = node({name: 'Neuer Antrag'});
        const theirs = node({
            configuration: {
                subject: literalAuthoredValue('Hallo'),
                formLayout: literalAuthoredValue({id: 'root', type: ElementType.FormLayout, children: []}),
            },
            updated: '2026-11-02T00:00:00Z',
            savedWithErrors: true,
            lockVersion: 2,
        });

        const result = threeWayMerge(base, mine, theirs, spec);

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({...theirs, name: 'Neuer Antrag'});
    });

    it('names conflicting fields by their labels', () => {
        const base = node({});
        const mine = node({configuration: {subject: literalAuthoredValue('Guten Tag')}, notes: 'Meine Notiz'});
        const theirs = node({configuration: {subject: literalAuthoredValue('Moin')}, notes: 'Andere Notiz', lockVersion: 2});

        const result = threeWayMerge(base, mine, theirs, spec);

        expect(result.conflicts.map((conflict) => spec.label!(conflict.path, {base, mine, theirs})))
            .toEqual(['Betreff', 'Notizen']);
    });
});
