import {configureStore} from '@reduxjs/toolkit';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {Provider} from 'react-redux';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../../components/generic-details-page/generic-details-page-context';
import {ConfirmProvider} from '../../../../../providers/confirm-provider';
import {shellReducer} from '../../../../../slices/shell-slice';
import {VUserDeputyWithDetailsApiService} from '../../../services/v-user-deputy-with-details-api-service';
import {UsersApiService} from '../../../users-api-service';
import {UserDetailsPageDeputies} from './user-details-page-deputies';

vi.mock('../../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
    useRefreshPermissionSet: () => vi.fn(),
}));
vi.mock('../../../../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../../../../utils/with-async-wrapper', () => ({
    withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main(),
}));

function renderProfile(url = '/') {
    const user = {...new UsersApiService().initialize(), id: 'anna', fullName: 'Anna Meyer'};
    const router = createMemoryRouter([{
        path: '/',
        element: (
            <GenericDetailsPageContext.Provider value={{
                item: user,
                setItem: vi.fn(),
                setAdditionalData: vi.fn(),
                isBusy: false,
                setIsBusy: vi.fn(),
                refresh: vi.fn(),
                subscribeEvent: vi.fn(() => vi.fn()),
                isEditable: true,
            }}>
                <UserDetailsPageDeputies />
            </GenericDetailsPageContext.Provider>
        ),
    }], {initialEntries: [url]});
    render(
        <Provider store={configureStore({reducer: {shell: shellReducer}})}>
            <ConfirmProvider>
                <RouterProvider router={router} />
            </ConfirmProvider>
        </Provider>,
    );
    return router;
}

it('shows and searches one direction at a time and resets the list when switching', async () => {
    const relation = new VUserDeputyWithDetailsApiService().initialize();
    const relations = [
        {
            ...relation, id: 1,
            originalUserId: 'anna', originalUserFullName: 'Anna Meyer',
            deputyUserId: 'ben', deputyUserFullName: 'Ben Schulz',
        },
        {
            ...relation, id: 2,
            originalUserId: 'clara', originalUserFullName: 'Clara Bauer',
            deputyUserId: 'anna', deputyUserFullName: 'Anna Meyer',
        },
    ];
    const list = vi.spyOn(VUserDeputyWithDetailsApiService.prototype, 'list').mockImplementation(async (page, size, _sort, _order, filter) => {
        const content = relations.filter(row =>
            (!filter?.originalUserId || row.originalUserId === filter.originalUserId)
            && (!filter?.deputyUserId || row.deputyUserId === filter.deputyUserId)
            && row.originalUserFullName.includes(filter?.originalUserFullName ?? '')
            && row.deputyUserFullName.includes(filter?.deputyUserFullName ?? ''),
        );
        return {content, page: {size, number: page, totalElements: content.length, totalPages: 1}};
    });
    const router = renderProfile('/?sort=deputyUserFullName&order=desc');

    expect(await screen.findByText('Ben Schulz')).toBeVisible();
    expect(screen.queryByText('Clara Bauer')).not.toBeInTheDocument();
    const deputies = screen.getByRole('button', {name: 'Wird vertreten von', pressed: true});
    const represented = screen.getByRole('button', {name: 'Vertritt folgende Personen', pressed: false});
    expect(screen.getByRole('button', {name: 'Stellvertretung hinzufügen'})).toBeVisible();
    expect(screen.getByRole('button', {name: 'Stellvertretung bearbeiten'})).toBeVisible();
    expect(screen.getByRole('button', {name: 'Stellvertretung löschen'})).toBeVisible();

    const deputySearch = screen.getByRole('searchbox', {name: 'Stellvertreter:innen suchen'});
    fireEvent.change(deputySearch, {target: {value: 'Clara'}});
    fireEvent.blur(deputySearch);
    expect(await screen.findByText('Keine Stellvertreter:innen für diesen Suchbegriff gefunden')).toBeVisible();
    expect(list).toHaveBeenLastCalledWith(0, 12, 'deputyUserFullName', 'DESC', {originalUserId: 'anna', deputyUserFullName: 'Clara'});
    fireEvent.click(deputies);
    expect(deputySearch).toHaveValue('Clara');

    fireEvent.click(represented);
    expect(await screen.findByText('Clara Bauer')).toBeVisible();
    expect(represented).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByRole('button', {name: /Stellvertretung (hinzufügen|bearbeiten|löschen)/})).not.toBeInTheDocument();
    expect(list).toHaveBeenLastCalledWith(0, 12, 'untilDate', 'ASC', {deputyUserId: 'anna', originalUserFullName: undefined});
    expect(router.state.location.search).toBe('?view=represented');
    const representedSearch = screen.getByRole('searchbox', {name: 'Vertretene Personen suchen'});
    expect(representedSearch).toHaveValue('');
    fireEvent.change(representedSearch, {target: {value: 'Ben'}});
    fireEvent.blur(representedSearch);
    expect(await screen.findByText('Keine vertretenen Personen für diesen Suchbegriff gefunden')).toBeVisible();
    expect(list).toHaveBeenLastCalledWith(0, 12, 'untilDate', 'ASC', {deputyUserId: 'anna', originalUserFullName: 'Ben'});

    fireEvent.click(deputies);
    expect(await screen.findByText('Ben Schulz')).toBeVisible();
    expect(screen.getByRole('searchbox', {name: 'Stellvertreter:innen suchen'})).toHaveValue('');
});

it('opens the requested page and keeps the page size when switching to the first page of the other direction', async () => {
    const relation = {...new VUserDeputyWithDetailsApiService().initialize(), id: 25, deputyUserFullName: 'Ben Schulz'};
    const list = vi.spyOn(VUserDeputyWithDetailsApiService.prototype, 'list')
        .mockResolvedValueOnce({content: [relation], page: {size: 24, number: 1, totalElements: 25, totalPages: 2}})
        .mockResolvedValue({content: [], page: {size: 24, number: 0, totalElements: 0, totalPages: 0}});
    renderProfile('/?page=2&size=24');

    expect(await screen.findByText('Ben Schulz')).toBeVisible();
    expect(list).toHaveBeenLastCalledWith(1, 24, 'untilDate', 'ASC', {originalUserId: 'anna', deputyUserFullName: undefined});
    fireEvent.click(screen.getByRole('button', {name: 'Vertritt folgende Personen'}));
    expect(await screen.findByRole('heading', {name: 'Keine vertretenen Personen eingetragen'})).toBeVisible();
    expect(list).toHaveBeenLastCalledWith(0, 24, 'untilDate', 'ASC', {deputyUserId: 'anna', originalUserFullName: undefined});
});

it.each([
    ['/', 'Stellvertreter:innen werden geladen…', 'Keine Stellvertreter:innen eingetragen'],
    ['/?view=represented', 'Vertretene Personen werden geladen…', 'Keine vertretenen Personen eingetragen'],
])('distinguishes loading from an empty table for %s', async (url, loading, empty) => {
    const result = {content: [], page: {size: 12, number: 0, totalElements: 0, totalPages: 0}};
    const pending = Promise.withResolvers<typeof result>();
    vi.spyOn(VUserDeputyWithDetailsApiService.prototype, 'list').mockReturnValue(pending.promise);
    renderProfile(url);

    expect(screen.getByRole('status')).toHaveTextContent(loading);
    expect(screen.queryByRole('heading', {name: empty})).not.toBeInTheDocument();
    await act(async () => pending.resolve(result));
    expect(await screen.findByRole('heading', {name: empty})).toBeVisible();
    expect(screen.queryByText(loading)).not.toBeInTheDocument();
});
