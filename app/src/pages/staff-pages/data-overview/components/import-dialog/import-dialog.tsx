import React, {useState} from 'react';
import {ImportDialogProps} from './import-dialog-props';
import {
    DialogTitleWithClose
} from '../../../../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {FileUpload} from '../../../../../components/file-upload/file-upload';
import strings from './import-dialog-strings.json';
import {Localization} from '../../../../../locale/localization';

const _ = Localization(strings);

export function ImportDialog({onImport, extension, onClose, ...dialogProps}: ImportDialogProps) {
    const [filesToImport, setFilesToImport] = useState<File[]>([]);

    const handleImport = () => {
        onImport(filesToImport);
        handleClose();
    };

    const handleClose = () => {
        setFilesToImport([]);
        onClose();
    };

    return (
        <Dialog
            {...dialogProps}
            onClose={handleClose}
        >
            <DialogTitleWithClose
                id="import-dialog-title"
                onClose={handleClose}
                closeTooltip={_.cancelLabel}
            >
                {_.importTitle}
            </DialogTitleWithClose>
            <DialogContent>
                <Typography
                    variant="body1"
                    sx={{mb: 2}}
                >
                    {_.importHint}
                </Typography>
                <FileUpload
                    onChange={setFilesToImport}
                    value={filesToImport}
                    extensions={[extension]}
                    multiple
                />
            </DialogContent>
            <DialogActions
                sx={{
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start',
                }}
            >
                <Button
                    onClick={handleImport}
                    size="large"
                    variant="outlined"
                    disabled={filesToImport.length === 0}
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
