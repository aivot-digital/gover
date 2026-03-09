import React, {useEffect, useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {type Preset} from '../../../models/entities/preset';
import {type BaseTabProps} from './base-tab-props';
import {Alert, Box, Button, Typography} from '@mui/material';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {cloneElement} from '../../../utils/clone-element';
import {Link} from 'react-router-dom';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {useApi} from '../../../hooks/use-api';
import {SearchInput} from '../../../components/search-input/search-input';
import {PresetsApiService} from '../../../modules/presets/presets-api-service';
import {PresetVersionApiService} from '../../../modules/presets/preset-version-api-service';

function getPresetSummary(preset: Preset): string {
    if (preset.publishedVersion != null && preset.draftedVersion != null) {
        return `Veröffentlicht als Version ${preset.publishedVersion}, Entwurf ${preset.draftedVersion}`;
    }

    if (preset.publishedVersion != null) {
        return `Veröffentlicht als Version ${preset.publishedVersion}`;
    }

    if (preset.draftedVersion != null) {
        return `Entwurf in Version ${preset.draftedVersion}`;
    }

    return 'Noch ohne veröffentlichte Version.';
}

export function PresetTab(props: BaseTabProps & {
    showPresetInfo: (preset: Preset) => void;
    highlightedPresetKey?: string;
}) {
    const api = useApi();
    const [presets, setPresets] = useState<Preset[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        const presetsApiService = new PresetsApiService(api);

        presetsApiService.listAll({
            published: true,
        })
            .then((page) => setPresets(page.content))
            .catch(() => setPresets([]));
    }, [api]);

    const filteredPresets = useMemo(() => {
        const trimmedSearch = search.trim();
        if (trimmedSearch.length === 0) {
            return presets ?? [];
        }

        const fuse = new Fuse(presets ?? [], {
            threshold: 0.3,
            ignoreLocation: true,
            keys: [
                {name: 'title', weight: 0.7},
                {name: 'key', weight: 0.3},
            ],
        });

        return fuse.search(trimmedSearch).map((entry) => entry.item);
    }, [presets, search]);

    const addPresetElement = (preset: Preset): void => {
        if (preset.publishedVersion == null) {
            return;
        }

        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        presetVersionApiService
            .retrieve(preset.publishedVersion)
            .then((presetVersion) => {
                props.onAddElement(cloneElement({
                    ...presetVersion.rootElement,
                    name: preset.title,
                }, true));
            })
            .catch(() => console.error('Fehler beim Laden der Preset-Version'));
    };

    if (presets == null) {
        return <LoadingPlaceholder/>;
    }

    if (presets.length === 0) {
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
                    Es existieren noch keine lokalen Vorlagen.
                </Alert>

                <Typography>
                    Sie können neue Vorlagen erstellen, indem Sie bestehende Elemente im Bearbeitungsmodus über die
                    Schaltfläche „Als Vorlage speichern“ sichern.
                </Typography>

                <Typography>
                    Alternativ können Sie im Bereich <Link to="/presets" target="_blank">Vorlagen</Link> neue Vorlagen anlegen
                    und bearbeiten.
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
                    label="Vorlage suchen"
                    value={search}
                    onChange={setSearch}
                    placeholder="Name der Vorlage eingeben"
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
                    filteredPresets.length === 0 &&
                    <Box sx={{px: 2, pt: 2}}>
                        <Alert severity="info">
                            Es wurden keine Vorlagen gefunden, die zu Ihrer Suche passen.
                        </Alert>
                    </Box>
                }

                {
                    filteredPresets.map((preset, index) => (
                        <React.Fragment key={preset.key}>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: 1.75,
                                    px: 2.25,
                                    py: 1.9,
                                    bgcolor: props.highlightedPresetKey === preset.key ? 'action.hover' : 'transparent',
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
                                    <MenuOutlinedIcon sx={{fontSize: 20, color: 'text.secondary'}}/>
                                </Box>

                                <Box sx={{minWidth: 0, flex: 1}}>
                                    <Typography fontWeight={700}>
                                        {preset.title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                                        {getPresetSummary(preset)}
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
                                    <Button
                                        variant="text"
                                        size="small"
                                        startIcon={<InfoOutlinedIcon sx={{fontSize: 18}}/>}
                                        onClick={() => {
                                            props.showPresetInfo(preset);
                                        }}
                                    >
                                        Details
                                    </Button>
                                    <Button
                                        variant="contained"
                                        size="small"
                                        startIcon={props.primaryActionIcon}
                                        onClick={() => {
                                            addPresetElement(preset);
                                        }}
                                    >
                                        {props.primaryActionLabel}
                                    </Button>
                                </Box>
                            </Box>
                            {
                                index < filteredPresets.length - 1 &&
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
