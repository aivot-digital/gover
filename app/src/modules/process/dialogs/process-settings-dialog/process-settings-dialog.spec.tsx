import {act, render, screen} from '@testing-library/react';
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
vi.mock('./process-settings-dialog-version-tab', () => ({
    ProcessSettingsDialogVersionTab: () => null,
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

        // Keyboard and screen-reader users cannot reach the hover tooltips, so the alert must explain the restriction.
        expect(screen.getByRole('alert')).toHaveTextContent('Zum Bearbeiten benötigen Sie die Berechtigung „Prozess bearbeiten“.');
        expect(screen.getByRole('tab', {name: 'Prozessberechtigungen'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('tab', {name: 'Standardrechte für neue Vorgänge'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('tab', {name: 'Allgemeine Einstellungen'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();
        expect(mocks.generalProps).toMatchObject({readOnly: true, canDeleteProcess: false});
    });

    it.each(['Prozessberechtigungen', 'Standardrechte für neue Vorgänge'])(
        'explains the missing permission on the blocked %s tab and prevents activation', async (label) => {
            const user = userEvent.setup();
            renderDialog();

            const tab = screen.getByRole('tab', {name: label});
            expect(tab).toHaveAttribute('tabindex', '-1');
            await user.hover(screen.getByText(label));
            expect(await screen.findByRole('tooltip')).toHaveTextContent('process_definition.update');
            await user.click(tab);

            expect(screen.getByRole('tab', {name: 'Allgemeine Einstellungen'})).toHaveAttribute('aria-selected', 'true');
            expect(mocks.accessProps).toBeUndefined();
            expect(mocks.presetProps).toBeUndefined();
        },
    );

    it.each(['Allgemeine Einstellungen', 'Versionsspezifische Einstellungen'])(
        'explains the missing update permission on the save button in %s', async (label) => {
            const user = userEvent.setup();
            renderDialog();
            await user.click(screen.getByRole('tab', {name: label}));

            const save = screen.getByRole('button', {name: 'Speichern'});
            expect(save).toBeDisabled();
            await user.hover(save.parentElement!);
            expect(await screen.findByRole('tooltip')).toHaveTextContent('process_definition.update');
        },
    );

    it('keeps saving disabled without permission even if changes are reported', () => {
        renderDialog();
        act(() => (mocks.generalProps?.onUnsavedChangesChange as (changed: boolean) => void)(true));
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();
    });

    it('does not report a missing permission when saving is disabled only because nothing changed', async () => {
        const user = userEvent.setup();
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderDialog();

        const save = screen.getByRole('button', {name: 'Speichern'});
        expect(save).toBeDisabled();
        await user.hover(save.parentElement!);
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();

        act(() => (mocks.generalProps?.onUnsavedChangesChange as (changed: boolean) => void)(true));
        expect(save).toBeEnabled();
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
        const save = screen.getByRole('button', {name: 'Speichern'});
        expect(save).toBeDisabled();
        await userEvent.hover(save.parentElement!);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Zum Ändern der Prozessberechtigungen benötigen Sie die Berechtigung „Prozess bearbeiten“ (process_definition.update) in der verwaltenden Organisationseinheit des Prozesses oder systemweit.');
        // Delegates hold the permission for this process, so the tooltip must not claim it is missing.
        expect(tooltip).not.toHaveTextContent('Sie besitzen nicht die erforderliche Berechtigung');
        await userEvent.unhover(save.parentElement!);

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
