import React from 'react';
import {AppHeaderMenuProps} from './app-header-menu-props';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {faBracketsCurly, faBuilding, faCopy, faGear, faLink, faSignOut, faUser} from '@fortawesome/pro-light-svg-icons';
import {AppMode} from '../../../../data/app-mode';
import strings from './app-header-menu-strings.json';
import {resetUserInput} from '../../../../slices/customer-input-slice';
import {Localization} from '../../../../locale/localization';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {resetStepper} from '../../../../slices/stepper-slice';
import {Link} from 'react-router-dom';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {logout} from '../../../../slices/auth-slice';
import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';

const __ = Localization(strings);

type StaffMenuItem = {
    linkTo?: string,
    adminOnly: boolean,
    icon: IconDefinition,
    label: string,
    events?: any[],
};

const staffMenuItems: (StaffMenuItem | 'divider')[] = [
    // Staff Menu Items
    {
        linkTo: '/profile',
        adminOnly: false,
        icon: faUser,
        label: __.profile,
    },
    {
        linkTo: '/settings',
        adminOnly: true,
        icon: faGear,
        label: __.settings,
    },
    'divider',
    {
        linkTo: '/presets',
        adminOnly: false,
        icon: faCopy,
        label: __.presets,
    },
    {
        linkTo: '/vendors',
        adminOnly: true,
        icon: faBuilding,
        label: __.vendors,
    },
    {
        linkTo: '/destinations',
        adminOnly: true,
        icon: faBracketsCurly,
        label: __.destinations,
    },
    {
        linkTo: '/provider-links',
        adminOnly: true,
        icon: faLink,
        label: __.providerLinks,
    },
    'divider',
    {
        events: [
            logout,
        ],
        adminOnly: false,
        icon: faSignOut,
        label: __.signOut,
    },
];

type CustomerMenuItem = {
    icon: IconDefinition,
    label: string,
    events: any[],
};

const customerMenuItems: (CustomerMenuItem | 'divider')[] = [
    // Customer Menu Items
    {
        events: [
            resetUserInput,
            resetStepper,
        ],
        icon: faSignOut,
        label: __.deleteCustomerData,
    },
];

export function AppHeaderMenu(props: AppHeaderMenuProps) {
    const dispatch = useAppDispatch();

    const items: (StaffMenuItem | 'divider')[] | (CustomerMenuItem | 'divider')[] =
        props.mode === AppMode.Staff ? staffMenuItems : customerMenuItems;

    return (
        <Menu
            anchorEl={props.anchorElement}
            open={props.anchorElement != null}
            onClose={props.onClose}
        >
            {
                items
                    //.filter(mi => typeof mi === 'string' || !('adminOnly' in mi) || (mi.adminOnly && user?.role === UserRole.Admin))
                    .map((mi, index) => typeof mi === 'string' ? <Divider key={index}/> : (
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
