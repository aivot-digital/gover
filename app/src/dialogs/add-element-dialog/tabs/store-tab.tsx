import React, {useEffect, useState} from 'react';
import {cloneElement} from '../../../utils/clone-element';
import {GoverStoreService} from '../../../services/gover-store.service';
import {type BaseTabProps} from './base-tab-props';
import {LoadingPlaceholderComponentView} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {Box, DialogContent, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip, Typography, useTheme} from '@mui/material';
import {AlertComponent} from '../../../components/alert/alert-component';
import {Link} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {type StoreListModule} from '../../../models/entities/store-list-module';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';

export function StoreTab({
                             parentType,
                             onAddElement,
                             showModuleId,
                             highlightedModuleId,
                         }: BaseTabProps & {
    showModuleId: (id: string) => void;
    highlightedModuleId?: string;
}): JSX.Element {
    const theme = useTheme();
    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));
    const [modules, setModules] = useState<StoreListModule[]>();
    const [search, setSearch] = useState<string>();

    useEffect(() => {
        GoverStoreService
            .listModules(0, '', storeKey)
            .then((res) => {
                setModules(res.items);
            });
    }, [parentType, setModules]);

    const addModuleElement = (module: StoreListModule): void => {
        GoverStoreService
            .fetchModuleCode(module.id, module.current_version, storeKey)
            .then((group) => {
                onAddElement(cloneElement(group, true));
            })
            .catch((err) => {
                console.error(err);
            });
    };

    if (modules == null) {
        return (
            <LoadingPlaceholderComponentView/>
        );
    } else if (modules.length === 0) {
        return (
            <DialogContent>
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
                >Vorlagen</Link> über die
                    Schaltfläche <strong>Veröffentlichen</strong> in den Store transportieren.
                </Typography>

                <Typography sx={{mt: 2}}>
                    Bitte beachten Sie, dass Sie für das Veröffentlichen von Bausteinen einen Gover Store Schlüssel
                    benötigen. Mehr dazu finden Sie
                    im <a
                    href="https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/store/schluessel-beantragen"
                    target="_blank"
                    rel="noreferrer"
                >Gover Store</a>.
                </Typography>
            </DialogContent>
        );
    } else {
        return (
            <>
                <Box
                    sx={{
                        px: 4,
                    }}
                >
                    <TextFieldComponent
                        label="Suche"
                        value={search}
                        onChange={setSearch}
                        placeholder="Suchen..."
                    />
                </Box>

                <List dense>
                    {
                        modules
                            .filter((m) => search == null || m.title.toLowerCase().includes(search.toLowerCase()))
                            .map((module) => (
                                <ListItem
                                    key={module.id}
                                    disablePadding
                                    secondaryAction={
                                        <Tooltip title="Mehr Informationen">
                                            <IconButton
                                                onClick={() => {
                                                    showModuleId(module.id);
                                                }}
                                            >
                                                <InfoOutlinedIcon/>
                                            </IconButton>
                                        </Tooltip>
                                    }
                                >
                                    <ListItemButton
                                        onClick={() => {
                                            addModuleElement(module);
                                        }}
                                        selected={highlightedModuleId === module.id}
                                    >
                                        <ListItemIcon sx={{pl: 1.5}}>
                                            <ExtensionOutlinedIcon/>
                                        </ListItemIcon>
                                        <ListItemText
                                            disableTypography
                                            primary={
                                                <Box sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                }}>
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
