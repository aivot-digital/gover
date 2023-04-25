import {
    Box,
    Button,
    Divider,
    IconButton,
    Menu,
    MenuItem,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow
} from '@mui/material';
import {SearchInput} from '../../../../../components/search-input/search-input';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faAdd, faBars} from '@fortawesome/pro-light-svg-icons';
import {UsersService} from '../../../../../services/users.service';
import React, {useEffect, useState} from 'react';
import {User} from '../../../../../models/entities/user';
import {EditUserDialog} from '../../dialogs/edit-user-dialog/edit-user.dialog';
import {MakeOptional} from '../../../../../types';
import {UserRole} from '../../../../../data/user-role';
import strings from './user-list-strings.json';
import {Localization} from '../../../../../locale/localization';

const __ = Localization(strings);

export function UserList() {
    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>([]);
    const [userToEdit, setUserToEdit] = useState<MakeOptional<User, 'id'>>();

    const fetchUsers = () => {
        UsersService
            .list()
            .then(response => setUsers(response._embedded.users));
    };

    const handleUserSave = async (user?: MakeOptional<User, 'id'>, password?: string) => {
        let updatedUser: User | null = null;
        if (user != null) {
            if (user.id == null) {
                updatedUser = await UsersService.create(user);
            } else {
                updatedUser = await UsersService.update(user.id, user as User);
            }
        }
        if (password != null) {
            if (updatedUser != null) {
                await UsersService.setPassword(password, updatedUser.id);
            } else {
                if (userToEdit != null && userToEdit.id != null) {
                    await UsersService.setPassword(password, userToEdit.id);
                }
            }
        }
        setUserToEdit(undefined);
    };

    const handleUserDialogClose = () => {
        setUserToEdit(undefined);
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const filteredUsers = users
        .filter(user => user.name.toLowerCase().includes(search.toLowerCase()))
        .sort((userA, userB) => userA.email.toLowerCase().localeCompare(userB.email.toLowerCase()));

    return (
        <>
            <Box sx={{display: 'flex', justifyContent: 'flex-end'}}>
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder={__.searchUserPlaceholder}
                />
                <Button
                    sx={{ml: 2}}
                    startIcon={
                        <FontAwesomeIcon icon={faAdd}/>
                    }
                    onClick={() => {
                        setUserToEdit({
                            name: '',
                            email: '',
                            role: UserRole.Editor,
                            active: true,
                        });
                    }}
                >
                    {__.addUserLabel}
                </Button>
            </Box>

            <Table sx={{mt: 2}}>
                <TableHead>
                    <TableRow>
                        <TableCell>
                            {__.colStatusLabel}
                        </TableCell>
                        <TableCell>
                            {__.colNameLabel}
                        </TableCell>
                        <TableCell>
                            {__.colMailLabel}
                        </TableCell>
                        <TableCell>
                            {__.colRoleLabel}
                        </TableCell>
                        <TableCell>
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {
                        filteredUsers.map(user => (
                            <UserRow
                                key={user.id}
                                user={user}
                                fetchUsers={fetchUsers}
                                editUser={setUserToEdit}
                            />
                        ))
                    }
                </TableBody>
            </Table>

            {
                userToEdit &&
                <EditUserDialog
                    user={userToEdit}
                    open={true}
                    onSave={handleUserSave}
                    onClose={handleUserDialogClose}
                />
            }
        </>
    );
}

function UserRow({user, fetchUsers, editUser}: { user: User, fetchUsers: () => void, editUser: (user: User) => void }) {
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    const activateUser = () => {
        UsersService.update(user.id, {
            ...user,
            active: true,
        }).then(fetchUsers);
        setMenuAnchorEl(undefined);
    };

    const deactivateUser = () => {
        UsersService.update(user.id, {
            ...user,
            active: false,
        }).then(fetchUsers);
        setMenuAnchorEl(undefined);
    };

    const deleteUser = () => {
        const confirmed = window.confirm(`${__.deleteUserQuestion} ${user.email}`);
        if (confirmed) {
            UsersService.destroy(user.id)
                .then(fetchUsers);
        }
        setMenuAnchorEl(undefined);
    };

    const handleEditUser = () => {
        editUser(user);
        setMenuAnchorEl(undefined);
    };

    return (
        <TableRow>
            <TableCell>
                {user.active ? __.enabledState : __.disabledState}
            </TableCell>
            <TableCell>
                {user.name}
            </TableCell>
            <TableCell>
                {user.email}
            </TableCell>
            <TableCell>
                {user.role}
            </TableCell>
            <TableCell>
                <IconButton
                    onClick={event => {
                        setMenuAnchorEl(event.currentTarget);
                    }}
                >
                    <FontAwesomeIcon icon={faBars}/>
                </IconButton>
                <Menu
                    anchorEl={menuAnchorEl}
                    open={menuAnchorEl != null}
                    onClose={() => {
                        setMenuAnchorEl(undefined);
                    }}
                >
                    {
                        user.active &&
                        <MenuItem
                            onClick={deactivateUser}
                        >
                            {__.disableUserLabel}
                        </MenuItem>
                    }
                    {
                        !user.active &&
                        <MenuItem
                            onClick={activateUser}
                        >
                            {__.enableUserLabel}
                        </MenuItem>
                    }
                    {
                        !user.active &&
                        <MenuItem
                            onClick={deleteUser}
                        >
                            {__.deleteUserLabel}
                        </MenuItem>
                    }
                    <Divider/>
                    <MenuItem
                        onClick={handleEditUser}
                    >
                        {__.changeUserLabel}
                    </MenuItem>
                </Menu>
            </TableCell>
        </TableRow>
    );
}
