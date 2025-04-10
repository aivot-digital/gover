import {SxProps} from '@mui/material';
import {MouseEventHandler, ReactNode} from 'react';

// A separator for the actions
type ActionSeparator = 'separator';

// Base type for all actions
type ActionBase = {
    label?: string;
    tooltip?: string;
    icon: ReactNode;
    disabled?: boolean;
    visible?: boolean;
    variant?: 'text' | 'outlined' | 'contained';
    ignoreBusy?: boolean; // optional property to ignore the busy state (e.g. the help button should always be enabled)
}

// Action with a click handler
type ClickAction = ActionBase & {
    onClick: MouseEventHandler;
}

// Action with a link
type LinkAction = ActionBase & {
    href: string;
}

// Action with a internal link
type InternalLinkAction = ActionBase & {
    to: string;
}

// All available actions
export type Action = ActionSeparator | ClickAction | LinkAction | InternalLinkAction;

export interface ActionsProps {
    actions: Action[];
    sx?: SxProps;
    isBusy?: boolean;
}