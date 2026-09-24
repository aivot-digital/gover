import {createRef, StrictMode} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MemoryRouter, useLocation, useNavigate} from 'react-router-dom';
import {GenericList} from './generic-list';
import {type GenericListFilter, type ListControlRef, type ListFilterCounts, type ListFilterCountsOptions, type GenericListPropsFetchOptions} from './generic-list-props';

const mocks = vi.hoisted(() => ({api: {}}));
vi.mock('../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../utils/with-async-wrapper', () => ({withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main()}));

const filters: GenericListFilter[] = [
    {value: 'all', label: 'Alle'},
    {value: 'failed', label: 'Fehlerhaft', countColor: 'error'},
];
const columns = [{field: 'name', headerName: 'Name', width: 180}];
const fetchRows = vi.fn(async (_options: GenericListPropsFetchOptions<{id: string; name: string}>) => ({
    content: [{id: '1', name: 'Eintrag'}],
    page: {number: 0, size: 12, totalElements: 1200, totalPages: 100},
}));
type FetchCounts = (options: ListFilterCountsOptions) => Promise<ListFilterCounts>;

function Harness({fetchCounts, controlRef, refreshKey, hideAllCount}: {
    fetchCounts?: FetchCounts;
    controlRef?: React.RefObject<ListControlRef | null>;
    refreshKey?: unknown;
    hideAllCount?: boolean;
}) {
    const navigate = useNavigate();
    const location = useLocation();
    const changeQuery = (values: Record<string, string>) => {
        const query = new URLSearchParams(location.search);
        Object.entries(values).forEach(([key, value]) => query.set(key, value));
        navigate(`?${query}`);
    };
    return (
        <>
            <button onClick={() => changeQuery({page: '2', sort: 'name', order: 'desc'})}>Seite wechseln</button>
            <button onClick={() => changeQuery({departmentId: '20', search: 'Anmeldung'})}>Suche eingrenzen</button>
            <button onClick={() => navigate(-1)}>Zurück</button>
            <GenericList
                fetch={fetchRows}
                fetchFilterCounts={fetchCounts}
                filters={hideAllCount ? filters.map(filter => ({...filter, showCount: filter.value !== 'all'})) : filters}
                defaultFilter="all"
                columnDefinitions={columns}
                getRowIdentifier={(item) => item.id}
                controlRef={controlRef}
                refreshKey={refreshKey}
            />
        </>
    );
}

