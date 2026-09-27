import {Box, Skeleton, Tooltip} from '@mui/material';
import {Chip} from '../chip/chip';
import {type GenericListFilter} from './generic-list-props';

const numberFormat = new Intl.NumberFormat('de-DE');

export function GenericListFilterLabel({filter, count, failed, busy}: {
    filter: GenericListFilter;
    count: number | undefined;
    failed: boolean;
    busy: boolean;
}) {
    return (
        <Box
            component="span"
            aria-busy={busy || undefined}
            sx={{display: 'inline-flex', alignItems: 'center', gap: 1}}
        >
            {filter.label}
            {count != null ? (
                <Chip
                    component="span"
                    label={numberFormat.format(count)}
                    size="small"
                    mode="soft"
                    color={count > 0 ? (filter.countColor ?? 'default') : 'default'}
                    sx={{height: 22, opacity: busy ? 0.5 : 1, fontVariantNumeric: 'tabular-nums', '& .MuiChip-label': {px: 0.75}}}
                />
            ) : failed ? (
                <Tooltip
                    title="Die Anzahl konnte nicht geladen werden."
                    arrow
                >
                    <Box
                        component="span"
                        aria-label="Anzahl nicht verfügbar"
                        sx={{minWidth: 22, textAlign: 'center', color: 'text.secondary'}}
                    >
                        –
                    </Box>
                </Tooltip>
            ) : busy ? (
                <Skeleton
                    component="span"
                    variant="rounded"
                    width={22}
                    height={14}
                    aria-hidden="true"
                    sx={{borderRadius: 7, bgcolor: 'action.hover'}}
                />
            ) : null}
        </Box>
    );
}
