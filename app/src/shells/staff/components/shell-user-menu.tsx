import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {Divider} from '@mui/material';
import {useMemo} from 'react';
import {getFullName} from '../../../models/entities/user';
import {Link} from 'react-router-dom';
import {useLogout} from '../../../hooks/use-logout';

interface ShellUserMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
}

export function ShellUserMenu(props: ShellUserMenuProps) {
    const {
        anchorEl,
        onClose,
    } = props;

    const logout = useLogout();

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
                onClick={logout}
            >
                Abmelden
            </MenuItem>
        </Menu>
    );
}