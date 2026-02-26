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
    IconButton,
    Paper,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import React from 'react';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';

import {PermissionDetails} from './permission-editor-props';

interface PermissionGroupAccordionProps {
    group: PermissionDetails;
    selectedPermissions: string[];
    isExpanded: boolean;
    isBusy: boolean;
    isEditable: boolean;
    onExpandedChange: (next: boolean) => void;
    onToggleGroup: (group: PermissionDetails, checked: boolean) => void;
    onTogglePermission: (permission: string, checked: boolean) => void;
    inferCrud: (permission: string) => 'create' | 'read' | 'update' | 'delete' | null;
    onCopyPermission: (permission: string) => void;
}

export function PermissionGroupAccordion(props: PermissionGroupAccordionProps): React.ReactElement {
    const {
        group,
        selectedPermissions,
        isExpanded,
        isBusy,
        isEditable,
        onExpandedChange,
        onToggleGroup,
        onTogglePermission,
        inferCrud,
        onCopyPermission,
    } = props;

    const total = group.permissions.length;
    const selectedInGroup = group.permissions.filter((p) => selectedPermissions.includes(p.permission)).length;
    const allSelected = total > 0 && selectedInGroup === total;

    return (
        <Accordion
            expanded={isExpanded}
            onChange={(_, next) => onExpandedChange(next)}
            disableGutters
            sx={{
                borderRadius: 2,
                '&:before': {display: 'none'},
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
        >
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Stack
                    direction="row"
                    alignItems="center"
                    spacing={1.5}
                    sx={{width: '100%', pr: 1}}
                >
                    <Typography
                        sx={{flex: 1, minWidth: 0}}
                        noWrap
                    >
                        {group.contextLabel}
                    </Typography>

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
                            >
                                {allSelected ? 'Abwählen' : 'Auswählen'}
                            </Button>
                        </span>
                    </Tooltip>
                </Stack>
            </AccordionSummary>

            <AccordionDetails>
                <Box
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: {xs: '1fr', md: '1fr 1fr'},
                        columnGap: 2,
                        rowGap: 2,
                    }}
                >
                    {group.permissions.map(({permission, label, description}) => {
                        const checked = selectedPermissions.includes(permission);
                        const crud = inferCrud(permission);

                        return (
                            <Paper
                                key={permission}
                                variant="outlined"
                                sx={{
                                    px: 1.5,
                                    py: 1,
                                    borderRadius: 2,
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: 1,
                                    '&:hover': {backgroundColor: 'action.hover'},
                                }}
                            >
                                <FormControlLabel
                                    sx={{m: 0, flex: 1, alignItems: 'flex-start'}}
                                    control={(
                                        <Checkbox
                                            checked={checked}
                                            onChange={(_, next) => onTogglePermission(permission, next)}
                                            disabled={isBusy || !isEditable}
                                            size="small"
                                            sx={{p: 0, pt: .25, pr: 1, m: 0}}
                                        />
                                    )}
                                    label={(
                                        <Box sx={{width: '100%'}}>
                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                alignItems="center"
                                                sx={{minWidth: 0}}
                                            >
                                                <Typography
                                                    variant="body2"
                                                    sx={{flex: 1, minWidth: 0}}
                                                    noWrap
                                                >
                                                    {label}
                                                </Typography>
                                            </Stack>

                                            <Typography
                                                variant="caption"
                                                color="text.secondary"
                                                sx={{
                                                    display: 'block',
                                                    fontFamily: 'monospace',
                                                    wordBreak: 'break-word',
                                                }}
                                            >
                                                {permission}
                                            </Typography>

                                            {description && (
                                                <Typography
                                                    variant="caption"
                                                    color="text.secondary"
                                                    sx={{
                                                        display: 'block',
                                                        mt: 0.25,
                                                        wordBreak: 'break-word',
                                                    }}
                                                >
                                                    {description}
                                                </Typography>
                                            )}
                                        </Box>
                                    )}
                                />

                                <Stack
                                    direction="row"
                                    spacing={1}
                                    alignItems="center"
                                    sx={{pt: 0.25}}
                                >
                                    {crud && (
                                        <Chip
                                            size="small"
                                            label={crud.toUpperCase()}
                                            variant="outlined"
                                        />
                                    )}

                                    <Tooltip title="Permission-Key kopieren">
                                        <span>
                                          <IconButton
                                              aria-label="Permission-Key kopieren"
                                              size="small"
                                              disabled={isBusy}
                                              onClick={() => onCopyPermission(permission)}
                                          >
                                            <ContentCopy fontSize="inherit" />
                                          </IconButton>
                                        </span>
                                    </Tooltip>
                                </Stack>
                            </Paper>
                        );
                    })}
                </Box>
            </AccordionDetails>
        </Accordion>
    );
}
