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
import {
    VDepartmentMembershipWithDetailsEntity
} from '../../../../departments/entities/v-department-membership-with-details-entity';
import {
    VDepartmentMembershipWithDetailsService
} from '../../../../departments/services/v-department-membership-with-details-service';
import {Button} from "@mui/material";
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {VDepartmentShadowedEntity} from "../../../../departments/entities/v-department-shadowed-entity";
import {SelectDepartmentDialog} from '../../../../departments/dialogs/select-department-dialog';
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
import {useConfirm} from "../../../../../providers/confirm-provider";
import {useRefreshPermissionSet} from '../../../../permissions/hooks/use-permissions';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../../slices/user-slice';
import {Permission} from '../../../../../data/permissions/permission';
import {
    hasAnyDepartmentPermission,
    hasDepartmentPermission,
    hasSystemPermission,
    formatMissingPermissionTooltip,
} from '../../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';
import {type PermissionSet} from '../../../../permissions/models/permission-set';

const deletedUserMembershipTooltip = 'Für im Identity Provider gelöschte Mitarbeiter:innen können Mitgliedschaften und Rollen nicht mehr geändert werden.';
const membershipIdsLoadingTooltip = 'Lade bestehende Mitgliedschaften…';
const membershipIdsLoadErrorTooltip = 'Die bestehenden Mitgliedschaften konnten nicht geladen werden.';
type MembershipIdsLoadState = 'loading' | 'loaded' | 'error';

