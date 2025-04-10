import {BadgeProps} from './badge-props';
import {Badge as MuiBadge, Tooltip} from '@mui/material';

export function Badge(props: BadgeProps) {
    if (props.tooltip) {
        return (
            <Tooltip {...props.tooltip}>
                <MuiBadge
                    color={props.color}
                    sx={props.sx}
                >
                    {props.label}
                </MuiBadge>
            </Tooltip>
        );
    }

    return (
        <MuiBadge
            color={props.color}
            sx={props.sx}
        >
            {props.label}
        </MuiBadge>
    );
}