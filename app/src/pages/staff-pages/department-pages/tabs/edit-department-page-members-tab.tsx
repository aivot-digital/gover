import React, {useEffect, useReducer, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, IconButton, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography} from '@mui/material';
import {DepartmentMembershipsService} from '../../../../services/department-memberships-service';
import {type DepartmentMembership} from '../../../../models/entities/department-membership';
import {type User} from '../../../../models/entities/user';
import {UsersService} from '../../../../services/users-service';
import {SearchInput} from '../../../../components/search-input/search-input';
import {SelectUserDialog} from '../../../../dialogs/select-user-dialog/select-user-dialog';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {UserRole, UserRoleLabels} from '../../../../data/user-role';
import {DialogTitleWithClose} from '../../../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../../../models/entities/department';
import {type DepartmentMembershipWithUserDto} from '../../../../models/dtos/department-membership-with-user-dto';
import {DepartmentsService} from '../../../../services/departments-service';
import {filterItems} from '../../../../utils/filter-items';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DriveFileRenameOutlineOutlinedIcon from '@mui/icons-material/DriveFileRenameOutlineOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';

interface MembershipUser {
    membership: DepartmentMembership;
    user: User;
}

interface EditDepartmentPageMembersTabProps {
    department: Department;
}

export function EditDepartmentPageMembersTab({department}: EditDepartmentPageMembersTabProps): JSX.Element {
    const [showAddMembership, toggleShowAddMembership] = useReducer((p) => !p, false);
    const [search, setSearch] = useState('');

    const [admins, setAdmins] = useState<User[]>();
    const [memberships, setMemberships] = useState<DepartmentMembershipWithUserDto[]>();

    const [selectedUser, setSelectedUser] = useState<User>();
    const [selectedMembership, setSelectedMembership] = useState<DepartmentMembershipWithUserDto>();
    const [selectedUserRole, setSelectedUserRole] = useState<UserRole>(UserRole.Editor);

    useEffect(() => {
        DepartmentsService
            .listMemberships(department.id)
            .then(setMemberships);

        UsersService
            .list({
                onlyAdmins: 'true',
            })
            .then(setAdmins);
    }, [department]);

    const handleDelete = (membershipId: number): void => {
        if (memberships != null) {
            DepartmentMembershipsService
                .destroy(membershipId);
            setMemberships(memberships.filter((mem) => mem.id !== membershipId));
        }
    };

    const handleAddMembership = (): void => {
        if (selectedUser != null && memberships != null) {
            DepartmentMembershipsService
                .create({
                    id: 0,
                    user: selectedUser.id,
                    department: department.id,
                    role: selectedUserRole,
                })
                .then((mem) => {
                    setMemberships([{
                        id: mem.id,
                        role: mem.role,
                        department: mem.department,
                        user: selectedUser,
                    }, ...memberships]);
                });
            setSelectedUser(undefined);
            setSelectedUserRole(UserRole.Editor);
        }
    };

    const handleUpdateMembership = (): void => {
        if (selectedMembership != null && memberships != null) {
            DepartmentMembershipsService
                .update(selectedMembership.id, {
                    id: selectedMembership.id,
                    user: selectedMembership.user.id,
                    department: selectedMembership.department,
                    role: selectedUserRole,
                })
                .then((mem) => {
                    setMemberships(memberships.map((m) => {
                        if (m.id === mem.id) {
                            return {
                                id: mem.id,
                                role: mem.role,
                                department: mem.department,
                                user: m.user,
                            };
                        }
                        return m;
                    }));
                });
            setSelectedMembership(undefined);
            setSelectedUserRole(UserRole.Editor);
        }
    };

    const filteredAdmins = filterItems(admins, 'name', search);

    const filteredMemberships = memberships == null ?
        undefined :
        memberships
            .filter((dep) => dep.user.name.toLowerCase().includes(search.toLowerCase()));

    return (
        <>
            {
                filteredMemberships == null &&
                <Skeleton variant="rectangular"/>
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
                                <AddOutlinedIcon/>
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
                                        Name
                                    </TableCell>
                                    <TableCell>
                                        Rolle
                                    </TableCell>
                                    <TableCell/>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {
                                    filteredAdmins?.map((user) => (
                                        <TableRow key={user.id}>
                                            <TableCell>
                                                {user.name}
                                            </TableCell>
                                            <TableCell>
                                                Globale Administrator:in
                                            </TableCell>
                                            <TableCell/>
                                        </TableRow>
                                    ))
                                }
                                {
                                    filteredMemberships.map((mem) => (
                                        <TableRow key={mem.id}>
                                            <TableCell>
                                                {mem.user.name} {mem.user.active ? '' : '(Inaktiv)'}
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
                                                    <DriveFileRenameOutlineOutlinedIcon/>
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
                                                            handleDelete(mem.id);
                                                        }}
                                                    >
                                                        <DeleteOutlinedIcon/>
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

            <SelectUserDialog
                userIdsToIgnore={[
                    ...(memberships ?? []).map((m) => m.user.id),
                    ...(admins ?? []).map((user) => user.id),
                ]}
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
                    Rolle für {selectedUser?.name} auswählen
                </DialogTitleWithClose>
                <DialogContent>
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
                        href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch"
                        target="_blank"
                    >Benutzerhandbuchs</a>.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleAddMembership}
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
                    Rolle für {selectedUser?.name} auswählen
                </DialogTitleWithClose>
                <DialogContent>
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
                        href="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch"
                        target="_blank"
                    >Benutzerhandbuchs</a>.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleUpdateMembership}
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
        </>
    );
}
