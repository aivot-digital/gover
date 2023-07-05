import {StepElement} from '../../models/elements/steps/step-element';
import {Box, FormControl, InputLabel, ListItemIcon, ListItemText, MenuItem, Select, TextField} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {StepIcons} from '../../data/step-icons';
import {BaseEditorProps} from "../../editors/base-editor";

export function StepComponentEditor(props: BaseEditorProps<StepElement>) {
    return (
        <>
            <TextField
                value={props.element.title ?? ''}
                label="Titel des Abschnitts"
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
                    value={props.element.icon ?? ''}
                    onChange={event => props.onPatch({
                        icon: event.target.value ?? '',
                    })}
                >
                    {
                        StepIcons.map(stepIcon => (
                            <MenuItem
                                key={stepIcon.id}
                                value={stepIcon.id}
                            >
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
