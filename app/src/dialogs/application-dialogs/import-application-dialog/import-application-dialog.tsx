import {Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogContentText} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {ImportApplicationDialogProps} from './import-application-dialog-props';
import {FileUpload} from '../../../components/file-upload/file-upload';
import {Application} from "../../../models/entities/application";


export function ImportApplicationDialog(props: ImportApplicationDialogProps) {
    const {
        onClose,
        onImport,
        ...passTroughProps
    } = props;

    const [importFailed, setImportFailed] = useState(false);

    const onFileSelect = (files: File[]) => {
        setImportFailed(false);
        if (files.length > 0) {
            const reader = new FileReader();
            reader.readAsText(files[0], 'UTF-8');
            reader.onload = function (evt) {
                const value = evt.target?.result;
                if (value != null) {
                    try {
                        const parsedModel: Application = JSON.parse(value.toString());

                        // Clear application
                        parsedModel.destination = undefined;

                        parsedModel.developingDepartment = 0;

                        parsedModel.managingDepartment = undefined;
                        parsedModel.responsibleDepartment = undefined;
                        parsedModel.imprintDepartment = undefined;
                        parsedModel.privacyDepartment = undefined;
                        parsedModel.accessibilityDepartment = undefined;
                        parsedModel.legalSupportDepartment = undefined;
                        parsedModel.technicalSupportDepartment = undefined;

                        onImport(parsedModel);
                        handleClose();
                    } catch (err) {
                        setImportFailed(true);
                        return;
                    }
                } else {
                    setImportFailed(true);
                    return;
                }
            };
            reader.onerror = function () {
                setImportFailed(true);
                return;
            };
        } else {
            setImportFailed(true);
            return;
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
            <DialogContent>

                <DialogContentText>
                    Importieren Sie einen bestehendes Formular im GOV-Format (*.gov).
                </DialogContentText>
                <Box
                    sx={{
                        mt: 2,
                    }}
                >
                    <FileUpload
                        onChange={onFileSelect}
                        value={[]}
                        extensions={['gov']}
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
                sx={{
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start',
                }}
            >
                <Button
                    onClick={handleClose}
                    sx={{
                        ml: 'auto!important',
                    }}
                    size="large"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
