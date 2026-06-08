import React from 'react';
import {BaseEditorProps} from './base-editor';
import {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function ProcessAttachmentDisplayEditor(props: BaseEditorProps<ProcessAttachmentDisplayElement>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Dateiname des Anhangs"
                    value={element.fileName}
                    onChange={(fileName) => {
                        onPatch({
                            fileName: fileName,
                        });
                    }}
                    hint="Alle Vorgangsanhänge mit exakt diesem Dateinamen werden angezeigt."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
