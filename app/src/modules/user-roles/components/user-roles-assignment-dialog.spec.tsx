import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {UserRolesAssignmentDialog} from './user-roles-assignment-dialog';
import {UserRolesApiService} from '../user-roles-api-service';
import {VDepartmentMembershipWithDetailsService} from '../../departments/services/v-department-membership-with-details-service';
import {VDepartmentUserRoleAssignmentWithDetailsService} from '../../departments/services/v-department-user-role-assignment-with-details-service';
import {VTeamUserRoleAssignmentWithDetailsApiService} from '../../teams/services/v-team-user-role-assignment-with-details-api-service';

const dispatch = vi.hoisted(() => vi.fn());

// A stable dispatch, as the real hook returns; the dialog reloads its data when it changes.
vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatch,
}));

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));

const roles = [
    {id: 1, name: 'Betrachtung'},
    {id: 2, name: 'Domänenadmin'},
];

function page<T>(content: T[]) {
    return {content} as any;
}

// Roles are clickable once both the roles and the membership of the user have loaded.
async function toggleRole(name: string) {
    const role = (await screen.findByText(name)).closest('[role="button"]') as HTMLElement;
    await waitFor(() => expect(role).not.toHaveClass('Mui-disabled'));
    fireEvent.click(role);
}

describe('UserRolesAssignmentDialog', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        vi.spyOn(UserRolesApiService.prototype, 'listAll').mockResolvedValue(page(roles));
    });

    function renderDialog(onSave: (roleIds: number[], lockVersion: number | null) => void) {
        render(
            <UserRolesAssignmentDialog
                open
                onClose={vi.fn()}
                onSave={onSave}
                userId="user"
                userLabel="Person X"
                parentId={5}
                parentLabel="Bauamt"
                parentType="orgUnit"
            />,
        );
    }

    it('saves the complete role set with the lock version of the loaded membership', async () => {
        vi.spyOn(VDepartmentMembershipWithDetailsService.prototype, 'listAll').mockResolvedValue(page([{
            membershipId: 7,
            lockVersion: 3,
            domainRoles: [{id: 1}],
            domainRoleAssignments: [{id: 70, domain_role_id: 1}],
        }]));
        const onSave = vi.fn();
        renderDialog(onSave);

        await toggleRole('Domänenadmin');
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(onSave).toHaveBeenCalledWith([1, 2], 3));
    });

    it('saves without a lock version for a new member', async () => {
        vi.spyOn(VDepartmentMembershipWithDetailsService.prototype, 'listAll').mockResolvedValue(page([]));
        const onSave = vi.fn();
        renderDialog(onSave);

        await toggleRole('Domänenadmin');
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(onSave).toHaveBeenCalledWith([2], null));
    });
});

describe('membership role set requests', () => {
    it('send the role set with the lock version of the department membership', async () => {
        const service = new VDepartmentUserRoleAssignmentWithDetailsService();
        const put = vi.spyOn(service, 'put').mockResolvedValue({});

        await service.updateRoles(7, 3, [1, 2]);

        expect(put).toHaveBeenCalledWith('/api/department-user-role-assignments-with-details/memberships/7/', {
            lockVersion: 3,
            roleIds: [1, 2],
        });
    });

    it('send the role set with the lock version of the team membership', async () => {
        const service = new VTeamUserRoleAssignmentWithDetailsApiService();
        const put = vi.spyOn(service, 'put').mockResolvedValue({});

        await service.updateRoles(8, 0, []);

        expect(put).toHaveBeenCalledWith('/api/team-user-role-assignments-with-details/memberships/8/', {
            lockVersion: 0,
            roleIds: [],
        });
    });
});
