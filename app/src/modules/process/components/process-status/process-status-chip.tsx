import {alpha, useTheme} from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import {ProcessStatus, ProcessStatusColors, ProcessStatusIcons, ProcessStatusLabels} from '../../enums/process-status';
import {SxProps} from '@mui/material';

interface FormStatusChipProps {
    status: ProcessStatus;
    size?: 'small' | 'medium';
    variant?: 'outlined' | 'filled' | 'soft';
    sx?: SxProps;
}

export function ProcessStatusChip({status, size = 'medium', variant = 'outlined', sx}: FormStatusChipProps) {
    const theme = useTheme();

    const label = ProcessStatusLabels[status];
    const Icon = ProcessStatusIcons[status];
    const colorKey = ProcessStatusColors[status]; // e.g. 'success' | 'error' | 'info' | 'warning' | 'default'
    const paletteColor = theme.palette[colorKey] || theme.palette.primary;

    const softStyles =
        variant === 'soft'
            ? {
                ...sx,
                color: paletteColor.main,
                backgroundColor: alpha(paletteColor.main, 0.08),
                '& .MuiChip-icon': {
                    color: paletteColor.main,
                },
            }
            : {
                ...sx,
            };

    return (
        <Chip
            label={label}
            icon={<Icon/>}
            color={variant === 'soft' ? undefined : (colorKey as any)} // MUI only allows color on known variants
            variant={variant === 'soft' ? 'filled' : variant}
            size={size}
            sx={softStyles}
        />
    );
}
