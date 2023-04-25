import React, {useEffect, useState} from 'react';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Typography
} from '@mui/material';
import {UserInputService} from '../../services/user-input.service';
import {useDispatch} from 'react-redux';
import {setUserInput} from '../../slices/customer-input-slice';
import {format} from 'date-fns';
import {faArrowRotateLeft, faFileImport} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {PrivacyUserInputKey} from '../../components/general-information/general-information.component.view';
import {Application} from '../../models/entities/application';
import {Logo} from '../../components/static-components/logo/logo';
import {CustomerInput} from "../../models/customer-input";
import {isFileUploadElementItem} from "../../models/elements/form/input/file-upload-element";

interface LoadUserInputDialogProps {
    application: Application;
}

export function LoadUserInputDialog({application}: LoadUserInputDialogProps) {
    const dispatch = useDispatch();
    const [lastSaveDate, setLastSaveDate] = useState<Date | null>(null);
    const [lastSaveData, setLastSaveData] = useState<CustomerInput | null>(null);

    useEffect(() => {
        setLastSaveDate(UserInputService.loadUserInputDate(application));
        setLastSaveData(UserInputService.loadUserInputState(application));
    }, [application]);

    const handleClose = () => {
        setLastSaveDate(null);
    };

    const handleRestart = () => {
        UserInputService.cleanUserInput(application);
        handleClose();
    };

    const handleLoad = () => {
        if (lastSaveData != null) {
            const cleanedSaveData = {
                ...lastSaveData,
            };
            for (const key of Object.keys(cleanedSaveData)) {
                const val = cleanedSaveData[key];
                if (Array.isArray(val) && val.length > 0 && isFileUploadElementItem(val[0])) {
                    delete cleanedSaveData[key];
                }
            }
            dispatch(setUserInput(cleanedSaveData));
        }
        handleClose();
    };

    return (
        <Dialog
            open={lastSaveDate != null && ((lastSaveData ?? {})[PrivacyUserInputKey] ?? false)}
            disableEscapeKeyDown={true}
        >
            <DialogTitle>
                Möchten Sie den existierenden Entwurf fortführen?
            </DialogTitle>
            <DialogContent>
                <DialogContentText component="div">
                    <Typography variant="body2">
                        Auf Ihrem Gerät existiert ein zwischengespeicherter Entwurf für diesen Antrag. Möchten Sie
                        diesen Entwurf verwenden und weiter bearbeiten?
                    </Typography>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'center',
                            border: '1px solid #DFDFDF',
                            px: 4,
                            py: 2,
                            mt: 3,
                        }}
                    >
                        <Box
                            sx={{
                                height: '4rem',
                            }}
                        >
                            <Logo
                                height={100}
                                width={200}
                            />
                        </Box>
                        <Box sx={{ml: 2}}>
                            <Typography
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

                    <Alert severity="warning" sx={{mt: 2}}>
                        Bitte beachten Sie, dass aus Datenschutzgründen zuvor hinzugefügte Anlagen <strong>nicht</strong> zwischengespeichert wurden.
                        Sie werden automatisch an den entsprechenden Stellen darauf hingewiesen, diese erneut hinzuzufügen.
                    </Alert>
                </DialogContentText>
            </DialogContent>
            <DialogActions
                sx={{
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start'
                }}
            >
                <Button
                    onClick={handleLoad}
                    size="large"
                    variant="contained"
                    startIcon={
                        <FontAwesomeIcon
                            icon={faFileImport}
                            style={{
                                marginTop: '-4px',
                            }}
                            fixedWidth
                        />
                    }
                >
                    Entwurf fortführen
                </Button>
                <Button
                    onClick={handleRestart}
                    sx={{
                        ml: 'auto!important',
                    }}
                    size="large"
                    startIcon={
                        <FontAwesomeIcon
                            icon={faArrowRotateLeft}
                            style={{
                                marginTop: '-4px',
                            }}
                            fixedWidth
                        />
                    }
                >
                    Neu beginnen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
