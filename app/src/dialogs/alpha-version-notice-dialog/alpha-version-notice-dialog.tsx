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
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';
import {alpha} from '@mui/material/styles';
import {format} from 'date-fns';
import {AppInfo} from '../../app-info';
import {StorageKey} from '../../data/storage-key';
import {StorageScope, StorageService} from '../../services/storage-service';

const alphaVersionRiskHints = [
    'Funktionen können unvollständig sein, sich ändern oder nicht wie erwartet funktionieren.',
    'Inhalte und Verhalten können sich in zukünftigen Versionen ohne Vorankündigung ändern.',
];

export function AlphaVersionNoticeDialog(): React.ReactElement {
    const [
        open,
        setOpen,
    ] = useState(() => !StorageService.loadFlag(StorageKey.AlphaVersionNoticeDismissed));
    const [
        dismissForSession,
        setDismissForSession,
    ] = useState(false);

    const buildInfo = useMemo(() => {
        const parsedBuildDate = new Date(AppInfo.date);
        const hasBuildVersion = AppInfo.version !== '@buildVersion';
        const hasBuildNumber = AppInfo.number !== '@buildNumber';
        const hasBuildDate = AppInfo.date !== '@buildTimestamp' && !Number.isNaN(parsedBuildDate.getTime());

        return {
            versionLabel: hasBuildVersion ? AppInfo.version : '5.x (DEV)',
            buildLabel: hasBuildNumber ? AppInfo.number : 'Entwicklungsbuild',
            buildDateLabel: hasBuildDate ?
                `${format(parsedBuildDate, 'dd.MM.yyyy HH:mm')} Uhr` :
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
            StorageService.storeFlag(
                StorageKey.AlphaVersionNoticeDismissed,
                true,
                StorageScope.Session,
            );
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
            disableEscapeKeyDown
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
                    spacing={1.75}
                    alignItems="center"
                >
                    <Box
                        sx={{
                            width: 44,
                            height: 44,
                            borderRadius: '50%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            bgcolor: (theme) => alpha(theme.palette.warning.main, 0.12),
                            color: 'warning.main',
                            flexShrink: 0,
                        }}
                    >
                        <ReportOutlinedIcon />
                    </Box>

                    <Box>
                        <Typography
                            variant="overline"
                            sx={{
                                display: 'block',
                                color: 'warning.dark',
                                lineHeight: 1.2,
                            }}
                        >
                            Alpha-Version
                        </Typography>

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
                                Sie verwenden eine Vorabversion dieser Anwendung. Diese dient zu Test- und Evaluationszwecken und ist nicht für den produktiven Einsatz vorgesehen.
                            </Typography>

                            <Box
                                component="ul"
                                sx={{
                                    m: 0,
                                    pl: 3,
                                }}
                            >
                                {
                                    alphaVersionRiskHints.map((riskHint, index) => (
                                        <Typography
                                            component="li"
                                            key={riskHint}
                                            sx={{
                                                mb: index < alphaVersionRiskHints.length - 1 ? 1 : 0,
                                            }}
                                        >
                                            {riskHint}
                                        </Typography>
                                    ))
                                }
                            </Box>

                            <Typography fontWeight={700}>
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
                                    color="text.secondary"
                                >
                                    Build-Informationen
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
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
                                            justifyContent="space-between"
                                            alignItems={{
                                                xs: 'flex-start',
                                                sm: 'center',
                                            }}
                                            spacing={2}
                                        >
                                            <Typography color="text.secondary">
                                                {row.label}
                                            </Typography>
                                            <Typography
                                                fontWeight={600}
                                                textAlign={{
                                                    xs: 'left',
                                                    sm: 'right',
                                                }}
                                            >
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
