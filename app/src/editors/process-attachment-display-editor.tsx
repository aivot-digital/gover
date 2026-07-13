import React, {useMemo} from 'react';
import type {BaseEditorProps} from './base-editor';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {Grid} from '@mui/material';
import {AutocompleteTextField, TextFieldComponent} from '../components/text-field/text-field-component';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';

export function ProcessAttachmentDisplayEditor(props: BaseEditorProps<ProcessAttachmentDisplayElement>): React.JSX.Element {
    const {
        element,
        onPatch,
        editable,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

    const attachmentSetSuggestions = useMemo(() => {
        const result = new Map<string, { id: string; label: string; subLabel: string }>();

        for (const attachmentSet of opec?.incomingMetadata?.forwardedAttachmentSets ?? []) {
            if (result.has(attachmentSet.dataKey)) {
                continue;
            }

            result.set(attachmentSet.dataKey, {
                id: attachmentSet.dataKey,
                label: attachmentSet.label.trim().length > 0 ? attachmentSet.label : attachmentSet.dataKey,
                subLabel: [
                    attachmentSet.dataKey,
                    attachmentSet.subLabel,
                    attachmentSet.origin.name,
                ]
                    .filter((part): part is string => part != null && part.trim().length > 0)
                    .join(' - '),
            });
        }

        return Array.from(result.values());
    }, [opec?.incomingMetadata?.forwardedAttachmentSets]);

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
                    <AutocompleteTextField
                        label="Schlüssel des Anlagensatzes"
                        value={element.attachmentSetKey}
                        onChange={(attachmentSetKey) => {
                            onPatch({
                                attachmentSetKey,
                            });
                        }}
                        hint="Alle Vorgangsanhänge aus Anlagensätzen mit diesem Schlüssel werden angezeigt."
                        suggestions={attachmentSetSuggestions}
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
