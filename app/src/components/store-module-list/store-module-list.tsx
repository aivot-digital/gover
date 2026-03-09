import {Alert, Box, Button, Chip, Typography} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import React, {useEffect, useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {type StoreListModule} from '../../models/entities/store-list-module';
import {GoverStoreService} from '../../services/gover-store.service';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {LoadingPlaceholder} from '../loading-placeholder/loading-placeholder';
import {Link} from 'react-router-dom';
import {SearchInput} from '../search-input/search-input';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {type StoreModuleListProps} from './store-module-list-props';

export function StoreModuleList(props: StoreModuleListProps) {
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
    }, [dispatch, props.storeKey]);

    const filteredModules = useMemo(() => {
        const trimmedSearch = search.trim();
        if (trimmedSearch.length === 0) {
            return modules ?? [];
        }

        const fuse = new Fuse(modules ?? [], {
            threshold: 0.3,
            ignoreLocation: true,
            keys: [
                {name: 'title', weight: 0.45},
                {name: 'organization', weight: 0.25},
                {name: 'description_short', weight: 0.2},
                {name: 'datenfeld_id', weight: 0.1},
            ],
        });

        return fuse.search(trimmedSearch).map((entry) => entry.item);
    }, [modules, search]);

    const addModuleElement = (module: StoreListModule): void => {
        Promise.all([
            GoverStoreService.fetchModuleCode(module.id, module.current_version, props.storeKey),
            GoverStoreService.fetchModule(module.id, props.storeKey),
        ])
            .then(([element, detailedModule]) => {
                props.onSelect(detailedModule, element);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden des Bausteins'));
            });
    };

    if (modules == null) {
        return <LoadingPlaceholder/>;
    }

    if (modules.length === 0) {
        return (
            <Box
                sx={{
                    p: 3,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2,
                }}
            >
                <Alert severity="info">
                    Im Gover Marktplatz wurden keine Bausteine gefunden.
                </Alert>

                <Typography>
                    Sie können neue Bausteine für den Gover Marktplatz erstellen, indem Sie bestehende Vorlagen im
                    Bereich <Link to="/presets" target="_blank" rel="noreferrer noopener">Vorlagen</Link> veröffentlichen.
                </Typography>
            </Box>
        );
    }

    return (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Box
                sx={{
                    p: 2,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <SearchInput
                    label="Baustein suchen"
                    value={search}
                    onChange={setSearch}
                    placeholder="Name, Organisation oder Beschreibung durchsuchen"
                />
            </Box>

            <Box
                sx={{
                    flex: 1,
                    overflowY: 'auto',
                    pb: 1.5,
                }}
            >
                {
                    filteredModules.length === 0 &&
                    <Box sx={{px: 2, pt: 2}}>
                        <Alert severity="info">
                            Es wurden keine Bausteine gefunden, die zu Ihrer Suche passen.
                        </Alert>
                    </Box>
                }

                {
                    filteredModules.map((module, index) => (
                        <React.Fragment key={module.id}>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: 1.75,
                                    px: 2.25,
                                    py: 1.9,
                                    bgcolor: props.selectedModuleId === module.id ? 'action.hover' : 'transparent',
                                }}
                            >
                                <Box
                                    sx={{
                                        width: 38,
                                        height: 38,
                                        borderRadius: '50%',
                                        bgcolor: 'grey.100',
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        flexShrink: 0,
                                    }}
                                >
                                    <ExtensionOutlinedIcon sx={{fontSize: 20, color: 'text.secondary'}}/>
                                </Box>

                                <Box sx={{minWidth: 0, flex: 1}}>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 0.75,
                                            minWidth: 0,
                                            flexWrap: 'wrap',
                                        }}
                                    >
                                        <Typography fontWeight={700}>
                                            {module.title}
                                        </Typography>
                                        {
                                            !module.is_public &&
                                            <LockOutlinedIcon sx={{fontSize: 16, color: 'text.secondary'}}/>
                                        }
                                        <Chip
                                            size="small"
                                            label={`Version ${module.current_version}`}
                                            sx={{flexShrink: 0}}
                                        />
                                    </Box>
                                    <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                                        @{module.organization} - {module.description_short}
                                    </Typography>
                                </Box>

                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 1.5,
                                        flexShrink: 0,
                                        pl: 1.5,
                                    }}
                                >
                                    {
                                        props.itemAction != null &&
                                        <Button
                                            variant="text"
                                            size="small"
                                            startIcon={props.itemAction.icon ?? <InfoOutlinedIcon sx={{fontSize: 18}}/>}
                                            onClick={() => {
                                                props.itemAction?.onClick(module);
                                            }}
                                        >
                                            Details
                                        </Button>
                                    }
                                    <Button
                                        variant="contained"
                                        size="small"
                                        startIcon={props.primaryActionIcon}
                                        onClick={() => {
                                            addModuleElement(module);
                                        }}
                                    >
                                        {props.primaryActionLabel}
                                    </Button>
                                </Box>
                            </Box>
                            {
                                index < filteredModules.length - 1 &&
                                <Box sx={{mx: 2}}>
                                    <Box sx={{borderBottom: '1px solid', borderColor: 'divider'}}/>
                                </Box>
                            }
                        </React.Fragment>
                    ))
                }
            </Box>
        </Box>
    );
}
