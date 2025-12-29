import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {useCallback, useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {UsersApiService} from '../../users/users-api-service';
import {User} from '../../users/models/user';
import {TeamsApiService} from '../../teams/services/teams-api-service';
import {UserRolesApiService} from '../user-roles-api-service';
import {UserRoleResponseDTO} from '../dtos/user-role-response-dto';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {DepartmentEntity} from '../../departments/entities/department-entity';
import {DepartmentApiService} from '../../departments/services/department-api-service';
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar} from "../../../slices/snackbar-slice";
import {TeamEntity} from "../../teams/entities/team-entity";
import {
    VDepartmentMembershipWithDetailsService
} from "../../departments/services/v-department-membership-with-details-service";
import {VTeamMembershipWithDetailsApiService} from "../../teams/services/v-team-membership-with-details-api-service";
import {
    VDepartmentMembershipWithDetailsEntity
} from "../../departments/entities/v-department-membership-with-details-entity";
import {VTeamMembershipWithDetailsEntity} from "../../teams/entities/v-team-membership-with-details-entity";

interface UserRolesAssignmentDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => void;
    userId?: string;
    parentId?: number;
    parentType: 'orgUnit' | 'team';
}

type ParentType<T extends 'orgUnit' | 'team'> =
    T extends 'orgUnit' ?
        DepartmentEntity :
        TeamEntity;

type MembershipType<T extends 'orgUnit' | 'team'> =
    T extends 'orgUnit' ?
        VDepartmentMembershipWithDetailsEntity :
        VTeamMembershipWithDetailsEntity;

export function UserRolesAssignmentDialog(props: UserRolesAssignmentDialogProps) {
    const dispatch = useAppDispatch();

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

    const [activeRoleIds, setActiveRoleIds] = useState<Set<number>>();

    // Load all available roles
    useEffect(() => {
        new UserRolesApiService()
            .listAll()
            .then((rolesPage) => setRoles(rolesPage.content))
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Rollen konnten nicht geladen werden'))
            });
    }, []);

    // Load user details
    useEffect(() => {
        if (userId == null) {
            setUser(undefined);
            return;
        }

        new UsersApiService()
            .retrieve(userId)
            .then(setUser)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Benutzer konnte nicht geladen werden'))
            });
    }, [userId]);

    // Load parent details
    useEffect(() => {
        if (parentId == null) {
            setParent(undefined);
            return;
        }

        if (parentType === 'orgUnit') {
            new DepartmentApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Organisationseinheit konnte nicht geladen werden'));
                });
        } else {
            new TeamsApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Team konnte nicht geladen werden'));
                })
        }
    }, [parentId, parentType]);

    // Load assignments
    useEffect(() => {
        if (userId == null) {
            setMemberships(undefined);
            return;
        }

        if (parentType === 'orgUnit') {
            new VDepartmentMembershipWithDetailsService()
                .listAll({
                    departmentId: parentId,
                    userId: userId,
                })
                .then((membershipsPage) => {
                    setMemberships(membershipsPage.content);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'))
                });
        } else {
            new VTeamMembershipWithDetailsApiService()
                .listAll({
                    teamId: parentId,
                    userId: userId,
                })
                .then((membershipsPage) => {
                    setMemberships(membershipsPage.content);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'))
                });
        }
    }, [userId, parentId, parentType]);

    // Determine active role IDs
    useEffect(() => {
        if (memberships == null) {
            setActiveRoleIds(undefined);
            return;
        }

        const activeRoleIdsSet = new Set<number>();

        memberships
            .flatMap((mem) => mem.domainRoles)
            .forEach((role) => activeRoleIdsSet.add(role.id));

        setActiveRoleIds(activeRoleIdsSet);
    }, [memberships]);

    const handleSave = useCallback(() => {
        if (memberships == null) {
            return;
        }

        const roleIdsToAdd: number[] = [];
        const userRoleAssignmentIdsToRemove: number[] = [];

        if (roles != null && activeRoleIds != null) {
            roles.forEach((role) => {
                const isActive = activeRoleIds.has(role.id);

                const isCurrentlyAssigned = memberships
                    .flatMap((m) => m.domainRoles)
                    .some(r => r.id === role.id);

                if (isActive && !isCurrentlyAssigned) {
                    roleIdsToAdd.push(role.id);
                } else if (!isActive && isCurrentlyAssigned) {
                    const membership = memberships?.find((m) => m.domainRoles.some(r => r.id === role.id));
                    if (membership != null) {
                        userRoleAssignmentIdsToRemove.push(membership.membershipId!);
                    }
                }
            });
        }

        onSave(roleIdsToAdd, userRoleAssignmentIdsToRemove);
    }, [onSave, roles, activeRoleIds, memberships]);

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
                            value={activeRoleIds?.has(role.id) ?? false}
                            onChange={(val) => {
                                const newSet = new Set(activeRoleIds);
                                if (val) {
                                    newSet.add(role.id);
                                } else {
                                    newSet.delete(role.id);
                                }

                                setActiveRoleIds(newSet);
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