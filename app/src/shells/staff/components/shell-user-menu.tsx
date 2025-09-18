import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser, setMemberships, setUser} from '../../../slices/user-slice';
import {Divider} from '@mui/material';
import {useMemo} from 'react';
import {getFullName} from '../../../models/entities/user';
import {resetLocalStorageJwt} from '../../../services/base-api-service';
import {Link, useNavigate} from 'react-router-dom';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {setStatus, ShellStatus} from '../../../slices/shell-slice';

interface ShellUserMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
}

export function ShellUserMenu(props: ShellUserMenuProps) {
    const {
        anchorEl,
        onClose,
    } = props;

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const user = useAppSelector(selectUser);
    const userName = useMemo(() => {
        return getFullName(user);
    }, [user]);

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            <MenuItem
                component={Link}
                to="/account"
                onClick={onClose}
            >
                {userName}
            </MenuItem>
            <Divider />
            <MenuItem
                onClick={() => {
                    resetLocalStorageJwt();
                    dispatch(setUser(undefined));
                    dispatch(setMemberships([]));
                    dispatch(setStatus(ShellStatus.Login));
                    navigate('/');
                }}
            >
                Abmelden
            </MenuItem>
        </Menu>
    );
}