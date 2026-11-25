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
import {DisabledTooltipMenuItem} from '../../../components/disabled-tooltip/disabled-tooltip-menu-item';
import {Permission} from '../../../data/permissions/permission';
import {useHasAnyDepartmentPermission, useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';

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

    const hasUpdatePermission = useHasProcessPermission(process.id, Permission.PROCESS_DEFINITION_UPDATE);
    const hasPublishPermission = useHasProcessPermission(process.id, Permission.PROCESS_DEFINITION_PUBLISH_LOCAL);
    const hasCreatePermission = useHasAnyDepartmentPermission(Permission.PROCESS_DEFINITION_CREATE);

    const lifecycleDisabled = lifecycleActionsDisabled === true || !hasPublishPermission;
    const lifecycleDisabledTooltip = hasPublishPermission
        ? lifecycleActionsDisabledReason
        : formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_PUBLISH_LOCAL);
    const missingUpdateTooltip = formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_UPDATE);

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            {
                canPublishVersion &&
                <DisabledTooltipMenuItem
                    disabled={lifecycleDisabled}
                    disabledTooltip={lifecycleDisabledTooltip}
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
                </DisabledTooltipMenuItem>
            }

            {
                canRevokeVersion &&
                <DisabledTooltipMenuItem
                    disabled={lifecycleDisabled}
                    disabledTooltip={lifecycleDisabledTooltip}
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
                </DisabledTooltipMenuItem>
            }

            {
                hasLifecycleAction &&
                <Divider/>
            }

            {
                process.draftedVersion !== processVersion.processVersion &&
                <DisabledTooltipMenuItem
                    disabled={!hasUpdatePermission}
                    disabledTooltip={missingUpdateTooltip}
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
                </DisabledTooltipMenuItem>
            }

            <DisabledTooltipMenuItem
                disabled={!hasCreatePermission}
                disabledTooltip={formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_CREATE)}
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
            </DisabledTooltipMenuItem>

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
                <DisabledTooltipMenuItem
                    disabled={!hasUpdatePermission}
                    disabledTooltip={missingUpdateTooltip}
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
                </DisabledTooltipMenuItem>
            }
        </Menu>
    );
}
