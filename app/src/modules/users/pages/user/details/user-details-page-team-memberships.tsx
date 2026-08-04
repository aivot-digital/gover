import React, {useCallback, useContext, useEffect, useMemo, useRef, useState} from 'react';
import {EmptyDataListPlaceholder} from '../../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {type GridColDef} from '@mui/x-data-grid';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import ManageAccountsOutlined from '@aivot/mui-material-symbols-400-n25-outlined/ManageAccounts';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {GenericList} from '../../../../../components/generic-list/generic-list';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {type User} from '../../../../../models/entities/user';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {UserRoleChips} from '../../../../user-roles/components/user-role-chips';
import {Button} from "@mui/material";
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {SearchBaseDialog} from "../../../../../dialogs/search-base-dialog/search-base-dialog";
import {useAppDispatch} from "../../../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar, showErrorSnackbar} from "../../../../../slices/snackbar-slice";
import {UserRolesAssignmentDialog} from "../../../../user-roles/components/user-roles-assignment-dialog";
import {ListControlRef} from "../../../../../components/generic-list/generic-list-props";
import {setLoadingMessage} from "../../../../../slices/shell-slice";
import {isApiError} from "../../../../../models/api-error";
import {TeamEntity} from "../../../../teams/entities/team-entity";
import {TeamsApiService} from "../../../../teams/services/teams-api-service";
import {TeamMembershipsApiService} from "../../../../teams/services/team-memberships-api-service";
import {
    VTeamUserRoleAssignmentWithDetailsApiService
} from "../../../../teams/services/v-team-user-role-assignment-with-details-api-service";
import {
    VTeamMembershipWithDetailsApiService
} from "../../../../teams/services/v-team-membership-with-details-api-service";
import {VTeamMembershipWithDetailsEntity} from "../../../../teams/entities/v-team-membership-with-details-entity";
import {useConfirm} from "../../../../../providers/confirm-provider";
import {useRefreshPermissionSet} from '../../../../permissions/hooks/use-permissions';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../../slices/user-slice';
import {Permission} from '../../../../../data/permissions/permission';
import {
    hasAnyTeamPermission,
    hasSystemPermission,
    hasTeamPermission,
    formatMissingPermissionTooltip,
} from '../../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';
import {type PermissionSet} from '../../../../permissions/models/permission-set';
import {ModuleIcons} from '../../../../../shells/staff/data/module-icons';

const deletedUserMembershipTooltip = 'Für im Identity Provider gelöschte Mitarbeiter:innen können Mitgliedschaften und Rollen nicht mehr geändert werden.';
const membershipIdsLoadingTooltip = 'Lade bestehende Mitgliedschaften…';
const membershipIdsLoadErrorTooltip = 'Die bestehenden Mitgliedschaften konnten nicht geladen werden.';
type MembershipIdsLoadState = 'loading' | 'loaded' | 'error';

