import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {Box, Button, Typography} from '@mui/material';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {SelectUserDialog} from '../../../users/dialogs/select-user-dialog';
import {User} from '../../../users/models/user';
import {GenericListPropsFetchOptions, ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {type GridColDef} from '@mui/x-data-grid';
import {UserRoleChips} from '../../../user-roles/components/user-role-chips';
import {UserStatusChip} from '../../../users/components/user-status-chip';
import {UserRolesAssignmentDialog} from '../../../user-roles/components/user-roles-assignment-dialog';
import {setLoadingMessage} from '../../../../slices/shell-slice';
import {isApiError} from '../../../../models/api-error';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useConfirm} from '../../../../providers/confirm-provider';
import {
    ListTeamMembershipsWithDetailsFilter,
    VTeamMembershipWithDetailsApiService
} from "../../services/v-team-membership-with-details-api-service";
import {VTeamMembershipWithDetailsEntity} from "../../entities/v-team-membership-with-details-entity";
import {TeamEntity} from "../../entities/team-entity";
import {TeamMembershipsApiService} from "../../services/team-memberships-api-service";
import {Page} from "../../../../models/dtos/page";
import {
    VTeamUserRoleAssignmentWithDetailsApiService
} from "../../services/v-team-user-role-assignment-with-details-api-service";
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {
    useCheckSystemPermission,
    useCheckTeamPermission,
    useRefreshPermissionSet,
} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export function TeamsDetailsPageMembers() {
    const dispatch = useAppDispatch();
    const refreshPermissionSet = useRefreshPermissionSet();

    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<TeamEntity, void>;
    const canReadMemberships = useCheckTeamPermission(item?.id, Permission.TEAM_MEMBERSHIP_READ);
    const canCreateMembership = useCheckTeamPermission(item?.id, Permission.TEAM_MEMBERSHIP_CREATE);
    const canUpdateMembership = useCheckTeamPermission(item?.id, Permission.TEAM_MEMBERSHIP_UPDATE);
    const canDeleteMembership = useCheckTeamPermission(item?.id, Permission.TEAM_MEMBERSHIP_DELETE);
    const canReadDomainRoles = useCheckSystemPermission(Permission.DOMAIN_ROLE_READ);

    const showConfirm = useConfirm();

    const listControlRef = useRef<ListControlRef | null>(null);
    const [showSelectNewMemberDialog, setShowSelectNewMemberDialog] = useState(false);
    const [showSelectRolesDialogForUser, setShowSelectRolesDialogForUser] = useState<User | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VTeamMembershipWithDetailsEntity | null>(null);
    const [existingMemberUserIds, setExistingMemberUserIds] = useState<string[]>([]);

    const refreshPermissionsAfterMembershipChange = useCallback(() => {
        // Effective permissions may include grants inherited through deputy assignments.
        // The frontend cannot know whether the changed membership belongs to a represented user.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Teammitgliedschaft nicht aktualisiert werden.',
            )));
    }, [dispatch, refreshPermissionSet]);

    const fetchMembers = useCallback((options: GenericListPropsFetchOptions<VTeamMembershipWithDetailsEntity>) => {
        if (item == null || !canReadMemberships) {
            const p: Page<VTeamMembershipWithDetailsEntity> = {
                content: [],
                page: {
                    number: 0,
                    totalPages: 0,
                    size: 0,
                    totalElements: 0,
                },
            };
            return Promise.resolve(p);
        }

        const filters: ListTeamMembershipsWithDetailsFilter = {
            teamId: item.id,
            userSearch: options.search,
        };

        switch (options.filter) {
            case 'active':
                filters.deletedUser = false;
                filters.enabledUser = true;
                break;
            case 'inactive':
                filters.deletedUser = false;
                filters.enabledUser = false;
                break;
            case 'deleted':
                filters.deletedUser = true;
                filters.enabledUser = undefined;
                break;
        }

        return new VTeamMembershipWithDetailsApiService()
            .listTeamMembershipsWithDetails(0, 999, options.sort as any, options.order, filters);
    }, [canReadMemberships, item]);

    const buildRowActions = useCallback((membershipItem: VTeamMembershipWithDetailsEntity) => {
        return [
            {
                icon: <EditOutlinedIcon />,
                onClick: () => {
                    setShowSelectRolesDialogForMembership(membershipItem);
                },
                tooltip: membershipItem.userDeletedInIdp ? `Kann für gelöschte Mitarbeiter:innen nicht geändert werden` : 'Rolle der Mitarbeiter:in bearbeiten',
                disabled: !canUpdateMembership || !canReadDomainRoles || (membershipItem.userDeletedInIdp ?? false),
                disabledTooltip: !canUpdateMembership
                    ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_UPDATE)
                    : !canReadDomainRoles
                        ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                    : undefined,
            },
            {
                icon: <Delete />,
                onClick: () => {
                    showConfirm({
                        title: 'Mitarbeiter:in entfernen',
                        children: (
                            <>
                                <Typography>
                                    Durch das Entfernen der Mitarbeiter:in <strong>{membershipItem.userFullName}</strong> aus dem Team <strong>{item?.name}</strong> verliert diese alle zugewiesenen Rollen und Berechtigungen in diesem
                                    Team.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Diese Aktion kann nicht rückgängig gemacht werden. Stellen Sie sicher, dass Sie die richtige Mitarbeiter:in entfernen.
                                </Typography>
                            </>
                        ),
                        confirmButtonText: 'Mitarbeiter:in entfernen',
                    })
                        .then((confirmed) => {
                            if (!confirmed) {
                                return;
                            }

                            dispatch(setLoadingMessage({
                                message: `Entferne Mitarbeiter:in ${membershipItem.userFullName} aus dem Team`,
                                blocking: true,
                                estimatedTime: 5000,
                            }));

                            new TeamMembershipsApiService()
                                .destroy(membershipItem.membershipId)
                                .then(() => {
                                    // Refresh list
                                    listControlRef.current?.refresh();
                                    refreshPermissionsAfterMembershipChange();
                                })
                                .catch((error) => {
                                    if (isApiError(error) && error.displayableToUser) {
                                        dispatch(showErrorSnackbar(error.message));
                                    } else {
                                        console.error(error);
                                        dispatch(showErrorSnackbar('Fehler beim Entfernen der Mitarbeiter:in aus dem Team'));
                                    }
                                })
                                .finally(() => {
                                    dispatch(setLoadingMessage(undefined));
                                });
                        });
                },
                tooltip: 'Mitarbeiter:in entfernen',
                disabled: !canDeleteMembership,
                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_DELETE),
            },
        ];
    }, [canDeleteMembership, canReadDomainRoles, canUpdateMembership, dispatch, item, refreshPermissionsAfterMembershipChange, showConfirm, listControlRef]);

    const columns = useMemo(() => buildColumns(canReadDomainRoles), [canReadDomainRoles]);

    const openSelectNewMemberDialog = useCallback(() => {
        if (item == null) {
            return;
        }

        new TeamMembershipsApiService()
            .listAll({
                teamId: item.id,
            })
            .then((membershipsPage) => {
                setExistingMemberUserIds(membershipsPage.content.map((membership) => membership.userId));
                setShowSelectNewMemberDialog(true);
            })
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die bestehenden Mitgliedschaften konnten nicht geladen werden.',
            )));
    }, [dispatch, item]);

    const preSearchElements = useMemo(() => {
        const addDisabled = !canCreateMembership || !canReadDomainRoles;
        const addDisabledTooltip = !canCreateMembership
            ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_CREATE)
            : !canReadDomainRoles
                ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                : '';

        return [
            <DisabledTooltip
                key="add-team-member"
                title={addDisabledTooltip}
                disabled={addDisabled}
            >
                <Button
                    variant="contained"
                    startIcon={<AddOutlinedIcon />}
                    onClick={openSelectNewMemberDialog}
                    disabled={addDisabled}
                >
                    Mitarbeiter:in hinzufügen
                </Button>
            </DisabledTooltip>,
        ];
    }, [canCreateMembership, canReadDomainRoles, openSelectNewMemberDialog]);

    const handleAddMembership = useCallback((user: User | null, roleIdsToAdd: number[]) => {
        if (user == null || item == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge Mitarbeiter:in ${user.fullName} zum Team hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new TeamMembershipsApiService()
            .create({
                id: 0,
                userId: user.id,
                teamId: item.id,
                created: new Date().toISOString(),
                updated: new Date().toISOString(),
            })
            .then((membership) => {
                const apiService = new VTeamUserRoleAssignmentWithDetailsApiService();
                return Promise.all(roleIdsToAdd.map((roleId) => apiService.create({
                    id: 0,
                    departmentMembershipId: null,
                    teamMembershipId: membership.id,
                    userRoleId: roleId,
                    created: new Date().toISOString(),
                })));
            })
            .then(() => {
                // Refresh list
                listControlRef.current?.refresh();
                refreshPermissionsAfterMembershipChange();
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar('Fehler beim Hinzufügen der Mitarbeiter:in zum Team'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    }, [dispatch, item, listControlRef, refreshPermissionsAfterMembershipChange]);

    const handleUpdateMembership = useCallback((membership: VTeamMembershipWithDetailsEntity | null, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        if (membership == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Aktualisiere Rollen der Mitarbeiter:in ${membership.userFullName}`,
            blocking: true,
            estimatedTime: 5000,
        }));

        const apiService = new VTeamUserRoleAssignmentWithDetailsApiService();

        const addPromises = roleIdsToAdd
            .map((roleId) => apiService.create({
                id: 0,
                departmentMembershipId: null,
                teamMembershipId: membership.membershipId,
                userRoleId: roleId,
                created: new Date().toISOString(),
            }));

        const removePromises = userRoleAssignmentIdsToRemove
            .map((assignmentId) => apiService.destroy(assignmentId));

        Promise
            .all([
                ...addPromises,
                ...removePromises,
            ])
            .then(() => {
                // Refresh list
                listControlRef.current?.refresh();
                refreshPermissionsAfterMembershipChange();
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar('Fehler beim Aktualisieren der Rollen der Mitarbeiter:in'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    }, [dispatch, listControlRef, refreshPermissionsAfterMembershipChange]);

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{
                    mt: 1.5,
                    mb: 1,
                }}
            >
                Mitarbeiter:innen des Teams
            </Typography>

            <Typography
                sx={{
                    mb: 3,
                    maxWidth: 900,
                }}
            >
                Eine Liste der Mitarbeiter:innen, die diesem Team zugeordnet sind. Mitarbeiter:innen können unterschiedliche Rollen besitzen, die ihre Berechtigungen innerhalb des Teams definieren.
            </Typography>

            <GenericList<VTeamMembershipWithDetailsEntity>
                controlRef={listControlRef}
                filters={Filters}
                defaultFilter="active"
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                fetch={fetchMembers}
                getRowIdentifier={getRowIdentifier}
                searchLabel="Mitarbeiter:in suchen"
                searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                rowActionsCount={2}
                rowActions={buildRowActions}
                defaultSortField="userId"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Mitgliedschaften im Zugriff"
                        description="Es wurden keine Mitgliedschaften gefunden, auf die Sie Zugriff haben. Möglicherweise wurden noch keine Mitarbeiter:innen zugeordnet oder Ihnen fehlt die Leseberechtigung für Mitgliedschaften."
                        addText="Mitarbeiter:in hinzufügen"
                        onAdd={openSelectNewMemberDialog}
                        addDisabled={!canCreateMembership || !canReadDomainRoles}
                        addDisabledTooltip={!canCreateMembership
                            ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_CREATE)
                            : formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)}
                    />
                }
                loadingPlaceholder="Lade Mitarbeiter:innen…"
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                preSearchElements={preSearchElements}
            />

            <SelectUserDialog
                open={showSelectNewMemberDialog}
                idsToExclude={existingMemberUserIds}
                onClose={() => setShowSelectNewMemberDialog(false)}
                onSelect={(user) => {
                    setShowSelectRolesDialogForUser(user);
                    setShowSelectNewMemberDialog(false);
                }}
            />

            <UserRolesAssignmentDialog
                open={showSelectRolesDialogForUser != null}
                onClose={() => {
                    setShowSelectRolesDialogForUser(null);
                }}
                onSave={(roleIdsToAdd) => {
                    handleAddMembership(showSelectRolesDialogForUser, roleIdsToAdd);
                    setShowSelectRolesDialogForUser(null);
                }}
                userId={showSelectRolesDialogForUser?.id ?? undefined}
                parentId={item.id}
                parentType="team"
            />

            <UserRolesAssignmentDialog
                open={showSelectRolesDialogForMembership != null}
                onClose={() => {
                    setShowSelectRolesDialogForMembership(null);
                }}
                onSave={(roleIdsToAdd, userRoleAssignmentIdsToRemove) => {
                    handleUpdateMembership(showSelectRolesDialogForMembership, roleIdsToAdd, userRoleAssignmentIdsToRemove);
                    setShowSelectRolesDialogForMembership(null);
                }}
                userId={showSelectRolesDialogForMembership?.userId ?? undefined}
                parentId={item.id}
                parentType="team"
            />
        </Box>
    );
}

const Filters = [
    {
        label: 'Aktiv',
        value: 'active',
    },
    {
        label: 'Inaktiv',
        value: 'inactive',
    },
    {
        label: 'Gelöscht',
        value: 'deleted',
    },
];

function buildColumns(canReadDomainRoles: boolean): Array<GridColDef<VTeamMembershipWithDetailsEntity>> {
    return [
        {
            field: 'userFullName',
            headerName: 'Mitarbeiter:in',
            flex: 1,
        },
        {
            field: 'userEmail',
            headerName: 'E-Mail',
            flex: 1,
        },
        {
            field: 'roles',
            headerName: 'Rollen',
            flex: 1,
            sortable: false,
            renderCell: (params) => canReadDomainRoles ? (
                <UserRoleChips
                    roles={params.row.domainRoles.map(item => ({
                        name: item.name ?? '',
                        id: item.id,
                    }))}
                    maxVisibleChips={1}
                />
            ) : (
                <UserRoleChips
                    roles={[{
                        id: 'domain-role-read-missing',
                        name: 'Keine Berechtigung zur Einsicht',
                    }]}
                    maxVisibleChips={1}
                />
            ),
        },
        {
            field: 'enabled',
            headerName: 'Status',
            type: 'boolean',
            sortable: false,
            renderCell: (params) => (
                <UserStatusChip
                    userDeletedInIdp={params.row.userDeletedInIdp}
                    userEnabled={params.row.userEnabled}
                />
            ),
        },
    ];
}

function getRowIdentifier(item: VTeamMembershipWithDetailsEntity): string {
    return item.userId;
}
