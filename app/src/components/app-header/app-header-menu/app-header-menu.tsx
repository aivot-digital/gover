import React from 'react';
import {AppHeaderMenuProps} from './app-header-menu-props';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {
    faBracketsCurly,
    faBuilding,
    faCopy, faFile,
    faLink,
    faSignOut,
    faUser,
    faUsers
} from '@fortawesome/pro-light-svg-icons';
import {AppMode} from '../../../data/app-mode';
import {resetUserInput} from '../../../slices/customer-input-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {resetStepper} from '../../../slices/stepper-slice';
import {Link} from 'react-router-dom';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {logout} from '../../../slices/auth-slice';
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectMemberships, selectUser} from "../../../slices/user-slice";
import {resetErrors} from "../../../slices/customer-input-errors-slice";
import {IconProp} from "@fortawesome/fontawesome-svg-core";
import {UserRole} from "../../../data/user-role";


export function AppHeaderMenu(props: AppHeaderMenuProps) {
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
                    icon={faSignOut}
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
                    icon={faUser}
                    to="/profile"
                />
            }

            {
                props.mode === AppMode.Staff &&
                user?.admin &&
                <LinkMenuItem
                    label="Einstellungen"
                    icon={faUser}
                    to="/settings"
                />
            }

            {
                props.mode === AppMode.Staff &&
                user?.admin &&
                <LinkMenuItem
                    label="Benutzerverwaltung"
                    icon={faUsers}
                    to="/users"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (
                    user?.admin ||
                    memberships?.some(mem => mem.role === UserRole.Admin)
                ) &&
                <LinkMenuItem
                    label="Fachbereichsverwaltung"
                    icon={faBuilding}
                    to="/departments"
                />
            }

            {
                props.mode === AppMode.Staff &&
                user?.admin &&
                <LinkMenuItem
                    label="Anlagen"
                    icon={faFile}
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
                    icon={faCopy}
                    to="/presets"
                />
            }

            {
                props.mode === AppMode.Staff &&
                user?.admin &&
                <LinkMenuItem
                    label="Schnittstellen"
                    icon={faBracketsCurly}
                    to="/destinations"
                />
            }

            {
                props.mode === AppMode.Staff &&
                user?.admin &&
                <LinkMenuItem
                    label="Links"
                    icon={faLink}
                    to="/provider-links"
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
                    icon={faSignOut}
                    onClick={() => {
                        dispatch(logout());
                    }}
                />
            }
        </Menu>
    );
}

function LinkMenuItem({to, icon, label}: { to: string, icon: IconProp, label: string }) {
    return (
        <MenuItem
            component={Link}
            to={to}
        >
            <ListItemIcon>
                <FontAwesomeIcon
                    icon={icon}
                />
            </ListItemIcon>
            <ListItemText>
                {label}
            </ListItemText>
        </MenuItem>
    );
}

function ActionMenuItem({onClick, icon, label}: { onClick: () => void, icon: IconProp, label: string }) {
    return (
        <MenuItem
            onClick={onClick}
        >
            <ListItemIcon>
                <FontAwesomeIcon
                    icon={icon}
                />
            </ListItemIcon>
            <ListItemText>
                {label}
            </ListItemText>
        </MenuItem>
    );
}