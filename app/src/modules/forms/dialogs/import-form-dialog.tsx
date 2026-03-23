import {Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogProps} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {FileUpload} from '../../../components/file-upload/file-upload';
import {hideLoadingOverlay, hideLoadingOverlayWithTimeout, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useApi} from '../../../hooks/use-api';
import {FormExport} from '../entities/form-export';
import {FormApiService} from '../services/form-api-service';
import {FormEntity} from '../entities/form-entity';
import {FormVersionEntity} from '../entities/form-version-entity';
import {VFormVersionWithDetailsEntity} from '../entities/v-form-version-with-details-entity';

export interface ImportFormDialogProps extends DialogProps {
    onClose: () => void;
    onImport: (form: FormEntity, version: FormVersionEntity) => void;
}

export function ImportFormDialog(props: ImportFormDialogProps) {
    const {
        onClose,
        onImport,
        ...passTroughProps
    } = props;

    const api = useApi();
    const dispatch = useAppDispatch();

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
                        new FormApiService()
                            .xdfTransform(value)
                            .then((parsedModel: VFormVersionWithDetailsEntity) => {
                                onImport(parsedModel as FormEntity, parsedModel as FormVersionEntity);
                                dispatch(hideLoadingOverlayWithTimeout(2000));
                            })
                            .catch((err) => {
                                console.error(err);
                                setImportFailed(true);
                                dispatch(hideLoadingOverlay());
                            });
                    } else {
                        try {
                            const parsedModel: FormExport = JSON.parse(value.toString());
                            onImport(parsedModel.form, parsedModel.version);
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
