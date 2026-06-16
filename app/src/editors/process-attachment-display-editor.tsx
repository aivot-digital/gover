import React from 'react';
import type {BaseEditorProps} from './base-editor';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function ProcessAttachmentDisplayEditor(props: BaseEditorProps<ProcessAttachmentDisplayElement>): React.JSX.Element {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
        <React.Fragment>
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
                                fileName,
                            });
                        }}
                        hint="Alle Vorgangsanhänge mit exakt diesem Dateinamen werden angezeigt."
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Hinweis"
                        value={element.hint}
                        onChange={(hint) => {
                            onPatch({
                                hint,
                            });
                        }}
                        hint="Geben Sie hier einen zusätzlichen Hinweis an (optional, wird unter dem Element angezeigt)."
                        disabled={!editable}
                    />
                </Grid>
            </Grid>
        </React.Fragment>
    );
}
