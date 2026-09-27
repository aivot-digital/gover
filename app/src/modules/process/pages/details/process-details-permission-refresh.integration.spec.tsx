import {type PropsWithChildren} from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {createMemoryRouter, RouterProvider, useRouteError} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {userReducer, setPermissions, setUser} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {type PermissionSet} from '../../../permissions/models/permission-set';
import {useHasProcessInstancePermission} from '../../../permissions/hooks/use-permissions';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ProcessNodeProviderApiService} from '../../services/process-node-provider-api-service';
import {ProcessInstanceDetailsPage} from './process-instance-details-page';
import {ProcessTaskViewPage, type ProcessTaskDetailsPageItem} from './process-task-view-page';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';

vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: PropsWithChildren) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../../../modules/search/hooks/use-record-recent-search-item', () => ({
    useRecordRecentSearchItem: () => {},
}));
vi.mock('../../../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});

function permissions(instanceId?: number, system = false, canEditTasks = true): PermissionSet {
    const grants = [
        Permission.PROCESS_INSTANCE_READ,
        Permission.PROCESS_INSTANCE_UPDATE,
        ...(canEditTasks ? [Permission.PROCESS_INSTANCE_EDIT_TASK] : []),
    ];
    return {
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions:
            instanceId == null
                ? []
                : [
                      {
                          id: 'grant',
                          userId: 'user',
                          processInstanceId: instanceId,
                          permissions: grants,
                      },
                  ],
        systemPermissions: system
            ? [
                  {
                      userId: 'user',
                      permissions: grants,
                  },
              ]
            : [],
    };
}

function DetailsContent() {
    const {item} = useGenericDetailsPageContext<ProcessInstanceDetails | ProcessTaskDetailsPageItem, undefined>();
    const canEdit = useHasProcessInstancePermission(item?.instance?.id, Permission.PROCESS_INSTANCE_UPDATE);
    return item == null ? <p>Details werden geladen</p> : <button disabled={!canEdit}>Bearbeiten</button>;
}

function RouteError() {
    const error = useRouteError() as {status: number};
    return <p role="alert">Fehler {error.status}</p>;
}

function renderPage(kind: 'instance' | 'task', initialPermissions = permissions(), initialPath?: string) {
    const store = configureStore({reducer: {user: userReducer}});
    store.dispatch(
        setUser({
            id: 'user',
            email: 'user@example.org',
            firstName: 'Test',
            lastName: 'User',
            fullName: 'Test User',
            enabled: true,
            verified: true,
            deletedInIdp: false,
            systemRoleId: null,
        }),
    );
    store.dispatch(setPermissions(initialPermissions));
    const router = createMemoryRouter(
        [
            {
                path: kind === 'instance' ? '/process-instances/:id' : '/tasks/:instanceId/:taskId',
                element: kind === 'instance' ? <ProcessInstanceDetailsPage /> : <ProcessTaskViewPage />,
                errorElement: <RouteError />,
                children: [
                    {
                        index: true,
                        element: <DetailsContent />,
                    },
                    {
                        path: kind === 'instance' ? 'permissions' : 'edit',
                        element: <DetailsContent />,
                    },
                ],
            },
        ],
        {initialEntries: [initialPath ?? (kind === 'instance' ? '/process-instances/17' : '/tasks/17/8')]},
    );
    render(
        <Provider store={store}>
            <RouterProvider router={router} />
        </Provider>,
    );
    return {
        router,
        store,
    };
}

describe('Permissions on process detail navigation', () => {
    beforeEach(() => {
        const instance = {
            ...new ProcessInstanceApiService().initialize(),
            id: 17,
        };
        vi.spyOn(ProcessInstanceApiService.prototype, 'retrieveDetails').mockImplementation(async (id) => ({
            instance: {
                ...instance,
                id,
            },
            processName: 'Prüfung',
            departmentId: 1,
            departmentName: null,
            triggerName: 'Start',
            triggerType: null,
            activeTasks: [],
        }));
        vi.spyOn(ProcessInstanceApiService.prototype, 'retrieve').mockResolvedValue(instance);
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'retrieveDetails').mockResolvedValue({
            task: {
                ...new ProcessInstanceTaskApiService().initialize(),
                id: 8,
                processInstanceId: 17,
            },
            instance,
            process: {
                id: 1,
                internalTitle: 'Prozess',
            },
            node: {
                name: 'Prüfung',
                description: null,
            },
            provider: null,
        });
        // Instance readers may not access the process-model endpoints at all.
        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockRejectedValue({status: 403});
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockRejectedValue({status: 403});
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockRejectedValue({status: 403});
    });
    afterEach(() => vi.restoreAllMocks());

    it.each(['instance', 'task'] as const)(
        'waits for fresh permissions on %s entry without refreshing on tab changes',
        async (kind) => {
            let resolvePermissions!: (value: PermissionSet) => void;
            const refresh = vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockReturnValue(
                new Promise((resolve) => {
                    resolvePermissions = resolve;
                }),
            );
            const {router, store} = renderPage(kind);
            await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
            expect(screen.getByText('Details werden geladen')).toBeInTheDocument();
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
            await act(async () => resolvePermissions(permissions(17)));
            expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
            expect(store.getState().user.permissions).toEqual(permissions(17));
            expect(ProcessNodeApiService.prototype.retrieve).not.toHaveBeenCalled();
            expect(ProcessDefinitionApiService.prototype.retrieve).not.toHaveBeenCalled();
            expect(ProcessNodeProviderApiService.prototype.getNodeProvider).not.toHaveBeenCalled();

            await act(async () =>
                router.navigate(kind === 'instance' ? '/process-instances/17/permissions' : '/tasks/17/8/edit'),
            );
            expect(screen.getByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
            expect(refresh).toHaveBeenCalledTimes(1);

            refresh.mockResolvedValue(permissions(kind === 'instance' ? 18 : 17));
            await act(async () => router.navigate(kind === 'instance' ? '/process-instances/18' : '/tasks/17/9'));
            await waitFor(() => expect(refresh).toHaveBeenCalledTimes(2));
            expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
        },
    );

    it.each([permissions(), permissions(99)])(
        'still denies instance access without the matching grant',
        async (refreshedPermissions) => {
            vi.spyOn(console, 'error').mockImplementation(() => {});
            vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(refreshedPermissions);
            renderPage('instance', permissions(17));
            expect(await screen.findByRole('alert')).toHaveTextContent('Fehler 403');
            expect(screen.queryByRole('button', {name: 'Bearbeiten'})).not.toBeInTheDocument();
        },
    );

    it('respects refreshed system permissions', async () => {
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(permissions(undefined, true));
        renderPage('instance');
        expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
    });

    it('updates task actions when permissions have been revoked', async () => {
        const readOnly = permissions(17);
        readOnly.processInstancePermissions[0].permissions = [Permission.PROCESS_INSTANCE_READ];
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(readOnly);
        renderPage('task', permissions(17));
        expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeDisabled();
    });

    it.each(['read', 'instance-update', 'other-instance'])(
        'keeps task details readable but denies editing with only %s access',
        async (scope) => {
            vi.spyOn(console, 'error').mockImplementation(() => {});
            const grants = permissions(17, false, false);
            if (scope === 'read') {
                grants.processInstancePermissions[0].permissions = [Permission.PROCESS_INSTANCE_READ];
            } else if (scope === 'other-instance') {
                grants.processInstancePermissions.push(...permissions(18).processInstancePermissions);
            }
            vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(grants);
            const {router} = renderPage('task');

            expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeInTheDocument();
            expect(screen.getByRole('tab', {name: 'Allgemeine Informationen'})).toHaveAttribute('aria-selected', 'true');
            const editTab = screen.getByRole('tab', {name: /process_instance\.edit_task/});
            expect(editTab).toHaveTextContent('Aufgabe bearbeiten');
            expect(editTab).toHaveAttribute('aria-disabled', 'true');

            await act(async () => router.navigate('/tasks/17/8/edit'));
            expect(await screen.findByRole('alert')).toHaveTextContent('Fehler 403');
            expect(screen.queryByRole('button', {name: 'Bearbeiten'})).not.toBeInTheDocument();
        },
    );

    it.each([permissions(17), permissions(undefined, true)])(
        'allows the task edit route with matching scoped or system edit rights',
        async (grants) => {
            vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(grants);
            renderPage('task', permissions(), '/tasks/17/8/edit');

            expect(await screen.findByRole('button', {name: 'Bearbeiten'})).toBeEnabled();
            expect(screen.getByRole('tab', {name: 'Aufgabe bearbeiten'})).toHaveAttribute('aria-selected', 'true');
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        },
    );

    it('rejects task details when refreshed permissions no longer grant access to their instance', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(permissions(99));
        renderPage('task', permissions(17));
        expect(await screen.findByRole('alert')).toHaveTextContent('Fehler 403');
    });

    it.each(['instance', 'task'] as const)(
        'does not fall back to stale grants when the %s permission refresh fails',
        async (kind) => {
            vi.spyOn(console, 'error').mockImplementation(() => {});
            vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockRejectedValue({
                status: 503,
                message: 'Unavailable',
                details: null,
                displayableToUser: false,
            });
            renderPage(kind, permissions(17));
            expect(await screen.findByRole('alert')).toHaveTextContent('Fehler 503');
            expect(screen.queryByRole('button', {name: 'Bearbeiten'})).not.toBeInTheDocument();
        },
    );

    it('preserves server-side task access denials', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(permissions(17));
        vi.mocked(ProcessInstanceTaskApiService.prototype.retrieveDetails).mockRejectedValue({
            status: 403,
            message: 'Forbidden',
            details: null,
            displayableToUser: false,
        });
        renderPage('task');
        expect(await screen.findByRole('alert')).toHaveTextContent('Fehler 403');
    });
});
