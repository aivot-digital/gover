import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {type ImportApplicationDialogProps} from './import-application-dialog-props';
import {FileUpload} from '../../../components/file-upload/file-upload';
import {type Form as Application} from '../../../models/entities/form';
import {ApplicationStatus} from '../../../data/application-status';
import {stripDataFromForm} from '../../../utils/strip-data-from-form';
import {
    hideLoadingOverlay,
    hideLoadingOverlayWithTimeout,
    showLoadingOverlay
} from "../../../slices/loading-overlay-slice";
import {useDispatch} from "react-redux";

export function ImportApplicationDialog(props: ImportApplicationDialogProps): JSX.Element {
    const {
        onClose,
        onImport,
        ...passTroughProps
    } = props;
    const dispatch = useDispatch();

    const [importFailed, setImportFailed] = useState(false);

    const onFileSelect = (files: File[]): void => {
        setImportFailed(false);
        if (files.length > 0) {
            dispatch(showLoadingOverlay('Formular wird importiert'));

            const reader = new FileReader();
            reader.readAsText(files[0], 'UTF-8');
            reader.onload = function (evt): void {
                const value = evt.target?.result;
                if (value != null) {
                    if (files[0].name.endsWith('.xml')) {
                        fetch('https://xdf2gov.gover.digital/', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'text/xml',
                            },
                            body: value,
                        })
                            .then((res) => {
                                if (res.status !== 200) {
                                    throw new Error('Invalid response');
                                }
                                return res.json();
                            })
                            .then((parsedModel: Application) => {
                                onImport(stripDataFromForm(parsedModel));
                                dispatch(hideLoadingOverlayWithTimeout(2000));
                            })
                            .catch((err) => {
                                console.error(err);
                                setImportFailed(true);
                                dispatch(hideLoadingOverlay());
                            });
                    } else {
                        try {
                            const parsedModel: Application = JSON.parse(value.toString());

                            parsedModel.id = 0;

                            // Clear application
                            parsedModel.destinationId = null;
                            parsedModel.status = ApplicationStatus.Drafted;

                            parsedModel.developingDepartmentId = 0;

                            parsedModel.managingDepartmentId = null;
                            parsedModel.responsibleDepartmentId = null;
                            parsedModel.imprintDepartmentId = null;
                            parsedModel.privacyDepartmentId = null;
                            parsedModel.accessibilityDepartmentId = null;
                            parsedModel.legalSupportDepartmentId = null;
                            parsedModel.technicalSupportDepartmentId = null;
                            parsedModel.paymentProvider = undefined;
                            parsedModel.themeId = null;

                            onImport(parsedModel);
                            handleClose();
                            dispatch(hideLoadingOverlayWithTimeout(1000));
                        } catch (err) {
                            setImportFailed(true);
                            dispatch(hideLoadingOverlay());
                        }
                    }
                } else {
                    setImportFailed(true);
                    dispatch(hideLoadingOverlay());
                }
            };
            reader.onerror = function () {
                setImportFailed(true);
                dispatch(hideLoadingOverlay());
            };
        } else {
            setImportFailed(true);
            dispatch(hideLoadingOverlay());
        }
    };

    const handleClose = () => {
        setImportFailed(false);
        onClose();
    };

    return (
        <Dialog
            {...passTroughProps}
            onClose={handleClose}
            fullWidth={true}
        >
            <DialogTitleWithClose
                onClose={handleClose}
            >
                Bestehendes Formular importieren
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>

                <DialogContentText>
                    Importieren Sie ein Formular im GOV-Format (*.gov) oder ein XDatenfeld-Schema (*.xml).
                </DialogContentText>
                <Box
                    sx={{
                        mt: 2,
                    }}
                >
                    <FileUpload
                        onChange={onFileSelect}
                        value={[]}
                        extensions={['gov', 'xml']}
                    />
                </Box>

                {
                    importFailed &&
                    <Alert
                        sx={{
                            mt: 4,
                        }}
                        severity="error"
                    >
                        Die Datei konnte nicht importiert werden.
                    </Alert>
                }
            </DialogContent>
            <DialogActions
            >
                <Button
                    onClick={handleClose}
                    sx={{
                        ml: 'auto!important',
                    }}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
