import {configureStore} from '@reduxjs/toolkit';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {Provider} from 'react-redux';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';
import {Actions} from '../../../../../components/actions/actions';
import {GenericDetailsPageContext} from '../../../../../components/generic-details-page/generic-details-page-context';
import {type GenericListProps} from '../../../../../components/generic-list/generic-list-props';
import {ConfirmProvider} from '../../../../../providers/confirm-provider';
import {SnackbarProvider} from '../../../../../providers/snackbar-provider';
import {ShellProgress} from '../../../../../shells/staff/components/shell-progress';
import {shellReducer} from '../../../../../slices/shell-slice';
import {parseLocalDateIso} from '../../../../../utils/temporal-utils';
import {type VUserDeputyWithDetailsEntity} from '../../../entities/v-user-deputy-with-details-entity';
import {UserDeputyApiService} from '../../../services/user-deputy-api-service';
import {VUserDeputyWithDetailsApiService} from '../../../services/v-user-deputy-with-details-api-service';
import {UsersApiService} from '../../../users-api-service';
import {UserDetailsPageDeputies} from './user-details-page-deputies';

const refreshPermissions = vi.hoisted(() => vi.fn(async () => {}));
vi.mock('../../../../../components/generic-list/generic-list', () => ({
    GenericList: ({preSearchElements, rowActions}: GenericListProps<VUserDeputyWithDetailsEntity>) => (
        <>
            {preSearchElements}
            <Actions actions={rowActions?.(relation) ?? []}/>
        </>
    ),
}));
vi.mock('../../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
    useRefreshPermissionSet: () => refreshPermissions,
}));

const deputy = {
    ...new UsersApiService().initialize(),
    id: 'deputy-id',
    firstName: 'Ben',
    lastName: 'Schulz',
    fullName: 'Ben Schulz',
};

const relation = {
    ...new VUserDeputyWithDetailsApiService().initialize(),
    id: 18,
    originalUserId: 'represented-id',
    originalUserFullName: 'Anna Meyer',
    deputyUserId: deputy.id,
    deputyUserFullName: deputy.fullName,
    fromDate: parseLocalDateIso('2026-05-01'),
    lockVersion: 3,
};

function renderProfile(fullName = relation.originalUserFullName) {
    const represented = {...new UsersApiService().initialize(), id: relation.originalUserId, fullName};
    const page = {number: 0, size: 1, totalPages: 1, totalElements: 1};
    vi.spyOn(UsersApiService.prototype, 'listAll').mockResolvedValue({content: [deputy], page});

    render(
        <MemoryRouter>
            <Provider store={configureStore({reducer: {shell: shellReducer}})}>
                <ConfirmProvider>
                    <SnackbarProvider>
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
                    </SnackbarProvider>
                </ConfirmProvider>
            </Provider>
        </MemoryRouter>,
    );
}

function expectParticipants(dialog: HTMLElement, originalUserFullName: string) {
    expect(within(dialog).getAllByRole('term').map(term => term.textContent))
        .toEqual(['Vertretene Person:', 'Stellvertreter:in:']);
    expect(within(dialog).getAllByRole('definition').map(value => value.textContent))
        .toEqual([originalUserFullName, deputy.fullName]);
}

it.each([
    ['Anna Meyer', 'Anna Meyer', 'Anna Meyer'],
    ['', 'diese Person', 'Kein Name hinterlegt'],
])('names both people while adding a deputy for a profile with name "%s"', async (fullName, displayedName, detailsName) => {
    renderProfile(fullName);
    let finishSave!: () => void;
    const create = vi.spyOn(UserDeputyApiService.prototype, 'create').mockImplementation(draft => new Promise(resolve => {
        finishSave = () => resolve({...draft, id: 1});
    }));
    fireEvent.click(screen.getByRole('button', {name: 'Stellvertretung hinzufügen'}));
    const select = screen.getByRole('dialog', {name: `Stellvertreter:in für ${displayedName} auswählen`});
    fireEvent.click(await within(select).findByRole('button', {name: deputy.fullName}));
    const period = screen.getByRole('dialog', {name: 'Stellvertretung hinzufügen'});
    expectParticipants(period, detailsName);
    fireEvent.click(within(period).getByRole('button', {name: 'Hinzufügen'}));

    expect(screen.getByText(`Füge Stellvertretung für ${displayedName} hinzu…`)).toBeVisible();
    expect(create).toHaveBeenCalledWith(expect.objectContaining({
        originalUserId: relation.originalUserId,
        deputyUserId: relation.deputyUserId,
    }));
    await act(async () => finishSave());
});

