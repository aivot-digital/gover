import React from 'react';
import {Divider, ListItemIcon, ListItemText, Menu} from '@mui/material';
import MoveItem from '@aivot/mui-material-symbols-400-n25-outlined/MoveItem';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {ProcessEntity} from '../entities/process-entity';
import {DisabledTooltipMenuItem} from '../../../components/disabled-tooltip/disabled-tooltip-menu-item';
import {Permission} from '../../../data/permissions/permission';
import {useHasAnyDepartmentPermission, useHasDepartmentPermission, useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';

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

    // Delegated process grants do not count, otherwise a delegate could take over the process and its access rules.
    const canUpdateInManagingDepartment = useHasDepartmentPermission(process.departmentId, Permission.PROCESS_DEFINITION_UPDATE);
    // A move also needs permission to create processes in the target department.
    const canCreateInDepartment = useHasAnyDepartmentPermission(Permission.PROCESS_DEFINITION_CREATE);
    const canDelete = useHasProcessPermission(process.id, Permission.PROCESS_DEFINITION_DELETE);

    return (
        <Menu
            anchorEl={anchorEl}
            open={anchorEl != null}
            onClose={onClose}
        >
            <DisabledTooltipMenuItem
                disabled={!canUpdateInManagingDepartment || !canCreateInDepartment}
                disabledTooltip={
                    canUpdateInManagingDepartment
                        ? formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_CREATE)
                        : 'Nur die verwaltende Organisationseinheit kann diesen Prozess übertragen.'
                }
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
            </DisabledTooltipMenuItem>

            <Divider/>

            <DisabledTooltipMenuItem
                disabled={!canDelete}
                disabledTooltip={formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_DELETE)}
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
            </DisabledTooltipMenuItem>
        </Menu>
    );
}