describe('Generic list tab counts', () => {
    it('does not start count requests canceled by the StrictMode effect cleanup', async () => {
        const counts = vi.fn<FetchCounts>().mockResolvedValue({all: 1, failed: 0});
        render(<StrictMode><MemoryRouter><Harness fetchCounts={counts} /></MemoryRouter></StrictMode>);
        expect(await screen.findByRole('tab', {name: 'Alle 1'})).toBeInTheDocument();
        expect(counts).toHaveBeenCalledTimes(1);
        expect(counts.mock.calls[0][0].signal.aborted).toBe(false);
    });

    it('shows exact localized counts, neutral zero and colored nonzero warnings', async () => {
        const counts = vi.fn<FetchCounts>().mockResolvedValue({all: 1200, failed: 2});
        const controlRef = createRef<ListControlRef>();
        render(<MemoryRouter><Harness fetchCounts={counts} controlRef={controlRef} /></MemoryRouter>);
        expect(await screen.findByRole('tab', {name: 'Alle 1.200'})).toBeInTheDocument();
        const errorChip = within(screen.getByRole('tab', {name: 'Fehlerhaft 2'})).getByText('2').closest('.MuiChip-root');
        expect(errorChip).toHaveStyle({color: 'rgb(211, 47, 47)'});
        counts.mockResolvedValue({all: 1200, failed: 0});
        act(() => controlRef.current?.refresh());
        expect(await screen.findByRole('tab', {name: 'Fehlerhaft 0'})).toBeInTheDocument();
        expect(within(screen.getByRole('tab', {name: 'Fehlerhaft 0'})).getByText('0').closest('.MuiChip-root'))
            .not.toHaveStyle({color: 'rgb(211, 47, 47)'});
    });

    it('refreshes on tab changes and explicit refresh, independently of search, filters, pagination and sorting', async () => {
        const counts = vi.fn<FetchCounts>().mockResolvedValue({all: 2400, failed: 2});
        const controlRef = createRef<ListControlRef>();
        render(<MemoryRouter><Harness fetchCounts={counts} controlRef={controlRef} /></MemoryRouter>);
        await screen.findByRole('tab', {name: 'Alle 2.400'});
        fireEvent.click(screen.getByRole('button', {name: 'Suche eingrenzen'}));
        await waitFor(() => expect(fetchRows).toHaveBeenLastCalledWith(expect.objectContaining({search: 'Anmeldung'})));
        expect(counts).toHaveBeenCalledTimes(1);
        fireEvent.click(screen.getByRole('button', {name: 'Seite wechseln'}));
        await waitFor(() => expect(fetchRows).toHaveBeenLastCalledWith(expect.objectContaining({page: 1, sort: 'name', order: 'DESC'})));
        expect(counts).toHaveBeenCalledTimes(1);
        fireEvent.click(screen.getByRole('tab', {name: 'Fehlerhaft 2'}));
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(2));
        expect(counts).toHaveBeenLastCalledWith({signal: expect.any(AbortSignal)});
        fireEvent.click(screen.getByRole('button', {name: 'Zurück'}));
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(3));
        act(() => controlRef.current?.refresh());
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(4));
        // The selected tab keeps the overview count, rather than adopting the table's filtered total of 1,200.
        expect(screen.getByRole('tab', {name: 'Alle 2.400'})).toHaveAttribute('aria-selected', 'true');
    });

    it('keeps existing numbers subdued while refreshing and omits counts on unconfigured tabs', async () => {
        let resolveRefresh!: (counts: ListFilterCounts) => void;
        const counts = vi.fn<FetchCounts>().mockResolvedValueOnce({failed: 2})
            .mockImplementationOnce(() => new Promise(resolve => {resolveRefresh = resolve;}));
        const controlRef = createRef<ListControlRef>();
        render(<MemoryRouter><Harness fetchCounts={counts} controlRef={controlRef} hideAllCount /></MemoryRouter>);
        expect(await screen.findByRole('tab', {name: 'Fehlerhaft 2'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Alle'})).toBeInTheDocument();
        expect(screen.queryByLabelText('Anzahl nicht verfügbar')).not.toBeInTheDocument();
        act(() => controlRef.current?.refresh());
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(2));
        const chip = within(screen.getByRole('tab', {name: 'Fehlerhaft 2'})).getByText('2').closest('.MuiChip-root');
        expect(chip).toHaveStyle({opacity: '0.5'});
        await act(async () => resolveRefresh({failed: 3}));
        expect(screen.getByRole('tab', {name: 'Fehlerhaft 3'})).toBeInTheDocument();
    });

    it('cancels obsolete requests and does not display their late responses', async () => {
        let resolveOld!: (counts: ListFilterCounts) => void;
        const counts = vi.fn<FetchCounts>()
            .mockImplementationOnce(() => new Promise(resolve => {resolveOld = resolve;}))
            .mockResolvedValue({all: 5, failed: 0});
        const rendered = render(<MemoryRouter><Harness fetchCounts={counts} /></MemoryRouter>);
        expect(screen.getByRole('tab', {name: 'Alle'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Fehlerhaft'})).toBeInTheDocument();
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(1));
        fireEvent.click(screen.getByRole('tab', {name: 'Fehlerhaft'}));
        await screen.findByRole('tab', {name: 'Alle 5'});
        expect(counts.mock.calls[0][0].signal.aborted).toBe(true);
        await act(async () => resolveOld({all: 999, failed: 999}));
        expect(screen.getByRole('tab', {name: 'Alle 5'})).toBeInTheDocument();
        rendered.unmount();
        expect(counts.mock.calls[1][0].signal.aborted).toBe(true);
    });

    it('keeps rows usable on count failure and recovers on refresh without showing a false zero', async () => {
        const counts = vi.fn<FetchCounts>().mockRejectedValueOnce(new Error('Unavailable'))
            .mockResolvedValue({all: 1, failed: 0});
        const controlRef = createRef<ListControlRef>();
        render(<MemoryRouter><Harness fetchCounts={counts} controlRef={controlRef} /></MemoryRouter>);
        expect(await screen.findAllByLabelText('Anzahl nicht verfügbar')).toHaveLength(2);
        expect(await screen.findByRole('gridcell', {name: 'Eintrag'})).toBeInTheDocument();
        expect(screen.queryByRole('tab', {name: 'Fehlerhaft 0'})).not.toBeInTheDocument();
        act(() => controlRef.current?.refresh());
        expect(await screen.findByRole('tab', {name: 'Fehlerhaft 0'})).toBeInTheDocument();
    });

    it('refreshes when the access context changes and leaves unconfigured lists unchanged', async () => {
        let resolveNewScope!: (counts: ListFilterCounts) => void;
        const counts = vi.fn<FetchCounts>().mockResolvedValueOnce({all: 1, failed: 0})
            .mockImplementationOnce(() => new Promise(resolve => {resolveNewScope = resolve;}));
        const rendered = render(<MemoryRouter><Harness fetchCounts={counts} refreshKey="one" /></MemoryRouter>);
        await screen.findByRole('tab', {name: 'Alle 1'});
        rendered.rerender(<MemoryRouter><Harness fetchCounts={counts} refreshKey="two" /></MemoryRouter>);
        await waitFor(() => expect(counts).toHaveBeenCalledTimes(2));
        expect(counts.mock.calls[0][0].signal.aborted).toBe(true);
        expect(screen.getByRole('tab', {name: 'Alle'})).toBeInTheDocument();
        expect(screen.queryByRole('tab', {name: 'Alle 1'})).not.toBeInTheDocument();
        await act(async () => resolveNewScope({all: 2, failed: 0}));
        expect(screen.getByRole('tab', {name: 'Alle 2'})).toBeInTheDocument();
        rendered.rerender(<MemoryRouter><Harness /></MemoryRouter>);
        expect(screen.getByRole('tab', {name: 'Alle'})).toBeInTheDocument();
        expect(screen.queryByLabelText('Anzahl wird geladen')).not.toBeInTheDocument();
    });
});
