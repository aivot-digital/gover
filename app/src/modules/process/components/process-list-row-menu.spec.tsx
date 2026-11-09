import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {type ProcessEntity} from '../entities/process-entity';
import {ProcessListRowMenu} from './process-list-row-menu';

const mocks = vi.hoisted(() => ({
    permissions: new Set<string>(),
    managingDepartmentPermissions: new Set<string>(),
    checkedProcessIds: new Set<number | undefined>(),
}));
vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: (id: number | undefined, permission: string) => {
        mocks.checkedProcessIds.add(id);
        return mocks.permissions.has(permission);
    },
    useHasDepartmentPermission: (id: number | undefined, permission: string) => id === 3 && mocks.managingDepartmentPermissions.has(permission),
    useHasAnyDepartmentPermission: (permission: string) => mocks.permissions.has(permission),
}));

const process = {id: 7, departmentId: 3} as ProcessEntity;

function renderMenu() {
    const onMove = vi.fn();
    const onDelete = vi.fn();
    render(
        <ProcessListRowMenu
            anchorEl={document.body}
            onClose={vi.fn()}
            process={process}
            onMoveProcessToDepartment={onMove}
            onDeleteProcess={onDelete}
        />,
    );
    return {onMove, onDelete};
}

const moveItem = () => screen.getByRole('menuitem', {name: 'Prozess an Organisationseinheit übertragen'});
const deleteItem = () => screen.getByRole('menuitem', {name: 'Prozess löschen'});

describe('ProcessListRowMenu', () => {
    beforeEach(() => {
        mocks.permissions = new Set();
        mocks.managingDepartmentPermissions = new Set();
        mocks.checkedProcessIds = new Set();
    });

    it('enables each action with its permissions for the row process', async () => {
        mocks.permissions = new Set([
            Permission.PROCESS_DEFINITION_CREATE,
            Permission.PROCESS_DEFINITION_DELETE,
        ]);
        mocks.managingDepartmentPermissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        const {onMove, onDelete} = renderMenu();

        await userEvent.click(moveItem());
        await userEvent.click(deleteItem());

        expect(onMove).toHaveBeenCalledWith(process);
        expect(onDelete).toHaveBeenCalledWith(process);
        expect(mocks.checkedProcessIds).toEqual(new Set([7]));
    });

    it('disables move and delete for read-only users and explains why', async () => {
        renderMenu();

        expect(moveItem()).toHaveAttribute('aria-disabled', 'true');
        expect(deleteItem()).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(deleteItem().parentElement as HTMLElement);
        expect(await screen.findByRole('tooltip')).toHaveTextContent(Permission.PROCESS_DEFINITION_DELETE);
    });

    it('keeps move disabled without create permission in any department', () => {
        mocks.managingDepartmentPermissions = new Set([Permission.PROCESS_DEFINITION_UPDATE]);
        renderMenu();

        expect(moveItem()).toHaveAttribute('aria-disabled', 'true');
    });

    it('does not let a delegate with process update take over the process', async () => {
        mocks.permissions = new Set([Permission.PROCESS_DEFINITION_UPDATE, Permission.PROCESS_DEFINITION_CREATE]);
        renderMenu();

        expect(moveItem()).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(moveItem().parentElement as HTMLElement);
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Nur die verwaltende Organisationseinheit');
    });
});
