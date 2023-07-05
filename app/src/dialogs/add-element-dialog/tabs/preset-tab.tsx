import React, {useEffect, useState} from "react";
import {Preset} from "../../../models/entities/preset";
import {PresetsService} from "../../../services/presets.service";
import {BaseTabProps} from "./base-tab-props";
import {DialogContent, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {ElementIcons} from "../../../data/element-type/element-icons";
import {ElementType} from "../../../data/element-type/element-type";
import {
    LoadingPlaceholderComponentView
} from "../../../components/static-components/loading-placeholder/loading-placeholder.component.view";
import {cloneElement} from "../../../utils/clone-element";
import {AlertComponent} from "../../../components/alert/alert-component";
import {Link} from "react-router-dom";

export function PresetTab({parentType, onAddElement}: BaseTabProps) {
    const [presets, setPresets] = useState<Preset[]>();

    useEffect(() => {
        PresetsService
            .list()
            .then(setPresets);
    }, [parentType, setPresets]);

    const addPresetElement = (preset: Preset) => {
        onAddElement(cloneElement(preset.root, true));
    };

    if (presets == null) {
        return (
            <LoadingPlaceholderComponentView/>
        );
    } else if (presets.length === 0) {
        return (
            <DialogContent>
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
        return (
            <List dense>
                {
                    presets
                        .map(preset => (
                            <ListItem
                                key={preset.root.name}
                                disablePadding
                            >
                                <ListItemButton onClick={() => addPresetElement(preset)}>
                                    <ListItemIcon sx={{pl: 1.5}}>
                                        <FontAwesomeIcon
                                            icon={ElementIcons[ElementType.Container]}
                                        />
                                    </ListItemIcon>
                                    <ListItemText
                                        disableTypography
                                        primary={preset.root.name}
                                    />
                                </ListItemButton>
                            </ListItem>
                        ))
                }
            </List>
        );
    }
}