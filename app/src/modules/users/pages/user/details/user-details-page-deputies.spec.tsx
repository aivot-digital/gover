import {configureStore} from '@reduxjs/toolkit';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {type ReactNode} from 'react';
import {Provider} from 'react-redux';
import {expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../../components/generic-details-page/generic-details-page-context';
import {ConfirmProvider} from '../../../../../providers/confirm-provider';
import {ShellProgress} from '../../../../../shells/staff/components/shell-progress';
import {shellReducer} from '../../../../../slices/shell-slice';
import {UserDeputyApiService} from '../../../services/user-deputy-api-service';
import {UsersApiService} from '../../../users-api-service';
import {UserDetailsPageDeputies} from './user-details-page-deputies';

vi.mock('../../../../../components/generic-list/generic-list', () => ({
    GenericList: ({preSearchElements}: {preSearchElements?: ReactNode}) => preSearchElements ?? null,
}));
vi.mock('../../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
    useRefreshPermissionSet: () => async () => {},
}));

it.each([
    ['Anna Meyer', 'Füge Stellvertretung für Anna Meyer hinzu…'],
    ['', 'Füge Stellvertretung für diese Person hinzu…'],
])('shows the represented person in the loading message for display name "%s"', async (fullName, message) => {
    const represented = {...new UsersApiService().initialize(), id: 'represented-id', fullName};
    const deputy = {
        ...new UsersApiService().initialize(),
        id: 'deputy-id',
        firstName: 'Ben',
        lastName: 'Schulz',
        fullName: 'Ben Schulz',
    };
    vi.spyOn(UsersApiService.prototype, 'listAll').mockResolvedValue({
        content: [deputy],
        page: {number: 0, size: 1, totalPages: 1, totalElements: 1},
    });
    let finishSave!: () => void;
    const create = vi.spyOn(UserDeputyApiService.prototype, 'create').mockImplementation(draft => new Promise(resolve => {
        finishSave = () => resolve({...draft, id: 1});
    }));
    render(
        <Provider store={configureStore({reducer: {shell: shellReducer}})}>
            <ConfirmProvider>
                <GenericDetailsPageContext.Provider value={{
                    item: represented,
                    setItem: vi.fn(),
                    setAdditionalData: vi.fn(),
                    isBusy: false,
                    setIsBusy: vi.fn(),
                    refresh: vi.fn(),
                    subscribeEvent: vi.fn(() => vi.fn()),
                    isEditable: true,
                }}>
                    <UserDetailsPageDeputies />
                    <ShellProgress />
                </GenericDetailsPageContext.Provider>
            </ConfirmProvider>
        </Provider>,
    );

    fireEvent.click(screen.getByRole('button', {name: 'Stellvertreter:in hinzufügen'}));
    fireEvent.click(await screen.findByRole('button', {name: 'Ben Schulz'}));
    fireEvent.click(screen.getByRole('button', {name: 'Hinzufügen'}));

    expect(screen.getByText(message)).toBeVisible();
    expect(create).toHaveBeenCalledWith(expect.objectContaining({
        originalUserId: represented.id,
        deputyUserId: deputy.id,
    }));
    await act(async () => finishSave());
});
