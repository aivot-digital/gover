import React, { useEffect, useReducer, useState } from 'react';
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
import { DepartmentMembershipsService } from '../../../../services/department-memberships-service';
import { type DepartmentMembership } from '../../../../models/entities/department-membership';
import { type User } from '../../../../models/entities/user';
import { UsersService } from '../../../../services/users-service';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faAdd, faEdit, faTrashAlt } from '@fortawesome/pro-light-svg-icons';
import { SearchInput } from '../../../../components/search-input/search-input';
import { SelectUserDialog } from '../../../../dialogs/select-user-dialog/select-user-dialog';
import { SelectFieldComponent } from '../../../../components/select-field/select-field-component';
import { UserRole, UserRoleLabels } from '../../../../data/user-role';
import {
    DialogTitleWithClose,
} from '../../../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import { type Department } from '../../../../models/entities/department';

interface MembershipUser {
    membership: DepartmentMembership;
    user: User;
}

const fetchMemberships = async (id: number): Promise<MembershipUser[]> => {
    const res: MembershipUser[] = [];
    const memberships = await DepartmentMembershipsService.list({department: id});
    for (const membership of memberships) {
        const user = await UsersService.retrieve(membership.user);
        res.push({
            membership,
            user,
        });
    }
    return res;
};

interface EditDepartmentPageMembersTabProps {
    department: Department;
}

export function EditDepartmentPageMembersTab({department}: EditDepartmentPageMembersTabProps): JSX.Element {
    const [showAddMembership, toggleShowAddMembership] = useReducer((p) => !p, false);
    const [search, setSearch] = useState('');

    const [admins, setAdmins] = useState<User[]>();
    const [memberships, setMemberships] = useState<MembershipUser[]>();

    const [selectedUser, setSelectedUser] = useState<User>();
    const [selectedMembership, setSelectedMembership] = useState<MembershipUser>();
    const [selectedUserRole, setSelectedUserRole] = useState<UserRole>(UserRole.Editor);

    useEffect(() => {
        fetchMemberships(department.id)
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
            setMemberships(memberships.filter((mem) => mem.membership.id !== membershipId));
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
                        membership: mem,
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
                .update(selectedMembership.membership.id, {
                    ...selectedMembership.membership,
                    role: selectedUserRole,
                })
                .then((mem) => {
                    setMemberships(memberships.map((m) => {
                        if (m.membership.id === mem.id) {
                            return {
                                membership: mem,
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

    const filteredAdmins = admins == null ?
        undefined :
        admins
            .filter((ad) => ad.name.toLowerCase().includes(search.toLowerCase()));

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
                        sx={ {
                            display: 'flex',
                            justifyContent: 'flex-end',
                        } }
                    >
                        <SearchInput
                            value={ search }
                            onChange={ setSearch }
                            placeholder="Mitarbeiter:in suchen..."
                        />

                        <Button
                            sx={ {
                                ml: 2,
                            } }
                            startIcon={
                                <FontAwesomeIcon icon={ faAdd }/>
                            }
                            onClick={ toggleShowAddMembership }
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
                                        <TableRow key={ user.id }>
                                            <TableCell>
                                                { user.name }
                                            </TableCell>
                                            <TableCell>
                                                Globale Administrator:in
                                            </TableCell>
                                            <TableCell/>
                                        </TableRow>
                                    ))
                                }
                                {
                                    filteredMemberships.map(({membership, user}) => (
                                        <TableRow key={ membership.id }>
                                            <TableCell>
                                                { user.name } { user.active ? '' : '(Inaktiv)' }
                                            </TableCell>
                                            <TableCell>
                                                { UserRoleLabels[membership.role] }

                                                <IconButton
                                                    color="primary"
                                                    size="small"
                                                    onClick={ () => {
                                                        setSelectedMembership({membership, user});
                                                    } }
                                                    sx={ {
                                                        ml: 1,
                                                    } }
                                                >
                                                    <FontAwesomeIcon icon={ faEdit }/>
                                                </IconButton>
                                            </TableCell>
                                            <TableCell>
                                                <Tooltip
                                                    title="Löschen"
                                                >
                                                    <IconButton
                                                        color="error"
                                                        size="small"
                                                        onClick={ () => {
                                                            handleDelete(membership.id);
                                                        } }
                                                    >
                                                        <FontAwesomeIcon icon={ faTrashAlt }/>
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
                userIdsToIgnore={ [
                    ...(memberships ?? []).map((m) => m.user.id),
                    ...(admins ?? []).map((user) => user.id),
                ] }
                title="Mitarbeiter:in auswählen"
                show={ showAddMembership }
                onSelect={ (user) => {
                    setSelectedUser(user);
                    toggleShowAddMembership();
                } }
                onCancel={ toggleShowAddMembership }
            />

            <Dialog open={ selectedUser != null }>
                <DialogTitleWithClose
                    onClose={ () => {
                        setSelectedUser(undefined);
                    } }
                >
                    Rolle für { selectedUser?.name } auswählen
                </DialogTitleWithClose>
                <DialogContent>
                    <SelectFieldComponent
                        label="Rolle"
                        required
                        value={ selectedUserRole.toString() }
                        onChange={ (val) => {
                            setSelectedUserRole(val != null ? parseInt(val) as UserRole : UserRole.Editor);
                        } }
                        options={ Object.keys(UserRoleLabels).map((key) => ({
                            value: key,
                            label: UserRoleLabels[parseInt(key) as UserRole],
                        })) }
                    />

                    <Typography
                        sx={ {
                            mt: 2,
                        } }
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
                        onClick={ handleAddMembership }
                    >
                        Hinzufügen
                    </Button>
                    <Button
                        onClick={ () => {
                            setSelectedUser(undefined);
                        } }
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={ selectedMembership != null }>
                <DialogTitleWithClose
                    onClose={ () => {
                        setSelectedMembership(undefined);
                    } }
                >
                    Rolle für { selectedUser?.name } auswählen
                </DialogTitleWithClose>
                <DialogContent>
                    <SelectFieldComponent
                        label="Rolle"
                        required
                        value={ selectedUserRole.toString() }
                        onChange={ (val) => {
                            setSelectedUserRole(val != null ? parseInt(val) as UserRole : UserRole.Editor);
                        } }
                        options={ Object.keys(UserRoleLabels).map((key) => ({
                            value: key,
                            label: UserRoleLabels[parseInt(key) as UserRole],
                        })) }
                    />

                    <Typography
                        sx={ {
                            mt: 2,
                        } }
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
                        onClick={ handleUpdateMembership }
                    >
                        Speichern
                    </Button>
                    <Button
                        onClick={ () => {
                            setSelectedMembership(undefined);
                        } }
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
