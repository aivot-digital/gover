import {act, createRef} from 'react';
import {render, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {type ProcessEntity} from '../../entities/process-entity';
import {type ProcessAccessControlEntity} from '../../entities/process-access-control-entity';
import {
    ProcessSettingsDialogProcessAccessTab,
    type ProcessSettingsDialogProcessAccessTabHandle,
} from './process-settings-dialog-process-access-tab';

const mocks = vi.hoisted(() => ({
    refreshPermissionSet: vi.fn(),
    create: vi.fn(),
    matrixProps: undefined as Record<string, any> | undefined,
}));

vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useRefreshPermissionSet: () => mocks.refreshPermissionSet,
}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../permissions/permission-api-service', () => ({
    PermissionApiService: class {
        listPermissions = () => Promise.resolve([]);
    },
}));
vi.mock('../../services/process-access-control-api-service', async (importOriginal) => {
    const original = await importOriginal<typeof import('../../services/process-access-control-api-service')>();
    return {
        ProcessAccessControlApiService: class {
            static initialize = original.ProcessAccessControlApiService.initialize;
            listAll = () => Promise.resolve({content: []});
            create = mocks.create;
        },
    };
});
vi.mock('./process-settings-dialog-access-control-matrix', () => ({
    ProcessSettingsDialogAccessControlMatrix: (props: Record<string, any>) => {
        mocks.matrixProps = props;
        return null;
    },
}));

async function renderAndAddTeamAccess() {
    const ref = createRef<ProcessSettingsDialogProcessAccessTabHandle>();
    render(
        <ProcessSettingsDialogProcessAccessTab
            ref={ref}
            open={true}
            readOnly={false}
            process={{id: 7, departmentId: 3} as ProcessEntity}
            departments={[]}
            teams={[]}
        />,
    );

    await waitFor(() => expect(mocks.matrixProps).toBeDefined());
    act(() => {
        mocks.matrixProps!.onAddAccessControl({type: 'team', value: 5, label: 'Team', icon: null});
    });

    return ref;
}

describe('ProcessSettingsDialogProcessAccessTab', () => {
    beforeEach(() => {
        mocks.refreshPermissionSet.mockReset().mockResolvedValue(undefined);
        mocks.create.mockReset();
        mocks.matrixProps = undefined;
    });

    it('refreshes and broadcasts the permission set after saving access rules', async () => {
        mocks.create.mockImplementation((access: ProcessAccessControlEntity) => Promise.resolve({...access, id: 11}));
        const ref = await renderAndAddTeamAccess();

        act(() => ref.current!.save());

        await waitFor(() => expect(mocks.refreshPermissionSet).toHaveBeenCalledWith({broadcast: true}));
        expect(mocks.create).toHaveBeenCalledWith(expect.objectContaining({sourceTeamId: 5, targetProcessId: 7}));
    });

    it('refreshes the permission set even when saving fails part-way', async () => {
        mocks.create.mockRejectedValue(new Error('failed'));
        const ref = await renderAndAddTeamAccess();

        act(() => ref.current!.save());

        await waitFor(() => expect(mocks.refreshPermissionSet).toHaveBeenCalledWith({broadcast: true}));
    });
});
