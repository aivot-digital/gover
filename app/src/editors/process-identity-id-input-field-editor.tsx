import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type ProcessIdentityIdInputElement} from '../models/elements/form/input/process-identity-id-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function ProcessIdentityIdInputFieldEditor(props: BaseEditorProps<ProcessIdentityIdInputElement>) {
    const {
        element,
        editable,
        onPatch,
        hasSummaryLayoutParent,
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
                    hint="Ein Platzhalter zeigt beispielhaft, welche Prozessidentität ausgewählt werden kann."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
