import React from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {type User} from '../../../users/models/user';
import {UsersApiService} from '../../../users/users-api-service';
import {type ProcessTaskDetailsPageItem} from './process-task-view-page';
import {ProcessTaskViewPageIndex} from './process-task-view-page-index';

const access = vi.hoisted(() => ({canReadUsers: true, user: undefined as User | undefined}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => access.user}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => access.canReadUsers}));
vi.mock('./process-task-view-page', () => ({
    getProcessTaskDescription: () => 'Unterlagen prüfen',
    getProcessTaskName: () => 'Prüfung',
    getProcessTaskNodeIcon: () => null,
    getProcessTaskProcessPath: () => null,
    getProcessTaskEditPath: () => '/edit',
}));

function createItem(): ProcessTaskDetailsPageItem {
    return {
        task: {...new ProcessInstanceTaskApiService().initialize(), status: ProcessTaskStatus.AwaitingPayment,
            updated: '2026-09-21T10:30:00Z'},
        instance: new ProcessInstanceApiService().initialize(),
        process: null, node: null, provider: null,
    };
}

function renderItem(item: ProcessTaskDetailsPageItem) {
    return render(
        <MemoryRouter>
            <GenericDetailsPageContext.Provider value={{item, setItem: vi.fn(), setAdditionalData: vi.fn(),
                isBusy: false, setIsBusy: vi.fn(), refresh: vi.fn(), isEditable: false}}>
                <ProcessTaskViewPageIndex />
            </GenericDetailsPageContext.Provider>
        </MemoryRouter>,
    );
}

const row = (label: string) => within(screen.getByRole('row', {name: new RegExp(label)}));

describe('ProcessTaskViewPageIndex metadata', () => {
    beforeEach(() => { access.canReadUsers = true; access.user = undefined; });

    it('shows the task status independently of the instance and omits absent optional metadata', () => {
        const retrieve = vi.spyOn(UsersApiService.prototype, 'retrieve');
        renderItem(createItem());
        expect(row('Aufgabenstatus').getByText('Wartet auf Zahlungsbestätigung')).toBeInTheDocument();
        expect(row('Zuständige Person').getByText('Nicht zugewiesen')).toBeInTheDocument();
        expect(row('Zuletzt aktualisiert').getByText(/21\.09\.2026/)).toBeInTheDocument();
        expect(screen.queryByText('Externe Beteiligung')).not.toBeInTheDocument();
        expect(screen.queryByText('Beendet am')).not.toBeInTheDocument();
        expect(retrieve).not.toHaveBeenCalled();
    });

    it('resolves the assigned staff member and external identity and shows the end time', async () => {
        const user = {...new UsersApiService().initialize(), id: 'staff', firstName: 'Alex', lastName: 'Beispiel'};
        vi.spyOn(UsersApiService.prototype, 'retrieve').mockResolvedValue(user);
        const item = createItem();
        item.task.assignedUserId = user.id;
        item.task.status = ProcessTaskStatus.Completed;
        item.task.statusOverride = 'Prüfung abgeschlossen';
        item.task.assignedCustomerIdentityId = 'external';
        item.task.finished = '2026-09-22T11:00:00Z';
        item.instance!.identities = {differentMapKey: {identityId: 'external', sessionId: 'session', type: 'Email',
            emailAddress: 'erika@example.org', attributes: {given_name: 'Erika', family_name: 'Muster'},
            providerKey: null, metadataIdentifier: null, uniqueIdFromIdentityProvider: null,
            communicationProviderBindingId: null, communicationProviderData: {}}};
        renderItem(item);
        expect(await screen.findByText('Alex Beispiel')).toBeInTheDocument();
        expect(row('Externe Beteiligung').getByText('Erika Muster')).toBeInTheDocument();
        expect(row('Aufgabenstatus').getByText('Prüfung abgeschlossen')).toBeInTheDocument();
        expect(row('Abgeschlossen am').getByText(/22\.09\.2026/)).toBeInTheDocument();
    });

    it.each([
        [ProcessTaskStatus.Aborted, 'Abgebrochen am'],
        [ProcessTaskStatus.Failed, 'Fehlgeschlagen am'],
        [ProcessTaskStatus.Restarted, 'Beendet am'],
    ])('labels the end time for %s accurately', (status, label) => {
        const item = createItem();
        item.task.status = status;
        item.task.finished = '2026-09-22T11:00:00Z';
        renderItem(item);
        expect(row(label).getByText(/22\.09\.2026/)).toBeInTheDocument();
        expect(screen.queryByText('Abgeschlossen am')).not.toBeInTheDocument();
    });

    it('uses the email address when the external identity has no name', () => {
        const item = createItem();
        item.task.assignedCustomerIdentityId = 'external';
        item.instance!.identities = {external: {identityId: 'external', sessionId: 'session', type: 'Email',
            emailAddress: 'erika@example.org', attributes: {}, providerKey: null, metadataIdentifier: null,
            uniqueIdFromIdentityProvider: null, communicationProviderBindingId: null, communicationProviderData: {}}};
        renderItem(item);
        expect(row('Externe Beteiligung').getByText('erika@example.org')).toBeInTheDocument();
    });

    it('does not request another user without permission and preserves unresolved external assignment', () => {
        access.canReadUsers = false;
        const retrieve = vi.spyOn(UsersApiService.prototype, 'retrieve');
        const item = createItem();
        item.task.assignedUserId = 'other';
        item.task.assignedCustomerIdentityId = 'missing';
        renderItem(item);
        expect(row('Zuständige Person').getByText('Zugewiesen · Name nicht verfügbar')).toBeInTheDocument();
        expect(row('Externe Beteiligung').getByText('Zugewiesen · Angaben nicht verfügbar')).toBeInTheDocument();
        expect(retrieve).not.toHaveBeenCalled();
    });

    it('uses the current user without requiring user read permission', () => {
        access.canReadUsers = false;
        access.user = {...new UsersApiService().initialize(), id: 'self', firstName: 'Kim', lastName: 'Muster'};
        const retrieve = vi.spyOn(UsersApiService.prototype, 'retrieve');
        const item = createItem();
        item.task.assignedUserId = 'self';
        renderItem(item);
        expect(row('Zuständige Person').getByText('Kim Muster')).toBeInTheDocument();
        expect(retrieve).not.toHaveBeenCalled();
    });

    it('keeps the task readable when the user lookup fails', async () => {
        vi.spyOn(UsersApiService.prototype, 'retrieve').mockRejectedValue(new Error('Unavailable'));
        const item = createItem();
        item.task.assignedUserId = 'other';
        renderItem(item);
        await waitFor(() => expect(row('Zuständige Person').getByText('Zugewiesen · Name nicht verfügbar')).toBeInTheDocument());
    });
});
