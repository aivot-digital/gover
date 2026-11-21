import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessDataStructureDialog} from './process-data-structure-dialog';
import {ProcessDefinitionVersionApiService} from '../../../services/process-definition-version-api-service';
import {
    ProcessDataStructureCategory,
    ProcessDataStructureIssueKind,
    type ProcessDataStructure,
} from '../../../entities/process-data-structure';
import {copyToClipboardText} from '../../../../../utils/copy-to-clipboard';

vi.mock('../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../services/process-definition-version-api-service', () => ({ProcessDefinitionVersionApiService: vi.fn()}));
vi.mock('../../../../../utils/copy-to-clipboard', () => ({copyToClipboardText: vi.fn().mockResolvedValue(true)}));
const load = vi.fn();
const listHint = 'Dieser Datenpfad bezeichnet die gesamte Liste. Unter [*] sehen Sie den Aufbau der einzelnen Listeneinträge.';
const listItemHint = '[*] ist ein Platzhalter für die einzelnen Einträge dieser Liste.';
const data: ProcessDataStructure = {
    entries: [1, 2].map(nodeId => ({
        category: ProcessDataStructureCategory.ProcessData,
        unexpandedStructure: false,
        path: 'person.name',
        label: 'Name',
        description: 'Vollständiger Name',
        origin: {
            nodeId,
            name: `Formular ${nodeId}`,
            dataKey: `form${nodeId}`,
        },
    })),
    issues: [],
};
const props = {
    processId: 10,
    processVersion: 3,
    savedRevision: 1,
    onClose: vi.fn(),
    onSelectNode: vi.fn(),
};

beforeEach(() => {
    load.mockReset().mockResolvedValue(data);
    vi.mocked(ProcessDefinitionVersionApiService).mockImplementation(function () {
        return {getDataStructure: load} as unknown as ProcessDefinitionVersionApiService;
    });
});