export function UserDetailsPageDepartmentMemberships() {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const refreshPermissionSet = useRefreshPermissionSet();
    const permissions = useAppSelector(selectPermissions);

    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const [showSelectNewDepartmentDialog, setShowSelectNewDepartmentDialog] = useState(false);
    const [showSelectRolesDialogForDepartment, setShowSelectRolesDialogForDepartment] = useState<VDepartmentShadowedEntity | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VDepartmentMembershipWithDetailsEntity | null>(null);
    const [assignedDepartmentIds, setAssignedDepartmentIds] = useState<Set<number>>(new Set());
    const [assignedDepartmentIdsLoadState, setAssignedDepartmentIdsLoadState] = useState<MembershipIdsLoadState>('loading');

    const canManageMemberships = user != null && !user.deletedInIdp;
    const canReadDomainRoles = hasSystemPermission(permissions, Permission.DOMAIN_ROLE_READ);
    const canReadAnyDepartment = hasAnyDepartmentPermission(permissions, Permission.DEPARTMENT_READ);
    const canCreateAnyDepartmentMembership = hasAnyDepartmentPermission(permissions, Permission.DEPARTMENT_MEMBERSHIP_CREATE);
    const canOpenSelectNewDepartmentDialogBase = canManageMemberships &&
        canReadAnyDepartment &&
        canCreateAnyDepartmentMembership &&
        canReadDomainRoles;
    const canOpenSelectNewDepartmentDialog = canOpenSelectNewDepartmentDialogBase &&
        assignedDepartmentIdsLoadState === 'loaded';

    const newMembershipDisabledTooltip = !canManageMemberships
        ? deletedUserMembershipTooltip
        : !canCreateAnyDepartmentMembership
            ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_CREATE)
            : !canReadAnyDepartment
                ? formatMissingPermissionTooltip(Permission.DEPARTMENT_READ)
                : !canReadDomainRoles
                    ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                    : assignedDepartmentIdsLoadState === 'error'
                        ? membershipIdsLoadErrorTooltip
                        : assignedDepartmentIdsLoadState !== 'loaded'
                            ? membershipIdsLoadingTooltip
                            : '';

    const columns = useMemo(() => buildColumns(permissions, canReadDomainRoles), [canReadDomainRoles, permissions]);

    const refreshAssignedDepartmentIds = useCallback(() => {
        const userId = user?.id;

        if (!canOpenSelectNewDepartmentDialogBase || userId == null) {
            setAssignedDepartmentIds(new Set());
            setAssignedDepartmentIdsLoadState('loaded');
            return;
        }

        setAssignedDepartmentIdsLoadState('loading');

        new VDepartmentMembershipWithDetailsService()
            .listAll({userId})
            .then(({content}) => {
                setAssignedDepartmentIds(new Set(content.map((membership) => membership.departmentId)));
                setAssignedDepartmentIdsLoadState('loaded');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showApiErrorSnackbar(
                    err,
                    'Die bestehenden Organisationseinheitsmitgliedschaften konnten nicht geladen werden.',
                ));
                setAssignedDepartmentIdsLoadState('error');
            });
    }, [canOpenSelectNewDepartmentDialogBase, dispatch, user?.id]);

    useEffect(() => {
        refreshAssignedDepartmentIds();
    }, [refreshAssignedDepartmentIds]);

    const refreshPermissionsAfterMembershipChange = () => {
        // Effective permissions may include grants inherited through deputy assignments.
        // The frontend cannot know whether the edited user is currently represented by the active user.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Organisationseinheitsmitgliedschaft nicht aktualisiert werden.',
            )));
    };

    const preSearchElements = useMemo(() => {
        return [
            <DisabledTooltip
                key="add-department-membership"
                title={newMembershipDisabledTooltip}
                disabled={!canOpenSelectNewDepartmentDialog}
            >
                <Button
                    variant="contained"
                    startIcon={<Add/>}
                    disabled={!canOpenSelectNewDepartmentDialog}
                    onClick={() => setShowSelectNewDepartmentDialog(true)}
                >
                    Mitgliedschaft hinzufügen
                </Button>
            </DisabledTooltip>,
        ];
    }, [canOpenSelectNewDepartmentDialog, newMembershipDisabledTooltip]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleAddMembership = (user: User, department: VDepartmentShadowedEntity, roleIdsToAdd: number[]) => {
        if (
            !canManageMemberships ||
            !canReadDomainRoles ||
            assignedDepartmentIds.has(department.id) ||
            !hasDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_MEMBERSHIP_CREATE)
        ) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge die Mitarbeiter:in zur Organisationseinheit ${department.name} hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new DepartmentMembershipApiService()
            .create({
                userId: user.id,
                departmentId: department.id,
                roleIds: roleIdsToAdd,
            })
            .then(() => {
                listControlRef.current?.refresh();
                refreshAssignedDepartmentIds();
                refreshPermissionsAfterMembershipChange();
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar('Fehler beim Hinzufügen der Mitarbeiter:in zur Organisationseinheit'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    const handleUpdateMembership = (membership: VDepartmentMembershipWithDetailsEntity, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        if (
            !canManageMemberships ||
            !canReadDomainRoles ||
            !hasDepartmentPermission(permissions, membership.departmentId, Permission.DEPARTMENT_MEMBERSHIP_UPDATE)
        ) {
            return;
        }

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

    const handleDeleteMembership = (membership: VDepartmentMembershipWithDetailsEntity) => {
        if (!hasDepartmentPermission(permissions, membership.departmentId, Permission.DEPARTMENT_MEMBERSHIP_DELETE)) {
            return;
        }

        confirm({
            title: 'Mitgliedschaft löschen',
            children: (
                <>
                    <Typography>
                        Durch das Entfernen der Mitarbeiter:in <strong>{membership.userFullName}</strong> aus der
                        Organisationseinheit <strong>{membership.departmentName}</strong> verliert diese alle
                        zugewiesenen Rollen und Berechtigungen in dieser Organisationseinheit.
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

                new DepartmentMembershipApiService()
                    .destroy(membership.membershipId)
                    .then(() => {
                        listControlRef.current?.refresh();
                        refreshAssignedDepartmentIds();
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
                    searchPlaceholder="Name der Organisationseinheit eingeben…"
                    defaultSortField="departmentName"
                    rowMenuItems={[]}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Organisationseinheiten zugeordnet"
                            description="Organisationseinheiten beschreiben, in welchen fachlichen Bereichen diese Person mitarbeitet."
                            addText="Mitgliedschaft hinzufügen"
                            onAdd={() => setShowSelectNewDepartmentDialog(true)}
                            addDisabled={!canOpenSelectNewDepartmentDialog}
                            addDisabledTooltip={newMembershipDisabledTooltip}
                        />
                    }
                    loadingPlaceholder="Lade Organisationseinheiten…"
                    noSearchResultsPlaceholder="Keine Organisationseinheiten gefunden"
                    rowActions={(item) => {
                        const canReadDepartment = hasDepartmentPermission(permissions, item.departmentId, Permission.DEPARTMENT_READ);
                        const canUpdateDepartment = hasDepartmentPermission(permissions, item.departmentId, Permission.DEPARTMENT_UPDATE);
                        const canUpdateMembership = canManageMemberships &&
                            hasDepartmentPermission(permissions, item.departmentId, Permission.DEPARTMENT_MEMBERSHIP_UPDATE);
                        const canDeleteMembership = hasDepartmentPermission(permissions, item.departmentId, Permission.DEPARTMENT_MEMBERSHIP_DELETE);

                        return [
                            {
                                icon: <ManageAccountsOutlined/>,
                                disabled: !canUpdateMembership || !canReadDomainRoles,
                                disabledTooltip: !canManageMemberships
                                    ? deletedUserMembershipTooltip
                                    : !canUpdateMembership
                                        ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_UPDATE)
                                        : !canReadDomainRoles
                                            ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                                            : undefined,
                                onClick: () => {
                                    setShowSelectRolesDialogForMembership(item);
                                },
                                tooltip: 'Rollen bearbeiten',
                            }, {
                                icon: canUpdateDepartment ? <EditOutlined/> : <Visibility/>,
                                to: `/departments/${item.departmentId}`,
                                tooltip: canUpdateDepartment ? 'Organisationseinheit bearbeiten' : 'Organisationseinheit anzeigen',
                                disabled: !canReadDepartment,
                                disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_READ),
                            }, {
                                icon: <Delete/>,
                                tooltip: 'Mitgliedschaft löschen',
                                disabled: !canDeleteMembership,
                                disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_DELETE),
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

            <SelectDepartmentDialog
                open={showSelectNewDepartmentDialog}
                onClose={() => {
                    setShowSelectNewDepartmentDialog(false);
                }}
                title="Organisationseinheit auswählen"
                isDepartmentSelectable={(department) => (
                    !assignedDepartmentIds.has(department.id) &&
                    hasDepartmentPermission(
                        permissions,
                        department.id,
                        Permission.DEPARTMENT_MEMBERSHIP_CREATE,
                    )
                )}
                getDepartmentDisabledTooltip={(department) => (
                    assignedDepartmentIds.has(department.id) ?
                        'Bereits Mitglied' :
                        formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_CREATE)
                )}
                onSelect={(department) => {
                    if (assignedDepartmentIds.has(department.id)) {
                        return;
                    }

                    setShowSelectRolesDialogForDepartment(department);
                    setShowSelectNewDepartmentDialog(false);
                }}
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
                userLabel={user.fullName}
                parentId={showSelectRolesDialogForDepartment?.id}
                parentLabel={showSelectRolesDialogForDepartment?.name}
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
                userLabel={user.fullName}
                parentId={showSelectRolesDialogForMembership?.departmentId}
                parentLabel={showSelectRolesDialogForMembership?.departmentName ?? undefined}
                parentType="orgUnit"
            />
        </>
    );
}

function buildColumns(
    permissions: PermissionSet | undefined,
    canReadDomainRoles: boolean,
): Array<GridColDef<VDepartmentMembershipWithDetailsEntity>> {
    return [
        {
            field: 'departmentName',
            headerName: 'Organisationseinheit',
            flex: 1,
            renderCell: (params) => {
                const departmentName = String(params.row.departmentName);

                if (!hasDepartmentPermission(permissions, params.row.departmentId, Permission.DEPARTMENT_READ)) {
                    return departmentName;
                }

                return (
                    <CellLink
                        to={`/departments/${params.row.departmentId}`}
                        title="Organisationseinheit anzeigen"
                    >
                        {departmentName}
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