export function UserDetailsPageTeamMemberships() {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const refreshPermissionSet = useRefreshPermissionSet();
    const permissions = useAppSelector(selectPermissions);

    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const [availableTeams, setAvailableTeams] = useState<TeamEntity[]>();
    const [showSelectNewTeamDialog, setShowSelectNewTeamDialog] = useState(false);
    const [showSelectRolesDialogForTeam, setShowSelectRolesDialogForTeam] = useState<TeamEntity | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VTeamMembershipWithDetailsEntity | null>(null);
    const [assignedTeamIds, setAssignedTeamIds] = useState<Set<number>>(new Set());
    const [assignedTeamIdsLoadState, setAssignedTeamIdsLoadState] = useState<MembershipIdsLoadState>('loading');

    const canManageMemberships = user != null && !user.deletedInIdp;
    const canReadDomainRoles = hasSystemPermission(permissions, Permission.DOMAIN_ROLE_READ);
    const canReadAnyTeam = hasAnyTeamPermission(permissions, Permission.TEAM_READ);
    const canCreateAnyTeamMembership = hasAnyTeamPermission(permissions, Permission.TEAM_MEMBERSHIP_CREATE);
    const canOpenSelectNewTeamDialogBase = canManageMemberships &&
        canReadAnyTeam &&
        canCreateAnyTeamMembership &&
        canReadDomainRoles;
    const canOpenSelectNewTeamDialog = canOpenSelectNewTeamDialogBase &&
        assignedTeamIdsLoadState === 'loaded';

    const newMembershipDisabledTooltip = !canManageMemberships
        ? deletedUserMembershipTooltip
        : !canCreateAnyTeamMembership
            ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_CREATE)
            : !canReadAnyTeam
                ? formatMissingPermissionTooltip(Permission.TEAM_READ)
                : !canReadDomainRoles
                    ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                    : assignedTeamIdsLoadState === 'error'
                        ? membershipIdsLoadErrorTooltip
                        : assignedTeamIdsLoadState !== 'loaded'
                            ? membershipIdsLoadingTooltip
                            : '';

    const columns = useMemo(() => buildColumns(permissions, canReadDomainRoles), [canReadDomainRoles, permissions]);

    const refreshPermissionsAfterMembershipChange = () => {
        // Effective permissions may include grants inherited through deputy assignments.
        // The frontend cannot know whether the edited user is currently represented by the active user.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Teammitgliedschaft nicht aktualisiert werden.',
            )));
    };

    const refreshAvailableTeams = useCallback(() => {
        const userId = user?.id;

        if (!canOpenSelectNewTeamDialogBase || userId == null) {
            setAvailableTeams(undefined);
            setAssignedTeamIds(new Set());
            setAssignedTeamIdsLoadState('loaded');
            return;
        }

        setAvailableTeams(undefined);
        setAssignedTeamIdsLoadState('loading');

        Promise
            .all([
                new TeamsApiService().listAll(),
                new VTeamMembershipWithDetailsApiService().listAll({userId}),
            ])
            .then(([teams, memberships]) => {
                const nextAssignedTeamIds = new Set(memberships.content.map((membership) => membership.teamId));

                setAssignedTeamIds(nextAssignedTeamIds);
                setAvailableTeams(teams.content.filter((team) => (
                    !nextAssignedTeamIds.has(team.id) &&
                    hasTeamPermission(
                        permissions,
                        team.id,
                        Permission.TEAM_MEMBERSHIP_CREATE,
                    )
                )));
                setAssignedTeamIdsLoadState('loaded');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der verfügbaren Teams ist ein Fehler aufgetreten.'));
                setAssignedTeamIdsLoadState('error');
            });
    }, [canOpenSelectNewTeamDialogBase, dispatch, permissions, user?.id]);

    useEffect(() => {
        refreshAvailableTeams();
    }, [refreshAvailableTeams]);

    const preSearchElements = useMemo(() => {
        return [
            <DisabledTooltip
                key="add-team-membership"
                title={newMembershipDisabledTooltip}
                disabled={!canOpenSelectNewTeamDialog}
            >
                <Button
                    variant="contained"
                    startIcon={<Add/>}
                    disabled={!canOpenSelectNewTeamDialog}
                    onClick={() => setShowSelectNewTeamDialog(true)}
                >
                    Mitgliedschaft hinzufügen
                </Button>
            </DisabledTooltip>,
        ];
    }, [canOpenSelectNewTeamDialog, newMembershipDisabledTooltip]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleAddMembership = (user: User, team: TeamEntity, roleIdsToAdd: number[]) => {
        if (
            !canManageMemberships ||
            !canReadDomainRoles ||
            assignedTeamIds.has(team.id) ||
            !hasTeamPermission(permissions, team.id, Permission.TEAM_MEMBERSHIP_CREATE)
        ) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge die Mitarbeiter:in zum Team ${team.name} hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new TeamMembershipsApiService()
            .create({
                userId: user.id,
                teamId: team.id,
                roleIds: roleIdsToAdd,
            })
            .then(() => {
                listControlRef.current?.refresh();
                refreshAvailableTeams();
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
    };

    const handleUpdateMembership = (membership: VTeamMembershipWithDetailsEntity, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        if (
            !canManageMemberships ||
            !canReadDomainRoles ||
            !hasTeamPermission(permissions, membership.teamId, Permission.TEAM_MEMBERSHIP_UPDATE)
        ) {
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
    };

    const handleDeleteMembership = (membership: VTeamMembershipWithDetailsEntity) => {
        if (!hasTeamPermission(permissions, membership.teamId, Permission.TEAM_MEMBERSHIP_DELETE)) {
            return;
        }

        confirm({
            title: 'Mitgliedschaft löschen',
            children: (
                <>
                    <Typography>
                        Durch das Entfernen der Mitarbeiter:in <strong>{membership.userFullName}</strong> aus dem Team
                        <strong> {membership.teamName}</strong> verliert diese alle zugewiesenen Rollen und
                        Berechtigungen in diesem Team.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Diese Aktion kann nicht rückgängig gemacht werden.
                    </Typography>
                </>
            ),
            confirmButtonText: 'Mitgliedschaft löschen',
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                dispatch(setLoadingMessage({
                    message: `Lösche Mitgliedschaft von ${membership.userFullName}`,
                    blocking: true,
                    estimatedTime: 5000,
                }));

                new TeamMembershipsApiService()
                    .destroy(membership.membershipId)
                    .then(() => {
                        listControlRef.current?.refresh();
                        refreshAvailableTeams();
                        refreshPermissionsAfterMembershipChange();
                    })
                    .catch((error) => {
                        if (isApiError(error) && error.displayableToUser) {
                            dispatch(showErrorSnackbar(error.message));
                        } else {
                            console.error(error);
                            dispatch(showErrorSnackbar('Fehler beim Löschen der Mitgliedschaft'));
                        }
                    })
                    .finally(() => {
                        dispatch(setLoadingMessage(undefined));
                    });
            });
    };

    return (
        <>
            <Box sx={{pt: 1.5}}>
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Mitgliedschaften in Teams
                </Typography>

                <Typography sx={{mb: 3, maxWidth: 900}}>
                    Eine Übersicht der Teams, in denen diese Mitarbeiter:in Mitglied ist, und die
                    dazugehörigen
                    Rollen.
                </Typography>

                <GenericList<VTeamMembershipWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    controlRef={listControlRef}
                    fetch={(options) => {
                        return new VTeamMembershipWithDetailsApiService()
                            .list(options.page, options.size, options.sort, options.order, {
                                userId: user?.id,
                                name: options.search,
                            });
                    }}
                    getRowIdentifier={(item) => item.membershipId.toString()}
                    searchLabel="Team suchen"
                    searchPlaceholder="Name des Teams eingeben…"
                    defaultSortField="teamName"
                    rowMenuItems={[]}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Teams zugeordnet"
                            description="Teams bündeln Personen für gemeinsame Zuständigkeiten, Berechtigungen oder Aufgaben in Prozessen."
                            addText="Mitgliedschaft hinzufügen"
                            onAdd={() => setShowSelectNewTeamDialog(true)}
                            addDisabled={!canOpenSelectNewTeamDialog}
                            addDisabledTooltip={newMembershipDisabledTooltip}
                        />
                    }
                    loadingPlaceholder="Lade Teams…"
                    noSearchResultsPlaceholder="Keine Teams gefunden"
                    rowActions={(item) => {
                        const canReadTeam = hasTeamPermission(permissions, item.teamId, Permission.TEAM_READ);
                        const canUpdateTeam = hasTeamPermission(permissions, item.teamId, Permission.TEAM_UPDATE);
                        const canUpdateMembership = canManageMemberships &&
                            hasTeamPermission(permissions, item.teamId, Permission.TEAM_MEMBERSHIP_UPDATE);
                        const canDeleteMembership = hasTeamPermission(permissions, item.teamId, Permission.TEAM_MEMBERSHIP_DELETE);

                        return [
                            {
                                icon: <ManageAccountsOutlined/>,
                                disabled: !canUpdateMembership || !canReadDomainRoles,
                                disabledTooltip: !canManageMemberships
                                    ? deletedUserMembershipTooltip
                                    : !canUpdateMembership
                                        ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_UPDATE)
                                        : !canReadDomainRoles
                                            ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                                            : undefined,
                                onClick: () => {
                                    setShowSelectRolesDialogForMembership(item);
                                },
                                tooltip: 'Rollen bearbeiten',
                            }, {
                                icon: canUpdateTeam ? <EditOutlined/> : <Visibility/>,
                                to: `/teams/${item.teamId}`,
                                tooltip: canUpdateTeam ? 'Team bearbeiten' : 'Team anzeigen',
                                disabled: !canReadTeam,
                                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_READ),
                            }, {
                                icon: <Delete/>,
                                tooltip: 'Mitgliedschaft löschen',
                                disabled: !canDeleteMembership,
                                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_DELETE),
                                onClick: () => {
                                    handleDeleteMembership(item);
                                },
                            }
                        ];
                    }}
                    rowActionsCount={3}
                    preSearchElements={preSearchElements}
                />
            </Box>

            <SearchBaseDialog
                open={showSelectNewTeamDialog}
                onClose={() => {
                    setShowSelectNewTeamDialog(false);
                }}
                title="Team auswählen"
                tabs={[{
                    title: 'Alle',
                    options: availableTeams ?? [],
                    onSelect: (dep) => {
                        if (assignedTeamIds.has(dep.id)) {
                            return;
                        }

                        setShowSelectRolesDialogForTeam(dep);
                        setShowSelectNewTeamDialog(false);
                    },
                    searchPlaceholder: 'Teams suchen',
                    searchKeys: ['name'],
                    primaryTextKey: 'name',
                    getId: o => String(o.id),
                    getIcon: () => ModuleIcons.teams,
                    noOptionsMessage: 'Keine weiteren Teams verfügbar.',
                }]}
            />

            <UserRolesAssignmentDialog
                open={showSelectRolesDialogForTeam != null}
                onClose={() => {
                    setShowSelectRolesDialogForTeam(null);
                }}
                onSave={(roleIdsToAdd) => {
                    if (user == null || showSelectRolesDialogForTeam == null || roleIdsToAdd == null) {
                        return;
                    }
                    handleAddMembership(user, showSelectRolesDialogForTeam, roleIdsToAdd);
                    setShowSelectRolesDialogForTeam(null);
                }}
                userId={user.id}
                userLabel={user.fullName}
                parentId={showSelectRolesDialogForTeam?.id}
                parentLabel={showSelectRolesDialogForTeam?.name}
                parentType="team"
            />

            <UserRolesAssignmentDialog
                open={showSelectRolesDialogForMembership != null}
                onClose={() => {
                    setShowSelectRolesDialogForMembership(null);
                }}
                onSave={(roleIdsToAdd, userRoleAssignmentIdsToRemove) => {
                    if (showSelectRolesDialogForMembership == null || roleIdsToAdd == null || userRoleAssignmentIdsToRemove == null) {
                        return;
                    }

                    handleUpdateMembership(showSelectRolesDialogForMembership, roleIdsToAdd, userRoleAssignmentIdsToRemove);
                    setShowSelectRolesDialogForMembership(null);
                }}
                userId={user.id}
                userLabel={user.fullName}
                parentId={showSelectRolesDialogForMembership?.teamId}
                parentLabel={showSelectRolesDialogForMembership?.teamName ?? undefined}
                parentType="team"
            />
        </>
    );
}

function buildColumns(
    permissions: PermissionSet | undefined,
    canReadDomainRoles: boolean,
): Array<GridColDef<VTeamMembershipWithDetailsEntity>> {
    return [
        {
            field: 'teamName',
            headerName: 'Team',
            flex: 1,
            renderCell: (params) => {
                const teamName = String(params.row.teamName);

                if (!hasTeamPermission(permissions, params.row.teamId, Permission.TEAM_READ)) {
                    return teamName;
                }

                return (
                    <CellLink
                        to={`/teams/${params.row.teamId}`}
                        title="Team anzeigen"
                    >
                        {teamName}
                    </CellLink>
                );
            },
        },
        {
            field: 'domainRoles',
            headerName: 'Rollen',
            flex: 1,
            sortable: false,
            renderCell: (params) => canReadDomainRoles ? (
                <UserRoleChips roles={params.row.domainRoles.map(item => ({
                    id: item.id!,
                    name: item.name ?? '',
                }))}/>
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
    ];
}
