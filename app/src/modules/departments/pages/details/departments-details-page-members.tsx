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
import {DepartmentEntity} from '../../entities/department-entity';
import {VDepartmentMembershipWithDetailsEntity} from '../../entities/v-department-membership-with-details-entity';
import {
    ListDepartmentMembershipsWithRolesFilter,
    VDepartmentMembershipWithDetailsService
} from '../../services/v-department-membership-with-details-service';
import {DepartmentMembershipApiService} from '../../services/department-membership-api-service';
import {
    VDepartmentUserRoleAssignmentWithDetailsService
} from "../../services/v-department-user-role-assignment-with-details-service";
import {resolveUserName} from "../../../users/utils/resolve-user-name";
import {snakeToCamel} from "../../../../utils/camel-to-snake";
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {
    useCheckDepartmentPermission,
    useCheckSystemPermission,
    useRefreshPermissionSet,
} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export function DepartmentsDetailsPageMembers() {
    const dispatch = useAppDispatch();
    const refreshPermissionSet = useRefreshPermissionSet();

    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, undefined>;
    const canReadMemberships = useCheckDepartmentPermission(item?.id, Permission.DEPARTMENT_MEMBERSHIP_READ);
    const canCreateMembership = useCheckDepartmentPermission(item?.id, Permission.DEPARTMENT_MEMBERSHIP_CREATE);
    const canUpdateMembership = useCheckDepartmentPermission(item?.id, Permission.DEPARTMENT_MEMBERSHIP_UPDATE);
    const canDeleteMembership = useCheckDepartmentPermission(item?.id, Permission.DEPARTMENT_MEMBERSHIP_DELETE);
    const canReadDomainRoles = useCheckSystemPermission(Permission.DOMAIN_ROLE_READ);
    const canReadUsers = useCheckSystemPermission(Permission.USER_READ);

    const showConfirm = useConfirm();

    const listControlRef = useRef<ListControlRef | null>(null);
    const [showSelectNewMemberDialog, setShowSelectNewMemberDialog] = useState(false);
    const [showSelectRolesDialogForUser, setShowSelectRolesDialogForUser] = useState<User | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VDepartmentMembershipWithDetailsEntity | null>(null);
    const [existingMemberUserIds, setExistingMemberUserIds] = useState<string[]>([]);

    const refreshPermissionsAfterMembershipChange = useCallback(() => {
        // Effective permissions may include grants inherited through deputy assignments.
        // The frontend cannot know whether the changed membership belongs to a represented user.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Organisationseinheitsmitgliedschaft nicht aktualisiert werden.',
            )));
    }, [dispatch, refreshPermissionSet]);

    const fetchMembers = useCallback((options: GenericListPropsFetchOptions<VDepartmentMembershipWithDetailsEntity>) => {
        if (item == null || !canReadMemberships) {
            // GenericList always expects an async page result. While the department details are still loading,
            // or the membership list is not readable, we return an empty page instead of hitting the API.
            return Promise.resolve({
                content: [],
                page: {
                    number: 0,
                    size: options.size,
                    totalElements: 0,
                    totalPages: 0,
                },
            });
        }

        const filters: ListDepartmentMembershipsWithRolesFilter = {
            departmentId: item.id,
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

        return new VDepartmentMembershipWithDetailsService()
            .listDepartmentMembershipsWithRoles(options.page, options.size, options.sort as any, options.order, filters);
    }, [canReadMemberships, item]);

    const buildRowActions = useCallback((membershipItem: VDepartmentMembershipWithDetailsEntity) => {
        return [
            {
                icon: <EditOutlinedIcon/>,
                onClick: () => {
                    setShowSelectRolesDialogForMembership(membershipItem);
                },
                tooltip: membershipItem.userDeletedInIdp ? `Kann für gelöschte Mitarbeiter:innen nicht geändert werden` : 'Rolle der Mitarbeiter:in bearbeiten',
                disabled: !canUpdateMembership || !canReadDomainRoles || (membershipItem.userDeletedInIdp ?? false),
                disabledTooltip: !canUpdateMembership
                    ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_UPDATE)
                    : !canReadDomainRoles
                        ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                        : undefined,
            },
            {
                icon: <Delete/>,
                onClick: () => {
                    showConfirm({
                        title: 'Mitarbeiter:in entfernen',
                        children: (
                            <>
                                <Typography>
                                    Durch das Entfernen der
                                    Mitarbeiter:in <strong>{membershipItem.userFullName}</strong> aus der
                                    Organisationseinheit <strong>{item?.name}</strong> verliert diese alle zugewiesenen Rollen
                                    und Berechtigungen in dieser Organisationseinheit.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Diese Aktion kann nicht rückgängig gemacht werden. Stellen Sie sicher, dass Sie die
                                    richtige Mitarbeiter:in entfernen.
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
                                message: `Entferne Mitarbeiter:in ${membershipItem.userFullName} aus der Organisationseinheit`,
                                blocking: true,
                                estimatedTime: 5000,
                            }));

                            new DepartmentMembershipApiService()
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
                                        dispatch(showErrorSnackbar('Fehler beim Entfernen der Mitarbeiter:in aus der Organisationseinheit'));
                                    }
                                })
                                .finally(() => {
                                    dispatch(setLoadingMessage(undefined));
                                });
                        });
                },
                tooltip: 'Mitarbeiter:in entfernen',
                disabled: !canDeleteMembership,
                disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_DELETE),
            },
        ];
    }, [canDeleteMembership, canReadDomainRoles, canUpdateMembership, dispatch, item, refreshPermissionsAfterMembershipChange, showConfirm, listControlRef]);

    const columns = useMemo(() => buildColumns(canReadDomainRoles), [canReadDomainRoles]);

    const openSelectNewMemberDialog = useCallback(() => {
        if (item == null) {
            return;
        }

        new DepartmentMembershipApiService()
            .listAll({
                departmentId: item.id,
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
        const addDisabled = !canCreateMembership || !canReadUsers || !canReadDomainRoles;
        const addDisabledTooltip = !canCreateMembership
            ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_CREATE)
            : !canReadUsers
                ? formatMissingPermissionTooltip(Permission.USER_READ)
                : !canReadDomainRoles
                    ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_READ)
                    : '';

        return [
            <DisabledTooltip
                key="add-department-member"
                title={addDisabledTooltip}
                disabled={addDisabled}
            >
                <Button
                    variant="contained"
                    startIcon={<AddOutlinedIcon/>}
                    onClick={openSelectNewMemberDialog}
                    disabled={addDisabled}
                >
                    Mitarbeiter:in hinzufügen
                </Button>
            </DisabledTooltip>,
        ];
    }, [canCreateMembership, canReadDomainRoles, canReadUsers, openSelectNewMemberDialog]);

    const handleAddMembership = useCallback((user: User | null, roleIdsToAdd: number[]) => {
        if (user == null || item == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge Mitarbeiter:in ${user.fullName} zur Organisationseinheit hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new DepartmentMembershipApiService()
            .create({
                userId: user.id,
                departmentId: item.id,
                roleIds: roleIdsToAdd,
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
                    dispatch(showErrorSnackbar('Fehler beim Hinzufügen der Mitarbeiter:in zur Organisationseinheit'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    }, [dispatch, item, listControlRef, refreshPermissionsAfterMembershipChange]);

    const handleUpdateMembership = useCallback((membership: VDepartmentMembershipWithDetailsEntity | null, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        if (membership == null) {
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
                Mitarbeiter:innen der Organisationseinheit
            </Typography>

            <Typography
                sx={{
                    mb: 3,
                    maxWidth: 900,
                }}
            >
                Eine Liste der Mitarbeiter:innen, die dieser Organisationseinheit zugeordnet sind. Mitarbeiter:innen können
                unterschiedliche Rollen besitzen, die ihre Berechtigungen innerhalb der Organisationseinheit definieren.
            </Typography>

            <GenericList<VDepartmentMembershipWithDetailsEntity>
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
                defaultSortField="userFullName"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Mitgliedschaften im Zugriff"
                        description="Es wurden keine Mitgliedschaften gefunden, auf die Sie Zugriff haben. Möglicherweise wurden noch keine Mitarbeiter:innen zugeordnet oder Ihnen fehlt die Leseberechtigung für Mitgliedschaften."
                        addText="Mitarbeiter:in hinzufügen"
                        onAdd={openSelectNewMemberDialog}
                        addDisabled={!canCreateMembership || !canReadUsers || !canReadDomainRoles}
                        addDisabledTooltip={!canCreateMembership
                            ? formatMissingPermissionTooltip(Permission.DEPARTMENT_MEMBERSHIP_CREATE)
                            : !canReadUsers
                                ? formatMissingPermissionTooltip(Permission.USER_READ)
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
                userLabel={showSelectRolesDialogForUser?.fullName}
                parentId={item.id}
                parentType="orgUnit"
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
                userLabel={showSelectRolesDialogForMembership?.userFullName ?? undefined}
                parentId={item.id}
                parentType="orgUnit"
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

function buildColumns(canReadDomainRoles: boolean): Array<GridColDef<VDepartmentMembershipWithDetailsEntity>> {
    return [
        {
            field: 'userFullName',
            headerName: 'Mitarbeiter:in',
            flex: 1,
            renderCell: (params) => (
                <Box
                    display="flex"
                    flexDirection="column"
                    justifyContent="center"
                    height="100%"
                >
                    <Typography>
                        {params.row.userFullName}
                    </Typography>
                    {
                        params.row.membershipHasDeputies && (
                            <Typography
                                variant="caption"

                                color="text.secondary"
                            >
                                (Stellvertretung durch {
                                    params
                                        .row
                                        .membershipDeputies
                                        .map(snakeToCamel)
                                        .map(resolveUserName)
                                        .join(', ')
                                })
                            </Typography>
                        )
                    }
                </Box>
            )
        },
        {
            field: 'userEmail',
            headerName: 'E-Mail',
            flex: 1,
        },
        {
            field: 'role',
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

function getRowIdentifier(item: VDepartmentMembershipWithDetailsEntity): string {
    return item.userId;
}
