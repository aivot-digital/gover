import {Grid} from '@mui/material';
import {type BaseEditorProps} from './base-editor';
import {type SecretSelectInputElement} from '../models/elements/form/input/secret-select-input-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function SecretSelectInputFieldEditor(props: BaseEditorProps<SecretSelectInputElement>) {
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
        <Grid container columnSpacing={4} rowSpacing={2}>
            <Grid size={{xs: 12, lg: 6}}>
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({placeholder: value});
                    }}
                    hint="Der Platzhalter wird angezeigt, solange kein Geheimnis ausgewählt ist."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