it('names the represented person while updating and shows the API error in the dialog flow', async () => {
    renderProfile();
    let rejectSave!: (reason: unknown) => void;
    const update = vi.spyOn(UserDeputyApiService.prototype, 'update').mockImplementation(() => new Promise((_resolve, reject) => {
        rejectSave = reject;
    }));
    fireEvent.click(await screen.findByRole('button', {name: 'Stellvertretung bearbeiten'}));
    const dialog = screen.getByRole('dialog', {name: 'Stellvertretung bearbeiten'});
    expectParticipants(dialog, relation.originalUserFullName);
    expect(within(dialog).getByText('Beginn der Stellvertretung')).toBeVisible();
    expect(within(dialog).getByText('Ende der Stellvertretung')).toBeVisible();
    fireEvent.click(within(dialog).getByRole('button', {name: 'Speichern'}));

    expect(update).toHaveBeenCalledWith(relation.id, expect.objectContaining({deputyUserId: deputy.id}));
    expect(screen.getByText('Aktualisiere Stellvertretung für Anna Meyer…')).toBeVisible();
    await act(async () => rejectSave({status: 409, displayableToUser: true, message: 'Der gewählte Zeitraum ist ungültig.'}));
    expect(await screen.findByText('Der gewählte Zeitraum ist ungültig.')).toBeVisible();
    expect(dialog).toBeVisible();
});

it('offers a page reload after an edit conflict and keeps the dialog open for another attempt', async () => {
    renderProfile();
    const message = 'Der Datensatz wurde zwischenzeitlich geändert. Laden Sie ihn bitte neu, bevor Sie Ihre Änderungen erneut speichern.';
    const update = vi.spyOn(UserDeputyApiService.prototype, 'update').mockRejectedValue({
        status: 409, message, details: {reason: 'lock_version_conflict'}, displayableToUser: true,
    });
    fireEvent.click(await screen.findByRole('button', {name: 'Stellvertretung bearbeiten'}));
    const dialog = screen.getByRole('dialog', {name: 'Stellvertretung bearbeiten'});
    fireEvent.click(within(dialog).getByRole('button', {name: 'Speichern'}));

    expect(await screen.findByText(`${message} Beim Neuladen gehen nicht gespeicherte Änderungen auf dieser Seite verloren.`)).toBeVisible();
    expect(dialog).toBeVisible();
    expect(refreshPermissions).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole('button', {name: 'Speichern'}));
    await waitFor(() => expect(update).toHaveBeenCalledTimes(2));
    expect(update).toHaveBeenLastCalledWith(relation.id, expect.objectContaining({deputyUserId: deputy.id, lockVersion: 3}));
});

it('shows the people and period before deletion and supports cancelling and confirming', async () => {
    renderProfile();
    const destroy = vi.spyOn(UserDeputyApiService.prototype, 'destroy').mockRejectedValue(new Error('unexpected'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const remove = await screen.findByRole('button', {name: 'Stellvertretung löschen'});
    fireEvent.click(remove);
    const dialog = screen.getByRole('dialog', {name: 'Stellvertretung löschen?'});
    expectParticipants(dialog, relation.originalUserFullName);
    expect(within(dialog).getByText(/ab 01\.05\.2026, unbegrenzt/)).toBeVisible();
    fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
    expect(destroy).not.toHaveBeenCalled();

    fireEvent.click(remove);
    const confirm = screen.getByRole('dialog', {name: 'Stellvertretung löschen?'});
    fireEvent.click(within(confirm).getByRole('button', {name: 'Stellvertretung löschen'}));
    expect(await screen.findByText('Die Stellvertretung konnte nicht gelöscht werden.')).toBeVisible();
    expect(destroy).toHaveBeenCalledWith(relation.id);
});

it('identifies a permission refresh failure after the deputy relation has been deleted', async () => {
    renderProfile();
    vi.spyOn(UserDeputyApiService.prototype, 'destroy').mockResolvedValue();
    refreshPermissions.mockRejectedValueOnce({status: 403, displayableToUser: true, message: 'Zugriff verweigert.'});
    fireEvent.click(await screen.findByRole('button', {name: 'Stellvertretung löschen'}));
    const dialog = screen.getByRole('dialog', {name: 'Stellvertretung löschen?'});
    fireEvent.click(within(dialog).getByRole('button', {name: 'Stellvertretung löschen'}));

    expect(await screen.findByText('Die Berechtigungen konnten nach der Änderung der Stellvertretung nicht aktualisiert werden. Zugriff verweigert.')).toBeVisible();
});
