import {type ChecklistProps} from './checklist-props';
import {Checkbox, List, ListItem, ListItemText} from '@mui/material';
import React from 'react';

export function Checklist(props: ChecklistProps): JSX.Element {
    return (
        <List sx={props.sx}>
            {
                props.items.map((item) => (
                    <ListItem
                        key={item.label}
                        secondaryAction={
                            <Checkbox
                                edge="start"
                                checked={item.done}
                            />
                        }
                    >
                        <ListItemText
                            primary={item.label}
                        />
                    </ListItem>
                ))
            }
        </List>
    );
}
