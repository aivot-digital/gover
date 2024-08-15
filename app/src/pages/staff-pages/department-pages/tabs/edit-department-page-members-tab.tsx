import React, {useEffect, useReducer, useState} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    IconButton,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from '@mui/material';
import {DepartmentMembershipWithUser} from '../../../../models/entities/department-membership';
import {getFullName, type User} from '../../../../models/entities/user';
import {SearchInput} from '../../../../components/search-input/search-input';
import {SelectUserDialog} from '../../../../dialogs/select-user-dialog/select-user-dialog';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {UserRole, UserRoleLabels} from '../../../../data/user-role';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../../../models/entities/department';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DriveFileRenameOutlineOutlinedIcon from '@mui/icons-material/DriveFileRenameOutlineOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {useApi} from '../../../../hooks/use-api';
import {useMembershipsApi} from '../../../../hooks/use-memberships-api';
import CheckOutlinedIcon from '@mui/icons-material/CheckOutlined';
import ClearOutlinedIcon from '@mui/icons-material/ClearOutlined';
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";

interface EditDepartmentPageMembersTabProps {
    department: Department;
}

export function EditDepartmentPageMembersTab({department}: EditDepartmentPageMembersTabProps): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [showAddMembership, toggleShowAddMembership] = useReducer((p) => !p, false);
    const [search, setSearch] = useState('');

    const [memberships, setMemberships] = useState<DepartmentMembershipWithUser[]>();

    const [selectedUser, setSelectedUser] = useState<User>();
    const [selectedMembership, setSelectedMembership] = useState<DepartmentMembershipWithUser>();
    const [selectedUserRole, setSelectedUserRole] = useState<UserRole>(UserRole.Editor);

    const [confirmMemberDelete, setConfirmMemberDelete] = useState<() => void>();

    useEffect(() => {
        useMembershipsApi(api)
            .listWithUsers({department: department.id})
            .then(setMemberships);
    }, [department]);

    const handleDelete = (membershipId: number): void => {
        setConfirmMemberDelete(undefined);
        if (memberships != null) {
            useMembershipsApi(api)
                .destroy(membershipId)
                .then(() => {
                    setMemberships(memberships.filter((mem) => mem.id !== membershipId));
                })
                .catch((err) => {
                    if (err.status === 409) {
                        dispatch(showErrorSnackbar('Die Mitarbeiter:in kann nicht entfernt werden, da sie noch mindestens einem offenen Antrag zugeordnet ist.'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Mitarbeiter:in kann nicht entfernt werden, bitte probieren Sie es später erneut.'));
                    }
                });
        }
    };

    const handleAddMembership = (): void => {
        if (selectedUser != null && memberships != null) {
            useMembershipsApi(api)
                .save({
                    id: 0,
                    userId: selectedUser.id,
                    departmentId: department.id,
                    role: selectedUserRole,
                })
                .then((mem) => {
                    setMemberships([{
                        id: mem.id,
                        role: mem.role,
                        departmentId: mem.departmentId,
                        userId: mem.userId,
                        user: selectedUser,
                    }, ...memberships]);
                });
            setSelectedUser(undefined);
            setSelectedUserRole(UserRole.Editor);
        }
    };

    const handleUpdateMembership = (): void => {
        if (selectedMembership != null && memberships != null) {
            useMembershipsApi(api)
                .save({
                    id: selectedMembership.id,
                    userId: selectedMembership.userId,
                    departmentId: selectedMembership.departmentId,
                    role: selectedUserRole,
                })
                .then((mem) => {
                    setMemberships(memberships.map((m) => {
                        if (m.id === mem.id) {
                            return {
                                id: mem.id,
                                role: mem.role,
                                departmentId: mem.departmentId,
                                userId: m.userId,
                                user: selectedMembership.user,
                            };
                        }
                        return m;
                    }));
                });
            setSelectedMembership(undefined);
            setSelectedUserRole(UserRole.Editor);
        }
    };

    const filteredMemberships = memberships == null ?
        undefined :
        memberships
            .filter((dep) => (
                getFullName(dep.user).toLowerCase().includes(search.toLowerCase())
            ));

    return (
        <>
            {
                filteredMemberships == null &&
                <Skeleton variant="rectangular" />
            }

            {
                filteredMemberships != null &&
                <Box>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'flex-end',
                        }}
                    >
                        <SearchInput
                            value={search}
                            onChange={setSearch}
                            placeholder="Mitarbeiter:in suchen..."
                        />

                        <Button
                            sx={{
                                ml: 2,
                            }}
                            startIcon={
                                <AddOutlinedIcon />
                            }
                            onClick={toggleShowAddMembership}
                        >
                            Mitarbeiter:in hinzufügen
                        </Button>
                    </Box>

                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>
                                        Aktiv
                                    </TableCell>
                                    <TableCell>
                                        Name
                                    </TableCell>
                                    <TableCell>
                                        Rolle
                                    </TableCell>
                                    <TableCell />
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {
                                    filteredMemberships.map((mem) => (
                                        <TableRow key={mem.id}>
                                            <TableCell>
                                                {
                                                    mem.user.enabled ?
                                                        <CheckOutlinedIcon /> :
                                                        <ClearOutlinedIcon />
                                                }
                                            </TableCell>
                                            <TableCell>
                                                {getFullName(mem.user)}
                                            </TableCell>
                                            <TableCell>
                                                {UserRoleLabels[mem.role]}

                                                <IconButton
                                                    color="primary"
                                                    size="small"
                                                    onClick={() => {
                                                        setSelectedMembership(mem);
                                                    }}
                                                    sx={{
                                                        ml: 1,
                                                    }}
                                                >
                                                    <DriveFileRenameOutlineOutlinedIcon />
                                                </IconButton>
                                            </TableCell>
                                            <TableCell>
                                                <Tooltip
                                                    title="Löschen"
                                                >
                                                    <IconButton
                                                        color="error"
                                                        size="small"
                                                        onClick={() => {
                                                            setConfirmMemberDelete(() => () => handleDelete(mem.id))
                                                        }}
                                                    >
                                                        <DeleteOutlinedIcon />
                                                    </IconButton>
                                                </Tooltip>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            }

            {
                filteredMemberships != null && filteredMemberships.length === 0 &&
                <Typography
                    sx={{
                        mt: 2,
                    }}
                >
                    Keine Mitarbeiter:innen vorhanden.
                </Typography>
            }

            <SelectUserDialog
                userIdsToIgnore={(memberships ?? []).map((m) => m.user.id)}
                title="Mitarbeiter:in auswählen"
                show={showAddMembership}
                onSelect={(user) => {
                    setSelectedUser(user);
                    toggleShowAddMembership();
                }}
                onCancel={toggleShowAddMembership}
            />

            <Dialog open={selectedUser != null}>
                <DialogTitleWithClose
                    onClose={() => {
                        setSelectedUser(undefined);
                    }}
                >
                    Rolle für {getFullName(selectedUser)} auswählen
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    <SelectFieldComponent
                        label="Rolle"
                        required
                        value={selectedUserRole.toString()}
                        onChange={(val) => {
                            setSelectedUserRole(val != null ? parseInt(val) as UserRole : UserRole.Editor);
                        }}
                        options={Object.keys(UserRoleLabels).map((key) => ({
                            value: key,
                            label: UserRoleLabels[parseInt(key) as UserRole],
                        }))}
                    />

                    <Typography
                        sx={{
                            mt: 2,
                        }}
                    >
                        Mehr Informationen zu den Rollen finden Sie
                        im <a
                        href="https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/konzepte/rollenkonzept"
                        target="_blank"
                    >Rollenkonzept</a> des <a
                        href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home"
                        target="_blank"
                    >Benutzerhandbuchs</a>.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleAddMembership}
                        variant="contained"
                    >
                        Hinzufügen
                    </Button>
                    <Button
                        onClick={() => {
                            setSelectedUser(undefined);
                        }}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={selectedMembership != null}>
                <DialogTitleWithClose
                    onClose={() => {
                        setSelectedMembership(undefined);
                    }}
                >
                    Rolle für {getFullName(selectedUser)} auswählen
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    <SelectFieldComponent
                        label="Rolle"
                        required
                        value={selectedUserRole.toString()}
                        onChange={(val) => {
                            setSelectedUserRole(val != null ? parseInt(val) as UserRole : UserRole.Editor);
                        }}
                        options={Object.keys(UserRoleLabels).map((key) => ({
                            value: key,
                            label: UserRoleLabels[parseInt(key) as UserRole],
                        }))}
                    />

                    <Typography
                        sx={{
                            mt: 2,
                        }}
                    >
                        Mehr Informationen zu den Rollen finden Sie
                        im <a
                        href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/konzepte/rollenkonzept"
                        target="_blank"
                    >Rollenkonzept</a> des <a
                        href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home"
                        target="_blank"
                    >Benutzerhandbuchs</a>.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleUpdateMembership}
                        variant="contained"
                    >
                        Speichern
                    </Button>
                    <Button
                        onClick={() => {
                            setSelectedMembership(undefined);
                        }}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
            <ConfirmDialog
                title="Mitarbeiter:in entfernen"
                onCancel={() => {
                    setConfirmMemberDelete(undefined);
                }}
                onConfirm={confirmMemberDelete}
            >
                Sind Sie sicher, dass Sie diese Mitarbeiter:in wirklich aus dem Fachbereich entfernen wollen? Die Mitarbeiter:in kann anschließend nicht mehr auf diesen Fachbereich zugreifen.
            </ConfirmDialog>
        </>
    );
}
