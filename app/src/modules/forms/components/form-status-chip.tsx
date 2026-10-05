import {alpha, useTheme} from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import {FormStatus, FormStatusColors, FormStatusIcons, FormStatusLabels} from '../enums/form-status';

interface FormStatusChipProps {
    status: FormStatus;
    size?: 'small' | 'medium';
    variant?: 'outlined' | 'filled' | 'soft';
}

export function FormStatusChip({ status, size = 'medium', variant = 'outlined' }: FormStatusChipProps) {
    const theme = useTheme();

    const label = FormStatusLabels[status];
    const icon = FormStatusIcons[status];
    const colorKey = FormStatusColors[status]; // e.g. 'success' | 'error' | 'info' | 'warning' | 'default'
    const paletteColor = theme.palette[colorKey] || theme.palette.primary;

    const softStyles =
        variant === 'soft'
            ? {
                color: paletteColor.main,
                backgroundColor: alpha(paletteColor.main, 0.08),
                '& .MuiChip-icon': {
                    color: paletteColor.main,
                },
            }
            : {};

    return (
        <Chip
            label={label}
            icon={icon || undefined}
            color={variant === 'soft' ? undefined : (colorKey as any)} // MUI only allows color on known variants
            variant={variant === 'soft' ? 'filled' : variant}
            size={size}
            sx={softStyles}
        />
    );
}
