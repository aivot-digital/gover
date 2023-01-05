import {TextField} from '@mui/material';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {ImageElement} from '../../models/elements/form-elements/content-elements/image-element';

export function ImageEditor(props: BaseEditorProps<ImageElement>) {
    return (
        <>
            <TextField
                value={props.component.src ?? ''}
                label="Url"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    src: event.target.value,
                })}
            />


            <TextField
                value={props.component.alt ?? ''}
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
