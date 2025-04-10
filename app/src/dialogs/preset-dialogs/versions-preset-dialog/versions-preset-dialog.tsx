import {type VersionsPresetDialogProps} from './versions-preset-dialog-props';
import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {type PresetVersion} from '../../../models/entities/preset-version';
import {Link} from 'react-router-dom';
import {format, parseISO} from 'date-fns';
import {determinePresetVersionDescriptor} from '../../../utils/determine-preset-version-descriptor';
import {useApi} from "../../../hooks/use-api";
import {PresetVersionApiService} from "../../../modules/presets/preset-version-api-service";

export function VersionsPresetDialog(props: DialogProps & VersionsPresetDialogProps): JSX.Element {
    const api = useApi();

    const {
        onClose,
        preset,

        ...passThroughProps
    } = props;

    const [versions, setVersions] = useState<PresetVersion[]>([]);

    useEffect(() => {
        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        presetVersionApiService.list(0, 999)
            .then(page => setVersions(page.content))
            .catch(() => setVersions([]));
    }, [preset, passThroughProps.open]);

    return (
        <Dialog
            {...passThroughProps}
            fullWidth
            onClose={onClose}
        >
            <DialogTitleWithClose
                onClose={onClose}
                closeTooltip="Schließen"
            >
                Alle Versionen von {preset.title}
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                <List>
                    {
                        versions.map((version) => {
                            const statusLabel = version.publishedAt != null ? 'Veröffentlicht am' : 'Zuletzt bearbeitet';
                            const timestamp = parseISO(version.publishedAt ?? version.updated);

                            return (
                                <ListItem
                                    key={version.version}
                                >
                                    <ListItemButton
                                        component={Link}
                                        to={`/presets/edit/${preset.key}/${version.version}`}
                                        replace={true}
                                    >
                                        <ListItemText
                                            primary={`${version.version} (${determinePresetVersionDescriptor(preset, version)})`}
                                            secondary={`${statusLabel}: ${format(timestamp, 'dd.MM.yyyy - HH:mm')}`}
                                        />
                                    </ListItemButton>
                                </ListItem>
                            )
                        })
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}
