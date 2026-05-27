import {Box, IconButton, InputAdornment, TextField, Tooltip, Typography} from '@mui/material';
import ChevronRight from '@mui/icons-material/ChevronRight';
import Close from '@mui/icons-material/Close';
import React from 'react';
import {type VDepartmentShadowedEntity} from '../entities/v-department-shadowed-entity';
import {getDepartmentPath, getDepartmentTypeIcons} from '../utils/department-utils';
import GroupWork from '@aivot/mui-material-symbols-400-outlined/dist/group-work/GroupWork';

interface DepartmentSelectFieldProps {
    label: string;
    value?: VDepartmentShadowedEntity | null;
    onOpenDialog: () => void;
    onClear: () => void;
    disabled?: boolean;
    error?: string;
    hint?: string;
    placeholder?: string;
    required?: boolean;
}

export function DepartmentSelectField(props: DepartmentSelectFieldProps): React.ReactElement {
    const {
        label,
        value,
        onOpenDialog,
        onClear,
        disabled = false,
        error,
        hint,
        placeholder = 'Keine Organisationseinheit ausgewählt',
        required = false,
    } = props;
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

    const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();
        onClear();
    };

    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (disabled) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onOpenDialog();
        }
    };

    return (
        <TextField
            fullWidth
            label={label}
            value={fieldValue}
            placeholder={placeholder}
            error={error != null}
            helperText={error ?? hint}
            required={required}
            onClick={() => {
                if (!disabled) {
                    onOpenDialog();
                }
            }}
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
    );
}
