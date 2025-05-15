import React, {useContext, useEffect, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {DepartmentMembershipFilters, DepartmentMembershipsApiService} from '../../department-memberships-api-service';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {DepartmentMembership} from '../../models/department-membership';
import {UserRole, UserRoleLabels} from '../../../../data/user-role';
import {Box, Button, Typography} from '@mui/material';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useConfirmDialog} from '../../../../hooks/use-confirm-dialog';
import {ConfirmDialogV2} from '../../../../dialogs/confirm-dialog/confirm-dialog-v2';
import {useApi} from '../../../../hooks/use-api';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {SelectUserDialog} from '../../../users/dialogs/select-user-dialog';
import {resolveUserName} from '../../../users/utils/resolve-user-name';
import {User} from '../../../users/models/user';
import {Department} from '../../models/department';
import {DepartmentMembershipResponseDTO} from '../../dtos/department-membership-response-dto';
import Chip from "@mui/material/Chip";

export function DepartmentsDetailsPageMembers() {
    const dispatch = useAppDispatch();
    const api = useApi();

    const {
        confirmOptions: confirmDeleteOptions,
        hideConfirmDialog: hideConfirmDeleteDialog,
        showConfirmDialog: showConfirmDeleteDialog,
    } = useConfirmDialog();

    const {
        confirmOptions: departmentMembershipConfirmOptions,
        hideConfirmDialog: hideDepartmentMembershipConfirmDialog,
        showConfirmDialog: showDepartmentMembershipConfirmDialog,
    } = useConfirmDialog<DepartmentMembership>();

    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Department, undefined>;

    const [showSelectNewMemberDialog, setShowSelectNewMemberDialog] = useState(false);

    const [memberships, setMemberships] = useState<DepartmentMembershipResponseDTO[]>([]);

    useEffect(() => {
        if (item == null) {
            setMemberships([]);
        } else {
            new DepartmentMembershipsApiService(api)
                .listAll({
                    departmentId: item.id,
                })
                .then((data) => {
                    setMemberships(data.content);
                });
        }
    }, [item]);

    const handleAddMembership = (
        selectedUser: User,
        selectedUserRole: UserRole,
    ): void => {
        if (item == null) {
            return;
        }

        new DepartmentMembershipsApiService(api)
            .create({
                userId: selectedUser.id,
                departmentId: item.id,
                role: selectedUserRole,
            })
            .then(() => {
                location.reload();
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Mitgliedschaft konnte nicht angelegt werden'));
            });
    };

    const handleDelete = (membership: DepartmentMembership): void => {
        if (item == null) {
            return;
        }

        showConfirmDeleteDialog({
            title: 'Mitgliedschaft beenden',
            state: {},
            onRender: () => (
                <Typography
                    variant="body1"
                >
                    Mitgliedschaft von <strong>{membership.userFirstName} {membership.userLastName}</strong> im Fachbereich <strong>{item.name}</strong> beenden?
                </Typography>
            ),
            onCancel: () => hideConfirmDeleteDialog(),
            onConfirm: () => {
                new DepartmentMembershipsApiService(api)
                    .destroy(membership.id)
                    .then(() => {
                        location.reload();
                    })
                    .catch((error) => {
                        console.error(error);
                        dispatch(showErrorSnackbar('Mitgliedschaft konnte nicht beendet werden'));
                    });
            },
        });
    };

    const handleUpdateMembership = (
        selectedMembership: DepartmentMembership,
        selectedUserRole: UserRole,
    ): void => {
        new DepartmentMembershipsApiService(api)
            .update(selectedMembership!.id, {
                ...selectedMembership,
                role: selectedUserRole,
            })
            .then(() => {
                location.reload();
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Mitgliedschaft konnte nicht aktualisiert werden'));
            });
    };

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Mitarbeiter:innen des Fachbereichs
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Liste der Mitarbeiter:innen, die diesem Fachbereich zugeordnet sind. Mitarbeiter:innen können unterschiedliche Rollen besitzen, die ihre Berechtigungen innerhalb des Fachbereichs definieren.
            </Typography>

            <GenericList<DepartmentMembershipResponseDTO>
                filters={[
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
                ]}
                defaultFilter="active"
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={[
                    {
                        field: 'userId',
                        headerName: 'Mitarbeiter:in',
                        flex: 1,
                        renderCell: (params) => {
                            return `${params.row.userFirstName} ${params.row.userLastName}`;
                        },
                    },
                    {
                        field: 'userEmail',
                        headerName: 'E-Mail',
                        flex: 1,
                    },
                    {
                        field: 'role',
                        headerName: 'Rolle im Fachbereich',
                        flex: 1,
                        renderCell: (params) => {
                            return UserRoleLabels[params.row.role];
                        },
                    },
                    {
                        field: 'enabled',
                        headerName: 'Status',
                        type: 'boolean',
                        renderCell: (params) => (
                            params.row?.userDeletedInIdp ?
                                <Chip
                                    label="Gelöscht"
                                    color="error"
                                    variant="outlined"
                                    size={'small'}
                                    title="Diese Mitarbeiter:in wurde im Identity Provider gelöscht und kann sich nicht anmelden."
                                /> : (
                                    params.row?.userEnabled ?
                                        <Chip
                                            label="Aktiv"
                                            variant="outlined"
                                            size={'small'}
                                        /> :
                                        <Chip
                                            label="Inaktiv"
                                            color="warning"
                                            variant="outlined"
                                            size={'small'}
                                            title="Diese Mitarbeiter:in ist inaktiv und kann sich nicht anmelden."
                                        />
                                )
                        ),
                    },
                ]}
                fetch={(options) => {
                    const filters: Partial<DepartmentMembershipFilters> = {
                        departmentId: item.id,
                        userName: options.search,
                    };

                    switch (options.filter) {
                        case 'active':
                            filters.deletedInIdp = false;
                            filters.userEnabled = true;
                            break;
                        case 'inactive':
                            filters.deletedInIdp = false;
                            filters.userEnabled = false;
                            break;
                        case 'deleted':
                            filters.deletedInIdp = true;
                            filters.userEnabled = undefined;
                            break;
                    }

                    return new DepartmentMembershipsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            filters,
                        );
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Mitarbeiter:in suchen"
                searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                rowActions={(membershipItem) => [
                    {
                        icon: <EditOutlinedIcon />,
                        onClick: () => {
                            showDepartmentMembershipConfirmDialog({
                                title: 'Rolle festlegen',
                                state: membershipItem,
                                onRender: (membership, update) => (
                                    <>
                                        <Typography>
                                            Rolle für {resolveUserName({
                                            id: membership.userId,
                                            firstName: membership.userFirstName,
                                            lastName: membership.userLastName,
                                            email: membership.userEmail,
                                            enabled: true,
                                            verified: true,
                                            deletedInIdp: false,
                                            globalAdmin: false,
                                            fullName: membership.userFirstName + ' ' + membership.userLastName,
                                        })} im Fachbereich {item.name} festlegen
                                        </Typography>

                                        <SelectFieldComponent
                                            label="Rolle im Fachbereich"
                                            value={membership.role.toString()}
                                            onChange={(value) => {
                                                update({
                                                    role: parseInt(value ?? '0'),
                                                });
                                            }}
                                            required={true}
                                            options={[
                                                {
                                                    label: UserRoleLabels[UserRole.Editor],
                                                    value: UserRole.Editor.toString(),
                                                },
                                                {
                                                    label: UserRoleLabels[UserRole.Publisher],
                                                    value: UserRole.Publisher.toString(),
                                                },
                                                {
                                                    label: UserRoleLabels[UserRole.Admin],
                                                    value: UserRole.Admin.toString(),
                                                },
                                            ]}
                                        />
                                    </>
                                ),
                                onCancel: () => hideDepartmentMembershipConfirmDialog(),
                                onConfirm: (membership) => {
                                    handleUpdateMembership(membership, membership.role);
                                },
                            });
                        },
                        tooltip: membershipItem.userDeletedInIdp ? `Kann für gelöschte Mitarbeiter:innen nicht geändert werden` : 'Rolle der Mitarbeiter:in bearbeiten',
                        disabled: membershipItem.userDeletedInIdp ?? undefined,
                    },
                    {
                        icon: <DeleteOutlineOutlinedIcon />,
                        onClick: () => {
                            handleDelete(membershipItem);
                        },
                        tooltip: 'Mitarbeiter:in entfernen',
                    },
                ]}
                defaultSortField="userId"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Mitarbeiter:innen vorhanden"
                loadingPlaceholder="Lade Mitarbeiter:innen…"
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                preSearchElements={[
                    <Button
                        variant="contained"
                        startIcon={<AddOutlinedIcon />}
                        onClick={() => setShowSelectNewMemberDialog(true)}
                    >
                        Mitarbeiter:in hinzufügen
                    </Button>,
                ]}
            />

            <ConfirmDialogV2
                options={confirmDeleteOptions}
            />

            <ConfirmDialogV2
                options={departmentMembershipConfirmOptions}
            />

            <SelectUserDialog
                open={showSelectNewMemberDialog}
                idsToExclude={memberships.map((membership) => membership.userId)}
                onClose={() => setShowSelectNewMemberDialog(false)}
                onSelect={(user) => {
                    const membership: DepartmentMembership = {
                        id: 0,
                        userId: user.id,
                        departmentId: item.id,
                        departmentName: item.name,
                        role: UserRole.Editor,
                        userEmail: user.email ?? '',
                        userFirstName: user.firstName ?? '',
                        userLastName: user.lastName ?? '',
                    };

                    showDepartmentMembershipConfirmDialog({
                        title: 'Rolle festlegen',
                        state: membership,
                        onRender: (membership, update) => (
                            <>
                                <Typography>
                                    Rolle für {resolveUserName(user)} im Fachbereich {item.name} festlegen
                                </Typography>

                                <SelectFieldComponent
                                    label="Rolle im Fachbereich"
                                    value={membership.role.toString()}
                                    onChange={(value) => {
                                        update({
                                            role: parseInt(value ?? '0'),
                                        });
                                    }}
                                    required={true}
                                    options={[
                                        {
                                            label: UserRoleLabels[UserRole.Editor],
                                            value: UserRole.Editor.toString(),
                                        },
                                        {
                                            label: UserRoleLabels[UserRole.Publisher],
                                            value: UserRole.Publisher.toString(),
                                        },
                                        {
                                            label: UserRoleLabels[UserRole.Admin],
                                            value: UserRole.Admin.toString(),
                                        },
                                    ]}
                                />
                            </>
                        ),
                        onCancel: () => hideDepartmentMembershipConfirmDialog(),
                        onConfirm: (membership) => {
                            handleAddMembership(user, membership.role);
                        },
                    });

                    setShowSelectNewMemberDialog(false);
                }}
            />
        </Box>
    );
}