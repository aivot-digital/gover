import {useCallback} from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MemoryRouter, useLocation, useNavigate} from 'react-router-dom';
import {GenericList} from './generic-list';
import {useListFilter} from './use-list-filter';
import {type GenericListPropsFetchOptions, type ListControlRef} from './generic-list-props';
import {createRef} from 'react';

const mocks = vi.hoisted(() => ({api: {}}));
vi.mock('../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../utils/with-async-wrapper', () => ({withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main()}));

const columns = [{field: 'name', headerName: 'Name', width: 200}];
const filters = [{value: 'all', label: 'Alle'}, {value: 'published', label: 'Veröffentlicht'}];
const page = {content: [{id: 'one', name: 'Eintrag'}], page: {number: 0, size: 12, totalElements: 120, totalPages: 10}};
type FetchRows = (options: GenericListPropsFetchOptions<{id: string; name: string}> & {departmentId?: string; modules: string[]}) => Promise<typeof page>;

function Harness({fetch, controlRef}: {fetch: FetchRows; controlRef?: React.RefObject<ListControlRef | null>}) {
    const department = useListFilter('departmentId');
    const modules = useListFilter('modules');
    const navigate = useNavigate();
    const location = useLocation();
    const fetchRows = useCallback((options: GenericListPropsFetchOptions<{id: string; name: string}>) =>
        fetch({...options, departmentId: department.value, modules: modules.values}), [fetch, department.value, modules.values]);
    return <>
        <output aria-label="URL">{location.search}</output>
        <button onClick={() => navigate(-1)}>Zurück</button>
        <button onClick={() => navigate(1)}>Vorwärts</button>
        <button onClick={() => modules.setValue(['A,B', 'C'])}>Module setzen</button>
        <button onClick={() => modules.setValue([])}>Module entfernen</button>
        <GenericList
            controlRef={controlRef}
            fetch={fetchRows}
            columnDefinitions={columns}
            getRowIdentifier={row => row.id}
            filters={filters}
            defaultFilter="all"
            defaultSortField="name"
            searchLabel="Suchen"
            noDataPlaceholder="Noch keine Einträge"
            noSearchResultsPlaceholder="Keine passenden Einträge"
            hasActiveAdditionalFilters={department.value != null || modules.values.length > 0}
            preSearchElements={[
                <select key="department" aria-label="Organisationseinheit" value={department.value ?? ''}
                    onChange={event => department.setValue(event.target.value)}>
                    <option value="">Alle Organisationseinheiten</option>
                    <option value="10">Alpha</option>
                    <option value="20">Beta</option>
                </select>,
                <button key="action" onClick={() => {}}>Hinzufügen</button>,
            ]}
        />
    </>;
}

function renderList(fetch: FetchRows, url = '/', controlRef?: React.RefObject<ListControlRef | null>) {
    return render(<MemoryRouter initialEntries={[url]}><Harness fetch={fetch} controlRef={controlRef}/></MemoryRouter>);
}

describe('GenericList additional filters', () => {
    it('loads URL filters and pagination together and restores them on back/forward navigation', async () => {
        const fetch = vi.fn().mockResolvedValue(page);
        renderList(fetch, '/?page=5&size=12&filter=published&search=hello&departmentId=10&sort=name&order=desc');
        await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.objectContaining({page: 4, departmentId: '10', filter: 'published', search: 'hello', order: 'DESC'})));
        expect(fetch).toHaveBeenCalledTimes(1);
        fireEvent.change(screen.getByRole('combobox', {name: 'Organisationseinheit'}), {target: {value: '20'}});
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, departmentId: '20', filter: 'published', search: 'hello', order: 'DESC'})));
        expect(fetch.mock.calls.filter(([request]) => request.departmentId === '20').every(([request]) => request.page === 0)).toBe(true);
        fireEvent.click(screen.getByText('Zurück'));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 4, departmentId: '10'})));
        expect(screen.getByRole('combobox', {name: 'Organisationseinheit'})).toHaveValue('10');
        fireEvent.click(screen.getByText('Vorwärts'));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, departmentId: '20'})));
    });

    it('encodes multiple filter values separately and preserves unrelated parameters when clearing', async () => {
        const fetch = vi.fn().mockResolvedValue(page);
        renderList(fetch, '/?page=3&departmentId=10&filter=published');
        fireEvent.click(screen.getByText('Module setzen'));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, modules: ['A,B', 'C'], departmentId: '10'})));
        const params = new URLSearchParams(screen.getByLabelText('URL').textContent!);
        expect(params.getAll('modules')).toEqual(['A,B', 'C']);
        fireEvent.click(screen.getByText('Module entfernen'));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, modules: [], departmentId: '10', filter: 'published'})));
        expect(new URLSearchParams(screen.getByLabelText('URL').textContent!).has('modules')).toBe(false);
    });

    it.each(['/?departmentId=10', '/?filter=published', '/?search=abc'])('uses the filtered empty state for %s', async url => {
        const fetch = vi.fn().mockResolvedValue({content: [], page: {number: 0, size: 12, totalElements: 0, totalPages: 0}});
        renderList(fetch, url);
        expect(await screen.findByText('Keine passenden Einträge')).toBeInTheDocument();
        expect(screen.queryByText('Noch keine Einträge')).not.toBeInTheDocument();
    });

    it('does not consider a pre-search action to be an active filter', async () => {
        const fetch = vi.fn().mockResolvedValue({content: [], page: {number: 0, size: 12, totalElements: 0, totalPages: 0}});
        renderList(fetch);
        expect(await screen.findByText('Noch keine Einträge')).toBeInTheDocument();
        expect(screen.getByText('Hinzufügen')).toBeEnabled();
    });

    it('retains additional filters when switching tabs and refreshing', async () => {
        const fetch = vi.fn().mockResolvedValue(page);
        const controlRef = createRef<ListControlRef>();
        renderList(fetch, '/?page=3&size=24&departmentId=10', controlRef);
        await waitFor(() => expect(fetch).toHaveBeenCalled());
        fireEvent.click(screen.getByRole('tab', {name: 'Veröffentlicht'}));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, size: 24, departmentId: '10', filter: 'published'})));
        controlRef.current!.refresh();
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, size: 24, departmentId: '10', filter: 'published'})));
    });
});
