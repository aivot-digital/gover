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
import {
    VDepartmentMembershipWithDetailsEntity
} from '../../../../departments/entities/v-department-membership-with-details-entity';
import {
    VDepartmentMembershipWithDetailsService
} from '../../../../departments/services/v-department-membership-with-details-service';
import {Button} from "@mui/material";
import Add from "@aivot/mui-material-symbols-400-outlined/dist/add/Add";
import {VDepartmentShadowedEntity} from "../../../../departments/entities/v-department-shadowed-entity";
import {SearchBaseDialog} from "../../../../../dialogs/search-base-dialog/search-base-dialog";
import {getDepartmentPath, getDepartmentTypeIcons} from "../../../../departments/utils/department-utils";
import {VDepartmentShadowedApiService} from "../../../../departments/services/v-department-shadowed-api-service";
import {useAppDispatch} from "../../../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar, showErrorSnackbar} from "../../../../../slices/snackbar-slice";
import {UserRolesAssignmentDialog} from "../../../../user-roles/components/user-roles-assignment-dialog";
import {ListControlRef} from "../../../../../components/generic-list/generic-list-props";
import {setLoadingMessage} from "../../../../../slices/shell-slice";
import {DepartmentMembershipApiService} from "../../../../departments/services/department-membership-api-service";
import {isApiError} from "../../../../../models/api-error";
import {
    VDepartmentUserRoleAssignmentWithDetailsService
} from "../../../../departments/services/v-department-user-role-assignment-with-details-service";


const columns: Array<GridColDef<VDepartmentMembershipWithDetailsEntity>> = [
    {
        field: 'departmentName',
        headerName: 'Organisationseinheit',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.row.departmentId}`}
                title="Organisationseinheit bearbeiten"
            >
                {String(params.row.departmentName)}
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

export function UserDetailsPageDepartmentMemberships() {
    const dispatch = useAppDispatch();

    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const [availableDepartments, setAvailableDepartments] = useState<VDepartmentShadowedEntity[]>();
    const [showSelectNewDepartmentDialog, setShowSelectNewDepartmentDialog] = useState(false);
    const [showSelectRolesDialogForDepartment, setShowSelectRolesDialogForDepartment] = useState<VDepartmentShadowedEntity | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VDepartmentMembershipWithDetailsEntity | null>(null);

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    useEffect(() => {
        new VDepartmentShadowedApiService()
            .listAll()
            .then(({content}) => setAvailableDepartments(content))
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der verfügbaren Organisationseinheiten ist ein Fehler aufgetreten.'));
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
                onClick={() => setShowSelectNewDepartmentDialog(true)}
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

    const handleAddMembership = (user: User, department: VDepartmentShadowedEntity, roleIdsToAdd: number[]) => {
        dispatch(setLoadingMessage({
            message: `Füge die Mitarbeiter:in zur Organisationseinheit ${department.name} hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new DepartmentMembershipApiService()
            .create({
                id: 0,
                userId: user.id,
                departmentId: department.id,
                created: new Date().toISOString(),
                updated: new Date().toISOString(),
            })
            .then((membership) => {
                const apiService = new VDepartmentUserRoleAssignmentWithDetailsService();
                return Promise.all(roleIdsToAdd.map((roleId) => apiService.create({
                    id: 0,
                    departmentMembershipId: membership.id,
                    teamMembershipId: null,
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
                    dispatch(showErrorSnackbar('Fehler beim Hinzufügen der Mitarbeiter:in zum Fachbereich'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    const handleUpdateMembership = (membership: VDepartmentMembershipWithDetailsEntity, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        dispatch(setLoadingMessage({
            message: `Aktualisiere Rollen der Mitarbeiter:in ${membership.userFullName}`,
            blocking: true,
            estimatedTime: 5000,
        }));

        const apiService = new VDepartmentUserRoleAssignmentWithDetailsService();

        const addPromises = roleIdsToAdd
            .map((roleId) => apiService.create({
                id: 0,
                departmentMembershipId: membership.membershipId,
                teamMembershipId: null,
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
                    Mitgliedschaften in Organisationseinheiten
                </Typography>

                <Typography sx={{mb: 3, maxWidth: 900}}>
                    Eine Übersicht der Organisationseinheiten, in denen diese Mitarbeiter:in Mitglied ist, und die
                    dazugehörigen
                    Rollen.
                </Typography>

                <GenericList<VDepartmentMembershipWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    controlRef={listControlRef}
                    fetch={(options) => {
                        return new VDepartmentMembershipWithDetailsService()
                            .list(options.page, options.size, options.sort, options.order, {
                                userId: user?.id,
                                name: options.search,
                            });
                    }}
                    getRowIdentifier={(item) => item.membershipId.toString()}
                    searchLabel="Organisationseinheit suchen"
                    searchPlaceholder="Titel der Organisationseinheit eingeben…"
                    defaultSortField="departmentName"
                    rowMenuItems={[]}
                    noDataPlaceholder="Keine Organisationseinheiten vorhanden"
                    loadingPlaceholder="Lade Organisationseinheiten…"
                    noSearchResultsPlaceholder="Keine Organisationseinheiten gefunden"
                    rowActions={(item) => [
                        {
                            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                            onClick: () => {
                                setShowSelectRolesDialogForMembership(item);
                            },
                            tooltip: hasAccess ? 'Rollen bearbeiten' : 'Rollen anzeigen',
                        }, {
                            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                            to: `/departments/${item.departmentId}`,
                            tooltip: hasAccess ? 'Organisationseinheit bearbeiten' : 'Organisationseinheit anzeigen',
                        }
                    ]}
                    preSearchElements={preSearchElements}
                />
            </Box>

            <SearchBaseDialog
                open={showSelectNewDepartmentDialog}
                onClose={() => {
                    setShowSelectNewDepartmentDialog(false);
                }}
                title="Organisationseinheit auswählen"
                tabs={[{
                    title: 'Alle',
                    options: availableDepartments ?? [],
                    onSelect: (dep) => {
                        setShowSelectRolesDialogForDepartment(dep);
                        setShowSelectNewDepartmentDialog(false);
                    },
                    searchPlaceholder: 'Organisationseinheit suchen',
                    searchKeys: ['name'],
                    primaryTextKey: 'name',
                    secondaryTextKey: (option) => getDepartmentPath(option),
                    getId: o => String(o.id),
                    getIcon: (option) => {
                        return getDepartmentTypeIcons(option.depth);
                    },
                }]}
            />

            <UserRolesAssignmentDialog
                open={showSelectRolesDialogForDepartment != null}
                onClose={() => {
                    setShowSelectRolesDialogForDepartment(null);
                }}
                onSave={(roleIdsToAdd) => {
                    if (user == null || showSelectRolesDialogForDepartment == null || roleIdsToAdd == null) {
                        return;
                    }
                    handleAddMembership(user, showSelectRolesDialogForDepartment, roleIdsToAdd);
                    setShowSelectRolesDialogForDepartment(null);
                }}
                userId={user.id}
                parentId={showSelectRolesDialogForDepartment?.id}
                parentType="orgUnit"
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
                parentId={showSelectRolesDialogForMembership?.departmentId}
                parentType="orgUnit"
            />
        </>
    );
}
