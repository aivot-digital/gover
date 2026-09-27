import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceAccessControlApiService} from '../../services/process-instance-access-control-api-service';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {VDepartmentShadowedApiService} from '../../../departments/services/v-department-shadowed-api-service';
import {TeamsApiService} from '../../../teams/services/teams-api-service';
import {Permission} from '../../../../data/permissions/permission';
import {ProcessInstanceDetailsPagePermissions} from './process-instance-details-page-permissions';

const access = vi.hoisted(() => ({
    canEdit: true,
    refresh: vi.fn(),
    dispatch: vi.fn(),
}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (_id: number, permission: string) =>
        permission === 'process_instance.read' || access.canEdit,
    useRefreshPermissionSet: () => access.refresh,
}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => access.dispatch}));
vi.mock('../../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: ({original, edited}: {original: unknown; edited: unknown}) => ({
        hasChanged: JSON.stringify(original) !== JSON.stringify(edited),
        dialog: null,
    }),
}));
const entry = {
    id: 3,
    sourceDepartmentId: null,
    sourceTeamId: 8,
    targetProcessInstanceId: 17,
    permissions: [Permission.PROCESS_INSTANCE_READ],
    created: '',
    updated: '',
};

function renderPermissions() {
    const item = {
        instance: {
            ...new ProcessInstanceApiService().initialize(),
            id: 17,
        },
        departmentId: 1,
    };
    return render(
        <GenericDetailsPageContext.Provider
            value={{
                item,
                setItem: vi.fn(),
                setAdditionalData: vi.fn(),
                isBusy: false,
                setIsBusy: vi.fn(),
                refresh: vi.fn(),
                isEditable: access.canEdit,
            }}
        >
            <ProcessInstanceDetailsPagePermissions />
        </GenericDetailsPageContext.Provider>,
    );
}

