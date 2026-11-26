import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {literalAuthoredValue} from '../../../models/element-data';
import {InputMode, InputVariableSource} from '../../../models/input-mode';
import {type MergeChoices, threeWayMerge} from '../../../utils/three-way-merge';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {createProcessNodeMergeSpec, getExternallyEditedFields} from './process-node-merge-spec';

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

    it('resolves each inline UI definition independently and preserves other saved changes', () => {
        const schema = {...layout, children: ['form', 'other', 'external'].map((id) => ({
            id, type: ElementType.UiDefinitionInput, label: id, openExternalEditor: id === 'external',
        }))} as GroupLayout;
        const field = {id: 'mail', label: 'E-Mail', type: ElementType.Text};
        const form = (parent?: string) => literalAuthoredValue({
            id: 'root', type: ElementType.FormLayout, children: ['a', 'b'].map((id) => ({
                id, label: id.toUpperCase(), type: ElementType.GroupLayout, children: parent === id ? [field] : [],
            })),
        });
        const base = node({configuration: {form: form('a'), other: form('a'), external: form('a')}});
        const mine = {...base, name: 'Eigener Name', configuration: {form: form('b'), other: form('b'), external: form('b')}};
        const theirs = {...base, notes: 'Gespeicherte Notiz', lockVersion: 2, configuration: {form: form(), other: form(), external: form()}};
        const inlineSpec = createProcessNodeMergeSpec(schema, theirs);
        const {conflicts} = threeWayMerge(base, mine, theirs, inlineSpec);
        expect(conflicts).toHaveLength(2);
        const choices: MergeChoices = {};
        for (const conflict of conflicts) {
            expect(conflict.elementPlacement).toMatchObject({elementId: 'mail'});
            expect(conflict.elementPlacement?.mine).toContain('B');
            expect(conflict.elementPlacement?.theirs).toBeUndefined();
            choices[conflict.id] = conflict.path[1] === 'form' ? 'mine' : 'theirs';
        }
        expect(threeWayMerge(base, mine, theirs, inlineSpec, choices)).toEqual({
            merged: {...theirs, name: mine.name, configuration: {...theirs.configuration, form: mine.configuration.form}},
            conflicts: [],
        });
    });

    it('adopts externally edited configuration keys from the stored node, including removed keys', () => {
        const externalLayout = {...layout, children: ['form', 'removed'].map((id) => ({
            id, type: ElementType.UiDefinitionInput, openExternalEditor: true,
        }))} as GroupLayout;
        const base = node({configuration: {form: literalAuthoredValue('Old'), removed: literalAuthoredValue('Old')}});
        const mine = {...base, name: 'Lokale Änderung', configuration: {form: literalAuthoredValue('Stale'), removed: literalAuthoredValue('Stale')}};
        const theirs = {...base, lockVersion: 2, configuration: {form: literalAuthoredValue('Current')}};
        expect(getExternallyEditedFields(externalLayout)).toEqual(['form', 'removed']);
        expect(threeWayMerge(base, mine, theirs, createProcessNodeMergeSpec(externalLayout, theirs)))
            .toEqual({merged: {...theirs, name: mine.name}, conflicts: []});
    });

    it('limits the structural fallback to the inline definition when the destination was also deleted', () => {
        const schema = {...layout, children: [{id: 'task', type: ElementType.UiDefinitionInput}]} as GroupLayout;
        const field = {id: 'email', type: ElementType.Text, label: 'E-Mail'};
        const group = (id: string, children: typeof field[]) => ({id, type: ElementType.GroupLayout, children});
        const form = (...groups: ReturnType<typeof group>[]) => literalAuthoredValue({
            id: 'root', type: ElementType.FormLayout, children: groups,
        });
        const base = node({configuration: {task: form(group('a', [field]), group('b', []))}});
        const mine = {...base, name: 'Lokaler Name', configuration: {task: form(group('a', []), group('b', [field]))}};
        const theirs = {...base, lockVersion: 2, notes: 'Gespeicherte Notizen', configuration: {task: form(group('a', []))}};
        const spec = createProcessNodeMergeSpec(schema, theirs);
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts.map(({path}) => path)).toEqual([['configuration', 'task', 'value']]);
        for (const choice of ['mine', 'theirs'] as const) {
            const resolved = threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: choice});
            expect(resolved).toEqual({conflicts: [], merged: {...theirs, name: mine.name,
                configuration: choice === 'mine' ? mine.configuration : theirs.configuration}});
        }
    });

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

    it('decides an authored value as a whole when one side changed its input mode', () => {
        const base = node({});
        const lowCode = {type: InputMode.LowCode, code: 'return "Dynamisch";'} as const;
        const mine = node({configuration: {subject: lowCode}});
        const theirs = node({configuration: {subject: literalAuthoredValue('Gespeichert')}, lockVersion: 2});

        const result = threeWayMerge(base, mine, theirs, spec);

        expect(result.conflicts.map((conflict) => conflict.id)).toEqual(['configuration/subject']);
        expect(spec.label!(result.conflicts[0].path, {base, mine, theirs})).toEqual('Betreff');
        expect(threeWayMerge(base, mine, theirs, spec, {'configuration/subject': 'theirs'}).merged.configuration)
            .toEqual({subject: literalAuthoredValue('Gespeichert')});
        expect(threeWayMerge(base, mine, theirs, spec, {'configuration/subject': 'mine'}).merged.configuration)
            .toEqual({subject: lowCode});
    });

    it('resolves a variable in a repeatable configuration row without losing its sibling fields', () => {
        const row = (nodeDataKey: string, path: string, first: string, second: string) => literalAuthoredValue([{
            id: 'row-1',
            values: {
                recipient: {type: InputMode.Variable, reference: {source: InputVariableSource.ElementData, nodeDataKey, path}},
                first: literalAuthoredValue(first), second: literalAuthoredValue(second),
            },
        }]);
        const fromRow = (rows: ReturnType<typeof row>) => node({configuration: {rows}});
        const base = fromRow(row('review', 'email', 'A', 'B'));
        const mine = fromRow(row('manager', 'email', 'A1', 'B'));
        const theirs = fromRow(row('review', 'name', 'A', 'B2'));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts.map(({id}) => id)).toEqual(['configuration/rows/value/[row-1]/values/recipient']);
        expect(threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: 'mine'}))
            .toEqual({merged: fromRow(row('manager', 'email', 'A1', 'B2')), conflicts: []});
    });

    it('groups general properties separately from individual configuration fields', () => {
        const value = node({});
        const states = {base: value, mine: value, theirs: value};
        const name = spec.describeConflict!(['name'], states);

        expect(name).toEqual({group: {id: '["process-node",1,"general"]', label: 'Allgemeine Angaben'}, propertyLabel: 'Name des Elementes'});
        expect(spec.describeConflict!(['notes'], states)).toEqual({...name, propertyLabel: 'Notizen'});
        expect(spec.describeConflict!(['configuration', 'subject', 'value'], states)).toEqual({
            group: {id: '["process-node",1,"configuration","subject"]', label: 'Betreff'}, propertyLabel: 'Wert',
        });
        expect(spec.describeConflict!([], states)).toEqual({...name, propertyLabel: 'Gesamte Konfiguration des Prozesselements'});
    });

    it('keeps an embedded form root in its field group and groups its elements by ID', () => {
        const form = {
            id: 'root', type: ElementType.FormLayout,
            children: [{id: 'first', type: ElementType.Text, label: 'Vorname'}],
        };
        const value = node({configuration: {formLayout: literalAuthoredValue(form)}});
        const states = {base: value, mine: value, theirs: value};
        const root = spec.describeConflict!(['configuration', 'formLayout', 'value', 'publicTitle'], states);

        expect(root).toEqual({
            group: {id: '["process-node",1,"configuration","formLayout"]', label: 'Formular'},
            propertyLabel: 'Öffentlicher Titel & Überschrift des Formulars',
        });
        expect(spec.describeConflict!(['configuration', 'formLayout', 'value'], states)).toEqual({...root, propertyLabel: 'Gesamtes Formular'});
        expect(spec.describeConflict!(['configuration', 'formLayout', 'value', 'children', '[first]', 'placeholder'], states)).toEqual({
            group: {id: '["process-node",1,"configuration","formLayout","first"]', label: 'Formular › Texteingabe „Vorname“'},
            propertyLabel: 'Platzhalter',
        });
    });

    it('preserves unknown configuration and output mapping identifiers', () => {
        const value = node({configuration: {extension: literalAuthoredValue({placeholder: 'Beispiel'})}});
        const states = {base: value, mine: value, theirs: value};

        expect(spec.describeConflict!(['configuration', 'extension', 'value', 'placeholder'], states)).toEqual({
            group: {id: '["process-node",1,"configuration","extension"]', label: 'extension'}, propertyLabel: 'placeholder',
        });
        expect(spec.describeConflict!(['outputMappings', 'placeholder'], states).propertyLabel).toBe('Zielpfad in den Vorgangsdaten › placeholder');
    });

    it('uses deadline labels and their editable property names', () => {
        const value = node({deadlineDefinitions: [{key: 'review', label: 'Prüfung', description: '', port: {key: 'expired', label: '', description: '', deadlineKey: 'review'}}]});
        const states = {base: value, mine: value, theirs: value};

        expect(spec.describeConflict!(['deadlines', 'review', 'strict'], states).propertyLabel)
            .toBe('Frist › Prüfung › Aufgabe bei Fristüberschreitung beenden');
    });
});
