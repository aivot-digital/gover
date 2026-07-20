import {Box, IconButton, InputAdornment, TextField, Tooltip, Typography} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import React, {useState} from 'react';
import {type VDepartmentShadowedEntity, type VDepartmentShadowedEntityWithChildren} from '../entities/v-department-shadowed-entity';
import {getDepartmentPath, getDepartmentTypeIcons} from '../utils/department-utils';
import GroupWork from '@aivot/mui-material-symbols-400-n25-outlined/GroupWork';
import {SelectDepartmentDialog} from '../dialogs/select-department-dialog';

interface DepartmentSelectFieldProps {
    label: string;
    value?: VDepartmentShadowedEntity | null;
    onChange: (department: VDepartmentShadowedEntityWithChildren | null) => void;
    disabled?: boolean;
    error?: string;
    hint?: string;
    placeholder?: string;
    required?: boolean;
    dialogTitle?: string;
    departments?: VDepartmentShadowedEntityWithChildren[];
    isDepartmentSelectable?: (department: VDepartmentShadowedEntityWithChildren) => boolean;
    getDepartmentDisabledTooltip?: (department: VDepartmentShadowedEntityWithChildren) => string | undefined;
}

export function DepartmentSelectField(props: DepartmentSelectFieldProps): React.ReactElement {
    const {
        label,
        value,
        onChange,
        disabled = false,
        error,
        hint,
        placeholder = 'Keine Organisationseinheit ausgewählt',
        required = false,
        dialogTitle = 'Organisationseinheit auswählen',
        departments,
        isDepartmentSelectable,
        getDepartmentDisabledTooltip,
    } = props;

    const [showSelectDepartmentDialog, setShowSelectDepartmentDialog] = useState(false);
    const showDepartmentPath = value != null && (value.parentNames?.length ?? 0) > 0;
    const departmentPath = showDepartmentPath ? getDepartmentPath(value) : undefined;
    const departmentIcon = value != null ? getDepartmentTypeIcons(value.depth) : <GroupWork />;
    const fieldValue = value?.name ?? '';
    const interactiveCursor = disabled ? 'default' : 'pointer';
    const primaryContentColor = disabled ? 'text.disabled' : 'text.primary';
    const secondaryContentColor = disabled ? 'text.disabled' : 'text.secondary';
    const iconColor = disabled ? 'text.disabled' : value != null ? 'primary.main' : 'action.active';
    const endIconColor = disabled ? 'text.disabled' : 'action.active';
    const clearTooltip = disabled || value == null ? '' : 'Auswahl entfernen';

    const handleOpenDialog = () => {
        if (!disabled) {
            setShowSelectDepartmentDialog(true);
        }
    };

    const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();
        onChange(null);
    };

    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (disabled) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            setShowSelectDepartmentDialog(true);
        }
    };

    return (
        <>
            <TextField
                fullWidth
                label={label}
                value={fieldValue}
                placeholder={placeholder}
                disabled={disabled}
                error={error != null}
                helperText={error ?? hint}
                required={required}
                onClick={handleOpenDialog}
                onKeyDown={handleKeyDown}
                InputLabelProps={{
                    title: label,
                }}
                FormHelperTextProps={{
                    title: error ?? hint,
                    sx: {
                        whiteSpace: 'normal',
                    },
                }}
                inputProps={{
                    readOnly: true,
                    title: value?.name,
                    'aria-label': label,
                }}
                InputProps={{
                    startAdornment: (
                        <InputAdornment
                            position="start"
                            sx={{
                                minWidth: 0,
                                flex: 1,
                                alignItems: 'center',
                                mr: 1,
                            }}
                        >
                            <Box
                                component="span"
                                sx={{
                                    display: 'inline-flex',
                                    flexShrink: 0,
                                    mr: 1.25,
                                    color: iconColor,
                                    '& .MuiSvgIcon-root': {
                                        fontSize: 20,
                                    },
                                }}
                            >
                                {departmentIcon}
                            </Box>

                            <Box
                                component="span"
                                sx={{
                                    minWidth: 0,
                                    flex: 1,
                                }}
                            >
                                {
                                    value != null ? (
                                        <>
                                            <Typography
                                                variant="body2"
                                                component="span"
                                                color={primaryContentColor}
                                                sx={{
                                                    display: 'block',
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                    fontSize: '1rem',
                                                    lineHeight: 1.25,
                                                }}
                                                title={value.name}
                                            >
                                                {value.name}
                                            </Typography>

                                            {
                                                departmentPath != null &&
                                                <Typography
                                                    variant="caption"
                                                    component="span"
                                                    color={secondaryContentColor}
                                                    sx={{
                                                        display: 'block',
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                        fontSize: '0.75rem',
                                                        lineHeight: 1.2,
                                                    }}
                                                    title={departmentPath}
                                                >
                                                    {departmentPath}
                                                </Typography>
                                            }
                                        </>
                                    ) : (
                                        <Typography
                                            variant="body2"
                                            component="span"
                                            color={secondaryContentColor}
                                            sx={{
                                                display: 'block',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            {placeholder}
                                        </Typography>
                                    )
                                }
                            </Box>
                        </InputAdornment>
                    ),
                    endAdornment: (
                        <InputAdornment position="end">
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 0.5,
                                    mr: -0.5,
                                }}
                            >
                                <Tooltip
                                    title={clearTooltip}
                                    arrow
                                >
                                    <span>
                                        <IconButton
                                            size="small"
                                            onClick={handleClear}
                                            onMouseDown={(event) => {
                                                event.preventDefault();
                                                event.stopPropagation();
                                            }}
                                            disabled={disabled || value == null}
                                            aria-label="Auswahl entfernen"
                                        >
                                            <Close fontSize="small"/>
                                        </IconButton>
                                    </span>
                                </Tooltip>

                                <ChevronRight
                                    fontSize="small"
                                    sx={{color: endIconColor}}
                                />
                            </Box>
                        </InputAdornment>
                    ),
                }}
                sx={{
                    '& .MuiOutlinedInput-root': {
                        cursor: interactiveCursor,
                        height: 56,
                        minHeight: 56,
                    },
                    '& .MuiOutlinedInput-input': {
                        width: 0,
                        minWidth: 0,
                        flex: '0 0 0',
                        p: 0,
                        cursor: interactiveCursor,
                        caretColor: 'transparent',
                    },
                    '& .MuiInputAdornment-root': {
                        pointerEvents: disabled ? 'none' : 'auto',
                    },
                }}
            />

            <SelectDepartmentDialog
                open={showSelectDepartmentDialog}
                title={dialogTitle}
                departments={departments}
                isDepartmentSelectable={isDepartmentSelectable}
                getDepartmentDisabledTooltip={getDepartmentDisabledTooltip}
                selectedDepartmentId={value?.id ?? null}
                onClose={() => {
                    setShowSelectDepartmentDialog(false);
                }}
                onSelect={(department) => {
                    onChange(department);
                    setShowSelectDepartmentDialog(false);
                }}
            />
        </>
    );
}
