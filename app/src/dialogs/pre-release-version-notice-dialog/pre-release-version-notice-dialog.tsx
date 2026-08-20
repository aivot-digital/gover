import React, {useMemo, useState} from 'react';
import {
    Box,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    Stack,
    Typography,
} from '@mui/material';
import ReportOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Report';
import {alpha} from '@mui/material/styles';
import {AppInfo} from '../../app-info';
import {StorageKey} from '../../data/storage-key';
import {StorageScope, StorageService} from '../../services/storage-service';
import {formatInstantInApplicationTimeZone} from '../../utils/temporal-utils';
import {
    getPreReleaseNoticeDismissalExpiry,
    isPreReleaseNoticeDismissalActive,
} from './pre-release-version-notice-time';

const preReleaseVersionRiskHints = [
    'Funktionen können unvollständig sein, sich ändern oder noch nicht wie erwartet funktionieren.',
    'Inhalte und Verhalten können sich in zukünftigen Versionen ohne Vorankündigung ändern.',
];

function storeDismissFlag() {
    StorageService.storeString(
        StorageKey.PreReleaseVersionNoticeDismissed,
        getPreReleaseNoticeDismissalExpiry(),
        StorageScope.Local,
    );
}

function loadDismissFlag(): boolean {
    const dismissedUntil = StorageService.loadString(StorageKey.PreReleaseVersionNoticeDismissed);

    return isPreReleaseNoticeDismissalActive(dismissedUntil);
}

export function PreReleaseVersionNoticeDialog(): React.ReactElement {
    const [
        open,
        setOpen,
    ] = useState(() => !loadDismissFlag());

    const [
        dismissForSession,
        setDismissForSession,
    ] = useState(false);

    const buildInfo = useMemo(() => {
        const hasBuildVersion = AppInfo.version !== '@buildVersion';
        const hasBuildNumber = AppInfo.number !== '@buildNumber';
        const formattedBuildDate = formatInstantInApplicationTimeZone(AppInfo.date, 'dd.MM.yyyy');
        const hasBuildDate = AppInfo.date !== '@buildTimestamp' && formattedBuildDate != null;

        return {
            versionLabel: hasBuildVersion ? AppInfo.version : '5.x (DEV)',
            buildLabel: hasBuildNumber ? AppInfo.number : 'Entwicklungsbuild',
            buildDateLabel: hasBuildDate ?
                formattedBuildDate :
                'Nicht im Build hinterlegt',
        };
    }, []);

    const buildInfoRows = [
        {
            label: 'Version',
            value: buildInfo.versionLabel,
        },
        {
            label: 'Build-Nummer',
            value: buildInfo.buildLabel,
        },
        {
            label: 'Build-Datum',
            value: buildInfo.buildDateLabel,
        },
    ];

    const handleClose = (): void => {
        if (dismissForSession) {
            storeDismissFlag();
        }

        setOpen(false);
    };

    return (
        <Dialog
            open={open}
            onClose={(_event, reason) => {
                if (reason === 'backdropClick' || reason === 'escapeKeyDown') {
                    return;
                }

                handleClose();
            }}
            maxWidth="sm"
            fullWidth
        >
            <DialogTitle
                sx={{
                    px: 3,
                    pt: 3,
                    pb: 1.5,
                }}
            >
                <Stack
                    direction="row"
                    spacing={1.25}
                    sx={{
                        alignItems: "center"
                    }}
                >
                    <ReportOutlinedIcon sx={{
                        color: 'warning.main',
                    }}/>

                    <Box>
                        <Typography
                            variant="h4"
                            component="div"
                        >
                            Wichtiger Hinweis
                        </Typography>
                    </Box>
                </Stack>
            </DialogTitle>

            <DialogContent
                sx={{
                    pt: 1,
                    pb: 2,
                }}
            >
                <Stack spacing={3} sx={{mt: 1}}>
                    <Box
                        sx={{
                            p: {
                                xs: 2,
                                sm: 2.5,
                            },
                            borderRadius: 2.5,
                            border: '1px solid',
                            borderColor: (theme) => alpha(theme.palette.warning.main, 0.22),
                            bgcolor: (theme) => alpha(theme.palette.warning.main, 0.08),
                        }}
                    >
                        <Stack spacing={2}>
                            <Typography>
                                Sie verwenden eine Vorabversion dieser Anwendung. Diese dient zu Test- und Evaluationszwecken und ist nicht für den produktiven Einsatz vorgesehen. Das bedeutet:
                            </Typography>

                            <Box
                                component="ul"
                                sx={{
                                    m: 0,
                                    pl: 3,
                                }}
                            >
                                {
                                    preReleaseVersionRiskHints.map((riskHint, index) => (
                                        <Typography
                                            component="li"
                                            key={riskHint}
                                            sx={{
                                                mb: index < preReleaseVersionRiskHints.length - 1 ? 1 : 0,
                                            }}
                                        >
                                            {riskHint}
                                        </Typography>
                                    ))
                                }
                            </Box>

                            <Typography sx={{
                                fontWeight: 700
                            }}>
                                Bitte verwenden Sie diese Version nicht im regulären Betrieb.
                            </Typography>
                        </Stack>
                    </Box>

                    <Box
                        sx={{
                            px: 2.25,
                            py: 2,
                            borderRadius: 2.5,
                            border: '1px solid',
                            borderColor: 'divider',
                            bgcolor: 'background.paper',
                        }}
                    >
                        <Stack spacing={1.75}>
                            <Box>
                                <Typography
                                    variant="overline"
                                    sx={{
                                        color: "text.secondary"
                                    }}
                                >
                                    Build-Informationen
                                </Typography>

                                <Typography
                                    variant="body2"
                                    sx={{
                                        color: "text.secondary"
                                    }}
                                >
                                    Hilfreich bei Rückfragen und Fehlerberichten zu dieser Vorabversion.
                                </Typography>
                            </Box>

                            <Stack spacing={1.25}>
                                {
                                    buildInfoRows.map((row) => (
                                        <Stack
                                            key={row.label}
                                            direction={{
                                                xs: 'column',
                                                sm: 'row',
                                            }}
                                            spacing={2}
                                            sx={{
                                                justifyContent: "space-between",

                                                alignItems: {
                                                    xs: 'flex-start',
                                                    sm: 'center',
                                                }
                                            }}>
                                            <Typography sx={{
                                                color: "text.secondary"
                                            }}>
                                                {row.label}
                                            </Typography>
                                            <Typography
                                                sx={{
                                                    fontWeight: 600,

                                                    textAlign: {
                                                        xs: 'left',
                                                        sm: 'right',
                                                    }
                                                }}>
                                                {row.value}
                                            </Typography>
                                        </Stack>
                                    ))
                                }
                            </Stack>
                        </Stack>
                    </Box>
                </Stack>
            </DialogContent>

            <DialogActions
                sx={{
                    px: 3,
                    pb: 3,
                    pt: 1,
                    justifyContent: 'flex-start',
                    alignItems: 'center',
                    gap: 2,
                    flexWrap: 'wrap',
                }}
            >
                <Button
                    variant="contained"
                    onClick={handleClose}
                >
                    Verstanden
                </Button>
                <FormControlLabel
                    sx={{
                        mr: 0,
                    }}
                    control={
                        <Checkbox
                            checked={dismissForSession}
                            onChange={(event) => {
                                setDismissForSession(event.target.checked);
                            }}
                        />
                    }
                    label="Für diese Sitzung nicht erneut anzeigen"
                />
            </DialogActions>
        </Dialog>
    );
}
