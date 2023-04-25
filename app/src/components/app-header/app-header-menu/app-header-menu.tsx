import React from 'react';
import {AppHeaderMenuProps} from './app-header-menu-props';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {faBracketsCurly, faBuilding, faCopy, faGear, faLink, faSignOut, faUser} from '@fortawesome/pro-light-svg-icons';
import {AppMode} from '../../../data/app-mode';
import strings from './app-header-menu-strings.json';
import {resetUserInput} from '../../../slices/customer-input-slice';
import {Localization} from '../../../locale/localization';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {resetStepper} from '../../../slices/stepper-slice';
import {Link} from 'react-router-dom';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {logout} from '../../../slices/auth-slice';
import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';
import {UserRole} from "../../../data/user-role";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectUser} from "../../../slices/user-slice";

const __ = Localization(strings);

type MenuItem = {
    type: AppMode;
} & ({
    linkTo?: string,
    adminOnly: boolean,
    icon: IconDefinition,
    label: string,
    events?: any[],
} | {
    divider: true;
});

const menuItems: MenuItem[] = [
    // Staff Menu Items
    {
        linkTo: '/profile',
        adminOnly: false,
        icon: faUser,
        label: __.profile,
        type: AppMode.Staff,
    },
    {
        linkTo: '/settings',
        adminOnly: true,
        icon: faGear,
        label: __.settings,
        type: AppMode.Staff,
    },
    {
        type: AppMode.Staff,
        divider: true,
    },
    {
        linkTo: '/presets',
        adminOnly: false,
        icon: faCopy,
        label: __.presets,
        type: AppMode.Staff,
    },
    {
        linkTo: '/vendors',
        adminOnly: true,
        icon: faBuilding,
        label: __.vendors,
        type: AppMode.Staff,
    },
    {
        linkTo: '/destinations',
        adminOnly: true,
        icon: faBracketsCurly,
        label: __.destinations,
        type: AppMode.Staff,
    },
    {
        linkTo: '/provider-links',
        adminOnly: true,
        icon: faLink,
        label: __.providerLinks,
        type: AppMode.Staff,
    },
    {
        type: AppMode.Staff,
        divider: true,
    },
    {
        events: [
            logout,
        ],
        adminOnly: false,
        icon: faSignOut,
        label: __.signOut,
        type: AppMode.Staff,
    },

    // Customer Menu Items
    {
        events: [
            resetUserInput,
            resetStepper,
        ],
        adminOnly: false,
        icon: faSignOut,
        label: __.deleteCustomerData,
        type: AppMode.Customer,
    },
];

export function AppHeaderMenu(props: AppHeaderMenuProps) {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);

    return (
        <Menu
            anchorEl={props.anchorElement}
            open={props.anchorElement != null}
            onClose={props.onClose}
        >
            {
                menuItems
                    .filter(mi => mi.type === props.mode && (!('adminOnly' in mi) || !mi.adminOnly || (user != null && user.role === UserRole.Admin)))
                    .map((mi, index) => 'divider' in mi ? <Divider key={index}/> : (
                        <MenuItem
                            key={index}
                            component={'linkTo' in mi ? Link : 'li'}
                            to={'linkTo' in mi ? mi.linkTo : undefined}
                            onClick={() => {
                                if ('events' in mi && mi.events != null) {
                                    mi.events.forEach(evt => dispatch(evt()));
                                    props.onClose();
                                }
                            }}
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon
                                    icon={mi.icon}
                                />
                            </ListItemIcon>
                            <ListItemText>
                                {mi.label}
                            </ListItemText>
                        </MenuItem>
                    ))
            }
        </Menu>
    );
}
