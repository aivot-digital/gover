import React, {FormEvent, useState} from 'react';
import {DialogProps} from '@mui/material/Dialog/Dialog';
import {User} from '../../../../../models/user';
import {
    Button,
    Dialog, DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Typography
} from '@mui/material';
import {UserRole, UserRoleLabels} from '../../../../../data/user-role';
import {MakeOptional} from '../../../../../types';
import strings from './edit-user-dialog-strings.json';
import {Localization} from '../../../../../locale/localization';

const __ = Localization(strings);

interface EditUserDialogProps extends DialogProps {
    user: MakeOptional<User, 'id'>;
    onSave: (user?: MakeOptional<User, 'id'>, password?: string) => void;
    onClose: () => void;
}

export function EditUserDialog(props: EditUserDialogProps) {
    const {user, onSave, onClose, ...dialogProps} = props;

    const [editedUser, setEditedUser] = useState<MakeOptional<User, 'id'>>();
    const [password, setPassword] = useState('');
    const [passwordRetyped, setPasswordRetyped] = useState('');

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (password.length > 0) {
            if (password.length < 6) {
                alert(__.passwordLengthError);
                return false;
            }
            if (password !== passwordRetyped) {
                alert(__.passwordMatchError);
                return false;
            }
        }

        if (editedUser != null || password.length > 0) {
            props.onSave(editedUser, password.length > 0 ? password : undefined);
        } else {
            props.onClose();
        }

        return false;
    }

    return (
        <Dialog {...dialogProps}>
            <DialogTitle>
                {
                    user.id != null ? __.editTitle : __.addTitle
                }
            </DialogTitle>

            <form onSubmit={handleSubmit}>
                <DialogContent>
                    <Typography variant="subtitle1">
                        {__.generalInformationTitle}
                    </Typography>

                    <TextField
                        label={__.nameLabel}
                        placeholder={__.namePlaceholder}
                        value={(editedUser ?? user).name}
                        required
                        onChange={(event) => {
                            setEditedUser({
                                ...(editedUser ?? user),
                                name: event.target.value ?? '',
                            });
                        }}
                        onBlur={() => {
                            if (editedUser != null) {
                                setEditedUser({
                                    ...editedUser,
                                    name: editedUser.name.trim(),
                                });
                            }
                        }}
                    />
                    <TextField
                        label={__.emailLabel}
                        placeholder={__.emailPlaceholder}
                        type="email"
                        value={(editedUser ?? user).email}
                        required
                        onChange={(event) => {
                            setEditedUser({
                                ...(editedUser ?? user),
                                email: event.target.value ?? '',
                            });
                        }}
                        onBlur={() => {
                            if (editedUser != null) {
                                setEditedUser({
                                    ...editedUser,
                                    email: editedUser.email.trim(),
                                });
                            }
                        }}
                    />

                    <FormControl>
                        <InputLabel>
                            {__.roleLabel}
                        </InputLabel>
                        <Select
                            value={(editedUser ?? user).role}
                            label={__.roleLabel}
                            required
                            onChange={event => {
                                setEditedUser({
                                    ...(editedUser ?? user),
                                    role: event.target.value as UserRole,
                                });
                            }}
                        >
                            <MenuItem
                                value={UserRole.Editor}
                            >
                                {UserRoleLabels[UserRole.Editor]}
                            </MenuItem>
                            <MenuItem
                                value={UserRole.Publisher}
                            >
                                {UserRoleLabels[UserRole.Publisher]}
                            </MenuItem>
                            <MenuItem
                                value={UserRole.Admin}
                            >
                                {UserRoleLabels[UserRole.Admin]}
                            </MenuItem>
                        </Select>
                    </FormControl>

                    <Typography
                        variant="subtitle1"
                        sx={{mt: 2}}
                    >
                        {user.id != null ? __.passwordChangeTitle : __.passwordTitle}
                    </Typography>

                    <TextField
                        type="password"
                        label={__.passwordLabel}
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                    />
                    <TextField
                        type="password"
                        label={__.retypePasswordLabel}
                        value={passwordRetyped}
                        onChange={event => setPasswordRetyped(event.target.value)}
                    />
                </DialogContent>

                <DialogActions sx={{pb: 3, px: 3, justifyContent: 'flex-start'}}>
                    <Button
                        type="submit"
                        size="large"
                    >
                        {__.saveLabel}
                    </Button>
                    <Button
                        type="button"
                        onClick={onClose}
                        sx={{ml: 'auto!important'}}
                        size="large"
                    >
                        {__.cancelLabel}
                    </Button>
                </DialogActions>

            </form>
        </Dialog>
    );
}
