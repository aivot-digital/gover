import React, {useEffect, useMemo, useState} from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from '@mui/material/Typography';
import {CustomerInputService} from '../../services/customer-input-service';
import {useDispatch} from 'react-redux';
import {format} from 'date-fns';
import {type Form as Application} from '../../models/entities/form';
import SettingsBackupRestoreOutlinedIcon from '@mui/icons-material/SettingsBackupRestoreOutlined';
import {flagLoadedSavedCustomerInput, selectHasLoadedSavedCustomerInput} from '../../slices/app-slice';
import {useSearchParams} from 'react-router-dom';
import RestorePageIcon from '@mui/icons-material/RestorePage';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import {IdentityStateQueryParam} from '../../modules/identity/constants/identity-state-query-param';
import {type ElementData, hasElementDataSomeInput} from '../../models/element-data';
import {IdentityIdQueryParam} from '../../modules/identity/constants/identity-id-query-param';
import {useAppSelector} from '../../hooks/use-app-selector';

interface LoadUserInputDialogProps {
    form: Application;
    onElementDataLoad: (elementData: ElementData) => void;
}

export function LoadCustomerInputDialog(props: LoadUserInputDialogProps) {
    const {
        form,
        onElementDataLoad,
    } = props;

    const [searchParams, _] = useSearchParams();
    const dispatch = useDispatch();

    const hasLoadedSavedCustomerInput = useAppSelector(selectHasLoadedSavedCustomerInput);

    const [lastSaveDate, setLastSaveDate] = useState<Date>();
    const [lastSaveData, setLastSaveData] = useState<ElementData>();

    useEffect(() => {
        setLastSaveDate(CustomerInputService.loadCustomerInputDate(form) ?? undefined);
        setLastSaveData(CustomerInputService.loadCustomerInputState(form) ?? undefined);
    }, [form]);

    const isReturningFromIdp = useMemo(() => {
        return (
            searchParams.get(IdentityStateQueryParam) != null &&
            searchParams.get(IdentityIdQueryParam) != null
        );
    }, [searchParams]);

    const isOpen = useMemo(() => {
        return (
            !isReturningFromIdp &&
            !hasLoadedSavedCustomerInput &&
            hasElementDataSomeInput(lastSaveData)
        );
    }, [lastSaveData, isReturningFromIdp, hasLoadedSavedCustomerInput]);

    const handleClose = () => {
        setLastSaveDate(undefined);
        setLastSaveData(undefined);
    };

    const handleRestart = () => {
        CustomerInputService.cleanCustomerInput(form);
        handleClose();
    };

    const handleLoad = () => {
        if (lastSaveData != null) {
            onElementDataLoad(lastSaveData);
            dispatch(flagLoadedSavedCustomerInput());
        }
        handleClose();
    };

    return (
        <Dialog
            open={isOpen}
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
                        alignItems={'center'}
                        sx={{
                            border: '1px solid #DFDFDF',
                            px: 4,
                            py: 2,
                            mt: 3,
                            mb: 3,
                        }}
                    >
                        <Box>
                            <RestorePageIcon
                                color={'primary'}
                                sx={{fontSize: 54}}
                            />
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

                    <Typography
                        variant="body2"
                        gutterBottom
                    >
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
                        <ArrowForwardOutlinedIcon />
                    }
                >
                    Entwurf fortführen
                </Button>
                <Button
                    onClick={handleRestart}
                    startIcon={
                        <SettingsBackupRestoreOutlinedIcon />
                    }
                >
                    Neu beginnen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
