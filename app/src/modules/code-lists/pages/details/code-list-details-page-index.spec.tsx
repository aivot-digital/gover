import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeListSourceType} from '../../enums/code-list-source-type';
import {type CodeList, type CodeListSourceColumns} from '../../models/code-list';
import {CodeListDetailsPageIndex} from './code-list-details-page-index';

vi.mock('../../../../hooks/use-change-blocker', () => ({useChangeBlocker: () => ({dialog: null})}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => true}));
vi.mock('../../../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: ({show, onSelect}: {show: boolean; onSelect: (key: string) => void}) => show
        ? <button onClick={() => onSelect('00000000-0000-4000-8000-000000000003')}>countries.csv</button>
        : null,
}));
vi.mock('../../../assets/assets-api-service', () => ({
    AssetsApiService: class {
        async retrieveByKey() { return {filename: 'countries.csv', pathFromRoot: '/countries.csv', storageProviderId: 3}; }
        async retrieveStorageProvider() { return {name: 'Importe'}; }
    },
}));
const dispatch = vi.fn();

afterEach(() => vi.restoreAllMocks());

function renderList(initialItem: CodeList, isNew = false) {
    function DetailsPage() {
        const [item, setItem] = useState(initialItem);
        const {pathname} = useLocation();
        const isNewItem = pathname === '/code-lists/new';
        return (
            <GenericDetailsPageContext.Provider value={{
                item, isNewItem, isExistingItem: !isNewItem, isEditable: true, isBusy: false,
                setItem, setIsBusy: vi.fn(), setAdditionalData: vi.fn(), refresh: vi.fn(),
                subscribeEvent: vi.fn(() => vi.fn()),
            }}>
                <CodeListDetailsPageIndex/>
            </GenericDetailsPageContext.Provider>
        );
    }
    return render(
        <MemoryRouter initialEntries={[`/code-lists/${isNew ? 'new' : initialItem.key}`]}>
            <DetailsPage/>
        </MemoryRouter>,
    );
}

async function renameColumn(key: string, nextKey: string) {
    const user = userEvent.setup();
    const grid = within(screen.getByRole('grid', {name: /Spalten/}));
    await user.dblClick(grid.getByRole('gridcell', {name: key}));
    const input = grid.getByRole('textbox');
    await user.clear(input);
    await user.type(input, `${nextKey}{Enter}`);
    await waitFor(() => expect(grid.getByRole('gridcell', {name: nextKey})).toBeInTheDocument());
}

describe('warnings about saved column keys', () => {
    const referenceWarning = /Verweise auf .* werden nicht angepasst/;
    const initialItem = {
        ...new CodeListsApiService().initialize(),
        key: 'countries', name: 'Länder',
        columns: ['code', 'label'],
        columnMappings: [
            {key: 'code', name: 'Ländercode', index: 1},
            {key: 'label', name: 'Land', index: 2},
        ],
        keyColumnKey: 'code',
    };

    it('starts warning about a renamed draft key only after the list has first been saved', async () => {
        const create = vi.spyOn(CodeListsApiService.prototype, 'create')
            .mockImplementation(async (item) => ({...item, id: 7}));
        renderList(initialItem, true);
        await renameColumn('code', 'iso');
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(create).toHaveBeenCalledOnce());
        await screen.findByRole('button', {name: 'Löschen'});
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();

        await renameColumn('iso', 'country');
        expect(screen.getByText(referenceWarning)).toHaveTextContent('Verweise auf „iso“ werden nicht angepasst.');
        fireEvent.click(screen.getByRole('button', {name: 'Zurücksetzen'}));
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();
    });

    it('does not warn when deleting a column while creating a list', async () => {
        renderList(initialItem, true);
        const row = within(screen.getByRole('grid')).getAllByRole('row')[2];
        fireEvent.click(within(row).getByRole('checkbox'));
        fireEvent.click(screen.getByRole('button', {name: 'Ausgewählte Einträge löschen'}));
        fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', {name: 'Löschen'}));

        expect(screen.queryByRole('gridcell', {name: 'label'})).not.toBeInTheDocument();
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();
    });

    it('does not warn when switching a new manual list to CSV and loading its columns', async () => {
        const metadata: CodeListSourceColumns = {
            columns: ['iso', 'country'],
            columnMappings: [
                {key: 'iso', name: 'Ländercode', index: 1},
                {key: 'country', name: 'Land', index: 2},
            ],
            keyColumnKey: 'iso',
        };
        let resolveMetadata!: (metadata: CodeListSourceColumns) => void;
        const getColumns = vi.spyOn(CodeListsApiService.prototype, 'getAssetColumns')
            .mockReturnValue(new Promise(resolve => { resolveMetadata = resolve; }));
        const create = vi.spyOn(CodeListsApiService.prototype, 'create')
            .mockImplementation(async (item) => ({...item, id: 7}));
        renderList(initialItem, true);
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Quelle der Einträge'}));
        fireEvent.click(await screen.findByRole('option', {name: 'CSV-Datei'}));
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: /CSV-Datei Keine Datei ausgewählt/}));
        fireEvent.click(screen.getByRole('button', {name: 'countries.csv'}));
        expect(getColumns).toHaveBeenCalledExactlyOnceWith('00000000-0000-4000-8000-000000000003');
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();
        await act(async () => resolveMetadata(metadata));
        expect(screen.getByRole('gridcell', {name: 'iso'})).toBeInTheDocument();
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(create).toHaveBeenCalledOnce());
        await screen.findByRole('button', {name: 'Löschen'});
        expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument();
    });

    it('uses the updated saved keys as the baseline after saving an existing list', async () => {
        const update = vi.spyOn(CodeListsApiService.prototype, 'update')
            .mockImplementation(async (_key, item) => item);
        renderList({...initialItem, id: 7});
        await renameColumn('code', 'iso');
        expect(screen.getByText(referenceWarning)).toHaveTextContent('Verweise auf „code“ werden nicht angepasst.');

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalledOnce());
        await waitFor(() => expect(screen.queryByText(referenceWarning)).not.toBeInTheDocument());

        await renameColumn('iso', 'country');
        expect(screen.getByText(referenceWarning)).toHaveTextContent('Verweise auf „iso“ werden nicht angepasst.');
    });
});

