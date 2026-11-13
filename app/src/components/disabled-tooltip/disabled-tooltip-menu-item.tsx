import {MenuItem, type MenuItemProps} from '@mui/material';
import React, {type ReactNode} from 'react';
import {DisabledTooltip} from './disabled-tooltip';

interface DisabledTooltipMenuItemProps extends MenuItemProps {
    disabledTooltip?: string;
}

// Disabled menu items ignore pointer events, so only those get a hoverable wrapper.
// Enabled items stay unwrapped because MenuList injects its focus handling into them.
export function DisabledTooltipMenuItem(props: DisabledTooltipMenuItemProps): ReactNode {
    const {disabledTooltip, ...menuItemProps} = props;
    const item = <MenuItem {...menuItemProps}/>;

    if (menuItemProps.disabled !== true) {
        return item;
    }

    return (
        <DisabledTooltip
            disabled
            title={disabledTooltip}
            wrapperSx={{display: 'block'}}
        >
            {item}
        </DisabledTooltip>
    );
}
