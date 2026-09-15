import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import SwapVert from '@aivot/mui-material-symbols-400-n25-outlined/SwapVert';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Inventory2 from '@aivot/mui-material-symbols-400-n25-outlined/Inventory2';
import Route from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus} from '../enums/process-status';

interface ProcessVersionsDialogRowMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    process: ProcessEntity;
    processVersion: ProcessVersionEntity;
    lifecycleActionsDisabled?: boolean;
    lifecycleActionsDisabledReason?: string;

    onPublishVersion: (version: number) => void;
    onRevokeVersion: (version: number) => void;
    onReuseVersionAsDraft: (version: number) => void;
    onReuseVersionAsNewProcess: (version: number) => void;
    onExportVersion: (version: number) => void;
    onDeleteVersion: (version: number) => void;
}

export function ProcessVersionsDialogRowMenu(props: ProcessVersionsDialogRowMenuProps) {
    const {
        anchorEl,
        onClose,
        process,
        processVersion,
        lifecycleActionsDisabled,
        lifecycleActionsDisabledReason,

        onPublishVersion,
        onRevokeVersion,
        onReuseVersionAsDraft,
        onReuseVersionAsNewProcess,
        onExportVersion,
        onDeleteVersion,
    } = props;

    const canPublishVersion = processVersion.status === ProcessStatus.Drafted || processVersion.status === ProcessStatus.Revoked;
    const canRevokeVersion = processVersion.status === ProcessStatus.Published;
    const hasLifecycleAction = canPublishVersion || canRevokeVersion;

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            {
                canPublishVersion &&
                <MenuItem
                    disabled={lifecycleActionsDisabled}
                    title={lifecycleActionsDisabled ? lifecycleActionsDisabledReason : undefined}
                    onClick={() => {
                        onPublishVersion(processVersion.processVersion);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Route/>
                    </ListItemIcon>
                    <ListItemText>
                        Veröffentlichen
                    </ListItemText>
                </MenuItem>
            }

            {
                canRevokeVersion &&
                <MenuItem
                    disabled={lifecycleActionsDisabled}
                    title={lifecycleActionsDisabled ? lifecycleActionsDisabledReason : undefined}
                    onClick={() => {
                        onRevokeVersion(processVersion.processVersion);
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <Inventory2/>
                    </ListItemIcon>
                    <ListItemText>
                        Zurückziehen
                    </ListItemText>
                </MenuItem>
            }

            {
                hasLifecycleAction &&
                <Divider/>
            }

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
