import {type ReactNode} from 'react';
import {Box, darken, type Theme} from '@mui/material';
import {Chip, type ChipProps} from '../../../components/chip/chip';
import {formatRelativeInstantInApplicationTimeZone} from '../../../utils/temporal-utils';
import {formatDateTimeWithRelative, ProcessEmptyValue} from './process-detail-values';

export type ProcessDeadlineColor = 'warning' | 'error';

export function getDeadlineForegroundColor(theme: Theme, color: ProcessDeadlineColor): string {
    // Use the same readable foreground in plain table cells and soft deadline chips, without changing other chips.
    if (theme.palette.mode === 'dark') return theme.palette[color].light;
    return color === 'warning' ? darken(theme.palette.warning.dark, 0.2) : theme.palette.error.dark;
}

export function ProcessDeadlineChip({color = 'default', sx, ...props}: Omit<ChipProps, 'color'> & {color?: ProcessDeadlineColor | 'default'}) {
    return (
        <Chip
            {...props}
            color={color}
            mode="soft"
            sx={[
                ...(color === 'default' ? [] : [(theme: Theme) => ({color: getDeadlineForegroundColor(theme, color)})]),
                ...(Array.isArray(sx) ? sx : [sx]),
            ]}
        />
    );
}

interface ProcessDeadlineValueProps {
    deadline?: string | null;
    now?: number;
    color?: ProcessDeadlineColor;
    label?: string;
    emptyLabel: string;
}

export function ProcessDeadlineValue({deadline, now, color, label, emptyLabel}: ProcessDeadlineValueProps): ReactNode {
    const validDeadline = deadline != null && Number.isFinite(Date.parse(deadline));
    const relative = validDeadline && now != null ? formatRelativeInstantInApplicationTimeZone(deadline, now) : null;
    const remaining = relative?.startsWith('in ') ? `noch ${relative.slice(3)}`
        : relative?.startsWith('vor ') ? `seit ${relative.slice(4)} überschritten` : relative;

    return (
        <Box component="span" sx={{display: 'inline-flex', alignItems: 'center', flexWrap: 'wrap', columnGap: 1, rowGap: 0.5}}>
            {validDeadline ? (
                <span>
                    {formatDateTimeWithRelative(deadline)}
                    {remaining && ` (${remaining})`}
                </span>
            ) : <ProcessEmptyValue>{emptyLabel}</ProcessEmptyValue>}
            {color != null && label != null && <ProcessDeadlineChip label={label} size="small" color={color}/>}
        </Box>
    );
}