it('preserves the label-first order when editing an existing manual code list', async () => {
    const item = {
        ...new CodeListsApiService().initialize(),
        id: 7, key: 'countries', name: 'Länder',
        columns: ['Beschriftung', 'Wert'],
        columnMappings: [
            {key: 'Beschriftung', name: 'Beschriftung', index: 1},
            {key: 'Wert', name: 'Wert', index: 2},
        ],
        keyColumnKey: 'Wert',
    };
    const update = vi.spyOn(CodeListsApiService.prototype, 'update').mockResolvedValue(item);
    renderList(item);
    fireEvent.change(screen.getByRole('textbox', {name: /Beschreibung/}), {target: {value: 'Länderauswahl'}});
    fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

    await waitFor(() => expect(update).toHaveBeenCalledOnce());
    expect(update.mock.calls[0][1]).toMatchObject({
        description: 'Länderauswahl',
        columns: item.columns,
        columnMappings: item.columnMappings,
        keyColumnKey: 'Wert',
    });
});

describe('refreshing code list source metadata', () => {
    it('retains the latest draft, its subset, and the original version when submitting a reordered source preview', async () => {
        const item = {
            ...new CodeListsApiService().initialize(),
            name: 'Länder', key: 'countries', keyColumnKey: 'iso',
            sourceType: CodeListSourceType.XRepository, sourceRef: 'urn:countries',
            columns: ['label', 'code'],
            columnMappings: [
                {key: 'iso', name: 'Ländercode', index: 2},
                {key: 'translated', name: 'Land', index: 1},
            ],
            lockVersion: 8,
        };
        let resolveMetadata!: (metadata: CodeListSourceColumns) => void;
        vi.spyOn(CodeListsApiService.prototype, 'getXRepositoryColumns')
            .mockReturnValue(new Promise(resolve => { resolveMetadata = resolve; }));
        const update = vi.spyOn(CodeListsApiService.prototype, 'update').mockResolvedValue(item);
        renderList(item);
        fireEvent.click(screen.getByRole('button', {name: 'Metadaten abrufen'}));
        fireEvent.change(screen.getByRole('textbox', {name: /Beschreibung/}), {target: {value: 'Während des Abrufs bearbeitet'}});
        await act(async () => resolveMetadata({
            columns: ['extra', 'newLabel', 'code'],
            columnMappings: [
                {key: 'extra', name: 'Zusatz', index: 1},
                {key: 'newLabel', name: 'Neu', index: 2},
                {key: 'code', name: 'Code', index: 3},
            ],
            keyColumnKey: 'code',
        }));

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalledExactlyOnceWith('countries', expect.objectContaining({
            lockVersion: 8, description: 'Während des Abrufs bearbeitet',
            columns: ['extra', 'newLabel', 'code'],
            columnMappings: [
                {key: 'iso', name: 'Ländercode', index: 3, formerSourceColumn: null},
                {key: 'translated', name: 'Land', index: null, formerSourceColumn: 'label'},
            ],
        }), {query: {refreshSource: true}}));
    });

    it('requires repairing a missing key before saving assignments against the current CSV header', async () => {
        const item = {
            ...new CodeListsApiService().initialize(),
            name: 'Länder', key: 'countries', keyColumnKey: 'iso',
            sourceType: CodeListSourceType.Asset, sourceRef: '00000000-0000-4000-8000-000000000003',
            columns: ['code', 'label'],
            columnMappings: [{key: 'iso', name: 'Ländercode', index: 1}],
            lockVersion: 9,
        };
        vi.spyOn(CodeListsApiService.prototype, 'getAssetColumns').mockResolvedValue({
            columns: ['label', 'countryId'], columnMappings: [], keyColumnKey: null,
        });
        const update = vi.spyOn(CodeListsApiService.prototype, 'update').mockResolvedValue(item);
        renderList(item);
        fireEvent.click(screen.getByRole('button', {name: 'Metadaten abrufen'}));
        await screen.findByText('„code“ fehlt in der Quelle');
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(update).not.toHaveBeenCalled();
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Quellspalte für Ländercode'}));
        fireEvent.click(await screen.findByRole('option', {name: '2 – countryId'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalledExactlyOnceWith('countries', expect.objectContaining({
            lockVersion: 9, keyColumnKey: 'iso', columns: ['label', 'countryId'],
            columnMappings: [{key: 'iso', name: 'Ländercode', index: 2, formerSourceColumn: null}],
        }), {query: {refreshSource: true}}));
    });
});
