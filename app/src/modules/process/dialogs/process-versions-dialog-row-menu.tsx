import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import SwapVert from '@aivot/mui-material-symbols-400-n25-outlined/SwapVert';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessVersionEntity} from '../entities/process-version-entity';

interface FormVersionsDialogRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    process: ProcessEntity;
    processVersion: ProcessVersionEntity;

    onReuseVersionAsDraft: (version: number) => void;
    onReuseVersionAsNewProcess: (version: number) => void;
    onExportVersion: (version: number) => void;
    onDeleteVersion: (version: number) => void;
}

export function ProcessVersionsDialogRowMenu(props: FormVersionsDialogRowMenuProps) {
    const {
        anchorEl,
        onClose,
        process,
        processVersion,

        onReuseVersionAsDraft,
        onReuseVersionAsNewProcess,
        onExportVersion,
        onDeleteVersion,
    } = props;

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            {
                process.draftedVersion !== processVersion.processVersion &&
                <MenuItem
                    onClick={() => {
                        onReuseVersionAsDraft(processVersion.processVersion);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Edit/>
                    </ListItemIcon>
                    <ListItemText>
                        Als Entwurf verwenden
                    </ListItemText>
                </MenuItem>
            }

            <MenuItem
                onClick={() => {
                    onReuseVersionAsNewProcess(processVersion.processVersion);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <ContentCopy/>
                </ListItemIcon>
                <ListItemText>
                    Als neuen Prozess kopieren
                </ListItemText>
            </MenuItem>

            <MenuItem
                onClick={() => {
                    onExportVersion(processVersion.processVersion);
                    onClose();
                }}
            >
                <ListItemIcon>
                    <SwapVert/>
                </ListItemIcon>
                <ListItemText>
                    Als Prozess exportieren
                </ListItemText>
            </MenuItem>

            {
                process.draftedVersion === processVersion.processVersion &&
                <Divider/>
            }

            {
                process.draftedVersion === processVersion.processVersion &&
                <MenuItem
                    onClick={() => {
                        onDeleteVersion(processVersion.processVersion);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Delete color="error"/>
                    </ListItemIcon>
                    <ListItemText
                        sx={{
                            color: 'error.main',
                        }}
                    >
                        Entwurf löschen
                    </ListItemText>
                </MenuItem>
            }
        </Menu>
    );
}
