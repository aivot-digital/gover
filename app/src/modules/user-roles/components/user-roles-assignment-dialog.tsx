import {
    Box,
    Breadcrumbs,
    Button,
    Checkbox,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    Divider,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Stack,
    Typography,
} from '@mui/material';
import {useCallback, useEffect, useMemo, useState} from 'react';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import Deselect from '@aivot/mui-material-symbols-400-outlined/dist/deselect/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-outlined/dist/select-all/SelectAll';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {UsersApiService} from '../../users/users-api-service';
import {User} from '../../users/models/user';
import {TeamsApiService} from '../../teams/services/teams-api-service';
import {UserRolesApiService} from '../user-roles-api-service';
import {UserRoleResponseDTO} from '../dtos/user-role-response-dto';
import {DepartmentEntity} from '../../departments/entities/department-entity';
import {DepartmentApiService} from '../../departments/services/department-api-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {TeamEntity} from '../../teams/entities/team-entity';
import {
    VDepartmentMembershipWithDetailsService,
} from '../../departments/services/v-department-membership-with-details-service';
import {VTeamMembershipWithDetailsApiService} from '../../teams/services/v-team-membership-with-details-api-service';
import {
    VDepartmentMembershipWithDetailsEntity,
} from '../../departments/entities/v-department-membership-with-details-entity';
import {VTeamMembershipWithDetailsEntity} from '../../teams/entities/v-team-membership-with-details-entity';

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
    const [orgUnitPathParts, setOrgUnitPathParts] = useState<string[]>();

    const [activeRoleIds, setActiveRoleIds] = useState<Set<number>>();

    // Load all available roles
    useEffect(() => {
        new UserRolesApiService()
            .listAll()
            .then((rolesPage) => setRoles(rolesPage.content))
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Rollen konnten nicht geladen werden'));
            });
    }, [dispatch]);

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
                dispatch(showApiErrorSnackbar(err, 'Benutzer konnte nicht geladen werden'));
            });
    }, [dispatch, userId]);

    // Load parent details
    useEffect(() => {
        if (parentId == null) {
            setParent(undefined);
            setOrgUnitPathParts(undefined);
            return;
        }

        if (parentType === 'orgUnit') {
            new DepartmentApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Organisationseinheit konnte nicht geladen werden'));
                });

            new VDepartmentShadowedApiService()
                .retrieve(parentId)
                .then((shadowedDepartment) => {
                    const path = [
                        ...(shadowedDepartment.parentNames ?? []),
                        shadowedDepartment.name,
                    ].filter(Boolean);

                    setOrgUnitPathParts(path);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Pfad der Organisationseinheit konnte nicht geladen werden'));
                });
        } else {
            setOrgUnitPathParts(undefined);
            new TeamsApiService()
                .retrieve(parentId)
                .then(setParent)
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Team konnte nicht geladen werden'));
                });
        }
    }, [dispatch, parentId, parentType]);

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
                    dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'));
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
                    dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'));
                });
        }
    }, [dispatch, userId, parentId, parentType]);

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

    const assignedRoleIds = useMemo(() => {
        return new Set(
            (memberships ?? []).flatMap((membership) => membership.domainRoles.map((role) => role.id)),
        );
    }, [memberships]);

    const sortedRoles = useMemo(() => {
        return [...(roles ?? [])].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''));
    }, [roles]);

    const selectedCount = activeRoleIds?.size ?? 0;
    const isLoading = roles == null || activeRoleIds == null;
    const totalRolesCount = sortedRoles.length;

    const changes = useMemo(() => {
        if (activeRoleIds == null) {
            return {added: 0, removed: 0, hasChanges: false};
        }

        let added = 0;
        let removed = 0;

        activeRoleIds.forEach((roleId) => {
            if (!assignedRoleIds.has(roleId)) {
                added += 1;
            }
        });

        assignedRoleIds.forEach((roleId) => {
            if (!activeRoleIds.has(roleId)) {
                removed += 1;
            }
        });

        return {
            added,
            removed,
            hasChanges: added > 0 || removed > 0,
        };
    }, [activeRoleIds, assignedRoleIds]);

    const handleToggleRole = (roleId: number, checked: boolean): void => {
        setActiveRoleIds((prev) => {
            const next = new Set(prev ?? []);

            if (checked) {
                next.add(roleId);
            } else {
                next.delete(roleId);
            }

            return next;
        });
    };

    const handleSelectAll = (): void => {
        setActiveRoleIds(new Set(sortedRoles.map((role) => role.id)));
    };

    const handleDeselectAll = (): void => {
        setActiveRoleIds(new Set());
    };

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
                    .some((r) => r.id === role.id);

                if (isActive && !isCurrentlyAssigned) {
                    roleIdsToAdd.push(role.id);
                } else if (!isActive && isCurrentlyAssigned) {
                    const membership = memberships.find((m) => m.domainRoles.some((r) => r.id === role.id));
                    if (membership != null) {
                        const domainRoleAssignment = membership
                            .domainRoleAssignments
                            .find((dra) => dra.domain_role_id === role.id);

                        if (domainRoleAssignment != null) {
                            userRoleAssignmentIdsToRemove.push(domainRoleAssignment.id);
                        }
                    }
                }
            });
        }

        onSave(roleIdsToAdd, userRoleAssignmentIdsToRemove);
    }, [onSave, roles, activeRoleIds, memberships]);

    const handleClose = () => {
        onClose();
        setTimeout(() => {
            setUser(undefined);
            setParent(undefined);
            setMemberships(undefined);
            setActiveRoleIds(undefined);
            setOrgUnitPathParts(undefined);
        }, 300);
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={handleClose}>
                Rollen zuweisen
            </DialogTitleWithClose>

            <DialogContent>
                <Stack spacing={2}>
                    <Box>
                        <Typography variant="subtitle1">
                            {user?.fullName ?? 'Benutzer:in'}
                        </Typography>
                        {parentType === 'orgUnit' ? (
                            <Box>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{mb: 0.25}}
                                >
                                    Organisationseinheit: {parent?.name ?? 'wird geladen...'}
                                </Typography>
                                {(orgUnitPathParts?.length ?? 0) > 0 && (
                                    <Box
                                        sx={{
                                            mt: 0.25,
                                            display: 'inline-flex',
                                            alignItems: 'center',
                                            maxWidth: '100%',
                                            color: 'text.secondary',
                                        }}
                                    >
                                        <Typography
                                            variant="caption"
                                            color="text.secondary"
                                            sx={{
                                                mr: 0.5,
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            ( Pfad:
                                        </Typography>
                                        <Breadcrumbs
                                            separator="›"
                                            maxItems={5}
                                            itemsBeforeCollapse={2}
                                            itemsAfterCollapse={2}
                                            sx={{
                                                transform: 'translateY(-2px)',
                                                color: 'text.secondary',
                                                '& .MuiBreadcrumbs-separator': {
                                                    mx: 0.5,
                                                },
                                                '& .MuiBreadcrumbs-li': {
                                                    minWidth: 0,
                                                },
                                                '& .MuiBreadcrumbs-ol': {
                                                    flexWrap: 'nowrap',
                                                    overflow: 'hidden',
                                                },
                                            }}
                                        >
                                            {orgUnitPathParts?.map((segment, index) => (
                                                <Typography
                                                    key={`${parentId ?? 'org'}-${index}`}
                                                    variant="caption"
                                                    color="text.secondary"
                                                    sx={{
                                                        maxWidth: 180,
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                    }}
                                                    title={segment}
                                                >
                                                    {segment}
                                                </Typography>
                                            ))}
                                        </Breadcrumbs>
                                        <Typography
                                            variant="caption"
                                            color="text.secondary"
                                            sx={{
                                                ml: 0.5,
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            )
                                        </Typography>
                                    </Box>
                                )}
                            </Box>
                        ) : (
                            <Typography
                                variant="body2"
                                color="text.secondary"
                            >
                                Team: {parent?.name ?? 'wird geladen...'}
                            </Typography>
                        )}
                    </Box>

                    <Stack
                        direction={{
                            xs: 'column',
                            sm: 'row',
                        }}
                        spacing={1}
                        alignItems={{
                            xs: 'stretch',
                            sm: 'center',
                        }}
                        justifyContent="space-between"
                    >
                        <Stack
                            direction="row"
                            spacing={1}
                            alignItems="center"
                            flexWrap="wrap"
                            useFlexGap
                        >
                            <Typography
                                variant="body2"
                                color="text.secondary"
                            >
                                {selectedCount} von {totalRolesCount} ausgewählt
                            </Typography>
                            <Chip
                                size="small"
                                label={`+${changes.added}`}
                                variant={changes.added > 0 ? 'filled' : 'outlined'}
                            />
                            <Chip
                                size="small"
                                label={`-${changes.removed}`}
                                variant={changes.removed > 0 ? 'filled' : 'outlined'}
                            />
                        </Stack>

                        <Stack
                            direction="row"
                            spacing={1}
                            sx={{ml: {sm: 'auto'}}}
                        >
                            <Button
                                size="small"
                                variant="outlined"
                                onClick={handleSelectAll}
                                disabled={isLoading || totalRolesCount === 0}
                                startIcon={<SelectAll fontSize="small"/>}
                            >
                                Alle auswählen
                            </Button>
                            <Button
                                size="small"
                                variant="outlined"
                                onClick={handleDeselectAll}
                                disabled={isLoading || selectedCount === 0}
                                startIcon={<Deselect fontSize="small"/>}
                            >
                                Alle abwählen
                            </Button>
                        </Stack>
                    </Stack>

                    {!isLoading && sortedRoles.length === 0 && (
                        <Typography
                            variant="body2"
                            color="text.secondary"
                        >
                            Keine Rollen verfügbar.
                        </Typography>
                    )}

                    <List
                        dense
                        disablePadding
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 1,
                            overflow: 'hidden',
                        }}
                    >
                        {sortedRoles.map((role, index) => {
                            const checked = activeRoleIds?.has(role.id) ?? false;

                            return (
                                <Box key={role.id}>
                                    <ListItemButton
                                        onClick={() => handleToggleRole(role.id, !checked)}
                                        disabled={isLoading}
                                        sx={{py: 1.25}}
                                    >
                                        <ListItemIcon sx={{minWidth: 40}}>
                                            <Checkbox
                                                edge="start"
                                                checked={checked}
                                                disableRipple
                                                onClick={(event) => event.stopPropagation()}
                                                onChange={(event) => handleToggleRole(role.id, event.target.checked)}
                                            />
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={role.name ?? 'Unbenannte Rolle'}
                                            secondary={role.description ?? 'Keine Beschreibung'}
                                        />
                                    </ListItemButton>

                                    {index < sortedRoles.length - 1 && <Divider/>}
                                </Box>
                            );
                        })}
                    </List>
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button
                    variant="contained"
                    onClick={handleSave}
                    disabled={isLoading || !changes.hasChanges || selectedCount === 0}
                    startIcon={<SaveOutlinedIcon/>}
                >
                    Speichern
                </Button>
                <Button
                    variant="outlined"
                    onClick={handleClose}
                    sx={{ml: 'auto'}}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
