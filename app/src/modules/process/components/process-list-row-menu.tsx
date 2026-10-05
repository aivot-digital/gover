import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import MoveItem from '@aivot/mui-material-symbols-400-n25-outlined/MoveItem';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {ProcessEntity} from '../entities/process-entity';

interface ProcessListRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    process: ProcessEntity;
    onMoveProcessToDepartment: (process: ProcessEntity) => void;
    onDeleteProcess: (process: ProcessEntity) => void;
}

export function ProcessListRowMenu(props: ProcessListRowMenuProps) {
    const {
        anchorEl,
        onClose,
        process,
        onMoveProcessToDepartment,
        onDeleteProcess,
    } = props;

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            <MenuItem
                onClick={() => {
                    onMoveProcessToDepartment(process);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <MoveItem/>
                </ListItemIcon>
                <ListItemText>
                    Prozess an Organisationseinheit übertragen
                </ListItemText>
            </MenuItem>

            <Divider/>

            <MenuItem
                onClick={() => {
                    onClose();
                    onDeleteProcess(process);
                }}
            >
                <ListItemIcon sx={{color: 'error.main'}}>
                    <Delete/>
                </ListItemIcon>
                <ListItemText
                    slotProps={{
                        primary: {
                            color: 'error.main',
                        }
                    }}
                >
                    Prozess löschen
                </ListItemText>
            </MenuItem>
        </Menu>
    );
}
