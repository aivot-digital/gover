import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {useCallback, useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {UsersApiService} from '../../users/users-api-service';
import {User} from '../../users/models/user';
import {TeamResponseDTO} from '../../teams/dtos/team-response-dto';
import {TeamsApiService} from '../../teams/teams-api-service';
import {UserRolesApiService} from '../user-roles-api-service';
import {UserRoleResponseDTO} from '../dtos/user-role-response-dto';
import {OrgUserRoleAssignmentsApiService} from '../org-user-role-assignments-api-service';
import {OrgUserRoleAssignmentResponseDTO} from '../dtos/org-user-role-assignment-response-dto';
import {TeamUserRoleAssignmentResponseDTO} from '../dtos/team-user-role-assignment-response-dto';
import {TeamUserRoleAssignmentsApiService} from '../team-user-role-assignments-api-service';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {DepartmentEntity} from '../../departments/entities/department-entity';
import {DepartmentApiService} from '../../departments/services/department-api-service';

interface UserRolesAssignmentDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => void;
    userId?: string;
    parentId: number;
    parentType: 'orgUnit' | 'team';
}

type ParentType<T extends 'orgUnit' | 'team'> =
    T extends 'orgUnit' ?
        DepartmentEntity :
        TeamResponseDTO;

type MembershipType<T extends 'orgUnit' | 'team'> =
    T extends 'orgUnit' ?
        OrgUserRoleAssignmentResponseDTO :
        TeamUserRoleAssignmentResponseDTO;

export function UserRolesAssignmentDialog(props: UserRolesAssignmentDialogProps) {
    const {
        open,
        onClose,
        onSave,
        userId,
        parentId,
        parentType,
    } = props;

    const [roles, setRoles] = useState<UserRoleResponseDTO[]>();
    const [user, setUser] = useState<User>();
    const [parent, setParent] = useState<ParentType<typeof parentType>>();
    const [memberships, setMemberships] = useState<MembershipType<typeof parentType>[]>();

    const [activeRoles, setActiveRoles] = useState<Record<number, boolean>>();

    useEffect(() => {
        new UserRolesApiService()
            .listAll()
            .then((rolesPage) => setRoles(rolesPage.content))
            .catch(console.error); // TODO: Dispatch Error Snackbar
    }, []);

    useEffect(() => {
        if (userId == null) {
            setUser(undefined);
            return;
        }

        new UsersApiService()
            .retrieve(userId)
            .then(setUser)
            .catch(console.error); // TODO: Dispatch Error Snackbar
    }, [userId]);

    useEffect(() => {
        if (userId == null) {
            setMemberships(undefined);
            return;
        }

        if (parentType === 'orgUnit') {
            new OrgUserRoleAssignmentsApiService()
                .listAll({
                    orgUnitMembershipOrganizationalUnitId: parentId,
                    orgUnitMembershipUserId: userId,
                })
                .then((membershipsPage) => {
                    setMemberships(membershipsPage.content);
                })
                .catch(console.error); // TODO: Dispatch Error Snackbar
        } else {
            new TeamUserRoleAssignmentsApiService()
                .listAll({
                    teamMembershipTeamId: parentId,
                    teamMembershipUserId: userId,
                })
                .then((membershipsPage) => {
                    setMemberships(membershipsPage.content);
                })
                .catch(console.error); // TODO: Dispatch Error Snackbar
        }
    }, [userId, parentId, parentType]);

    useEffect(() => {
        if (memberships == null) {
            setActiveRoles(undefined);
            return;
        }

        setActiveRoles(memberships.reduce((acc, membership) => ({
            ...acc,
            [membership.userRoleId]: true,
        }), {} as Record<number, boolean>));
    }, [memberships]);

    useEffect(() => {
        if (parentType === 'orgUnit') {
            new DepartmentApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch(console.error); // TODO: Dispatch Error Snackbar
        } else {
            new TeamsApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch(console.error); // TODO: Dispatch Error Snackbar
        }
    }, [parentId, parentType]);

    const handleSave = useCallback(() => {
        const roleIdsToAdd: number[] = [];
        const userRoleAssignmentIdsToRemove: number[] = [];

        if (roles != null && activeRoles != null) {
            roles.forEach((role) => {
                const isActive = activeRoles[role.id] ?? false;
                const isCurrentlyAssigned = memberships?.some((membership) => membership.userRoleId === role.id) ?? false;

                if (isActive && !isCurrentlyAssigned) {
                    roleIdsToAdd.push(role.id);
                } else if (!isActive && isCurrentlyAssigned) {
                    const membership = memberships?.find((m) => m.userRoleId === role.id);
                    if (membership != null) {
                        userRoleAssignmentIdsToRemove.push(membership.userRoleAssignmentId);
                    }
                }
            });
        }

        onSave(roleIdsToAdd, userRoleAssignmentIdsToRemove);
    }, [onSave, roles, activeRoles, memberships]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={onClose}>
                Rollen von {user?.fullName} in {parent?.name}
            </DialogTitleWithClose>

            <DialogContent>
                {
                    roles != null &&
                    roles.map((role) => (
                        <CheckboxFieldComponent
                            key={role.id}
                            label={role.name ?? ''}
                            hint={role.description ?? ''}
                            value={activeRoles?.[role.id] ?? false}
                            onChange={(val) => {
                                setActiveRoles({
                                    ...activeRoles,
                                    [role.id]: val,
                                });
                            }}
                        />
                    ))
                }
            </DialogContent>

            <DialogActions>
                <Button
                    variant="contained"
                    onClick={handleSave}
                >
                    Speichern
                </Button>
                <Button
                    variant="contained"
                    onClick={onClose}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}