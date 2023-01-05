import {TextField} from '@mui/material';
import {SpacerElement} from '../../models/elements/form-elements/content-elements/spacer-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function SpacerComponentEditor(props: BaseEditorProps<SpacerElement>) {
    return (
        <>
            <TextField
                value={props.component.height ?? ''}
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
