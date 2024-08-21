import React from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {Box, FormControl, InputLabel, ListItemIcon, ListItemText, MenuItem, Select} from '@mui/material';
import {StepIcons} from '../../data/step-icons';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type Form as Application} from '../../models/entities/form';

export function StepComponentEditor(props: BaseEditorProps<StepElement, Application>): JSX.Element {
    return (
        <>
            <TextFieldComponent
                value={props.element.title ?? ''}
                label="Titel des Abschnitts"
                onChange={(val) => {
                    props.onPatch({
                        title: val,
                    });
                }}
                disabled={!props.editable}
            />

            <FormControl
                margin="normal"
            >
                <InputLabel
                    id="icon-select-label"
                    disabled={!props.editable}
                >
                    Icon
                </InputLabel>
                <Select
                    labelId="icon-select-label"
                    label="Icon"
                    value={props.element.icon ?? ''}
                    onChange={(event) => {
                        props.onPatch({
                            icon: event.target.value ?? '',
                        });
                    }}
                    disabled={!props.editable}
                >
                    {
                        StepIcons.map((stepIcon) => {
                            const Icon = stepIcon.def;
                            return (
                                <MenuItem
                                    key={stepIcon.id}
                                    value={stepIcon.id}
                                >
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                        }}
                                    >
                                        <ListItemIcon>
                                            <Icon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            {stepIcon.label}
                                        </ListItemText>
                                    </Box>
                                </MenuItem>
                            );
                        })
                    }

                </Select>
            </FormControl>
        </>
    );
}
