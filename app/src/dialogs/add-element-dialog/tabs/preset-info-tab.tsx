import React from 'react';
import {Box, Chip, Typography} from '@mui/material';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import {type Preset} from '../../../models/entities/preset';
import {type AnyElement} from '../../../models/elements/any-element';
import {cloneElement} from '../../../utils/clone-element';
import {useApi} from '../../../hooks/use-api';
import {PresetVersionApiService} from '../../../modules/presets/preset-version-api-service';
import {type ReactNode} from 'react';
import {SelectionDetailsPanel} from '../../../components/selection-dialog/selection-details-panel';

function formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(value));
}

function getPresetSummary(preset: Preset): string {
    if (preset.publishedVersion != null && preset.draftedVersion != null) {
        return `Diese Vorlage ist als Version ${preset.publishedVersion} veröffentlicht und liegt zusätzlich als Entwurf in Version ${preset.draftedVersion} vor.`;
    }

    if (preset.publishedVersion != null) {
        return `Diese Vorlage ist als Version ${preset.publishedVersion} veröffentlicht.`;
    }

    if (preset.draftedVersion != null) {
        return `Diese Vorlage liegt aktuell nur als Entwurf in Version ${preset.draftedVersion} vor.`;
    }

    return 'Für diese Vorlage liegt aktuell keine veröffentlichte Version vor.';
}

export function PresetInfoTab({
    preset,
    onAddElement,
    primaryActionLabel,
    primaryActionIcon,
    onClose,
}: {
    preset: Preset;
    onAddElement: (element: AnyElement) => void;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    onClose: () => void;
}) {
    const api = useApi();

    const handleAddPreset = () => {
        if (preset.publishedVersion == null) {
            return;
        }

        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        presetVersionApiService
            .retrieve(preset.publishedVersion)
            .then((presetVersion) => {
                onAddElement(cloneElement({
                    ...presetVersion.rootElement,
                    name: preset.title,
                }, true));
            })
            .catch(() => console.error('Fehler beim Laden der Preset-Version'));
    };

    return (
        <SelectionDetailsPanel
            icon={<MenuOutlinedIcon sx={{fontSize: 20, color: 'text.secondary'}}/>}
            label="Vorlage"
            title={preset.title}
            titleAdornment={
                preset.publishedVersion != null ? (
                    <Chip
                        size="small"
                        label={`Version ${preset.publishedVersion}`}
                        sx={{flexShrink: 0}}
                    />
                ) : undefined
            }
            description={getPresetSummary(preset)}
            primaryActionLabel={primaryActionLabel}
            primaryActionIcon={primaryActionIcon}
            onPrimaryAction={handleAddPreset}
            onClose={onClose}
            primaryActionDisabled={preset.publishedVersion == null}
        >
            <PresetInfoSection title="Allgemein">
                <PresetInfoRow label="Schlüssel" value={preset.key}/>
                <PresetInfoRow
                    label="Veröffentlichte Version"
                    value={preset.publishedVersion != null ? `${preset.publishedVersion}` : 'Keine'}
                />
                <PresetInfoRow
                    label="Entwurfsversion"
                    value={preset.draftedVersion != null ? `${preset.draftedVersion}` : 'Keine'}
                />
                <PresetInfoRow label="Erstellt" value={formatDateTime(preset.created)}/>
                <PresetInfoRow label="Zuletzt geändert" value={formatDateTime(preset.updated)}/>
            </PresetInfoSection>
        </SelectionDetailsPanel>
    );
}

function PresetInfoSection({
    title,
    children,
}: {
    title: string;
    children: React.ReactNode;
}) {
    return (
        <Box>
            <Typography
                variant="subtitle2"
                sx={{
                    mb: 1.25,
                    fontWeight: 700,
                }}
            >
                {title}
            </Typography>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1.25,
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

function PresetInfoRow({
    label,
    value,
}: {
    label: string;
    value: string;
}) {
    return (
        <Box sx={{py: 0.25}}>
            <Typography variant="caption" color="text.secondary">
                {label}
            </Typography>
            <Typography variant="body2" sx={{mt: 0.25}}>
                {value}
            </Typography>
        </Box>
    );
}
