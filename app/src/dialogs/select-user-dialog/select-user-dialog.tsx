import {SelectUserDialogProps} from "./select-user-dialog-props";
import {Box, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText} from "@mui/material";
import {PropsWithChildren, useEffect, useState} from "react";
import {DialogTitleWithClose} from "../../components/static-components/dialog-title-with-close/dialog-title-with-close";
import {SearchInput} from "../../components/search-input/search-input";
import {User} from "../../models/entities/user";
import {UsersService} from "../../services/users-service";
import {AlertComponent} from "../../components/alert/alert-component";
import {Link} from "react-router-dom";

export function SelectUserDialog(props: PropsWithChildren<SelectUserDialogProps>) {
    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();

    useEffect(() => {
        UsersService
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

    const filteredUsers = users == null ? undefined : users.filter(usr => (props.userIdsToIgnore == null || !props.userIdsToIgnore.includes(usr.id)) && usr.name.toLowerCase().includes(search.toLowerCase()));

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


            <DialogContent>
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
                                    <ListItemText primary={usr.name}/>
                                </ListItemButton>
                            </ListItem>
                        ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}
