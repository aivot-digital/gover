import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Button,
    Checkbox,
    Chip,
    Divider,
    FormControlLabel,
    Paper,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import React from 'react';
import ExpandMoreIcon from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {CopyToClipboardButton} from '../../../components/copy-to-clipboard-button/copy-to-clipboard-button';
import {inferCrud, type PermissionGroup} from './permission-editor-utils';

interface PermissionGroupAccordionProps {
    group: PermissionGroup;
    groupId: string;
    selectedPermissionsSet: ReadonlySet<string>;
    isExpanded: boolean;
    isBusy: boolean;
    isEditable: boolean;
    onExpandedChange: (groupId: string, next: boolean) => void;
    onToggleGroup: (group: PermissionGroup, checked: boolean) => void;
    onTogglePermission: (permission: string, checked: boolean) => void;
}

export const PermissionGroupAccordion = React.memo(function PermissionGroupAccordion(props: PermissionGroupAccordionProps): React.ReactElement {
    const {
        group,
        groupId,
        selectedPermissionsSet,
        isExpanded,
        isBusy,
        isEditable,
        onExpandedChange,
        onToggleGroup,
        onTogglePermission,
    } = props;

    const total = group.permissions.length;
    const selectedInGroup = group.permissions.reduce(
        (count, permission) => count + (selectedPermissionsSet.has(permission.permission) ? 1 : 0),
        0,
    );
    const allSelected = total > 0 && selectedInGroup === total;

    return (
        <Accordion
            expanded={isExpanded}
            onChange={(_, next) => onExpandedChange(groupId, next)}
            disableGutters
            sx={{
                borderRadius: 2,
                '&:before': {
                    display: 'none',
                },
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
            slotProps={{
                transition: {
                    unmountOnExit: true,
                },
            }}
        >
            <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                <Stack
                    direction="row"
                    spacing={1.5}
                    sx={{
                        alignItems: "center",
                        width: '100%',
                        pr: 1
                    }}>
                    <Typography
                        sx={{
                            flex: 1,
                            minWidth: 0,
                        }}
                        noWrap
                    >
                        {group.contextLabel}
                    </Typography>

                    {group.availabilityWarningLabel != null && (
                        <Chip
                            size="small"
                            color="warning"
                            variant="outlined"
                            label={group.availabilityWarningLabel}
                        />
                    )}

                    <Chip
                        size="small"
                        variant={selectedInGroup > 0 ? 'filled' : 'outlined'}
                        label={`${selectedInGroup} von ${total}`}
                    />

                    <Divider
                        orientation="vertical"
                        flexItem
                    />

                    <Tooltip
                        title={allSelected ? 'Gruppe abwählen' : 'Gruppe auswählen'}
                        arrow
                    >
                        <span>
                            <Button
                                component="span"
                                size="small"
                                variant="text"
                                disabled={isBusy || !isEditable || total === 0}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onToggleGroup(group, !allSelected);
                                }}
                                sx={{mr: 1}}
                            >
                                {allSelected ? 'Abwählen' : 'Auswählen'}
                            </Button>
                        </span>
                    </Tooltip>
                </Stack>
            </AccordionSummary>

            <AccordionDetails>
                {group.assignmentHint != null && (
                    <Typography
                        variant="body2"
                        sx={{
                            color: "text.secondary",
                            mb: 2
                        }}>
                        {group.assignmentHint}
                    </Typography>
                )}

                <Box
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: {
                            xs: '1fr',
                            md: '1fr 1fr',
                        },
                        columnGap: 2,
                        rowGap: 2,
                    }}
                >
                    {group.permissions.map(({permission, label, description}) => {
                        const checked = selectedPermissionsSet.has(permission);
                        const crud = inferCrud(permission);

                        return (
                            <Paper
                                key={permission}
                                variant="outlined"
                                sx={{
                                    px: 1.5,
                                    py: 1,
                                    borderRadius: 2,
                                    borderColor: group.availabilityWarningLabel != null ? 'warning.main' : undefined,
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: 1,
                                    '&:hover': {
                                        backgroundColor: 'action.hover',
                                    },
                                }}
                            >
                                <FormControlLabel
                                    sx={{
                                        m: 0,
                                        flex: 1,
                                        alignItems: 'flex-start',
                                    }}
                                    control={(
                                        <Checkbox
                                            checked={checked}
                                            onChange={(_, next) => onTogglePermission(permission, next)}
                                            disabled={isBusy || !isEditable}
                                            size="small"
                                            sx={{
                                                p: 0,
                                                pt: 0.25,
                                                pr: 1,
                                                m: 0,
                                            }}
                                        />
                                    )}
                                    label={(
                                        <Box
                                            sx={{
                                                width: '100%',
                                            }}
                                        >
                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                sx={{
                                                    alignItems: "center",
                                                    minWidth: 0
                                                }}>
                                                <Typography
                                                    variant="body2"
                                                    sx={{
                                                        flex: 1,
                                                        minWidth: 0,
                                                    }}
                                                    noWrap
                                                >
                                                    {label}
                                                </Typography>
                                            </Stack>

                                            <Typography
                                                variant="caption"
                                                sx={{
                                                    color: "text.secondary",
                                                    display: 'block',
                                                    fontFamily: 'monospace',
                                                    wordBreak: 'break-word'
                                                }}>
                                                {permission}
                                            </Typography>

                                            {description && (
                                                <Typography
                                                    variant="caption"
                                                    sx={{
                                                        color: "text.secondary",
                                                        display: 'block',
                                                        mt: 0.25,
                                                        wordBreak: 'break-word'
                                                    }}>
                                                    {description}
                                                </Typography>
                                            )}
                                        </Box>
                                    )}
                                />

                                <Stack
                                    direction="row"
                                    spacing={1}
                                    sx={{
                                        alignItems: "center",
                                        pt: 0.25
                                    }}>
                                    {crud && (
                                        <Chip
                                            size="small"
                                            label={crud.toUpperCase()}
                                            variant="outlined"
                                        />
                                    )}

                                    <CopyToClipboardButton
                                        text={permission}
                                        tooltip="Permission-Key kopieren"
                                        ariaLabel="Permission-Key kopieren"
                                        size="small"
                                        disabled={isBusy}
                                        icon={<ContentCopy fontSize="inherit"/>}
                                        successMessage="Permission-Key kopiert."
                                        errorMessage="Kopieren nicht möglich."
                                    />
                                </Stack>
                            </Paper>
                        );
                    })}
                </Box>
            </AccordionDetails>
        </Accordion>
    );
});
