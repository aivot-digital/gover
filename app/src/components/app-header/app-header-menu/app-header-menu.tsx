import React from 'react';
import {type AppHeaderMenuProps} from './app-header-menu-props';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem, type SvgIconProps} from '@mui/material';
import {AppMode} from '../../../data/app-mode';
import {resetUserInput} from '../../../slices/customer-input-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {resetStepper} from '../../../slices/stepper-slice';
import {Link} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {logout, selectMemberships, selectUser} from '../../../slices/user-slice';
import {resetErrors} from '../../../slices/customer-input-errors-slice';
import {UserRole} from '../../../data/user-role';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import SettingsApplicationsOutlinedIcon from '@mui/icons-material/SettingsApplicationsOutlined';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import FilePresentOutlinedIcon from '@mui/icons-material/FilePresentOutlined';
import DashboardCustomizeOutlinedIcon from '@mui/icons-material/DashboardCustomizeOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import ColorLensIcon from '@mui/icons-material/ColorLens';

export function AppHeaderMenu(props: AppHeaderMenuProps): JSX.Element {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    return (
        <Menu
            anchorEl={props.anchorElement}
            open={props.anchorElement != null}
            onClose={props.onClose}
        >
            {
                props.mode === AppMode.Customer &&
                <ActionMenuItem
                    label="Alle Antragsdaten löschen"
                    icon={<LogoutOutlinedIcon/>}
                    onClick={() => {
                        const conf = window.confirm('Sollen wirklich alle Daten gelöscht werden?');
                        if (conf) {
                            dispatch(resetUserInput());
                            dispatch(resetStepper());
                            dispatch(resetErrors());
                        }
                    }}
                />
            }

            {
                props.mode === AppMode.Staff &&
                <LinkMenuItem
                    label="Profil"
                    icon={<AccountCircleOutlinedIcon/>}
                    to="/profile"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Einstellungen"
                    icon={<SettingsApplicationsOutlinedIcon/>}
                    to="/settings"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Mitarbeiter:innen"
                    icon={<GroupOutlinedIcon/>}
                    to="/users"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (
                    (user?.admin ?? false) ||
                    (memberships?.some((mem) => mem.role === UserRole.Admin) ?? false)
                ) &&
                <LinkMenuItem
                    label="Fachbereichsverwaltung"
                    icon={<BusinessOutlinedIcon/>}
                    to="/departments"
                />
            }

            {
                props.mode === AppMode.Staff &&
                <LinkMenuItem
                    label="Dokumente & Medieninhalte"
                    icon={<FilePresentOutlinedIcon/>}
                    to="/assets"
                />
            }

            {
                props.mode === AppMode.Staff &&
                <Divider/>
            }

            {
                props.mode === AppMode.Staff &&
                <LinkMenuItem
                    label="Vorlagen"
                    icon={<DashboardCustomizeOutlinedIcon/>}
                    to="/presets"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Schnittstellen"
                    icon={<DataObjectOutlinedIcon/>}
                    to="/destinations"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Links"
                    icon={<LinkOutlinedIcon/>}
                    to="/provider-links"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Farbschemata"
                    icon={<ColorLensIcon/>}
                    to="/themes"
                />
            }

            {
                props.mode === AppMode.Staff &&
                <Divider/>
            }

            {
                props.mode === AppMode.Staff &&
                <ActionMenuItem
                    label="Abmelden"
                    icon={<LogoutOutlinedIcon/>}
                    onClick={() => {
                        dispatch(logout());
                    }}
                />
            }
        </Menu>
    );
}

interface LinkMenuItemProps {
    to: string;
    icon: JSX.Element;
    label: string;
}

function LinkMenuItem(props: LinkMenuItemProps): JSX.Element {
    return (
        <MenuItem
            component={Link}
            to={props.to}
        >
            <ListItemIcon>
                {props.icon}
            </ListItemIcon>
            <ListItemText>
                {props.label}
            </ListItemText>
        </MenuItem>
    );
}

function ActionMenuItem({
                            onClick,
                            icon,
                            label,
                        }: {onClick: () => void, icon: SvgIconProps, label: string}) {
    return (
        <MenuItem
            onClick={onClick}
        >
            <ListItemIcon>
                {icon}
            </ListItemIcon>
            <ListItemText>
                {label}
            </ListItemText>
        </MenuItem>
    );
}
