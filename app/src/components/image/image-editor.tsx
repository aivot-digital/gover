import {TextField} from '@mui/material';
import {ImageElement} from '../../models/elements/form/content/image-element';
import {BaseEditorProps} from "../../editors/base-editor";

export function ImageEditor(props: BaseEditorProps<ImageElement>) {
    return (
        <>
            <TextField
                value={props.element.src ?? ''}
                label="Url"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    src: event.target.value,
                })}
            />


            <TextField
                value={props.element.alt ?? ''}
                label="Alt-Text"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    alt: event.target.value,
                })}
            />
        </>
    );
}
