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
import {
    VDepartmentUserRoleAssignmentWithDetailsService
} from "../../departments/services/v-department-user-role-assignment-with-details-service";
import {
    VDepartmentUserRoleAssignmentWithDetailsEntity
} from "../../departments/entities/v-department-user-role-assignment-with-details-entity";
import {TeamEntity} from "../../teams/entities/team-entity";
import {
    VTeamUserRoleAssignmentWithDetailsApiService
} from "../../teams/services/v-team-user-role-assignment-with-details-api-service";
import {
    VTeamUserRoleAssignmentWithDetailsEntity
} from "../../teams/entities/v-team-user-role-assignment-with-details-entity";

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
        VDepartmentUserRoleAssignmentWithDetailsEntity :
        VTeamUserRoleAssignmentWithDetailsEntity;

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
            new VDepartmentUserRoleAssignmentWithDetailsService()
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
            new VTeamUserRoleAssignmentWithDetailsApiService()
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

    // Determine
    useEffect(() => {
        if (memberships == null) {
            setActiveRoleIds(undefined);
            return;
        }

        console.log(memberships);

        const activeRoleIdsSet = new Set<number>();

        memberships
            .map(mem => mem.userRoleId)
            .filter(id => id != null)
            .forEach(id => activeRoleIdsSet.add(id));

        console.log('Active Role IDs Set:', activeRoleIdsSet);

        setActiveRoleIds(activeRoleIdsSet);
    }, [memberships]);

    const handleSave = useCallback(() => {
        const roleIdsToAdd: number[] = [];
        const userRoleAssignmentIdsToRemove: number[] = [];

        if (roles != null && activeRoleIds != null) {
            roles.forEach((role) => {
                const isActive = activeRoleIds.has(role.id) ?? false;
                const isCurrentlyAssigned = memberships?.some((membership) => membership.userRoleId === role.id) ?? false;

                if (isActive && !isCurrentlyAssigned) {
                    roleIdsToAdd.push(role.id);
                } else if (!isActive && isCurrentlyAssigned) {
                    const membership = memberships?.find((m) => m.userRoleId === role.id);
                    if (membership != null) {
                        userRoleAssignmentIdsToRemove.push(membership.userRoleAssignmentId!);
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