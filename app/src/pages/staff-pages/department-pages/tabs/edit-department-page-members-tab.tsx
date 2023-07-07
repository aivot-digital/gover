import React, { useEffect, useReducer, useState } from "react";
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
    Typography
} from "@mui/material";
import { DepartmentMembershipsService } from "../../../../services/department-memberships-service";
import { DepartmentMembership } from "../../../../models/entities/department-membership";
import { User } from "../../../../models/entities/user";
import { UsersService } from "../../../../services/users-service";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faAdd, faTrashAlt } from "@fortawesome/pro-light-svg-icons";
import { SearchInput } from "../../../../components/search-input/search-input";
import { SelectUserDialog } from "../../../../dialogs/select-user-dialog/select-user-dialog";
import { SelectFieldComponent } from "../../../../components/select-field/select-field-component";
import { UserRole, UserRoleLabels } from "../../../../data/user-role";
import {
    DialogTitleWithClose
} from "../../../../components/static-components/dialog-title-with-close/dialog-title-with-close";
import { Department } from "../../../../models/entities/department";

interface MembershipUser {
    membership: DepartmentMembership;
    user: User;
}

const fetchMemberships = async (id: number) => {
    const res: MembershipUser[] = [];
    const memberships = await DepartmentMembershipsService.list({ department: id });
    for (const membership of memberships) {
        const user = await UsersService.retrieve(membership.user);
        res.push({
            membership: membership,
            user: user,
        });
    }
    return res;
};

interface EditDepartmentPageMembersTabProps {
    department: Department;
}

export function EditDepartmentPageMembersTab ({ department }: EditDepartmentPageMembersTabProps) {
    const [showAddMembership, toggleShowAddMembership] = useReducer(p => !p, false);
    const [search, setSearch] = useState('');
    const [admins, setAdmins] = useState<User[]>();
    const [memberships, setMemberships] = useState<MembershipUser[]>();
    const [selectedUser, setSelectedUser] = useState<User>();
    const [selectedUserRole, setSelectedUserRole] = useState<UserRole>(UserRole.Editor);

    useEffect(() => {
        fetchMemberships(department.id)
            .then(setMemberships);
        UsersService
            .list({ admin: 'true' })
            .then(setAdmins)
    }, [department]);

    const handleDelete = (membershipId: number) => {
        if (memberships != null) {
            DepartmentMembershipsService.destroy(membershipId);
            setMemberships(memberships.filter(mem => mem.membership.id !== membershipId));
        }
    };

    const handleAddMembership = () => {
        if (selectedUser != null && memberships != null) {
            DepartmentMembershipsService.create({
                id: 0,
                user: selectedUser.id,
                department: department.id,
                role: selectedUserRole,
            })
                .then(mem => setMemberships([{
                    membership: mem,
                    user: selectedUser,
                }, ...memberships]));
            setSelectedUser(undefined);
            setSelectedUserRole(UserRole.Editor);
        }
    };

    const filteredMemberships = memberships == null ? undefined : memberships
        .filter(dep => dep.user.name.toLowerCase().includes(search.toLowerCase()));

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
                            sx={ { ml: 2 } }
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
                                    admins &&
                                    admins.map(user => (
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
                                    filteredMemberships.map(({ membership, user }) => (
                                        <TableRow>
                                            <TableCell>
                                                { user.name } { user.active ? '' : '(Inaktiv)' }
                                            </TableCell>
                                            <TableCell>
                                                { UserRoleLabels[membership.role] }
                                            </TableCell>
                                            <TableCell>
                                                <Tooltip
                                                    title="Löschen"
                                                >
                                                    <IconButton
                                                        color="error"
                                                        size="small"
                                                        onClick={ () => handleDelete(membership.id) }
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
                    ...(memberships ?? []).map(m => m.user.id),
                    ...(admins ?? []).map(user => user.id),
                ] }
                title="Mitarbeiter:in auswählen"
                show={ showAddMembership }
                onSelect={ user => {
                    setSelectedUser(user);
                    toggleShowAddMembership();
                } }
                onCancel={ toggleShowAddMembership }
            />

            <Dialog open={ selectedUser != null }>
                <DialogTitleWithClose onClose={ () => setSelectedUser(undefined) }>
                    Rolle für { selectedUser?.name } auswählen
                </DialogTitleWithClose>
                <DialogContent>
                    <SelectFieldComponent
                        label="Rolle"
                        required
                        value={ selectedUserRole.toString() }
                        onChange={ val => setSelectedUserRole(val != null ? parseInt(val) as UserRole : UserRole.Editor) }
                        options={ Object.keys(UserRoleLabels).map(key => ({
                            value: key,
                            label: UserRoleLabels[parseInt(key) as UserRole],
                        })) }
                    />

                    <Typography
                        variant="h6"
                        sx={ {
                            mt: 2,
                            textDecoration: selectedUserRole === UserRole.Editor ? 'underline' : undefined,
                        } }
                    >
                        Rolle: Bearbeiter
                    </Typography>
                    <Typography>
                        Bearbeiter können Formulare anlegen und bearbeiten. Außerdem können Bearbeiter eingegangene
                        Anträge einsehen,
                        bearbeiten und archivieren.
                    </Typography>

                    <Typography
                        variant="h6"
                        sx={ {
                            mt: 2,
                            textDecoration: selectedUserRole === UserRole.Publisher ? 'underline' : undefined,
                        } }
                    >
                        Rolle: Veröffentlicher
                    </Typography>
                    <Typography>
                        Veröffentlicher können Formulare anlegen, bearbeiten, eingegangene Anträge einsehen,
                        bearbeiten
                        und archivieren, sowie Formulare veröffentlichen.
                    </Typography>

                    <Typography
                        variant="h6"
                        sx={ {
                            mt: 2,
                            textDecoration: selectedUserRole === UserRole.Admin ? 'underline' : undefined,
                        } }
                    >
                        Rolle: Administrator
                    </Typography>
                    <Typography>
                        Administratoren können Formulare bearbeiten, eingegangene Anträge einsehen, bearbeiten
                        und archivieren, Anträge veröffentlichen und die Informationen eines Fachbereichs bearbeiten.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={ handleAddMembership }
                    >
                        Hinzufügen
                    </Button>
                    <Button
                        onClick={ () => setSelectedUser(undefined) }
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
