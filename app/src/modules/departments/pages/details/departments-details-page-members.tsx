import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {Box, Button, Typography} from '@mui/material';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
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
import {OrgUserRoleAssignmentsApiService} from '../../../user-roles/org-user-role-assignments-api-service';
import {isApiError} from '../../../../models/api-error';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useConfirm} from '../../../../providers/confirm-provider';
import {DepartmentEntity} from '../../entities/department-entity';
import {vDepartmentMembershipWithDetailsEntityAsUser, VDepartmentMembershipWithDetailsEntityWithRoles} from '../../entities/v-department-membership-with-details-entity';
import {ListDepartmentMembershipsWithRolesFilter, VDepartmentMembershipWithDetailsService} from '../../services/v-department-membership-with-details-service';
import {DepartmentMembershipApiService} from '../../services/department-membership-api-service';
import {resolveUserName} from '../../../users/utils/resolve-user-name';

export function DepartmentsDetailsPageMembers() {
    const dispatch = useAppDispatch();

    const {
        item,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, undefined>;

    const showConfirm = useConfirm();

    const listControlRef = useRef<ListControlRef | null>(null);
    const [showSelectNewMemberDialog, setShowSelectNewMemberDialog] = useState(false);
    const [showSelectRolesDialogForUser, setShowSelectRolesDialogForUser] = useState<User | null>(null);
    const [showSelectRolesDialogForMembership, setShowSelectRolesDialogForMembership] = useState<VDepartmentMembershipWithDetailsEntityWithRoles | null>(null);

    const fetchMembers = useCallback((options: GenericListPropsFetchOptions<VDepartmentMembershipWithDetailsEntityWithRoles>) => {
        const filters: Partial<ListDepartmentMembershipsWithRolesFilter> = {
            departmentId: item!.id,
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
            .listDepartmentMembershipsWithRoles(0, 999, options.sort as any, options.order, filters);
    }, [item]);

    const buildRowActions = useCallback((membershipItem: VDepartmentMembershipWithDetailsEntityWithRoles) => {
        return [
            {
                icon: <EditOutlinedIcon />,
                onClick: () => {
                    setShowSelectRolesDialogForMembership(membershipItem);
                },
                tooltip: membershipItem.deletedInIdp ? `Kann für gelöschte Mitarbeiter:innen nicht geändert werden` : 'Rolle der Mitarbeiter:in bearbeiten',
                disabled: membershipItem.deletedInIdp ?? undefined,
            },
            {
                icon: <DeleteOutlineOutlinedIcon />,
                onClick: () => {
                    showConfirm({
                        title: 'Mitarbeiter:in entfernen',
                        children: (
                            <>
                                <Typography>
                                    Durch das Entfernen der Mitarbeiter:in <strong>{membershipItem.fullName}</strong> aus dem Fachbereich <strong>{item?.name}</strong> verliert diese alle zugewiesenen Rollen und Berechtigungen in diesem
                                    Fachbereich.
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
                                message: `Entferne Mitarbeiter:in ${membershipItem.fullName} aus dem Fachbereich`,
                                blocking: true,
                                estimatedTime: 5000,
                            }));

                            new DepartmentMembershipApiService()
                                .destroy(membershipItem.id)
                                .then(() => {
                                    // Refresh list
                                    listControlRef.current?.refresh();
                                })
                                .catch((error) => {
                                    if (isApiError(error) && error.displayableToUser) {
                                        dispatch(showErrorSnackbar(error.message));
                                    } else {
                                        console.error(error);
                                        dispatch(showErrorSnackbar('Fehler beim Entfernen der Mitarbeiter:in aus dem Fachbereich'));
                                    }
                                })
                                .finally(() => {
                                    dispatch(setLoadingMessage(undefined));
                                });
                        });
                },
                tooltip: 'Mitarbeiter:in entfernen',
            },
        ];
    }, [dispatch, item, showConfirm, listControlRef]);

    const preSearchElements = useMemo(() => {
        if (!isEditable) {
            return undefined;
        }

        return [
            <Button
                variant="contained"
                startIcon={<AddOutlinedIcon />}
                onClick={() => setShowSelectNewMemberDialog(true)}
            >
                Mitarbeiter:in hinzufügen
            </Button>,
        ];
    }, [isEditable]);

    const handleAddMembership = useCallback((user: User | null, roleIdsToAdd: number[]) => {
        if (user == null || item == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge Mitarbeiter:in ${user.fullName} zum Fachbereich hinzu`,
            blocking: true,
            estimatedTime: 5000,
        }));

        new DepartmentMembershipApiService()
            .create({
                id: 0,
                userId: user.id,
                departmentId: item.id,
                created: new Date().toISOString(),
                updated: new Date().toISOString(),
            })
            .then((membership) => {
                const apiService = new OrgUserRoleAssignmentsApiService();
                return Promise.all(roleIdsToAdd.map((roleId) => apiService.create({
                    departmentMembershipId: membership.id,
                    userRoleId: roleId,
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
    }, [dispatch, item, listControlRef]);

    const handleUpdateMembership = useCallback((membership: VDepartmentMembershipWithDetailsEntityWithRoles | null, roleIdsToAdd: number[], userRoleAssignmentIdsToRemove: number[]) => {
        if (membership == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Aktualisiere Rollen der Mitarbeiter:in ${membership.fullName}`,
            blocking: true,
            estimatedTime: 5000,
        }));

        const apiService = new OrgUserRoleAssignmentsApiService();

        const addPromises = roleIdsToAdd
            .map((roleId) => apiService.create({
                departmentMembershipId: membership.id,
                userRoleId: roleId,
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
    }, [dispatch, listControlRef]);

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
                Mitarbeiter:innen des Fachbereichs
            </Typography>

            <Typography
                sx={{
                    mb: 3,
                    maxWidth: 900,
                }}
            >
                Eine Liste der Mitarbeiter:innen, die diesem Fachbereich zugeordnet sind. Mitarbeiter:innen können unterschiedliche Rollen besitzen, die ihre Berechtigungen innerhalb des Fachbereichs definieren.
            </Typography>

            <GenericList<VDepartmentMembershipWithDetailsEntityWithRoles>
                controlRef={listControlRef}
                filters={Filters}
                defaultFilter="active"
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={Columns}
                fetch={fetchMembers}
                getRowIdentifier={getRowIdentifier}
                searchLabel="Mitarbeiter:in suchen"
                searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                rowActionsCount={isEditable ? 2 : 0}
                rowActions={isEditable ? buildRowActions : undefined}
                defaultSortField="userId"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Mitarbeiter:innen vorhanden"
                loadingPlaceholder="Lade Mitarbeiter:innen…"
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                preSearchElements={preSearchElements}
            />

            <SelectUserDialog
                open={showSelectNewMemberDialog}
                idsToExclude={/*memberships.map((membership) => membership.userId)*/ []}
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

const Columns: Array<GridColDef<VDepartmentMembershipWithDetailsEntityWithRoles>> = [
    {
        field: 'fullName',
        headerName: 'Mitarbeiter:in',
        flex: 1,
        renderCell: (params) => {
            return resolveUserName(vDepartmentMembershipWithDetailsEntityAsUser(params.row));
        },
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
        renderCell: (params) => (
            <UserRoleChips
                roles={params.row.roles}
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
                userDeletedInIdp={params.row.deletedInIdp}
                userEnabled={params.row.enabled}
            />
        ),
    },
];

function getRowIdentifier(item: VDepartmentMembershipWithDetailsEntityWithRoles): string {
    return item.userId;
}