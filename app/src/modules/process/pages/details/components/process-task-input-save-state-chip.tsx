import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Chip as MuiChip, CircularProgress, Tooltip} from '@mui/material';
import CloudAlert from '@aivot/mui-material-symbols-400-outlined/dist/cloud-alert/CloudAlert';
import CloudDone from '@aivot/mui-material-symbols-400-outlined/dist/cloud-done/CloudDone';
import {formatDistanceToNowStrict} from 'date-fns';
import {de} from 'date-fns/locale';

export enum ProcessTaskInputSaveState {
    Saved,
    Waiting,
    Saving,
    Failed,
}

interface ProcessTaskInputSaveStateChipProps {
    state: ProcessTaskInputSaveState;
    lastSavedAt?: Date | null;
}

function getRelativeLastSavedAt(lastSavedAt: Date): string {
    const diffMs = Date.now() - lastSavedAt.getTime();
    const underOneMinute = diffMs >= 0 && diffMs < 60_000;

    if (underOneMinute) {
        return 'vor weniger als einer Minute';
    }

    return formatDistanceToNowStrict(lastSavedAt, {
        addSuffix: true,
        locale: de,
    });
}

export function ProcessTaskInputSaveStateChip(props: ProcessTaskInputSaveStateChipProps): ReactNode {
    const {
        state,
        lastSavedAt,
    } = props;

    const [minuteTick, setMinuteTick] = useState(0);

    useEffect(() => {
        if (lastSavedAt == null) {
            return;
        }

        const intervalId = window.setInterval(() => {
            setMinuteTick((current) => current + 1);
        }, 60_000);

        return () => {
            window.clearInterval(intervalId);
        };
    }, [lastSavedAt]);

    const relativeLastSavedAt = useMemo(() => {
        if (lastSavedAt == null) {
            return null;
        }

        return getRelativeLastSavedAt(lastSavedAt);
    }, [lastSavedAt, minuteTick]);

    const chipProps = useMemo(() => {
        const iconSlotSx = {
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 20,
            minWidth: 20,
            height: 20,
            lineHeight: 0,
        } as const;

        if (state === ProcessTaskInputSaveState.Saved) {
            return {
                label: 'Eingaben wurden zwischengespeichert',
                tooltip: relativeLastSavedAt == null
                    ? 'Ihre Eingaben wurden im Vorgang zwischengespeichert.'
                    : `Ihre Eingaben wurden im Vorgang zwischengespeichert. Zuletzt gespeichert ${relativeLastSavedAt}.`,
                icon: (
                    <Box
                        component="span"
                        sx={{
                            ...iconSlotSx,
                            color: (theme) => theme.palette.primary.main,
                        }}
                    >
                        <CloudDone fontSize="small"/>
                    </Box>
                ),
            };
        }

        if (state === ProcessTaskInputSaveState.Waiting) {
            return {
                label: 'Ungespeicherte Eingaben vorhanden',
                tooltip: 'Es liegen ungespeicherte Eingaben vor, die in Kürze automatisch zwischengespeichert werden.',
                icon: (
                    <Box
                        component="span"
                        sx={{
                            ...iconSlotSx,
                            color: (theme) => theme.palette.grey['600'],
                        }}
                    >
                        <CloudAlert fontSize="small"/>
                    </Box>
                ),
            };
        }

        if (state === ProcessTaskInputSaveState.Failed) {
            return {
                label: 'Zwischenspeichern fehlgeschlagen',
                tooltip: relativeLastSavedAt == null
                    ? 'Ihre Eingaben konnten nicht automatisch zwischengespeichert werden. Bitte prüfen Sie Ihre Internetverbindung.'
                    : `Ihre Eingaben konnten nicht automatisch zwischengespeichert werden. Bitte prüfen Sie Ihre Internetverbindung. Letzte erfolgreiche Zwischenspeicherung ${relativeLastSavedAt}.`,
                icon: (
                    <Box
                        component="span"
                        sx={{
                            ...iconSlotSx,
                            color: (theme) => theme.palette.warning.main,
                        }}
                    >
                        <CloudAlert
                            fontSize="small"
                            color="error"
                        />
                    </Box>
                ),
            };
        }

        return {
            label: 'Eingaben werden zwischengespeichert',
            tooltip: 'Ihre Eingaben werden gerade zwischengespeichert.',
            icon: (
                <Box
                    component="span"
                    sx={iconSlotSx}
                >
                    <CircularProgress
                        size={16}
                        thickness={5}
                    />
                </Box>
            ),
        };
    }, [relativeLastSavedAt, state]);

    return (
        <Tooltip
            title={chipProps.tooltip}
            arrow
        >
            <MuiChip
                sx={{
                    ml: 'auto',
                    width: {
                        xs: '100%',
                        sm: '18.25rem',
                    },
                    maxWidth: '100%',
                    alignItems: 'center',
                    justifyContent: 'flex-start',
                    '& .MuiChip-label': {
                        display: 'block',
                        width: '100%',
                        px: 1.5,
                        py: 0.5,
                    },
                }}
                color={state === ProcessTaskInputSaveState.Failed ? 'error' : undefined}
                label={
                    <Box
                        component="span"
                        sx={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 1,
                            width: '100%',
                            minHeight: 28,
                        }}
                    >
                        {chipProps.icon}
                        <Box
                            component="span"
                            sx={{
                                display: 'block',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {chipProps.label}
                        </Box>
                    </Box>
                }
                variant="outlined"
            />
        </Tooltip>
    );
}
