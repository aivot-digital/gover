import React, {useCallback, useState} from 'react';
import {type AppHeaderMenuProps} from './app-header-menu-props';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {AppMode} from '../../data/app-mode';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetStepper} from '../../slices/stepper-slice';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../slices/user-slice';
import {UserRole} from '../../data/user-role';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import SettingsApplicationsOutlinedIcon from '@mui/icons-material/SettingsApplicationsOutlined';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import PaymentOutlinedIcon from '@mui/icons-material/PaymentOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {isAdmin} from '../../utils/is-admin';
import {clearAuthData, selectLogoutLink} from '../../slices/auth-slice';
import {clearCustomerInput, clearDisabled, clearErrors, selectLoadedForm} from '../../slices/app-slice';
import TaskOutlinedIcon from '@mui/icons-material/TaskOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {StorageService} from '../../services/storage-service';
import {StorageKey} from '../../data/storage-key';
import {getPath} from '../../apps/staff-app-routes';
import CompareArrowsOutlinedIcon from '@mui/icons-material/CompareArrowsOutlined';
import {CopyAllOutlined, SvgIconComponent} from '@mui/icons-material';
import {useApi} from '../../hooks/use-api';
import {AppConfig} from '../../app-config';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import {setIdentityId} from '../../slices/identity-slice';

export function AppHeaderMenu(props: AppHeaderMenuProps): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const location = useLocation();
    const form = useAppSelector(selectLoadedForm);
    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const isUserAdmin = isAdmin(user);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const logoutLink = useAppSelector(selectLogoutLink);

    const handleDelete = useCallback((closeMenuCallback: () => void) => {
        dispatch(clearCustomerInput());
        dispatch(resetStepper());
        dispatch(clearErrors());
        dispatch(clearDisabled());
        dispatch(setIdentityId(undefined));
        closeMenuCallback();
        setConfirmDelete(undefined);
    }, []);

    return (
        <>
            <Menu
                anchorEl={props.anchorElement}
                open={props.anchorElement != null}
                onClose={props.onClose}
            >
                {
                    form != null &&
                    props.mode === AppMode.Customer &&
                    <ActionMenuItem
                        label="Alle Antragsdaten löschen"
                        icon={DeleteForeverOutlinedIcon}
                        onClick={() => {
                            setConfirmDelete(() => () => handleDelete(props.onClose));
                        }}
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <LinkMenuItem
                        label="Formularverwaltung"
                        icon={DescriptionOutlinedIcon}
                        to="/forms"
                        active={location.pathname.startsWith('/forms')}
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <LinkMenuItem
                        label="Antragsverarbeitung"
                        icon={TaskOutlinedIcon}
                        to="/submissions"
                        active={location.pathname.startsWith('/submissions')}
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <ActionMenuItem
                        label="Modulauswahl"
                        icon={CompareArrowsOutlinedIcon}
                        onClick={() => {
                            StorageService.clearItem(StorageKey.SavedModule);
                            navigate(getPath('moduleSelect'));
                        }}
                        active={location.pathname === '/'}
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <Divider />
                }

                {
                    props.mode === AppMode.Staff &&
                    <LinkMenuItem
                        label="Konto verwalten"
                        icon={AccountCircleOutlinedIcon}
                        to="/account"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Systemeinstellungen"
                        icon={SettingsApplicationsOutlinedIcon}
                        to="/settings"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Mitarbeiter:innen"
                        icon={GroupOutlinedIcon}
                        to="/users"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    (
                        isUserAdmin ||
                        (memberships?.some((mem) => mem.role === UserRole.Admin) ?? false)
                    ) &&
                    <LinkMenuItem
                        label="Fachbereiche"
                        icon={BusinessOutlinedIcon}
                        to="/departments"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <LinkMenuItem
                        label="Dokumente & Medieninhalte"
                        icon={DriveFolderUploadOutlinedIcon}
                        to="/assets"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <Divider />
                }

                {
                    props.mode === AppMode.Staff &&
                    <LinkMenuItem
                        label="Vorlagen"
                        icon={CopyAllOutlined}
                        to="/presets"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Schnittstellen"
                        icon={DataObjectOutlinedIcon}
                        to="/destinations"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Links"
                        icon={LinkOutlinedIcon}
                        to="/provider-links"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Farbschemata"
                        icon={PaletteOutlinedIcon}
                        to="/themes"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Geheimnisse"
                        icon={KeyOutlinedIcon}
                        to="/secrets"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Zahlungsdienstleister"
                        icon={PaymentOutlinedIcon}
                        to="/payment-providers"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    isUserAdmin &&
                    <LinkMenuItem
                        label="Nutzerkontenanbieter"
                        icon={BadgeOutlinedIcon}
                        to="/identity-providers"
                    />
                }

                {
                    props.mode === AppMode.Staff &&
                    <Divider />
                }

                {
                    props.mode === AppMode.Staff &&
                    <ActionMenuItem
                        label="Abmelden"
                        icon={LogoutOutlinedIcon}
                        onClick={() => {
                            dispatch(clearAuthData());
                            window.location.href = logoutLink;
                        }}
                    />
                }
            </Menu>

            <ConfirmDialog
                title="Möchten Sie die eingegebenen Antragsdaten wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben.
            </ConfirmDialog>
        </>
    );
}

interface LinkMenuItemProps {
    to: string;
    icon: SvgIconComponent;
    label: string;
    active?: boolean;
}

function LinkMenuItem(props: LinkMenuItemProps): JSX.Element {
    const Icon = props.icon;
    return (
        <MenuItem
            component={Link}
            to={props.to}
            selected={props.active}
        >
            <ListItemIcon>
                <Icon
                    color={props.active ? 'primary' : 'inherit'}
                />
            </ListItemIcon>
            <ListItemText>
                {props.label}
            </ListItemText>
        </MenuItem>
    );
}

interface ActionMenuItemProps {
    onClick: () => void;
    icon: SvgIconComponent;
    label: string;
    active?: boolean;
}

function ActionMenuItem(props: ActionMenuItemProps): JSX.Element {
    const Icon = props.icon;
    return (
        <MenuItem
            onClick={props.onClick}
            selected={props.active}
        >
            <ListItemIcon>
                <Icon
                    color={props.active ? 'primary' : 'inherit'}
                />
            </ListItemIcon>
            <ListItemText>
                {props.label}
            </ListItemText>
        </MenuItem>
    );
}
