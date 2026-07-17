import React from 'react';
import {ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import MoveItem from '@aivot/mui-material-symbols-400-n25-outlined/MoveItem';
import {ProcessEntity} from '../entities/process-entity';

interface ProcessListRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    process: ProcessEntity;
    onMoveProcessToDepartment: (process: ProcessEntity) => void;
}

export function ProcessListRowMenu(props: ProcessListRowMenuProps) {
    const {
        anchorEl,
        onClose,
        process,
        onMoveProcessToDepartment,
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
        </Menu>
    );
}
