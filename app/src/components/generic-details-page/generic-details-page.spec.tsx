import {createRef, StrictMode, useEffect, useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {createMemoryRouter, RouterProvider, useRouteError} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {GenericDetailsPage} from './generic-details-page';
import {useGenericDetailsPageContext} from './generic-details-page-context';
import {type GenericDetailsPageControlRef, type GenericDetailsPageProps} from './generic-details-page-props';
import {useGenericDetailsPageEvent} from './use-generic-details-page-event';

const mocks = vi.hoisted(() => ({api: {}}));
vi.mock('../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../hooks/use-app-selector', () => ({useAppSelector: () => undefined}));
vi.mock('../../modules/search/hooks/use-record-recent-search-item', () => ({useRecordRecentSearchItem: () => {}}));
vi.mock('../generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));

interface Item {
    id: string;
    name: string;
}
interface AdditionalData {
    detail: string;
}
interface Events {
    changed: {value: number};
}
type PageProps = GenericDetailsPageProps<Item, string, AdditionalData, Events>;

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (error: unknown) => void;
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return {promise, resolve, reject};
}

function RouteError() {
    const error = useRouteError() as {status: number};
    return <p role="alert">Failure {error.status}</p>;
}

function renderPage({strict = false}: {strict?: boolean} = {}) {
    const controlRef = createRef<GenericDetailsPageControlRef<Events>>();
    const fetchData = vi.fn<PageProps['fetchData']>().mockImplementation(async (_, id) => ({id, name: 'Initial'}));
    const fetchDetail = vi.fn().mockResolvedValue('Initial detail');
    const onRefresh = vi.fn();
    const onChanged = vi.fn();
    const onOtherChanged = vi.fn();
    const onMount = vi.fn();
    const onUnmount = vi.fn();
    const onRender = vi.fn();

    function Content({other = false}: {other?: boolean}) {
        const {item, additionalData, isBusy, refresh, setItem, setAdditionalData, setIsBusy} =
            useGenericDetailsPageContext<Item, AdditionalData, Events>();
        const [draft, setDraft] = useState('');
        useGenericDetailsPageEvent('refresh', () => {
            onRefresh({item, additionalData, isBusy});
        });
        useGenericDetailsPageEvent<Events, 'changed'>('changed', payload => {
            (other ? onOtherChanged : onChanged)(payload);
        });
        useEffect(() => {
            onMount();
            return () => {onUnmount();};
        }, []);
        onRender();

        return (
            <>
                <output aria-label="Item">{item?.id}:{item?.name}</output>
                <output aria-label="Detail">{additionalData?.detail}</output>
                <output aria-label="Busy">{String(isBusy)}</output>
                <label>Draft<input value={draft} onChange={event => setDraft(event.target.value)} /></label>
                <button onClick={refresh}>Refresh from tab</button>
                <button onClick={() => setItem(current => ({...current, name: 'Edited'}))}>Edit item</button>
                <button onClick={() => setAdditionalData({detail: 'Edited detail'})}>Edit detail</button>
                <button onClick={() => setIsBusy(!isBusy)}>Toggle busy</button>
            </>
        );
    }
    function OtherContent() {
        return <Content other />;
    }

    const router = createMemoryRouter([{
        path: '/details/:id',
        element: <GenericDetailsPage<Item, string, AdditionalData, Events>
            controlRef={controlRef}
            header={{title: 'Details', icon: <span />}}
            initializeItem={() => ({id: '', name: ''})}
            fetchData={fetchData}
            fetchAdditionalData={{detail: fetchDetail}}
            getTabTitle={item => item.name}
            tabs={[{path: '/details/:id', label: 'Details'}]}
        />,
        errorElement: <RouteError />,
        children: [
            {index: true, element: <Content />},
            {path: 'other', element: <OtherContent />},
        ],
    }], {initialEntries: ['/details/17']});
    const tree = <RouterProvider router={router} />;
    const rendered = render(strict ? <StrictMode>{tree}</StrictMode> : tree);
    return {
        ...rendered, router, controlRef, fetchData, fetchDetail, onRefresh,
        onChanged, onOtherChanged, onMount, onUnmount, onRender,
    };
}

async function waitForInitialLoad(page: ReturnType<typeof renderPage>) {
    await waitFor(() => expect(page.getByLabelText('Busy')).toHaveTextContent('false'));
    expect(page.getByLabelText('Item')).toHaveTextContent('17:Initial');
    expect(page.getByLabelText('Detail')).toHaveTextContent('Initial detail');
}

describe('Generic details page events', () => {
    it.each(['control', 'context'] as const)(
        'notifies after committing refreshed data through %s while preserving the tab and its state',
        async source => {
            const page = renderPage();
            await waitForInitialLoad(page);
            expect(page.onRefresh).not.toHaveBeenCalled();
            const input = page.getByRole('textbox', {name: 'Draft'});
            fireEvent.change(input, {target: {value: 'Unsaved draft'}});
            const refreshedItem = deferred<Item>();
            const refreshedDetail = deferred<string>();
            page.fetchData.mockReturnValueOnce(refreshedItem.promise);
            page.fetchDetail.mockReturnValueOnce(refreshedDetail.promise);

            act(() => {
                if (source === 'control') {
                    page.controlRef.current!.refresh();
                } else {
                    fireEvent.click(page.getByRole('button', {name: 'Refresh from tab'}));
                }
            });
            expect(page.getByLabelText('Busy')).toHaveTextContent('true');
            expect(page.getByRole('textbox', {name: 'Draft'})).toBe(input);
            expect(input).toHaveValue('Unsaved draft');
            expect(page.onUnmount).not.toHaveBeenCalled();

            await act(async () => refreshedItem.resolve({id: '17', name: 'Refreshed'}));
            expect(page.onRefresh).not.toHaveBeenCalled();
            await act(async () => refreshedDetail.resolve('Refreshed detail'));
            expect(page.onRefresh.mock.calls).toEqual([[{
                item: {id: '17', name: 'Refreshed'},
                additionalData: {detail: 'Refreshed detail'},
                isBusy: false,
            }]]);
            expect(page.getByRole('textbox', {name: 'Draft'})).toBe(input);
            expect(input).toHaveValue('Unsaved draft');
            expect(page.onMount).toHaveBeenCalledTimes(1);
            expect(page.onUnmount).not.toHaveBeenCalled();
        },
    );

    it('delivers custom events without fetching, rendering or remounting the tab', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        const renderCount = page.onRender.mock.calls.length;
        act(() => {
            page.controlRef.current!.emitEvent('changed', {value: 1});
            page.controlRef.current!.emitEvent('changed', {value: 1});
        });
        expect(page.onChanged.mock.calls).toEqual([[{value: 1}], [{value: 1}]]);
        expect(page.onRender).toHaveBeenCalledTimes(renderCount);
        expect(page.fetchData).toHaveBeenCalledTimes(1);
        expect(page.fetchDetail).toHaveBeenCalledTimes(1);
        expect(page.onMount).toHaveBeenCalledTimes(1);
        expect(page.onRefresh).not.toHaveBeenCalled();
    });

    it('unsubscribes the previous tab and only delivers to the current tab, without replay', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        act(() => page.controlRef.current!.emitEvent('changed', {value: 1}));
        await act(async () => page.router.navigate('/details/17/other'));
        expect(page.onOtherChanged).not.toHaveBeenCalled();
        act(() => page.controlRef.current!.emitEvent('changed', {value: 2}));
        expect(page.onChanged.mock.calls).toEqual([[{value: 1}]]);
        expect(page.onOtherChanged.mock.calls).toEqual([[{value: 2}]]);
        expect(page.fetchData).toHaveBeenCalledTimes(1);
        page.unmount();
        expect(page.controlRef.current).toBeNull();
    });

    it('notifies the tab open when a refresh finishes, after switching tabs during loading', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        const pending = deferred<Item>();
        page.fetchData.mockReturnValueOnce(pending.promise);
        act(() => page.controlRef.current!.refresh());
        await act(async () => page.router.navigate('/details/17/other'));
        await act(async () => pending.resolve({id: '17', name: 'Refreshed'}));
        expect(page.onRefresh).toHaveBeenCalledTimes(1);
        expect(page.onRefresh).toHaveBeenCalledWith(expect.objectContaining({item: {id: '17', name: 'Refreshed'}}));
        expect(page.onUnmount).toHaveBeenCalledTimes(1);
        expect(page.onMount).toHaveBeenCalledTimes(2);
    });

    it('isolates events between concurrent details page instances', async () => {
        const first = renderPage();
        await waitForInitialLoad(first);
        const second = renderPage();
        await waitFor(() => expect(second.fetchDetail).toHaveBeenCalledTimes(1));
        act(() => first.controlRef.current!.emitEvent('changed', {value: 1}));
        expect(first.onChanged).toHaveBeenCalledTimes(1);
        expect(second.onChanged).not.toHaveBeenCalled();
        act(() => second.controlRef.current!.emitEvent('changed', {value: 2}));
        expect(second.onChanged).toHaveBeenCalledWith({value: 2});
        expect(first.onChanged).toHaveBeenCalledTimes(1);
    });

    it('does not emit refresh for direct setters, ID changes or returning to a previous ID', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        fireEvent.click(page.getByRole('button', {name: 'Edit item'}));
        fireEvent.click(page.getByRole('button', {name: 'Edit detail'}));
        fireEvent.click(page.getByRole('button', {name: 'Toggle busy'}));
        fireEvent.click(page.getByRole('button', {name: 'Toggle busy'}));
        expect(page.onRefresh).not.toHaveBeenCalled();
        act(() => page.controlRef.current!.refresh());
        await waitFor(() => expect(page.onRefresh).toHaveBeenCalledTimes(1));

        await act(async () => page.router.navigate('/details/18'));
        expect(page.getByLabelText('Item')).toHaveTextContent('18:Initial');
        await act(async () => page.router.navigate('/details/17'));
        expect(page.getByLabelText('Item')).toHaveTextContent('17:Initial');
        expect(page.onRefresh).toHaveBeenCalledTimes(1);
    });

    it('only emits for the successful current refresh and ignores superseded responses', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        const old = deferred<Item>();
        const current = deferred<Item>();
        page.fetchData.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
        act(() => page.controlRef.current!.refresh());
        act(() => page.controlRef.current!.refresh());

        await act(async () => current.resolve({id: '17', name: 'Current'}));
        expect(page.onRefresh).toHaveBeenCalledTimes(1);
        await act(async () => old.resolve({id: '17', name: 'Obsolete'}));
        expect(page.onRefresh).toHaveBeenCalledTimes(1);
        expect(page.getByLabelText('Item')).toHaveTextContent('17:Current');

        act(() => {
            page.controlRef.current!.refresh();
            page.controlRef.current!.refresh();
        });
        await waitFor(() => expect(page.onRefresh).toHaveBeenCalledTimes(2));
        expect(page.fetchData).toHaveBeenCalledTimes(4);
    });

    it('discards a pending refresh when the resource changes, including when returning to it', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        const old = deferred<Item>();
        page.fetchData.mockReturnValueOnce(old.promise);
        act(() => page.controlRef.current!.refresh());
        await act(async () => page.router.navigate('/details/18'));
        await act(async () => page.router.navigate('/details/17'));
        await act(async () => old.resolve({id: '17', name: 'Obsolete'}));
        expect(page.getByLabelText('Item')).toHaveTextContent('17:Initial');
        expect(page.onRefresh).not.toHaveBeenCalled();
    });

    it('does not emit refresh when the API changes after an explicit refresh', async () => {
        const page = renderPage();
        await waitForInitialLoad(page);
        act(() => page.controlRef.current!.refresh());
        await waitFor(() => expect(page.onRefresh).toHaveBeenCalledTimes(1));

        mocks.api = {};
        await act(async () => page.router.navigate('/details/17?authentication=changed'));
        expect(page.fetchData).toHaveBeenCalledTimes(3);
        expect(page.onRefresh).toHaveBeenCalledTimes(1);
    });

    it.each(['item', 'additional data', 'not found'] as const)('does not notify on failed %s loading', async failure => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        const page = renderPage();
        await waitForInitialLoad(page);
        if (failure === 'additional data') {
            page.fetchDetail.mockRejectedValueOnce(new Error('Detail failure'));
        } else if (failure === 'not found') {
            page.fetchData.mockRejectedValueOnce({status: 404, message: 'Missing', displayableToUser: false});
        } else {
            page.fetchData.mockRejectedValueOnce(new Error('Item failure'));
        }
        act(() => page.controlRef.current!.refresh());
        if (failure === 'not found') {
            expect(await screen.findByText('Diese Ressource konnte leider nicht (mehr) gefunden werden')).toBeInTheDocument();
        } else {
            expect(await screen.findByRole('alert')).toHaveTextContent('Failure 500');
        }
        expect(page.onRefresh).not.toHaveBeenCalled();
    });

    it('keeps the loaded page usable when a refresh listener fails', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        const page = renderPage();
        await waitForInitialLoad(page);
        page.onRefresh.mockImplementation(() => {throw new Error('Listener failure');});
        page.fetchData.mockResolvedValueOnce({id: '17', name: 'Refreshed'});
        act(() => page.controlRef.current!.refresh());
        await waitFor(() => expect(page.onRefresh).toHaveBeenCalledTimes(1));
        expect(page.getByLabelText('Item')).toHaveTextContent('17:Refreshed');
        expect(page.getByLabelText('Busy')).toHaveTextContent('false');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('delivers one refresh notification in StrictMode', async () => {
        const page = renderPage({strict: true});
        await waitForInitialLoad(page);
        expect(page.onRefresh).not.toHaveBeenCalled();
        act(() => page.controlRef.current!.refresh());
        await waitFor(() => expect(page.onRefresh).toHaveBeenCalledTimes(1));
    });
});
