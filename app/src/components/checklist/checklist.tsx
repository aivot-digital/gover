import {type ChecklistProps} from './checklist-props';
import {Checkbox, Divider, List, ListItem, ListItemIcon, ListItemText} from '@mui/material';
import React from 'react';
import {RadioButtonUncheckedOutlined, TaskAltOutlined} from '@mui/icons-material';

export function Checklist(props: ChecklistProps) {
    // @ts-ignore
    return (
        <List sx={props.sx} dense>
            {
                props.items.map((item, i) => (
                    <React.Fragment key={item.label}>
                        <ListItem>
                            <ListItemIcon sx={{minWidth: 44}}>
                                {
                                    item.done ? <TaskAltOutlined
                                        sx={{
                                            color: 'primary.main',
                                        }}
                                    /> : <RadioButtonUncheckedOutlined
                                        sx={{
                                            color: 'text.secondary',
                                            opacity: 0.5,
                                        }}
                                    />
                                }

                            </ListItemIcon>
                            <ListItemText
                                primary={item.label}
                                sx={{maxWidth: 920}}
                            />
                        </ListItem>
                        {i < props.items.length - 1 && (
                            <Divider variant="inset" component="li" sx={{ml: 7.5}} />
                        )}
                    </React.Fragment>
                ))
            }
        </List>
    );
}