describe('Instance permissions', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        access.canEdit = true;
        access.refresh.mockReset().mockResolvedValue(undefined);
        access.dispatch.mockReset();
        vi.spyOn(ProcessInstanceAccessControlApiService.prototype, 'listAll').mockResolvedValue({
            content: [entry],
        } as never);
        vi.spyOn(PermissionApiService.prototype, 'listPermissions').mockResolvedValue([
            {
                contextLabel: 'Vorgänge',
                permissions: [
                    {
                        permission: Permission.PROCESS_INSTANCE_READ,
                        label: 'Vorgang anzeigen',
                    },
                    {
                        permission: Permission.PROCESS_INSTANCE_UPDATE,
                        label: 'Vorgang bearbeiten',
                    },
                ],
            },
        ] as never);
        vi.spyOn(VDepartmentShadowedApiService.prototype, 'listAll').mockResolvedValue({content: []} as never);
        vi.spyOn(TeamsApiService.prototype, 'listAll').mockResolvedValue({
            content: [
                {
                    id: 8,
                    name: 'Prüfteam',
                },
            ],
        } as never);
    });

    it('shows the matrix without allowing changes for instance readers', async () => {
        access.canEdit = false;
        renderPermissions();
        expect(await screen.findByRole('checkbox', {name: 'Vorgang anzeigen für Prüfteam'})).toBeChecked();
        for (const checkbox of screen.getAllByRole('checkbox')) expect(checkbox).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Berechtigungen speichern'})).not.toBeInTheDocument();
        expect(ProcessInstanceAccessControlApiService.prototype.listAll).toHaveBeenCalledWith({
            targetProcessInstanceId: 17,
        });
    });

    it('saves instance rights and refreshes permissions across tabs', async () => {
        const update = vi
            .spyOn(ProcessInstanceAccessControlApiService.prototype, 'replace')
            .mockImplementation(async (_id, entities) => entities);
        renderPermissions();
        const user = userEvent.setup();
        await user.click(await screen.findByRole('checkbox', {name: 'Vorgang bearbeiten für Prüfteam'}));
        await user.click(screen.getByRole('button', {name: 'Berechtigungen speichern'}));
        await waitFor(() =>
            expect(update).toHaveBeenCalledWith(17, [
                {
                    ...entry,
                    permissions: [Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_INSTANCE_UPDATE],
                },
            ]),
        );
        expect(access.refresh).toHaveBeenCalledWith({broadcast: true});
        expect(screen.getByRole('button', {name: 'Berechtigungen speichern'})).toBeDisabled();
    });

    it('keeps failed changes editable and permits retry', async () => {
        const update = vi
            .spyOn(ProcessInstanceAccessControlApiService.prototype, 'replace')
            .mockRejectedValueOnce(new Error('Unavailable'))
            .mockImplementation(async (_id, entities) => entities);
        renderPermissions();
        const user = userEvent.setup();
        await user.click(await screen.findByRole('checkbox', {name: 'Vorgang bearbeiten für Prüfteam'}));
        await user.click(screen.getByRole('button', {name: 'Berechtigungen speichern'}));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Berechtigungen speichern'})).toBeEnabled());
        expect(access.refresh).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Berechtigungen speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalledTimes(2));
        expect(access.refresh).toHaveBeenCalledWith({broadcast: true});
    });

    it('shows affected tasks, preserves the entire draft and retries the complete matrix', async () => {
        const update = vi
            .spyOn(ProcessInstanceAccessControlApiService.prototype, 'replace')
            .mockRejectedValueOnce({
                status: 409,
                displayableToUser: true,
                message: 'Bitte weisen Sie die betroffenen Aufgaben zuerst anderen berechtigten Personen zu.',
                details: {
                    reason: 'assigned_tasks_lose_access',
                    tasks: [
                        {
                            id: 41,
                            name: 'Antrag prüfen',
                            assignedUserName: 'Alex Beispiel',
                        },
                        {
                            id: 42,
                            name: 'Freigabe prüfen',
                            assignedUserName: 'Sam Beispiel',
                        },
                    ],
                },
            })
            .mockImplementation(async (_id, entities) => entities);
        renderPermissions();
        const user = userEvent.setup();
        await user.click(await screen.findByRole('checkbox', {name: 'Vorgang anzeigen für Prüfteam'}));
        await user.click(screen.getByRole('button', {name: 'Berechtigungen speichern'}));
        const details = await screen.findByRole('alert', {name: 'Betroffene Aufgaben'});
        expect(details).toHaveTextContent('Bitte weisen Sie');
        expect(screen.getByRole('link', {name: 'Antrag prüfen'})).toHaveAttribute(
            'href',
            'http://localhost/staff/tasks/17/41',
        );
        expect(screen.getByRole('link', {name: 'Antrag prüfen'})).toHaveAttribute('target', '_blank');
        expect(details).toHaveTextContent('Alex Beispiel');
        expect(details).toHaveTextContent('Sam Beispiel');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
        expect(access.dispatch).toHaveBeenCalledWith(
            expect.objectContaining({
                payload: expect.objectContaining({
                    message:
                        'Berechtigungen nicht gespeichert. Bitte weisen Sie die betroffenen Aufgaben zuerst neu zu.',
                    severity: 'error',
                }),
            }),
        );
        expect(screen.getByRole('checkbox', {name: 'Vorgang anzeigen für Prüfteam'})).not.toBeChecked();
        expect(access.refresh).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Berechtigungen speichern'}));
        await waitFor(() => expect(access.refresh).toHaveBeenCalledTimes(1));
        expect(update.mock.calls[0]).toEqual(update.mock.calls[1]);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Berechtigungen speichern'})).toBeDisabled();
    });

    it('does not allow editing when loading access rules fails', async () => {
        vi.mocked(ProcessInstanceAccessControlApiService.prototype.listAll).mockRejectedValue(new Error('Unavailable'));
        renderPermissions();
        await screen.findByText('Die Berechtigungen konnten nicht geladen werden.');
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Berechtigungen speichern'})).not.toBeInTheDocument();
    });
});
