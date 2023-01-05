import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {HeadlineElement} from '../../models/elements/form-elements/content-elements/headline-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function HeadlineComponentEditor(props: BaseEditorProps<HeadlineElement>) {
    return (
        <>
            <TextField
                value={props.component.content ?? ''}
                label="Überschrift"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    content: event.target.value,
                })}
            />

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.small ?? false}
                            onChange={event => props.onPatch({
                                small: event.target.checked,
                            })}
                        />
                    }
                    label="Kompakte Überschrift"
                />
            </FormControl>
        </>
    );
}
