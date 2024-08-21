import {SelectUserDialogProps} from './select-user-dialog-props';
import {Box, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText} from '@mui/material';
import {PropsWithChildren, useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from '../../components/search-input/search-input';
import {getFullName, User} from '../../models/entities/user';
import {AlertComponent} from '../../components/alert/alert-component';
import {Link} from 'react-router-dom';
import {useApi} from '../../hooks/use-api';
import {useUsersApi} from '../../hooks/use-users-api';

function filterUser(user: User, userIdsToIgnore: string[] | undefined, search: string) {
    return (userIdsToIgnore == null || !userIdsToIgnore.includes(user.id)) && getFullName(user).toLowerCase().includes(search.toLowerCase());
}

export function SelectUserDialog(props: PropsWithChildren<SelectUserDialogProps>) {
    const api = useApi();
    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();

    useEffect(() => {
        useUsersApi(api)
            .list()
            .then(setUsers);
    }, []);

    const handleClose = () => {
        setSearch('');
        props.onCancel();
    };

    const handleSelect = (user: User) => {
        setSearch('');
        props.onSelect(user);
    };

    const filteredUsers = users == null ? undefined : users.filter(usr => filterUser(usr, props.userIdsToIgnore, search));

    return (
        <Dialog
            fullWidth
            open={props.show}
            onClose={handleClose}
        >
            <DialogTitleWithClose
                onClose={handleClose}
            >
                {props.title}
            </DialogTitleWithClose>


            <DialogContent tabIndex={0}>
                {
                    props.children != null &&
                    <Box sx={{mb: 2}}>
                        {props.children}
                    </Box>
                }

                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder="Suchen..."
                />

                {
                    filteredUsers?.length === 0 &&
                    <AlertComponent
                        title="Keine Benutzer:innen vorhanden"
                        color="info"
                    >
                        Es sind keine Benutzer:innen vorhanden die Sie hier auswählen könnten.
                        Legen Sie neue Benutzer:innen in der <Link to="/users">Benutzerverwaltung</Link> an.
                    </AlertComponent>
                }

                <List>
                    {
                        filteredUsers != null &&
                        filteredUsers.map(usr => (
                            <ListItem
                                key={usr.id}
                            >
                                <ListItemButton
                                    onClick={() => handleSelect(usr)}
                                >
                                    <ListItemText
                                        primary={getFullName(usr)}
                                    />
                                </ListItemButton>
                            </ListItem>
                        ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}
