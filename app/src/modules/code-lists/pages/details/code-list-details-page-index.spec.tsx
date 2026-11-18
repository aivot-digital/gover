import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeListSourceType} from '../../enums/code-list-source-type';
import {type CodeList, type CodeListSourceColumns} from '../../models/code-list';
import {CodeListDetailsPageIndex} from './code-list-details-page-index';

vi.mock('../../../../hooks/use-change-blocker', () => ({useChangeBlocker: () => ({dialog: null})}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => true}));
vi.mock('../../../../dialogs/select-asset-dialog/select-asset-dialog', () => ({SelectAssetDialog: () => null}));
vi.mock('../../../assets/assets-api-service', () => ({
    AssetsApiService: class {
        async retrieveByKey() { return {filename: 'countries.csv', pathFromRoot: '/countries.csv', storageProviderId: 3}; }
        async retrieveStorageProvider() { return {name: 'Importe'}; }
    },
}));
const dispatch = vi.fn();

afterEach(() => vi.restoreAllMocks());

function renderList(item: CodeList) {
    return render(
        <MemoryRouter>
            <GenericDetailsPageContext.Provider value={{
                item, isExistingItem: true, isEditable: true, isBusy: false,
                setItem: vi.fn(), setIsBusy: vi.fn(), setAdditionalData: vi.fn(), refresh: vi.fn(),
                subscribeEvent: vi.fn(() => vi.fn()),
            }}>
                <CodeListDetailsPageIndex/>
            </GenericDetailsPageContext.Provider>
        </MemoryRouter>,
    );
}

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
