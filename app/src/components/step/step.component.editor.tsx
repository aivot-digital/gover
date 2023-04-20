import {StepElement} from '../../models/elements/./steps/step-element';
import {Box, FormControl, InputLabel, ListItemIcon, ListItemText, MenuItem, Select, TextField} from '@mui/material';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {StepIcons} from '../../data/step-icons';

export function StepComponentEditor(props: BaseEditorProps<StepElement>) {
    return (
        <>
            <TextField
                value={props.component.title ?? ''}
                label="Name des Abschnitts"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    title: event.target.value,
                })}
            />

            <FormControl
                margin="normal"
            >
                <InputLabel id="icon-select-label">
                    Icon
                </InputLabel>
                <Select
                    labelId="icon-select-label"
                    label="Icon"
                    value={props.component.icon ?? null}
                    onChange={event => props.onPatch({
                        icon: event.target.value ?? '',
                    })}
                >
                    {
                        StepIcons.map(stepIcon => (
                            <MenuItem value={stepIcon.id}>
                                <Box sx={{display: 'flex', alignItems: 'center'}}>
                                    <ListItemIcon>
                                        <FontAwesomeIcon icon={stepIcon.def}/>
                                    </ListItemIcon>
                                    <ListItemText>
                                        {stepIcon.label}
                                    </ListItemText>
                                </Box>
                            </MenuItem>
                        ))
                    }

                </Select>
            </FormControl>
        </>
    );
}
