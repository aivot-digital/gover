import {TextField} from '@mui/material';
import {SpacerElement} from '../../models/elements/form/content/spacer-element';
import {BaseEditorProps} from "../../editors/base-editor";

export function SpacerComponentEditor(props: BaseEditorProps<SpacerElement>) {
    return (
        <>
            <TextField
                value={props.element.height ?? ''}
                label="Abstand"
                helperText="Die Angabe erfolgt in Pixeln (px)."
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    height: event.target.value,
                })}
            />
        </>
    );
}