describe('ProcessDataStructureDialog', () => {
    it('shows the saved-state limitation, searches, copies full paths and navigates to every origin', async () => {
        const user = userEvent.setup();
        render(<ProcessDataStructureDialog {...props}/>);
        expect(screen.getByText(/Ungespeicherte Änderungen werden nicht berücksichtigt/)).toBeVisible();
        expect(screen.getByText(/Dynamisch erzeugte Pfade können fehlen/)).toBeVisible();
        expect(screen.queryByText('Hinweise zur Vorschau')).not.toBeInTheDocument();
        expect(within(screen.getByRole('heading', {level: 2})).getByText('Datenstruktur')).toBeVisible();
        expect(screen.queryByText('Version 3')).not.toBeInTheDocument();
        const search = await screen.findByRole('searchbox', {name: /Vorgangsdaten durchsuchen/});
        fireEvent.change(search, {target: {value: 'form2'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$.person.name'})).getByText('name'));
        expect(screen.getAllByRole('button', {name: /^Zum Prozesselement/})).toHaveLength(2);
        expect(screen.getAllByText('Vollständiger Name')).toHaveLength(1);
        expect(screen.getByRole('heading', {
            level: 4,
            name: 'Name',
        })).toBeVisible();
        expect(search).not.toHaveAccessibleName(/optional/);
        expect(screen.getByText('1 von 1 Pfad angezeigt')).toBeVisible();
        expect(screen.getByText(/Dieser Pfad ist in zwei Prozesselementen definiert/)).toBeVisible();
        expect(screen.getByText('Definiert in')).toBeVisible();
        expect(screen.queryByRole('button', {name: /Aktualisieren/})).not.toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Schließen'})).toHaveLength(1);
        expect(screen.queryByText(/Die Übersicht legt keine Datentypen fest/)).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenCalledWith('$.person.name');
        expect(await screen.findByRole('tooltip', {name: 'Pfad kopiert'})).toBeInTheDocument();
        await user.click(screen.getAllByRole('button', {name: /^Zum Prozesselement/})[1]);
        expect(props.onSelectNode).toHaveBeenCalledWith(2);
        expect(load).toHaveBeenCalledWith({
            processDefinitionId: 10,
            processDefinitionVersion: 3,
        });
    });

    it('separates categories, resets search and selection, and copies each complete namespace', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: [
                ...data.entries,
                {
                    ...data.entries[0],
                    category: ProcessDataStructureCategory.ElementData,
                    path: 'form1.response',
                    label: 'Antwort',
                    unexpandedStructure: true,
                },
                {
                    category: ProcessDataStructureCategory.ProtectedProcessData,
                    path: 'initialPayload',
                    label: 'Initiale Vorgangsdaten',
                    description: null,
                    origin: null,
                    unexpandedStructure: true,
                },
                {
                    category: ProcessDataStructureCategory.ProtectedProcessData,
                    path: 'taskMetadata.form1.finished',
                    label: 'Abschlusszeitpunkt',
                    description: null,
                    origin: data.entries[0].origin,
                    unexpandedStructure: false,
                },
            ],
            issues: [{
                category: ProcessDataStructureCategory.ElementData,
                kind: ProcessDataStructureIssueKind.MetadataUnavailable,
                origin: data.entries[0].origin,
                path: null,
            }],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        await screen.findByText('1 definierter Pfad');
        expect(screen.getByRole('tab', {name: 'Vorgangsdaten ($)'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: 'person'}});

        await user.click(screen.getByRole('tab', {name: 'Elementdaten (_)'}));
        expect(screen.getByRole('searchbox', {name: 'Elementdaten durchsuchen'})).toHaveValue('');
        expect(screen.getByRole('heading', {name: 'Pfad auswählen'})).toBeVisible();
        expect(screen.getByRole('alert')).toHaveTextContent('Die Übersicht ist unvollständig.');
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: '_.form1.response'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '_.form1.response'})).getByText('response'));
        expect(screen.getByText(/Unterstruktur nicht aufgeschlüsselt/)).toBeVisible();
        expect(screen.getByText('Erzeugt von')).toBeVisible();
        expect(screen.getByRole('note')).toHaveTextContent('Unterstruktur nicht aufgeschlüsselt');
        expect(screen.getByRole('heading', {name: 'Antwort'}).compareDocumentPosition(screen.getByRole('note')))
            .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('_.form1.response');

        await user.click(screen.getByRole('tab', {name: 'Geschützte Vorgangsdaten ($$)'}));
        expect(screen.getByRole('tabpanel', {name: 'Geschützte Vorgangsdaten ($$)'})).toBeVisible();
        expect(screen.getByText('1 definierter Pfad')).toBeVisible();
        expect(screen.getByRole('searchbox', {name: 'Geschützte Vorgangsdaten durchsuchen'})).toBeVisible();
        expect(screen.queryByRole('treeitem', {name: '$$.taskMetadata'})).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        await user.click(within(screen.getByRole('treeitem', {name: '$$.initialPayload'})).getByText('initialPayload'));
        expect(screen.getByRole('heading', {name: 'Initiale Vorgangsdaten'})).toBeVisible();
        expect(screen.getByRole('note')).toHaveTextContent('Vom System bereitgestellt.');
        expect(screen.queryByRole('button', {name: /^Zum Prozesselement/})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('$$.initialPayload');
        await user.click(screen.getByRole('tab', {name: 'Elementmetadaten ($$.taskMetadata)'}));
        expect(screen.getByRole('searchbox', {name: 'Elementmetadaten durchsuchen'})).toHaveValue('');
        expect(screen.queryByRole('treeitem', {name: '$$.initialPayload'})).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: '$$.taskMetadata.form1.finished'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$$.taskMetadata.form1.finished'})).getByText('finished'));
        expect(screen.getByText('Prozesselement')).toBeVisible();
        expect(screen.queryByText(/Unterstruktur nicht aufgeschlüsselt/)).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('$$.taskMetadata.form1.finished');
        await user.click(screen.getByRole('button', {name: 'Zum Prozesselement Formular 1'}));
        expect(props.onSelectNode).toHaveBeenLastCalledWith(1);
        expect(load).toHaveBeenCalledOnce();
    });

    it('explains an element metadata container once and links its actual process element', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: ['finished', 'started'].map(path => ({
                ...data.entries[0],
                category: ProcessDataStructureCategory.ProtectedProcessData,
                path: `taskMetadata.form1.${path}`,
            })),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        await screen.findByText('Keine Datenpfade in dieser Kategorie ermittelt.');
        await user.click(screen.getByRole('tab', {name: 'Elementmetadaten ($$.taskMetadata)'}));
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: 'finished'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$$.taskMetadata.form1'})).getByText('form1'));
        expect(screen.getByRole('heading', {name: 'Elementmetadaten'})).toBeVisible();
        expect(screen.getAllByText('Enthält zwei direkt untergeordnete Pfade.')).toHaveLength(1);
        expect(screen.queryByText(/ergibt sich aus/)).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Bereitstellung durch das System'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Zum Prozesselement Formular 1'}));
        expect(props.onSelectNode).toHaveBeenLastCalledWith(1);
    });

    it('loads fresh data when reopened and closes through the title action', async () => {
        const onClose = vi.fn();
        const {unmount} = render(<ProcessDataStructureDialog {...props} onClose={onClose}/>);
        await screen.findByText('1 definierter Pfad');
        fireEvent.click(screen.getByRole('button', {name: 'Schließen'}));
        expect(onClose).toHaveBeenCalledOnce();
        unmount();
        load.mockResolvedValue({
            entries: [],
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        expect(await screen.findByText('Keine Datenpfade in dieser Kategorie ermittelt.')).toBeVisible();
        expect(load).toHaveBeenCalledTimes(2);
    });

    it('selects parent paths without expanding and supports keyboard navigation and expand all', async () => {
        const user = userEvent.setup();
        render(<ProcessDataStructureDialog {...props}/>);
        const parent = await screen.findByRole('treeitem', {name: '$.person'});
        await user.click(within(parent).getByText('person'));
        expect(screen.getByRole('treeitem', {name: '$.person'})).toHaveAttribute('aria-checked', 'true');
        expect(parent).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByRole('heading', {name: '$.person'})).toBeInTheDocument();
        expect(screen.getByText('Enthält einen direkt untergeordneten Pfad.')).toBeVisible();
        expect(screen.getByRole('heading', {name: 'Untergeordnete Pfade'})).toBeVisible();
        expect(screen.queryByRole('button', {name: /^Zum Prozesselement/})).not.toBeInTheDocument();
        await user.keyboard('{ArrowRight}{ArrowDown}{Enter}');
        expect(screen.getByRole('treeitem', {name: '$.person.name'})).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByRole('heading', {name: '$.person.name'})).toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Alle zuklappen'}));
        expect(parent).toHaveAttribute('aria-expanded', 'false');
        await user.click(screen.getByRole('button', {name: 'Alle aufklappen'}));
        expect(parent).toHaveAttribute('aria-expanded', 'true');
    });

    it('counts filtered paths once per definition and restores expansion when clearing search', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            ...data,
            entries: [...data.entries, {
                ...data.entries[0],
                path: 'person.age',
                label: 'Alter',
                description: null,
            }],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        const search = await screen.findByRole('searchbox', {name: 'Vorgangsdaten durchsuchen'});
        expect(screen.getByRole('status')).toHaveTextContent('2 definierte Pfade');
        fireEvent.change(search, {target: {value: 'name'}});
        expect(screen.getByRole('status')).toHaveTextContent('1 von 2 Pfaden angezeigt');
        expect(await screen.findByRole('treeitem', {name: '$.person.name'})).toBeVisible();
        fireEvent.change(search, {target: {value: 'person'}});
        expect(screen.getByRole('status')).toHaveTextContent('2 von 2 Pfaden angezeigt');
        fireEvent.change(search, {target: {value: 'not-found'}});
        expect(screen.getByRole('status')).toHaveTextContent('0 von 2 Pfaden angezeigt');
        expect(await screen.findByText('Keine passenden Pfade gefunden.')).toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Suche löschen'}));
        expect(search).toHaveValue('');
        expect(screen.getByRole('status')).toHaveTextContent('2 definierte Pfade');
        expect(await screen.findByRole('treeitem', {name: '$.person'})).toHaveAttribute('aria-expanded', 'false');
    });

    it.each([false, true])('explains lists and their entries before the child count with own definitions: %s', async (hasOwnDefinitions) => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: data.entries.flatMap(entry => [
                ...(hasOwnDefinitions ? ['people', 'people[*]'].map(path => ({...entry, path})) : []),
                {...entry, path: 'people[*].name'},
            ]),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        fireEvent.change(await screen.findByRole('searchbox'), {target: {value: 'people'}});

        for (const [path, label, hint] of [
            ['people', 'people', listHint],
            ['people[*]', '[*]', listItemHint],
        ]) {
            await user.click(within(await screen.findByRole('treeitem', {name: `$.${path}`})).getByText(label));
            expect(screen.getAllByText(hint)).toHaveLength(1);
            expect(screen.queryByText(hint === listHint ? listItemHint : listHint)).not.toBeInTheDocument();
            expect(screen.getByText(hint).compareDocumentPosition(screen.getByText('Enthält einen direkt untergeordneten Pfad.')))
                .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
            if (hasOwnDefinitions) expect(screen.getAllByRole('button', {name: /^Zum Prozesselement/})).toHaveLength(2);
        }
        await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('$.people[*]');
    });

    it('explains nested lists, wildcard leaves and entries that are themselves lists', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: ['people[*].children[*]', 'matrix[*][*].value'].map(path => ({...data.entries[0], path})),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        const search = await screen.findByRole('searchbox');
        fireEvent.change(search, {target: {value: 'children'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$.people[*].children'})).getByText('children'));
        expect(screen.getAllByText(listHint)).toHaveLength(1);
        expect(screen.queryByText(listItemHint)).not.toBeInTheDocument();

        await user.click(within(screen.getByRole('treeitem', {name: '$.people[*].children[*]'})).getByText('[*]'));
        expect(screen.getAllByText(listItemHint)).toHaveLength(1);
        expect(screen.queryByText(listHint)).not.toBeInTheDocument();
        expect(screen.queryByText(/Enthält .+ direkt untergeordnet/)).not.toBeInTheDocument();

        fireEvent.change(search, {target: {value: 'matrix'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$.matrix[*]'})).getAllByText('[*]')[0]);
        const count = screen.getByText('Enthält einen direkt untergeordneten Pfad.');
        for (const hint of [listItemHint, listHint]) {
            expect(screen.getAllByText(hint)).toHaveLength(1);
            expect(screen.getByText(hint).compareDocumentPosition(count)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        }
    });

    it('does not add wildcard explanations to ordinary objects or concrete list indices', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: ['person.name', 'people[1].name'].map(path => ({...data.entries[0], path})),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        fireEvent.change(await screen.findByRole('searchbox'), {target: {value: 'name'}});
        for (const [path, label] of [['person', 'person'], ['people', 'people'], ['people[1]', '[1]']]) {
            await user.click(within(await screen.findByRole('treeitem', {name: `$.${path}`})).getByText(label));
            expect(screen.queryByText(listHint)).not.toBeInTheDocument();
            expect(screen.queryByText(listItemHint)).not.toBeInTheDocument();
        }
    });

    it.each([false, true])('keeps list hints and mixed-structure warnings stable while searching with mixed structure: %s', async (mixed) => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: ['people[*].name', mixed ? 'people.special' : 'people[1].special']
                .map(path => ({...data.entries[0], path})),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        await user.click(within(await screen.findByRole('treeitem', {name: '$.people'})).getByText('people'));
        const search = screen.getByRole('searchbox');
        for (const query of ['', 'special']) {
            fireEvent.change(search, {target: {value: query}});
            expect(screen.getByText('Enthält zwei direkt untergeordnete Pfade.')).toBeVisible();
            if (mixed) {
                expect(screen.queryByText(listHint)).not.toBeInTheDocument();
                expect(screen.getByRole('alert')).toHaveTextContent('Mögliche Strukturabweichung');
            } else {
                expect(screen.getAllByText(listHint)).toHaveLength(1);
                expect(screen.queryByRole('alert')).not.toBeInTheDocument();
            }
        }
    });

    it('explains list paths across element data, protected process data and element metadata', async () => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: [
                {...data.entries[0], category: ProcessDataStructureCategory.ElementData, path: 'form1.people[*].name'},
                {...data.entries[0], category: ProcessDataStructureCategory.ProtectedProcessData, path: 'people[*].name'},
                {...data.entries[0], category: ProcessDataStructureCategory.ProtectedProcessData, path: 'taskMetadata.form1.people[*].name'},
            ],
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        for (const [tab, path] of [
            ['Elementdaten (_)', '_.form1.people'],
            ['Geschützte Vorgangsdaten ($$)', '$$.people'],
            ['Elementmetadaten ($$.taskMetadata)', '$$.taskMetadata.form1.people'],
        ]) {
            await user.click(screen.getByRole('tab', {name: tab}));
            fireEvent.change(await screen.findByRole('searchbox'), {target: {value: 'name'}});
            await user.click(within(await screen.findByRole('treeitem', {name: path})).getByText('people'));
            expect(screen.getAllByText(listHint)).toHaveLength(1);
            await user.click(within(screen.getByRole('treeitem', {name: `${path}[*]`})).getByText('[*]'));
            expect(screen.getAllByText(listItemHint)).toHaveLength(1);
            await user.click(screen.getByRole('button', {name: 'Pfad kopieren'}));
            expect(copyToClipboardText).toHaveBeenLastCalledWith(`${path}[*]`);
        }
    });

    it.each<[number, string]>([[12, 'zwölf'], [13, '13']])('humanizes %i child paths while keeping the tree count numeric', async (count, expected) => {
        const user = userEvent.setup();
        load.mockResolvedValue({
            entries: Array.from({length: count}, (_, index) => ({...data.entries[0], path: `person.field${index}`})),
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        await user.click(within(await screen.findByRole('treeitem', {name: '$.person'})).getByText('person'));
        expect(screen.getByText(`Enthält ${expected} direkt untergeordnete Pfade.`)).toBeVisible();
        expect(screen.getByRole('status')).toHaveTextContent(`${count} definierte Pfade`);
    });

    it.each<[number, string | null]>([[1, null], [2, 'zwei'], [12, 'zwölf'], [13, '13']])('counts %i distinct origins across repeated definitions', async (count, expected) => {
        const user = userEvent.setup();
        const entries = Array.from({length: count}, (_, index) => ({
            ...data.entries[0],
            origin: {nodeId: index + 1, name: 'Formular', dataKey: `form${index + 1}`},
        }));
        load.mockResolvedValue({
            entries: [...entries, {...entries[0], label: 'Weitere Bezeichnung'}],
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        fireEvent.change(await screen.findByRole('searchbox'), {target: {value: 'name'}});
        await user.click(within(await screen.findByRole('treeitem', {name: '$.person.name'})).getByText('name'));
        if (expected == null) {
            expect(screen.queryByText(/Dieser Pfad ist in/)).not.toBeInTheDocument();
        } else {
            expect(screen.getByText(`Dieser Pfad ist in ${expected} Prozesselementen definiert. Diese können aufeinanderfolgen oder zu unterschiedlichen Prozesspfaden gehören.`)).toBeVisible();
        }
        expect(screen.getAllByRole('button', {name: 'Zum Prozesselement Formular'})).toHaveLength(count + 1);
        expect(screen.getByRole('status')).toHaveTextContent('1 von 1 Pfad angezeigt');
    });

    it('shows partial results with actionable diagnostics', async () => {
        load.mockResolvedValue({
            ...data,
            issues: [{
                category: ProcessDataStructureCategory.ProcessData,
                kind: ProcessDataStructureIssueKind.InvalidPath,
                path: 'bad-key',
                origin: data.entries[0].origin!,
            }],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        expect(await screen.findByRole('alert')).toHaveTextContent('Ungültiger Datenpfad: bad-key');
        fireEvent.click(screen.getByRole('button', {name: 'Formular 1'}));
        expect(props.onSelectNode).toHaveBeenCalledWith(1);
        expect(screen.getByRole('navigation', {name: 'Vorgangsdaten durchsuchen'})).toBeInTheDocument();
    });

    it('offers retry after a failure and distinguishes empty results', async () => {
        load.mockRejectedValueOnce(new Error('failure')).mockResolvedValue({
            entries: [],
            issues: [],
        });
        render(<ProcessDataStructureDialog {...props}/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Erneut versuchen'}));
        expect(await screen.findByText('Keine Datenpfade in dieser Kategorie ermittelt.')).toBeInTheDocument();
        expect(load).toHaveBeenCalledTimes(2);
    });

    it('refreshes after saved changes and ignores stale responses for a previous version', async () => {
        let resolveOld!: (value: ProcessDataStructure) => void;
        load.mockImplementationOnce(() => new Promise(resolve => {
            resolveOld = resolve;
        }));
        const {rerender} = render(<ProcessDataStructureDialog {...props}/>);
        rerender(<ProcessDataStructureDialog {...props} processVersion={4}
                                             savedRevision={2}/>);
        await screen.findByRole('searchbox', {name: /Vorgangsdaten durchsuchen/});
        await act(async () => {
            resolveOld({
                entries: [],
                issues: [],
            });
        });
        expect(screen.queryByText('Keine Datenpfade in dieser Kategorie ermittelt.')).not.toBeInTheDocument();
        rerender(<ProcessDataStructureDialog {...props} processVersion={4}
                                             savedRevision={3}/>);
        await waitFor(() => expect(load).toHaveBeenCalledTimes(3));
    });
});
