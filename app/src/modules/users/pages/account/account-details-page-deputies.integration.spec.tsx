import {configureStore} from '@reduxjs/toolkit';
import {render, screen} from '@testing-library/react';
import {Provider} from 'react-redux';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {expect, it, vi} from 'vitest';
import {authReducer} from '../../../../slices/auth-slice';
import {setUser, userReducer} from '../../../../slices/user-slice';
import {VUserDeputyWithDetailsApiService} from '../../services/v-user-deputy-with-details-api-service';
import {UsersApiService} from '../../users-api-service';
import {AccountDetailsPage} from './account-details-page';
import {AccountDetailsPageDeputies} from './account-details-page-deputies';

vi.mock('../../../../utils/with-async-wrapper', () => ({
    withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main(),
}));

it('shows only the people the signed-in user represents in the Stellvertretung tab', async () => {
    const user = {...new UsersApiService().initialize(), id: 'anna', fullName: 'Anna Meyer'};
    const store = configureStore({reducer: {user: userReducer, auth: authReducer}});
    store.dispatch(setUser(user));
    const api = VUserDeputyWithDetailsApiService.prototype;
    const list = vi.spyOn(api, 'list').mockResolvedValue({
        content: [],
        page: {size: 12, number: 0, totalPages: 0, totalElements: 0},
    });

    render(
        <Provider store={store}>
            <MemoryRouter initialEntries={['/account/deputies']}>
                <Routes>
                    <Route path="/account" element={<AccountDetailsPage />}>
                        <Route path="deputies" element={<AccountDetailsPageDeputies />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </Provider>,
    );

    expect(screen.getByRole('tab', {name: 'Stellvertretung', selected: true})).toBeVisible();
    expect(screen.getByRole('heading', {name: 'Personen, die Sie vertreten'})).toBeVisible();
    expect(screen.getByRole('searchbox', {name: 'Vertretene Personen suchen'})).toBeVisible();
    expect(screen.queryByRole('button', {name: 'Wird vertreten von'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Stellvertretung hinzufügen'})).not.toBeInTheDocument();
    expect(await screen.findByRole('heading', {name: 'Keine vertretenen Personen eingetragen'})).toBeVisible();
    expect(list).toHaveBeenCalledWith(0, 12, 'untilDate', 'ASC', {deputyUserId: 'anna', originalUserFullName: undefined});
});
