import {FormStatus, FormStatusColors, FormStatusIcons, FormStatusLabels} from '../enums/form-status';
import Chip from '@mui/material/Chip';

interface FormStatusChipProps {
    status: FormStatus;
    size?: 'small' | 'medium';
    variant?: 'outlined' | 'filled';
}

export function FormStatusChip(props: FormStatusChipProps) {
    const {
        status,
        size,
        variant,
    } = props;

    const label = FormStatusLabels[status];
    const icon = FormStatusIcons[status];
    const color = FormStatusColors[status];

    return (
        <Chip
            label={label}
            icon={icon ? icon : undefined}
            color={color}
            variant={variant ? variant : 'outlined'}
            size={size ? size : 'medium'}
        />
    );
}