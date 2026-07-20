import {
    Box,
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
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import Deselect from '@aivot/mui-material-symbols-400-n25-outlined/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-n25-outlined/SelectAll';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {AlertComponent} from '../../../components/alert/alert-component';
import {UserRolesApiService} from '../user-roles-api-service';
import {UserRoleResponseDTO} from '../dtos/user-role-response-dto';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {
    VDepartmentMembershipWithDetailsService,
} from '../../departments/services/v-department-membership-with-details-service';
import {VTeamMembershipWithDetailsApiService} from '../../teams/services/v-team-membership-with-details-api-service';
import {
    VDepartmentMembershipWithDetailsEntity,
} from '../../departments/entities/v-department-membership-with-details-entity';
import {VTeamMembershipWithDetailsEntity} from '../../teams/entities/v-team-membership-with-details-entity';
import {useCheckSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {isApiError} from '../../../models/api-error';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';

interface UserRolesAssignmentDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => void;
    userId?: string;
    userLabel?: string;
    parentId?: number;
    parentLabel?: string;
    parentType: 'orgUnit' | 'team';
}

type MembershipType<T extends 'orgUnit' | 'team'> =
    T extends 'orgUnit' ?
        VDepartmentMembershipWithDetailsEntity :
        VTeamMembershipWithDetailsEntity;

export function UserRolesAssignmentDialog(props: UserRolesAssignmentDialogProps) {
    const dispatch = useAppDispatch();
    const canReadDomainRoles = useCheckSystemPermission(Permission.DOMAIN_ROLE_READ);

    const {
        open,
        onClose,
        onSave,
        userId,
        userLabel,
        parentId,
        parentLabel,
        parentType,
    } = props;

    const [roles, setRoles] = useState<UserRoleResponseDTO[]>();
    const [memberships, setMemberships] = useState<MembershipType<typeof parentType>[]>();
    const [rolesAccessDenied, setRolesAccessDenied] = useState(false);

    const [activeRoleIds, setActiveRoleIds] = useState<Set<number>>();
    // Callers clear their selected row as soon as the dialog closes; keep the last context visible during the close transition.
    const renderUserId = useRetainedDialogValue(open, userId);
    const renderUserLabel = useRetainedDialogValue(open, userLabel);
    const renderParentId = useRetainedDialogValue(open, parentId);
    const renderParentLabel = useRetainedDialogValue(open, parentLabel);
    const renderParentType = useRetainedDialogValue(open, parentType);

    useEffect(() => {
        if (!open) {
            return;
        }

        let isActive = true;

        setRoles(undefined);
        setRolesAccessDenied(false);

        if (!canReadDomainRoles) {
            setRoles([]);
            setRolesAccessDenied(true);
            return undefined;
        }

        // The dialog is mounted while closed on membership pages; scope permission-protected loads to the open state.
        new UserRolesApiService()
            .listAll()
            .then((rolesPage) => {
                if (isActive) {
                    setRoles(rolesPage.content);
                }
            })
            .catch((err) => {
                if (!isActive) {
                    return;
                }

                setRoles([]);
                if (isApiError(err) && err.status === 403) {
                    setRolesAccessDenied(true);
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Rollen konnten nicht geladen werden'));
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadDomainRoles, dispatch, open]);

    // Load assignments
    useEffect(() => {
        if (!open) {
            return;
        }

        if (renderUserId == null || renderParentId == null) {
            setMemberships(undefined);
            return undefined;
        }

        let isActive = true;

        setMemberships(undefined);

        if (renderParentType === 'orgUnit') {
            new VDepartmentMembershipWithDetailsService()
                .listAll({
                    departmentId: renderParentId,
                    userId: renderUserId,
                })
                .then((membershipsPage) => {
                    if (isActive) {
                        setMemberships(membershipsPage.content);
                    }
                })
                .catch((err) => {
                    if (isActive) {
                        dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'));
                    }
                });
        } else {
            new VTeamMembershipWithDetailsApiService()
                .listAll({
                    teamId: renderParentId,
                    userId: renderUserId,
                })
                .then((membershipsPage) => {
                    if (isActive) {
                        setMemberships(membershipsPage.content);
                    }
                })
                .catch((err) => {
                    if (isActive) {
                        dispatch(showApiErrorSnackbar(err, 'Rollen-Zuweisungen konnten nicht geladen werden'));
                    }
                });
        }

        return () => {
            isActive = false;
        };
    }, [dispatch, open, renderParentId, renderParentType, renderUserId]);

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
    const isNewMembership = memberships != null && memberships.length === 0;

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
    };

    const resetDialogState = () => {
        setMemberships(undefined);
        setActiveRoleIds(undefined);
        setRoles(undefined);
        setRolesAccessDenied(false);
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="md"
            TransitionProps={{
                onExited: resetDialogState,
            }}
        >
            <DialogTitleWithClose onClose={handleClose}>
                Rollen zuweisen
            </DialogTitleWithClose>

            <DialogContent>
                <Stack spacing={2}>
                    <Box>
                        <Typography variant="subtitle1">
                            {renderUserLabel ?? 'Benutzer:in'}
                        </Typography>
                        <Typography
                            variant="body2"
                            color="text.secondary"
                        >
                            {renderParentType === 'orgUnit' ? 'Organisationseinheit' : 'Team'}:{' '}
                            {renderParentLabel ?? 'wird geladen...'}
                        </Typography>
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

                    {rolesAccessDenied && (
                        <AlertComponent color="info">
                            Keine Berechtigung zur Einsicht von Rollen.
                            Für die Rollenauswahl ist die Berechtigung domain_role.read erforderlich.
                        </AlertComponent>
                    )}

                    {!rolesAccessDenied && !isLoading && selectedCount === 0 && (
                        <AlertComponent color="warning">
                            Es wurde keine Domänenrolle ausgewählt.
                            Die Mitarbeiter:in wird ohne Domänenrolle gespeichert und hat dadurch in dieser Domäne
                            möglicherweise keine zusätzlichen Berechtigungen.
                        </AlertComponent>
                    )}

                    {!rolesAccessDenied && !isLoading && sortedRoles.length === 0 && (
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
                    disabled={isLoading || rolesAccessDenied || (!isNewMembership && !changes.hasChanges)}
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
