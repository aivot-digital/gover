import {Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogContentText} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Application} from '../../models/application';
import {Localization} from '../../locale/localization';
import strings from './import-application-dialog-strings.json';
import {ImportApplicationDialogProps} from './import-application-dialog-props';
import {FileUpload} from '../../components/file-upload/file-upload';
import {ApplicationInitForm, validateApplication} from '../../components/application-init-form/application-init-form';
import {ApplicationInitFormPropsErrors} from '../../components/application-init-form/application-init-form-props';

const _ = Localization(strings);

export function ImportApplicationDialog(props: ImportApplicationDialogProps) {
    const {applications, onHide, onImport, ...passTroughProps} = props;
    const [applicationToImport, setApplicationToImport] = useState<Application>();
    const [applicationToImportErrors, setApplicationToImportErrors] = useState<ApplicationInitFormPropsErrors>({});
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
                        const parsedModel = JSON.parse(value.toString());
                        setApplicationToImport(parsedModel);
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
        setApplicationToImport(undefined);
        setImportFailed(false);
        props.onHide();
    };

    const handleImport = (navigateToEditAfterwards: boolean) => {
        if (applicationToImport != null) {
            setApplicationToImportErrors({});

            const errors = validateApplication(applicationToImport, applications);
            if (Object.keys(errors).length > 0) {
                setApplicationToImportErrors(errors);
                return;
            }

            props.onImport(applicationToImport, navigateToEditAfterwards);
            handleClose();
        }
    };

    return (
        <Dialog
            {...passTroughProps}
            onClose={handleClose}
            fullWidth={true}
        >
            <DialogTitleWithClose
                id="import-dialog-title"
                onClose={handleClose}
                closeTooltip={_.closeTooltip}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent>

                {
                    applicationToImport &&
                    <ApplicationInitForm
                        application={applicationToImport}
                        onChange={setApplicationToImport}
                        errors={applicationToImportErrors}
                    />
                }
                {
                    !applicationToImport &&
                    <>
                        <DialogContentText>
                            {_.importHelper}
                        </DialogContentText>
                        <Box sx={{
                            mt: 2,
                        }}>
                            <FileUpload
                                onChange={onFileSelect}
                                value={[]}
                                extensions={['gov']}
                            />
                        </Box>
                    </>
                }
                {
                    importFailed &&
                    <Alert
                        sx={{
                            mt: 4,
                        }}
                        severity="error"
                    >
                        {_.importError}
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
                    onClick={() => handleImport(true)}
                    disabled={applicationToImport == null}
                    size="large"
                    variant="outlined"
                >
                    {_.importEditLabel}
                </Button>
                <Button
                    onClick={() => handleImport(false)}
                    disabled={applicationToImport == null}
                    size="large"
                >
                    {_.importLabel}
                </Button>
                <Button
                    onClick={handleClose}
                    sx={{
                        ml: 'auto!important',
                    }}
                    size="large"
                >
                    {_.cancelLabel}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
