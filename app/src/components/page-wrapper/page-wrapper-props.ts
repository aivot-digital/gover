import {type AppToolbarAction} from '../app-toolbar/app-toolbar-props';
import {type LoadingWrapperProps} from '../loading-wrapper/loading-wrapper-props';

export interface PageWrapperProps extends LoadingWrapperProps {
    is404?: boolean;
    error?: string;
    title: string;
    fullWidth?: boolean;
    background?: boolean;
    toolbarActions?: AppToolbarAction[];
}
