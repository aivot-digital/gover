import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type ProcessInstanceAttachmentSetSelectElement} from '../models/elements/form/input/process-instance-attachment-set-select-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';

export function ProcessInstanceAttachmentSetSelectFieldEditor(props: BaseEditorProps<ProcessInstanceAttachmentSetSelectElement>) {
    const {
        element,
        editable,
        onPatch,
        hasSummaryLayoutParent,
        configurationErrors,
    } = props;

    if (hasSummaryLayoutParent) {
        return null;
    }

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({
                            placeholder: value,
                        });
                    }}
                    hint="Ein Platzhalter zeigt beispielhaft, welcher Anlagensatz ausgewählt werden kann."
                    disabled={!editable}
                />
            </Grid>

            {
                element.required === true &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 3,
                    }}
                >
                    <NumberFieldComponent
                        label="Minimale Anzahl"
                        value={element.minItems ?? undefined}
                        onChange={(value) => {
                            onPatch({
                                minItems: value,
                            });
                        }}
                        hint="Geben Sie 0 oder nichts ein, um keine Mindestanzahl zu fordern."
                        error={configurationErrors?.minItems?.join(' ')}
                        disabled={!editable}
                    />
                </Grid>
            }

            <Grid
                size={{
                    xs: 12,
                    lg: element.required === true ? 3 : 6,
                }}
            >
                <NumberFieldComponent
                    label="Maximale Anzahl"
                    value={element.maxItems ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            maxItems: value,
                        });
                    }}
                    hint="Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern. Bei 1 wird eine einfache Auswahl angezeigt."
                    error={configurationErrors?.maxItems?.join(' ')}
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
