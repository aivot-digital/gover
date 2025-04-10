import {MouseEventHandler, ReactNode} from 'react';

// TODO: Replace with "actions" component

// A separator for the toolbar actions
type AppToolbarActionSeparator = 'separator';

// Base type for all toolbar actions
type AppToolbarActionBase = {
    label?: string;
    tooltip: string;
    icon: ReactNode;
    disabled?: boolean;
    visible?: boolean;
}

// Toolbar action with a click handler
type AppToolbarClickAction = AppToolbarActionBase & {
    onClick: MouseEventHandler;
}

// Toolbar action with a link
type AppToolbarLinkAction = AppToolbarActionBase & {
    href: string;
}

// All available actions for the toolbar
export type AppToolbarAction = AppToolbarActionSeparator | AppToolbarClickAction | AppToolbarLinkAction;

export interface AppToolbarProps {
    // The title of the toolbar
    title: string;
    // The actions to display in the toolbar
    actions?: AppToolbarAction[];
    // Listener for toolbar height changes
    updateToolbarHeight?: (height: number) => void;
}