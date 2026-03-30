import React, {useMemo, useState} from 'react';
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    FormControlLabel,
    Stack,
    Typography,
} from '@mui/material';
import {format} from 'date-fns';
import {AppInfo} from '../../app-info';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {StorageKey} from '../../data/storage-key';
import {StorageScope, StorageService} from '../../services/storage-service';

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
            onClose={handleClose}
            maxWidth="sm"
            fullWidth
        >
            <DialogTitleWithClose onClose={handleClose}>
                Hinweis zur Alpha-Version
            </DialogTitleWithClose>

            <DialogContent dividers>
                <Stack spacing={2.5}>
                    <Alert severity="warning">
                        Diese Gover-Version ist eine Alpha-Version und nicht für den produktiven Einsatz vorgesehen.
                    </Alert>

                    <Typography>
                        Die aktuelle Version wird sich in den kommenden Releases weiter verändern. Funktionen können
                        noch unvollständig sein, fehlen oder sich fachlich und technisch ändern.
                    </Typography>

                    <Typography fontWeight={600}>
                        Bitte verwenden Sie diese Version nicht in Produktion.
                    </Typography>

                    <Box
                        sx={{
                            px: 2,
                            py: 1.75,
                            borderRadius: 2,
                            bgcolor: 'grey.100',
                        }}
                    >
                        <Stack spacing={1.25}>
                            <Typography
                                variant="overline"
                                color="text.secondary"
                            >
                                Build-Informationen
                            </Typography>

                            <Stack
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
                                <Typography color="text.secondary">Version</Typography>
                                <Typography
                                    fontWeight={600}
                                    textAlign={{
                                        xs: 'left',
                                        sm: 'right',
                                    }}
                                >
                                    {buildInfo.versionLabel}
                                </Typography>
                            </Stack>

                            <Stack
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
                                <Typography color="text.secondary">Aktueller Build</Typography>
                                <Typography
                                    fontWeight={600}
                                    textAlign={{
                                        xs: 'left',
                                        sm: 'right',
                                    }}
                                >
                                    {buildInfo.buildLabel}
                                </Typography>
                            </Stack>

                            <Stack
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
                                <Typography color="text.secondary">Build-Datum</Typography>
                                <Typography
                                    fontWeight={600}
                                    textAlign={{
                                        xs: 'left',
                                        sm: 'right',
                                    }}
                                >
                                    {buildInfo.buildDateLabel}
                                </Typography>
                            </Stack>
                        </Stack>
                    </Box>

                    <FormControlLabel
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
                </Stack>
            </DialogContent>

            <DialogActions sx={{px: 3, py: 2}}>
                <Button
                    variant="contained"
                    onClick={handleClose}
                >
                    Verstanden
                </Button>
            </DialogActions>
        </Dialog>
    );
}
