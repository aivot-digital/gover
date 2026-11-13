import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../../data/permissions/permission';
import {type ProcessEntity} from '../../entities/process-entity';
import {type ProcessVersionEntity} from '../../entities/process-version-entity';
import {ProcessSettingsDialog} from './process-settings-dialog';

const mocks = vi.hoisted(() => ({
    permissions: new Set<string>(),
    managingDepartmentPermissions: new Set<string>(),
    generalProps: undefined as Record<string, unknown> | undefined,
    accessProps: undefined as Record<string, unknown> | undefined,
    presetProps: undefined as Record<string, unknown> | undefined,
}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: (_id: number, permission: string) => mocks.permissions.has(permission),
    useHasDepartmentPermission: (id: number, permission: string) => id === 3 && mocks.managingDepartmentPermissions.has(permission),
}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => ({})}));
vi.mock('../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../departments/services/v-department-shadowed-api-service', () => ({
    VDepartmentShadowedApiService: class {
        listAll = () => Promise.resolve({content: []});
    },
}));
vi.mock('../../../teams/services/teams-api-service', () => ({
    TeamsApiService: class {
        listAll = () => Promise.resolve({content: []});
    },
}));
vi.mock('../../../themes/themes-api-service', () => ({
    ThemesApiService: class {
        listAll = () => Promise.resolve({content: []});
    },
}));
vi.mock('./process-settings-dialog-general-tab', () => ({
    ProcessSettingsDialogGeneralTab: (props: Record<string, unknown>) => {
        mocks.generalProps = props;
        return null;
    },
}));
vi.mock('./process-settings-dialog-process-access-tab', () => ({
    ProcessSettingsDialogProcessAccessTab: (props: Record<string, unknown>) => {
        mocks.accessProps = props;
        return null;
    },
}));
vi.mock('./process-settings-dialog-process-instance-access-preset-tab', () => ({
    ProcessSettingsDialogProcessInstanceAccessPresetTab: (props: Record<string, unknown>) => {
        mocks.presetProps = props;
        return null;
    },
}));

function renderDialog() {
    render(
        <ProcessSettingsDialog
            open={true}
            onClose={vi.fn()}
            process={{id: 7, departmentId: 3} as ProcessEntity}
            version={{processVersion: 1} as ProcessVersionEntity}
            onProcessChange={vi.fn()}
            onVersionChange={vi.fn()}
            onDeleteProcess={vi.fn()}
        />,
    );
}

describe('ProcessSettingsDialog', () => {
    beforeEach(() => {
        mocks.permissions = new Set();
        mocks.managingDepartmentPermissions = new Set();
        mocks.generalProps = undefined;
        mocks.accessProps = undefined;
        mocks.presetProps = undefined;
    });

    it('is read-only and blocks the access tabs without the update permission', () => {
        renderDialog();

        expect(screen.getByRole('alert')).toHaveTextContent('Prozess bearbeiten');
        expect(screen.getByRole('tab', {name: 'Prozessberechtigungen'})).toBeDisabled();
        expect(screen.getByRole('tab', {name: 'Standardrechte für neue Vorgänge'})).toBeDisabled();
        expect(screen.getByRole('tab', {name: 'Allgemeine Einstellungen'})).toBeEnabled();
        expect(mocks.generalProps).toMatchObject({readOnly: true, canDeleteProcess: false});
    });

    it('is editable with the update permission and keeps delete separate', () => {
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderDialog();

        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Prozessberechtigungen'})).toBeEnabled();
        expect(mocks.generalProps).toMatchObject({readOnly: false, canDeleteProcess: false});
    });

    it('keeps process access and moving read-only for delegates but lets them manage Vorgang defaults', async () => {
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderDialog();

        expect(mocks.generalProps).toMatchObject({readOnly: false, canMoveProcess: false});

        await userEvent.click(screen.getByRole('tab', {name: 'Prozessberechtigungen'}));
        expect(mocks.accessProps).toMatchObject({readOnly: true});

        await userEvent.click(screen.getByRole('tab', {name: 'Standardrechte für neue Vorgänge'}));
        expect(mocks.presetProps).toBeDefined();
    });

    it('lets the managing department change process access and move the process', async () => {
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        mocks.managingDepartmentPermissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderDialog();

        expect(mocks.generalProps).toMatchObject({canMoveProcess: true});

        await userEvent.click(screen.getByRole('tab', {name: 'Prozessberechtigungen'}));
        expect(mocks.accessProps).toMatchObject({readOnly: false});
    });
});
