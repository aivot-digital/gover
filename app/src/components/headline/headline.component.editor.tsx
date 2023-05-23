import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {HeadlineElement} from '../../models/elements/form/content/headline-element';
import {BaseEditorProps} from "../../editors/base-editor";

export function HeadlineComponentEditor(props: BaseEditorProps<HeadlineElement>) {
    return (
        <>
            <TextField
                value={props.element.content ?? ''}
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
                            checked={props.element.small ?? false}
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
