import {type PropsWithChildren} from 'react';
import {act, renderHook, waitFor} from '@testing-library/react';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {setPermissions, setUser, userReducer} from '../../../slices/user-slice';
import {shellReducer} from '../../../slices/shell-slice';
import {PermissionApiService} from '../../permissions/permission-api-service';
import {type PermissionSet} from '../../permissions/models/permission-set';
import {useHasProcessInstancePermission} from '../../permissions/hooks/use-permissions';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessInstanceAttachmentApiService} from '../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../services/process-instance-attachment-set-api-service';
import {useProcessRuntimeData} from './use-process-runtime-data';

function permissions(instanceIds: number[], system = false): PermissionSet {
    return {
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions: instanceIds.map((processInstanceId) => ({
            id: `grant-${processInstanceId}`,
            userId: 'user',
            viaSourceDepartmentId: 1,
            processInstanceId,
            permissions: [Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_INSTANCE_EDIT_TASK],
        })),
        systemPermissions: system
            ? [
                  {
                      userId: 'user',
                      permissions: [Permission.PROCESS_INSTANCE_EDIT_TASK],
                  },
              ]
            : [],
    };
}

function emptyPage() {
    return {
        content: [],
        page: {
            size: 0,
            number: 0,
            totalElements: 0,
            totalPages: 0,
        },
    };
}

function renderRuntimeData(instanceId: number, initialPermissions: PermissionSet) {
    const store = configureStore({reducer: {user: userReducer, shell: shellReducer}});
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

    const hook = renderHook(
        ({instanceId}: {instanceId: number}) => {
            const runtime = useProcessRuntimeData(instanceId);
            // Mirrors the gate of "Aufgabe erneut ausführen" in the flow editor node.
            const canEditTask = useHasProcessInstancePermission(
                runtime.runtimeData?.instance.id,
                Permission.PROCESS_INSTANCE_EDIT_TASK,
            );
            return {...runtime, canEditTask};
        },
        {
            initialProps: {instanceId},
            wrapper: ({children}: PropsWithChildren) => <Provider store={store}>{children}</Provider>,
        },
    );

    return {...hook, store};
}

describe('useProcessRuntimeData', () => {
    beforeEach(() => {
        vi.spyOn(ProcessInstanceApiService.prototype, 'retrieve').mockImplementation(async (id) => ({
            ...new ProcessInstanceApiService().initialize(),
            id,
        }));
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'listAll').mockResolvedValue(emptyPage());
        vi.spyOn(ProcessInstanceAttachmentApiService.prototype, 'listAll').mockResolvedValue(emptyPage());
        vi.spyOn(ProcessInstanceAttachmentSetApiService.prototype, 'listAll').mockResolvedValue(emptyPage());
    });
    afterEach(() => vi.restoreAllMocks());

    it('waits for fresh permissions before exposing an instance created after they were loaded', async () => {
        let resolvePermissions!: (value: PermissionSet) => void;
        const refresh = vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockReturnValue(
            new Promise((resolve) => {
                resolvePermissions = resolve;
            }),
        );
        const {result} = renderRuntimeData(18, permissions([17]));

        await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
        expect(result.current.runtimeData).toBeNull();

        await act(async () => resolvePermissions(permissions([17, 18])));
        await waitFor(() => expect(result.current.runtimeData?.instance.id).toBe(18));
        expect(result.current.canEditTask).toBe(true);
    });

    it('refreshes the permissions when switching instances and reloading the runtime data', async () => {
        const refresh = vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet')
            .mockResolvedValueOnce(permissions([17]))
            .mockResolvedValue(permissions([17, 18]));
        const {result, rerender} = renderRuntimeData(17, permissions([17]));
        await waitFor(() => expect(result.current.runtimeData?.instance.id).toBe(17));

        rerender({instanceId: 18});
        await waitFor(() => expect(result.current.runtimeData?.instance.id).toBe(18));
        expect(result.current.canEditTask).toBe(true);
        expect(refresh).toHaveBeenCalledTimes(2);

        await act(async () => result.current.loadRuntimeData());
        expect(refresh).toHaveBeenCalledTimes(3);
    });

    it.each([
        ['a grant for another instance only', permissions([17]), false],
        ['a system-level grant', permissions([], true), true],
    ])('evaluates the refreshed permissions with %s', async (_, refreshedPermissions, expected) => {
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockResolvedValue(refreshedPermissions);
        const {result} = renderRuntimeData(18, permissions([18]));

        await waitFor(() => expect(result.current.runtimeData?.instance.id).toBe(18));
        expect(result.current.canEditTask).toBe(expected);
    });

    it('does not show the runtime data with stale permissions when the refresh fails', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.spyOn(PermissionApiService.prototype, 'getOwnPermissionSet').mockRejectedValue(new Error('Unavailable'));
        const {result, store} = renderRuntimeData(18, permissions([18]));

        await waitFor(() => expect(result.current.isRefreshingRuntimeData).toBe(false));
        expect(result.current.runtimeData).toBeNull();
        expect(store.getState().shell.snackbars).toContainEqual(
            expect.objectContaining({message: 'Der Vorgang konnte nicht geladen werden.'}),
        );
    });
});
