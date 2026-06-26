import React, {type ReactNode} from 'react';
import {Box, Button, Chip, Typography} from '@mui/material';
import {AuditLogEntity} from '../../models/audit-log-entity';
import {getActorTypeColor, getActorTypeIcon, getActorTypeLabel} from '../../data/actor-type';
import {getTriggerTypeColor, getTriggerTypeIcon, getTriggerTypeLabel} from '../../data/trigger-type';

interface AuditLogDetailsDialogContentProps {
    row: AuditLogEntity;
    actorLabelsById: Record<string, string | undefined>;
}

function parseDate(value: string): Date | undefined {
    if (value.trim().length === 0) {
        return undefined;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return undefined;
    }

    return date;
}

function formatDateTime(value: string): string {
    const date = parseDate(value);
    if (date == null) {
        return '-';
    }

    return `${new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(date)} Uhr`;
}

function prettyJson(value: Record<string, unknown> | null | undefined): string {
    if (value == null) {
        return '{}';
    }

    return JSON.stringify(value, null, 2);
}

async function copyToClipboard(value: string): Promise<void> {
    if (typeof navigator !== 'undefined' && navigator.clipboard != null) {
        await navigator.clipboard.writeText(value);
    }
}

export function AuditLogDetailsDialogContent(props: AuditLogDetailsDialogContentProps): ReactNode {
    const actorId = props.row.actorId?.trim() || undefined;
    const actorIsUser = props.row.actorType === 'User';
    const actorLabel = actorIsUser
        ? ((actorId != null ? props.actorLabelsById[actorId] : undefined) || actorId || getActorTypeLabel(props.row.actorType))
        : getActorTypeLabel(props.row.actorType);
    const ActorIcon = getActorTypeIcon(props.row.actorType);
    const TriggerIcon = getTriggerTypeIcon(props.row.triggerType);
    const diffText = prettyJson(props.row.diff);
    const metadataText = prettyJson(props.row.metadata);
    const fullEntryText = JSON.stringify(props.row, null, 2);

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
            <Box>
                <Typography variant="subtitle2">Basisdaten</Typography>
                <Typography variant="body2">ID: {props.row.id}</Typography>
                <Typography variant="body2">Zeitpunkt: {formatDateTime(props.row.timestamp)}</Typography>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mt: 0.5}}>
                    <Typography variant="body2">Trigger:</Typography>
                    <Chip
                        size="small"
                        variant="outlined"
                        icon={TriggerIcon != null ? <TriggerIcon/> : undefined}
                        label={getTriggerTypeLabel(props.row.triggerType)}
                        color={getTriggerTypeColor(props.row.triggerType)}
                    />
                </Box>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mt: 0.5}}>
                    <Typography variant="body2">Akteur:</Typography>
                    <Chip
                        size="small"
                        variant="outlined"
                        icon={ActorIcon != null ? <ActorIcon/> : undefined}
                        label={actorLabel}
                        color={getActorTypeColor(props.row.actorType)}
                    />
                    <Typography variant="body2" color="text.secondary">
                        ({actorId ?? '-'})
                    </Typography>
                </Box>
                <Typography variant="body2">Akteur-Typ: {props.row.actorType ?? '-'}</Typography>
                <Typography variant="body2">Entity-Typ: {props.row.entityType ?? '-'}</Typography>
                <Typography variant="body2">Referenz: {props.row.entityRef ?? '-'}</Typography>
                <Typography variant="body2">Referenz-Typ: {props.row.entityRefType ?? '-'}</Typography>
                <Typography variant="body2">Modul: {props.row.module}</Typography>
                <Typography variant="body2">Nachricht: {props.row.message ?? '-'}</Typography>
                <Typography variant="body2">IP-Adresse: {maskIp(props.row.ipAddress)}</Typography>
            </Box>

            <Box>
                <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5}}>
                    <Typography variant="subtitle2">Diff</Typography>
                    <Button
                        size="small"
                        variant="text"
                        onClick={() => {
                            copyToClipboard(diffText)
                                .catch(console.error);
                        }}
                    >
                        Kopieren
                    </Button>
                </Box>
                <Box
                    component="pre"
                    sx={{
                        m: 0,
                        p: 1.25,
                        borderRadius: 1,
                        bgcolor: 'background.default',
                        border: '1px solid',
                        borderColor: 'divider',
                        overflowX: 'auto',
                        fontSize: '0.75rem',
                        lineHeight: 1.5,
                    }}
                >
                    {diffText}
                </Box>
            </Box>

            <Box>
                <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5}}>
                    <Typography variant="subtitle2">Metadata</Typography>
                    <Button
                        size="small"
                        variant="text"
                        onClick={() => {
                            copyToClipboard(metadataText)
                                .catch(console.error);
                        }}
                    >
                        Kopieren
                    </Button>
                </Box>
                <Box
                    component="pre"
                    sx={{
                        m: 0,
                        p: 1.25,
                        borderRadius: 1,
                        bgcolor: 'background.default',
                        border: '1px solid',
                        borderColor: 'divider',
                        overflowX: 'auto',
                        fontSize: '0.75rem',
                        lineHeight: 1.5,
                    }}
                >
                    {metadataText}
                </Box>
            </Box>
        </Box>
    );
}

function maskIp(ip: string | null | undefined): string {
    if (ip == null) {
        return '-';
    }
    const parts = ip.split('.');
    if (parts.length !== 4) {
        return ip;
    }
    return `${parts[0]}.${parts[1]}.***.***`;
}
