import React from 'react';
import type {BaseEditorProps} from './base-editor';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {ProcessInstanceAttachmentSetSelect} from '../components/process-instance-attachment-set-select/process-instance-attachment-set-select';

export function ProcessAttachmentDisplayEditor(props: BaseEditorProps<ProcessAttachmentDisplayElement>): React.JSX.Element {
    const {
        element,
        onPatch,
        editable,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

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
                    <ProcessInstanceAttachmentSetSelect
                        attachmentSets={opec?.incomingMetadata?.forwardedAttachmentSets}
                        label="Schlüssel des Anlagensatzes"
                        value={element.attachmentSetKey == null ? null : [element.attachmentSetKey]}
                        onChange={(attachmentSetKeys) => {
                            onPatch({
                                attachmentSetKey: attachmentSetKeys?.[0] ?? null,
                            });
                        }}
                        hint="Alle Vorgangsanhänge aus Anlagensätzen mit diesem Schlüssel werden angezeigt."
                        disabled={!editable}
                        maxItems={1}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Beschriftung"
                        value={element.label}
                        onChange={(label) => {
                            onPatch({
                                label,
                            });
                        }}
                        hint="Optional. Wenn leer, wird die Standardbeschriftung angezeigt."
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={12}
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
