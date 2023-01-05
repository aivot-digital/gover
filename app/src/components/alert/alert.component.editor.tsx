import {AlertColor, FormControl, InputLabel, MenuItem, Select, TextField} from '@mui/material';
import {AlertElement} from '../../models/elements/form-elements/content-elements/alert-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

const colors = [
    ['success', 'Erfolg (Grün)'],
    ['info', 'Information (Blau)'],
    ['warning', 'Warnung (Orange)'],
    ['error', 'Fehler (Rot)'],
];

export function AlertComponentEditor(props: BaseEditorProps<AlertElement>) {
    return (
        <>
            <TextField
                value={props.component.title ?? ''}
                label="Titel"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    title: event.target.value,
                })}
            />

            <TextField
                value={props.component.text ?? ''}
                label="Hinweis"
                margin="normal"
                rows={4}
                multiline
                onChange={event => props.onPatch({
                    text: event.target.value,
                })}
            />

            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Hinweistyp</InputLabel>
                <Select
                    value={props.component.alertType ?? 'info'}
                    label="Hinweistyp"
                    onChange={event => props.onPatch({
                        alertType: event.target.value as AlertColor,
                    })}
                >
                    {
                        colors.map(([type, label]) => (
                            <MenuItem
                                key={type}
                                value={type}
                            >
                                {label}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>
        </>
    );
}
