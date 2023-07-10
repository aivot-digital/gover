import React from 'react';
import { type AppHeaderMenuProps } from './app-header-menu-props';
import { Divider, ListItemIcon, ListItemText, Menu, MenuItem } from '@mui/material';
import {
    faBracketsCurly,
    faBuilding,
    faCopy,
    faFile,
    faLink,
    faSignOut,
    faUser,
    faUsers,
    faCogs,
    faPalette,
} from '@fortawesome/pro-light-svg-icons';
import { AppMode } from '../../../data/app-mode';
import { resetUserInput } from '../../../slices/customer-input-slice';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { resetStepper } from '../../../slices/stepper-slice';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useAppSelector } from '../../../hooks/use-app-selector';
import { logout, selectMemberships, selectUser } from '../../../slices/user-slice';
import { resetErrors } from '../../../slices/customer-input-errors-slice';
import { type IconProp } from '@fortawesome/fontawesome-svg-core';
import { UserRole } from '../../../data/user-role';

export function AppHeaderMenu(props: AppHeaderMenuProps): JSX.Element {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    return (
        <Menu
            anchorEl={ props.anchorElement }
            open={ props.anchorElement != null }
            onClose={ props.onClose }
        >
            {
                props.mode === AppMode.Customer &&
                <ActionMenuItem
                    label="Alle Antragsdaten löschen"
                    icon={ faSignOut }
                    onClick={ () => {
                        const conf = window.confirm('Sollen wirklich alle Daten gelöscht werden?');
                        if (conf) {
                            dispatch(resetUserInput());
                            dispatch(resetStepper());
                            dispatch(resetErrors());
                        }
                    } }
                />
            }

            {
                props.mode === AppMode.Staff &&
                <LinkMenuItem
                    label="Profil"
                    icon={ faUser }
                    to="/profile"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Einstellungen"
                    icon={ faCogs }
                    to="/settings"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Mitarbeiter:innen"
                    icon={ faUsers }
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
                    label="Fachbereiche"
                    icon={ faBuilding }
                    to="/departments"
                />
            }

            {
                props.mode === AppMode.Staff &&
                <LinkMenuItem
                    label="Dokumente & Medieninhalte"
                    icon={ faFile }
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
                    icon={ faCopy }
                    to="/presets"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Schnittstellen"
                    icon={ faBracketsCurly }
                    to="/destinations"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Links"
                    icon={ faLink }
                    to="/provider-links"
                />
            }

            {
                props.mode === AppMode.Staff &&
                (user?.admin ?? false) &&
                <LinkMenuItem
                    label="Farbschemata"
                    icon={ faPalette }
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
                    icon={ faSignOut }
                    onClick={ () => {
                        dispatch(logout());
                    } }
                />
            }
        </Menu>
    );
}

function LinkMenuItem({ to, icon, label }: { to: string, icon: IconProp, label: string }): JSX.Element {
    return (
        <MenuItem
            component={ Link }
            to={ to }
        >
            <ListItemIcon>
                <FontAwesomeIcon
                    icon={ icon }
                />
            </ListItemIcon>
            <ListItemText>
                { label }
            </ListItemText>
        </MenuItem>
    );
}

function ActionMenuItem({ onClick, icon, label }: { onClick: () => void, icon: IconProp, label: string }): JSX.Element {
    return (
        <MenuItem
            onClick={ onClick }
        >
            <ListItemIcon>
                <FontAwesomeIcon
                    icon={ icon }
                />
            </ListItemIcon>
            <ListItemText>
                { label }
            </ListItemText>
        </MenuItem>
    );
}
