import {Box, Button, Typography} from '@mui/material';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import React from 'react';
import {Actions} from '../../../components/actions/actions';
import {type VDepartmentShadowedEntity} from '../entities/v-department-shadowed-entity';
import {getDepartmentPath} from '../utils/department-utils';

interface DepartmentSelectFieldProps {
    label: string;
    value?: VDepartmentShadowedEntity | null;
    onOpenDialog: () => void;
    onClear: () => void;
    disabled?: boolean;
    placeholder?: string;
}

export function DepartmentSelectField(props: DepartmentSelectFieldProps): React.ReactElement {
    const {
        label,
        value,
        onOpenDialog,
        onClear,
        disabled = false,
        placeholder = 'Keine Organisationseinheit ausgewählt',
    } = props;

    return (
        <Box sx={{width: '100%'}}>
            <Typography>
                {label}
            </Typography>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'stretch',
                    gap: 1,
                    mt: 1,
                }}
            >
                <Box
                    sx={{
                        flex: 1,
                        minWidth: 0,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1,
                        px: 2,
                        py: 1.5,
                        minHeight: 56,
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    {
                        value != null ? (
                            <Box sx={{minWidth: 0}}>
                                <Typography
                                    variant="body2"
                                    sx={{
                                        fontWeight: 600,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                    }}
                                    title={value.name}
                                >
                                    {value.name}
                                </Typography>

                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                    sx={{
                                        display: 'block',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                    }}
                                    title={getDepartmentPath(value)}
                                >
                                    {getDepartmentPath(value)}
                                </Typography>
                            </Box>
                        ) : (
                            <Typography
                                variant="body2"
                                color="text.secondary"
                            >
                                {placeholder}
                            </Typography>
                        )
                    }
                </Box>

                <Button
                    variant="outlined"
                    onClick={onOpenDialog}
                    disabled={disabled}
                    sx={{
                        flexShrink: 0,
                        minWidth: 130,
                    }}
                >
                    {value != null ? 'Ändern' : 'Auswählen'}
                </Button>

                <Actions
                    dense={true}
                    size="small"
                    sx={{
                        my: 'auto',
                        flexShrink: 0,
                    }}
                    actions={[
                        {
                            icon: <DeleteOutline/>,
                            tooltip: 'Auswahl entfernen',
                            onClick: onClear,
                            disabled: disabled || value == null,
                        },
                    ]}
                />
            </Box>
        </Box>
    );
}
