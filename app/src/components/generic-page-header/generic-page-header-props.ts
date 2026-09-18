import {type ReactNode} from 'react';
import {type Action} from '../actions/actions-props';
import {type BadgeProps} from '../badge/badge-props';

export interface GenericPageHeaderProps {
    icon: ReactNode;
    badge?: BadgeProps | BadgeProps[] | ReactNode;
    title: string;
    actions?: Action[];
    helpDialog?: GenericPageHeaderPropsHelpDialog;
    isBusy?: boolean;
}

export interface GenericPageHeaderPropsHelpDialog {
    tooltip: string;
    title: string;
    content: ReactNode;
}
