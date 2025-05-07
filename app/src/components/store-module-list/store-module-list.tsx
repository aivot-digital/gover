import {Box, DialogContent, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip, Typography, useTheme} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import React, {useEffect, useState} from 'react';
import {type StoreListModule} from '../../models/entities/store-list-module';
import {GoverStoreService} from '../../services/gover-store.service';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {LoadingPlaceholder} from '../loading-placeholder/loading-placeholder';
import {AlertComponent} from '../alert/alert-component';
import {Link} from 'react-router-dom';
import {TextFieldComponent} from '../text-field/text-field-component';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import {type StoreModuleListProps} from './store-module-list-props';
import {filterItems} from '../../utils/filter-items';

export function StoreModuleList(props: StoreModuleListProps): JSX.Element {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const [modules, setModules] = useState<StoreListModule[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        GoverStoreService
            .listModules(0, '', props.storeKey)
            .then((res) => {
                setModules(res.items);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Bausteine'));
            });
    }, [setModules]);

    const addModuleElement = (module: StoreListModule): void => {
        Promise.all([
            GoverStoreService
                .fetchModuleCode(module.id, module.current_version, props.storeKey),
            GoverStoreService
                .fetchModule(module.id, props.storeKey),
        ])
            .then(([element, module]) => {
                props.onSelect(module, element);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden des Bausteins'));
            });
    };

    if (modules == null) {
        return (
            <LoadingPlaceholder/>
        );
    } else if (modules.length === 0) {
        return (
            <DialogContent tabIndex={0}>
                <AlertComponent
                    title="Keine Bausteine gefunden"
                    text="Im Gover Store wurden keine Bausteine gefunden."
                    color="info"
                />

                <Typography>
                    Sie können neue Bausteine für den Gover Store erstellen, in dem Sie bestehende Vorlagen im
                    Bereich <Link
                    to="/presets"
                    target="_blank"
                    rel="noreferrer noopener"
                >Vorlagen</Link> über die
                    Schaltfläche <strong>Veröffentlichen</strong> in den Store transportieren.
                </Typography>

                <Typography sx={{mt: 2}}>
                    Bitte beachten Sie, dass Sie für das Veröffentlichen von Bausteinen einen Gover Store Schlüssel
                    benötigen. Mehr dazu finden Sie
                    im <a
                    href="https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/store/schluessel-beantragen"
                    target="_blank"
                    rel="noreferrer noopener"
                >Gover Store</a>.
                </Typography>
            </DialogContent>
        );
    } else {
        const filteredModules = filterItems(modules, 'title', search);

        return (
            <>
                <Box
                    sx={{
                        px: 4,
                    }}
                >
                    <TextFieldComponent
                        label="Modul suchen"
                        value={search}
                        onChange={(val) => {
                            setSearch(val ?? '');
                        }}
                        placeholder="Suchen…"
                    />
                </Box>

                <List dense>
                    {
                        filteredModules
                            .map((module) => (
                                <ListItem
                                    key={module.id}
                                    disablePadding
                                    secondaryAction={
                                        props.itemAction != null ?
                                            (
                                                <Tooltip title={props.itemAction.tooltip}>
                                                    <IconButton
                                                        onClick={() => {
                                                            props.itemAction?.onClick(module);
                                                        }}
                                                    >
                                                        {props.itemAction.icon}
                                                    </IconButton>
                                                </Tooltip>
                                            ) :
                                            undefined
                                    }
                                >
                                    <ListItemButton
                                        onClick={() => {
                                            addModuleElement(module);
                                        }}
                                        selected={props.selectedModuleId === module.id}
                                    >
                                        <ListItemIcon sx={{pl: 1.5}}>
                                            <ExtensionOutlinedIcon/>
                                        </ListItemIcon>
                                        <ListItemText
                                            disableTypography
                                            primary={
                                                <Box
                                                    sx={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                    }}
                                                >
                                                    {
                                                        !module.is_public &&
                                                        <Tooltip
                                                            title="Privater Baustein"
                                                        >
                                                            <LockOutlinedIcon
                                                                fontSize="small"
                                                                sx={{marginRight: '0.25em'}}
                                                            />
                                                        </Tooltip>
                                                    }

                                                    <Typography>
                                                        {module.title}
                                                    </Typography>

                                                    <Typography
                                                        variant="caption"
                                                        sx={{ml: 1}}
                                                    >
                                                        {module.current_version}
                                                    </Typography>
                                                </Box>
                                            }
                                            secondary={
                                                <Box>
                                                    <Typography
                                                        sx={{
                                                            display: 'inline',
                                                            fontSize: '90%',
                                                        }}
                                                    >
                                                        @{module.organization}
                                                    </Typography>
                                                    &nbsp;-&nbsp;
                                                    <Typography
                                                        sx={{
                                                            display: 'inline',
                                                            fontSize: '90%',
                                                            color: theme.palette.grey['500'],
                                                        }}
                                                    >
                                                        {module.description_short}
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                    </ListItemButton>
                                </ListItem>
                            ))
                    }
                </List>
            </>
        );
    }
}
