import {ReactNode} from 'react';
import {Action} from '../actions/actions-props';
import {BadgeProps} from '../badge/badge-props';

export interface GenericPageHeaderProps {
    icon: ReactNode;
    badge?: BadgeProps;
    title: string;
    actions?: Action[];
    helpDialog?: {
        tooltip: string;
        title: string;
        content: ReactNode;
    };
    isBusy?: boolean;
}
