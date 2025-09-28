import {BadgeProps} from './badge-props';
import {Chip, Tooltip} from '@mui/material';

export function Badge(props: BadgeProps) {
    if (props.tooltip) {
        return (
            <Tooltip {...props.tooltip}>
                <Chip
                    color={props.color}
                    sx={props.sx}
                    label={props.label}
                    size="small"
                />
            </Tooltip>
        );
    }

    return (
        <Chip
            color={props.color}
            sx={props.sx}
            label={props.label}
            size="small"
        />
    );
}