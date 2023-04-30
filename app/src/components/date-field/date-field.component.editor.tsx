import {Checkbox, FormControl, FormControlLabel, InputLabel, MenuItem, Select} from '@mui/material';
import {DateFieldComponentModelMode, DateFieldElement} from '../../models/elements/form/input/date-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

const modes: [DateFieldComponentModelMode, string][] = [
    [DateFieldComponentModelMode.Date, 'TT.MM.JJJJ'],
    [DateFieldComponentModelMode.Month, 'MM.JJJJ'],
    [DateFieldComponentModelMode.Year, 'JJJJ'],
];

export function DateFieldComponentEditor(props: BaseEditorProps<DateFieldElement>) {
    return (
        <>
            <FormControl
                margin="normal"
            >
                <InputLabel>
                    Datums-Format
                </InputLabel>
                <Select
                    value={props.component.mode ?? DateFieldComponentModelMode.Date}
                    label="Datums-Format"
                    onChange={event => props.onPatch({
                        mode: event.target.value as DateFieldComponentModelMode,
                    })}
                >
                    {
                        modes.map(([type, label]) => (
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

            {
                (props.component.mode == null || props.component.mode === DateFieldComponentModelMode.Date) &&
                <FormControl>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={props.component.mustBePast ?? false}
                                onChange={event => props.onPatch({
                                    mustBeFuture: false,
                                    mustBePast: event.target.checked,
                                })}
                            />
                        }
                        label="Das Datum muss in der Vergangenheit liegen"
                    />
                </FormControl>
            }

            {
                (props.component.mode == null || props.component.mode === DateFieldComponentModelMode.Date) &&
                <FormControl>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={props.component.mustBeFuture ?? false}
                                onChange={event => props.onPatch({
                                    mustBeFuture: event.target.checked,
                                    mustBePast: false,
                                })}
                            />
                        }
                        label="Das Datum muss in der Zukunft liegen"
                    />
                </FormControl>
            }
        </>
    );
}
