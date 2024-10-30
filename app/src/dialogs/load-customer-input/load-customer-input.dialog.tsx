import React, {useEffect, useMemo, useState} from 'react';
import {Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Typography} from '@mui/material';
import {CustomerInputService} from '../../services/customer-input-service';
import {useDispatch} from 'react-redux';
import {format} from 'date-fns';
import {Form as Application} from '../../models/entities/form';
import {CustomerInput} from '../../models/customer-input';
import SettingsBackupRestoreOutlinedIcon from '@mui/icons-material/SettingsBackupRestoreOutlined';
import {hydrateCustomerInput, selectHasLoadedSavedCustomerInput, setHasLoadedSavedCustomerInput} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useSearchParams} from 'react-router-dom';
import {CodeQueryKey} from '../../components/id-input/id-input';
import {selectCurrentStep} from '../../slices/stepper-slice';
import RestorePageIcon from '@mui/icons-material/RestorePage';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';

interface LoadUserInputDialogProps {
    form: Application;
}

export function LoadCustomerInputDialog({form}: LoadUserInputDialogProps) {
    const [searchParams, _] = useSearchParams();
    const dispatch = useDispatch();
    const [lastSaveDate, setLastSaveDate] = useState<Date | null>(null);
    const [lastSaveData, setLastSaveData] = useState<CustomerInput | null>(null);
    const hasLoadedSavedCustomerInput = useAppSelector(selectHasLoadedSavedCustomerInput());
    const currentStep = useAppSelector(selectCurrentStep);
    const isIdpCodePresent = useMemo(() => searchParams.get(CodeQueryKey) != null, [searchParams]);

    useEffect(() => {
        if (currentStep === 0) {
            setLastSaveDate(CustomerInputService.loadCustomerInputDate(form));
            setLastSaveData(CustomerInputService.loadCustomerInputState(form));
        } else {
            setLastSaveDate(null);
            setLastSaveData(null);
        }
    }, [form]);

    const handleClose = () => {
        setLastSaveDate(null);
    };

    const handleRestart = () => {
        CustomerInputService.cleanCustomerInput(form);
        handleClose();
    };

    const handleLoad = () => {
        if (lastSaveData != null) {
            dispatch(hydrateCustomerInput(lastSaveData));
            dispatch(setHasLoadedSavedCustomerInput(true));
        }
        handleClose();
    };

    return (
        <Dialog
            open={lastSaveDate != null && lastSaveData != null && Object.keys(lastSaveData).length > 0 && !hasLoadedSavedCustomerInput && !isIdpCodePresent}
            disableEscapeKeyDown={true}
        >
            <DialogTitle>
                Möchten Sie den existierenden Entwurf fortführen?
            </DialogTitle>
            <DialogContent tabIndex={0}>
                <DialogContentText component="div">
                    <Typography variant="body2">
                        Auf Ihrem Gerät existiert ein zwischengespeicherter Entwurf für diesen Antrag. Möchten Sie
                        diesen Entwurf verwenden und weiter bearbeiten?
                    </Typography>

                    <Box
                        display="flex"
                        justifyContent="center"
                        alignItems={"center"}
                        sx={{
                            border: '1px solid #DFDFDF',
                            px: 4,
                            py: 2,
                            mt: 3,
                            mb: 3,
                        }}
                    >
                        <Box>
                            <RestorePageIcon color={'primary'}
                                             sx={{fontSize: 54}}/>
                        </Box>

                        <Box sx={{ml: 2}}>
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{
                                    color: '#16191F',
                                    mt: -0.5,
                                }}
                            >
                                Antrags-Entwurf aus Ihrem lokalen Speicher
                            </Typography>
                            <Typography
                                variant="body2"
                                sx={{
                                    mt: -0.5,
                                }}
                            >
                                Zuletzt bearbeitet am {
                                lastSaveDate && format(lastSaveDate, 'dd.MM.yyyy')
                            } um {
                                lastSaveDate && format(lastSaveDate, 'HH:mm')
                            } Uhr
                            </Typography>
                        </Box>
                    </Box>

                    <Typography variant="body2" gutterBottom>
                        Bitte beachten Sie, dass Sie aus Datenschutzgründen ggf. folgende Aktionen <b>erneut
                        ausführen</b> müssen, da diese nicht gespeichert wurden:
                    </Typography>
                    <ul style={{margin: 0}}>
                        <li>Anmeldung mit einem Nutzer- oder Unternehmenskonto</li>
                        <li>Hinzufügen von Anlagen/Dateien</li>
                    </ul>
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={handleLoad}
                    variant="contained"
                    startIcon={
                        <ArrowForwardOutlinedIcon/>
                    }
                >
                    Entwurf fortführen
                </Button>
                <Button
                    onClick={handleRestart}
                    startIcon={
                        <SettingsBackupRestoreOutlinedIcon/>
                    }
                >
                    Neu beginnen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
