import React, {useRef} from 'react';
import {Button, TextField, Typography} from '@mui/material';
import {RootElement} from '../../../models/elements/root-element';
import {BaseEditorProps} from '../../_lib/base-editor-props';
import {CodeService} from '../../../services/code.service';
import {selectLoadedApplication} from '../../../slices/app-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';

export function RootComponentEditorTabCode(_: BaseEditorProps<RootElement>) {
    const codeUploadRef = useRef<HTMLInputElement>();

    const application = useAppSelector(selectLoadedApplication);

    const upload = async () => {
        const files = codeUploadRef.current?.files ?? [];
        if (application != null && files.length > 0) {
            await CodeService.uploadCode(application.id, files[0]);
            await CodeService.loadCode(application.id);
            alert('Code erfolgreich hochgeladen!');
        }
    };

    return (
        <>
            <Typography
                variant="subtitle1"
                sx={{mb: 2}}
            >
                Code Hochladen
            </Typography>

            <TextField
                type="file"
                InputProps={{
                    // @ts-ignore
                    accept: '*.js'
                }}
                inputRef={codeUploadRef}
            />
            <Button
                onClick={upload}
            >
                Hochladen
            </Button>
        </>
    );
}
