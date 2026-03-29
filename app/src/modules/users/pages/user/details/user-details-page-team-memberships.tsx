import React, {useContext, useEffect, useMemo, useRef, useState} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import EditOutlined from '@mui/icons-material/EditOutlined';
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
import {useAccessGuard} from '../../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserRoleChips} from '../../../../user-roles/components/user-role-chips';
import {Button} from "@mui/material";
import Add from "@aivot/mui-material-symbols-400-outlined/dist/add/Add";
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


const columns: Array<GridColDef<VTeamMembershipWithDetailsEntity>> = [
    {
        field: 'teamName',
        headerName: 'Team',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/teams/${params.row.teamId}`}
                title="Team bearbeiten"
            >
                {String(params.row.teamName)}
            </CellLink>
        ),
    },
    {
        field: 'domainRoles',
        headerName: 'Rollen',
        flex: 1,
        sortable: false,
        renderCell: (params) => (
            <UserRoleChips roles={params.row.domainRoles.map(item => ({
                id: item.id!,
                name: item.name ?? '',
            }))}/>
        ),
    },
];

export function UserDetailsPageTeamMemberships() {
    const dispatch = useAppDispatch();

    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const [availableTeams, setAvailableTeams] = useState<TeamEntity[]>();
    const [showSelectNewTeamDialog, setShowSelectNewTeamDialog] = useState(false);
    const [showSelectRolesDialogForTeam, setShowSelectRolesDialogForTeam] = useState<TeamEntity | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VTeamMembershipWithDetailsEntity | null>(null);

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    useEffect(() => {
        new TeamsApiService()
            .listAll()
            .then(({content}) => setAvailableTeams(content))
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der verfügbaren Teams ist ein Fehler aufgetreten.'));
            });
    }, []);

    const preSearchElements = useMemo(() => {
        if (!hasAccess) {
            return undefined;
        }

        return [
            <Button
                variant="contained"
                startIcon={<Add/>}
                onClick={() => setShowSelectNewTeamDialog(true)}
            >
                Mitgliedschaft hinzufügen
            </Button>,
        ];
    }, [hasAccess]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleAddMembership = (user: User, team: TeamEntity, roleIdsToAdd: number[]) => {
        dispatch(setLoadingMessage({
            message: `Füge die Mitarbeiter:in zum Team ${team.name} hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new TeamMembershipsApiService()
            .create({
                id: 0,
                userId: user.id,
                teamId: team.id,
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

    return (
        <>
            <Box sx={{pt: 2}}>
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
                    noDataPlaceholder="Keine Teams vorhanden"
                    loadingPlaceholder="Lade Teams…"
                    noSearchResultsPlaceholder="Keine Teams gefunden"
                    rowActions={(item) => [
                        {
                            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                            onClick: () => {
                                setShowSelectRolesDialogForMembership(item);
                            },
                            tooltip: hasAccess ? 'Rollen bearbeiten' : 'Rollen anzeigen',
                        }, {
                            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                            to: `/teams/${item.teamId}`,
                            tooltip: hasAccess ? 'Team bearbeiten' : 'Team anzeigen',
                        }
                    ]}
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
                        setShowSelectRolesDialogForTeam(dep);
                        setShowSelectNewTeamDialog(false);
                    },
                    searchPlaceholder: 'Teams suchen',
                    searchKeys: ['name'],
                    primaryTextKey: 'name',
                    getId: o => String(o.id),
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
                parentId={showSelectRolesDialogForTeam?.id}
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
                parentId={showSelectRolesDialogForMembership?.teamId}
                parentType="team"
            />
        </>
    );
}
