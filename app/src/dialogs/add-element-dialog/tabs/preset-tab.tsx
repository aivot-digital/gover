import React, {useEffect, useState} from 'react';
import {type Preset} from '../../../models/entities/preset';
import {type BaseTabProps} from './base-tab-props';
import {
    Box,
    DialogContent,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Typography
} from '@mui/material';
import {
    LoadingPlaceholder
} from '../../../components/loading-placeholder/loading-placeholder';
import {cloneElement} from '../../../utils/clone-element';
import {AlertComponent} from '../../../components/alert/alert-component';
import {Link} from 'react-router-dom';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import {useApi} from "../../../hooks/use-api";
import {usePresetsApi} from "../../../hooks/use-presets-api";
import {filterItems} from "../../../utils/filter-items";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";

export function PresetTab(props: BaseTabProps): JSX.Element {
    const api = useApi();
    const [presets, setPresets] = useState<Preset[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        usePresetsApi(api)
            .list({onlyPublished: true})
            .then(setPresets);
    }, [props.parentType, setPresets]);

    const addPresetElement = (preset: Preset): void => {
        usePresetsApi(api)
            .retrieveVersion(preset.key, preset.currentPublishedVersion ?? '')
            .then((presetVersion) => {
                props.onAddElement(cloneElement({
                    ...presetVersion.root,
                    name: preset.title,
                }, true));
            });
    };

    if (presets == null) {
        return (
            <LoadingPlaceholder/>
        );
    } else if (presets.length === 0) {
        return (
            <DialogContent tabIndex={0}>
                <AlertComponent
                    title="Keine Vorlagen gefunden"
                    text="Es existieren noch keine lokalen Vorlagen."
                    color="info"
                />

                <Typography>
                    Sie können neue Vorlagen erstellen, in dem Sie bestehende Elemente im Bearbeitungs-Modus durch einen
                    Klick auf die Schaltfläche &bdquo;Als Vorlage speichern&rdquo; am unteren Bildschirmrand hinzufügen.
                    Alternativ können Sie im Bereich <Link
                    to="/presets"
                    target="_blank"
                >Vorlagen</Link> neue Vorlagen anlegen und bearbeiten.
                </Typography>
            </DialogContent>
        );
    } else {
        const filteredPresets = filterItems(presets, 'title', search);

        return (
            <>
                <Box
                    sx={{
                        px: 4,
                    }}
                >
                    <TextFieldComponent
                        label="Vorlage suchen"
                        value={search}
                        onChange={(val) => {
                            setSearch(val ?? '');
                        }}
                        placeholder="Suchen..."
                    />
                </Box>

                <List dense>
                    {
                        filteredPresets
                            .map((preset) => {
                                return (
                                    <ListItem
                                        key={preset.title}
                                        disablePadding
                                    >
                                        <ListItemButton
                                            onClick={() => {
                                                addPresetElement(preset);
                                            }}
                                        >
                                            <ListItemIcon sx={{pl: 1.5}}>
                                                <MenuOutlinedIcon/>
                                            </ListItemIcon>
                                            <ListItemText
                                                primary={preset.title}
                                                secondary={`Aktuelle Version ${preset.currentPublishedVersion ?? ''}`}
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                );
                            })
                    }
                </List>
            </>
        );
    }
}
